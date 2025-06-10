from __future__ import annotations
from operator import itemgetter
import os
from agent_core.commands import change_approver
from agent_core.commands import repository_operations
from agent_core.utils.path_utils import path_utils
from agent_core.commands import sonar_qube_analysis
from agent_core.logs.logger import logger
from agent_core.agents.base import BaseAgent
from agent_core.command_decorator import command
from agent_core.app.main import shutdown

from agent_core.utils.write_fix_utils.change_tracking import FileChanges


import hashlib
import json


from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from agent_core.agents import BaseAgent

COMMAND_CATEGORY = "write_fix"
COMMAND_CATEGORY_TITLE = "Apply a proposed fix"
ALLOWLIST_CONTROL = "allowlist"
DENYLIST_CONTROL = "denylist"


@command(
    "write_fix",
    "Use this command to implement the fix you came up with. Only use this command if you think that you have collected all necessary information by using other commands. The project will automatically be rebuilt and reanalyzed by SonarQube. Changes are reverted automatically if the build fails or if the rule violation remains.",
    {
        "changed_lines": {
            "type": "dict",
            "description": "a dictionary of the changed lines",
            "required": True

        }
    },
)
def write_fix(changes_dicts: list, agent: BaseAgent) -> str:
    agent.write_fix_attempts += 1

    feedback = ""

    if len(changes_dicts) == 0:
        return "REJECTED  \nThe fix you passed is empty. Please provide a non empty implementation of the fix."

    state_switched = False
    if agent.current_state != "no_state_machine" and agent.current_state != "Trying out Fix Candidates":
        agent.update_prompt_state("Trying out Fix Candidates")
        state_switched = True

    try:
        all_files_with_changes = execute_write_range(changes_dicts, agent)

    except ApplyChangesError as ace:
        # Need to rollback in case one of the files to change was already overwritten successfully or partially
        rollback_changes(agent)
        feedback = "REJECTED  \nFailure when trying to apply the fix: " + ace.msg + \
            "  \n\nIMPORTANT: The repository has been restored to its original state! You need to start applying changes from scratch again."

        sanitized_warning_file_path = agent.ai_config.warning_file_path.replace(
            "/", ".")
        with open(os.path.join("experimental_setups", agent.exps[-1], "implausible_patches",
                               f"{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{sanitized_warning_file_path}_line_{str(agent.ai_config.warning_start_line)}_implausible_patches.json"), "a+") as exps:
            exps.write(
                f"  \n### IMPLAUSIBLE FIX (fix no. {str(agent.write_fix_attempts)})\n{json.dumps(changes_dicts, indent=4)}\n\n ###CHANGE APPROVER FEEDBACK:  \n{feedback}")

        if state_switched:
            feedback += "  \n**Note:** You are automatically switched to the state 'Trying out Fix Candidates'"

        return feedback

    change_approver_feedback = change_approver.approve_changes(
        changes_dicts, all_files_with_changes, agent)

    feedback += change_approver_feedback

    rollback_changes(agent)

    if state_switched:
        feedback += "  \n**Note:** You are automatically switched to the state 'Trying out Fix Candidates'"

    return feedback


def execute_write_range(changes_dicts: list[dict], agent: BaseAgent) -> list[FileChanges]:
    """
    Applies all the changes from the full changes_dicts list and writes them to the respective files.

    Returns:
        list[dict]: list of the list of lines (with changes) per file
    Throws:
        ApplyChangesError: If one of the lines to change (delete/modify/insert) is out of the file's range
    """

    all_files_with_changes = []

    project_dir = os.path.join(
        agent.config.workspace_path, agent.ai_config.warning_repository_name)

    change_dicts_merged_by_path = merge_changes_dicts_with_same_file_path(
        changes_dicts, agent)

    # Apply the changes from all of the grouped change_dicts
    for (file_relative_path_from_list, change_dicts) in change_dicts_merged_by_path:
        file_full_path_from_list = os.path.join(
            project_dir, file_relative_path_from_list)

        # If the file is not in the agent's list of initial analysis reports, create and add it
        add_new_file_to_initial_analysis_report(
            file_relative_path_from_list, agent)

        changed_file = apply_changes(
            change_dicts, file_relative_path_from_list, file_full_path_from_list)
        all_files_with_changes.append(changed_file)

    return all_files_with_changes


def merge_changes_dicts_with_same_file_path(changes_dicts: list[dict], agent: BaseAgent) -> list[str, list[dict]]:
    """
    Merge separate dicts for the same file, for correct change tracking
    """
    change_dicts_merged_by_path: list[tuple[str, list[dict]]] = []

    for change_dict in changes_dicts:

        file_relative_path = change_dict.get("file_name", None)

        if file_relative_path is None:
            logger.error("apply_changes failed",
                         "The write_fix command was in a wrong format. Couldn't find `file_name` in the change_dict. change_dict:" + str(change_dict))
            raise ApplyChangesError(
                "The write_fix command was in a wrong format. Couldn't find `file_name` in the change_dict.")
        try:
            file_relative_path = path_utils.preprocess_paths(
                agent.config.workspace_path, agent.ai_config.warning_repository_name, file_relative_path)
        except ValueError as ve:
            logger.error("apply_changes failed",
                         "The path couldn't be processed with error: " + str(ve))
            raise ApplyChangesError(str(ve))

        change_dicts_with_same_file_path = list(filter(
            lambda change_dict_merged_by_path, file_relative_path=file_relative_path: change_dict_merged_by_path[0] == file_relative_path, change_dicts_merged_by_path))
        if len(change_dicts_with_same_file_path) == 0:
            # No change_dict for the current file path present yet, so add it
            change_dicts_merged_by_path.append(
                (file_relative_path, [change_dict]))
        else:
            change_dicts_with_same_file_path[0][1].append(change_dict)

    return change_dicts_merged_by_path


def add_new_file_to_initial_analysis_report(file_relative_path: str, agent: BaseAgent) -> None:
    """
    If the file is not in the agent's list of initial analysis reports, create and add it
    """

    if file_relative_path not in agent.initial_analysis_reports:

        sanitized_warning_file_path = agent.ai_config.warning_file_path.replace(
            "/", ".")
        sanitized_file_relative_path = file_relative_path.replace("/", ".")
        initial_analysis_report_new_file = sonar_qube_analysis.analyze_file_and_parse_report(file_relative_path, agent.sonar_qube_rules_in_active_profile, agent.ai_config.warning_repository_name,
                                                                                             f"{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{sanitized_warning_file_path}_line_{str(agent.ai_config.warning_start_line)}_initial_analysis_report_file_{sanitized_file_relative_path}.json", agent)

        agent.initial_analysis_reports[file_relative_path] = initial_analysis_report_new_file


def apply_changes(change_dicts_with_same_file_path: list[dict], file_relative_path: str, file_full_path: str) -> FileChanges:
    """
    Applies the changes from all change_dicts regarding one file and writes them back to the file.

    Returns:
        FileChanges: Object tracking all of the made changes in a file and how they relate to the initial file.

    Throws:
        ApplyChangesError: If one of the lines to change (delete/modify/insert) is out of the file's range
    """
    # Read the original code from the file
    with open(file_full_path, 'r') as file:
        lines = file.readlines()

    file_changes = FileChanges(file_relative_path, lines)

    insertions = []
    deletions = []
    modifications = []

    # Go through the change dicts with the same file path and collect the changes
    for change_dict in change_dicts_with_same_file_path:
        # Check for correct format
        new_insertions = change_dict.get("insertions", None)
        new_deletions = change_dict.get("deletions", None)
        new_modifications = change_dict.get("modifications", None)

        if all(map(lambda element: element is None, [new_insertions, new_deletions, new_modifications])):
            logger.error("apply_changes failed",
                         "The write_fix command was in a wrong format. Neither `insertions`, `deletions` nor `modifications` was given in the change_dict. change_dict:" + str(change_dict))
            raise ApplyChangesError(
                "The write_fix command was in a wrong format. Neither `insertions`, `deletions` nor `modifications` was given in the change_dict."
            )
        if new_insertions is not None:
            insertions.extend(new_insertions)
        if new_deletions is not None:
            deletions.extend(new_deletions)
        if new_modifications is not None:
            modifications.extend(new_modifications)

    # Mark deletions via an identifier first to avoid conflicts with line number changes
    deleted_lines_identifier = pre_mark_deletions(file_changes, deletions)

    # Apply modifications
    apply_modifications(file_changes, modifications)

    # Apply insertions, from last to first for correct line referencing
    apply_insertions(file_changes, insertions)

    # Finally apply the deletions
    apply_deletions(file_changes, deleted_lines_identifier)

    # Remember pairs of insertions and deletions of the same line number (considered as a kind of modification)
    file_changes.change_tracked_lines.find_insertion_deletion_pairs_forming_modification()

    # Write the modified code back to the file
    with open(file_full_path, 'w') as file:
        file.writelines(file_changes.change_tracked_lines)

    return file_changes


def pre_mark_deletions(file_changes: FileChanges, deletions: list) -> str:
    """
    Marks deletions via an identifier in the file_changse object first to avoid conflicts with line number changes
    """
    deleted_lines_identifier = hashlib.sha512(
        b"THIS_LINE_IS_TO_BE_DELETED_IDENTIFIER").hexdigest()

    for line_number in deletions:
        if 1 <= int(line_number) <= len(file_changes.change_tracked_lines):
            file_changes.change_tracked_lines[int(
                line_number) - 1] = deleted_lines_identifier + "\n"

        else:
            logger.error("apply_changes failed",
                         f"Line {line_number} to delete was out of range for the file {file_changes.file_path}. The file only has {len(file_changes.change_tracked_lines)} lines.")
            raise ApplyChangesError(
                f"Line {line_number} to delete was out of range for the file {file_changes.file_path}. The file only has {len(file_changes.change_tracked_lines)} lines.")

    return deleted_lines_identifier


def apply_modifications(file_changes: FileChanges, modifications: list[dict]):
    """
    Applies all the modifications to the file_changes object.
    """
    for modification in modifications:
        line_number = modification.get("line_number", 0)
        modified_line = modification.get("modified_line", "")
        if 1 <= int(line_number) <= len(file_changes.change_tracked_lines):

            if not modified_line.endswith("\n"):
                modified_line += "\n"
            file_changes.change_tracked_lines.update(
                int(line_number - 1), modified_line)
        else:
            logger.error("apply_changes failed",
                         f"Line {line_number} to modify was out of range for the file {file_changes.file_path}. The file only has {len(file_changes.change_tracked_lines)} lines.")
            raise ApplyChangesError(
                f"Line {line_number} to modify was out of range for the file {file_changes.file_path}. The file only has {len(file_changes.change_tracked_lines)} lines.")


def apply_insertions(file_changes: FileChanges, insertions: list[dict]):
    """
    Apply insertions, from last to first for correct line referencing
    """

    sorted_insertions = sorted(
        insertions, key=itemgetter('line_number'), reverse=True)
    for insertion in sorted_insertions:
        line_number = int(insertion.get("line_number", 0))

        # There might be items in new_lines that hold multiple lines in one string (via \n), or if commas are missing between the items.
        # So we clean this into one proper list of singular lines, by splitting by \n and then adding it back at the end of each line item.
        new_lines_uncleaned: list[str] = insertion.get("new_lines", [])
        new_lines_cleaned_with_new_line = [
            line + "\n"
            for item in new_lines_uncleaned
            for line in item.rstrip("\n").split("\n")
        ]

        for new_line in new_lines_cleaned_with_new_line:
            # If the line to insert is multiple lines out of the files line range
            # then fill up with newlines until the index to insert is reached
            while int(line_number) > len(file_changes.change_tracked_lines) + 1:
                file_changes.change_tracked_lines.append("\n")

            file_changes.change_tracked_lines.insert(
                int(line_number) - 1, new_line)
            line_number += 1


def apply_deletions(file_changes: FileChanges, deleted_lines_identifier: str):
    """ 
    Finally delete all the lines from the list that are flagged as to be deleted.
    It is possible that a modification of the same line as a deletion overwrites the flag.
    This is intended and therefore in this case the modification wins.
    """

    for line_index, line in reversed(list(enumerate(file_changes.change_tracked_lines))):
        if line == deleted_lines_identifier + "\n":
            file_changes.change_tracked_lines.pop(line_index)


def rollback_changes(agent: BaseAgent):
    try:
        repository_operations.checkout_project(agent)
    except repository_operations.GitError as git_error:
        # Checking out the project shouldn't fail here, because it didn't fail before.
        # If it still fails, there is no way to recover anyways, because we require a clean version of the repo.
        logger.error(
            "Aborting", f"Re-checking out the project failed with Error: {git_error}. There is no way to recover from this. Therefore aborting the execution.")
        shutdown(agent, 1)


class ApplyChangesError(Exception):
    def __init__(self, msg):
        super().__init__(msg)
        self.msg = msg

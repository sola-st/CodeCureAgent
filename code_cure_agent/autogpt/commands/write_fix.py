from __future__ import annotations
from operator import itemgetter
import os
from autogpt.commands import change_approver
from autogpt.commands import repository_operations
from autogpt.commands import path_utils
from autogpt.logs.logger import logger
from autogpt.agents.base import BaseAgent
from autogpt.command_decorator import command

from autogpt.utils.write_fix_utils.change_tracking import FileChanges


import hashlib


from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from autogpt.agents import BaseAgent

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
        # Need to rollback in case one of the files to change was already overwritten successfully
        rollback_changes(agent)
        feedback = "REJECTED  \nFailure when trying to apply the fix: " + ace.msg + \
            "  \nThe repository has been restored to its original state."

        if state_switched:
            feedback += "  \n**Note:** You are automatically switched to the state 'Trying out Fix Candidates'"

        return feedback

    change_approver_feedback = change_approver.approve_changes(
        changes_dicts, all_files_with_changes, agent)

    feedback += "  \n" + change_approver_feedback

    rollback_changes(agent)
    feedback += "  \nThe repository has been restored to its original state."

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

    change_dicts_merged_by_path = []

    project_dir = os.path.join(
        agent.config.workspace_path, agent.ai_config.warning_repository_name)

    # Merge separate dicts for the same file, for correct change tracking
    for change_dict in changes_dicts:

        file_relative_path = change_dict.get("file_name", None)

        if file_relative_path is None:
            raise ApplyChangesError(
                "The write_fix command was in a wrong format. Couldn't find `file_name` in the change_dict.")
        try:
            file_relative_path = path_utils.preprocess_paths(
                agent.config.workspace_path, agent.ai_config.warning_repository_name, file_relative_path)
        except ValueError as ve:
            raise ApplyChangesError(str(ve))

        file_full_path = os.path.join(project_dir, file_relative_path)

        try:
            index_change_dict_with_same_path = list(map(
                lambda change_dict_merged_by_path: change_dict_merged_by_path[0], change_dicts_merged_by_path)).index(file_full_path)

            change_dicts_merged_by_path[index_change_dict_with_same_path][1].append(
                change_dict)
        except ValueError as ve:
            # No change_dict for the current file path present yet, so add it
            new_list = list()
            new_list.append(change_dict)
            change_dicts_merged_by_path.append(
                (file_full_path, new_list))

    for (file_full_path, change_dicts) in change_dicts_merged_by_path:
        changed_file = apply_changes(
            change_dicts, file_relative_path, file_full_path)
        all_files_with_changes.append(changed_file)

    return all_files_with_changes


def apply_changes(change_dicts_with_same_file_path: list[dict], file_relative_path: str, file_full_path: str) -> FileChanges:
    """
    Applies the changes from a single change_dict (one file's changes) and writes them to the file.

    Returns:
        dict: the lines of the file with the changes applied. Contains file_name and file_content.

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
            raise ApplyChangesError(
                "The write_fix command was in a wrong format. Neither `insertions`, `deletions` nor `modifications` was given in the change_dict."
            )
        if new_insertions is not None:
            insertions.extend(new_insertions)
        if new_deletions is not None:
            deletions.extend(new_deletions)
        if modifications is not None:
            modifications.extend(new_modifications)

    # Mark deletions via an identifier first to avoid conflicts with line number changes
    deleted_lines_identifier = hashlib.sha512(
        b"THIS_LINE_IS_TO_BE_DELETED_IDENTIFIER").hexdigest()

    for line_number in deletions:
        if 1 <= int(line_number) <= len(file_changes.change_tracked_lines):
            file_changes.change_tracked_lines[int(
                line_number) - 1] = deleted_lines_identifier + "\n"

        else:
            logger.warn(
                f"Line {line_number} to delete was out of range for the file {file_relative_path}. The file only has {len(file_changes.change_tracked_lines)} lines.", "apply_changes failed")
            raise ApplyChangesError(
                f"Line {line_number} to delete was out of range for the file {file_relative_path}. The file only has {len(file_changes.change_tracked_lines)} lines.")

    # Apply modifications
    for modification in modifications:
        line_number = modification.get("line_number", 0)
        modified_line = modification.get("modified_line", "")
        if 1 <= int(line_number) <= len(file_changes.change_tracked_lines):

            if modified_line.endswith("\n"):
                file_changes.change_tracked_lines[int(
                    line_number) - 1] = modified_line
            else:
                file_changes.change_tracked_lines[int(
                    line_number) - 1] = modified_line + "\n"
        else:
            logger.warn(
                f"Line {line_number} to modify was out of range for the file {file_relative_path}. The file only has {len(file_changes.change_tracked_lines)} lines.", "apply_changes failed")
            raise ApplyChangesError(
                f"Line {line_number} to modify was out of range for the file {file_relative_path}. The file only has {len(file_changes.change_tracked_lines)} lines.")

    # Apply insertions
    sorted_insertions = sorted(insertions, key=itemgetter('line_number'))
    for insertion in sorted_insertions:
        line_number = int(insertion.get("line_number", 0))
        for new_line in insertion.get("new_lines", []):
            # If the line to insert is multiple lines out of the files line range
            # then fill up with newlines until the index to insert is reached
            while int(line_number) > len(file_changes.change_tracked_lines) + 1:
                file_changes.change_tracked_lines.append("\n")

            file_changes.change_tracked_lines.insert(
                int(line_number) - 1, new_line)
            line_number += 1

    # Finally delete all the lines from the list that are flagged as to be deleted.
    # It is possible that a modification of the same line as a deletion overwrites the flag.
    # This is intended and therefore in this case the modification wins.
    for line_index, line in enumerate(file_changes.change_tracked_lines):
        if line == deleted_lines_identifier + "\n":
            file_changes.change_tracked_lines.pop(line_index)

    # Write the modified code back to the file
    with open(file_full_path, 'w') as file:
        file.writelines(file_changes.change_tracked_lines)

    return file_changes


def rollback_changes(agent: BaseAgent):
    try:
        repository_operations.checkout_project(agent)
    except repository_operations.GitError as git_error:
        # Checking out the project shouldn't fail here, because it didn't fail before.
        # If it still fails, there is no way to recover anyways, because we require a clean version of the repo.
        logger.error(
            "Aborting", f"Re-checking out the project failed with Error: {git_error}. There is no way to recover from this. Therefore aborting the execution.")
        exit(1)


class ApplyChangesError(Exception):
    def __init__(self, msg):
        super().__init__(msg)
        self.msg = msg

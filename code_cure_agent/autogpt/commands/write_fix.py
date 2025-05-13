from __future__ import annotations
from operator import itemgetter
import os
from autogpt.commands import change_approver
from autogpt.commands import repository_operations
from autogpt.commands import path_utils
from autogpt.logs.logger import logger
from autogpt.agents.base import BaseAgent
from autogpt.command_decorator import command


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
    run_ret = ""

    state_switched = False
    if agent.current_state != "no_state_machine" and agent.current_state != "Trying out Fix Candidates":
        agent.update_prompt_state("Trying out Fix Candidates")
        state_switched = True

    if len(changes_dicts) == 0:
        return "The fix you passed is empty. Please provide a non empty implementation of the fix."

    try:
        all_files_with_changes = execute_write_range(changes_dicts, agent)
        run_ret = "Fix applied successfully."

    except ApplyChangesError as ace:
        # Need to rollback in case one of the files to change was already overwritten successfully
        rollback_changes(agent)
        run_ret = "Failure when trying to apply the fix: " + ace.msg

        if state_switched:
            run_ret += "  \n**Note:** You are automatically switched to the state 'Trying out Fix Candidates'"

        return run_ret

    # TODO: implement change_approver and pass it also the unchanged version of the files
    unchanged_files = []
    change_approver_feedback = change_approver.approve_changes(
        changes_dicts, all_files_with_changes, unchanged_files, agent)

    run_ret += "  \n" + change_approver_feedback

    if state_switched:
        run_ret += "  \n**Note:** You are automatically switched to the state 'Trying out Fix Candidates'"

    return run_ret


def execute_write_range(changes_dicts, agent: BaseAgent) -> list[dict]:
    """
    Applies all the changes from the full changes_dicts list and writes them to the respective files.

    Returns:
        list[dict]: list of the list of lines (with changes) per file
    Throws:
        ApplyChangesError: If one of the lines to change (delete/modify/insert) is out of the file's range
    """

    all_files_with_changes = []

    for change_dict in changes_dicts:
        changed_file = apply_changes(change_dict, agent)
        all_files_with_changes.append(changed_file)


def apply_changes(change_dict: dict, agent: BaseAgent) -> dict:
    """
    Applies the changes from a single change_dict (one file's changes) and writes them to the file.

    Returns:
        dict: the lines of the file with the changes applied. Contains file_name and file_content.

    Throws:
        ApplyChangesError: If one of the lines to change (delete/modify/insert) is out of the file's range
    """

    project_dir = os.path.join(
        agent.config.workspace_path, agent.ai_config.warning_repository_name)

    file_relative_path = change_dict.get("file_name", "")

    file_relative_path = path_utils.preprocess_paths(
        agent.config.workspace_path, agent.ai_config.warning_repository_name, file_relative_path)
    file_full_path = os.path.join(project_dir, file_relative_path)

    insertions = change_dict.get("insertions", [])
    deletions = change_dict.get("deletions", [])
    modifications = change_dict.get("modifications", [])

    # Read the original code from the file
    with open(file_full_path, 'r') as file:
        lines = file.readlines()

    # Mark deletions via an identifier first to avoid conflicts with line number changes
    deleted_lines_identifier = hashlib.sha512(b"THIS_LINE_IS_TO_BE_DELETED_IDENTIFIER").hexdigest()

    for line_number in deletions:
        if 1 <= int(line_number) <= len(lines):
            lines[int(line_number) - 1] = deleted_lines_identifier + "\n" 
            
        else:
            logger.warn(
                f"Line {line_number} to delete was out of range for the file {file_relative_path}. The file only has {len(lines)} lines.", "apply_changes failed")
            raise ApplyChangesError(
                f"Line {line_number} to delete was out of range for the file {file_relative_path}. The file only has {len(lines)} lines.")

    # Apply modifications
    for modification in modifications:
        line_number = modification.get("line_number", 0)
        modified_line = modification.get("modified_line", "")
        if 1 <= int(line_number) <= len(lines):

            if modified_line.endswith("\n"):
                lines[int(line_number) - 1] = modified_line
            else:
                lines[int(line_number) - 1] = modified_line + "\n"
        else:
            logger.warn(
                f"Line {line_number} to modify was out of range for the file {file_relative_path}. The file only has {len(lines)} lines.", "apply_changes failed")
            raise ApplyChangesError(
                f"Line {line_number} to modify was out of range for the file {file_relative_path}. The file only has {len(lines)} lines.")

    # Apply insertions
    sorted_insertions = sorted(insertions, key=itemgetter('line_number'))
    for insertion in sorted_insertions:
        line_number = int(insertion.get("line_number", 0))
        for new_line in insertion.get("new_lines", []):
            # If the line to insert is multiple lines out of the files line range
            # then fill up with newlines until the index to insert is reached
            while int(line_number) > len(lines) + 1:
                lines.append("\n")
            lines.insert(int(line_number) - 1, new_line)
            line_number += 1

    
    # Finally delete all the lines from the list that are flagged as to be deleted.
    # It is possible that a modification of the same line as a deletion overwrites the flag. 
    # This is intended and therefore in this case the modification wins.
    lines = [line for line in lines if line != deleted_lines_identifier + "\n" ]

    # Write the modified code back to the file
    with open(file_full_path, 'w') as file:
        file.writelines(lines)

    return {
        "file_name": file_relative_path,
        "file_content": lines
    }


def rollback_changes(agent: BaseAgent):
    repository_operations.checkout_project(agent)


class ApplyChangesError(Exception):
    def __init__(self, msg):
        super().__init__(msg)
        self.msg = msg

from __future__ import annotations


from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from autogpt.agents import BaseAgent

import os

from autogpt.command_decorator import command
from autogpt.commands import path_utils

COMMAND_CATEGORY = "repository_reading_tools"
COMMAND_CATEGORY_TITLE = "Commands to read lines or search strings in the repository"

ALLOWLIST_CONTROL = "allowlist"
DENYLIST_CONTROL = "denylist"


@command(
    "read_range",
    "Read a range of lines in a given file",
    {
        "file_path": {
            "type": "string",
            "description": "The path to the file to read from.",
            "required": True,
        },
        "start_line": {
            "type": "integer",
            "description": "The number of the line to start reading from in the given file.",
            "required": True

        },
        "end_line": {
            "type": "integer",
            "description": "The number of the line to stop reading at.",
            "required": True

        }
    },
)
def read_range(file_path: str, start_line: int, end_line: int, agent: BaseAgent) -> str:
    """Read a range of lines starting from line number start_line and ending at line number end_line

    Args:
        name (str): The name of the project
        index (int): The index number of the target bug
        filename (str): The path to the file to read from
        start_line (int): The line number at which the reading starts
        end_line (int): The line number at which the reading ends

    Returns:
        str: The read lines between start_line and end_line
    """

    # sanity checks
    if start_line < 1:
        return "Reading lines failed. start_line must be greater than 0."

    if end_line < start_line:
        return "Reading lines failed. end_line must be greater or equal than start_line."

    workspace = agent.config.workspace_path
    project_dir = os.path.join(
        workspace, agent.ai_config.warning_repository_name)

    file_path = path_utils.preprocess_paths(
        workspace, agent.ai_config.warning_repository_name, file_path)
    with open(os.path.join(project_dir, file_path)) as fp:
        lines = fp.readlines()

    lines_str = "\n"

    for i in range(start_line-1, end_line, 1):

        # Prevent reading further than the file is long (a last newline character without any character following it is not considered a new line)
        if len(lines) <= i:
            lines_str += "\nEOF"
            break
        lines_str += "Line {}:".format(i+1) + lines[i]
    return lines_str

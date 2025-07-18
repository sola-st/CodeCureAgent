from __future__ import annotations


import subprocess
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from agent_core.agents import BaseAgent

import os

from agent_core.command_decorator import command
from agent_core.utils.path_utils import path_utils
from agent_core.logs.logger import logger

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
    if start_line < 0:
        # using < 0 instead of <= 0 here, because the model sometimes tries to read starting from line 0
        return "Reading lines failed. start_line must be greater or equal 0."

    if end_line < start_line:
        return "Reading lines failed. end_line must be greater or equal than start_line."

    workspace = agent.config.workspace_path
    project_dir = os.path.join(
        workspace, agent.ai_config.warning_repository_name)
    try:
        file_path = path_utils.preprocess_paths(
            workspace, agent.ai_config.warning_repository_name, file_path)

    except ValueError as ve:
        return f"Reading lines failed. The file_path '{file_path}' does not exist."

    with open(os.path.join(project_dir, file_path)) as fp:
        lines = fp.readlines()

    lines_str = ""

    for i in range(start_line - 1, end_line, 1):
        # if start_line was 0 skip this non existing line
        if i < 0:
            continue

        # Prevent reading further than the file is long (a last newline character without any character following it is not considered a new line)
        if i >= len(lines):
            lines_str += "EOF"
            break
        lines_str += "Line {}:".format(i + 1) + lines[i]
    return lines_str.rstrip("\n")


@command(
    "search_for_patterns",
    "Search for the patterns in java files using grep",
    {
        "patterns": {
            "type": "list[string]",
            "description": "The pattern or patterns to search for",
            "required": True,
        },
        "include": {
            "type": "string",
            "description": "The files to include",
            "required": True
        }
    },
)
def search_for_patterns(patterns: list[str], include: str, agent: BaseAgent) -> str:
    if len(patterns) == 0:
        logger.error(
            "search_for_patterns was called with an empty list of patterns")
        return "Error in search_for_patterns: The 'patterns' list was empty. Provide at least one pattern to search for."

    repo_path = os.path.join(agent.config.workspace_path,
                             agent.ai_config.warning_repository_name)

    cmd = "grep -rinHsIE --max-count=1000"

    if include != "":
        if os.path.isfile(os.path.join(repo_path, include)):
            cmd += f" '{include}'"
        else:
            cmd += f" --include '{include}'"

    for pattern in patterns:
        # Add the patterns with cleaned single quotes
        pattern_cleaned = str(pattern).replace(
            r"\'", "'").replace("'", r"'\''")
        cmd += f" --regexp='{pattern_cleaned}'"

    logger.info(title="search_for_patterns command:", message=cmd)

    result = subprocess.run(
        cmd,
        capture_output=True,
        encoding="utf8",
        cwd=repo_path,
        shell=True
    )

    if result.returncode > 1:
        logger.error("search_for_patterns failed in subprocess",
                     f"Error: {result.stderr};\n{result.stdout}")
        return "Error in search_for_patterns. Don't use the command again."

    # 1 indicates no results found
    elif result.returncode == 1:
        return "No search results found."

    else:
        search_results = result.stdout.splitlines()

        number_of_results = len(search_results)

        if number_of_results <= 50:
            return f"Found {str(number_of_results)} search results:\n\n" + "\n".join(search_results)
        else:
            search_results = search_results[:50]
            return f"Found {str(number_of_results)} search results. Only showing the first 50 results:\n\n" + "\n".join(search_results)

from __future__ import annotations


from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from autogpt.agents import BaseAgent

import os

from autogpt.command_decorator import command
from autogpt.commands import path_utils



@command(
    "read_range",
    "Read a range of lines in a given file",
    {
        "project_name": {
            "type": "string",
            "description": "The name of the project.",
            "required": True,
        },
        "bug_index":{
            "type": "integer",
            "description": "The index (number) of the bug that you want to get info about.",
            "required": True

        },
        "file_path": {
            "type": "string",
            "description": "The path to the file to read from.",
            "required": True,
        },
        "start_line":{
            "type": "integer",
            "description": "The number of the line to start reading from in the given file.",
            "required": True

        },
        "end_line":{
            "type": "integer",
            "description": "The number of the line to stop reading at.",
            "required": True

        }
    },
)
def read_range(project_name: str, bug_index: str, file_path: str, start_line: int, end_line: int, agent: BaseAgent) -> str:
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
    ai_name = agent.ai_config.ai_name

    return execute_read_range(project_name, bug_index, file_path, start_line, end_line, agent)


def execute_read_range(project_name: str, bug_index: str, file_path: str, start_line: int, end_line: int, agent: BaseAgent):
    workspace = agent.config.workspace_path
    project_dir = os.path.join(workspace, project_name.lower()+"_"+str(bug_index)+"_buggy")
    """
    if not os.path.exists(os.path.join(project_dir,file_path)):
        if not os.path.exists(os.path.join(project_dir, "files_index.txt")):
            with open(os.path.join(project_dir, "files_index.txt"), "w") as fit:
                fit.write("\n".join(list_java_files(project_dir)))
            
        with open(os.path.join(project_dir, "files_index.txt")) as fit:
            files_index = [f for f in fit.read().splitlines() if file_path in f]
        
        if len(files_index) == 1:
            file_path = files_index[0]
        elif len(files_index) >= 1:
            raise ValueError("Multiple Candidate Paths. We do not handle this yet!")
        else:
            return "The file_path {} does not exist.".format(file_path)
    """
    file_path = path_utils.preprocess_paths(agent, project_name, bug_index, file_path)
    with open(os.path.join(project_dir,file_path)) as fp:
        lines = fp.readlines()

    lines_str = ""
    for i in range(start_line-1, end_line, 1):
        lines_str+="Line {}:".format(i+1) + lines[i]
    return lines_str
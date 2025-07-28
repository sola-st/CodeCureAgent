"""Commands to control the internal state of the program"""

from __future__ import annotations
import json
import os
import re
from git.exc import GitError
from agent_core.logs import logger
from agent_core.command_decorator import command
from agent_core.agents.agent import Agent
from typing import NoReturn
from agent_core.app.main import shutdown
from agent_core.commands import write_fix
from agent_core.commands import repository_operations

COMMAND_CATEGORY = "system"
COMMAND_CATEGORY_TITLE = "System"


@command(
    "goals_accomplished",
    "Goals are accomplished and there is nothing left to do",
    {
        "reason": {
            "type": "string",
            "description": "A summary to the user of how the goals were accomplished",
            "required": True,
        }
    },
)
def goals_accomplished(reason: str, agent: Agent) -> NoReturn:
    """
    A function that takes in a string and exits the program after reapplying the last plausible fix.
    Doesn't exit the program if no plausible fix was created yet.

    Parameters:
        reason (str): A summary to the user of how the goals were accomplished.
    """

    if agent.plausible_fixes == 0:
        return "Trying to set the goals as accomplished failed! You have not yet accomplished the goal!  "  \
            "\nNone of your previous calls to write_fix have been approved. All of them have been labeled as 'REJECTED'.  " \
            "\nYour goals are only accomplished after one of your write_fix calls returns 'APPROVED'.  " \
            "\nAdhere to the information given to you about your failed write_fix attempts and propose a fix that resolves the issues.  "  \
            "\nOnly call this command again after one of your write_fix attempts returns 'APPROVED'."

    reapply_last_plausible_fix(agent)

    logger.info(title="Shutting down...\n", message=reason)
    shutdown(agent, 0)


def reapply_last_plausible_fix(agent: Agent) -> None:
    """
    Extracts the last plausible fix from the plausible_patches file and reapplies it to the project.
    """

    try:
        repository_operations.checkout_project(agent)
    except GitError as e:
        logger.error(
            "Could not reapply the last plausible fix. Re-checking out the project was not successful. Likely deleting the project folder failed.")
        return

    with open(os.path.join("experimental_setups", agent.exps[-1], agent.current_state, "plausible_patches",
                           f"{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{agent.ai_config.warning_file_name}_line_{str(agent.ai_config.warning_start_line)}_plausible_patches.json")) as patch_file:
        patch_file_cont = patch_file.read()

    json_list_pattern = re.compile(
        r'### PLAUSIBLE FIX \(fix no. \d+\)\n(\[\s*(?:.|\n)*?\])\n\n ###CHANGE APPROVER FEEDBACK', re.MULTILINE)

    matches = json_list_pattern.findall(patch_file_cont)
    changes_dicts = json.loads(matches[-1])

    # This should not be able to fail, as it already successfully passed once before
    write_fix.execute_write_range(changes_dicts, agent,
                                  create_analysis_reports=False)

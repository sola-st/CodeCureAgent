"""Commands to control the internal state of the program"""

from __future__ import annotations
from agent_core.logs import logger
from agent_core.command_decorator import command
from agent_core.agents.agent import Agent
from typing import NoReturn
from agent_core.app.main import shutdown

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
    A function that takes in a string and exits the program

    Parameters:
        reason (str): A summary to the user of how the goals were accomplished.
    Returns:
        A result string from create chat completion. A list of suggestions to
            improve the code.
    """

    if agent.plausible_fixes == 0:
        return "Trying to set the goals as accomplished failed! You have not yet accomplished the goal!  "  \
            "\nNone of your previous calls to write_fix have been approved. All of them have been labeled as 'REJECTED'.  " \
            "\nYour goals are only accomplished after one of your write_fix calls returns 'APPROVED'.  " \
            "\nAdhere to the information given to you about your failed write_fix attempts and propose a fix that resolves the issues.  "  \
            "\nOnly call this command again after one of your write_fix attempts returns 'APPROVED'."

    logger.info(title="Shutting down...\n", message=reason)
    shutdown(agent, 0)

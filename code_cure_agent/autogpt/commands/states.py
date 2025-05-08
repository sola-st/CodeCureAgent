COMMAND_CATEGORY = "states"
COMMAND_CATEGORY_TITLE = "STATES"
ALLOWLIST_CONTROL = "allowlist"
DENYLIST_CONTROL = "denylist"

from autogpt.command_decorator import command
from autogpt.agents.agent import Agent


@command(
        "go_to_gather_context_for_fix",
        "go_to_gather_context_for_fix: Transitions to the state `Gathering Context for a Fix`.",
        {
        }
)
def go_to_gather_context_for_fix(agent: Agent) -> str:
    if agent.current_state != "Gathering Context for a Fix":
        agent.update_prompt_state("Gathering Context for a Fix")
    
    return "The current state has been changed from 'Understanding the Violated Rule' to 'Gathering Context for a Fix'."

@command(
        "go_back_to_understanding_rule",
        "Allows you to return back to the state 'Understanding the Violated Rule' where you can collect more information of the specific rule.",
        {
            "reason_for_going_back":{
                "type": "string",
                "description": "Give your reason for going back to the state 'Understanding the Violated Rule'",
                "required": True
            }
        }
)
def go_back_to_understanding_rule(reason_for_going_back: str, agent: Agent) -> str:
    if agent.current_state != "Understanding the Violated Rule":
        agent.update_prompt_state("Understanding the Violated Rule")
    return "You are now back at the state 'Understanding the Violated Rule'."

@command(
        "go_back_to_gather_context_for_fix",
        "Allows you to return back to the state 'Gathering Context for a Fix' where you can collect more information about the code.",
        {
            "reason_for_going_back":{
                "type": "string",
                "description": "Give your reason for going back to the state 'Gathering Context for a Fix'",
                "required": True
            }
        }
)
def go_back_to_gather_context_for_fix(reason_for_going_back: str, agent: Agent) -> str:
    if agent.current_state != "Gathering Context for a Fix":
        agent.update_prompt_state("Gathering Context for a Fix")
    return "You are now back at the state 'Gathering Context for a Fix'."





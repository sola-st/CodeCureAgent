from autogpt.command_decorator import command

from autogpt.agents.base import BaseAgent


COMMAND_CATEGORY = "planning"
COMMAND_CATEGORY_TITLE = "Creating and updating a plan to follow"

ALLOWLIST_CONTROL = "allowlist"
DENYLIST_CONTROL = "denylist"


@command(
        "formulate_plan",
        "Formulate or update a plan, with fine-grained steps, about how you want to approach collecting all relevant information to fix the rule violation.",
        {
            "plan":{
                "type": "string",
                "description": "The fine-grained plan you want to follow for collecting relevant information",
                "required": True
            }
        }
)
def formulate_plan(plan: str, agent: BaseAgent) -> str:
    no_plan_yet = False
    if len(agent.plans) == 0:
        no_plan_yet = True

    agent.plans.append(plan)
    
    if no_plan_yet:
        return "Your plan was successfully created."
    else:
        return "Your plan was successfully updated."
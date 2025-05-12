import os
from autogpt.agents.base import BaseAgent

# TODO: Implement approval logic
def approve_changes(changes_dicts: list[dict], all_files_with_changes: list[dict], unchanged_files: list[dict], agent: BaseAgent):

    sanitized_warning_file_path = agent.ai_config.warning_file_path.replace(
        "/", ".")
    with open(os.path.join("experimental_setups", agent.exps[-1], "plausible_patches",
                           f"{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{sanitized_warning_file_path}_{str(agent.ai_config.warning_start_line)}_plausible_patches.json"), "a+") as exps:
        exps.write("  \n### PLAUSIBLE FIX\n{}\n".format(str(changes_dicts)))

    return "Accepted."

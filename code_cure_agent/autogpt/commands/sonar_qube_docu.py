from __future__ import annotations


from autogpt.logs import logger


from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from autogpt.agents import BaseAgent

from autogpt.command_decorator import command

import subprocess
import os
import json
import shutil

COMMAND_CATEGORY = "sonarQubeDocuTool"
COMMAND_CATEGORY_TITLE = "Retrieve the SonarQube rule documentation"

ALLOWLIST_CONTROL = "allowlist"
DENYLIST_CONTROL = "denylist"


@command(
    "read_sonarqube_docu",
    "Read the SonarQube docu for a given rule_key.",
    {
        "rule_key": {
            "type": "string",
            "description": "The rule whos docu is to be read.",
            "required": True,
        }
    },
)
def read_sonarqube_docu(rule_key: str, agent: BaseAgent) -> str:

    docu_output_file_name = f"{agent.ai_config.warning_ID}_docu_tool_output_rule_{rule_key}.json"
    docu_output_path = os.path.join(
        agent.config.workspace_path, docu_output_file_name)

    result = call_sonarqube_docu_retriever(rule_key, docu_output_path, agent)

    if result.returncode == 0:
        logger.info("",
                    "Retrieving docu was successful."
                    )
        return parse_and_format_docu(docu_output_path, docu_output_file_name, agent)
    else:
        logger.error(
            "Error", f"Retrieving docu of SonarQube rule {rule_key} failed with error: " + result.stderr)
        # TODO: Remove stack trace from the message here, as it can't be easily removed in the Java program
        return f"Error: Reading the SonarQube docu for rule {rule_key} failed with error: " + result.stderr


def call_sonarqube_docu_retriever(rule_key: str, docu_output_path: str, agent: BaseAgent) -> subprocess.CompletedProcess[str]:

    logger.info("",
                f"Retrieving docu for SonarQube rule '{rule_key}'. \nThe output will be saved to '{docu_output_path}'"
                )

    # Create docu command
    cmd = ["java", "-jar", agent.config.sorald_jar_path, "docu",
           "--rule-key", rule_key, "--stats-output-file", docu_output_path]

    result = subprocess.run(
        cmd,
        capture_output=True,
        encoding="utf8",
        shell=False
    )

    return result


def parse_and_format_docu(docu_output_path: str, docu_output_file_name: str, agent: BaseAgent) -> str:

    with open(docu_output_path, "r") as docu_output_file:
        rule_description = json.load(docu_output_file)

    # Save to experiment folder for debugging and informational purposes
    shutil.copy(docu_output_path, os.path.join("experimental_setups",
                agent.exps[-1], "docu_tool_outputs", docu_output_file_name))

    # TODO: Format this as needed; How to transform html with many tags to more readable text? Try html2text (GNUv3 (no changes allowed)) or html-text
    return str(rule_description)

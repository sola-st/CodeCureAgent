from __future__ import annotations


from agent_core.logs import logger


from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from agent_core.agents import BaseAgent

from agent_core.command_decorator import command

import subprocess
import os
import json
import shutil
import re

import html2text

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
    """
    Implementation of the read_sonarqube_docu command.
    Retrieves the documentation of the passed rule directly from the sonarsource.sonarlint.core, via a Java CLI command (as part of the Sorald CLI).
    The result is transformed from a html document to a markdown-like readable text and returned to the agent.
    """

    docu_output_file_name = f"{agent.ai_config.warning_ID}_docu_tool_output_rule_{rule_key}.json"
    docu_output_path = os.path.join(
        agent.config.workspace_path, docu_output_file_name)

    result = call_sonarqube_docu_retriever(rule_key, docu_output_path, agent)

    if result.returncode == 0:
        logger.info("",
                    "Retrieving docu was successful."
                    )
        return parse_and_format_docu(docu_output_path, docu_output_file_name, rule_key, agent)
    else:
        logger.error(
            "Error in read_sonarqube_docu command: ", f"Retrieving docu of SonarQube rule {rule_key} failed with error: " + result.stderr)

        # Print error message with Stack Traces removed
        return f"Error: Reading the SonarQube docu for rule {rule_key} failed with error:  \n" + "\n".join([line for line in result.stderr.splitlines() if not re.match(r"at .+\(.+:[0-9]+\)", line.strip())])


def call_sonarqube_docu_retriever(rule_key: str, docu_output_path: str, agent: BaseAgent) -> subprocess.CompletedProcess[str]:
    """
    Calls the Sorald CLI with the added docu command to retrieve the docu of rule rule_key.
    """

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


def parse_and_format_docu(docu_output_path: str, docu_output_file_name: str, rule_key: str, agent: BaseAgent) -> str:
    """
    Uses the result from calling the CLI to create a single docu string with html transformed to markdown-like readable text.
    """

    with open(docu_output_path, "r") as docu_output_file:
        rule_description: dict = json.load(docu_output_file)

    # Save to experiment folder for debugging and informational purposes
    shutil.copy(docu_output_path, os.path.join("experimental_setups",
                agent.exps[-1], agent.current_state, "docu_tool_outputs", docu_output_file_name))

    rule_name = rule_description.get("name")
    caption = ""
    if rule_name:
        caption = "**" + rule_name + "**  \n\n"

    html_description: str = rule_description.get("htmlDescription")
    if html_description:
        try:
            # Convert html document to readable document
            converted_description = html2text.html2text(
                html_description, bodywidth=0)
            return caption + re.sub(r"#+ ", "", converted_description).rstrip("\n")
        except Exception as e:
            logger.error("Exception in read_sonarqube_docu command: ",
                         f"Couldn't convert html to markdown. Exception was: {str(e)}. Falling back to giving the plain html to the LLM. The html was: {str(html_description)}")
            return caption + str(html_description).rstrip("\n")

    # Hotspot-type rules use the desciptionSections instead of the htmlDescription in the docu object
    else:
        description_sections: list[dict] = rule_description.get(
            "descriptionSections")
        if not description_sections:
            logger.error("Error in read_sonarqube_docu command: ",
                         f"The retrieved rule_description object for rule {rule_key} had no 'htmlDescription' or 'descriptionSections' fields, or they were empty. The rule_description object was: " + str(rule_description))

            return f"Error: Reading the SonarQube docu for rule {rule_key} failed. The docu was not found. Please don't try to use this command with the same rule_key again.  "
        else:
            converted_description = ""
            for description_section in description_sections:
                html_content = description_section.get("htmlContent")
                if html_content:
                    try:
                        converted_description += html2text.html2text(
                            html_content, bodywidth=0) + "\n"
                    except Exception as e:
                        logger.error("Exception in read_sonarqube_docu command: ",
                                     f"Couldn't convert html to markdown. Exception was: {str(e)}. Falling back to giving the plain html to the LLM. The html was: {str(html_content)}")
                        converted_description += html_content + "\n"

            return caption + re.sub(r"#+ ", "", converted_description).rstrip("\n")

from __future__ import annotations


import subprocess
import os
import shutil
import json
from autogpt.logs import logger


from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from autogpt.agents import BaseAgent

from autogpt.command_decorator import command
from autogpt.utils.path_utils import path_utils

COMMAND_CATEGORY = "sonarQubeAnalysis"
COMMAND_CATEGORY_TITLE = "Run SonarQube analysis"

ALLOWLIST_CONTROL = "allowlist"
DENYLIST_CONTROL = "denylist"


class AnalysisError(Exception):
    def __init__(self, msg):
        super().__init__(msg)


@command(
    "analyze_file",
    "Run SonarQube analysis on a file",
    {
        "file_path": {
            "type": "string",
            "description": "The path to the file to analyze",
            "required": True,
        }
    },
)
def analyze_file_command(file_path: str, agent: BaseAgent):
    return analyze_file_and_parse_report(file_path, agent.sonar_qube_rules_in_active_profile, agent.ai_config.warning_repository_name, "analysis_report.json", agent)


def analyze_file_and_parse_report(file_relative_path: str, rules: list[str], repo_name: str, analysis_report_file_name: str, agent: BaseAgent) -> dict:
    """
    Analyze a file with SonarQube (via Sorald miner). Creates the analysis report json. Then parses this report into a dict object and returns it.

    Args:
        file_relative_path (str): Path to the file to analyze. (Relative to the repository under analysis)
        rules (list[str]): SonarQube rules to check (list of SIds). If the list is empty all rules are checked.
        repo_name (str): Name of the repository under analysis
        analysis_report_file_name (src): Name of the analysis report file to be saved.
        agent (BaseAgent): The agent with its configuration
    Returns:
        dict: The created and parsed analysis report.
    """

    result = analyze_file(file_relative_path, rules,
                          repo_name, analysis_report_file_name, agent)

    if result.returncode == 0:
        logger.info("",
                    f"Running SonarQube analysis was successful."
                    )
        return parse_analysis_report(analysis_report_file_name, agent)
    else:
        logger.error(
            "Error", "Running SonarQube analysis failed with error: " + result.stderr)
        raise AnalysisError(f"Error: {result.stderr}")


def rule_violation_present_in_analysis_report(analysis_report: dict, warning_rule_key: str, warning_start_line: int) -> bool:
    """Checks whether the expected rule violation is present in the analysis report"""
    mined_rules = analysis_report["minedRules"]
    return any(map(lambda mined_rule: mined_rule["ruleKey"] == warning_rule_key and ___warning_locations_contains_start_line(mined_rule["warningLocations"], warning_start_line), mined_rules))


def ___warning_locations_contains_start_line(warning_locations: dict, warning_start_line: int) -> bool:
    return any(map(lambda warning_location: warning_location["startLine"] == warning_start_line, warning_locations))


def analyze_file(file_relative_path: str, rules: list[str], repo_name: str, analysis_report_file_name: str, agent: BaseAgent) -> subprocess.CompletedProcess[str]:
    """
    Analyze a file with SonarQube (via Sorald miner). Creates the analysis report json.
    Args:
        file_relative_path (str): Path to the file to analyze. (Relative to the repository under analysis)
        rules (list[str]): SonarQube rules to check (list of SIds). If the list is empty all rules are checked.
        repo_name (str): Name of the repository under analysis
        analysis_report_file_name (src): Name of the analysis report file to be saved.
        agent (BaseAgent): The agent with its configuration
    Returns:
        subprocess.CompletedProcess[str]: Result of running the mining suprocess. If subprocess was succesful then the property "returncode" is 0.
    """
    workspace = agent.config.workspace_path

    # Prepare the paths
    file_relative_path = path_utils.preprocess_paths(
        workspace, repo_name, file_relative_path)
    file_path = os.path.join(workspace, repo_name, file_relative_path)

    analysis_report_path = os.path.join(workspace, analysis_report_file_name)

    logger.info("",
                f"Running SonarQube analysis on file '{file_path}'. \nThe report will be saved to '{analysis_report_path}'"
                )

    # Create mining command
    cmd = ["java", "-jar", agent.config.sorald_jar_path, "mine", "--source", file_path, "--stats-output-file",
           analysis_report_path, "--rule-parameters", "sonarqube_quality_profile/quality_profile_rule_parameters.json"]

    if rules is not None and len(rules) > 0:
        cmd.append("--rule-keys")
        cmd.append(",".join(rules))

    logger.debug("",
                 f"The SonarQube analysis on file '{file_path}' is run with the following command: {' '.join(cmd)}"
                 )

    result = subprocess.run(
        cmd,
        capture_output=True,
        encoding="utf8",
        shell=False
    )
    return result


def parse_analysis_report(analysis_report_file_name: str, agent: BaseAgent) -> dict:
    workspace = agent.config.workspace_path

    analysis_report_path = os.path.join(workspace, analysis_report_file_name)
    with open(analysis_report_path, "r") as analysis_report_file:
        analysis_report = json.load(analysis_report_file)

    shutil.copy(analysis_report_path, os.path.join("experimental_setups",
                agent.exps[-1], "analysis_reports", analysis_report_file_name))

    return analysis_report

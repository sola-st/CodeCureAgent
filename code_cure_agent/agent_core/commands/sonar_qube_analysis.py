from __future__ import annotations


import subprocess
import os
import shutil
import json
import time
from agent_core.logs import logger


from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from agent_core.agents import BaseAgent

from agent_core.utils.path_utils.path_utils import preprocess_paths

COMMAND_CATEGORY = "sonarQubeAnalysis"
COMMAND_CATEGORY_TITLE = "Run SonarQube analysis"

ALLOWLIST_CONTROL = "allowlist"
DENYLIST_CONTROL = "denylist"


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
    Raises:
        AnalysisError if the analysis failed
    """

    result = analyze_file(file_relative_path, rules,
                          repo_name, analysis_report_file_name, agent)

    if result.returncode == 0:
        logger.info("",
                    "Running SonarQube analysis was successful."
                    )
        return parse_analysis_report(analysis_report_file_name, agent)
    else:
        logger.error(
            "Error", "Running SonarQube analysis failed with error: " + result.stderr)
        raise AnalysisError(f"Error: {result.stderr}")


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
    Raises:
        AnalysisError if the file_relative_path couldn't be resolved
    """
    workspace = agent.config.workspace_path

    # Prepare the paths
    try:
        file_relative_path = preprocess_paths(
            workspace, repo_name, file_relative_path)
    except ValueError as ve:
        logger.error("Error in running SonarQube analysis",
                     f"The provided relative file path could not be resolved by running preprocess_paths. The file_relative_path was: {file_relative_path} and the error info of preprocess_paths: {str(ve)}")
        raise AnalysisError(
            f"Error: The provided relative file path could not be resolved by running preprocess_paths. The file_relative_path was: {file_relative_path} and the error info of preprocess_paths: {str(ve)}")

    file_path = os.path.join(workspace, repo_name, file_relative_path)

    analysis_report_path = os.path.join(workspace, analysis_report_file_name)

    logger.info("",
                f"Running SonarQube analysis on file '{file_path}'. \nThe report will be saved to '{analysis_report_path}'"
                )

    # Create mining command
    cmd = ["java", "-jar", agent.config.sorald_jar_path, "mine", "--source", file_path, "--stats-output-file",
           analysis_report_path, "--rule-parameters", "sonarqube_quality_profile/quality_profile_rule_parameters.json", "--target-java-version", agent.ai_config.warning_repository_target_java_version]

    if rules is not None and len(rules) > 0:
        cmd.append("--rule-keys")
        cmd.append(",".join(rules))

    logger.debug("",
                 f"The SonarQube analysis on file '{file_path}' is run with the following command: {' '.join(cmd)}"
                 )

    # Write the analysis start to the execution info file
    with open(os.path.join("experimental_setups", agent.exps[-1], agent.current_state, "execution_info", f"{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{agent.ai_config.warning_file_name}_line_{str(agent.ai_config.warning_start_line)}_execution_info"), "a+") as patf:
        patf.write(
            f"!! SonarQube analysis startup timestamp: " + str(time.time_ns()) + "\n")

    result = subprocess.run(
        cmd,
        capture_output=True,
        encoding="utf8",
        shell=False
    )

    with open(os.path.join("experimental_setups", agent.exps[-1], agent.current_state, "execution_info", f"{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{agent.ai_config.warning_file_name}_line_{str(agent.ai_config.warning_start_line)}_execution_info"), "a+") as patf:
        patf.write(
            f"!! SonarQube analysis end timestamp: " + str(time.time_ns()) + "\n")

    return result


def parse_analysis_report(analysis_report_file_name: str, agent: BaseAgent) -> dict:
    workspace = agent.config.workspace_path

    analysis_report_path = os.path.join(workspace, analysis_report_file_name)
    with open(analysis_report_path, "r") as analysis_report_file:
        analysis_report = json.load(analysis_report_file)

    shutil.copy(analysis_report_path, os.path.join("experimental_setups",
                agent.exps[-1], agent.current_state, "analysis_reports", analysis_report_file_name))

    return analysis_report


def rule_violation_present_in_analysis_report(analysis_report: dict, warning_rule_key: str, warning_start_line: int) -> bool:
    """
    Checks whether the expected rule violation is present in the analysis report
    """
    mined_rules = analysis_report["minedRules"]
    return any(map(lambda mined_rule: mined_rule["ruleKey"] == warning_rule_key and ___warning_locations_contains_start_line(mined_rule["warningLocations"], warning_start_line), mined_rules))


def ___warning_locations_contains_start_line(warning_locations: dict, warning_start_line: int) -> bool:
    return any(map(lambda warning_location: warning_location["startLine"] == warning_start_line, warning_locations))


def number_of_same_rule_violation_in_line_reduced(initial_analysis_report: dict, sonar_qube_report_after: dict, warning_rule_key: str, warning_start_line_before: int, warning_start_line_after: int) -> bool:
    '''
    Counts the number of the same warning in the target line before and after the changes and then checks if this number is reduced from before to after.
    Same warning means same ruleKey and same line here, but there might be several of such warnings in a line.
    Returns: (bool) if the number of this warning is reduced.
    '''
    mined_rules_initial = initial_analysis_report["minedRules"]
    target_rules_in_initial = [
        mined_rule for mined_rule in mined_rules_initial if mined_rule["ruleKey"] == warning_rule_key]
    if len(target_rules_in_initial) == 0:
        target_rule_violations_in_line_before = []
    else:
        target_rule_violations_in_line_before = [
            warning_location for warning_location in target_rules_in_initial[0]["warningLocations"] if warning_location["startLine"] == warning_start_line_before]

    mined_rules_after = sonar_qube_report_after["minedRules"]
    target_rules_after = [
        mined_rule for mined_rule in mined_rules_after if mined_rule["ruleKey"] == warning_rule_key]
    if len(target_rules_after) == 0:
        target_rule_violations_in_line_after = []
    else:
        target_rule_violations_in_line_after = [
            warning_location for warning_location in target_rules_after[0]["warningLocations"] if warning_location["startLine"] == warning_start_line_after]

    return len(target_rule_violations_in_line_after) < len(target_rule_violations_in_line_before)


class AnalysisError(Exception):
    def __init__(self, msg):
        super().__init__(msg)

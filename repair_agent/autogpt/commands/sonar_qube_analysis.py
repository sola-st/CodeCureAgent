import subprocess
import os
import json
from autogpt.logs import logger
from autogpt.agents import BaseAgent

SORALD_JAR_PATH = "/workspaces/master-thesis-pascal-joos/repair_agent/sorald/sorald.jar"


"""
Analyze a file with SonarQube (via Sorald miner). Creates the analysis report json.

Args:
        agent (BaseAgent): The agent with its configuration
        file_relative_path (str): Path to the file to analyze. (Relative to the repository under analysis)
        rules (list[str]): SonarQube rules to check (list of SIds). If the list is empty all rules are checked.
        repo_name (str): Name of the repository under analysis
        analysis_report_relative_path (src): Path where the analysis report should be saved to. (Relative from workspace)
    Returns:
        subprocess.CompletedProcess[str]: Result of running the mining suprocess. If subprocess was succesful then the property "returncode" is 0.
"""
def analyze_file(agent: BaseAgent, file_relative_path: str, rules: list[str], repo_name: str, analysis_report_relative_path: str) -> subprocess.CompletedProcess[str]:

    workspace = agent.config.workspace_path

    # Prepare the paths
    file_relative_path = preprocess_paths(workspace, repo_name, file_relative_path)
    file_path = os.path.join(workspace, repo_name, file_relative_path)

    analysis_report_path = os.path.join(workspace, analysis_report_relative_path)

    

    logger.info(
            f"Running SonarQube analysis on file '{file_path}'. \nThe report will be saved to '{analysis_report_path}'"
        )


    # Create mining command
    cmd_temp = "java -jar {} mine --source {} --stats-output-file {}"

    if len(rules) > 0:
        cmd_temp = cmd_temp + " --rule-keys " + ",".join(rules)
    
    cmd = cmd_temp.format(SORALD_JAR_PATH, file_path, analysis_report_path)

    logger.debug(
            f"The SonarQube analysis on file '{file_path}' is run with the following command: {cmd}"
        )


    result = subprocess.run(
            [cmd],
            capture_output=True,
            encoding="utf8",
            shell=True
        )
    return result
    

def parse_analysis_report(agent: BaseAgent, analysis_report_relative_path: str):
    workspace = agent.config.workspace_path

    analysis_report_path = os.path.join(workspace, analysis_report_relative_path)
    with open(analysis_report_path, "r") as analysis_report_file:
        analysis_report = json.load(analysis_report_file)

    return analysis_report



def analyze_and_parse_report(agent: BaseAgent, file_relative_path: str, rules: list[str], repo_name: str, analysis_report_relative_path: str) -> str:

    result = analyze_file(agent, file_relative_path, rules, repo_name, analysis_report_relative_path)

    if result.returncode == 0:
        logger.info(
            f"Running SonarQube analysis was successful."
        )
        return parse_analysis_report(agent, analysis_report_relative_path)
    else:
        logger.error("Running SonarQube analysis failed with error: " + result.stderr)
        return f"Error: {result.stderr}"



# TODO: test and understand what this does in detail. Is it what we need?

def preprocess_paths(workspace, project_name: str, filepath):
    project_dir = os.path.join(workspace, project_name.lower())
    
    if filepath.endswith(".java"):
        filepath = filepath[:-5]
        filepath = filepath.replace(".", "/")
        filepath += ".java"
    else:
        filepath = filepath.replace(".", "/")
    
    if not os.path.exists(os.path.join(project_dir,filepath)):
        if not os.path.exists(os.path.join(project_dir, "files_index.txt")):
            with open(os.path.join(project_dir, "files_index.txt"), "w") as fit:
                fit.write("\n".join(list_java_files(project_dir)))
            
        with open(os.path.join(project_dir, "files_index.txt")) as fit:
            files_index = [f for f in fit.read().splitlines() if filepath in f]
        
        if len(files_index) == 1:
            filepath = files_index[0]
        elif len(files_index) >= 1:
            raise ValueError("Multiple Candidate Paths. We do not handle this yet!")
        else:
            return "The filepath {} does not exist.".format(filepath)
    return filepath


def list_java_files(main_dir) -> list:
    directory = main_dir
    java_files = []
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith(".java"):
                java_files.append(os.path.join(root.replace("{}/".format(main_dir), ""), file))

    return java_files







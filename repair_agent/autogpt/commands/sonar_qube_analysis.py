import subprocess
import os
import json
from autogpt.logs import logger
from autogpt.config import Config
from autogpt.command_decorator import command

COMMAND_CATEGORY = "sonarQubeAnalysis"
COMMAND_CATEGORY_TITLE = "Run SonarQube analysis"

ALLOWLIST_CONTROL = "allowlist"
DENYLIST_CONTROL = "denylist"


@command(
    "analyze_file",
    "Run SonarQube analysis on a file",
    {
        "filepath": {
            "type": "string",
            "description": "The path to the file to analyze",
            "required": True,
        }
    },
)
def analyze_file_command(filepath: str, config: Config):
    # TODO: will need to get the repo_name from the experiment input
    return analyze_and_parse_report(filepath, None, "codec_4_buggy", "analysis_report.json", config)



def analyze_and_parse_report(file_relative_path: str, rules: list[str], repo_name: str, analysis_report_relative_path: str, config: Config) -> str:

    result = analyze_file(file_relative_path, rules, repo_name, analysis_report_relative_path, config)

    if result.returncode == 0:
        logger.info("",
            f"Running SonarQube analysis was successful."
        )
        return parse_analysis_report(analysis_report_relative_path, config)
    else:
        logger.error("Error", "Running SonarQube analysis failed with error: " + result.stderr)
        return f"Error: {result.stderr}"
    




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
def analyze_file(file_relative_path: str, rules: list[str], repo_name: str, analysis_report_relative_path: str, config: Config) -> subprocess.CompletedProcess[str]:

    workspace = config.workspace_path

    # Prepare the paths
    file_relative_path = preprocess_paths(workspace, repo_name, file_relative_path)
    file_path = os.path.join(workspace, repo_name, file_relative_path)

    analysis_report_path = os.path.join(workspace, analysis_report_relative_path)

    

    logger.info("",
            f"Running SonarQube analysis on file '{file_path}'. \nThe report will be saved to '{analysis_report_path}'"
        )


    # Create mining command
    cmd = ["java", "-jar", config.sorald_jar_path, "mine", "--source", file_path, "--stats-output-file", analysis_report_path]

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
    

def parse_analysis_report(analysis_report_relative_path: str, config: Config):
    workspace = config.workspace_path

    analysis_report_path = os.path.join(workspace, analysis_report_relative_path)
    with open(analysis_report_path, "r") as analysis_report_file:
        analysis_report = json.load(analysis_report_file)

    return analysis_report




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







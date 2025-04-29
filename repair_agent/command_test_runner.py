from autogpt.commands.sonar_qube_analysis import analyze_and_parse_report
from autogpt.commands.repository_operations import checkout_project


# File for testing the implemented commands statically

SORALD_JAR_PATH = "/workspaces/master-thesis-pascal-joos/repair_agent/sorald/sorald.jar"

class Config():
    def __init__(self, workspace_path):
        self.workspace_path = workspace_path
        self.sorald_jar_path = SORALD_JAR_PATH

class AIConfig():
    def __init__(self, warning_repository_URL, warning_repository_commit, warning_file_path, warning_repository_name, warning_rule_key, warning_rule_name):
        self.warning_repository_URL = warning_repository_URL
        self.warning_repository_commit = warning_repository_commit
        self.warning_file_path = warning_file_path
        self.warning_repository_name = warning_repository_name
        self.warning_rule_key = warning_rule_key
        self.warning_rule_name = warning_rule_name

class Agent():
    def __init__(self, warning_repository_URL, warning_repository_commit, warning_file_path, warning_repository_name, warning_rule_key, warning_rule_name):
        self.config = Config("auto_gpt_workspace/")
        self.ai_config = AIConfig(warning_repository_URL, warning_repository_commit, warning_file_path, warning_repository_name, warning_rule_key, warning_rule_name)





if __name__ == "__main__":
    warning_repository_URL = "https://github.com/argparse4j/argparse4j.git"
    warning_repository_commit = "MASTER"
    warning_repository_name = "argparse4j"
    warning_file_path = "main/src/main/java/net/sourceforge/argparse4j/internal/TerminalWidth.java"
    warning_rule_key = "S2142"
    warning_rule_name = "'InterruptedException' should not be ignored"

    agent = Agent(warning_repository_URL, warning_repository_commit, warning_file_path, warning_repository_name, warning_rule_key, warning_rule_name)


    checkout_project(agent)


    # Example parameters for testing
    rules = ["S2142"]
    analysis_report_relative_path = "analysis_report.json"

    # Call the analyze_file function
    report = analyze_and_parse_report(warning_file_path, rules, warning_repository_name, analysis_report_relative_path, agent)
    print(report)
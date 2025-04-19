from autogpt.commands.sonar_qube_analysis import analyze_and_parse_report


# File for testing the implemented commands statically

SORALD_JAR_PATH = "/workspaces/master-thesis-pascal-joos/repair_agent/sorald/sorald.jar"

class Config():
    def __init__(self, workspace_path):
        self.workspace_path = workspace_path
        self.sorald_jar_path = SORALD_JAR_PATH

class Agent():
    def __init__(self,):
        self.config = Config("auto_gpt_workspace/")

agent = Agent()



if __name__ == "__main__":
    # Example parameters for testing
    file_relative_path = "src/java/org/apache/commons/codec/BinaryDecoder.java"
    rules = ["S1120", "S1106"]
    repo_name = "codec_4_buggy"
    analysis_report_relative_path = "analysis_report.json"

    # Call the analyze_file function
    report = analyze_and_parse_report(file_relative_path, rules, repo_name, analysis_report_relative_path, agent)
    print(report)
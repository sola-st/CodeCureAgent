import subprocess
import os

SORALD_JAR_PATH = "/workspaces/master-thesis-pascal-joos/repair_agent/sorald/sorald.jar"


"""
Analyze a file with SonarQube (via Sorald miner). Creates the analysis report json.

Args:
        file_relative_path (str): Path to the file to analyze. (Relative to the repository under analysis)
        rules (list[str]): SonarQube rules to check (list of SIds). If the list is empty all rules are checked.
        workspace (str): Absolute path to the auto_gpt_workspace
        repo_name (str): Name of the repository under analysis
        analysis_report_relative_path (src): Path where the analysis report should be saved to. (Relative from workspace)
    Returns:
        subprocess.CompletedProcess[str]: Result of running the mining suprocess. If subprocess was succesful then the property "returncode" is 0.
"""
def analyze_file(file__relative_path: str, rules: list[str], workspace: str, repo_name: str, analysis_report_relative_path: str) -> subprocess.CompletedProcess[str]:

    #workspace = agent.config.workspace_path

    # Prepare the paths
    project_dir = os.path.join(workspace, repo_name)
    file__relative_path = preprocess_paths(workspace, repo_name, file__relative_path)
    file_full_path = os.path.join(project_dir, file__relative_path)
    
    analysis_report_full_path = os.path.join(workspace, analysis_report_relative_path)


    # Create mining command
    cmd_temp = "java -jar {} mine --source {} --stats-output-file {}"

    if len(rules) > 0:
        cmd_temp = cmd_temp + " --rule-keys " + ",".join(rules)
    
    cmd = cmd_temp.format(SORALD_JAR_PATH, file_full_path, analysis_report_full_path)


    result = subprocess.run(
            [cmd],
            capture_output=True,
            encoding="utf8",
            cwd=workspace,
            shell=True
        )
    return result
    

def parse_analysis_report(workspace: str, analysis_report_path: str):
    pass



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



if __name__ == "__main__":
    # Example parameters for testing
    file_relative_path = "src/java/org/apache/commons/codec/BinaryDecoder.java"
    rules = ["S1120", "S1106"]
    workspace = "/workspaces/master-thesis-pascal-joos/repair_agent/auto_gpt_workspace/"
    repo_name = "codec_4_buggy"
    analysis_report_relative_path = "analysis_report.json"

    # Call the analyze_file function
    result = analyze_file(file_relative_path, rules, workspace, repo_name, analysis_report_relative_path)
    print(result)




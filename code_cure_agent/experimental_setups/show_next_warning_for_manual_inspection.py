import json
import re
import subprocess
import sys
from pathlib import Path
sys.path.append(str(Path(__file__).parent.parent))
import shutil
import csv
import os
import click
from agent_core.commands.write_fix import execute_write_range
from agent_core.commands import repository_operations


import yaml


SORALD_JAR_PATH = "/workspaces/master-thesis-pascal-joos/code_cure_agent/sorald/sorald.jar"


class Config():
    def __init__(self, workspace_path):
        self.workspace_path = workspace_path
        self.sorald_jar_path = SORALD_JAR_PATH


class AIConfig():
    def __init__(self, warning_repository_URL, warning_repository_commit, warning_file_path, warning_repository_name, warning_rule_key, warning_start_line, warning_rule_name, warning_specific_message):
        self.warning_repository_URL = warning_repository_URL
        self.warning_repository_commit = warning_repository_commit
        self.warning_file_path = warning_file_path
        self.warning_rule_key = warning_rule_key
        self.warning_start_line = warning_start_line
        self.warning_rule_name = warning_rule_name
        self.warning_specific_message = warning_specific_message

        self.warning_repository_name = warning_repository_name


class AgentMock():
    def __init__(self, workspace_path, warning_repository_URL, warning_repository_commit, warning_file_path, warning_repository_name, warning_rule_key, warning_start_line, warning_rule_name, warning_specific_message):
        self.config = Config(workspace_path)
        self.ai_config = AIConfig(warning_repository_URL, warning_repository_commit, warning_file_path,
                                  warning_repository_name, warning_rule_key, warning_start_line, warning_rule_name, warning_specific_message)
        self.exps = ["experiment_test"]


@click.command(help="This command collects info on the next uninspected warning in the evaluation results csv for manual inspection. Then the relevant files are opened and displayed for inspection.")
@click.option(
    "--evaluation-results-file",
    type=click.File(),
    default="evaluation_results/evaluation_results.csv",
    help="The csv file with the evaluation results, created via 'write_experiment_results_to_csv_file.py'"
)
@click.option(
    "--id-to-show",
    type=int,
    default=-1,
    help="The ID of the warning to show for manual inspection. If not provided then the next row in the csv that has no manual inspection results yet is used."
)
def show_next_warning_for_manual_inspection(evaluation_results_file: click.File, id_to_show: int) -> None:

    warning_csv_info, line_in_evaluation_results_csv = retrieve_info_of_next_warning_to_show(
        evaluation_results_file, id_to_show)

    if not os.path.exists(os.path.join("evaluation_results", "check_correctness_workspace")):
        os.mkdir(os.path.join("evaluation_results",
                 "check_correctness_workspace"))

    warning_id = warning_csv_info["instanceID"]
    relative_path_inspection_folder = os.path.join("evaluation_results",
                                                   "check_correctness_workspace", f"ID{str(warning_id)}")
    if os.path.exists(relative_path_inspection_folder):
        shutil.rmtree(relative_path_inspection_folder)
    os.mkdir(relative_path_inspection_folder)

    copy_relevant_files_of_warning_experiment_run(
        relative_path_inspection_folder, warning_csv_info)

    checkout_project_unchanged_and_with_changes(
        relative_path_inspection_folder, warning_csv_info)

    open_relevant_files_and_diffs(
        relative_path_inspection_folder, warning_csv_info, evaluation_results_file, line_in_evaluation_results_csv)

    print_brief_info_about_the_warning(warning_csv_info)


def retrieve_info_of_next_warning_to_show(evaluation_results_file: click.File, id_to_show: int) -> tuple[dict, int]:

    warning_csv_info = None
    line_in_evaluation_results_csv = -1

    csv_reader = csv.DictReader(evaluation_results_file,
                                dialect=csv.unix_dialect)

    for i, line in enumerate(csv_reader):
        if id_to_show != -1:
            if line["instanceID"] == str(id_to_show):
                warning_csv_info = line
                # +2 because +1 as the header is not part of csv_reader, and +1 for 1-indexing
                line_in_evaluation_results_csv = i + 2
                break
        else:
            if line["classificationSoundness"] == "" or (line["plausibleFix"] == "True" and line["fixCorrectness"] == ""):
                warning_csv_info = line
                line_in_evaluation_results_csv = i + 2
                break

    if warning_csv_info is None:
        if id_to_show == -1:
            print("No more un-inspected warning was found in the csv file.")
        else:
            print(
                f"Warning with ID {str(id_to_show)} was not found in the csv file.")
        exit(1)

    return warning_csv_info, line_in_evaluation_results_csv


def copy_relevant_files_of_warning_experiment_run(relative_path_inspection_folder: str, warning_csv_info: dict) -> None:
    warning_id = warning_csv_info["instanceID"]
    experiment_number = warning_csv_info["experimentNumber"]
    experiment_folder_rel_path = os.path.join(
        "experimental_setups", f"experiment_{str(experiment_number)}")

    # Copy the classification results file
    try:
        classification_file_name = next(f for f in os.listdir(os.path.join(experiment_folder_rel_path, "classification"))
                                        if os.path.isfile(os.path.join(
                                            experiment_folder_rel_path, "classification", f)) and f.startswith(str(warning_id) + "_"))

        shutil.copy2(os.path.join(experiment_folder_rel_path, "classification", classification_file_name), os.path.join(
            relative_path_inspection_folder, f"ID{str(warning_id)}_classification_result"))
    except StopIteration:
        # happens if no classification and no questions answered
        pass

    if warning_csv_info["classification"] == "TP":
        fix_folder = "fix_tp"
    elif warning_csv_info["classification"] == "FP":
        fix_folder = "fix_fp"
    else:
        fix_folder = None

    if fix_folder is not None:
        # Copy the plausible and implausible patches files
        try:
            plausible_patch_file_name = next(f for f in os.listdir(os.path.join(experiment_folder_rel_path, fix_folder, "plausible_patches"))
                                             if os.path.isfile(os.path.join(
                                                 experiment_folder_rel_path, fix_folder, "plausible_patches", f)) and f.startswith(str(warning_id) + "_"))

            shutil.copy2(os.path.join(experiment_folder_rel_path, fix_folder, "plausible_patches", plausible_patch_file_name), os.path.join(
                relative_path_inspection_folder, f"ID{str(warning_id)}_plausible_patches.json"))
        except StopIteration:
            # happens if no plausible patches
            pass
        try:
            implausible_patch_file_name = next(f for f in os.listdir(os.path.join(experiment_folder_rel_path, fix_folder, "implausible_patches"))
                                               if os.path.isfile(os.path.join(
                                                   experiment_folder_rel_path, fix_folder, "implausible_patches", f)) and f.startswith(str(warning_id) + "_"))

            shutil.copy2(os.path.join(experiment_folder_rel_path, fix_folder, "implausible_patches", implausible_patch_file_name), os.path.join(
                relative_path_inspection_folder, f"ID{str(warning_id)}_implausible_patches.json"))
        except StopIteration:
            # happens if no implausible patches
            pass

    # Copy the task file
    shutil.copy2(os.path.join(experiment_folder_rel_path, "tasks", f"{str(warning_id)}.yaml"), os.path.join(
        relative_path_inspection_folder, f"ID{str(warning_id)}_task_info.yaml"))


def checkout_project_unchanged_and_with_changes(relative_path_inspection_folder: str, warning_csv_info: dict) -> None:
    warning_id = warning_csv_info["instanceID"]
    global task_info
    with open(os.path.join(relative_path_inspection_folder, f"ID{str(warning_id)}_task_info.yaml")) as task_file:
        task_info = yaml.load(task_file, Loader=yaml.FullLoader)

    agent_mock = AgentMock(relative_path_inspection_folder, task_info["warning_repository_URL"], task_info["warning_repository_commit"], task_info["warning_file_path"],
                           task_info["warning_repository_name"], task_info["warning_rule_key"], task_info["warning_start_line"], task_info["warning_rule_name"], task_info["warning_specific_message"])
    repository_operations.checkout_project(agent_mock, overwrite_target_workspace_path=relative_path_inspection_folder,
                                           overwrite_target_folder_name=f"{agent_mock.ai_config.warning_repository_name}_unfixed")

    if warning_csv_info["plausibleFix"] == "True":
        repository_operations.checkout_project(agent_mock, overwrite_target_workspace_path=relative_path_inspection_folder,
                                               overwrite_target_folder_name=f"{agent_mock.ai_config.warning_repository_name}_fixed")

        with open(os.path.join(
                relative_path_inspection_folder, f"ID{str(warning_id)}_plausible_patches.json")) as patch_file:
            patch_file_cont = patch_file.read()

        json_list_pattern = re.compile(
            r'### PLAUSIBLE FIX \(fix no. \d+\)\n(\[\s*(?:.|\n)*?\])\n\n ###CHANGE APPROVER FEEDBACK', re.MULTILINE)

        matches = json_list_pattern.findall(patch_file_cont)
        changes_dicts = json.loads(matches[-1])

        global file_changes
        file_changes = execute_write_range(changes_dicts, agent_mock,
                                           create_analysis_reports=False, overwrite_warning_repository_name=f"{agent_mock.ai_config.warning_repository_name}_fixed")


def open_relevant_files_and_diffs(relative_path_inspection_folder: str, warning_csv_info: dict, evaluation_results_file: click.File, line_in_evaluation_results_csv: int) -> None:
    open_evaluation_results_csv(
        evaluation_results_file, line_in_evaluation_results_csv)

    open_task_info(relative_path_inspection_folder, warning_csv_info)
    try_to_open_classification_result(
        relative_path_inspection_folder, warning_csv_info)
    if warning_csv_info["plausibleFix"] != "True":
        open_unfixed_target_file(
            relative_path_inspection_folder)
    else:
        open_plausible_fix_file(
            relative_path_inspection_folder, warning_csv_info)
        open_diffs_for_changed_files(
            relative_path_inspection_folder)

    # Go back to the first file to open
    open_evaluation_results_csv(
        evaluation_results_file, line_in_evaluation_results_csv)


def open_evaluation_results_csv(evaluation_results_file: click.File, line_in_evaluation_results_csv: int) -> None:
    subprocess.run(
        ["code", "--goto", evaluation_results_file.name +
            f":{str(line_in_evaluation_results_csv)}"],
        capture_output=True,
        encoding="utf8",
        shell=False)


def open_task_info(relative_path_inspection_folder: str, warning_csv_info: dict) -> None:
    warning_id = warning_csv_info["instanceID"]
    subprocess.run(
        ["code", "-r", os.path.join(
            relative_path_inspection_folder, f"ID{str(warning_id)}_task_info.yaml")],
        capture_output=True,
        encoding="utf8",
        shell=False)


def try_to_open_classification_result(relative_path_inspection_folder: str, warning_csv_info: dict) -> None:
    warning_id = warning_csv_info["instanceID"]
    # Might not exist, if no classification and no question answered
    if os.path.exists(os.path.join(relative_path_inspection_folder, f"ID{str(warning_id)}_classification_result")):

        subprocess.run(
            ["code", "-r", os.path.join(
                relative_path_inspection_folder, f"ID{str(warning_id)}_classification_result")],
            capture_output=True,
            encoding="utf8",
            shell=False)


def open_unfixed_target_file(relative_path_inspection_folder: str) -> None:
    warning_repository_name = task_info["warning_repository_name"]
    warning_start_line = task_info["warning_start_line"]
    subprocess.run(
        ["code", "--goto", os.path.join(
            relative_path_inspection_folder, f"{warning_repository_name}_unfixed", task_info["warning_file_path"]) + f":{str(warning_start_line)}"],
        capture_output=True,
        encoding="utf8",
        shell=False)


def open_plausible_fix_file(relative_path_inspection_folder: str, warning_csv_info: dict) -> None:
    warning_id = warning_csv_info["instanceID"]
    subprocess.run(
        ["code", "-r", os.path.join(
            relative_path_inspection_folder, f"ID{str(warning_id)}_plausible_patches.json")],
        capture_output=True,
        encoding="utf8",
        shell=False)


def open_diffs_for_changed_files(relative_path_inspection_folder: str) -> None:
    warning_repository_name = task_info["warning_repository_name"]
    for file_change in file_changes:

        subprocess.run(
            ["code", "--diff", os.path.join(
                relative_path_inspection_folder, f"{warning_repository_name}_unfixed", file_change.file_path), os.path.join(
                relative_path_inspection_folder, f"{warning_repository_name}_fixed", file_change.file_path)],
            capture_output=True,
            encoding="utf8",
            shell=False)


def print_brief_info_about_the_warning(warning_csv_info: dict) -> None:
    print(warning_csv_info)
    warning_id = warning_csv_info["instanceID"]
    rule_key = warning_csv_info["ruleKey"]
    rule_name = warning_csv_info["ruleName"]
    classification = warning_csv_info["classification"]
    plausible_fix = warning_csv_info["plausibleFix"]
    warning_start_line = task_info["warning_start_line"]
    print("----------------------")
    print(f"To inspect: ID {warning_id}")
    print(f"Rule {rule_key}: '{rule_name}' at line {str(warning_start_line)}")
    print("----------------------")
    print(f"Classification: {classification}")
    if classification == "Unclassified":
        print("Task 1: Check the reason why it was not classified.")
        print("----------------------")
    else:
        print("Task 1: Check and denote soundness of classification.")
        print("----------------------")
        print(f"Fixing {classification}: " + "Plausible fix created" if plausible_fix ==
              "True" else "No plausible fix created")
        if plausible_fix == "True":
            print(
                f"Task 2: Check for correctness of {classification} plausible fix.")
        print("----------------------")


if __name__ == "__main__":
    show_next_warning_for_manual_inspection()

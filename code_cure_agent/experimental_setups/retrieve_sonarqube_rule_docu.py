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
from agent_core.commands.sonar_qube_docu import read_sonarqube_docu
from agent_core.utils.agent_utils.agent_mock import AgentMock


import yaml


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
    csv_reader = csv.DictReader(evaluation_results_file,
                                dialect=csv.unix_dialect)

    for i, warning_csv_info in enumerate(csv_reader):
        
        if warning_csv_info["ruleType"] != "Vulnerability" and warning_csv_info["ruleType"] != "Security_Hotspot":
            continue

        if not os.path.exists(os.path.join("evaluation_results", "check_correctness_workspace")):
            os.mkdir(os.path.join("evaluation_results",
                    "check_correctness_workspace"))

        warning_id = warning_csv_info["instanceID"]
        relative_path_inspection_folder = os.path.join("evaluation_results",
                                                    "check_correctness_workspace", f"ID{str(warning_id)}")
        if os.path.exists(relative_path_inspection_folder):
            shutil.rmtree(relative_path_inspection_folder)
        os.mkdir(relative_path_inspection_folder)

        copy_relevant_files_of_warning_experiment_run(relative_path_inspection_folder, warning_csv_info)

        checkout_project_unchanged_and_with_changes(
            relative_path_inspection_folder, warning_csv_info)

def add_sonarqube_rule_docu_file(relative_path_inspection_folder: str, warning_csv_info: dict, agent: AgentMock) -> None:
    
    rule_key = warning_csv_info["ruleKey"]
    sonar_docu = read_sonarqube_docu(rule_key, agent)
    
    with open(os.path.join("../security_rules_docu", f"sonar_rule_{rule_key}_docu.md"), "w") as docu_file:
        docu_file.write(sonar_docu)

    os.remove(os.path.join(relative_path_inspection_folder, f"-1_docu_tool_output_rule_{rule_key}.json"))

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

    # Copy the run summary file if exists
    if os.path.exists(os.path.join(experiment_folder_rel_path, "run_summaries")):
        if os.path.exists(os.path.join(experiment_folder_rel_path, "run_summaries", f"{str(warning_id)}_summary.diff")):
            shutil.copy2(os.path.join(experiment_folder_rel_path, "run_summaries", f"{str(warning_id)}_summary.diff"), os.path.join(
                relative_path_inspection_folder, f"ID{str(warning_id)}_summary.diff"))


def checkout_project_unchanged_and_with_changes(relative_path_inspection_folder: str, warning_csv_info: dict) -> None:
    warning_id = warning_csv_info["instanceID"]
    global task_info
    with open(os.path.join(relative_path_inspection_folder, f"ID{str(warning_id)}_task_info.yaml")) as task_file:
        task_info = yaml.load(task_file, Loader=yaml.FullLoader)

    agent_mock = AgentMock(task_info["warning_repository_URL"], task_info["warning_repository_commit"], task_info["warning_file_path"],
                           task_info["warning_repository_name"], task_info["warning_rule_key"], task_info["warning_start_line"], task_info["warning_rule_name"], task_info["warning_specific_message"], workspace_path=relative_path_inspection_folder)

    add_sonarqube_rule_docu_file(relative_path_inspection_folder, warning_csv_info, agent_mock)





if __name__ == "__main__":
    show_next_warning_for_manual_inspection()

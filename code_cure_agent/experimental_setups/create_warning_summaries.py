

import json
import shutil
import subprocess
import sys
from pathlib import Path

import click
import pandas as pd
import yaml
sys.path.append(str(Path(__file__).parent.parent))
import os
import re
from agent_core.commands.write_fix import execute_write_range
from agent_core.commands import repository_operations
from agent_core.utils.agent_utils.agent_mock import AgentMock


@click.command(help="This command creates summaries for each warning, that shows the warning input, results and a diff of any plausible fix.")
@click.option(
    "--evaluation-results-extended-file",
    type=click.File(),
    default="evaluation_results/evaluation_results_extended.csv",
    help="The csv file with the extended evaluation results, created via 'write_experiment_results_to_csv_file.py' + 'extend_evaluation_results_with_more_stats.py'.",
)
def create_warning_summaries(evaluation_results_extended_file):

    if not os.path.exists(os.path.join("evaluation_results", "check_correctness_workspace")):
        os.mkdir(os.path.join("evaluation_results",
                 "check_correctness_workspace"))

    # Read the evaluation results CSV file
    warnings_df = pd.read_csv(evaluation_results_extended_file)

    for index, warning_item in warnings_df.iterrows():

        warning_id = warning_item["instanceID"]

        experiment_folder_rel_path = os.path.join(
            "experimental_setups", f"experiment_{str(warning_item['experimentNumber'])}")

        task_file_path = os.path.join(
            experiment_folder_rel_path, "tasks", f"{str(warning_id)}.yaml")

        with open(task_file_path) as task_file:
            task_info = yaml.load(task_file, Loader=yaml.FullLoader)

        rule_key = task_info["warning_rule_key"]
        rule_name = task_info["warning_rule_name"]
        specific_message = task_info["warning_specific_message"]
        repository_url = task_info["warning_repository_URL"]
        commit = task_info["warning_repository_commit"]
        file_path = task_info["warning_file_path"]
        warning_start_line = task_info["warning_start_line"]
        classification = warning_item["classification"]
        plausible_fix = warning_item["plausibleFix"]

        implausible_fixes_count = warning_item["implausible_fixes_count"]
        plausible_fixes_count = warning_item["plausible_fixes_count"]
        total_fixes_count = implausible_fixes_count + plausible_fixes_count
        print(f"Summarizing for Warning ID: {warning_id}")

        relative_path_inspection_folder = os.path.join("evaluation_results",
                                                       "check_correctness_workspace", f"ID{str(warning_id)}")

        if os.path.exists(relative_path_inspection_folder):
            shutil.rmtree(relative_path_inspection_folder)
        os.mkdir(relative_path_inspection_folder)

        classification_reason = get_classification_rational(
            warning_id, warning_item)

        diff_of_plausible_fix = create_diff_of_changes(warning_id, warning_item, task_info,
                                                       relative_path_inspection_folder)

        if not os.path.exists(os.path.join(experiment_folder_rel_path, "run_summaries")):
            os.mkdir(os.path.join(experiment_folder_rel_path, "run_summaries"))

        with open(os.path.join(experiment_folder_rel_path, "run_summaries", f"{str(warning_id)}_summary.diff"), "w") as summary_file:
            summary_file.write(f"Warning ID: {warning_id}\n")
            summary_file.write(f"Rule Key: {rule_key}\n")
            summary_file.write(f"Rule Name: {rule_name}\n")
            summary_file.write(f"Specific Message: {specific_message}\n")
            summary_file.write(f"Warning Start Line: {warning_start_line}\n")
            summary_file.write(f"Repository URL: {repository_url}\n")
            summary_file.write(f"Commit: {commit}\n")
            summary_file.write(f"File Path: {file_path}\n")
            summary_file.write("\n")
            summary_file.write(f"Classification (TP/FP): {classification}\n")
            summary_file.write(
                f"Classification Reasoning: {classification_reason}\n")
            summary_file.write(
                f"Plausible Fix (True/False): {plausible_fix}\n")
            summary_file.write(f"Total Fix Attempts: {total_fixes_count}\n")
            summary_file.write("\n")
            if diff_of_plausible_fix is not None:
                summary_file.write(
                    f"\nDiff of Plausible Fix:\n{diff_of_plausible_fix}\n")
            else:
                summary_file.write(f"\nNo plausible fix created.\n")


def get_classification_rational(warning_id, warning_csv_info):

    experiment_folder_rel_path = os.path.join(
        "experimental_setups", f"experiment_{str(warning_csv_info['experimentNumber'])}")
    try:
        classification_file_name = next(f for f in os.listdir(os.path.join(experiment_folder_rel_path, "classification"))
                                        if os.path.isfile(os.path.join(
                                            experiment_folder_rel_path, "classification", f)) and f.startswith(str(warning_id) + "_"))
    except StopIteration:
        print(
            f"ERROR: No classification file found for warning ID {warning_id}")
        return "Unclassified"

    with open(os.path.join(experiment_folder_rel_path, "classification", classification_file_name)) as crf:
        file_cont = crf.read()
    index_reason = file_cont.rfind("Reason:  ")
    relative_path_classification_file = os.path.join(
        experiment_folder_rel_path, "classification", classification_file_name)
    if index_reason == -1:
        print(
            f"ERROR: 'Reason:  ' not found in file {relative_path_classification_file}. This should not happen.")
        return "Unclassified"

    return file_cont[index_reason + 10:].rstrip()


def create_diff_of_changes(warning_id, warning_csv_info, task_info, relative_path_inspection_folder):
    experiment_folder_rel_path = os.path.join(
        "experimental_setups", f"experiment_{str(warning_csv_info['experimentNumber'])}")

    if warning_csv_info["plausibleFix"] == True:
        print("Creating diff of plausible fix...")

        agent_mock = AgentMock(task_info["warning_repository_URL"], task_info["warning_repository_commit"], task_info["warning_file_path"],
                               task_info["warning_repository_name"], task_info["warning_rule_key"], task_info["warning_start_line"], task_info["warning_rule_name"], task_info["warning_specific_message"], workspace_path=relative_path_inspection_folder)
        repository_operations.checkout_project(agent_mock, overwrite_target_workspace_path=relative_path_inspection_folder,
                                               overwrite_target_folder_name=f"{agent_mock.ai_config.warning_repository_name}_unfixed")

        repository_operations.checkout_project(agent_mock, overwrite_target_workspace_path=relative_path_inspection_folder,
                                               overwrite_target_folder_name=f"{agent_mock.ai_config.warning_repository_name}_fixed")

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

            except StopIteration:
                # happens if no plausible patches
                print(
                    "ERROR: No plausible patches file found, but warning is marked as plausible fix in evaluation results csv.")
                return

        with open(os.path.join(experiment_folder_rel_path, fix_folder, "plausible_patches", plausible_patch_file_name)) as patch_file:
            patch_file_cont = patch_file.read()

        json_list_pattern = re.compile(
            r'### PLAUSIBLE FIX \(fix no. \d+\)\n(\[\s*(?:.|\n)*?\])\n\n ###CHANGE APPROVER FEEDBACK', re.MULTILINE)

        matches = json_list_pattern.findall(patch_file_cont)
        changes_dicts = json.loads(matches[-1])

        global file_changes
        file_changes = execute_write_range(changes_dicts, agent_mock,
                                           create_analysis_reports=False, overwrite_warning_repository_name=f"{agent_mock.ai_config.warning_repository_name}_fixed")

        return_diff = subprocess.run(
            [
                "diff",
                os.path.join(relative_path_inspection_folder,
                             f"{agent_mock.ai_config.warning_repository_name}_unfixed"),
                os.path.join(relative_path_inspection_folder,
                             f"{agent_mock.ai_config.warning_repository_name}_fixed"),
                "-u",
                "--ignore-all-space",
                "-r",
                "--exclude=.git"
            ],
            capture_output=True,
            encoding="utf8",
            shell=False
        )

        return return_diff.stdout


if __name__ == "__main__":
    create_warning_summaries()

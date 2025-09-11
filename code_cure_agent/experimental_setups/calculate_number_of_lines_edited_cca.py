

import json

import click
import pandas as pd
import yaml
import os
import re


@click.command(help="This command calculates the number of lines edited by CodeCureAgent over all fix attempts per warning.")
@click.option(
    "--evaluation-results-extended-file",
    type=click.File(),
    default="evaluation_results/evaluation_results_extended.csv",
    help="The csv file with the extended evaluation results, created via 'write_experiment_results_to_csv_file.py' + 'extend_evaluation_results_with_more_stats.py'.",
)
def calculate_number_of_lines_edited(evaluation_results_extended_file):

    if not os.path.exists(os.path.join("evaluation_results", "check_correctness_workspace")):
        os.mkdir(os.path.join("evaluation_results",
                 "check_correctness_workspace"))

    # Read the evaluation results CSV file
    warnings_df = pd.read_csv(evaluation_results_extended_file)

    total_edited_lines_count = 0
    total_fix_format_lines_count = 0

    for index, warning_item in warnings_df.iterrows():

        warning_id = warning_item["instanceID"]

        experiment_folder_rel_path = os.path.join(
            "experimental_setups", f"experiment_{str(warning_item['experimentNumber'])}")

        classification = warning_item["classification"]
        plausible_fix = warning_item["plausibleFix"]

        print("ID: ", warning_id)

        warning_total_fix_format_lines_count = 0
        warning_total_edited_lines_count = 0

        if plausible_fix:
            if classification == "TP":
                fix_folder = "fix_tp"
            elif classification == "FP":
                fix_folder = "fix_fp"
            else:
                fix_folder = None

            try:
                plausible_patch_file_name = next(f for f in os.listdir(os.path.join(experiment_folder_rel_path, fix_folder, "plausible_patches"))
                                                 if os.path.isfile(os.path.join(
                                                     experiment_folder_rel_path, fix_folder, "plausible_patches", f)) and f.startswith(str(warning_id) + "_"))

            except StopIteration:
                print(
                    "ERROR: No plausible patches file found, but warning is marked as plausible fix in evaluation results csv.")
                return

            with open(os.path.join(experiment_folder_rel_path, fix_folder, "plausible_patches", plausible_patch_file_name)) as patch_file:
                patch_file_cont = patch_file.read()

            json_list_pattern = re.compile(
                r'### PLAUSIBLE FIX \(fix no. \d+\)\n(\[\s*(?:.|\n)*?\])\n\n ###CHANGE APPROVER FEEDBACK', re.MULTILINE)

            matches = json_list_pattern.findall(patch_file_cont)
            for match in matches:
                fix_format_lines_count = len(match.splitlines())
                edited_lines_count = 0

                changes_dicts = json.loads(match)

                for change_dict in changes_dicts:
                    if change_dict["insertions"]:
                        for insertion in change_dict["insertions"]:
                            edited_lines_count += len(insertion["new_lines"])

                    if change_dict["deletions"]:
                        edited_lines_count += len(change_dict["deletions"])

                print("Plausible fix fix_format lines: ", fix_format_lines_count)
                print("Plausible fix edited lines: ", edited_lines_count)

                warning_total_fix_format_lines_count += fix_format_lines_count
                warning_total_edited_lines_count += edited_lines_count
        else:
            if classification == "TP":
                fix_folder = "fix_tp"
            elif classification == "FP":
                fix_folder = "fix_fp"
            else:
                fix_folder = None

            try:
                implausible_patch_file_name = next(f for f in os.listdir(os.path.join(experiment_folder_rel_path, fix_folder, "implausible_patches"))
                                                   if os.path.isfile(os.path.join(
                                                       experiment_folder_rel_path, fix_folder, "implausible_patches", f)) and f.startswith(str(warning_id) + "_"))
            except StopIteration:
                print(
                    "WARN: No implausible patches file found.")
                return

            with open(os.path.join(experiment_folder_rel_path, fix_folder, "implausible_patches", implausible_patch_file_name)) as patch_file:
                patch_file_cont = patch_file.read()

            pattern = r"(### IMPLAUSIBLE FIX \(fix no\. \d+\)\n(\[\s*(?:.|\n)*?\])\n\n ###CHANGE APPROVER FEEDBACK)"
            matches = re.findall(pattern, patch_file_cont, re.MULTILINE)
            for match in matches:
                fix_format_lines_count = len(match[1].splitlines())
                edited_lines_count = 0

                changes_dicts = json.loads(match[1])

                for change_dict in changes_dicts:
                    if change_dict["insertions"]:
                        for insertion in change_dict["insertions"]:
                            edited_lines_count += len(insertion["new_lines"])

                    if change_dict["deletions"]:
                        edited_lines_count += len(change_dict["deletions"])

                print("Implausible fix fix_format lines: ",
                      fix_format_lines_count)
                print("Implausible fix edited lines: ",
                      edited_lines_count)

                warning_total_fix_format_lines_count += fix_format_lines_count
                warning_total_edited_lines_count += edited_lines_count

        total_fix_format_lines_count += warning_total_fix_format_lines_count
        total_edited_lines_count += warning_total_edited_lines_count

    print("Total fix_format lines: ", total_fix_format_lines_count)
    print("Total edited lines: ", total_edited_lines_count)
    # Mean
    print("Mean fix_format lines per warning: ",
          total_fix_format_lines_count / len(warnings_df))
    print("Mean edited lines per warning: ",
          total_edited_lines_count / len(warnings_df))


if __name__ == "__main__":
    calculate_number_of_lines_edited()


import json
import click
import re
import sys
import os
import csv

import yaml


@click.command(help="This command adds the results of experiment runs to the evaluation_results.csv")
@click.option(
    "--start-exp",
    type=int,
    default=1,
    help="The start experiment number."
)
@click.option(
    "--end-exp",
    type=int,
    default=sys.maxsize,
    help="The end experiment number."
)
@click.option(
    "--overwrite",
    "-o",
    is_flag=True,
    help="Overwrite the csv results file. Default is appending new Ids to the existing csv."
)
def write_experiment_results_to_csv_file(start_exp: int, end_exp: int, overwrite: bool):

    relevant_experiment_numbers = get_relevant_experiment_numbers(
        start_exp, end_exp)
    print("Experiments looked at: " + str(relevant_experiment_numbers))

    results_csv_path = "evaluation_results/evaluation_results.csv"

    create_results_csv_if_needed(results_csv_path, overwrite)

    for experiment_number in relevant_experiment_numbers:
        experiment_folder = f"experiment_{str(experiment_number)}"
        tasks_to_add = get_tasks_in_experiment_folder_to_add(
            experiment_folder, results_csv_path)
        print(
            f"Experiment {str(experiment_number)} warnings to add: {str(tasks_to_add)}")

        with open(results_csv_path, "a+") as results_csv_file:
            csv_writer = csv.writer(results_csv_file, dialect=csv.unix_dialect)

            for task_to_add in tasks_to_add:
                instanceID = str(task_to_add)
                with open(os.path.join("experimental_setups", experiment_folder, "tasks", f"{str(task_to_add)}.yaml")) as task_file:
                    task_info = yaml.load(task_file, Loader=yaml.FullLoader)
                projectName = task_info["warning_repository_name"]
                ruleKey = task_info["warning_rule_key"]
                ruleName = task_info["warning_rule_name"]
                ruleType = task_info["warning_rule_type"]
                experimentNumber = experiment_number
                classification = retrieve_classification(
                    experiment_folder, task_to_add)
                plausibleFix, fixComplexity = retrieve_fix_info(
                    experiment_folder, task_to_add, classification)

                if classification == "Unclassified":
                    classificationSoundness = "Unclassified"
                else:
                    classificationSoundness = ""
                if not plausibleFix:
                    fixCorrectness = "Unfixed"
                else:
                    fixCorrectness = ""

                if classification == "Unclassified":
                    classificationSoundnessExplanation = "-"
                else:
                    classificationSoundnessExplanation = ""

                if not plausibleFix:
                    fixCorrectnessExplanation = "-"
                else:
                    fixCorrectnessExplanation = ""

                csv_writer.writerow([instanceID, projectName, ruleKey, ruleName, ruleType, experimentNumber, classification, plausibleFix,
                                    fixComplexity, classificationSoundness, fixCorrectness, classificationSoundnessExplanation, fixCorrectnessExplanation])


def get_relevant_experiment_numbers(start_exp: int, end_exp: int) -> list[int]:
    # The experiments_list.txt dictates what experiment_x folders are included
    with open('experimental_setups/experiments_list.txt', 'r') as experiments_list_file:
        all_experiment_folders = experiments_list_file.read().splitlines()

    relevant_experiment_numbers = []
    experiment_pattern = re.compile(r"experiment_(\d+)")
    for experiment_folder in all_experiment_folders:
        match = re.match(experiment_pattern, experiment_folder)
        if match is not None:
            experiment_number = int(match.groups()[0])
            if experiment_number >= start_exp and experiment_number <= end_exp:
                relevant_experiment_numbers.append(experiment_number)

    return relevant_experiment_numbers


def create_results_csv_if_needed(results_csv_path: str, overwrite: bool) -> None:
    if os.path.exists(results_csv_path) and overwrite:
        os.remove(results_csv_path)

    if not os.path.exists(results_csv_path):
        with open(results_csv_path, "w") as results_csv_file:
            csv_writer = csv.writer(results_csv_file, dialect=csv.unix_dialect)
            csv_writer.writerow(["instanceID", "projectName", "ruleKey", "ruleName", "ruleType", "experimentNumber", "classification", "plausibleFix",
                                "fixComplexity", "classificationSoundness", "fixCorrectness", "classificationSoundnessExplanation", "fixCorrectnessExplanation"])


def get_tasks_in_experiment_folder_to_add(experiment_folder: str, results_csv_path: str) -> list[int]:
    tasks_to_add = []

    tasks_folder = os.path.join("experimental_setups",
                                experiment_folder, "tasks")

    task_files = [f for f in os.listdir(
        tasks_folder) if os.path.isfile(os.path.join(tasks_folder, f))]

    for task_file_name in task_files:
        task_id = int(task_file_name.strip(".yaml"))
        if not task_id_in_results_csv(task_id, results_csv_path):
            tasks_to_add.append(task_id)

    tasks_to_add.sort()
    return tasks_to_add


def task_id_in_results_csv(task_id: int, results_csv_path: str) -> bool:
    with open(results_csv_path, "r") as results_csv_file:
        csv_reader = csv.reader(results_csv_file, dialect=csv.unix_dialect)
        for line in csv_reader:
            if line[0] == str(task_id):
                return True

    return False


def retrieve_classification(experiment_folder: str, task_to_add: int) -> str:
    try:
        classification_file_name = next(f for f in os.listdir(os.path.join("experimental_setups", experiment_folder, "classification"))
                                        if os.path.isfile(os.path.join(
                                            "experimental_setups", experiment_folder, "classification", f)) and f.startswith(str(task_to_add) + "_"))
    except StopIteration:
        return "Unclassified"

    with open(os.path.join("experimental_setups", experiment_folder, "classification", classification_file_name)) as crf:
        file_cont = crf.read()
    index_final_verdict = file_cont.rfind("Final Verdict:  ")
    relative_path_classification_file = os.path.join(
        experiment_folder, "classification", classification_file_name)
    if index_final_verdict == -1:
        print(
            f"WARN: 'Final Verdict:  ' not found in file {relative_path_classification_file}. This can happen if a question was answered, but no final verdict given.")
        return "Unclassified"
    final_verdict = file_cont[index_final_verdict +
                              17: index_final_verdict + 19]

    if final_verdict == "TP":
        return "TP"
    elif final_verdict == "FP":
        return "FP"
    else:
        print(
            f"ERROR: Unknown Final Verdict '{final_verdict}' in file {relative_path_classification_file}.")
        return "Unclassified"


def retrieve_fix_info(experiment_folder: str, task_to_add: int, classification: str) -> tuple[bool, str]:
    '''Returns plausibleFix (True/False) and fixComplexity (Single Line, Multi Line, Multi File, Unknown)'''

    if classification == "Unclassified":
        return False, "Unknown"

    if classification == "FP":
        fix_plausible_patches_folder = os.path.join(
            "experimental_setups", experiment_folder, "fix_fp", 'plausible_patches')
    else:
        fix_plausible_patches_folder = os.path.join(
            "experimental_setups", experiment_folder, "fix_tp", 'plausible_patches')

    fix_plausible_patches_files = [f for f in os.listdir(
        fix_plausible_patches_folder) if os.path.isfile(os.path.join(fix_plausible_patches_folder, f))]

    try:
        file_name_task_file_to_find = next(
            file for file in fix_plausible_patches_files if file.startswith(str(task_to_add) + "_"))
    except StopIteration:
        return False, "Unknown"

    with open(os.path.join(fix_plausible_patches_folder, file_name_task_file_to_find)) as patch_file:
        patch_file_cont = patch_file.read()

    json_list_pattern = re.compile(
        r'### PLAUSIBLE FIX \(fix no. \d+\)\n(\[\s*(?:.|\n)*?\])\n\n ###CHANGE APPROVER FEEDBACK', re.MULTILINE)

    matches = json_list_pattern.findall(patch_file_cont)
    last_plausible_fix = json.loads(matches[-1])

    # not necessarily the exact line numbers and also mixed between different files,
    # but only used for checking Single/Multi File, so sufficient for that
    covered_lines = []
    covered_files = []

    for change_dict in last_plausible_fix:
        covered_files.append(change_dict["file_name"])
        if change_dict["insertions"]:
            for insertion in change_dict["insertions"]:
                start_line = insertion["line_number"]
                for i, line in enumerate(insertion["new_lines"]):
                    covered_lines.append(start_line + i)

        if change_dict["deletions"]:
            for deletion in change_dict["deletions"]:
                covered_lines.append(deletion)

    covered_lines_unique = set(covered_lines)
    covered_files_unique = set(covered_files)

    if len(covered_files_unique) > 1:
        return True, "Multi File"
    elif len(covered_lines_unique) > 1:
        return True, "Multi Line"
    elif len(covered_lines_unique) == 1:
        return True, "Single Line"
    else:
        print(
            f"ERROR: 'Classifying the plausible fix as Single Line/Multi Line/Multi File failed. covered_lines_unique: {str(covered_lines_unique)}; covered_files_unique: {str(covered_files_unique)}")
        return True, "Unknown"


if __name__ == "__main__":
    write_experiment_results_to_csv_file()

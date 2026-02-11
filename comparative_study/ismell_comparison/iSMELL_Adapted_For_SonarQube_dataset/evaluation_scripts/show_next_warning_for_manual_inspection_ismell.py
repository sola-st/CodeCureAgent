
import subprocess
import csv
import os
import glob
import click



@click.command(help="This command collects info on the next uninspected warning in the evaluation results csv for manual inspection. Then the relevant files are opened and displayed for inspection.")
@click.option(
    "--evaluation-results-file",
    type=click.File(),
    default="ismell_comparison_results.csv",
    help="The csv file with the evaluation results'"
)
@click.option(
    "--id-to-show",
    type=int,
    default=-1,
    help="The ID of the warning to show for manual inspection. If not provided then the next row in the csv that has no manual inspection results yet is used."
)
def show_next_warning_for_manual_inspection_ismell(evaluation_results_file: click.File, id_to_show: int) -> None:

    warning_csv_info, line_in_evaluation_results_csv = retrieve_info_of_next_warning_to_show(
        evaluation_results_file, id_to_show)


    warning_id = warning_csv_info["instanceID"]
    
    relative_path_inspection_folder = os.path.join("cca_dataset", str(warning_id))
    open_relevant_files_and_diffs(relative_path_inspection_folder)

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



def open_relevant_files_and_diffs(relative_path_inspection_folder: str) -> None:
    
    open_diff(
        relative_path_inspection_folder)
    
    subprocess.run(["code", "ismell_manual_inspection_21.csv"], 
        capture_output=True,
        encoding="utf8",
        shell=False)




def open_diff(relative_path_inspection_folder: str) -> None:
    # Find the Java files using glob
    before_java_files = glob.glob(os.path.join(relative_path_inspection_folder, "before", "*.java"))
    after_java_files = glob.glob(os.path.join(relative_path_inspection_folder, "after", "*.java"))
    
    if not before_java_files or not after_java_files:
        print(f"Warning: Could not find Java files in {relative_path_inspection_folder}")
        return
    
    # Since there's only one Java file in each folder, take the first one
    before_file = before_java_files[0]
    after_file = after_java_files[0]

    subprocess.run(
        ["code", "--diff", before_file, after_file],
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
    print("----------------------")
    print(f"To inspect: ID {warning_id}")
    print(f"Rule {rule_key}: '{rule_name}'")
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
    show_next_warning_for_manual_inspection_ismell()

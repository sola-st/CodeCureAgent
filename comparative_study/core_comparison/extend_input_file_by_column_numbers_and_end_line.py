
import csv
import json
import pandas as pd

WARNINGS_TO_RUN_ON_FILE_NAME = "../../code_cure_agent/experimental_setups/evaluation_dataset/evaluation_dataset_filled_up_to_1000_input_file.csv"
TARGET_CSV_FILE_PATH = "evaluation_dataset_filled_up_to_1000_input_file_extended_with_column_numbers.csv"


def extend_input_file_by_column_numbers_and_end_line():

    warnings_to_run_on_df = pd.read_csv(WARNINGS_TO_RUN_ON_FILE_NAME)

    for index, warning_item in warnings_to_run_on_df.iterrows():
        print(index)

        warning_object = extract_warning_from_mining_report(
            warning_item)
        if warning_object is not None:
            warnings_to_run_on_df.at[index,
                                     "startColumn"] = warning_object["startColumn"]
            warnings_to_run_on_df.at[index,
                                     "endLine"] = warning_object["endLine"]
            warnings_to_run_on_df.at[index,
                                     "endColumn"] = warning_object["endColumn"]

    warnings_to_run_on_df["startColumn"] = warnings_to_run_on_df["startColumn"].fillna(
        -1).astype(int)
    warnings_to_run_on_df["endLine"] = warnings_to_run_on_df["endLine"].fillna(
        -1).astype(int)
    warnings_to_run_on_df["endColumn"] = warnings_to_run_on_df["endColumn"].fillna(
        -1).astype(int)

    warnings_to_run_on_df.to_csv(
        TARGET_CSV_FILE_PATH, encoding="utf-8", index=False, header=True, quoting=csv.QUOTE_ALL, quotechar='"',
        doublequote=True)


def extract_warning_from_mining_report(warning_item: pd.Series) -> dict | None:
    mining_report_file_name = "../../code_cure_agent/experimental_setups/evaluation_dataset/mining_results/evaluation_dataset_mining_result.json"
    with open(mining_report_file_name) as mining_report_file:
        json_report = json.load(mining_report_file)
    mined_repositories = json_report["minedRepositories"]
    repo_in_json = next(
        mined_repository for mined_repository in mined_repositories if mined_repository["repositoryURL"] == warning_item["repositoryURL"] and mined_repository["commit"] == warning_item["commit"])
    print(repo_in_json["repositoryURL"])

    rule_in_json = next(
        rule for rule in repo_in_json["minedRules"] if rule["ruleKey"] == warning_item["ruleKey"])

    print(rule_in_json["ruleName"])

    matching_warning_locations_in_json = [
        warning_location for warning_location in rule_in_json["warningLocations"] if warning_location["filePath"] == warning_item["filePath"] and warning_location["startLine"] == warning_item["startLine"] and warning_location["specificMessage"] == warning_item["specificMessage"]]

    print(len(matching_warning_locations_in_json))

    warning_ID = warning_item["instanceID"]
    if len(matching_warning_locations_in_json) > 1:
        print(
            f"WARNING: Found more than one matching warning for ID {warning_ID}. Defaulting to choosing the first warning.")
    if len(matching_warning_locations_in_json) == 0:
        print(
            f"ERROR: Found no matching warning for ID {warning_ID}. This should not happen")
        return None

    return matching_warning_locations_in_json[0]


if __name__ == "__main__":
    extend_input_file_by_column_numbers_and_end_line()

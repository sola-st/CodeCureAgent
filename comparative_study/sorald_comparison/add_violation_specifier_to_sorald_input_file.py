
import csv
import json
import pandas as pd


def add_violation_specifier_to_sorald_input_file():
    warnings_to_run_on_file_name = "dataset_sorald_supported_instances_1000_instances_dataset.csv"
    target_csv_file_path = "dataset_sorald_supported_instances_1000_instances_dataset_with_violation_specifier.csv"

    warnings_to_run_on_df = pd.read_csv(warnings_to_run_on_file_name)

    for index, warning_item in warnings_to_run_on_df.iterrows():

        violation_specifier = extract_violation_specifier_from_mining_report(
            warning_item)
        warnings_to_run_on_df.at[index,
                                 "violationSpecifier"] = violation_specifier
        print(violation_specifier)
    warnings_to_run_on_df.to_csv(
        target_csv_file_path, encoding="utf-8", index=False, header=True, quoting=csv.QUOTE_ALL, quotechar='"',
        doublequote=True)


def extract_violation_specifier_from_mining_report(warning_item: pd.Series) -> str:
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
        return ""

    return matching_warning_locations_in_json[0]["violationSpecifier"]


if __name__ == "__main__":
    add_violation_specifier_to_sorald_input_file()



import click
import json
import csv
import pandas as pd


@click.command()
@click.argument(
    "json-mining-report-file",
    type=click.File()
)
@click.option(
    "--target-csv-file-path",
    "-t",
    default="./new_experiment_input_file.csv",
    help="Path where the csv file should be written to."
)
@click.option(
    "--rule-violations-mode",
    type=click.Choice(["all", "single"]),
    required=True,
    help="Two modes:\n\n'all': All rule violations of a specific rule in one file in one project are aggregated into one input file line.\n\n'single': Each single rule violation is a separate line in the input file (and therefore a separate run of the agent)."
)
def prepare_experiment_input_file(json_mining_report_file, target_csv_file_path, rule_violations_mode):
    """Creates a csv-file from a json-SonarQube mining result (mined via sorald).\n
    The csv file can be used as input to the Static Warning Repair Agent, where each line triggers a separate agent run.\n 
    Specify the mining report's path via argument JSON_MINING_REPORT_FILE. """

    json_report = json.load(json_mining_report_file)

    with open(target_csv_file_path, "w") as target_csv_file:
        csv_writer = csv.writer(target_csv_file, dialect=csv.unix_dialect)
        if rule_violations_mode == "all":
            csv_writer.writerow(["instanceID", "repositoryURL", "commit", "targetJavaVersion", "ruleKey",
                                "filePath", "ruleName", "ruleType", "ruleViolationCount"])
        else:
            csv_writer.writerow(["instanceID", "repositoryURL", "commit", "targetJavaVersion", "ruleKey", "filePath",
                                "startLine", "ruleName", "specificMessage", "ruleType"])

        mined_repositories = json_report["minedRepositories"]

        instance_ID = 1

        for mined_repository in mined_repositories:
            commit = mined_repository["commit"]
            repository_URL = mined_repository["repositoryURL"]
            target_java_version = mined_repository["targetJavaVersion"]
            mined_rules = mined_repository["minedRules"]

            for mined_rule in mined_rules:

                if rule_violations_mode == "all":
                    # Per rule group by filePath to only have one line per rule and file.
                    # Additionally count how many instances of a rule are in a file
                    warning_locations_df = pd.DataFrame(
                        mined_rule["warningLocations"])
                    warning_locations_df = warning_locations_df.groupby("filePath")[
                        "filePath"].count()
                    warning_locations_dict = warning_locations_df.to_dict()

                    for file_path in warning_locations_dict:
                        csv_writer.writerow([instance_ID, repository_URL, commit, target_java_version, mined_rule["ruleKey"], file_path,
                                            mined_rule["ruleName"], mined_rule["ruleType"], warning_locations_dict[file_path]])
                        instance_ID += 1
                else:
                    # Have a separate line for each rule violation (warningLocation)
                    for rule_violation in mined_rule["warningLocations"]:
                        csv_writer.writerow([instance_ID, repository_URL, commit, target_java_version, mined_rule["ruleKey"], rule_violation["filePath"],
                                            rule_violation["startLine"], mined_rule["ruleName"].replace('\n', '\\n'), rule_violation["specificMessage"].replace('\n', '\\n'), mined_rule["ruleType"]])
                        instance_ID += 1


if __name__ == "__main__":
    prepare_experiment_input_file()

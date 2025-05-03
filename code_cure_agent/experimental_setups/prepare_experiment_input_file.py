


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
def prepare_experiment_input_file(json_mining_report_file, target_csv_file_path):
    """Creates a csv-file from a json-SonarQube mining result (mined via sorald).\n
    The csv file can be used as input to the Static Warning Repair Agent, where each line triggers a separate agent run.\n 
    Specify the mining report's path via argument JSON_MINING_REPORT_FILE. """
    
    
    json_report = json.load(json_mining_report_file)
    

    with open(target_csv_file_path, "w") as target_csv_file:
        csv_writer = csv.writer(target_csv_file, dialect=csv.unix_dialect)
        csv_writer.writerow(["repositoryURL", "commit", "ruleKey", "filePath", "ruleName", "ruleType", "ruleViolationCount"])


        mined_repositories = json_report["minedRepositories"]
        
        for mined_repository in mined_repositories:
            commit = mined_repository["commit"]
            repository_URL = mined_repository["repositoryURL"]
            mined_rules  = mined_repository["minedRules"]

            for mined_rule in mined_rules:
                # Per rule group by filePath to only have one line per rule and file. 
                # Additionally count how many instances of a rule are in a file
                warning_locations_df = pd.DataFrame(mined_rule["warningLocations"])
                warning_locations_df = warning_locations_df.groupby("filePath")["filePath"].count()
                warning_locations_dict = warning_locations_df.to_dict()

                for file_path in warning_locations_dict:
                    csv_writer.writerow([repository_URL, commit, mined_rule["ruleKey"], file_path, mined_rule["ruleName"], mined_rule["ruleType"], warning_locations_dict[file_path]])


    


if __name__ == "__main__":
    prepare_experiment_input_file()

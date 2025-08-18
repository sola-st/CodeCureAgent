import csv
import json
import os
import shutil
import pandas as pd
import re

import sys
from pathlib import Path
sys.path.append(str(Path(__file__).parent.parent.parent / "code_cure_agent"))
os.chdir(str(Path(__file__).parent.parent.parent / "code_cure_agent"))
import subprocess

from agent_core.utils.agent_utils.agent_mock import AgentMock
from agent_core.commands.repository_operations import checkout_project


WARNINGS_TO_RUN_ON_FILE_NAME = "../comparative_study/core_comparison/evaluation_dataset_filled_up_to_1000_input_file_extended_with_column_numbers.csv"

CORE_COMPARISON_DATASET_FILE_PATH = "../comparative_study/core_comparison/COREMSRI/dataset/cca_dataset"


def create_core_comparison_dataset():

    if os.path.exists(CORE_COMPARISON_DATASET_FILE_PATH):
        print("ERROR: Target dataset folder already exists. If you are sure you want to overwrite it then delete the folder first.")
        exit(1)
    os.mkdir(CORE_COMPARISON_DATASET_FILE_PATH)

    with open(os.path.join(CORE_COMPARISON_DATASET_FILE_PATH, "covered_rules.txt"), "w"):
        pass

    warnings_to_run_on_df = pd.read_csv(WARNINGS_TO_RUN_ON_FILE_NAME)

    for index, warning_item in warnings_to_run_on_df.iterrows():
        print(index)

        target_rule_folder = os.path.join(
            CORE_COMPARISON_DATASET_FILE_PATH, warning_item["ruleKey"])
        instance_overview_csv_file_path = os.path.join(
            target_rule_folder, "results_" + str(warning_item["ruleKey"]) + ".csv")

        warning_to_add_df = pd.DataFrame([[
            warning_item["ruleKey"],
            str(warning_item["ruleName"]),
            "recommendation",
            "",
            "/" + str(warning_item["instanceID"]) + ".java",
            warning_item["startLine"],
            warning_item["startColumn"],
            warning_item["endLine"],
            warning_item["endColumn"]
        ]])

        if not os.path.exists(target_rule_folder):
            with open(os.path.join(CORE_COMPARISON_DATASET_FILE_PATH, "covered_rules.txt"), "a+") as covered_rules_file:
                covered_rules_file.write(" " + str(warning_item["ruleKey"]))

            os.mkdir(target_rule_folder)
            warning_to_add_df.to_csv(
                instance_overview_csv_file_path, index=False, header=False)
        else:
            instance_overview_csv_df = pd.read_csv(
                instance_overview_csv_file_path, header=None)
            # Append the current warning_item as a new row to the dataframe
            instance_overview_csv_df = pd.concat(
                [instance_overview_csv_df, warning_to_add_df],
                ignore_index=True
            )
            # Save the updated dataframe back to the CSV without headers
            instance_overview_csv_df.to_csv(
                instance_overview_csv_file_path, index=False, header=False)

        # Add the target Java file
        temp_folder = "../comparative_study/core_comparison/temp"

        repository_name = warning_item["repositoryURL"].split(
            "/")[-1].removesuffix(".git")

        if not os.path.exists(os.path.join(temp_folder, repository_name, warning_item["filePath"])):

            agent = AgentMock(warning_item["repositoryURL"], warning_item["commit"], warning_item["filePath"],
                              repository_name, "irrelevant", -1, "irrelevant", "irrelevant", workspace_path=temp_folder)

            checkout_project(
                agent, overwrite_target_workspace_path=temp_folder)

        shutil.copyfile(os.path.join(temp_folder, repository_name, warning_item["filePath"]), os.path.join(
            target_rule_folder, str(warning_item["instanceID"]) + ".java"))

        if index == 9:
            break


if __name__ == "__main__":
    create_core_comparison_dataset()

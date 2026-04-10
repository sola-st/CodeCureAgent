
import os
import shutil
import pandas as pd

import sys
from pathlib import Path
sys.path.append(str(Path(__file__).parent.parent.parent.parent.parent / "code_cure_agent"))
os.chdir(str(Path(__file__).parent.parent.parent.parent.parent / "code_cure_agent"))
import subprocess

from agent_core.utils.agent_utils.agent_mock import AgentMock
from agent_core.commands.repository_operations import checkout_project


WARNINGS_TO_RUN_ON_FILE_NAME = "../comparative_study/ismell_comparison/iSMELL_Adapted_For_SonarQube_dataset/evaluation_dataset_filled_up_to_1000_input_file.csv"

ISMELL_COMPARISON_DATASET_FILE_PATH = "../comparative_study/ismell_comparison/iSMELL_Adapted_For_SonarQube_dataset/cca_dataset"


def create_ismell_comparison_dataset():

    if os.path.exists(ISMELL_COMPARISON_DATASET_FILE_PATH):
        print("ERROR: Target dataset folder already exists. If you are sure you want to overwrite it then delete the folder first.")
        exit(1)
    os.mkdir(ISMELL_COMPARISON_DATASET_FILE_PATH)

    

    warnings_to_run_on_df = pd.read_csv(WARNINGS_TO_RUN_ON_FILE_NAME)

    for index, warning_item in warnings_to_run_on_df.iterrows():
        print(index)

        target_rule_folder = os.path.join(
            ISMELL_COMPARISON_DATASET_FILE_PATH, str(warning_item["instanceID"]))



        # Add the target Java file
        temp_folder = "../comparative_study/ismell_comparison/iSMELL_Adapted_For_SonarQube_dataset/temp"

        repository_name = warning_item["repositoryURL"].split(
            "/")[-1].removesuffix(".git")

        if not os.path.exists(os.path.join(temp_folder, repository_name, warning_item["filePath"])):

            agent = AgentMock(warning_item["repositoryURL"], warning_item["commit"], warning_item["filePath"],
                              repository_name, "irrelevant", -1, "irrelevant", "irrelevant", workspace_path=temp_folder)

            checkout_project(
                agent, overwrite_target_workspace_path=temp_folder)

        os.makedirs(os.path.join(target_rule_folder, "before"), exist_ok=True)

        shutil.copyfile(os.path.join(temp_folder, repository_name, warning_item["filePath"]), os.path.join(
            target_rule_folder, "before", warning_item["filePath"].split('/')[-1]))


if __name__ == "__main__":
    create_ismell_comparison_dataset()

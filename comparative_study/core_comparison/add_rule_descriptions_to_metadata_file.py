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
from agent_core.commands.sonar_qube_docu import read_sonarqube_docu

QUALITY_PROFILE_RULE_KEYS_FILE = "sonarqube_quality_profile/quality_profile_rule_keys.txt"

TARGET_METADATA_FILE = "metadata.json"


def get_rule_keys_from_file(filepath):
    with open(filepath, "r") as f:
        content = f.read()
        rule_keys = [rk.strip() for rk in content.split(",") if rk.strip()]
    return rule_keys


def add_rule_descriptions_to_metadata_file():
    rule_keys = get_rule_keys_from_file(QUALITY_PROFILE_RULE_KEYS_FILE)

    agent = AgentMock("irrelevant", "irrelevant", "irrelevant",
                      "irrelevant", "irrelevant", -1, "irrelevant", "irrelevant")
    if not os.path.exists("experimental_setups/experiment_test"):
        os.mkdir("experimental_setups/experiment_test")
    if not os.path.exists("experimental_setups/experiment_test/fix_tp"):
        os.mkdir("experimental_setups/experiment_test/fix_tp")
    if not os.path.exists("experimental_setups/experiment_test/fix_tp/docu_tool_outputs"):
        os.mkdir("experimental_setups/experiment_test/fix_tp/docu_tool_outputs")

    rule_meta_json = {}

    for rule_key in rule_keys:
        rule_description_with_title = read_sonarqube_docu(rule_key, agent)
        description_match = re.match("\*\*(?P<rule_name>.+)\*\*(?P<rule_description>.*)",
                                     rule_description_with_title, re.DOTALL)
        rule_name = description_match.group("rule_name")
        rule_description = description_match.group("rule_description").strip()

        print(rule_name)
        print(rule_description)

        rule_meta_json[rule_key] = {
            "name": rule_name,
            "desc": rule_description,
            "folder_name": rule_key,
            "contextual": False,
            "ruleset": "SonarQube"
        }

    with open(TARGET_METADATA_FILE, "w") as meta_file:
        json.dump(rule_meta_json, meta_file, indent=2)


if __name__ == "__main__":
    add_rule_descriptions_to_metadata_file()


import csv
import json
import os
import shutil
import pandas as pd

import sys
from pathlib import Path
sys.path.append(str(Path(__file__).parent.parent.parent / "code_cure_agent"))
os.chdir(str(Path(__file__).parent.parent.parent / "code_cure_agent"))
import subprocess

from agent_core.utils.agent_utils.agent_mock import AgentMock
from agent_core.commands import repository_operations

WARNINGS_TO_RUN_ON_FILE_PATH = "../comparative_study/sorald_comparison/dataset_sorald_supported_instances_1000_instances_dataset_with_violation_specifier.csv"
CCA_RELEVANT_RESULTS_FILE_PATH = "../comparative_study/sorald_comparison/cca_relevant_evaluation_results.csv"

TARGET_CSV_FILE_PATH = "../comparative_study/sorald_comparison/sorald_comparison_results.csv"

CCA_WORKSPACE = "cca_workspace"

REPAIR_OUTPUT_FOLDER = "../comparative_study/sorald_comparison/sorald_run_outputs/repair_output"
MINING_OUTPUT_FOLDER = "../comparative_study/sorald_comparison/sorald_run_outputs/mining_output"

PROJECTS_BEFORE_AFTER_FOLDER = "../comparative_study/sorald_comparison/sorald_run_outputs/projects_before_after"


def run_sorald_comparison():

    warnings_to_run_on_df = pd.read_csv(WARNINGS_TO_RUN_ON_FILE_PATH)

    cca_relevant_results_df = pd.read_csv(CCA_RELEVANT_RESULTS_FILE_PATH)

    if not os.path.exists(TARGET_CSV_FILE_PATH):
        with open(TARGET_CSV_FILE_PATH, "w") as results_csv_file:
            csv_writer = csv.writer(
                results_csv_file, dialect=csv.unix_dialect)
            csv_writer.writerow(["instanceID", "projectName", "ruleKey", "ruleName", "ruleType", "experimentNumber", "classification", "plausibleFix",
                                "fixComplexity", "classificationSoundness", "fixCorrectness", "classificationSoundnessExplanation", "fixCorrectnessExplanation", "soraldFixCreated", "soraldFixingTimeInMs", "soraldBuildSuccessful", "soraldNumberOfTargetWarningsRemoved", "soraldNoNewWarningIntroduced", "soraldKeysOfNewlyIntroducedWarnings", "soraldTestSuccessful"])

    with open(TARGET_CSV_FILE_PATH, "a+") as results_csv_file:
        csv_writer = csv.writer(
            results_csv_file, dialect=csv.unix_dialect)

        for index, warning_item in warnings_to_run_on_df.iterrows():
            print("Running Sorald Evaluation on ID " +
                  str(warning_item["instanceID"]))

            repository_name = warning_item["repositoryURL"].split(
                "/")[-1].removesuffix(".git")
            repository_path = os.path.join(CCA_WORKSPACE, repository_name)
            agent = AgentMock(warning_item["repositoryURL"], warning_item["commit"], warning_item["filePath"], repository_name, warning_item["ruleKey"],
                              warning_item["startLine"], warning_item["ruleName"], warning_item["specificMessage"], workspace_path=CCA_WORKSPACE, warning_ID=warning_item["instanceID"])

            repository_operations.checkout_project(agent)

            # mine warnings before (only for the file with the warning to fix, else it takes too long)
            cmd = ["java", "-jar", agent.config.sorald_jar_path, "mine", "--source", os.path.join(CCA_WORKSPACE, agent.ai_config.warning_repository_name, agent.ai_config.warning_file_path), "--stats-output-file",
                   os.path.join(MINING_OUTPUT_FOLDER, str(agent.ai_config.warning_ID) + "_mining_out_before.json"), "--rule-parameters", "sonarqube_quality_profile/quality_profile_rule_parameters.json", "--target-java-version", agent.ai_config.warning_repository_target_java_version]

            cmd.append("--rule-keys")
            cmd.append(",".join(agent.sonar_qube_rules_in_active_profile))

            result = subprocess.run(
                cmd,
                capture_output=True,
                encoding="utf8",
                shell=False
            )

            # run sorald command
            violation_specifier = warning_item["violationSpecifier"]
            target_java_version = warning_item["targetJavaVersion"]

            stats_file_path = os.path.join(
                REPAIR_OUTPUT_FOLDER, str(agent.ai_config.warning_ID) + "_repair_out.json")

            result = subprocess.run(
                f"java -jar {agent.config.sorald_jar_path} repair --source {repository_path} --stats-output-file {stats_file_path} --violation-specs {violation_specifier} --target-java-version {target_java_version}",
                capture_output=True,
                encoding="utf8",
                shell=True
            )

            print(result.stdout)
            print(result.stderr)

            # mine warnings after
            cmd = ["java", "-jar", agent.config.sorald_jar_path, "mine", "--source", os.path.join(CCA_WORKSPACE, agent.ai_config.warning_repository_name, agent.ai_config.warning_file_path), "--stats-output-file",
                   os.path.join(MINING_OUTPUT_FOLDER, str(agent.ai_config.warning_ID) + "_mining_out_after.json"), "--rule-parameters", "sonarqube_quality_profile/quality_profile_rule_parameters.json", "--target-java-version", agent.ai_config.warning_repository_target_java_version]

            cmd.append("--rule-keys")
            cmd.append(",".join(agent.sonar_qube_rules_in_active_profile))

            result = subprocess.run(
                cmd,
                capture_output=True,
                encoding="utf8",
                shell=False
            )

            with open(stats_file_path) as repair_out_file:
                repair_out_json = json.load(repair_out_file)

            if len(repair_out_json["repairs"]) > 0 and repair_out_json["repairs"][0]["nbPerformedRepairs"] == 1:
                sorald_fix_created = "True"
            else:
                sorald_fix_created = "False"

            time_in_ms = repair_out_json["totalTimeMs"]

            # save changed files (and diff) for inspection

            single_before_after_file_path = os.path.join(
                PROJECTS_BEFORE_AFTER_FOLDER, f"{agent.ai_config.warning_ID}_before_after")

            if os.path.exists(single_before_after_file_path):
                shutil.rmtree(single_before_after_file_path)
            os.mkdir(single_before_after_file_path)

            # Add unchanged version of project
            repository_operations.checkout_project(agent, overwrite_target_workspace_path=single_before_after_file_path,
                                                   overwrite_target_folder_name=agent.ai_config.warning_repository_name + "_before")
            # Add changed version of project
            shutil.copytree(os.path.join(CCA_WORKSPACE, agent.ai_config.warning_repository_name), os.path.join(
                single_before_after_file_path, agent.ai_config.warning_repository_name + "_after"))

            # Save diff over all files
            before_project_file_path = os.path.join(
                single_before_after_file_path, agent.ai_config.warning_repository_name + "_before")
            after_project_file_path = os.path.join(
                single_before_after_file_path, agent.ai_config.warning_repository_name + "_after")
            result = subprocess.run(
                f"diff {before_project_file_path} {after_project_file_path} -r --exclude .git",
                capture_output=True,
                encoding="utf8",
                shell=True
            )
            with open(os.path.join(single_before_after_file_path, str(agent.ai_config.warning_ID) + "_diff.txt"), "w") as diff_file:
                diff_file.write(result.stdout)

            # Open diff of all files
            subprocess.run(
                ["code", "-r", os.path.join(single_before_after_file_path, str(
                    agent.ai_config.warning_ID) + "_diff.txt")],
                capture_output=True,
                encoding="utf8",
                shell=False)

            # Add unchanged version of only the target file
            if os.path.exists(os.path.join(single_before_after_file_path, agent.ai_config.warning_repository_name + "_target_file_before")):
                shutil.rmtree(os.path.join(single_before_after_file_path,
                              agent.ai_config.warning_repository_name + "_target_file_before"))
            os.mkdir(os.path.join(single_before_after_file_path,
                     agent.ai_config.warning_repository_name + "_target_file_before"))

            shutil.copyfile(os.path.join(single_before_after_file_path, agent.ai_config.warning_repository_name + "_before", agent.ai_config.warning_file_path),
                            os.path.join(single_before_after_file_path, agent.ai_config.warning_repository_name + "_target_file_before", agent.ai_config.warning_file_name))

            # Add changed version of only the target file
            if os.path.exists(os.path.join(single_before_after_file_path, agent.ai_config.warning_repository_name + "_target_file_after")):
                shutil.rmtree(os.path.join(single_before_after_file_path,
                              agent.ai_config.warning_repository_name + "_target_file_after"))
            os.mkdir(os.path.join(single_before_after_file_path,
                     agent.ai_config.warning_repository_name + "_target_file_after"))
            shutil.copyfile(os.path.join(single_before_after_file_path, agent.ai_config.warning_repository_name + "_after", agent.ai_config.warning_file_path),
                            os.path.join(single_before_after_file_path, agent.ai_config.warning_repository_name + "_target_file_after", agent.ai_config.warning_file_name))

            # Open vscode diff view of file with warning removed
            subprocess.run(
                ["code", "--diff", os.path.join(single_before_after_file_path, agent.ai_config.warning_repository_name + "_target_file_before", agent.ai_config.warning_file_name), os.path.join(
                    single_before_after_file_path, agent.ai_config.warning_repository_name + "_target_file_after", agent.ai_config.warning_file_name)],
                capture_output=True,
                encoding="utf8",
                shell=False)

            # Build project
            error = None
            build_successful = False
            try:
                repository_operations.build_project(
                    agent, time_monitoring=False)

                build_successful = True

            except repository_operations.BuildError as be:
                error = be.stdout
            except subprocess.TimeoutExpired as te:
                error = f"TimeoutExpired exception: Build timed out after {te.timeout / 60} minutes. \nThe stdout was the following: \n\n{te.stdout.decode('utf-8')}"
            except Exception as e:
                error = str(e)

            if error is not None:
                with open(os.path.join("../comparative_study/sorald_comparison/sorald_run_outputs/build_errors", f"{agent.ai_config.warning_ID}_error.log"), "w") as log:
                    log.write(str(error))

            # Check if warning removed (number of target warnings reduced)

            # number of warnings of the target ruleKey
            if len(repair_out_json["repairs"]) > 0:
                number_of_removed_target_violations = repair_out_json["repairs"][0][
                    "nbViolationsBefore"] - repair_out_json["repairs"][0]["nbViolationsAfter"]
            else:
                number_of_removed_target_violations = 0

            # check no other warnings introduced
            no_new_warning_introduced = True
            rule_keys_of_newly_introduced_warnings = []
            with open(os.path.join(MINING_OUTPUT_FOLDER, str(agent.ai_config.warning_ID) + "_mining_out_before.json")) as out_before_file:
                mining_out_before = json.load(out_before_file)

            with open(os.path.join(MINING_OUTPUT_FOLDER, str(agent.ai_config.warning_ID) + "_mining_out_after.json")) as out_after_file:
                mining_out_after = json.load(out_after_file)
            mined_rules_before = mining_out_before["minedRules"]
            mined_rules_after = mining_out_after["minedRules"]

            for mined_rule_after in mined_rules_after:
                if mined_rule_after["ruleKey"] != agent.ai_config.warning_rule_key:
                    rule_key_in_after_matched_to_before = [
                        rule_before for rule_before in mined_rules_before if rule_before["ruleKey"] == mined_rule_after["ruleKey"]]
                    # Check if the rule present in the after report can be found in the before report too (if not it is new)
                    if len(rule_key_in_after_matched_to_before) == 0:
                        no_new_warning_introduced = False
                        rule_keys_of_newly_introduced_warnings.append(
                            mined_rule_after["ruleKey"])
                    # Check if the number of warnings of the rule is the same or less than before (if not then there are new warnings)
                    elif len(rule_key_in_after_matched_to_before[0]["warningLocations"]) < len(mined_rule_after["warningLocations"]):
                        no_new_warning_introduced = False
                        rule_keys_of_newly_introduced_warnings.append(
                            mined_rule_after["ruleKey"])

            # Test project
            error = None
            try:
                test_result = repository_operations.run_tests(
                    agent, time_monitoring=False)

            except repository_operations.BuildError as be:
                error = be.stdout
            except subprocess.TimeoutExpired as te:
                error = f"TimeoutExpired exception: Build timed out after {te.timeout / 60} minutes. \nThe stdout was the following: \n\n{te.stdout.decode('utf-8')}"
            except Exception as e:
                error = str(e)

            if test_result.returncode == 0:
                test_successful = True
            else:
                test_successful = False
                error = test_result.stdout + "\n\n" + test_result.stderr

            if error is not None:
                with open(os.path.join("../comparative_study/sorald_comparison/sorald_run_outputs/test_errors", f"{agent.ai_config.warning_ID}_error.log"), "w") as log:
                    log.write(str(error))

            # Save results in csv (including the cca results)
            csv_writer.writerow(
                list(
                    cca_relevant_results_df.iloc[index]) + [sorald_fix_created, time_in_ms, build_successful, number_of_removed_target_violations, no_new_warning_introduced, ",".join(rule_keys_of_newly_introduced_warnings), test_successful]
            )


if __name__ == "__main__":
    run_sorald_comparison()


import csv
import json
import os
import re
import shutil
import time
import pandas as pd

import sys
from pathlib import Path
sys.path.append(str(Path(__file__).parent.parent.parent / "code_cure_agent"))
os.chdir(str(Path(__file__).parent.parent.parent / "code_cure_agent"))
import subprocess

from agent_core.utils.agent_utils.agent_mock import AgentMock
from agent_core.commands import repository_operations

WARNINGS_TO_RUN_ON_FILE_PATH = "../comparative_study/core_comparison/evaluation_dataset_first_291_input_file_extended_with_column_numbers.csv"
CCA_RESULTS_FILE_PATH = "evaluation_results/evaluation_results.csv"

TARGET_CSV_FILE_PATH = "../comparative_study/core_comparison/core_comparison_results.csv"

CCA_WORKSPACE = "cca_workspace"

CORE_DEBUG_FOLDER = "../comparative_study/core_comparison/COREMSRI/comparison_output/cca_dataset_results"

CORE_DATASET_FOLDER = "../comparative_study/core_comparison/COREMSRI/dataset/cca_dataset"

MINING_OUTPUT_FOLDER = "../comparative_study/core_comparison/COREMSRI/comparison_output/cca_dataset_results/full_mining_output"


def evaluate_core_run_results():
    script_start_time = time.time_ns()
    print(f"Start time: {str(script_start_time)}")

    warnings_to_run_on_df = pd.read_csv(WARNINGS_TO_RUN_ON_FILE_PATH)

    cca_results_df = pd.read_csv(CCA_RESULTS_FILE_PATH)

    if not os.path.exists(MINING_OUTPUT_FOLDER):
        os.mkdir(MINING_OUTPUT_FOLDER)

    if not os.path.exists(TARGET_CSV_FILE_PATH):
        with open(TARGET_CSV_FILE_PATH, "w") as results_csv_file:
            csv_writer = csv.writer(
                results_csv_file, dialect=csv.unix_dialect)
            csv_writer.writerow(["instanceID", "projectName", "ruleKey", "ruleName", "ruleType", "experimentNumber", "classification", "plausibleFix",
                                "fixComplexity", "classificationSoundness", "fixCorrectness", "classificationSoundnessExplanation", "fixCorrectnessExplanation", "coreFixCreated", "coreNumberOfFixesCreated", "corePromptingTime", "coreStage4Time", "coreRankingTime", "coreTotalTime", "coreNumberBuildSuccessful", "coreNumberSonarCheckRemovedWarning", "coreNumberSonarCheckNoNewWarning", "coreNumberTestSuccessful", "coreNumberBuildAndRemovedWarning", "coreNumberBuildAndRemovedWarningAndNoNewWarning", "coreNumberBuildAndRemovedWarningAndNoNewWarningAndTest", "coreTPAssumptionSoundness", "coreNumberCorrectFixes", "codeSmellOutsideOfSonarQubeIntroduced", "coreTPAssumptionSoundnessExplanation", "coreFixCorrectnessExplanation"])

    with open(TARGET_CSV_FILE_PATH, "a+") as results_csv_file:
        csv_writer = csv.writer(
            results_csv_file, dialect=csv.unix_dialect)

        for index, warning_item in warnings_to_run_on_df.iterrows():
            print("Collecting CORE Evaluation on ID " +
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

            final_results_of_warning_folder = os.path.join(
                CORE_DEBUG_FOLDER, "results", str(warning_item["ruleKey"]))

            warning_final_fixes_file_names = []

            for file_name_fix in os.listdir(final_results_of_warning_folder):
                if file_name_fix.endswith(".java") and file_name_fix.startswith(str(warning_item["instanceID"]) + "_"):
                    warning_final_fixes_file_names.append(file_name_fix)

            core_fix_created = False
            core_number_of_fixes_created = 0
            core_prompting_time = 0
            core_stage4_time = 0
            core_ranking_time = 0
            core_total_time = 0
            coreNumberBuildSuccessful = 0
            coreNumberSonarCheckRemovedWarning = 0
            coreNumberSonarCheckNoNewWarning = 0
            coreNumberTestSuccessful = 0
            coreNumberBuildAndRemovedWarning = 0
            coreNumberBuildAndRemovedWarningAndNoNewWarning = 0
            coreNumberBuildAndRemovedWarningAndNoNewWarningAndTest = 0

            # Calc times

            # Calc proposer time for the target warning
            proposer_log_file_path = os.path.join(CORE_DEBUG_FOLDER, "proposer_results", warning_item["ruleKey"], "logs", str(
                warning_item["instanceID"]) + "_execution_log.log")

            with open(proposer_log_file_path, "r") as proposer_log_file:
                proposer_time_content = proposer_log_file.read()

            pattern = r"!! Warning \d+ fixing startup timestamp: (\d+)"
            start_time_match = re.search(
                pattern, proposer_time_content)
            proposer_start_time = int(start_time_match.group(1).strip())

            pattern = r"!! Warning \d+ fixing end timestamp: (\d+)"
            end_time_match = re.search(
                pattern, proposer_time_content)
            proposer_end_time = int(end_time_match.group(1).strip())

            core_prompting_time = proposer_end_time - proposer_start_time

            stage4_log_file_path = os.path.join(CORE_DEBUG_FOLDER, "stage4_execution", warning_item["ruleKey"], str(
                warning_item["instanceID"]) + "_execution_log.log")

            with open(stage4_log_file_path, "r") as stage4_log_file:
                stage4_time_content = stage4_log_file.read()

            pattern = r"!! Warning \d+ stage 4 startup timestamp: (\d+)"
            start_time_match = re.search(
                pattern, stage4_time_content)
            stage4_start_time = int(start_time_match.group(1).strip())

            pattern = r"!! Warning \d+ stage 4 end timestamp: (\d+)"
            end_time_match = re.search(
                pattern, stage4_time_content)
            stage4_end_time = int(end_time_match.group(1).strip())

            core_stage4_time = stage4_end_time - stage4_start_time

            ranker_log_file_path = os.path.join(CORE_DEBUG_FOLDER, "ranker_results", warning_item["ruleKey"], str(
                warning_item["instanceID"]) + "_execution_log.log")

            if os.path.exists(ranker_log_file_path):
                with open(ranker_log_file_path, "r") as ranker_log_file:
                    ranker_time_content = ranker_log_file.read()

                pattern = r"!! Warning \d+ ranker startup timestamp: (\d+)"
                start_time_match = re.search(
                    pattern, ranker_time_content)
                ranker_start_time = int(start_time_match.group(1).strip())

                pattern = r"!! Warning \d+ ranker end timestamp: (\d+)"
                end_time_match = re.search(
                    pattern, ranker_time_content)
                ranker_end_time = int(end_time_match.group(1).strip())

                core_ranking_time = ranker_end_time - ranker_start_time
            else:
                # No log file is created, if there is no fix that passes stage 4
                core_ranking_time = 0

            core_total_time = core_prompting_time + core_stage4_time + core_ranking_time

            # Iterate over all fixes
            for file_name_fix in warning_final_fixes_file_names:

                core_fix_created = True
                core_number_of_fixes_created = core_number_of_fixes_created + 1

                with open(os.path.join(final_results_of_warning_folder, file_name_fix.rstrip(".java") + "_oracle_results.csv"), "w") as single_fix_oracle_result_file:
                    csv_writer_single_fix_file = csv.writer(
                        single_fix_oracle_result_file, dialect=csv.unix_dialect)
                    csv_writer_single_fix_file.writerow(
                        ["buildSuccessful", "sonarCheckRemovedWarning", "sonarCheckNoNewWarning", "testSuccessful", "correctFix"])

                    buildSuccessful = False
                    sonarCheckRemovedWarning = False
                    sonarCheckNoNewWarning = True
                    testSuccessful = False

                    # apply the fixed file to the target project
                    os.remove(os.path.join(
                        CCA_WORKSPACE, agent.ai_config.warning_repository_name, agent.ai_config.warning_file_path))
                    shutil.copyfile(os.path.join(final_results_of_warning_folder, file_name_fix), os.path.join(
                        CCA_WORKSPACE, agent.ai_config.warning_repository_name, agent.ai_config.warning_file_path))

                    # mine warnings after
                    cmd = ["java", "-jar", agent.config.sorald_jar_path, "mine", "--source", os.path.join(CCA_WORKSPACE, agent.ai_config.warning_repository_name, agent.ai_config.warning_file_path), "--stats-output-file",
                           os.path.join(MINING_OUTPUT_FOLDER, file_name_fix + "_mining_out_after.json"), "--rule-parameters", "sonarqube_quality_profile/quality_profile_rule_parameters.json", "--target-java-version", agent.ai_config.warning_repository_target_java_version]

                    cmd.append("--rule-keys")
                    cmd.append(
                        ",".join(agent.sonar_qube_rules_in_active_profile))

                    result = subprocess.run(
                        cmd,
                        capture_output=True,
                        encoding="utf8",
                        shell=False
                    )

                    # Open vscode diff view of file with warning removed
                    # subprocess.run(
                    #    ["code", "--diff", os.path.join(CORE_DATASET_FOLDER, str(warning_item["ruleKey"]), str(
                    #        warning_item["instanceID"]) + ".java"), os.path.join(final_results_of_warning_folder, file_name_fix)],
                    #    capture_output=True,
                    #    encoding="utf8",
                    #    shell=False)

                    # Build project
                    error = None
                    try:
                        repository_operations.build_project(
                            agent, time_monitoring=False)

                        buildSuccessful = True

                    except repository_operations.BuildError as be:
                        error = be.stdout
                    except subprocess.TimeoutExpired as te:
                        error = f"TimeoutExpired exception: Build timed out after {te.timeout / 60} minutes. \nThe stdout was the following: \n\n{te.stdout.decode('utf-8')}"
                    except Exception as e:
                        error = str(e)

                    if error is not None:
                        with open(os.path.join(final_results_of_warning_folder, file_name_fix.rstrip(".java") + "_build_error.log"), "w") as log:
                            log.write(str(error))

                    if buildSuccessful:
                        coreNumberBuildSuccessful = coreNumberBuildSuccessful + 1

                    # Check if warning removed (number of target warnings reduced)

                    with open(os.path.join(MINING_OUTPUT_FOLDER, str(agent.ai_config.warning_ID) + "_mining_out_before.json")) as out_before_file:
                        mining_out_before = json.load(out_before_file)

                    with open(os.path.join(MINING_OUTPUT_FOLDER, file_name_fix + "_mining_out_after.json")) as out_after_file:
                        mining_out_after = json.load(out_after_file)
                    mined_rules_before = mining_out_before["minedRules"]
                    mined_rules_after = mining_out_after["minedRules"]

                    rule_match_before = [
                        mined_rule for mined_rule in mined_rules_before if mined_rule["ruleKey"] == warning_item["ruleKey"]]

                    if len(rule_match_before) == 0:

                        print(
                            f"ERROR: Initial SonarQube report for {agent.ai_config.warning_ID} was missing the target rule. This should not happen.")

                    rule_match_after = [
                        mined_rule for mined_rule in mined_rules_after if mined_rule["ruleKey"] == warning_item["ruleKey"]]

                    if len(rule_match_after) == 0:
                        # was removed
                        sonarCheckRemovedWarning = True
                        print(
                            f"For ID {agent.ai_config.warning_ID} and the fix {file_name_fix} the target warning was removed.")

                    else:
                        if len(rule_match_after[0]["warningLocations"]) < len(rule_match_before[0]["warningLocations"]):
                            # was removed
                            sonarCheckRemovedWarning = True
                            print(
                                f"For ID {agent.ai_config.warning_ID} and the fix {file_name_fix} the target warning was removed.")

                        else:
                            # was not removed, so don't add to the folder
                            sonarCheckRemovedWarning = False
                            print(
                                f"For ID {agent.ai_config.warning_ID} and the fix {file_name_fix} the target warning was !NOT! removed.")

                    if sonarCheckRemovedWarning:
                        coreNumberSonarCheckRemovedWarning = coreNumberSonarCheckRemovedWarning + 1

                    # check no other warnings introduced
                    rule_keys_of_newly_introduced_warnings = []

                    for mined_rule_after in mined_rules_after:
                        if mined_rule_after["ruleKey"] != agent.ai_config.warning_rule_key:
                            rule_key_in_after_matched_to_before = [
                                rule_before for rule_before in mined_rules_before if rule_before["ruleKey"] == mined_rule_after["ruleKey"]]
                            # Check if the rule present in the after report can be found in the before report too (if not it is new)
                            if len(rule_key_in_after_matched_to_before) == 0:
                                sonarCheckNoNewWarning = False
                                rule_keys_of_newly_introduced_warnings.append(
                                    mined_rule_after["ruleKey"])
                            # Check if the number of warnings of the rule is the same or less than before (if not then there are new warnings)
                            elif len(rule_key_in_after_matched_to_before[0]["warningLocations"]) < len(mined_rule_after["warningLocations"]):
                                sonarCheckNoNewWarning = False
                                rule_keys_of_newly_introduced_warnings.append(
                                    mined_rule_after["ruleKey"])

                    if sonarCheckNoNewWarning:
                        coreNumberSonarCheckNoNewWarning = coreNumberSonarCheckNoNewWarning + 1

                    # Test project
                    if buildSuccessful:
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
                            testSuccessful = True
                        else:
                            testSuccessful = False
                            error = test_result.stdout + "\n\n" + test_result.stderr

                        if error is not None:
                            with open(os.path.join(final_results_of_warning_folder, file_name_fix.rstrip(".java") + "_test_error.log"), "w") as log:
                                log.write(str(error))

                        if testSuccessful:
                            coreNumberTestSuccessful = coreNumberTestSuccessful + 1

                    if buildSuccessful and sonarCheckRemovedWarning:
                        coreNumberBuildAndRemovedWarning = coreNumberBuildAndRemovedWarning + 1

                    if buildSuccessful and sonarCheckRemovedWarning and sonarCheckNoNewWarning:
                        coreNumberBuildAndRemovedWarningAndNoNewWarning = coreNumberBuildAndRemovedWarningAndNoNewWarning + 1

                    if buildSuccessful and sonarCheckRemovedWarning and sonarCheckNoNewWarning and testSuccessful:
                        coreNumberBuildAndRemovedWarningAndNoNewWarningAndTest = coreNumberBuildAndRemovedWarningAndNoNewWarningAndTest + 1

                    csv_writer_single_fix_file.writerow(
                        [buildSuccessful, sonarCheckRemovedWarning, sonarCheckNoNewWarning, testSuccessful, ""])

            # Save results in csv (including the cca results)
            csv_writer.writerow(
                list(
                    cca_results_df.iloc[index]) + [core_fix_created, core_number_of_fixes_created, core_prompting_time, core_stage4_time, core_ranking_time, core_total_time, coreNumberBuildSuccessful, coreNumberSonarCheckRemovedWarning, coreNumberSonarCheckNoNewWarning, coreNumberTestSuccessful, coreNumberBuildAndRemovedWarning, coreNumberBuildAndRemovedWarningAndNoNewWarning, coreNumberBuildAndRemovedWarningAndNoNewWarningAndTest, "", "", "", "", ""]
            )

    script_end_time = time.time_ns()
    print(f"End time: {str(script_end_time)}")

    print(f"Time needed: {str(script_end_time - script_start_time)}")

    print(
        f"Time per instance: {str((script_end_time - script_start_time) / float((index + 1)))}")


if __name__ == "__main__":
    evaluate_core_run_results()

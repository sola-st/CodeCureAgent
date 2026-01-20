
import csv
import json
import os
import re
import shutil
import time
import pandas as pd

import sys
from pathlib import Path
sys.path.append(str(Path(__file__).parent.parent.parent.parent.parent / "code_cure_agent"))
os.chdir(str(Path(__file__).parent.parent.parent.parent.parent / "code_cure_agent"))
import subprocess

from agent_core.utils.agent_utils.agent_mock import AgentMock
from agent_core.commands import repository_operations

WARNINGS_TO_RUN_ON_FILE_PATH = "../comparative_study/ismell_comparison/iSMELL_Adapted_For_SonarQube_dataset/evaluation_dataset_filled_up_to_1000_input_file.csv"
CCA_RESULTS_FILE_PATH = "evaluation_results/evaluation_results.csv"

TARGET_CSV_FILE_PATH = "../comparative_study/ismell_comparison/iSMELL_Adapted_For_SonarQube_dataset/ismell_comparison_results.csv"

CCA_WORKSPACE = "cca_workspace"

ISMELL_DATASET_FOLDER = "../comparative_study/ismell_comparison/iSMELL_Adapted_For_SonarQube_dataset/cca_dataset"


def evaluate_ismell_run_results():
    script_start_time = time.time_ns()
    print(f"Start time: {str(script_start_time)}")

    warnings_to_run_on_df = pd.read_csv(WARNINGS_TO_RUN_ON_FILE_PATH)

    cca_results_df = pd.read_csv(CCA_RESULTS_FILE_PATH)


    if not os.path.exists(TARGET_CSV_FILE_PATH):
        with open(TARGET_CSV_FILE_PATH, "w") as results_csv_file:
            csv_writer = csv.writer(
                results_csv_file, dialect=csv.unix_dialect)
            csv_writer.writerow(["instanceID", "projectName", "ruleKey", "ruleName", "ruleType", "experimentNumber", "classification", "plausibleFix",
                                "fixComplexity", "classificationSoundness", "fixCorrectness", "classificationSoundnessExplanation", "fixCorrectnessExplanation", "ismellFixCreated", "ismellTotalTime", "ismellBuildSuccessful", "ismellSonarCheckRemovedWarning", "ismellSonarCheckNoNewWarning", "ismellTestSuccessful", "ismellBuildAndRemovedWarning", "ismellBuildAndRemovedWarningAndNoNewWarning", "ismellBuildAndRemovedWarningAndNoNewWarningAndTest", "ismellUncachedInputTokens", "ismellCachedInputTokens", "ismellOutputTokens", "ismellTotalTokens", "ismellCost", "ismellTPAssumptionSoundness", "ismellCorrectFix", "codeSmellOutsideOfSonarQubeIntroduced", "ismellTPAssumptionSoundnessExplanation", "ismellFixCorrectnessExplanation"])

    with open(TARGET_CSV_FILE_PATH, "a+") as results_csv_file:
        csv_writer = csv.writer(
            results_csv_file, dialect=csv.unix_dialect)

        for index, warning_item in warnings_to_run_on_df.iterrows():
            print("Collecting ISMELL Evaluation on ID " +
                  str(warning_item["instanceID"]))

            repository_name = warning_item["repositoryURL"].split(
                "/")[-1].removesuffix(".git")
            
            warning_after_fix_folder = os.path.join(ISMELL_DATASET_FOLDER, str(
                warning_item['instanceID']), "after")
            

            # Calc time

            ismell_total_time = 0

            time_log_file_path = os.path.join(warning_after_fix_folder, "execution_time.log")

            if os.path.exists(time_log_file_path):

                with open(time_log_file_path, "r") as proposer_log_file:
                    proposer_time_content = proposer_log_file.read()

                pattern = r"!! Warning \d+ fixing startup timestamp: (\d+)"
                start_time_match = re.search(
                    pattern, proposer_time_content)
                start_time = int(start_time_match.group(1).strip())

                pattern = r"!! Warning \d+ fixing end timestamp: (\d+)"
                end_time_match = re.search(
                    pattern, proposer_time_content)
                end_time = int(end_time_match.group(1).strip())

                ismell_total_time = end_time - start_time

            # Calc token consuption

            ismell_uncached_input_tokens = 0
            ismell_cached_input_tokens = 0
            ismell_output_tokens = 0
            ismell_total_tokens = 0
            ismell_cost = 0.0

            prompt_log_file_path = os.path.join(warning_after_fix_folder, "llm_interactions_log.json")

            if os.path.exists(prompt_log_file_path):
                
                with open(prompt_log_file_path, "r") as prompt_log_file:
                    prompt_log_content = json.load(prompt_log_file)

                for interaction in prompt_log_content:
                    ismell_uncached_input_tokens += interaction.get("token_usage", 0).get("prompt_tokens_details", 0).get("uncached_tokens", 0)
                    ismell_cached_input_tokens += interaction.get("token_usage", 0).get("prompt_tokens_details", 0).get("cached_tokens", 0)
                    ismell_output_tokens += interaction.get("token_usage", 0).get("completion_tokens", 0)
                    ismell_cost += interaction.get("estimated_cost", 0.0).get("total_cost", 0.0)

                ismell_total_tokens = ismell_uncached_input_tokens + ismell_cached_input_tokens + ismell_output_tokens

            ismell_fix_created = False
            
            warning_fix_file_path = os.path.join(warning_after_fix_folder, warning_item["filePath"].split('/')[-1])
            
            if not os.path.exists(warning_fix_file_path):
                print(
                    f"WARNING: No ISMELL fix found for warning ID {warning_item['instanceID']}. Skipping evaluation for this warning.")
                ismell_fix_created = False
                csv_writer.writerow(
                    list(
                        cca_results_df.iloc[index]) + [ismell_fix_created, ismell_total_time, False, False, False, False, False, False, False, ismell_uncached_input_tokens, ismell_cached_input_tokens, ismell_output_tokens, ismell_total_tokens, ismell_cost, "", "", "", "", ""]
                )
                continue
            
            ismell_fix_created = True


            agent = AgentMock(warning_item["repositoryURL"], warning_item["commit"], warning_item["filePath"], repository_name, warning_item["ruleKey"],
                              warning_item["startLine"], warning_item["ruleName"], warning_item["specificMessage"], workspace_path=CCA_WORKSPACE, warning_ID=warning_item["instanceID"])

            repository_operations.checkout_project(agent)

            

            # mine warnings before (only for the file with the warning to fix, else it takes too long)
            cmd = ["java", "-jar", agent.config.sorald_jar_path, "mine", "--source", os.path.join(CCA_WORKSPACE, agent.ai_config.warning_repository_name, agent.ai_config.warning_file_path), "--stats-output-file",
                   os.path.join(warning_after_fix_folder, str(agent.ai_config.warning_ID) + "_mining_out_before.json"), "--rule-parameters", "sonarqube_quality_profile/quality_profile_rule_parameters.json", "--target-java-version", str(warning_item["targetJavaVersion"])]

            cmd.append("--rule-keys")
            cmd.append(",".join(agent.sonar_qube_rules_in_active_profile))

            result = subprocess.run(
                cmd,
                capture_output=True,
                encoding="utf8",
                shell=False
            )

            

            
            
            ismellBuildSuccessful = False
            ismellSonarCheckRemovedWarning = False
            # This has to be true for correct alg
            ismellSonarCheckNoNewWarning = True
            ismellTestSuccessful = False
            ismellBuildAndRemovedWarning = False
            ismellBuildAndRemovedWarningAndNoNewWarning = False
            ismellBuildAndRemovedWarningAndNoNewWarningAndTest = False

            
            

            # apply the fixed file to the target project
            os.remove(os.path.join(
                CCA_WORKSPACE, agent.ai_config.warning_repository_name, agent.ai_config.warning_file_path))
            shutil.copyfile(warning_fix_file_path, os.path.join(
                CCA_WORKSPACE, agent.ai_config.warning_repository_name, agent.ai_config.warning_file_path))

            # mine warnings after
            cmd = ["java", "-jar", agent.config.sorald_jar_path, "mine", "--source", os.path.join(CCA_WORKSPACE, agent.ai_config.warning_repository_name, agent.ai_config.warning_file_path), "--stats-output-file",
                    os.path.join(warning_after_fix_folder, str(agent.ai_config.warning_ID) + "_mining_out_after.json"), "--rule-parameters", "sonarqube_quality_profile/quality_profile_rule_parameters.json", "--target-java-version", str(warning_item["targetJavaVersion"])]

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

                ismellBuildSuccessful = True

            except repository_operations.BuildError as be:
                error = be.stdout
            except subprocess.TimeoutExpired as te:
                error = f"TimeoutExpired exception: Build timed out after {te.timeout / 60} minutes. \nThe stdout was the following: \n\n{te.stdout.decode('utf-8')}"
            except Exception as e:
                error = str(e)

            if error is not None:
                with open(os.path.join(warning_after_fix_folder, str(agent.ai_config.warning_ID) + "_build_error.log"), "w") as log:
                    log.write(str(error))

                

            # Check if warning removed (number of target warnings reduced)

            with open(os.path.join(warning_after_fix_folder, str(agent.ai_config.warning_ID) + "_mining_out_before.json")) as out_before_file:
                mining_out_before = json.load(out_before_file)

            with open(os.path.join(warning_after_fix_folder, str(agent.ai_config.warning_ID) + "_mining_out_after.json")) as out_after_file:
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
                ismellSonarCheckRemovedWarning = True
                print(
                    f"For ID {agent.ai_config.warning_ID} the target warning was removed.")

            else:
                if len(rule_match_after[0]["warningLocations"]) < len(rule_match_before[0]["warningLocations"]):
                    # was removed
                    ismellSonarCheckRemovedWarning = True
                    print(
                        f"For ID {agent.ai_config.warning_ID} the target warning was removed.")

                else:
                    # was not removed, so don't add to the folder
                    ismellSonarCheckRemovedWarning = False
                    print(
                        f"For ID {agent.ai_config.warning_ID} the target warning was !NOT! removed.")


            # check no other warnings introduced
            rule_keys_of_newly_introduced_warnings = []

            for mined_rule_after in mined_rules_after:
                if mined_rule_after["ruleKey"] != agent.ai_config.warning_rule_key:
                    rule_key_in_after_matched_to_before = [
                        rule_before for rule_before in mined_rules_before if rule_before["ruleKey"] == mined_rule_after["ruleKey"]]
                    # Check if the rule present in the after report can be found in the before report too (if not it is new)
                    if len(rule_key_in_after_matched_to_before) == 0:
                        ismellSonarCheckNoNewWarning = False
                        rule_keys_of_newly_introduced_warnings.append(
                            (mined_rule_after["ruleKey"], mined_rule_after["ruleName"]))
                    # Check if the number of warnings of the rule is the same or less than before (if not then there are new warnings)
                    elif len(rule_key_in_after_matched_to_before[0]["warningLocations"]) < len(mined_rule_after["warningLocations"]):
                        ismellSonarCheckNoNewWarning = False
                        rule_keys_of_newly_introduced_warnings.append(
                            (mined_rule_after["ruleKey"], mined_rule_after["ruleName"]))

            if len(rule_keys_of_newly_introduced_warnings) > 0:
                with open(os.path.join(warning_after_fix_folder, str(agent.ai_config.warning_ID) + "_new_warnings_introduced.csv"), "w") as log:
                    for rule_key, rule_name in rule_keys_of_newly_introduced_warnings:
                        log.write(f"{rule_key},{rule_name}\n")

            # Test project
            if ismellBuildSuccessful:
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
                    ismellTestSuccessful = True
                else:
                    ismellTestSuccessful = False
                    error = test_result.stdout + "\n\n" + test_result.stderr

                if error is not None:
                    with open(os.path.join(warning_after_fix_folder, str(agent.ai_config.warning_ID) + "_test_error.log"), "w") as log:
                        log.write(str(error))


            if ismellBuildSuccessful and ismellSonarCheckRemovedWarning:
                ismellBuildAndRemovedWarning = True

            if ismellBuildSuccessful and ismellSonarCheckRemovedWarning and ismellSonarCheckNoNewWarning:
                ismellBuildAndRemovedWarningAndNoNewWarning = True

            if ismellBuildSuccessful and ismellSonarCheckRemovedWarning and ismellSonarCheckNoNewWarning and ismellTestSuccessful:
                ismellBuildAndRemovedWarningAndNoNewWarningAndTest = True
                

            # Save results in csv (including the cca results)
            csv_writer.writerow(
                list(
                    cca_results_df.iloc[index]) + [ismell_fix_created, ismell_total_time, ismellBuildSuccessful, ismellSonarCheckRemovedWarning, ismellSonarCheckNoNewWarning, ismellTestSuccessful, ismellBuildAndRemovedWarning, ismellBuildAndRemovedWarningAndNoNewWarning, ismellBuildAndRemovedWarningAndNoNewWarningAndTest, ismell_uncached_input_tokens, ismell_cached_input_tokens, ismell_output_tokens, ismell_total_tokens, ismell_cost, "", "", "", "", ""]
            )

    script_end_time = time.time_ns()
    print(f"End time: {str(script_end_time)}")

    print(f"Time needed: {str(script_end_time - script_start_time)}")

    print(
        f"Time per instance: {str((script_end_time - script_start_time) / float((index + 1)))}")


if __name__ == "__main__":
    evaluate_ismell_run_results()

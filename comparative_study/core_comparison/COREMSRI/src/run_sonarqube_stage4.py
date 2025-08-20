

import argparse
import json
import os
import re
import subprocess
import time
import pandas as pd


WARNINGS_TO_RUN_ON_FILE_NAME = "../evaluation_dataset_first_291_input_file_extended_with_column_numbers.csv"

SORALD_JAR_PATH = "/workspaces/master-thesis-pascal-joos/code_cure_agent/sorald/sorald.jar"

SONARQUBE_QUALITY_PROFILE_RULE_PARAMS = "/workspaces/master-thesis-pascal-joos/code_cure_agent/sonarqube_quality_profile/quality_profile_rule_parameters.json"


'''
This script does the following:

 Takes in the created fixes of the LLM proposer

 - Removes ```java from the start of the files
 - Deduplicates the files
 - Runs static analyzer and checks if number of warnings of the target warning's rule is reduced
 - Creates and saves the diff for files that passed the previous steps

'''


def run_sonarqube_stage4(dataset, debug_folder, diff_folder):

    # Iterate the Warnings

    if not os.path.exists(os.path.join(debug_folder, "dedub_fixes")):
        os.mkdir(os.path.join(debug_folder, "dedub_fixes"))

    if not os.path.exists(os.path.join(debug_folder, "analysis_results")):
        os.mkdir(os.path.join(debug_folder, "analysis_results"))

    if not os.path.exists(os.path.join(debug_folder, "all_check_passing_fixes")):
        os.mkdir(os.path.join(debug_folder, "all_check_passing_fixes"))

    if not os.path.exists(os.path.join(debug_folder, "stage4_execution")):
        os.mkdir(os.path.join(debug_folder, "stage4_execution"))

    if not os.path.exists(diff_folder):
        os.mkdir(diff_folder)

    warnings_to_run_on_df = pd.read_csv(WARNINGS_TO_RUN_ON_FILE_NAME)

    for index, warning_item in warnings_to_run_on_df.iterrows():
        print(warning_item["instanceID"])

        warning_id = warning_item["instanceID"]

        if not os.path.exists(os.path.join(debug_folder, "stage4_execution", str(warning_item["ruleKey"]))):
            os.mkdir(os.path.join(debug_folder,
                     "stage4_execution", str(warning_item["ruleKey"])))

        with open(os.path.join(debug_folder, "stage4_execution", str(warning_item["ruleKey"]), str(warning_id) + "_execution_log.log"), "w") as execution_log_file:
            execution_log_file.write(
                f"!! Warning {str(warning_id)} stage 4 startup timestamp: " + str(time.time_ns()) + "\n")

        proposer_results_of_warning_folder = os.path.join(
            debug_folder, "proposer_results", str(warning_item["ruleKey"]))

        # Run Static Analysis on the original file
        if not os.path.exists(os.path.join(debug_folder, "analysis_results", str(warning_item["ruleKey"]))):
            os.mkdir(os.path.join(debug_folder, "analysis_results",
                     str(warning_item["ruleKey"])))

        original_file_path = os.path.join(dataset, str(
            warning_item["ruleKey"]), str(warning_item["instanceID"]) + ".java")
        stats_out_file_original = os.path.join(debug_folder, "analysis_results", str(
            warning_item["ruleKey"]), "original_analysis_result.json")

        cmd = ["java", "-jar", SORALD_JAR_PATH, "mine", "--source", original_file_path, "--stats-output-file",
               stats_out_file_original, "--rule-parameters", SONARQUBE_QUALITY_PROFILE_RULE_PARAMS, "--target-java-version", str(warning_item["targetJavaVersion"]), "--rule-keys", str(warning_item["ruleKey"])]

        initial_analysis_result = subprocess.run(
            cmd,
            capture_output=True,
            encoding="utf8",
            shell=False
        )

        if initial_analysis_result.returncode != 0:
            print(
                f"ERROR: SonarQube analysis failed for original file with error: {initial_analysis_result.stdout} {initial_analysis_result.stderr}")

        warning_created_fixes_file_names = []

        for file_name_fix in os.listdir(proposer_results_of_warning_folder):
            if file_name_fix.startswith(str(warning_item["instanceID"]) + "_"):
                warning_created_fixes_file_names.append(file_name_fix)

        if not os.path.exists(os.path.join(debug_folder, "dedub_fixes", str(warning_item["ruleKey"]))):
            os.mkdir(os.path.join(debug_folder, "dedub_fixes",
                     str(warning_item["ruleKey"])))

        if not os.path.exists(os.path.join(debug_folder, "all_check_passing_fixes", str(warning_item["ruleKey"]))):
            os.mkdir(os.path.join(debug_folder, "all_check_passing_fixes",
                     str(warning_item["ruleKey"])))

        if not os.path.exists(os.path.join(diff_folder, str(warning_item["ruleKey"]))):
            os.mkdir(os.path.join(diff_folder, str(warning_item["ruleKey"])))

        unique_contents = {}
        for fix_file_name in warning_created_fixes_file_names:
            fix_path = os.path.join(
                proposer_results_of_warning_folder, fix_file_name)
            with open(fix_path, "r") as f:
                content = f.read()
            # Remove ```java prefix if present for comparison
            if content.startswith("```java"):
                content = content[len("```java"):].lstrip()

             # Remove duplicate fixes with identical content
            if content not in unique_contents.values():
                unique_contents[fix_file_name] = content

                # Only save the non-duplicates to the dedub_fixes folder
                cleaned_fix_path = os.path.join(debug_folder, "dedub_fixes", str(
                    warning_item["ruleKey"]), fix_file_name)
                with open(cleaned_fix_path, "w") as f:
                    f.write(content)

                # Mine the fix
                stats_out_file_fixed = os.path.join(debug_folder, "analysis_results", str(
                    warning_item["ruleKey"]), fix_file_name + "_analysis_result.json")

                cmd = ["java", "-jar", SORALD_JAR_PATH, "mine", "--source", cleaned_fix_path, "--stats-output-file",
                       stats_out_file_fixed, "--rule-parameters", SONARQUBE_QUALITY_PROFILE_RULE_PARAMS, "--target-java-version", str(warning_item["targetJavaVersion"]), "--rule-keys", str(warning_item["ruleKey"])]

                analysis_result = subprocess.run(
                    cmd,
                    capture_output=True,
                    encoding="utf8",
                    shell=False
                )

                instance = str(warning_item["instanceID"])

                if analysis_result.returncode != 0:

                    print(
                        f"ERROR: SonarQube analysis failed for {instance} with error: {analysis_result.stdout} {analysis_result.stderr}")
                else:
                    with open(stats_out_file_original) as out_before_file:
                        mining_out_before = json.load(out_before_file)

                    with open(stats_out_file_fixed) as out_after_file:
                        mining_out_after = json.load(out_after_file)
                    mined_rules_before = mining_out_before["minedRules"]
                    mined_rules_after = mining_out_after["minedRules"]

                    rule_match_before = [
                        mined_rule for mined_rule in mined_rules_before if mined_rule["ruleKey"] == warning_item["ruleKey"]]

                    if len(rule_match_before) == 0:

                        print(
                            f"ERROR: Initial SonarQube report for {instance} was missing the target rule. This should not happen.")

                    rule_match_after = [
                        mined_rule for mined_rule in mined_rules_after if mined_rule["ruleKey"] == warning_item["ruleKey"]]

                    if len(rule_match_after) == 0:
                        # was removed
                        print(
                            f"For ID {instance} and the fix {fix_file_name} the target warning was removed.")

                        all_check_passing_fixes_path = os.path.join(debug_folder, "all_check_passing_fixes", str(
                            warning_item["ruleKey"]), fix_file_name)
                        with open(all_check_passing_fixes_path, "w") as f:
                            f.write(content)
                        create_diff(warning_item, fix_file_name, original_file_path,
                                    all_check_passing_fixes_path, diff_folder)
                    else:
                        if len(rule_match_after[0]["warningLocations"]) < len(rule_match_before[0]["warningLocations"]):
                            # was removed
                            print(
                                f"For ID {instance} and the fix {fix_file_name} the target warning was removed.")
                            all_check_passing_fixes_path = os.path.join(debug_folder, "all_check_passing_fixes", str(
                                warning_item["ruleKey"]), fix_file_name)
                            with open(all_check_passing_fixes_path, "w") as f:
                                f.write(content)
                            create_diff(warning_item, fix_file_name, original_file_path,
                                        all_check_passing_fixes_path, diff_folder)

                        else:
                            # was not removed, so don't add to the folder
                            print(
                                f"For ID {instance} and the fix {fix_file_name} the target warning was !NOT! removed.")

        with open(os.path.join(debug_folder, "stage4_execution", str(warning_item["ruleKey"]), str(warning_id) + "_execution_log.log"), "a+") as execution_log_file:
            execution_log_file.write(
                f"!! Warning {str(warning_id)} stage 4 end timestamp: " + str(time.time_ns()) + "\n")


def create_diff(warning_item, fix_file_name: str, original_file_path, fix_file_path, diff_folder):

    return_diff = subprocess.run(
        ["diff", original_file_path, fix_file_path, "-u", "--ignore-all-space"],
        capture_output=True,
        encoding="utf8",
        shell=False)

    diff_cleaned = re.sub(r'^(--- .*\n|\+\+\+ .*\n)', '',
                          return_diff.stdout, flags=re.MULTILINE)

    with open(os.path.join(diff_folder, str(warning_item["ruleKey"]), fix_file_name.rstrip(".java") + ".diff"), "w") as diff_file:
        diff_file.write(diff_cleaned)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--Exp_folder", "-e", type=str, required=True)
    parser.add_argument("--debug_folder",
                        "-i", type=str, required=True)
    parser.add_argument('-s', "--diff_folder", type=str)

    args = parser.parse_args()

    run_sonarqube_stage4(args.Exp_folder, args.debug_folder, args.diff_folder)

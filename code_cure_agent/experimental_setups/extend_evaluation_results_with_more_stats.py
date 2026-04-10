import os
import re
import click
import pandas as pd
import csv

import sys
from pathlib import Path

sys.path.append(str(Path(__file__).parent.parent))
from agent_core.llm.providers.openai import OPEN_AI_CHAT_MODELS


@click.command()
@click.argument(
    "evaluation-results-file",
    type=click.File()
)
@click.option(
    "--target-csv-file-path",
    "-t",
    default="./evaluation_results/evaluation_results_extended.csv",
    help="Path where the extended evaluation results file should be written."
)
def extend_evaluation_results_with_more_stats(evaluation_results_file: click.File, target_csv_file_path: str):

    evaluation_results_file_df = pd.read_csv(evaluation_results_file)

    add_info_on_passed_stages(evaluation_results_file_df)

    add_info_on_used_iterations(evaluation_results_file_df)

    add_info_on_fixes_count(evaluation_results_file_df)

    add_info_on_used_tokens(evaluation_results_file_df)

    add_info_on_cost(evaluation_results_file_df)

    add_info_on_execution_time(evaluation_results_file_df)

    add_info_change_approver_ablation(evaluation_results_file_df)

    add_info_execution_time_in_substeps(evaluation_results_file_df)

    add_info_number_of_different_tool_calls(evaluation_results_file_df)

    # Move the new columns to the end of the DataFrame
    # cols = [col for col in evaluation_results_file_df.columns if col not in ["compilationPassed", "sonarQubeCheckPassed"]]
    # cols += ["compilationPassed", "sonarQubeCheckPassed"]
    # evaluation_results_file_df.reindex(columns=cols, copy=False)
    # evaluation_results_file_df[:] = evaluation_results_file_df[cols]

    evaluation_results_file_df.to_csv(
        target_csv_file_path, encoding="utf-8", index=False, header=True, quoting=csv.QUOTE_ALL, quotechar='"',
        doublequote=True)


def add_info_on_passed_stages(evaluation_results_file_df: pd.DataFrame):

    evaluation_results_file_df["compilationPassed"] = evaluation_results_file_df.apply(
        lambda row: get_compilation_passed(
            row["instanceID"],
            row["experimentNumber"],
            row["classification"],
            row["plausibleFix"]
        ),
        axis=1
    )
    evaluation_results_file_df["sonarQubeCheckPassed"] = evaluation_results_file_df.apply(
        lambda row: get_sonarqube_check_passed(
            row["instanceID"],
            row["experimentNumber"],
            row["classification"],
            row["plausibleFix"]
        ),
        axis=1
    )


def add_info_on_used_iterations(evaluation_results_file_df: pd.DataFrame):
    evaluation_results_file_df["iterationsClassification"] = evaluation_results_file_df.apply(
        lambda row: get_iterations_in_classification(
            row["instanceID"],
            row["experimentNumber"]
        ),
        axis=1
    )
    evaluation_results_file_df["iterationsFixTP"] = evaluation_results_file_df.apply(
        lambda row: get_iterations_in_fix_x(
            row["instanceID"],
            row["experimentNumber"],
            row["classification"],
            "TP"
        ),
        axis=1
    )
    evaluation_results_file_df["iterationsFixFP"] = evaluation_results_file_df.apply(
        lambda row: get_iterations_in_fix_x(
            row["instanceID"],
            row["experimentNumber"],
            row["classification"],
            "FP"
        ),
        axis=1
    )


def add_info_on_fixes_count(evaluation_results_file_df: pd.DataFrame):
    evaluation_results_file_df["implausible_fixes_count"] = evaluation_results_file_df.apply(
        lambda row: get_implausible_fixes_count(
            row["instanceID"],
            row["experimentNumber"],
            row["classification"]
        ),
        axis=1
    )
    evaluation_results_file_df["plausible_fixes_count"] = evaluation_results_file_df.apply(
        lambda row: get_plausible_fixes_count(
            row["instanceID"],
            row["experimentNumber"],
            row["classification"]
        ),
        axis=1
    )


def add_info_on_used_tokens(evaluation_results_file_df: pd.DataFrame):
    evaluation_results_file_df[["tokensInputUncachedClassification", "tokensInputCachedClassification", "tokensOutputClassification"]] = evaluation_results_file_df.apply(
        lambda row: get_tokens(
            row["instanceID"],
            row["experimentNumber"],
            row["classification"],
            "classification"
        ),
        axis=1
    )
    evaluation_results_file_df[["tokensInputUncachedFixTP", "tokensInputCachedFixTP", "tokensOutputFixTP"]] = evaluation_results_file_df.apply(
        lambda row: get_tokens(
            row["instanceID"],
            row["experimentNumber"],
            row["classification"],
            "fixTP"
        ),
        axis=1
    )
    evaluation_results_file_df[["tokensInputUncachedFixFP", "tokensInputCachedFixFP", "tokensOutputFixFP"]] = evaluation_results_file_df.apply(
        lambda row: get_tokens(
            row["instanceID"],
            row["experimentNumber"],
            row["classification"],
            "fixFP"
        ),
        axis=1
    )


def add_info_on_cost(evaluation_results_file_df: pd.DataFrame):
    evaluation_results_file_df["costClassification"] = evaluation_results_file_df.apply(
        lambda row: get_cost(
            row["tokensInputUncachedClassification"],
            row["tokensInputCachedClassification"],
            row["tokensOutputClassification"]
        ),
        axis=1
    )
    evaluation_results_file_df["costFixTP"] = evaluation_results_file_df.apply(
        lambda row: get_cost(
            row["tokensInputUncachedFixTP"],
            row["tokensInputCachedFixTP"],
            row["tokensOutputFixTP"]
        ),
        axis=1
    )
    evaluation_results_file_df["costFixFP"] = evaluation_results_file_df.apply(
        lambda row: get_cost(
            row["tokensInputUncachedFixFP"],
            row["tokensInputCachedFixFP"],
            row["tokensOutputFixFP"]
        ),
        axis=1
    )

    # print(evaluation_results_file_df["costClassification"].sum())
    # print(evaluation_results_file_df["costFixTP"].sum())
    # print(evaluation_results_file_df["costFixFP"].sum())


def add_info_on_execution_time(evaluation_results_file_df: pd.DataFrame):
    evaluation_results_file_df["executionTimeClassification"] = evaluation_results_file_df.apply(
        lambda row: get_execution_time(
            row["instanceID"],
            row["experimentNumber"],
            "classification"
        ),
        axis=1
    )
    evaluation_results_file_df["executionTimeFixTP"] = evaluation_results_file_df.apply(
        lambda row: get_execution_time(
            row["instanceID"],
            row["experimentNumber"],
            "fixTP"
        ),
        axis=1
    )
    evaluation_results_file_df["executionTimeFixFP"] = evaluation_results_file_df.apply(
        lambda row: get_execution_time(
            row["instanceID"],
            row["experimentNumber"],
            "fixFP"
        ),
        axis=1
    )


def add_info_change_approver_ablation(evaluation_results_file_df: pd.DataFrame):
    evaluation_results_file_df["ablationNoChangeApproverPlausibleFix"] = evaluation_results_file_df.apply(
        lambda row: get_ablation_no_change_approver_is_still_plausible(
            row["instanceID"],
            row["experimentNumber"],
            row["classification"],
            row["plausibleFix"]
        ),
        axis=1
    )
    evaluation_results_file_df["ablationOnlyBuildPlausibleFix"] = evaluation_results_file_df.apply(
        lambda row: get_ablation_only_build_step_is_still_plausible(
            row["instanceID"],
            row["experimentNumber"],
            row["classification"],
            row["plausibleFix"]
        ),
        axis=1
    )
    evaluation_results_file_df["ablationBuildAndSonarQubeCheckPlausibleFix"] = evaluation_results_file_df.apply(
        lambda row: get_ablation_build_step_and_sonar_qube_check_is_still_plausible(
            row["instanceID"],
            row["experimentNumber"],
            row["classification"],
            row["plausibleFix"]
        ),
        axis=1
    )


def add_info_execution_time_in_substeps(evaluation_results_file_df: pd.DataFrame):

    evaluation_results_file_df["mavenBuildAddedTime"] = evaluation_results_file_df.apply(
        lambda row: get_time_added("build",
                                   row["instanceID"],
                                   row["experimentNumber"],
                                   row["classification"]
                                   ),
        axis=1
    )
    evaluation_results_file_df["mavenTestAddedTime"] = evaluation_results_file_df.apply(
        lambda row: get_time_added("test",
                                   row["instanceID"],
                                   row["experimentNumber"],
                                   row["classification"]
                                   ),
        axis=1
    )
    evaluation_results_file_df["mavenAnalysisAddedTime"] = evaluation_results_file_df.apply(
        lambda row: get_time_added("analysis",
                                   row["instanceID"],
                                   row["experimentNumber"],
                                   row["classification"]
                                   ),
        axis=1
    )
    evaluation_results_file_df["LLMAddedTime"] = evaluation_results_file_df.apply(
        lambda row: get_time_added("LLM",
                                   row["instanceID"],
                                   row["experimentNumber"],
                                   row["classification"]
                                   ),
        axis=1
    )


def add_info_number_of_different_tool_calls(evaluation_results_file_df: pd.DataFrame):
    evaluation_results_file_df["tool_calls_read_sonarqube_docu"] = evaluation_results_file_df.apply(
        lambda row: get_number_tool_calls("read_sonarqube_docu",
                                          row["instanceID"],
                                          row["experimentNumber"],
                                          row["classification"]
                                          ),
        axis=1
    )
    evaluation_results_file_df["tool_calls_read_range"] = evaluation_results_file_df.apply(
        lambda row: get_number_tool_calls("read_range",
                                          row["instanceID"],
                                          row["experimentNumber"],
                                          row["classification"]
                                          ),
        axis=1
    )
    evaluation_results_file_df["tool_calls_find_references"] = evaluation_results_file_df.apply(
        lambda row: get_number_tool_calls("find_references",
                                          row["instanceID"],
                                          row["experimentNumber"],
                                          row["classification"]
                                          ),
        axis=1
    )
    evaluation_results_file_df["tool_calls_find_definition"] = evaluation_results_file_df.apply(
        lambda row: get_number_tool_calls("find_definition",
                                          row["instanceID"],
                                          row["experimentNumber"],
                                          row["classification"]
                                          ),
        axis=1
    )
    evaluation_results_file_df["tool_calls_search_for_patterns"] = evaluation_results_file_df.apply(
        lambda row: get_number_tool_calls("search_for_patterns",
                                          row["instanceID"],
                                          row["experimentNumber"],
                                          row["classification"]
                                          ),
        axis=1
    )
    evaluation_results_file_df["tool_calls_answer_question"] = evaluation_results_file_df.apply(
        lambda row: get_number_tool_calls("answer_question",
                                          row["instanceID"],
                                          row["experimentNumber"],
                                          row["classification"]
                                          ),
        axis=1
    )
    evaluation_results_file_df["tool_calls_give_final_verdict"] = evaluation_results_file_df.apply(
        lambda row: get_number_tool_calls("give_final_verdict",
                                          row["instanceID"],
                                          row["experimentNumber"],
                                          row["classification"]
                                          ),
        axis=1
    )
    evaluation_results_file_df["tool_calls_formulate_plan"] = evaluation_results_file_df.apply(
        lambda row: get_number_tool_calls("formulate_plan",
                                          row["instanceID"],
                                          row["experimentNumber"],
                                          row["classification"]
                                          ),
        axis=1
    )
    evaluation_results_file_df["tool_calls_write_fix"] = evaluation_results_file_df.apply(
        lambda row: get_number_tool_calls("write_fix",
                                          row["instanceID"],
                                          row["experimentNumber"],
                                          row["classification"]
                                          ),
        axis=1
    )
    evaluation_results_file_df["tool_calls_goals_accomplished"] = evaluation_results_file_df.apply(
        lambda row: get_number_tool_calls("goals_accomplished",
                                          row["instanceID"],
                                          row["experimentNumber"],
                                          row["classification"]
                                          ),
        axis=1
    )


def get_number_tool_calls(tool_name: str, instance_id: int, experiment_number: int, classification: str) -> int:

    tool_calls = 0

    # tool calls in classification
    try:
        execution_file_name = next(f for f in os.listdir(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "classification", "execution_info"))
                                   if os.path.isfile(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "classification", "execution_info", f)) and f.startswith(str(instance_id) + "_"))
    except StopIteration:
        print(
            f"ERROR: execution_info file not found for {str(instance_id)} in classification")
        return 0

    execution_file_path = os.path.join("experimental_setups", "experiment_" + str(
        experiment_number), "classification", "execution_info", execution_file_name)

    with open(execution_file_path, "r") as f:
        execution_file_content = f.read()

    pattern = rf"!! Command {tool_name} startup timestamp: (\d+)"
    matches = re.findall(pattern, execution_file_content)
    tool_calls += len(matches)

    # tool calls in fix_tp or fix_fp
    try:
        execution_file_name = next(f for f in os.listdir(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "fix_" + str(classification).lower(), "execution_info"))
                                   if os.path.isfile(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "fix_" + str(classification).lower(), "execution_info", f)) and f.startswith(str(instance_id) + "_"))
    except StopIteration:
        print(
            f"ERROR: execution_info file not found for {str(instance_id)} in fix_{str(classification).lower()}")
        return 0

    execution_file_path = os.path.join("experimental_setups", "experiment_" + str(
        experiment_number), "fix_" + str(classification).lower(), "execution_info", execution_file_name)

    with open(execution_file_path, "r") as f:
        execution_file_content = f.read()

    pattern = rf"!! Command {tool_name} startup timestamp: (\d+)"
    matches = re.findall(pattern, execution_file_content)
    tool_calls += len(matches)

    return tool_calls


def get_time_added(step_to_get_time_for: str, instance_id: int, experiment_number: int, classification: str) -> int:

    time_added = 0

    # times in classification
    try:
        execution_info_file_name = next(f for f in os.listdir(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "classification", "execution_info"))
                                        if os.path.isfile(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "classification", "execution_info", f)) and f.startswith(str(instance_id) + "_"))
    except StopIteration:
        print(
            f"ERROR: execution_info file not found for {str(instance_id)} in classification")
        return 0

    execution_info_file_path = os.path.join("experimental_setups", "experiment_" + str(
        experiment_number), "classification", "execution_info", execution_info_file_name)

    with open(execution_info_file_path, "r") as f:
        execution_info_file_content = f.read()

    if step_to_get_time_for == "build":
        pattern_start = r"!! Maven build startup timestamp: (\d+)"
    elif step_to_get_time_for == "test":
        pattern_start = r"!! Maven test startup timestamp: (\d+)"
    elif step_to_get_time_for == "analysis":
        pattern_start = r"!! SonarQube analysis startup timestamp: (\d+)"
    elif step_to_get_time_for == "LLM":
        pattern_start = r"!! AI Chat Completion startup timestamp: (\d+)"

    start_time_match_iter = re.finditer(
        pattern_start, execution_info_file_content)

    if step_to_get_time_for == "build":
        pattern_end = r"!! Maven build end timestamp: (\d+)"
    elif step_to_get_time_for == "test":
        pattern_end = r"!! Maven test end timestamp: (\d+)"
    elif step_to_get_time_for == "analysis":
        pattern_end = r"!! SonarQube analysis end timestamp: (\d+)"
    elif step_to_get_time_for == "LLM":
        pattern_end = r"!! AI Chat Completion end timestamp: (\d+)"

    end_time_match_iter = re.finditer(
        pattern_end, execution_info_file_content)

    for start_time_match in start_time_match_iter:
        start_time = int(start_time_match.group(1).strip())
        try:
            end_time_match = next(end_time_match_iter)
        except StopIteration:
            print(
                f"ERROR: No end time found for a start time of {step_to_get_time_for} for {str(instance_id)} in classification")
            return 0
        end_time = int(end_time_match.group(1).strip())
        time_added += end_time - start_time

    # times in fix_tp or fix_fp
    try:
        execution_info_file_name = next(f for f in os.listdir(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "fix_" + str(classification).lower(), "execution_info"))
                                        if os.path.isfile(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "fix_" + str(classification).lower(), "execution_info", f)) and f.startswith(str(instance_id) + "_"))
    except StopIteration:
        print(
            f"ERROR: execution_info file not found for {str(instance_id)} in fix_{str(classification).lower()}")
        return 0

    execution_info_file_path = os.path.join("experimental_setups", "experiment_" + str(
        experiment_number), "fix_" + str(classification).lower(), "execution_info", execution_info_file_name)

    with open(execution_info_file_path, "r") as f:
        execution_info_file_content = f.read()

    start_time_match_iter = re.finditer(
        pattern_start, execution_info_file_content)

    end_time_match_iter = re.finditer(
        pattern_end, execution_info_file_content)

    for start_time_match in start_time_match_iter:
        start_time = int(start_time_match.group(1).strip())
        try:
            end_time_match = next(end_time_match_iter)
        except StopIteration:
            print(
                f"ERROR: No end time found for a start time of {step_to_get_time_for} for {str(instance_id)} in fix_{str(classification).lower()}")
            return 0
        end_time = int(end_time_match.group(1).strip())
        time_added += end_time - start_time

    return time_added


def get_ablation_no_change_approver_is_still_plausible(instance_id: int, experiment_number: int, classification: str, plausible_fix: bool) -> bool:
    if not plausible_fix:
        # If there is no plausible_fix with ChangeApprover then there also isn't one without ChangeApprover (plausible_fix means it passed/can pass all three steps)
        return False
    
    if not os.path.exists(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "fix_" + str(classification).lower(), "implausible_patches")):
        # If there is no implausible_patches folder then there also isn't an implausible patch that would have been wrongly accepted without ChangeApprover. So return True in this case.
        return True
    try:
        implausible_patch_file_name = next(f for f in os.listdir(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "fix_" + str(classification).lower(), "implausible_patches"))
                                           if os.path.isfile(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "fix_" + str(classification).lower(), "implausible_patches", f)) and f.startswith(str(instance_id) + "_"))
    except StopIteration:
        # No implausible_patches file was found. This means that the first fix attempt was already a plausible_fix. So this would have been accepted with no ChangeApprover, too.
        # Therefore return True only in this case.
        return True
    # If a implausible_patches file was created then there was an implausible patch so without ChangeApprover this would have been wrongly accepted (can not be plausible nor correct)
    return False


def get_ablation_only_build_step_is_still_plausible(instance_id: int, experiment_number: int, classification: str, plausible_fix: bool) -> bool:
    if not plausible_fix:
        # If there is no plausible_fix with ChangeApprover then there also isn't one without ChangeApprover
        return False
    
    if not os.path.exists(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "fix_" + str(classification).lower(), "implausible_patches")):
        # If there is no implausible_patches folder then there also isn't an implausible patch that would have been wrongly accepted without ChangeApprover. So return True in this case.
        return True
    try:
        implausible_patch_file_name = next(f for f in os.listdir(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "fix_" + str(classification).lower(), "implausible_patches"))
                                           if os.path.isfile(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "fix_" + str(classification).lower(), "implausible_patches", f)) and f.startswith(str(instance_id) + "_"))
    except StopIteration:
        # No implausible_patches file was found. This means that the first fix attempt was already a plausible_fix.
        # Therefore return True in this case.
        return True

    implausible_patch_file_path = os.path.join("experimental_setups", "experiment_" + str(
        experiment_number), "fix_" + str(classification).lower(), "implausible_patches", implausible_patch_file_name)

    with open(implausible_patch_file_path, "r") as f:
        implausbile_file_content = f.read()

    pattern = r"(### IMPLAUSIBLE FIX \(fix no\. \d+\)[\s\S]*?)(?=### IMPLAUSIBLE FIX|\Z)"
    matches = re.findall(pattern, implausbile_file_content)
    for match in matches:
        if match.find("Project was successfully built with the applied changes.") != -1:
            # If a implausible fix with the successful build message was found, then this means that the fix must have failed at some later step.
            # Without the later steps this fix would have been accepted but it is not plausible (as the later steps failed). Therefore return False here.
            return False

    # If no implausible fix with successful built was found then this means that the plausible_fix is the first one that the ablated version would have accepted.
    # So only the build step would have found this same plausible fix.
    return True


def get_ablation_build_step_and_sonar_qube_check_is_still_plausible(instance_id: int, experiment_number: int, classification: str, plausible_fix: bool) -> bool:
    if not plausible_fix:
        # If there is no plausible_fix with ChangeApprover then there also isn't one without ChangeApprover
        return False
    
    if not os.path.exists(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "fix_" + str(classification).lower(), "implausible_patches")):
        # If there is no implausible_patches folder then there also isn't an implausible patch that would have been wrongly accepted without ChangeApprover. So return True in this case.
        return True
    try:
        implausible_patch_file_name = next(f for f in os.listdir(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "fix_" + str(classification).lower(), "implausible_patches"))
                                           if os.path.isfile(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "fix_" + str(classification).lower(), "implausible_patches", f)) and f.startswith(str(instance_id) + "_"))
    except StopIteration:
        # No implausible_patches file was found. This means that the first fix attempt was already a plausible_fix.
        # Therefore return True in this case.
        return True

    implausible_patch_file_path = os.path.join("experimental_setups", "experiment_" + str(
        experiment_number), "fix_" + str(classification).lower(), "implausible_patches", implausible_patch_file_name)

    with open(implausible_patch_file_path, "r") as f:
        implausbile_file_content = f.read()

    pattern = r"(### IMPLAUSIBLE FIX \(fix no\. \d+\)[\s\S]*?)(?=### IMPLAUSIBLE FIX|\Z)"
    matches = re.findall(pattern, implausbile_file_content)
    for match in matches:
        if match.find("Rerunning the SonarQube analysis confirmed that your fix successfully removed the targeted rule violation and didn't introduce any new violations.") != -1:
            # If a implausible fix with the successful SonarQube check was found, then this means that the fix must have failed at the later step of running tests.
            # Without the later step this fix would have been accepted but it is not plausible (as the later step must have failed). Therefore return False here.
            return False

    # If no implausible fix with successful SonarQube check was found, then this means that the plausible_fix is the first one that the ablated version would have accepted.
    # So the two first steps would have found this same plausible fix.
    return True


def get_execution_time(instance_id: int, experiment_number: int, target_phase: str):
    if target_phase == "classification":
        target_folder = "classification"
    elif target_phase == "fixTP":
        target_folder = "fix_tp"
    elif target_phase == "fixFP":
        target_folder = "fix_fp"

    try:
        execution_info_file_name = next(f for f in os.listdir(os.path.join("experimental_setups", "experiment_" + str(experiment_number), target_folder, "execution_info"))
                                        if os.path.isfile(os.path.join("experimental_setups", "experiment_" + str(experiment_number), target_folder, "execution_info", f)) and f.startswith(str(instance_id) + "_"))
    except StopIteration:
        assert target_folder != "classification"
        return 0

    execution_info_file_path = os.path.join("experimental_setups", "experiment_" + str(
        experiment_number), target_folder, "execution_info", execution_info_file_name)

    with open(execution_info_file_path, "r") as f:
        execution_info_file_content = f.read()

    pattern = r"!! Start up timestamp: (\d+)"
    start_time_match = re.search(
        pattern, execution_info_file_content)
    start_time = int(start_time_match.group(1).strip())

    pattern = r"!! Shutdown timestamp: (\d+)"
    end_time_match = re.search(
        pattern, execution_info_file_content)
    end_time = int(end_time_match.group(1).strip())

    return end_time - start_time


def get_cost(tokens_input_uncached: int, tokens_input_cached: int, tokens_output: int, model="gpt-4.1-mini-2025-04-14"):
    prompt_token_cost_uncached = OPEN_AI_CHAT_MODELS[model].prompt_token_cost
    prompt_token_cost_cached = OPEN_AI_CHAT_MODELS[model].prompt_token_cost_cached
    output_token_cost = OPEN_AI_CHAT_MODELS[model].completion_token_cost

    total_tokens_cost = (tokens_input_uncached / 1000) * prompt_token_cost_uncached + (
        tokens_input_cached / 1000) * prompt_token_cost_cached + (tokens_output / 1000) * output_token_cost

    return total_tokens_cost


def get_tokens(instance_id: int, experiment_number: int, classification: str, target_phase: str) -> pd.Series:
    col_names = ["tokensInputUncached" + target_phase.capitalize(), "tokensInputCached" +
                 target_phase.capitalize(), "tokensOutput" + target_phase.capitalize()]
    if target_phase == "classification":
        folder_of_phase = "classification"
    elif target_phase.startswith("fix"):
        if target_phase.endswith("TP") and classification != "TP":
            return pd.Series([0, 0, 0], index=col_names)
        if target_phase.endswith("FP") and classification != "FP":
            return pd.Series([0, 0, 0], index=col_names)

        folder_of_phase = "fix_" + str(classification).lower()
    try:
        execution_info_file_name = next(f for f in os.listdir(os.path.join("experimental_setups", "experiment_" + str(experiment_number), folder_of_phase, "execution_info"))
                                        if os.path.isfile(os.path.join("experimental_setups", "experiment_" + str(experiment_number), folder_of_phase, "execution_info", f)) and f.startswith(str(instance_id) + "_"))
    except StopIteration:
        print(
            f"No responses file found in {folder_of_phase} for ID {str(instance_id)} in experiment {str(experiment_number)}. This must be a bug.")
        return pd.Series([0, 0, 0], index=col_names)

    execution_info_file_path = os.path.join("experimental_setups", "experiment_" + str(
        experiment_number), folder_of_phase, "execution_info", execution_info_file_name)

    with open(execution_info_file_path, "r") as f:
        execution_info_file_content = f.read()

    pattern = r"!! AI Chat Completion prompt_tokens_uncached: (\d+)"
    input_tokens_uncached_match_iter = re.finditer(
        pattern, execution_info_file_content)
    tokens_input_uncached = 0
    for match in input_tokens_uncached_match_iter:
        tokens_input_uncached += int(match.group(1).strip())

    pattern = r"!! AI Chat Completion prompt_tokens_cached: (\d+)"
    input_tokens_cached_match_iter = re.finditer(
        pattern, execution_info_file_content)
    tokens_input_cached = 0
    for match in input_tokens_cached_match_iter:
        tokens_input_cached += int(match.group(1).strip())

    pattern = r"!! AI Chat Completion completion_tokens: (\d+)"
    output_tokens_match_iter = re.finditer(
        pattern, execution_info_file_content)
    tokens_output = 0
    for match in output_tokens_match_iter:
        tokens_output += int(match.group(1).strip())

    return pd.Series(
        [tokens_input_uncached,
         tokens_input_cached,
         tokens_output], index=col_names)


def get_iterations_in_classification(instance_id, experiment_number) -> int:
    try:
        responses_file_name = next(f for f in os.listdir(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "classification", "responses"))
                                   if os.path.isfile(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "classification", "responses", f)) and f.startswith(str(instance_id) + "_"))
    except StopIteration:
        print(
            f"No responses file found in classification for ID {str(instance_id)} in experiment {str(experiment_number)}. This must be a bug.")
        return 0

    responses_file_path = os.path.join("experimental_setups", "experiment_" + str(
        experiment_number), "classification", "responses", responses_file_name)

    with open(responses_file_path, "r") as f:
        responses_file_content = f.read()

    pattern = r"============== ChatSequence ==============\nLength: (\d+) tokens; \d+ messages"
    matches = re.findall(pattern, responses_file_content)

    return len(matches)


def get_implausible_fixes_count(instance_id, experiment_number, classification) -> int:
    if not os.path.exists(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "fix_" + str(classification).lower(), "implausible_patches")):
        return 0
    try:
        implausible_patch_file_name = next(f for f in os.listdir(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "fix_" + str(classification).lower(), "implausible_patches"))
                                           if os.path.isfile(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "fix_" + str(classification).lower(), "implausible_patches", f)) and f.startswith(str(instance_id) + "_"))
    except StopIteration:
        return 0

    implausible_patch_file_path = os.path.join("experimental_setups", "experiment_" + str(
        experiment_number), "fix_" + str(classification).lower(), "implausible_patches", implausible_patch_file_name)

    with open(implausible_patch_file_path, "r") as f:
        implausbile_file_content = f.read()

    pattern = r"(### IMPLAUSIBLE FIX \(fix no\. \d+\)[\s\S]*?)(?=### IMPLAUSIBLE FIX|\Z)"
    matches = re.findall(pattern, implausbile_file_content)
    return len(matches)


def get_plausible_fixes_count(instance_id, experiment_number, classification) -> int:
    if not os.path.exists(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "fix_" + str(classification).lower(), "plausible_patches")):
        return 0
    try:
        plausible_patch_file_name = next(f for f in os.listdir(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "fix_" + str(classification).lower(), "plausible_patches"))
                                         if os.path.isfile(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "fix_" + str(classification).lower(), "plausible_patches", f)) and f.startswith(str(instance_id) + "_"))
    except StopIteration:
        return 0

    plausible_patch_file_path = os.path.join("experimental_setups", "experiment_" + str(
        experiment_number), "fix_" + str(classification).lower(), "plausible_patches", plausible_patch_file_name)

    with open(plausible_patch_file_path, "r") as f:
        plausbile_file_content = f.read()

    pattern = r"(### PLAUSIBLE FIX \(fix no\. \d+\)[\s\S]*?)(?=### PLAUSIBLE FIX|\Z)"
    matches = re.findall(pattern, plausbile_file_content)
    return len(matches)


def get_iterations_in_fix_x(instance_id, experiment_number, classification, target_classification) -> int:
    if classification != target_classification:
        return -1
    try:
        responses_file_name = next(f for f in os.listdir(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "fix_" + str(classification).lower(), "responses"))
                                   if os.path.isfile(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "fix_" + str(classification).lower(), "responses", f)) and f.startswith(str(instance_id) + "_"))
    except StopIteration:
        print(
            f"No responses file found in classification for ID {str(instance_id)} in experiment {str(experiment_number)}. This must be a bug.")
        return 0

    responses_file_path = os.path.join("experimental_setups", "experiment_" + str(
        experiment_number), "fix_" + str(classification).lower(), "responses", responses_file_name)

    with open(responses_file_path, "r") as f:
        responses_file_content = f.read()

    pattern = r"============== ChatSequence ==============\nLength: (\d+) tokens; \d+ messages"
    matches = re.findall(pattern, responses_file_content)

    return len(matches)


def get_compilation_passed(instance_id, experiment_number, classification, plausible_fix) -> bool:
    if plausible_fix:
        return True
    try:
        implausible_patch_file_name = next(f for f in os.listdir(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "fix_" + str(classification).lower(), "implausible_patches"))
                                           if os.path.isfile(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "fix_" + str(classification).lower(), "implausible_patches", f)) and f.startswith(str(instance_id) + "_"))
    except StopIteration:
        print(
            f"No implausible patches found. But also no plausible patches for ID {str(instance_id)} in experiment {str(experiment_number)}. This might be a bug.")
        return False

    implausible_patch_file_path = os.path.join("experimental_setups", "experiment_" + str(
        experiment_number), "fix_" + str(classification).lower(), "implausible_patches", implausible_patch_file_name)

    with open(implausible_patch_file_path, "r") as f:
        implausbile_file_content = f.read()

    pattern = r"(### IMPLAUSIBLE FIX \(fix no\. \d+\)[\s\S]*?)(?=### IMPLAUSIBLE FIX|\Z)"
    matches = re.findall(pattern, implausbile_file_content)
    for match in matches:
        if match.find("Project was successfully built with the applied changes.") != -1:
            return True
    return False


def get_sonarqube_check_passed(instance_id, experiment_number, classification, plausible_fix) -> bool:
    if plausible_fix:
        return True
    try:
        implausible_patch_file_name = next(f for f in os.listdir(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "fix_" + str(classification).lower(), "implausible_patches"))
                                           if os.path.isfile(os.path.join("experimental_setups", "experiment_" + str(experiment_number), "fix_" + str(classification).lower(), "implausible_patches", f)) and f.startswith(str(instance_id) + "_"))
    except StopIteration:
        print(
            f"No implausible patches found. But also no plausible patches for ID {str(instance_id)} in experiment {str(experiment_number)}. This might be a bug.")
        return False

    implausible_patch_file_path = os.path.join("experimental_setups", "experiment_" + str(
        experiment_number), "fix_" + str(classification).lower(), "implausible_patches", implausible_patch_file_name)

    with open(implausible_patch_file_path, "r") as f:
        implausbile_file_content = f.read()

    pattern = r"(### IMPLAUSIBLE FIX \(fix no\. \d+\)[\s\S]*?)(?=### IMPLAUSIBLE FIX|\Z)"
    matches = re.findall(pattern, implausbile_file_content)
    for match in matches:
        if match.find("Rerunning the SonarQube analysis confirmed that your fix successfully removed the targeted rule violation and didn't introduce any new violations.") != -1:
            return True
    return False


if __name__ == "__main__":
    extend_evaluation_results_with_more_stats()

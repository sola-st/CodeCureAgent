
# Workaround to have access to the autogpt package
import sys
from pathlib import Path
sys.path.append(str(Path(__file__).parent.parent))
from agent_core.llm.providers.openai import OPEN_AI_CHAT_MODELS
import mdutils
import re
import os


def analyze_general_stats():
    with open('experimental_setups/experiments_list.txt', 'r') as experiments_list_file:
        experiment_folders = experiments_list_file.read().splitlines()

    total_rule_violations, classified_tp, classified_fp, unclassified = calc_classification_stats(
        experiment_folders)

    print(
        f"Total rule violations: {total_rule_violations}")
    print(
        f"Classification: TP: {classified_tp}; FP: {classified_fp}; Unclassified: {unclassified}")

    tp_plausible_fix, tp_no_plausible_fix, fp_plausible_fix, fp_no_plausible_fix, total_plausible_fix, total_no_plausible_fix = calc_plausible_fix_stats(
        experiment_folders, classified_tp, classified_fp)

    print(
        f"Fixing:\nTotal plausible fixes: {total_plausible_fix}/{total_rule_violations}")
    print(f"TP plausible fixes: {tp_plausible_fix}/{classified_tp}")
    print(f"FP plausible fixes: {fp_plausible_fix}/{classified_fp}")

    total_execution_time, execution_time_classification, execution_time_fix_tp, execution_time_fix_fp, avg_execution_time, avg_execution_time_classification, avg_execution_time_fix_tp, avg_execution_time_fix_fp = calc_total_execution_time(
        experiment_folders, classified_tp, classified_fp, total_rule_violations)

    total_tokens_count, tokens_count_classification, tokens_count_fix_tp, tokens_count_fix_fp, total_tokens_cost, tokens_cost_classification, tokens_cost_fix_tp, tokens_cost_fix_fp, avg_cost, avg_cost_classification, avg_cost_fix_tp, avg_cost_fix_fp = calc_total_cost(
        experiment_folders, classified_tp, classified_fp, total_rule_violations)

    write_to_markdown(total_rule_violations, classified_tp, classified_fp,
                      unclassified, tp_plausible_fix, fp_plausible_fix, total_plausible_fix, total_execution_time, execution_time_classification, execution_time_fix_tp, execution_time_fix_fp, avg_execution_time, avg_execution_time_classification, avg_execution_time_fix_tp, avg_execution_time_fix_fp, total_tokens_count, tokens_count_classification, tokens_count_fix_tp, tokens_count_fix_fp, total_tokens_cost, tokens_cost_classification, tokens_cost_fix_tp, tokens_cost_fix_fp, avg_cost, avg_cost_classification, avg_cost_fix_tp, avg_cost_fix_fp)


def write_to_markdown(total_rule_violations, classified_tp, classified_fp, unclassified, tp_plausible_fix, fp_plausible_fix, total_plausible_fix, total_execution_time, execution_time_classification, execution_time_fix_tp, execution_time_fix_fp, avg_execution_time, avg_execution_time_classification, avg_execution_time_fix_tp, avg_execution_time_fix_fp, total_tokens_count, tokens_count_classification, tokens_count_fix_tp, tokens_count_fix_fp, total_tokens_cost, tokens_cost_classification, tokens_cost_fix_tp, tokens_cost_fix_fp, avg_cost, avg_cost_classification, avg_cost_fix_tp, avg_cost_fix_fp):
    mdFile = mdutils.MdUtils(
        file_name='experimental_setups/analysis_results_overview', title='Experiment Analysis Results', title_header_style="atx")
    mdFile.new_header(level=2, title="Overall stats",
                      add_table_of_contents="n")
    mdFile.new_header(level=3, title="Total rule violations",
                      add_table_of_contents="n")
    mdFile.new_line(str(total_rule_violations))

    mdFile.new_header(level=3, title="Classification",
                      add_table_of_contents="n")
    mdFile.new_line(
        f"TP: {classified_tp}  \nFP: {classified_fp}  \nUnclassified: {unclassified}")

    mdFile.new_header(level=3, title="Plausible Fixes",
                      add_table_of_contents="n")
    mdFile.new_line(
        f"Total plausible fixes: {total_plausible_fix}/{total_rule_violations}  ")
    mdFile.new_line(
        f"TP plausible fixes: {tp_plausible_fix}/{classified_tp}  ")
    mdFile.new_line(
        f"FP plausible fixes: {fp_plausible_fix}/{classified_fp}  ")

    mdFile.new_header(level=3, title="Execution time",
                      add_table_of_contents="n")
    mdFile.new_line("Total execution time: " +
                    str(nano_to_second(total_execution_time) / 60) + " minutes  ")
    mdFile.new_line("Execution time by sub-agent:  ")
    mdFile.new_line("Classification: " +
                    str(nano_to_second(execution_time_classification) / 60) + " minutes  ")
    mdFile.new_line(
        "Fix_TP: " + str(nano_to_second(execution_time_fix_tp) / 60) + " minutes  ")
    mdFile.new_line(
        "Fix_FP: " + str(nano_to_second(execution_time_fix_fp) / 60) + " minutes  ")

    mdFile.new_line()
    mdFile.new_line("Average execution time: " +
                    str(nano_to_second(avg_execution_time) / 60) + " minutes  ")
    mdFile.new_line("Average execution time by sub-agent:  ")
    mdFile.new_line("Classification: " +
                    str(nano_to_second(avg_execution_time_classification) / 60) + " minutes  ")
    mdFile.new_line(
        "Fix_TP: " + str(nano_to_second(avg_execution_time_fix_tp) / 60) + " minutes  ")
    mdFile.new_line(
        "Fix_FP: " + str(nano_to_second(avg_execution_time_fix_fp) / 60) + " minutes  ")

    mdFile.new_header(level=3, title="Cost",
                      add_table_of_contents="n")

    mdFile.new_header(level=4, title="Tokens Count",
                      add_table_of_contents="n")
    mdFile.new_line("Total tokens count: " + str(total_tokens_count) + "  ")
    mdFile.new_line("Tokens by sub-agent:  ")
    mdFile.new_line("Classification: " +
                    str(tokens_count_classification) + "  ")
    mdFile.new_line("Fix_TP: " + str(tokens_count_fix_tp) + "  ")
    mdFile.new_line("Fix_FP: " + str(tokens_count_fix_fp) + "  ")

    mdFile.new_header(level=4, title="Tokens Cost",
                      add_table_of_contents="n")
    mdFile.new_line("Total Cost: " + str(total_tokens_cost) + " USD  ")
    mdFile.new_line("Cost by sub-agent:  ")
    mdFile.new_line("Classification: " +
                    str(tokens_cost_classification) + " USD  ")
    mdFile.new_line("Fix_TP: " + str(tokens_cost_fix_tp) + " USD  ")
    mdFile.new_line("Fix_FP: " + str(tokens_cost_fix_fp) + " USD  ")

    mdFile.new_header(level=4, title="Average Tokens Cost",
                      add_table_of_contents="n")
    mdFile.new_line("Average Total Cost: " + str(avg_cost) + " USD  ")
    mdFile.new_line("Average cost by sub-agent:  ")
    mdFile.new_line("Classification: " +
                    str(avg_cost_classification) + " USD  ")
    mdFile.new_line("Fix_TP: " + str(avg_cost_fix_tp) + " USD  ")
    mdFile.new_line("Fix_FP: " + str(avg_cost_fix_fp) + " USD  ")

    mdFile.create_md_file()


def calc_classification_stats(experiment_folders: list[str]):
    # TODO: Save classification and plausible or not for each sample in an array
    classified_tp = 0
    classified_fp = 0
    unclassified = 0

    for experiment_folder in experiment_folders:
        classification_prompt_history_folder = os.path.join(
            "experimental_setups", experiment_folder, "classification", 'prompt_history')

        classification_prompt_history_files = [f for f in os.listdir(
            classification_prompt_history_folder) if os.path.isfile(os.path.join(classification_prompt_history_folder, f))]

        classification_folder = os.path.join(
            "experimental_setups", experiment_folder, "classification")

        classification_result_files = [f for f in os.listdir(
            classification_folder) if os.path.isfile(os.path.join(classification_folder, f)) and f != "parsing_errors_responses.txt"]

        unclassified += len(classification_prompt_history_files) - \
            len(classification_result_files)

        for classification_result_file in classification_result_files:
            with open(os.path.join(classification_folder, classification_result_file)) as crf:
                file_cont = crf.read()
            index_final_verdict = file_cont.rfind("Final Verdict:  ")
            if index_final_verdict == -1:
                print(
                    f"ERROR: 'Final Verdict:  ' not found in file {os.path.join(classification_folder, classification_result_file)}")
                continue
            final_verdict = file_cont[index_final_verdict +
                                      17:index_final_verdict + 19]

            if final_verdict == "TP":
                classified_tp += 1
            elif final_verdict == "FP":
                classified_fp += 1
            else:
                print(
                    f"ERROR: Unknown Final Verdict '{final_verdict}' in file {os.path.join(classification_folder, classification_result_file)}.")

    total_rule_violations = classified_tp + classified_fp + unclassified

    return total_rule_violations, classified_tp, classified_fp, unclassified


def calc_plausible_fix_stats(experiment_folders: list[str], classified_tp: int, classified_fp: int):
    tp_plausible_fix = 0

    fp_plausible_fix = 0

    total_plausible_fix = 0

    for experiment_folder in experiment_folders:
        fix_tp_plausible_patches_folder = os.path.join(
            "experimental_setups", experiment_folder, "fix_tp", 'plausible_patches')

        fix_tp_plausible_patches_files = [f for f in os.listdir(
            fix_tp_plausible_patches_folder) if os.path.isfile(os.path.join(fix_tp_plausible_patches_folder, f))]

        tp_plausible_fix += len(fix_tp_plausible_patches_files)

        fix_fp_plausible_patches_folder = os.path.join(
            "experimental_setups", experiment_folder, "fix_fp", 'plausible_patches')

        fix_fp_plausible_patches_files = [f for f in os.listdir(
            fix_fp_plausible_patches_folder) if os.path.isfile(os.path.join(fix_fp_plausible_patches_folder, f))]

        fp_plausible_fix += len(fix_fp_plausible_patches_files)

    tp_no_plausible_fix = classified_tp - tp_plausible_fix
    fp_no_plausible_fix = classified_fp - fp_plausible_fix

    total_plausible_fix = tp_plausible_fix + fp_plausible_fix
    total_no_plausible_fix = (
        classified_tp + classified_fp) - total_plausible_fix

    return tp_plausible_fix, tp_no_plausible_fix, fp_plausible_fix, fp_no_plausible_fix, total_plausible_fix, total_no_plausible_fix


def calc_total_execution_time(experiment_folders: list[str], classified_tp: int, classified_fp: int, total_rule_violations: int):
    # TODO: Save execution time of each sample in an array, to plot distribution and calc. mean
    # TODO: Also execution time for plausible/not plausible

    execution_time_classification = 0
    execution_time_fix_tp = 0
    execution_time_fix_fp = 0

    for experiment_folder in experiment_folders:

        # classification
        classification_execution_info_folder = os.path.join(
            "experimental_setups", experiment_folder, "classification", "execution_info")

        classification_execution_info_files = [f for f in os.listdir(
            classification_execution_info_folder) if os.path.isfile(os.path.join(classification_execution_info_folder, f))]

        for classification_execution_info_file in classification_execution_info_files:
            with open(os.path.join("experimental_setups", experiment_folder, "classification", "execution_info", classification_execution_info_file)) as crf:
                execution_info_lines = crf.readlines()

            execution_time_classification += retrieve_execution_time_single_phase(
                classification_execution_info_file, execution_info_lines)

        # fix_tp
        fix_tp_execution_info_folder = os.path.join(
            "experimental_setups", experiment_folder, "fix_tp", 'execution_info')

        fix_tp_execution_info_files = [f for f in os.listdir(
            fix_tp_execution_info_folder) if os.path.isfile(os.path.join(fix_tp_execution_info_folder, f))]

        for fix_tp_execution_info_file in fix_tp_execution_info_files:
            with open(os.path.join("experimental_setups", experiment_folder, "fix_tp", "execution_info", fix_tp_execution_info_file)) as crf:
                fix_tp_execution_info_lines = crf.readlines()

            execution_time_fix_tp += retrieve_execution_time_single_phase(
                fix_tp_execution_info_file, fix_tp_execution_info_lines)

        # fix_fp
        fix_fp_execution_info_folder = os.path.join(
            "experimental_setups", experiment_folder, "fix_fp", 'execution_info')

        fix_fp_execution_info_files = [f for f in os.listdir(
            fix_fp_execution_info_folder) if os.path.isfile(os.path.join(fix_fp_execution_info_folder, f))]

        for fix_fp_execution_info_file in fix_fp_execution_info_files:
            with open(os.path.join("experimental_setups", experiment_folder, "fix_fp", "execution_info", fix_fp_execution_info_file)) as crf:
                fix_fp_execution_info_lines = crf.readlines()

            execution_time_fix_fp += retrieve_execution_time_single_phase(
                fix_fp_execution_info_file, fix_fp_execution_info_lines)

    total_execution_time = execution_time_classification + \
        execution_time_fix_tp + execution_time_fix_fp

    print("Total Execution Time " +
          str(nano_to_second(total_execution_time) / 60) + " minutes")
    if total_rule_violations != 0:
        avg_execution_time = total_execution_time / total_rule_violations
    else:
        avg_execution_time = 0

    print("Execution Time Classification " +
          str(nano_to_second(execution_time_classification) / 60) + " minutes")
    print("Execution Time Fix TP " +
          str(nano_to_second(execution_time_fix_tp) / 60) + " minutes")
    print("Execution Time Fix FP " +
          str(nano_to_second(execution_time_fix_fp) / 60) + " minutes")

    print("Average Execution Time " +
          str(nano_to_second(avg_execution_time) / 60) + " minutes")

    if total_rule_violations != 0:
        avg_execution_time_classification = execution_time_classification / total_rule_violations
    else:
        avg_execution_time_classification = 0

    print("Average Execution Time Classification " +
          str(nano_to_second(avg_execution_time_classification) / 60) + " minutes")

    if classified_tp != 0:
        avg_execution_time_fix_tp = execution_time_fix_tp / classified_tp
    else:
        avg_execution_time_fix_tp = 0

    print("Average Execution Time Fix TP " +
          str(nano_to_second(avg_execution_time_fix_tp) / 60) + " minutes")

    if classified_fp != 0:
        avg_execution_time_fix_fp = execution_time_fix_fp / classified_fp
    else:
        avg_execution_time_fix_fp = 0

    print("Average Execution Time Fix FP " +
          str(nano_to_second(avg_execution_time_fix_fp) / 60) + " minutes")

    return total_execution_time, execution_time_classification, execution_time_fix_tp, execution_time_fix_fp, avg_execution_time, avg_execution_time_classification, avg_execution_time_fix_tp, avg_execution_time_fix_fp


def nano_to_second(time_in_nano: float):
    return int(time_in_nano / 1000000000)


def retrieve_execution_time_single_phase(filename: str, execution_info_lines: list[str]):

    start_time = -1
    end_time = -1
    for line in execution_info_lines:
        if line.startswith("!! Start up timestamp: "):
            start_time = int(line.removeprefix("!! Start up timestamp: "))
        if line.startswith("!! Shutdown timestamp: "):
            end_time = int(line.removeprefix("!! Shutdown timestamp: "))

    if start_time == -1:
        print(f"ERROR: No startup time in {filename}")
        return 0
    if end_time == -1:
        print(f"ERROR: No shutdown time in {filename}")
        # Use last timestamp of any kind as fallback
        last_line_with_any_time = execution_info_lines[-1]
        end_time = int(
            last_line_with_any_time[last_line_with_any_time.find("timestamp: ") + 11:])

    return end_time - start_time


def calc_total_cost(experiment_folders: list[str], classified_tp: int, classified_fp: int, total_rule_violations: int, model="gpt-4.1-mini-2025-04-14"):
    # The cost is a very unprecise upper bound. In reality the cost is much lower.
    # This is because cached input is much cheaper and ca. 5/6 are cached, as the prompt is largely always the same!

    # => TODO: use the info about AI usage in the execution_info file instead

    # TODO: Calculate tokens also grouped by plausible /no plausible

    tokens_count_classification_input = 0
    tokens_count_classification_output = 0
    tokens_count_fix_tp_input = 0
    tokens_count_fix_tp_output = 0
    tokens_count_fix_fp_input = 0
    tokens_count_fix_fp_output = 0

    for experiment_folder in experiment_folders:
        # The prompt_history holds the prompts to the model
        # The responses holds the responses of the model to the prompts
        classification_prompt_history_folder = os.path.join(
            "experimental_setups", experiment_folder, "classification", 'prompt_history')

        classification_prompt_history_files = [f for f in os.listdir(
            classification_prompt_history_folder) if os.path.isfile(os.path.join(classification_prompt_history_folder, f))]

        classification_responses_folder = os.path.join(
            "experimental_setups", experiment_folder, "classification", 'responses')

        classification_responses_files = [f for f in os.listdir(
            classification_responses_folder) if os.path.isfile(os.path.join(classification_responses_folder, f))]

        fix_tp_prompt_history_folder = os.path.join(
            "experimental_setups", experiment_folder, "fix_tp", 'prompt_history')

        fix_tp_prompt_history_files = [f for f in os.listdir(
            fix_tp_prompt_history_folder) if os.path.isfile(os.path.join(fix_tp_prompt_history_folder, f))]

        fix_tp_responses_folder = os.path.join(
            "experimental_setups", experiment_folder, "fix_tp", 'responses')

        fix_tp_responses_files = [f for f in os.listdir(
            fix_tp_responses_folder) if os.path.isfile(os.path.join(fix_tp_responses_folder, f))]

        fix_fp_prompt_history_folder = os.path.join(
            "experimental_setups", experiment_folder, "fix_fp", 'prompt_history')

        fix_fp_prompt_history_files = [f for f in os.listdir(
            fix_fp_prompt_history_folder) if os.path.isfile(os.path.join(fix_fp_prompt_history_folder, f))]

        fix_fp_responses_folder = os.path.join(
            "experimental_setups", experiment_folder, "fix_fp", 'responses')

        fix_fp_responses_files = [f for f in os.listdir(
            fix_fp_responses_folder) if os.path.isfile(os.path.join(fix_fp_responses_folder, f))]

        for classification_prompt_history_file in classification_prompt_history_files:
            with open(os.path.join("experimental_setups", experiment_folder, "classification", "prompt_history", classification_prompt_history_file)) as crf:
                file_content = crf.read()
            tokens_count_classification_input += retrieve_tokens_count(
                file_content)

        for classification_responses_file in classification_responses_files:
            with open(os.path.join("experimental_setups", experiment_folder, "classification", "responses", classification_responses_file)) as crf:
                file_content = crf.read()
            tokens_count_classification_output += retrieve_tokens_count(
                file_content)

        for fix_tp_prompt_history_file in fix_tp_prompt_history_files:
            with open(os.path.join("experimental_setups", experiment_folder, "fix_tp", "prompt_history", fix_tp_prompt_history_file)) as crf:
                file_content = crf.read()
            tokens_count_fix_tp_input += retrieve_tokens_count(
                file_content)

        for fix_tp_responses_file in fix_tp_responses_files:
            with open(os.path.join("experimental_setups", experiment_folder, "fix_tp", "responses", fix_tp_responses_file)) as crf:
                file_content = crf.read()
            tokens_count_fix_tp_output += retrieve_tokens_count(
                file_content)

        for fix_fp_prompt_history_file in fix_fp_prompt_history_files:
            with open(os.path.join("experimental_setups", experiment_folder, "fix_fp", "prompt_history", fix_fp_prompt_history_file)) as crf:
                file_content = crf.read()
            tokens_count_fix_fp_input += retrieve_tokens_count(
                file_content)

        for fix_fp_responses_file in fix_fp_responses_files:
            with open(os.path.join("experimental_setups", experiment_folder, "fix_fp", "responses", fix_fp_responses_file)) as crf:
                file_content = crf.read()
            tokens_count_fix_fp_output += retrieve_tokens_count(
                file_content)

    tokens_count_classification = tokens_count_classification_input + \
        tokens_count_classification_output
    tokens_count_fix_tp = tokens_count_fix_tp_input + tokens_count_fix_tp_output
    tokens_count_fix_fp = tokens_count_fix_fp_input + tokens_count_fix_fp_output

    total_tokens_count_input = tokens_count_classification_input + \
        tokens_count_fix_tp_input + tokens_count_fix_fp_input

    total_tokens_count_output = tokens_count_classification_output + \
        tokens_count_fix_tp_output + tokens_count_fix_fp_output

    total_tokens_count = total_tokens_count_input + total_tokens_count_output

    prompt_token_cost = OPEN_AI_CHAT_MODELS[model].prompt_token_cost
    output_token_cost = OPEN_AI_CHAT_MODELS[model].completion_token_cost

    total_tokens_cost = (total_tokens_count_input / 1000) * prompt_token_cost + \
        (total_tokens_count_output / 1000) * output_token_cost
    tokens_cost_classification = (
        tokens_count_classification_input / 1000) * prompt_token_cost + (tokens_count_classification_output / 1000) * output_token_cost

    tokens_cost_fix_tp = (tokens_count_fix_tp_input / 1000) * prompt_token_cost + \
        (tokens_count_fix_tp_output / 1000) * output_token_cost
    tokens_cost_fix_fp = (tokens_count_fix_fp_input / 1000) * prompt_token_cost + \
        (tokens_count_fix_fp_input / 1000) * output_token_cost

    print("total_tokens_count: " + str(total_tokens_count))
    print("tokens_count_classification: " + str(tokens_count_classification))
    print("tokens_count_fix_tp: " + str(tokens_count_fix_tp))
    print("tokens_count_fix_fp: " + str(tokens_count_fix_fp))
    print("total_tokens_cost: " + str(total_tokens_cost) + " USD")
    print("tokens_cost_classification: " +
          str(tokens_cost_classification) + " USD")
    print("tokens_cost_fix_tp: " + str(tokens_cost_fix_tp) + " USD")
    print("tokens_cost_fix_fp: " + str(tokens_cost_fix_fp) + " USD")

    if total_rule_violations != 0:
        avg_cost = total_tokens_cost / total_rule_violations
    else:
        avg_cost = 0
    if total_rule_violations != 0:
        avg_cost_classification = tokens_cost_classification / total_rule_violations
    else:
        avg_cost_classification = 0
    if classified_tp != 0:
        avg_cost_fix_tp = tokens_cost_fix_tp / classified_tp
    else:
        avg_cost_fix_tp = 0

    if classified_fp != 0:
        avg_cost_fix_fp = tokens_cost_fix_fp / classified_fp
    else:
        avg_cost_fix_fp = 0

    print("avg_total_tokens_cost: " + str(avg_cost) + " USD")
    print("avg_tokens_cost_classification: " +
          str(avg_cost_classification) + " USD")
    print("avg_tokens_cost_fix_tp: " + str(avg_cost_fix_tp) + " USD")
    print("avg_tokens_cost_fix_fp: " + str(avg_cost_fix_fp) + " USD")

    return total_tokens_count, tokens_count_classification, tokens_count_fix_tp, tokens_count_fix_fp, total_tokens_cost, tokens_cost_classification, tokens_cost_fix_tp, tokens_cost_fix_fp, avg_cost, avg_cost_classification, avg_cost_fix_tp, avg_cost_fix_fp


def retrieve_tokens_count(file_content: str):
    token_count = 0

    token_match_iter = re.finditer(
        r"============== ChatSequence ==============\nLength: (\d+) tokens; \d+ messages", file_content)
    for match in token_match_iter:
        token_count += int(match.group(1).strip())

    return token_count


if __name__ == "__main__":

    analyze_general_stats()

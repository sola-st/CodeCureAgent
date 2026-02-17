#!/usr/bin/env python3

import click
import pandas as pd
import mdutils


@click.command()
@click.argument(
    "ismell-evaluation-results-file",
    type=click.File()
)
@click.option(
    "--target-md-file-path",
    "-t",
    default="./analysis_results_overview_ismell.md",
    help="Path where the stats file should be written."
)
def calculate_ismell_stats_from_evaluation_results(ismell_evaluation_results_file: click.File, target_md_file_path: str):

    evaluation_results_file_df = pd.read_csv(ismell_evaluation_results_file)

    mdFile = mdutils.MdUtils(
        file_name=target_md_file_path, title='iSMELL Comparison Analysis Results', title_header_style="atx")

    mdFile.new_header(level=2, title="Total warnings in comparison",
                      add_table_of_contents="n")

    total_rule_violations = evaluation_results_file_df["instanceID"].count()

    mdFile.new_line(
        str(total_rule_violations))

    # ---- CodeCureAgent stats on the samples that are compared

    mdFile.new_header(level=2, title="CodeCureAgent stats on the compared warnings",
                      add_table_of_contents="n")

    # CCA Classification section

    mdFile.new_header(level=3, title="CodeCureAgent Classification",
                      add_table_of_contents="n")
    classified_tp = (
        evaluation_results_file_df["classification"] == "TP").sum()
    classified_fp = (
        evaluation_results_file_df["classification"] == "FP").sum()
    unclassified = (
        evaluation_results_file_df["classification"] == "Unclassified").sum()
    assert unclassified == total_rule_violations - classified_tp - classified_fp

    percent_tp = 100 * classified_tp / \
        total_rule_violations if total_rule_violations else 0
    percent_fp = 100 * classified_fp / \
        total_rule_violations if total_rule_violations else 0
    percent_unclassified = 100 * unclassified / \
        total_rule_violations if total_rule_violations else 0

    mdFile.new_line(
        f"TP: {classified_tp} ({percent_tp:.2f}%)  \nFP: {classified_fp} ({percent_fp:.2f}%)  \nUnclassified: {unclassified} ({percent_unclassified:.2f}%)")

    # CCA Plausible fixes section

    mdFile.new_header(level=3, title="CodeCureAgent Plausible Fixes",
                      add_table_of_contents="n")

    total_plausible_fix = (evaluation_results_file_df["plausibleFix"]).sum()
    tp_plausible_fix = ((evaluation_results_file_df["plausibleFix"]) & (
        evaluation_results_file_df["classification"] == "TP")).sum()
    fp_plausible_fix = ((evaluation_results_file_df["plausibleFix"]) & (
        evaluation_results_file_df["classification"] == "FP")).sum()

    percent_total_plausible = 100 * total_plausible_fix / \
        total_rule_violations if total_rule_violations else 0
    percent_tp_plausible = 100 * tp_plausible_fix / \
        classified_tp if classified_tp else 0
    percent_fp_plausible = 100 * fp_plausible_fix / \
        classified_fp if classified_fp else 0

    mdFile.new_line(
        f"Total plausible fixes: {total_plausible_fix}/{total_rule_violations} ({percent_total_plausible:.2f}%)  ")
    mdFile.new_line(
        f"TP plausible fixes: {tp_plausible_fix}/{classified_tp} ({percent_tp_plausible:.2f}%)  ")
    mdFile.new_line(
        f"FP plausible fixes: {fp_plausible_fix}/{classified_fp} ({percent_fp_plausible:.2f}%)  ")

    # CCA Sound classifications section

    total_sound_classification = (
        evaluation_results_file_df["classificationSoundness"] == "Sound").sum()

    total_not_sound_classification = (
        evaluation_results_file_df["classificationSoundness"] == "Not sound").sum()

    tp_sound_classification = ((evaluation_results_file_df["classification"] == "TP") & (
        evaluation_results_file_df["classificationSoundness"] == "Sound")).sum()
    tp_not_sound_classification = ((evaluation_results_file_df["classification"] == "TP") & (
        evaluation_results_file_df["classificationSoundness"] == "Not sound")).sum()

    fp_sound_classification = ((evaluation_results_file_df["classification"] == "FP") & (
        evaluation_results_file_df["classificationSoundness"] == "Sound")).sum()
    fp_not_sound_classification = ((evaluation_results_file_df["classification"] == "FP") & (
        evaluation_results_file_df["classificationSoundness"] == "Not sound")).sum()

    percent_total_sound = 100 * total_sound_classification / \
        (total_sound_classification + total_not_sound_classification) if (
            total_sound_classification + total_not_sound_classification) else 0
    percent_tp_sound = 100 * tp_sound_classification / \
        (tp_sound_classification + tp_not_sound_classification) if (
            tp_sound_classification + tp_not_sound_classification) else 0
    percent_fp_sound = 100 * fp_sound_classification / \
        (fp_sound_classification + fp_not_sound_classification) if (
            fp_sound_classification + fp_not_sound_classification) else 0

    mdFile.new_header(
        level=3, title="CodeCureAgent Soundness of classification", add_table_of_contents="n")

    mdFile.new_line(
        f"Total sound classifications: {total_sound_classification}/{total_sound_classification + total_not_sound_classification} ({percent_total_sound:.2f}%)  ")
    mdFile.new_line(
        f"Sound TP classifications: {tp_sound_classification}/{tp_sound_classification + tp_not_sound_classification} ({percent_tp_sound:.2f}%)  ")
    mdFile.new_line(
        f"Sound FP classifications: {fp_sound_classification}/{fp_sound_classification + fp_not_sound_classification} ({percent_fp_sound:.2f}%)  ")

    # CCA Sound classification Precision and Recall

    # True TP warning => TP
    # True FP warning => TN
    # False TP warning (classified TP but not sound) => FP
    # False FP warning (classified FP but not sound) => FN

    # Precision: TP / (TP + FP) => True TP / (True TP + False TP)
    precision = tp_sound_classification / \
        (tp_sound_classification + tp_not_sound_classification) if (
            tp_sound_classification + tp_not_sound_classification) else 0

    # Recall: TP / (TP + FN) => True TP / (True TP + False FP)
    recall = tp_sound_classification / \
        (tp_sound_classification + fp_not_sound_classification) if (
            tp_sound_classification + fp_not_sound_classification) else 0

    mdFile.new_line(
        f"Precision: {precision:.2f}  ")
    mdFile.new_line(
        f"Recall: {recall:.2f}  ")

    f1_score = (2 * precision * recall) / (precision +
                                           recall) if (precision + recall) else 0

    mdFile.new_line(
        f"F1 Score: {f1_score:.2f}  ")

    # CCA Correct classification section

    # Doesn't include unfixed instances
    total_correct_and_sound = ((
        evaluation_results_file_df["classificationSoundness"] == "Sound") & (
        evaluation_results_file_df["fixCorrectness"] == "Correct")).sum()

    total_not_correct_and_sound = ((
        evaluation_results_file_df["classificationSoundness"] == "Sound") & (
        evaluation_results_file_df["fixCorrectness"] == "Not correct")).sum()

    tp_correct_and_sound = ((evaluation_results_file_df["classification"] == "TP") & (
        evaluation_results_file_df["classificationSoundness"] == "Sound") & (
        evaluation_results_file_df["fixCorrectness"] == "Correct")).sum()
    tp_not_correct_and_sound = ((evaluation_results_file_df["classification"] == "TP") & (
        evaluation_results_file_df["classificationSoundness"] == "Sound") & (
        evaluation_results_file_df["fixCorrectness"] == "Not correct")).sum()

    fp_correct_and_sound = ((evaluation_results_file_df["classification"] == "FP") & (
        evaluation_results_file_df["classificationSoundness"] == "Sound") & (
        evaluation_results_file_df["fixCorrectness"] == "Correct")).sum()
    fp_not_correct_and_sound = ((evaluation_results_file_df["classification"] == "FP") & (
        evaluation_results_file_df["classificationSoundness"] == "Sound") & (
        evaluation_results_file_df["fixCorrectness"] == "Not correct")).sum()

    percent_total_correct_and_sound = 100 * total_correct_and_sound / \
        (total_correct_and_sound + total_not_correct_and_sound) if (
            total_correct_and_sound + total_not_correct_and_sound) else 0
    percent_tp_correct_and_sound = 100 * tp_correct_and_sound / \
        (tp_correct_and_sound + tp_not_correct_and_sound) if (tp_correct_and_sound +
                                                              tp_not_correct_and_sound) else 0
    percent_fp_correct_and_sound = 100 * fp_correct_and_sound / \
        (fp_correct_and_sound + fp_not_correct_and_sound) if (fp_correct_and_sound +
                                                              fp_not_correct_and_sound) else 0

    mdFile.new_header(
        level=3, title="CodeCureAgent Correctness of fix", add_table_of_contents="n")

    mdFile.new_line(
        f"Total correct fixes (sound and correct / sound and fixed): {total_correct_and_sound}/{total_correct_and_sound + total_not_correct_and_sound} ({percent_total_correct_and_sound:.2f}%)  ")
    mdFile.new_line(
        f"Correct TP fixes  (sound and correct / sound and fixed): {tp_correct_and_sound}/{tp_correct_and_sound + tp_not_correct_and_sound} ({percent_tp_correct_and_sound:.2f}%)  ")
    mdFile.new_line(
        f"Correct FP fixes  (sound and correct / sound and fixed): {fp_correct_and_sound}/{fp_correct_and_sound + fp_not_correct_and_sound} ({percent_fp_correct_and_sound:.2f}%)  ")

    mdFile.new_header(
        level=3, title="CodeCureAgent End-to-end performance (fixed, sound and correct)", add_table_of_contents="n")

    percent_end_to_end_all = 100 * total_correct_and_sound / \
        (total_sound_classification + total_not_sound_classification) if (
            total_sound_classification + total_not_sound_classification) else 0
    percent_end_to_end_tp = 100 * tp_correct_and_sound / \
        (tp_sound_classification + tp_not_sound_classification) if (
            tp_sound_classification + tp_not_sound_classification) else 0
    percent_end_to_end_fp = 100 * fp_correct_and_sound / \
        (fp_sound_classification + fp_not_sound_classification) if (
            fp_sound_classification + fp_not_sound_classification) else 0
    mdFile.new_line(
        f"End-to-end total: {total_correct_and_sound}/{total_sound_classification + total_not_sound_classification} ({percent_end_to_end_all:.2f}%)  ")
    mdFile.new_line(
        f"End-to-end TP: {tp_correct_and_sound}/{tp_sound_classification + tp_not_sound_classification} ({percent_end_to_end_tp:.2f}%)  ")
    mdFile.new_line(
        f"End-to-end FP: {fp_correct_and_sound}/{fp_sound_classification + fp_not_sound_classification} ({percent_end_to_end_fp:.2f}%)  ")

    # CCA Fix complexity

    # Count by fixComplexity type
    fix_complexity_counts = evaluation_results_file_df["fixComplexity"].value_counts(
    )
    single_line_count = fix_complexity_counts.get("Single Line", 0)
    multi_line_count = fix_complexity_counts.get("Multi Line", 0)
    multi_file_count = fix_complexity_counts.get("Multi File", 0)

    percent_single_line = 100 * single_line_count / \
        total_rule_violations if total_rule_violations else 0
    percent_multi_line = 100 * multi_line_count / \
        total_rule_violations if total_rule_violations else 0
    percent_multi_file = 100 * multi_file_count / \
        total_rule_violations if total_rule_violations else 0

    mdFile.new_header(level=3, title="CodeCureAgent Fix Complexity",
                      add_table_of_contents="n")
    mdFile.new_line(
        f"Single Line problems: {single_line_count} ({percent_single_line:.2f}%)  ")
    mdFile.new_line(
        f"Multi Line problems: {multi_line_count} ({percent_multi_line:.2f}%)  ")
    mdFile.new_line(
        f"Multi File problems: {multi_file_count} ({percent_multi_file:.2f}%)  ")

    # Split by TP/FP
    tp_single_line = ((evaluation_results_file_df["classification"] == "TP") & (
        evaluation_results_file_df["fixComplexity"] == "Single Line")).sum()
    tp_multi_line = ((evaluation_results_file_df["classification"] == "TP") & (
        evaluation_results_file_df["fixComplexity"] == "Multi Line")).sum()
    tp_multi_file = ((evaluation_results_file_df["classification"] == "TP") & (
        evaluation_results_file_df["fixComplexity"] == "Multi File")).sum()

    fp_single_line = ((evaluation_results_file_df["classification"] == "FP") & (
        evaluation_results_file_df["fixComplexity"] == "Single Line")).sum()
    fp_multi_line = ((evaluation_results_file_df["classification"] == "FP") & (
        evaluation_results_file_df["fixComplexity"] == "Multi Line")).sum()
    fp_multi_file = ((evaluation_results_file_df["classification"] == "FP") & (
        evaluation_results_file_df["fixComplexity"] == "Multi File")).sum()

    percent_tp_single_line = 100 * tp_single_line / \
        classified_tp if classified_tp else 0
    percent_tp_multi_line = 100 * tp_multi_line / \
        classified_tp if classified_tp else 0
    percent_tp_multi_file = 100 * tp_multi_file / \
        classified_tp if classified_tp else 0

    percent_fp_single_line = 100 * fp_single_line / \
        classified_fp if classified_fp else 0
    percent_fp_multi_line = 100 * fp_multi_line / \
        classified_fp if classified_fp else 0
    percent_fp_multi_file = 100 * fp_multi_file / \
        classified_fp if classified_fp else 0

    mdFile.new_line("")
    mdFile.new_line("Fix complexity split by type of fix:  ")
    mdFile.new_line(
        f"TP - Single Line: {tp_single_line} ({percent_tp_single_line:.2f}%)  ")
    mdFile.new_line(
        f"TP - Multi Line: {tp_multi_line} ({percent_tp_multi_line:.2f}%)  ")
    mdFile.new_line(
        f"TP - Multi File: {tp_multi_file} ({percent_tp_multi_file:.2f}%)  ")
    mdFile.new_line(
        f"FP - Single Line: {fp_single_line} ({percent_fp_single_line:.2f}%)  ")
    mdFile.new_line(
        f"FP - Multi Line: {fp_multi_line} ({percent_fp_multi_line:.2f}%)  ")
    mdFile.new_line(
        f"FP - Multi File: {fp_multi_file} ({percent_fp_multi_file:.2f}%)  ")


    # Number of fixes created per fixComplexity
    plausible_fixes_created_per_complexity = evaluation_results_file_df[evaluation_results_file_df["plausibleFix"]]["fixComplexity"].value_counts(
    )
    mdFile.new_line()
    mdFile.new_line("Number of plausible fixes created per fixComplexity:  ")
    mdFile.new_line(
        f"Single Line: {plausible_fixes_created_per_complexity.get('Single Line', 0)} / {single_line_count} ({100 * plausible_fixes_created_per_complexity.get('Single Line', 0) / single_line_count:.2f}%)  " if single_line_count else "Single Line: 0 / 0 (0.00%)  ")
    mdFile.new_line(
        f"Multi Line: {plausible_fixes_created_per_complexity.get('Multi Line', 0)} / {multi_line_count} ({100 * plausible_fixes_created_per_complexity.get('Multi Line', 0) / multi_line_count:.2f}%)  " if multi_line_count else "Multi Line: 0 / 0 (0.00%)  ")
    mdFile.new_line(
        f"Multi File: {plausible_fixes_created_per_complexity.get('Multi File', 0)} / {multi_file_count} ({100 * plausible_fixes_created_per_complexity.get('Multi File', 0) / multi_file_count:.2f}%)  " if multi_file_count else "Multi File: 0 / 0 (0.00%)  ")

    # Number of correct fixes created per fixComplexity
    correct_fixes = evaluation_results_file_df[((evaluation_results_file_df["classificationSoundness"]
                                               == "Sound") & (evaluation_results_file_df["fixCorrectness"] == "Correct"))]

    correct_fixes_per_complexity = correct_fixes["fixComplexity"].value_counts(
    )
    mdFile.new_line()
    mdFile.new_line(
        f"Number of correct fixes created per fixComplexity (of the {total_correct_and_sound} sound and correct fixes (only for inspected samples)):  ")
    mdFile.new_line(
        f"Single Line: {correct_fixes_per_complexity.get('Single Line', 0)} ({100 * correct_fixes_per_complexity.get('Single Line', 0) / total_correct_and_sound:.2f}%)  " if total_correct_and_sound else "Single Line: 0 (0.00%)  ")
    mdFile.new_line(
        f"Multi Line: {correct_fixes_per_complexity.get('Multi Line', 0)} ({100 * correct_fixes_per_complexity.get('Multi Line', 0) / total_correct_and_sound:.2f}%)  " if total_correct_and_sound else "Multi Line: 0 (0.00%)  ")
    mdFile.new_line(
        f"Multi File: {correct_fixes_per_complexity.get('Multi File', 0)} ({100 * correct_fixes_per_complexity.get('Multi File', 0) / total_correct_and_sound:.2f}%)  " if total_correct_and_sound else "Multi File: 0 / 0 (0.00%)  ")


    # ---- iSMELL stats section

    mdFile.new_header(level=2, title="iSMELL stats", add_table_of_contents="n")

    # iSMELL Performance Stats

    mdFile.new_header(level=3, title="Performance Stats", add_table_of_contents="n")

    ismell_fix_created = (evaluation_results_file_df["ismellFixCreated"]).sum()
    ismell_build_successful = (evaluation_results_file_df["ismellBuildSuccessful"]).sum()
    ismell_warning_removed = (evaluation_results_file_df["ismellSonarCheckRemovedWarning"]).sum()
    ismell_no_new_warning = (evaluation_results_file_df["ismellSonarCheckNoNewWarning"]).sum()
    ismell_test_successful = (evaluation_results_file_df["ismellTestSuccessful"]).sum()
    ismell_build_and_removed = (evaluation_results_file_df["ismellBuildAndRemovedWarning"]).sum()
    ismell_build_and_removed_and_no_new = (evaluation_results_file_df["ismellBuildAndRemovedWarningAndNoNewWarning"]).sum()
    ismell_all_checks = (evaluation_results_file_df["ismellBuildAndRemovedWarningAndNoNewWarningAndTest"]).sum()

    percent_fix_created = 100 * ismell_fix_created / total_rule_violations if total_rule_violations else 0
    percent_build_successful = 100 * ismell_build_successful / total_rule_violations if total_rule_violations else 0
    percent_warning_removed = 100 * ismell_warning_removed / total_rule_violations if total_rule_violations else 0
    percent_no_new_warning = 100 * ismell_no_new_warning / total_rule_violations if total_rule_violations else 0
    percent_test_successful = 100 * ismell_test_successful / total_rule_violations if total_rule_violations else 0
    percent_build_and_removed = 100 * ismell_build_and_removed / total_rule_violations if total_rule_violations else 0
    percent_build_and_removed_and_no_new = 100 * ismell_build_and_removed_and_no_new / total_rule_violations if total_rule_violations else 0
    percent_all_checks = 100 * ismell_all_checks / total_rule_violations if total_rule_violations else 0

    mdFile.new_line(f"iSMELL fix created: {ismell_fix_created}/{total_rule_violations} ({percent_fix_created:.2f}%)  ")
    mdFile.new_line(f"iSMELL build successful: {ismell_build_successful}/{total_rule_violations} ({percent_build_successful:.2f}%)  ")
    mdFile.new_line(f"iSMELL warning removed: {ismell_warning_removed}/{total_rule_violations} ({percent_warning_removed:.2f}%)  ")
    mdFile.new_line(f"iSMELL no new warning introduced: {ismell_no_new_warning}/{total_rule_violations} ({percent_no_new_warning:.2f}%)  ")
    mdFile.new_line(f"iSMELL test successful: {ismell_test_successful}/{total_rule_violations} ({percent_test_successful:.2f}%)  ")
    mdFile.new_line("")
    mdFile.new_line(f"iSMELL build successful + warning removed: {ismell_build_and_removed}/{total_rule_violations} ({percent_build_and_removed:.2f}%)  ")
    mdFile.new_line(f"iSMELL build successful + warning removed + no new warning: {ismell_build_and_removed_and_no_new}/{total_rule_violations} ({percent_build_and_removed_and_no_new:.2f}%)  ")
    mdFile.new_line(f"iSMELL all checks (build + warning removed + no new warning + test): {ismell_all_checks}/{total_rule_violations} ({percent_all_checks:.2f}%)  ")

    # Performance on TP vs FP
    mdFile.new_line("")
    mdFile.new_line("Performance breakdown by classification:")

    tp_all_checks = ((evaluation_results_file_df["classification"] == "TP") & 
                     (evaluation_results_file_df["ismellBuildAndRemovedWarningAndNoNewWarningAndTest"] == True)).sum()
    fp_all_checks = ((evaluation_results_file_df["classification"] == "FP") & 
                     (evaluation_results_file_df["ismellBuildAndRemovedWarningAndNoNewWarningAndTest"] == True)).sum()

    percent_tp_all_checks = 100 * tp_all_checks / classified_tp if classified_tp else 0
    percent_fp_all_checks = 100 * fp_all_checks / classified_fp if classified_fp else 0

    mdFile.new_line(f"iSMELL TP all checks: {tp_all_checks}/{classified_tp} ({percent_tp_all_checks:.2f}%)  ")
    mdFile.new_line(f"iSMELL FP all checks: {fp_all_checks}/{classified_fp} ({percent_fp_all_checks:.2f}%)  ")

    # TP assumption soundness analysis
    mdFile.new_line("")
    
    # Count instances with TP assumption data
    tp_assumption_data = evaluation_results_file_df[evaluation_results_file_df["ismellTPAssumptionSoundness"].notna() & 
                                                   (evaluation_results_file_df["ismellTPAssumptionSoundness"] != "")]
    
    if len(tp_assumption_data) > 0:
        tp_assumption_sound = (tp_assumption_data["ismellTPAssumptionSoundness"] == "Sound").sum()
        tp_assumption_not_sound = (tp_assumption_data["ismellTPAssumptionSoundness"] == "Not sound").sum()
        total_tp_assumption_evaluated = len(tp_assumption_data)
        
        percent_tp_assumption_sound = 100 * tp_assumption_sound / total_tp_assumption_evaluated if total_tp_assumption_evaluated else 0
        percent_tp_assumption_not_sound = 100 * tp_assumption_not_sound / total_tp_assumption_evaluated if total_tp_assumption_evaluated else 0
        
        mdFile.new_line(f"iSMELL TP assumption sound (not in conjunction with if a fix was created): {tp_assumption_sound}/{total_tp_assumption_evaluated} ({percent_tp_assumption_sound:.2f}%)  ")
        mdFile.new_line(f"iSMELL instances where TP assumption was not sound: {tp_assumption_not_sound} ({percent_tp_assumption_not_sound:.2f}%)  ")
    else:
        mdFile.new_line(f"iSMELL TP assumption sound (not in conjunction with if a fix was created): 0/0 (0.00%)  ")
        mdFile.new_line(f"iSMELL instances where TP assumption was not sound: 0 (0.00%)  ")

    # Fix correctness analysis
    mdFile.new_line("")
    
    # Count instances with fix correctness data
    fix_correctness_data = evaluation_results_file_df[evaluation_results_file_df["ismellCorrectFix"].notna() & 
                                                     (evaluation_results_file_df["ismellCorrectFix"] != "")]
    fix_correct_of_all_inspected = 0
    total_inspected = 0
    percent_fix_correct_of_all_inspected = 0

    if len(fix_correctness_data) > 0:
        # Fix correct (and fix created) (of instances where fix was created (and manually inspected))
        fix_created_and_inspected = fix_correctness_data[fix_correctness_data["ismellFixCreated"] == True]
        fix_correct_of_created = (fix_created_and_inspected["ismellCorrectFix"] == "Correct").sum()
        total_fix_created_and_inspected = len(fix_created_and_inspected)
        
        # Fix correct (and fix created) (of all manually inspected warnings)
        fix_correct_of_all_inspected = (fix_correctness_data["ismellCorrectFix"] == "Correct").sum()
        total_inspected = len(fix_correctness_data)
        
        # Fix correct (not only for instances where fix was created (no fix => incorrect fix))
        fix_correct_including_no_fix = fix_correct_of_all_inspected
        # Count unfixed as incorrect
        unfixed_count = (fix_correctness_data["ismellCorrectFix"] == "Unfixed").sum()
        not_correct_count = (fix_correctness_data["ismellCorrectFix"] == "Not correct").sum()
        incorrect_total = unfixed_count + not_correct_count + (total_inspected - fix_correct_of_all_inspected - unfixed_count - not_correct_count)
        
        percent_fix_correct_of_created = 100 * fix_correct_of_created / total_fix_created_and_inspected if total_fix_created_and_inspected else 0
        percent_fix_correct_of_all_inspected = 100 * fix_correct_of_all_inspected / total_inspected if total_inspected else 0
        percent_fix_correct_including_no_fix = 100 * fix_correct_including_no_fix / total_inspected if total_inspected else 0
        
        mdFile.new_line(f"iSMELL fix correct (and fix created) (of instances where fix was created (and manually inspected)): {fix_correct_of_created}/{total_fix_created_and_inspected} ({percent_fix_correct_of_created:.2f}%)  ")
        mdFile.new_line(f"iSMELL fix correct (and fix created) (of all manually inspected warnings): {fix_correct_of_all_inspected}/{total_inspected} ({percent_fix_correct_of_all_inspected:.2f}%)  ")
        mdFile.new_line(f"iSMELL fix correct (not only for instances where fix was created (no fix => incorrect fix) (of all manually inspected warnings)): {fix_correct_including_no_fix}/{total_inspected} ({percent_fix_correct_including_no_fix:.2f}%)  ")
    else:
        mdFile.new_line(f"iSMELL fix correct (and fix created) (of instances where fix was created (and manually inspected)): 0/0 (0.00%)  ")
        mdFile.new_line(f"iSMELL fix correct (and fix created) (of all manually inspected warnings): 0/0 (0.00%)  ")
        mdFile.new_line(f"iSMELL fix correct (not only for instances where fix was created (no fix => incorrect fix) (of all manually inspected warnings)): 0/0 (0.00%)  ")

    # Code smell analysis
    mdFile.new_line("")
    
    # Count instances with code smell data
    code_smell_data = evaluation_results_file_df[evaluation_results_file_df["codeSmellOutsideOfSonarQubeIntroduced"].notna() & 
                                                (evaluation_results_file_df["codeSmellOutsideOfSonarQubeIntroduced"] != "")]
    
    if len(code_smell_data) > 0:
        code_smell_introduced = (code_smell_data["codeSmellOutsideOfSonarQubeIntroduced"] == True).sum()
        total_code_smell_evaluated = len(code_smell_data)
        
        # Code smell for warnings where all other stats pass
        all_other_stats_pass = code_smell_data[code_smell_data["ismellBuildAndRemovedWarningAndNoNewWarningAndTest"] == True]
        code_smell_of_passing = (all_other_stats_pass["codeSmellOutsideOfSonarQubeIntroduced"] == True).sum()
        total_passing = len(all_other_stats_pass)
        
        percent_code_smell_introduced = 100 * code_smell_introduced / total_code_smell_evaluated if total_code_smell_evaluated else 0
        percent_code_smell_of_passing = 100 * code_smell_of_passing / total_passing if total_passing else 0
        
        mdFile.new_line(f"iSMELL further code smell introduced not reported by SonarQube (of all manually inspected warnings): {code_smell_introduced}/{total_code_smell_evaluated} ({percent_code_smell_introduced:.2f}%)  ")
        mdFile.new_line(f"iSMELL further code smell introduced not reported by SonarQube (of all warnings where all other stats pass): {code_smell_of_passing}/{total_passing} ({percent_code_smell_of_passing:.2f}%)  ")
    else:
        mdFile.new_line(f"iSMELL further code smell introduced not reported by SonarQube (of all manually inspected warnings): 0/0 (0.00%)  ")
        mdFile.new_line(f"iSMELL further code smell introduced not reported by SonarQube (of all warnings where all other stats pass): 0/0 (0.00%)  ")

    # Time Efficiency
    mdFile.new_header(level=3, title="Time Efficiency", add_table_of_contents="n")

    # Filter out rows where time is 0 (no fix created)
    time_data = evaluation_results_file_df[evaluation_results_file_df["ismellTotalTime"] > 0]["ismellTotalTime"]
    
    if len(time_data) > 0:
        mean_time = nano_to_second(time_data.mean())
        median_time = nano_to_second(time_data.median())
        
        mdFile.new_line(f"iSMELL total time (instances with fixes): mean={mean_time}s, median={median_time}s  ")
        mdFile.new_line(f"Number of instances with time data: {len(time_data)}/{total_rule_violations}  ")
    else:
        mdFile.new_line("No time data available (no fixes created)")

    # Token consumption and cost analysis
    mdFile.new_header(level=3, title="Token Consumption and Cost", add_table_of_contents="n")

    # Filter out rows where tokens/cost are 0 (no fix attempted)
    token_data = evaluation_results_file_df[evaluation_results_file_df["ismellTotalTokens"] > 0]

    if len(token_data) > 0:
        total_uncached_tokens = token_data["ismellUncachedInputTokens"].sum()
        total_cached_tokens = token_data["ismellCachedInputTokens"].sum()
        total_output_tokens = token_data["ismellOutputTokens"].sum()
        total_tokens = token_data["ismellTotalTokens"].sum()
        total_cost = token_data["ismellCost"].sum()

        mean_uncached_tokens = token_data["ismellUncachedInputTokens"].mean()
        mean_cached_tokens = token_data["ismellCachedInputTokens"].mean()
        mean_output_tokens = token_data["ismellOutputTokens"].mean()
        mean_total_tokens = token_data["ismellTotalTokens"].mean()
        mean_cost = token_data["ismellCost"].mean()

        instances_with_tokens = len(token_data)

        mdFile.new_line(f"**Total across all instances with token data ({instances_with_tokens} instances):**  ")
        mdFile.new_line(f"Total uncached input tokens: {total_uncached_tokens:,}  ")
        mdFile.new_line(f"Total cached input tokens: {total_cached_tokens:,}  ")
        mdFile.new_line(f"Total output tokens: {total_output_tokens:,}  ")
        mdFile.new_line(f"Total tokens: {total_tokens:,}  ")
        mdFile.new_line(f"Total cost: ${total_cost:.4f}  ")
        mdFile.new_line("")
        mdFile.new_line(f"**Average per instance:**  ")
        mdFile.new_line(f"Average uncached input tokens: {mean_uncached_tokens:.1f}  ")
        mdFile.new_line(f"Average cached input tokens: {mean_cached_tokens:.1f}  ")
        mdFile.new_line(f"Average output tokens: {mean_output_tokens:.1f}  ")
        mdFile.new_line(f"Average total tokens: {mean_total_tokens:.1f}  ")
        mdFile.new_line(f"Average cost per instance: ${mean_cost:.4f}  ")
        
        # Cost efficiency metrics
        mdFile.new_line("")
        mdFile.new_line(f"**Cost efficiency:**  ")
        cost_per_successful_fix = total_cost / ismell_all_checks if ismell_all_checks > 0 else 0
        mdFile.new_line(f"Cost per successful fix (all checks passed): ${cost_per_successful_fix:.4f}  ")
        
        if ismell_fix_created > 0:
            cost_per_fix_created = total_cost / ismell_fix_created
            mdFile.new_line(f"Cost per fix created: ${cost_per_fix_created:.4f}  ")
    else:
        mdFile.new_line("No token/cost data available")

    # Comparison with CodeCureAgent performance
    mdFile.new_header(level=2, title="Comparison Summary", add_table_of_contents="n")

    mdFile.new_line(f"**End-to-end performance comparison:**  ")
    mdFile.new_line(f"CodeCureAgent end-to-end: {total_correct_and_sound}/{total_sound_classification + total_not_sound_classification} ({percent_end_to_end_all:.2f}%)  ")
    mdFile.new_line(f"iSMELL end-to-end {fix_correct_of_all_inspected}/{total_inspected} ({percent_fix_correct_of_all_inspected:.2f}%)  ")

    performance_gap = percent_end_to_end_all - percent_fix_correct_of_all_inspected
    mdFile.new_line(f"Performance gap (CCA - iSMELL): {performance_gap:.2f} percentage points  ")

    mdFile.create_md_file()


def nano_to_second(time_in_nano: float):
    return round(time_in_nano / 1_000_000_000, 4)


if __name__ == "__main__":
    calculate_ismell_stats_from_evaluation_results()
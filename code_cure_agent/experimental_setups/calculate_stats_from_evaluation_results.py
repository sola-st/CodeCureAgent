import os
import re
import click
import mdutils
import pandas as pd
import csv

import sys
from pathlib import Path

sys.path.append(str(Path(__file__).parent.parent))
from agent_core.llm.providers.openai import OPEN_AI_CHAT_MODELS


@click.command()
@click.argument(
    "evaluation-results-extended-file",
    type=click.File()
)
@click.option(
    "--target-md-file-path",
    "-t",
    default="./analysis_results_overview.md",
    help="Path where the stats file should be written."
)
def calculate_stats_from_evaluation_results(evaluation_results_extended_file: click.File, target_md_file_path: str):

    evaluation_results_file_df = pd.read_csv(evaluation_results_extended_file)

    mdFile = mdutils.MdUtils(
        file_name=target_md_file_path, title='Experiment Analysis Results', title_header_style="atx")
    mdFile.new_header(level=2, title="Overall stats",
                      add_table_of_contents="n")
    mdFile.new_header(level=3, title="Total rule violations",
                      add_table_of_contents="n")

    total_rule_violations = evaluation_results_file_df["instanceID"].count()

    mdFile.new_line(
        str(total_rule_violations))

    # Classification section

    mdFile.new_header(level=3, title="Classification",
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

    # Plausible fixes section

    mdFile.new_header(level=3, title="Plausible Fixes",
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

    total_compilation_passed = (
        evaluation_results_file_df["compilationPassed"]).sum()
    total_sonar_qube_check_passed = (
        evaluation_results_file_df["sonarQubeCheckPassed"]).sum()

    fix_tp_compilation_passed = ((evaluation_results_file_df["compilationPassed"]) & (
        evaluation_results_file_df["classification"] == "TP")).sum()
    fix_fp_compilation_passed = ((evaluation_results_file_df["compilationPassed"]) & (
        evaluation_results_file_df["classification"] == "FP")).sum()

    fix_tp_sonar_qube_check_passed = ((evaluation_results_file_df["sonarQubeCheckPassed"]) & (
        evaluation_results_file_df["classification"] == "TP")).sum()
    fix_fp_sonar_qube_check_passed = ((evaluation_results_file_df["sonarQubeCheckPassed"]) & (
        evaluation_results_file_df["classification"] == "FP")).sum()

    percent_total_compilation = 100 * total_compilation_passed / \
        total_rule_violations if total_rule_violations else 0
    percent_tp_compilation = 100 * fix_tp_compilation_passed / \
        classified_tp if classified_tp else 0
    percent_fp_compilation = 100 * fix_fp_compilation_passed / \
        classified_fp if classified_fp else 0

    percent_total_sonar = 100 * total_sonar_qube_check_passed / \
        total_rule_violations if total_rule_violations else 0
    percent_tp_sonar = 100 * fix_tp_sonar_qube_check_passed / \
        classified_tp if classified_tp else 0
    percent_fp_sonar = 100 * fix_fp_sonar_qube_check_passed / \
        classified_fp if classified_fp else 0

    # Passed previous agent steps successfully

    mdFile.new_header(level=4, title="Passed previous steps",
                      add_table_of_contents="n")
    mdFile.new_line(
        f"Total compilation step passed: {total_compilation_passed}/{total_rule_violations} ({percent_total_compilation:.2f}%)  ")
    mdFile.new_line(
        f"TP compilation step passed: {fix_tp_compilation_passed}/{classified_tp} ({percent_tp_compilation:.2f}%)  ")
    mdFile.new_line(
        f"FP compilation step passed: {fix_fp_compilation_passed}/{classified_fp} ({percent_fp_compilation:.2f}%)  ")

    mdFile.new_line(
        f"Total SonarQube check step passed: {total_sonar_qube_check_passed}/{total_rule_violations} ({percent_total_sonar:.2f}%)  ")
    mdFile.new_line(
        f"TP SonarQube check step passed: {fix_tp_sonar_qube_check_passed}/{classified_tp} ({percent_tp_sonar:.2f}%)  ")
    mdFile.new_line(
        f"FP SonarQube check step passed: {fix_fp_sonar_qube_check_passed}/{classified_fp} ({percent_fp_sonar:.2f}%)  ")

    # Sound classifications section

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
        level=3, title="Soundness of classification", add_table_of_contents="n")

    mdFile.new_line(
        f"Total sound classifications: {total_sound_classification}/{total_sound_classification + total_not_sound_classification} ({percent_total_sound:.2f}%)  ")
    mdFile.new_line(
        f"Sound TP classifications: {tp_sound_classification}/{tp_sound_classification + tp_not_sound_classification} ({percent_tp_sound:.2f}%)  ")
    mdFile.new_line(
        f"Sound FP classifications: {fp_sound_classification}/{fp_sound_classification + fp_not_sound_classification} ({percent_fp_sound:.2f}%)  ")

    # Sound classification Precision and Recall

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

    # Correct classification section

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
        level=3, title="Correctness of fix", add_table_of_contents="n")

    mdFile.new_line(
        f"Total correct fixes (sound and correct / sound and fixed): {total_correct_and_sound}/{total_correct_and_sound + total_not_correct_and_sound} ({percent_total_correct_and_sound:.2f}%)  ")
    mdFile.new_line(
        f"Correct TP fixes  (sound and correct / sound and fixed): {tp_correct_and_sound}/{tp_correct_and_sound + tp_not_correct_and_sound} ({percent_tp_correct_and_sound:.2f}%)  ")
    mdFile.new_line(
        f"Correct FP fixes  (sound and correct / sound and fixed): {fp_correct_and_sound}/{fp_correct_and_sound + fp_not_correct_and_sound} ({percent_fp_correct_and_sound:.2f}%)  ")

    mdFile.new_header(
        level=3, title="End-to-end performance (fixed, sound and correct)", add_table_of_contents="n")

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

    # Fix complexity

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

    mdFile.new_header(level=3, title="Fix Complexity",
                      add_table_of_contents="n")
    mdFile.new_line(
        f"Single Line problems: {single_line_count} ({percent_single_line:.2f}%)  ")
    mdFile.new_line(
        f"Multi Line problems: {multi_line_count} ({percent_multi_line:.2f}%)  ")
    mdFile.new_line(
        f"Multi File problems: {multi_file_count} ({percent_multi_file:.2f}%)  ")

    # Split by type of fix (TP/FP)
    tp_fix_complexity = evaluation_results_file_df[evaluation_results_file_df["classification"]
                                                   == "TP"]["fixComplexity"].value_counts()
    fp_fix_complexity = evaluation_results_file_df[evaluation_results_file_df["classification"]
                                                   == "FP"]["fixComplexity"].value_counts()
    mdFile.new_line()
    mdFile.new_line("Fix complexity split by type of fix:  ")
    mdFile.new_line(
        f"TP - Single Line: {tp_fix_complexity.get('Single Line', 0)} ({100 * tp_fix_complexity.get('Single Line', 0) / classified_tp:.2f}%)  " if classified_tp else f"TP - Single Line: 0 (0.00%)  ")
    mdFile.new_line(
        f"TP - Multi Line: {tp_fix_complexity.get('Multi Line', 0)} ({100 * tp_fix_complexity.get('Multi Line', 0) / classified_tp:.2f}%)  " if classified_tp else f"TP - Multi Line: 0 (0.00%)  ")
    mdFile.new_line(
        f"TP - Multi File: {tp_fix_complexity.get('Multi File', 0)} ({100 * tp_fix_complexity.get('Multi File', 0) / classified_tp:.2f}%)  " if classified_tp else f"TP - Multi File: 0 (0.00%)  ")
    mdFile.new_line(
        f"FP - Single Line: {fp_fix_complexity.get('Single Line', 0)} ({100 * fp_fix_complexity.get('Single Line', 0) / classified_fp:.2f}%)  " if classified_fp else f"FP - Single Line: 0 (0.00%)  ")
    mdFile.new_line(
        f"FP - Multi Line: {fp_fix_complexity.get('Multi Line', 0)} ({100 * fp_fix_complexity.get('Multi Line', 0) / classified_fp:.2f}%)  " if classified_fp else f"FP - Multi Line: 0 (0.00%)  ")
    mdFile.new_line(
        f"FP - Multi File: {fp_fix_complexity.get('Multi File', 0)} ({100 * fp_fix_complexity.get('Multi File', 0) / classified_fp:.2f}%)  " if classified_fp else f"FP - Multi File: 0 (0.00%)  ")

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

    # Iterations
    iterations_classification = evaluation_results_file_df["iterationsClassification"].sum(
    )
    iterations_fix_tp = evaluation_results_file_df.loc[
        evaluation_results_file_df["classification"] == "TP", "iterationsFixTP"
    ].sum()
    iterations_fix_fp = evaluation_results_file_df.loc[
        evaluation_results_file_df["classification"] == "FP", "iterationsFixFP"].sum()

    total_iterations = iterations_classification + \
        iterations_fix_tp + iterations_fix_fp

    mean_iterations_classification = evaluation_results_file_df["iterationsClassification"].mean(
    )
    mean_iterations_fix_tp = (
        iterations_fix_tp / classified_tp if classified_tp else 0
    )
    mean_iterations_fix_fp = (
        iterations_fix_fp / classified_fp if classified_fp else 0
    )
    mean_total_iterations = (
        total_iterations / total_rule_violations if total_rule_violations else 0
    )

    median_iterations_classification = evaluation_results_file_df["iterationsClassification"].median(
    )
    median_iterations_fix_tp = evaluation_results_file_df.loc[
        evaluation_results_file_df["classification"] == "TP", "iterationsFixTP"
    ].median(
    )
    median_iterations_fix_fp = evaluation_results_file_df.loc[
        evaluation_results_file_df["classification"] == "FP", "iterationsFixFP"
    ].median(
    )
    median_total_iterations = median_iterations_classification + \
        median_iterations_fix_tp + median_iterations_fix_fp

    mdFile.new_header(level=3, title="Iterations", add_table_of_contents="n")
    mdFile.new_line(f"Total iterations: {total_iterations}  ")
    mdFile.new_line(f"Iterations by sub-agent:  ")
    mdFile.new_line(f"Classification: {iterations_classification}  ")
    mdFile.new_line(f"Fix_TP: {iterations_fix_tp}  ")
    mdFile.new_line(f"Fix_FP: {iterations_fix_fp}  ")

    mdFile.new_line()
    mdFile.new_line(f"Mean iterations: {mean_total_iterations:.2f}  ")
    mdFile.new_line(f"Mean iterations by sub-agent:  ")
    mdFile.new_line(f"Classification: {mean_iterations_classification:.2f}  ")
    mdFile.new_line(f"Fix_TP: {mean_iterations_fix_tp:.2f}  ")
    mdFile.new_line(f"Fix_FP: {mean_iterations_fix_fp:.2f}  ")

    mdFile.new_line()
    mdFile.new_line(f"Median iterations: {median_total_iterations:.2f}  ")
    mdFile.new_line(f"Median iterations by sub-agent:  ")
    mdFile.new_line(
        f"Classification: {median_iterations_classification:.2f}  ")
    mdFile.new_line(f"Fix_TP: {median_iterations_fix_tp:.2f}  ")
    mdFile.new_line(f"Fix_FP: {median_iterations_fix_fp:.2f}  ")

    # Plausible and implausible fixes section

    # Means
    mean_implausible_fixes = evaluation_results_file_df["implausible_fixes_count"].mean(
    )
    mean_plausible_fixes = evaluation_results_file_df["plausible_fixes_count"].mean(
    )
    mean_implausible_fixes_tp = (
        evaluation_results_file_df.loc[
            evaluation_results_file_df["classification"] == "TP", "implausible_fixes_count"
        ].mean()
        if classified_tp else 0
    )
    mean_implausible_fixes_fp = (
        evaluation_results_file_df.loc[
            evaluation_results_file_df["classification"] == "FP", "implausible_fixes_count"
        ].mean()
        if classified_fp else 0
    )
    mean_plausible_fixes_tp = (
        evaluation_results_file_df.loc[
            evaluation_results_file_df["classification"] == "TP", "plausible_fixes_count"
        ].mean()
        if classified_tp else 0
    )
    mean_plausible_fixes_fp = (
        evaluation_results_file_df.loc[
            evaluation_results_file_df["classification"] == "FP", "plausible_fixes_count"
        ].mean()
        if classified_fp else 0
    )

    # Medians
    median_implausible_fixes = evaluation_results_file_df["implausible_fixes_count"].median(
    )
    median_plausible_fixes = evaluation_results_file_df["plausible_fixes_count"].median(
    )
    median_implausible_fixes_tp = evaluation_results_file_df.loc[
        evaluation_results_file_df["classification"] == "TP", "implausible_fixes_count"
    ].median()
    median_implausible_fixes_fp = evaluation_results_file_df.loc[
        evaluation_results_file_df["classification"] == "FP", "implausible_fixes_count"
    ].median()
    median_plausible_fixes_tp = evaluation_results_file_df.loc[
        evaluation_results_file_df["classification"] == "TP", "plausible_fixes_count"
    ].median()
    median_plausible_fixes_fp = evaluation_results_file_df.loc[
        evaluation_results_file_df["classification"] == "FP", "plausible_fixes_count"
    ].median()

    mdFile.new_header(
        level=3, title="Number of Plausible and Implausible Fixes created", add_table_of_contents="n")
    mdFile.new_line(
        f"Mean number of implausible fixes: {mean_implausible_fixes:.2f}  ")
    mdFile.new_line(
        f"Mean number of implausible fixes (TP): {mean_implausible_fixes_tp:.2f}  ")
    mdFile.new_line(
        f"Mean number of implausible fixes (FP): {mean_implausible_fixes_fp:.2f}  ")
    mdFile.new_line(
        f"Mean number of plausible fixes: {mean_plausible_fixes:.2f}  ")
    mdFile.new_line(
        f"Mean number of plausible fixes (TP): {mean_plausible_fixes_tp:.2f}  ")
    mdFile.new_line(
        f"Mean number of plausible fixes (FP): {mean_plausible_fixes_fp:.2f}  ")

    mdFile.new_line()
    mdFile.new_line(
        f"Median number of implausible fixes: {median_implausible_fixes:.2f}  ")
    mdFile.new_line(
        f"Median number of implausible fixes (TP): {median_implausible_fixes_tp:.2f}  ")
    mdFile.new_line(
        f"Median number of implausible fixes (FP): {median_implausible_fixes_fp:.2f}  ")
    mdFile.new_line(
        f"Median number of plausible fixes: {median_plausible_fixes:.2f}  ")
    mdFile.new_line(
        f"Median number of plausible fixes (TP): {median_plausible_fixes_tp:.2f}  ")
    mdFile.new_line(
        f"Median number of plausible fixes (FP): {median_plausible_fixes_fp:.2f}  ")

    # ChangeApprover Ablation section

    ablation_no_change_approver_plausible_fixes = evaluation_results_file_df["ablationNoChangeApproverPlausibleFix"].sum(
    )

    ablationNoChangeApproverPlausibleFix_TP = evaluation_results_file_df.loc[
        evaluation_results_file_df["classification"] == "TP", "ablationNoChangeApproverPlausibleFix"].sum()
    ablationNoChangeApproverPlausibleFix_FP = evaluation_results_file_df.loc[
        evaluation_results_file_df["classification"] == "FP", "ablationNoChangeApproverPlausibleFix"].sum()

    percent_ablation_no_change_approver_plausible_fixes = 100 * \
        ablation_no_change_approver_plausible_fixes / \
        total_rule_violations if total_rule_violations else 0
    percent_ablation_no_change_approver_plausible_fixes_tp = 100 * \
        ablationNoChangeApproverPlausibleFix_TP / classified_tp if classified_tp else 0
    percent_ablation_no_change_approver_plausible_fixes_fp = 100 * \
        ablationNoChangeApproverPlausibleFix_FP / classified_fp if classified_fp else 0

    ablation_only_build_plausible_fixes = evaluation_results_file_df["ablationOnlyBuildPlausibleFix"].sum(
    )

    ablation_only_build_plausible_fixes_TP = evaluation_results_file_df.loc[
        evaluation_results_file_df["classification"] == "TP", "ablationOnlyBuildPlausibleFix"].sum()
    ablation_only_build_plausible_fixes_FP = evaluation_results_file_df.loc[
        evaluation_results_file_df["classification"] == "FP", "ablationOnlyBuildPlausibleFix"].sum()

    percent_ablation_only_build_plausible_fixes = 100 * ablation_only_build_plausible_fixes / \
        total_compilation_passed if total_compilation_passed else 0
    percent_ablation_only_build_plausible_fixes_TP = 100 * ablation_only_build_plausible_fixes_TP / \
        fix_tp_compilation_passed if fix_tp_compilation_passed else 0
    percent_ablation_only_build_plausible_fixes_FP = 100 * ablation_only_build_plausible_fixes_FP / \
        fix_fp_compilation_passed if fix_fp_compilation_passed else 0

    ablation_build_and_sonar_Qube_check_plausible_fixes = evaluation_results_file_df[
        "ablationBuildAndSonarQubeCheckPlausibleFix"].sum()

    ablation_build_and_sonar_Qube_check_plausible_fixes_TP = evaluation_results_file_df.loc[
        evaluation_results_file_df["classification"] == "TP", "ablationBuildAndSonarQubeCheckPlausibleFix"].sum()
    ablation_build_and_sonar_Qube_check_plausible_fixes_FP = evaluation_results_file_df.loc[
        evaluation_results_file_df["classification"] == "FP", "ablationBuildAndSonarQubeCheckPlausibleFix"].sum()

    percent_ablation_build_and_sonar_Qube_check_plausible_fixes = 100 * ablation_build_and_sonar_Qube_check_plausible_fixes / \
        total_sonar_qube_check_passed if total_sonar_qube_check_passed else 0
    percent_ablation_build_and_sonar_Qube_check_plausible_fixes_TP = 100 * ablation_build_and_sonar_Qube_check_plausible_fixes_TP / \
        fix_tp_sonar_qube_check_passed if fix_tp_sonar_qube_check_passed else 0
    percent_ablation_build_and_sonar_Qube_check_plausible_fixes_FP = 100 * ablation_build_and_sonar_Qube_check_plausible_fixes_FP / \
        fix_fp_sonar_qube_check_passed if fix_fp_sonar_qube_check_passed else 0

    mdFile.new_header(
        level=3, title="Ablation of the ChangeApprover", add_table_of_contents="n")
    mdFile.new_header(level=4, title="No ChangeApprover (accepts more fixes than with ChangeApprover (all the fixed ones + the unfixed ones) => how many of these are still plausible (would pass the full ChangeApprover) => all the other ones would falsely be labeled as plausible)",
                      add_table_of_contents="n")
    mdFile.new_line(
        f"Still plausible fixes / accepted fixes: {ablation_no_change_approver_plausible_fixes} / {total_rule_violations} ({percent_ablation_no_change_approver_plausible_fixes:.2f}%)  ")
    mdFile.new_line(
        f"Still plausible fixes / accepted fixes (TP): {ablationNoChangeApproverPlausibleFix_TP} / {classified_tp} ({percent_ablation_no_change_approver_plausible_fixes_tp:.2f}%)  ")
    mdFile.new_line(
        f"Still plausible fixes / accepted fixes (FP): {ablationNoChangeApproverPlausibleFix_FP} / {classified_fp} ({percent_ablation_no_change_approver_plausible_fixes_fp:.2f}%)  ")

    mdFile.new_header(level=4, title="Only build step (no SonarQube check and test steps)",
                      add_table_of_contents="n")
    mdFile.new_line(
        f"Still plausible fixes / accepted fixes: {ablation_only_build_plausible_fixes} / {total_compilation_passed} ({percent_ablation_only_build_plausible_fixes:.2f}%)  ")
    mdFile.new_line(
        f"Still plausible fixes / accepted fixes (TP): {ablation_only_build_plausible_fixes_TP} / {fix_tp_compilation_passed} ({percent_ablation_only_build_plausible_fixes_TP:.2f}%)  ")
    mdFile.new_line(
        f"Still plausible fixes / accepted fixes (FP): {ablation_only_build_plausible_fixes_FP} / {fix_fp_compilation_passed} ({percent_ablation_only_build_plausible_fixes_FP:.2f}%)  ")

    mdFile.new_header(
        level=4, title="Only build and SonarQube check steps (no test step)", add_table_of_contents="n")
    mdFile.new_line(
        f"Still plausible fixes / accepted fixes: {ablation_build_and_sonar_Qube_check_plausible_fixes} / {total_sonar_qube_check_passed} ({percent_ablation_build_and_sonar_Qube_check_plausible_fixes:.2f}%)  ")
    mdFile.new_line(
        f"Still plausible fixes / accepted fixes (TP): {ablation_build_and_sonar_Qube_check_plausible_fixes_TP} / {fix_tp_sonar_qube_check_passed} ({percent_ablation_build_and_sonar_Qube_check_plausible_fixes_TP:.2f}%)  ")
    mdFile.new_line(
        f"Still plausible fixes / accepted fixes (FP): {ablation_build_and_sonar_Qube_check_plausible_fixes_FP} / {fix_fp_sonar_qube_check_passed} ({percent_ablation_build_and_sonar_Qube_check_plausible_fixes_FP:.2f}%)  ")

    # Execution time section

    execution_time_classification = evaluation_results_file_df["executionTimeClassification"].sum(
    )
    execution_time_fix_tp = evaluation_results_file_df["executionTimeFixTP"].sum(
    )
    execution_time_fix_fp = evaluation_results_file_df["executionTimeFixFP"].sum(
    )

    total_execution_time = execution_time_classification + \
        execution_time_fix_tp + execution_time_fix_fp

    avg_execution_time_classification = execution_time_classification / total_rule_violations
    avg_execution_time_fix_tp = execution_time_fix_tp / classified_tp
    avg_execution_time_fix_fp = execution_time_fix_fp / classified_fp
    avg_execution_time = total_execution_time / total_rule_violations

    median_execution_time_classification = evaluation_results_file_df["executionTimeClassification"].median(
    )
    median_execution_time_fix_tp = evaluation_results_file_df.loc[
        evaluation_results_file_df["classification"] == "TP", "executionTimeFixTP"
    ].median()

    median_execution_time_fix_fp = evaluation_results_file_df.loc[
        evaluation_results_file_df["classification"] == "FP", "executionTimeFixFP"
    ].median()

    median_execution_time = (
        evaluation_results_file_df["executionTimeClassification"] +
        evaluation_results_file_df["executionTimeFixTP"] +
        evaluation_results_file_df["executionTimeFixFP"]
    ).median()

    mdFile.new_header(level=3, title="Execution time",
                      add_table_of_contents="n")
    mdFile.new_line("Total execution time: " +
                    str(round(nano_to_second(total_execution_time) / 60, 2)) + " minutes  ")

    mdFile.new_line("Execution time by sub-agent:  ")
    mdFile.new_line("Classification: " +
                    str(round(nano_to_second(execution_time_classification) / 60, 2)) + " minutes  ")
    mdFile.new_line(
        "Fix_TP: " + str(round(nano_to_second(execution_time_fix_tp) / 60, 2)) + " minutes  ")
    mdFile.new_line(
        "Fix_FP: " + str(round(nano_to_second(execution_time_fix_fp) / 60, 2)) + " minutes  ")

    mdFile.new_line()
    mdFile.new_line("Mean execution time: " +
                    str(round(nano_to_second(avg_execution_time) / 60, 2)) + " minutes  ")
    mdFile.new_line("Mean execution time by sub-agent:  ")
    mdFile.new_line("Classification: " +
                    str(round(nano_to_second(avg_execution_time_classification) / 60, 2)) + " minutes  ")
    mdFile.new_line(
        "Fix_TP: " + str(round(nano_to_second(avg_execution_time_fix_tp) / 60, 2)) + " minutes  ")
    mdFile.new_line(
        "Fix_FP: " + str(round(nano_to_second(avg_execution_time_fix_fp) / 60, 2)) + " minutes  ")

    mdFile.new_line()
    mdFile.new_line("Median execution time: " +
                    str(round(nano_to_second(median_execution_time) / 60, 2)) + " minutes  ")
    mdFile.new_line("Median execution time by sub-agent:  ")
    mdFile.new_line("Classification: " +
                    str(round(nano_to_second(median_execution_time_classification) / 60, 2)) + " minutes  ")
    mdFile.new_line(
        "Fix_TP: " + str(round(nano_to_second(median_execution_time_fix_tp) / 60, 2)) + " minutes  ")
    mdFile.new_line(
        "Fix_FP: " + str(round(nano_to_second(median_execution_time_fix_fp) / 60, 2)) + " minutes  ")

    # Time in maven build, test, analysis and LLM
    total_maven_build_time = evaluation_results_file_df["mavenBuildAddedTime"].sum(
    )
    total_maven_test_time = evaluation_results_file_df["mavenTestAddedTime"].sum(
    )
    total_sonar_qube_time = evaluation_results_file_df["mavenAnalysisAddedTime"].sum(
    )
    total_llm_time = evaluation_results_file_df["LLMAddedTime"].sum()
    mean_maven_build_time = evaluation_results_file_df["mavenBuildAddedTime"].mean(
    )
    mean_maven_test_time = evaluation_results_file_df["mavenTestAddedTime"].mean(
    )
    mean_sonar_qube_time = evaluation_results_file_df["mavenAnalysisAddedTime"].mean(
    )
    mean_llm_time = evaluation_results_file_df["LLMAddedTime"].mean()
    median_maven_build_time = evaluation_results_file_df["mavenBuildAddedTime"].median(
    )
    median_maven_test_time = evaluation_results_file_df["mavenTestAddedTime"].median(
    )
    median_sonar_qube_time = evaluation_results_file_df["mavenAnalysisAddedTime"].median(
    )
    median_llm_time = evaluation_results_file_df["LLMAddedTime"].median()
    mdFile.new_header(level=3, title="Maven Build, Test, SonarQube Analysis Time and LLM Time",
                      add_table_of_contents="n")
    mdFile.new_line(
        f"Total Maven Build Time: {round(nano_to_second(total_maven_build_time) / 60, 2)} minutes  ")
    mdFile.new_line(
        f"Total Maven Test Time: {round(nano_to_second(total_maven_test_time) / 60, 2)} minutes  ")
    mdFile.new_line(
        f"Total SonarQube Analysis Time: {round(nano_to_second(total_sonar_qube_time) / 60, 2)} minutes  ")
    mdFile.new_line(
        f"Total LLM Time: {round(nano_to_second(total_llm_time) / 60, 2)} minutes  ")
    mdFile.new_line()
    mdFile.new_line(
        f"Mean Maven Build Time (per warning): {round(nano_to_second(mean_maven_build_time) / 60, 2)} minutes  ")
    mdFile.new_line(
        f"Mean Maven Test Time (per warning): {round(nano_to_second(mean_maven_test_time) / 60, 2)} minutes  ")
    mdFile.new_line(
        f"Mean SonarQube Analysis Time (per warning): {round(nano_to_second(mean_sonar_qube_time) / 60, 2)} minutes  ")
    mdFile.new_line(
        f"Mean LLM Time (per warning): {round(nano_to_second(mean_llm_time) / 60, 2)} minutes  ")
    mdFile.new_line()
    mdFile.new_line(
        f"Median Maven Build Time (per warning): {round(nano_to_second(median_maven_build_time) / 60, 2)} minutes  ")
    mdFile.new_line(
        f"Median Maven Test Time (per warning): {round(nano_to_second(median_maven_test_time) / 60, 2)} minutes  ")
    mdFile.new_line(
        f"Median SonarQube Analysis Time (per warning): {round(nano_to_second(median_sonar_qube_time) / 60, 2)} minutes  ")
    mdFile.new_line(
        f"Median LLM Time (per warning): {round(nano_to_second(median_llm_time) / 60, 2)} minutes  ")

    total_time_outside_of_cca = total_maven_build_time + \
        total_maven_test_time + total_sonar_qube_time
    mean_time_outside_of_cca = total_time_outside_of_cca / total_rule_violations
    median_time_outside_of_cca = (
        evaluation_results_file_df["mavenBuildAddedTime"] +
        evaluation_results_file_df["mavenTestAddedTime"] +
        evaluation_results_file_df["mavenAnalysisAddedTime"]
    ).median()
    mdFile.new_line()
    mdFile.new_line(
        f"Total Time outside of CCA: {round(nano_to_second(total_time_outside_of_cca) / 60, 2)} minutes  ")
    mdFile.new_line(
        f"Mean Time outside of CCA: {round(nano_to_second(mean_time_outside_of_cca) / 60, 2)} minutes  ")
    mdFile.new_line(
        f"Median Time outside of CCA: {round(nano_to_second(median_time_outside_of_cca) / 60, 2)} minutes  ")

    percentage_of_time_outside_of_cca = (
        total_time_outside_of_cca / total_execution_time * 100
        if total_execution_time else 0
    )
    mdFile.new_line()
    mdFile.new_line(
        f"Percentage of Time outside of CCA: {percentage_of_time_outside_of_cca:.2f}%  ")

    percentage_of_time_in_llm = (
        total_llm_time / total_execution_time * 100
        if total_execution_time else 0
    )
    mdFile.new_line(
        f"Percentage of Time in LLM: {percentage_of_time_in_llm:.2f}%  ")

    percentage_of_time_executing_tools_and_middleware = (
        (total_execution_time - total_llm_time -
         total_time_outside_of_cca) / total_execution_time * 100
        if total_execution_time else 0
    )
    mdFile.new_line(
        f"Percentage of Time executing tools and middleware (everything else): {percentage_of_time_executing_tools_and_middleware:.2f}%  ")

    mdFile.new_line(
        "#### Execution time in subparts for unfixed warnings only  ")
    mdFile.new_line()

    mean_time_outside_of_cca_for_unfixed_warnings = (
        (evaluation_results_file_df.loc[
            ~evaluation_results_file_df["plausibleFix"], "mavenBuildAddedTime"
        ].sum() +
            evaluation_results_file_df.loc[
            ~evaluation_results_file_df["plausibleFix"], "mavenTestAddedTime"
        ].sum() +
            evaluation_results_file_df.loc[
            ~evaluation_results_file_df["plausibleFix"], "mavenAnalysisAddedTime"
        ].sum()) /
        evaluation_results_file_df.loc[
            ~evaluation_results_file_df["plausibleFix"]].shape[0]
        if evaluation_results_file_df.loc[
            ~evaluation_results_file_df["plausibleFix"]].shape[0] else 0
    )
    mdFile.new_line(
        f"Mean Time outside of CCA for unfixed warnings: {round(nano_to_second(mean_time_outside_of_cca_for_unfixed_warnings) / 60, 2)} minutes  ")

    mean_time_in_llm_for_unfixed_warnings = (
        evaluation_results_file_df.loc[
            ~evaluation_results_file_df["plausibleFix"], "LLMAddedTime"
        ].sum() /
        evaluation_results_file_df.loc[
            ~evaluation_results_file_df["plausibleFix"]].shape[0]
        if evaluation_results_file_df.loc[
            ~evaluation_results_file_df["plausibleFix"]].shape[0] else 0
    )
    mdFile.new_line(
        f"Mean Time in LLM for unfixed warnings: {round(nano_to_second(mean_time_in_llm_for_unfixed_warnings) / 60, 2)} minutes  ")

    percentage_of_time_outside_of_cca_for_unfixed_warnings = (
        (evaluation_results_file_df.loc[
            ~evaluation_results_file_df["plausibleFix"], "mavenBuildAddedTime"
        ].sum() +
            evaluation_results_file_df.loc[
            ~evaluation_results_file_df["plausibleFix"], "mavenTestAddedTime"
        ].sum() +
            evaluation_results_file_df.loc[
            ~evaluation_results_file_df["plausibleFix"], "mavenAnalysisAddedTime"
        ].sum()) /
        (evaluation_results_file_df.loc[
            ~evaluation_results_file_df["plausibleFix"], "executionTimeClassification"
        ].sum() + evaluation_results_file_df.loc[
            ~evaluation_results_file_df["plausibleFix"], "executionTimeFixTP"
        ].sum() + evaluation_results_file_df.loc[
            ~evaluation_results_file_df["plausibleFix"], "executionTimeFixFP"
        ].sum()) * 100
        if (evaluation_results_file_df.loc[
            ~evaluation_results_file_df["plausibleFix"], "executionTimeClassification"
        ].sum() + evaluation_results_file_df.loc[
            ~evaluation_results_file_df["plausibleFix"], "executionTimeFixTP"
        ].sum() + evaluation_results_file_df.loc[
            ~evaluation_results_file_df["plausibleFix"], "executionTimeFixFP"
        ].sum()) else 0
    )
    mdFile.new_line(
        f"Percentage of Time outside of CCA for unfixed warnings: {percentage_of_time_outside_of_cca_for_unfixed_warnings:.2f}%  ")

    percentage_of_time_in_llm_for_unfixed_warnings = (
        evaluation_results_file_df.loc[
            ~evaluation_results_file_df["plausibleFix"], "LLMAddedTime"
        ].sum() /
        (evaluation_results_file_df.loc[
            ~evaluation_results_file_df["plausibleFix"], "executionTimeClassification"
        ].sum() + evaluation_results_file_df.loc[
            ~evaluation_results_file_df["plausibleFix"], "executionTimeFixTP"
        ].sum() + evaluation_results_file_df.loc[
            ~evaluation_results_file_df["plausibleFix"], "executionTimeFixFP"
        ].sum()) * 100
        if (evaluation_results_file_df.loc[
            ~evaluation_results_file_df["plausibleFix"], "executionTimeClassification"
        ].sum() + evaluation_results_file_df.loc[
            ~evaluation_results_file_df["plausibleFix"], "executionTimeFixTP"
        ].sum() + evaluation_results_file_df.loc[
            ~evaluation_results_file_df["plausibleFix"], "executionTimeFixFP"
        ].sum()) else 0
    )
    mdFile.new_line(
        f"Percentage of Time in LLM for unfixed warnings: {percentage_of_time_in_llm_for_unfixed_warnings:.2f}%  ")

    percentage_of_time_executing_tools_and_middleware_for_unfixed_warnings = (
        (100 - percentage_of_time_in_llm_for_unfixed_warnings -
         percentage_of_time_outside_of_cca_for_unfixed_warnings)
    )
    mdFile.new_line(
        f"Percentage of Time executing tools and middleware (everything else) for unfixed warnings: {percentage_of_time_executing_tools_and_middleware_for_unfixed_warnings:.2f}%  ")

    # --- Tokens and Cost Calculations ---

    # Classification tokens
    tokens_input_uncached_classification = evaluation_results_file_df["tokensInputUncachedClassification"].sum(
    )
    tokens_input_cached_classification = evaluation_results_file_df["tokensInputCachedClassification"].sum(
    )
    tokens_input_classification = tokens_input_uncached_classification + \
        tokens_input_cached_classification
    tokens_output_classification = evaluation_results_file_df["tokensOutputClassification"].sum(
    )
    tokens_count_classification = tokens_input_classification + \
        tokens_output_classification

    # FixTP tokens
    tokens_input_uncached_fix_tp = evaluation_results_file_df["tokensInputUncachedFixTP"].sum(
    )
    tokens_input_cached_fix_tp = evaluation_results_file_df["tokensInputCachedFixTP"].sum(
    )
    tokens_input_fix_tp = tokens_input_uncached_fix_tp + tokens_input_cached_fix_tp
    tokens_output_fix_tp = evaluation_results_file_df["tokensOutputFixTP"].sum(
    )
    tokens_count_fix_tp = tokens_input_fix_tp + tokens_output_fix_tp

    # FixFP tokens
    tokens_input_uncached_fix_fp = evaluation_results_file_df["tokensInputUncachedFixFP"].sum(
    )
    tokens_input_cached_fix_fp = evaluation_results_file_df["tokensInputCachedFixFP"].sum(
    )
    tokens_input_fix_fp = tokens_input_uncached_fix_fp + tokens_input_cached_fix_fp
    tokens_output_fix_fp = evaluation_results_file_df["tokensOutputFixFP"].sum(
    )
    tokens_count_fix_fp = tokens_input_fix_fp + tokens_output_fix_fp

    # Totals
    total_tokens_input_uncached = tokens_input_uncached_classification + \
        tokens_input_uncached_fix_tp + tokens_input_uncached_fix_fp
    total_tokens_input_cached = tokens_input_cached_classification + \
        tokens_input_cached_fix_tp + tokens_input_cached_fix_fp
    total_tokens_input = tokens_input_classification + \
        tokens_input_fix_tp + tokens_input_fix_fp
    total_tokens_output = tokens_output_classification + \
        tokens_output_fix_tp + tokens_output_fix_fp
    total_tokens_count = tokens_count_classification + \
        tokens_count_fix_tp + tokens_count_fix_fp

    # Means
    mean_tokens_input_uncached_classification = evaluation_results_file_df["tokensInputUncachedClassification"].mean(
    )
    mean_tokens_input_cached_classification = evaluation_results_file_df["tokensInputCachedClassification"].mean(
    )
    mean_tokens_input_classification = mean_tokens_input_uncached_classification + \
        mean_tokens_input_cached_classification
    mean_tokens_output_classification = evaluation_results_file_df["tokensOutputClassification"].mean(
    )
    mean_tokens_count_classification = mean_tokens_input_classification + \
        mean_tokens_output_classification

    mean_tokens_input_uncached_fix_tp = (
        evaluation_results_file_df.loc[
            evaluation_results_file_df["classification"] == "TP", "tokensInputUncachedFixTP"
        ].mean()
        if classified_tp else 0
    )
    mean_tokens_input_cached_fix_tp = (
        evaluation_results_file_df.loc[
            evaluation_results_file_df["classification"] == "TP", "tokensInputCachedFixTP"
        ].mean()
        if classified_tp else 0
    )
    mean_tokens_input_fix_tp = mean_tokens_input_uncached_fix_tp + \
        mean_tokens_input_cached_fix_tp
    mean_tokens_output_fix_tp = (
        evaluation_results_file_df.loc[
            evaluation_results_file_df["classification"] == "TP", "tokensOutputFixTP"
        ].mean()
        if classified_tp else 0
    )
    mean_tokens_count_fix_tp = mean_tokens_input_fix_tp + mean_tokens_output_fix_tp

    mean_tokens_input_uncached_fix_fp = (
        evaluation_results_file_df.loc[
            evaluation_results_file_df["classification"] == "FP", "tokensInputUncachedFixFP"
        ].mean()
        if classified_fp else 0
    )
    mean_tokens_input_cached_fix_fp = (
        evaluation_results_file_df.loc[
            evaluation_results_file_df["classification"] == "FP", "tokensInputCachedFixFP"
        ].mean()
        if classified_fp else 0
    )
    mean_tokens_input_fix_fp = mean_tokens_input_uncached_fix_fp + \
        mean_tokens_input_cached_fix_fp
    mean_tokens_output_fix_fp = (
        evaluation_results_file_df.loc[
            evaluation_results_file_df["classification"] == "FP", "tokensOutputFixFP"
        ].mean()
        if classified_fp else 0
    )
    mean_tokens_count_fix_fp = mean_tokens_input_fix_fp + mean_tokens_output_fix_fp

    mean_total_tokens_input_uncached = (
        (evaluation_results_file_df["tokensInputUncachedClassification"].sum() +
         evaluation_results_file_df["tokensInputUncachedFixTP"].sum() +
         evaluation_results_file_df["tokensInputUncachedFixFP"].sum()) / total_rule_violations
        if total_rule_violations else 0
    )
    mean_total_tokens_input_cached = (
        (evaluation_results_file_df["tokensInputCachedClassification"].sum() +
         evaluation_results_file_df["tokensInputCachedFixTP"].sum() +
         evaluation_results_file_df["tokensInputCachedFixFP"].sum()) / total_rule_violations
        if total_rule_violations else 0
    )
    mean_total_tokens_input = (
        (evaluation_results_file_df["tokensInputUncachedClassification"].sum() +
         evaluation_results_file_df["tokensInputCachedClassification"].sum() +
         evaluation_results_file_df["tokensInputUncachedFixTP"].sum() +
         evaluation_results_file_df["tokensInputCachedFixTP"].sum() +
         evaluation_results_file_df["tokensInputUncachedFixFP"].sum() +
         evaluation_results_file_df["tokensInputCachedFixFP"].sum()) / total_rule_violations
        if total_rule_violations else 0
    )
    mean_total_tokens_output = (
        (evaluation_results_file_df["tokensOutputClassification"].sum() +
         evaluation_results_file_df["tokensOutputFixTP"].sum() +
         evaluation_results_file_df["tokensOutputFixFP"].sum()) / total_rule_violations
        if total_rule_violations else 0
    )
    mean_total_tokens_count = mean_total_tokens_input + mean_total_tokens_output

    # Medians
    median_tokens_input_uncached_classification = evaluation_results_file_df["tokensInputUncachedClassification"].median(
    )
    median_tokens_input_cached_classification = evaluation_results_file_df["tokensInputCachedClassification"].median(
    )
    median_tokens_input_classification = median_tokens_input_uncached_classification + \
        median_tokens_input_cached_classification
    median_tokens_output_classification = evaluation_results_file_df["tokensOutputClassification"].median(
    )
    median_tokens_count_classification = median_tokens_input_classification + \
        median_tokens_output_classification

    median_tokens_input_uncached_fix_tp = evaluation_results_file_df.loc[
        evaluation_results_file_df["classification"] == "TP", "tokensInputUncachedFixTP"
    ].median(
    )
    median_tokens_input_cached_fix_tp = evaluation_results_file_df.loc[
        evaluation_results_file_df["classification"] == "TP", "tokensInputCachedFixTP"
    ].median(
    )
    median_tokens_input_fix_tp = median_tokens_input_uncached_fix_tp + \
        median_tokens_input_cached_fix_tp
    median_tokens_output_fix_tp = evaluation_results_file_df.loc[
        evaluation_results_file_df["classification"] == "TP", "tokensOutputFixTP"
    ].median(
    )
    median_tokens_count_fix_tp = median_tokens_input_fix_tp + median_tokens_output_fix_tp

    median_tokens_input_uncached_fix_fp = evaluation_results_file_df.loc[
        evaluation_results_file_df["classification"] == "FP", "tokensInputUncachedFixFP"
    ].median(
    )
    median_tokens_input_cached_fix_fp = evaluation_results_file_df.loc[
        evaluation_results_file_df["classification"] == "FP", "tokensInputCachedFixFP"
    ].median(
    )
    median_tokens_input_fix_fp = median_tokens_input_uncached_fix_fp + \
        median_tokens_input_cached_fix_fp
    median_tokens_output_fix_fp = evaluation_results_file_df.loc[
        evaluation_results_file_df["classification"] == "FP", "tokensOutputFixFP"
    ].median(
    )
    median_tokens_count_fix_fp = median_tokens_input_fix_fp + median_tokens_output_fix_fp

    # Median calculations for total tokens (over all instances)
    median_total_tokens_input_uncached = (
        evaluation_results_file_df["tokensInputUncachedClassification"] +
        evaluation_results_file_df["tokensInputUncachedFixTP"] +
        evaluation_results_file_df["tokensInputUncachedFixFP"]
    ).median()
    median_total_tokens_input_cached = (
        evaluation_results_file_df["tokensInputCachedClassification"] +
        evaluation_results_file_df["tokensInputCachedFixTP"] +
        evaluation_results_file_df["tokensInputCachedFixFP"]
    ).median()
    median_total_tokens_input = (
        evaluation_results_file_df["tokensInputUncachedClassification"] +
        evaluation_results_file_df["tokensInputCachedClassification"] +
        evaluation_results_file_df["tokensInputUncachedFixTP"] +
        evaluation_results_file_df["tokensInputCachedFixTP"] +
        evaluation_results_file_df["tokensInputUncachedFixFP"] +
        evaluation_results_file_df["tokensInputCachedFixFP"]
    ).median()
    median_total_tokens_output = (
        evaluation_results_file_df["tokensOutputClassification"] +
        evaluation_results_file_df["tokensOutputFixTP"] +
        evaluation_results_file_df["tokensOutputFixFP"]
    ).median()
    median_total_tokens_count = median_total_tokens_input + median_total_tokens_output

    # Cost
    tokens_cost_classification = evaluation_results_file_df["costClassification"].sum(
    )
    tokens_cost_fix_tp = evaluation_results_file_df["costFixTP"].sum()
    tokens_cost_fix_fp = evaluation_results_file_df["costFixFP"].sum()
    total_tokens_cost = tokens_cost_classification + \
        tokens_cost_fix_tp + tokens_cost_fix_fp

    avg_cost_classification = evaluation_results_file_df["costClassification"].mean(
    )
    avg_cost_fix_tp = (
        evaluation_results_file_df.loc[
            evaluation_results_file_df["classification"] == "TP", "costFixTP"
        ].mean()
        if classified_tp else 0
    )
    avg_cost_fix_fp = (
        evaluation_results_file_df.loc[
            evaluation_results_file_df["classification"] == "FP", "costFixFP"
        ].mean()
        if classified_fp else 0
    )
    avg_cost = (
        (evaluation_results_file_df["costClassification"].sum() +
         evaluation_results_file_df["costFixTP"].sum() +
         evaluation_results_file_df["costFixFP"].sum()) / total_rule_violations
        if total_rule_violations else 0
    )

    median_cost_classification = evaluation_results_file_df["costClassification"].median(
    )
    median_cost_fix_tp = evaluation_results_file_df.loc[
        evaluation_results_file_df["classification"] == "TP", "costFixTP"
    ].median()
    median_cost_fix_fp = evaluation_results_file_df.loc[
        evaluation_results_file_df["classification"] == "FP", "costFixFP"
    ].median()
    median_cost = (evaluation_results_file_df["costClassification"] +
                   evaluation_results_file_df["costFixTP"] + evaluation_results_file_df["costFixFP"]).median()

    # --- Markdown Output ---

    mdFile.new_header(level=3, title="Cost",
                      add_table_of_contents="n")

    mdFile.new_header(level=4, title="Tokens Count",
                      add_table_of_contents="n")
    mdFile.new_line(f"Total tokens count: {total_tokens_count}  ")
    mdFile.new_line(
        f"Total tokens input uncached: {total_tokens_input_uncached}  ")
    mdFile.new_line(
        f"Total tokens input cached: {total_tokens_input_cached}  ")
    mdFile.new_line(f"Total tokens input: {total_tokens_input}  ")
    mdFile.new_line(f"Total tokens output: {total_tokens_output}  ")

    mdFile.new_line("Tokens by sub-agent:  ")
    mdFile.new_line(f"Classification: {tokens_count_classification} (input uncached: {tokens_input_uncached_classification}, input cached: {tokens_input_cached_classification}, input: {tokens_input_classification}, output: {tokens_output_classification})  ")
    mdFile.new_line(
        f"Fix_TP: {tokens_count_fix_tp} (input uncached: {tokens_input_uncached_fix_tp}, input cached: {tokens_input_cached_fix_tp}, input: {tokens_input_fix_tp}, output: {tokens_output_fix_tp})  ")
    mdFile.new_line(
        f"Fix_FP: {tokens_count_fix_fp} (input uncached: {tokens_input_uncached_fix_fp}, input cached: {tokens_input_cached_fix_fp}, input: {tokens_input_fix_fp}, output: {tokens_output_fix_fp})  ")

    mdFile.new_header(level=4, title="Mean Tokens Count",
                      add_table_of_contents="n")
    mdFile.new_line(
        f"Mean total tokens count: {mean_total_tokens_count:.2f}  ")
    mdFile.new_line(
        f"Mean total tokens input uncached: {mean_total_tokens_input_uncached:.2f}  ")
    mdFile.new_line(
        f"Mean total tokens input cached: {mean_total_tokens_input_cached:.2f}  ")
    mdFile.new_line(
        f"Mean total tokens input: {mean_total_tokens_input:.2f}  ")
    mdFile.new_line(
        f"Mean total tokens output: {mean_total_tokens_output:.2f}  ")

    mdFile.new_line("Mean tokens by sub-agent:  ")
    mdFile.new_line(f"Classification: {mean_tokens_count_classification:.2f} (input uncached: {mean_tokens_input_uncached_classification:.2f}, input cached: {mean_tokens_input_cached_classification:.2f}, input: {mean_tokens_input_classification:.2f}, output: {mean_tokens_output_classification:.2f})  ")
    mdFile.new_line(f"Fix_TP: {mean_tokens_count_fix_tp:.2f} (input uncached: {mean_tokens_input_uncached_fix_tp:.2f}, input cached: {mean_tokens_input_cached_fix_tp:.2f}, input: {mean_tokens_input_fix_tp:.2f}, output: {mean_tokens_output_fix_tp:.2f})  ")
    mdFile.new_line(f"Fix_FP: {mean_tokens_count_fix_fp:.2f} (input uncached: {mean_tokens_input_uncached_fix_fp:.2f}, input cached: {mean_tokens_input_cached_fix_fp:.2f}, input: {mean_tokens_input_fix_fp:.2f}, output: {mean_tokens_output_fix_fp:.2f})  ")

    mdFile.new_header(level=4, title="Median Tokens Count",
                      add_table_of_contents="n")
    mdFile.new_line(
        f"Median total tokens count: {median_total_tokens_count:.2f}  ")
    mdFile.new_line(
        f"Median total tokens input uncached: {median_total_tokens_input_uncached:.2f}  ")
    mdFile.new_line(
        f"Median total tokens input cached: {median_total_tokens_input_cached:.2f}  ")
    mdFile.new_line(
        f"Median total tokens input: {median_total_tokens_input:.2f}  ")
    mdFile.new_line(
        f"Median total tokens output: {median_total_tokens_output:.2f}  ")

    mdFile.new_line("Median tokens by sub-agent:  ")
    mdFile.new_line(f"Classification: {median_tokens_count_classification:.2f} (input uncached: {median_tokens_input_uncached_classification:.2f}, input cached: {median_tokens_input_cached_classification:.2f}, input: {median_tokens_input_classification:.2f}, output: {median_tokens_output_classification:.2f})  ")
    mdFile.new_line(f"Fix_TP: {median_tokens_count_fix_tp:.2f} (input uncached: {median_tokens_input_uncached_fix_tp:.2f}, input cached: {median_tokens_input_cached_fix_tp:.2f}, input: {median_tokens_input_fix_tp:.2f}, output: {median_tokens_output_fix_tp:.2f})  ")
    mdFile.new_line(f"Fix_FP: {median_tokens_count_fix_fp:.2f} (input uncached: {median_tokens_input_uncached_fix_fp:.2f}, input cached: {median_tokens_input_cached_fix_fp:.2f}, input: {median_tokens_input_fix_fp:.2f}, output: {median_tokens_output_fix_fp:.2f})  ")

    mdFile.new_header(level=4, title="Tokens Cost",
                      add_table_of_contents="n")
    mdFile.new_line(f"Total Cost: {total_tokens_cost:.6f} USD  ")
    mdFile.new_line("Cost by sub-agent:  ")
    mdFile.new_line(f"Classification: {tokens_cost_classification:.6f} USD  ")
    mdFile.new_line(f"Fix_TP: {tokens_cost_fix_tp:.6f} USD  ")
    mdFile.new_line(f"Fix_FP: {tokens_cost_fix_fp:.6f} USD  ")

    mdFile.new_header(level=4, title="Average Tokens Cost",
                      add_table_of_contents="n")
    mdFile.new_line(f"Average Total Cost: {avg_cost:.6f} USD  ")
    mdFile.new_line("Average cost by sub-agent:  ")
    mdFile.new_line(f"Classification: {avg_cost_classification:.6f} USD  ")
    mdFile.new_line(f"Fix_TP: {avg_cost_fix_tp:.6f} USD  ")
    mdFile.new_line(f"Fix_FP: {avg_cost_fix_fp:.6f} USD  ")

    mdFile.new_header(level=4, title="Median Tokens Cost",
                      add_table_of_contents="n")
    mdFile.new_line(f"Median Total Cost: {median_cost:.6f} USD  ")
    mdFile.new_line("Median cost by sub-agent:  ")
    mdFile.new_line(f"Classification: {median_cost_classification:.6f} USD  ")
    mdFile.new_line(f"Fix_TP: {median_cost_fix_tp:.6f} USD  ")
    mdFile.new_line(f"Fix_FP: {median_cost_fix_fp:.6f} USD  ")

    mdFile.create_md_file()


def nano_to_second(time_in_nano: float):
    return round(time_in_nano / 1_000_000_000, 4)


if __name__ == "__main__":
    calculate_stats_from_evaluation_results()

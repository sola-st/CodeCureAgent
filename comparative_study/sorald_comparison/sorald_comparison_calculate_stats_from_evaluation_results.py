import click
import mdutils
import pandas as pd


@click.command()
@click.argument(
    "sorald-evaluation-results-file",
    type=click.File()
)
@click.option(
    "--target-md-file-path",
    "-t",
    default="./analysis_results_overview.md",
    help="Path where the stats file should be written."
)
def sorald_comparison_calculate_stats_from_evaluation_results(sorald_evaluation_results_file: click.File, target_md_file_path: str):

    evaluation_results_file_df = pd.read_csv(sorald_evaluation_results_file)

    mdFile = mdutils.MdUtils(
        file_name=target_md_file_path, title='Sorald Comparison Analysis Results', title_header_style="atx")
    mdFile.new_header(level=2, title="Total warnings covered by Sorald",
                      add_table_of_contents="n")

    total_rule_violations = evaluation_results_file_df["instanceID"].count()

    mdFile.new_line(
        str(total_rule_violations))

    # ---- CodeCureAgent stats on the samples that Sorald supports

    mdFile.new_header(level=2, title="CodeCureAgent stats on the Sorald-covered warnings",
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

    # ---- Sorald stats on the samples that Sorald supports

    mdFile.new_header(level=2, title="Sorald stats on the Sorald-covered warnings",
                      add_table_of_contents="n")

    mdFile.new_header(level=3, title="Separate Performance Stats",
                      add_table_of_contents="n")

    # Sorald fix created
    sorald_fix_created = (
        evaluation_results_file_df["soraldFixCreated"] == True).sum()
    percent_sorald_fix_created = 100 * sorald_fix_created / \
        total_rule_violations if total_rule_violations else 0
    mdFile.new_line(
        f"Sorald fix created: {sorald_fix_created}/{total_rule_violations} ({percent_sorald_fix_created:.2f}%)  ")

    mdFile.new_line()

    # Sorald build successful (only when fix created)
    build_successful_and_fix_created = ((evaluation_results_file_df["soraldFixCreated"] == True) &
                                        (evaluation_results_file_df["soraldBuildSuccessful"] == True)).sum()
    percent_build_successful = 100 * build_successful_and_fix_created / \
        sorald_fix_created if sorald_fix_created else 0
    mdFile.new_line(
        f"Sorald build successful (of instances where fix was created): {build_successful_and_fix_created}/{sorald_fix_created} ({percent_build_successful:.2f}%)  ")

    percent_build_successful_of_all_warnings = 100 * build_successful_and_fix_created / \
        total_rule_violations if total_rule_violations else 0
    mdFile.new_line(
        f"Sorald build successful (after creating a fix) (of all instances): {build_successful_and_fix_created}/{total_rule_violations} ({percent_build_successful_of_all_warnings:.2f}%)  ")

    mdFile.new_line()

    # Sorald correct removal (soraldNumberOfTargetWarningsRemoved >= 1)
    correct_removal = ((evaluation_results_file_df["soraldNumberOfTargetWarningsRemoved"].fillna(0).astype(int) >= 1) &
                       (evaluation_results_file_df["soraldFixCreated"] == True)).sum()
    percent_correct_removal = 100 * correct_removal / \
        sorald_fix_created if sorald_fix_created else 0
    mdFile.new_line(
        f"Sorald removal of target warning (removed >=1 warnings) (of instances where fix was created): {correct_removal}/{sorald_fix_created} ({percent_correct_removal:.2f}%)  ")

    percent_correct_removal_of_all_warnings = 100 * correct_removal / \
        total_rule_violations if total_rule_violations else 0
    mdFile.new_line(
        f"Sorald removal of target warning (removed >=1 warnings) (after creating a fix) (of all instances): {correct_removal}/{total_rule_violations} ({percent_correct_removal_of_all_warnings:.2f}%)  ")

    mdFile.new_line()

    # Sorald no new warning introduced
    no_new_warning = ((evaluation_results_file_df["soraldNoNewWarningIntroduced"] == True) &
                      (evaluation_results_file_df["soraldFixCreated"] == True)).sum()
    percent_no_new_warning = 100 * no_new_warning / \
        sorald_fix_created if sorald_fix_created else 0
    mdFile.new_line(
        f"Sorald no new warning introduced (of instances where fix was created): {no_new_warning}/{sorald_fix_created} ({percent_no_new_warning:.2f}%)  ")

    percent_no_new_warning_of_all_warnings = 100 * no_new_warning / \
        total_rule_violations if total_rule_violations else 0
    mdFile.new_line(
        f"Sorald no new warning introduced (after creating a fix) (of all instances): {no_new_warning}/{total_rule_violations} ({percent_no_new_warning_of_all_warnings:.2f}%)  ")

    mdFile.new_line()

    # Sorald test successful (only when fix created and build successful)
    test_successful = ((evaluation_results_file_df["soraldFixCreated"] == True) &
                       (evaluation_results_file_df["soraldBuildSuccessful"] == True) &
                       (evaluation_results_file_df["soraldTestSuccessful"] == True)).sum()
    eligible_for_test = ((evaluation_results_file_df["soraldFixCreated"] == True) &
                         (evaluation_results_file_df["soraldBuildSuccessful"] == True)).sum()
    percent_test_successful = 100 * test_successful / \
        eligible_for_test if eligible_for_test else 0
    mdFile.new_line(
        f"Sorald test successful (of instances where fix created and build successful): {test_successful}/{eligible_for_test} ({percent_test_successful:.2f}%)  ")

    percent_test_successful_of_all_warnings = 100 * test_successful / \
        total_rule_violations if total_rule_violations else 0
    mdFile.new_line(
        f"Sorald test successful (after creating a fix and passing build) (of all instances): {test_successful}/{total_rule_violations} ({percent_test_successful_of_all_warnings:.2f}%)  ")

    mdFile.new_line()

    # Sorald TP assumption soundness

    tp_assumption_not_sound = (
        evaluation_results_file_df["soraldTPAssumptionSoundness"] == "Not sound").sum()
    tp_assumption_sound = total_rule_violations - tp_assumption_not_sound
    percent_tp_assumption_sound = 100 * tp_assumption_sound / \
        total_rule_violations if total_rule_violations else 0
    mdFile.new_line(
        f"Sorald TP assumption sound (not in conjunction with if a fix was created): {tp_assumption_sound}/{total_rule_violations} ({percent_tp_assumption_sound:.2f}%)  ")
    mdFile.new_line(
        f"Sorald instances where TP assumption was not sound: {tp_assumption_not_sound} ({100.0 - percent_tp_assumption_sound:.2f}%)  ")

    mdFile.new_line()

    # Sorald fix correctness
    fix_correct = ((evaluation_results_file_df["soraldFixCorrectness"] == "Correct") &
                   (evaluation_results_file_df["soraldFixCreated"] == True)).sum()
    fix_not_correct = ((evaluation_results_file_df["soraldFixCorrectness"] == "Not correct") &
                       (evaluation_results_file_df["soraldFixCreated"] == True)).sum()
    total_fix_correctness = fix_correct + fix_not_correct
    percent_fix_correct = 100 * fix_correct / \
        total_fix_correctness if total_fix_correctness else 0
    mdFile.new_line(
        f"Sorald fix correct (and fix created) (of instances where fix was created (and manually inspected)): {fix_correct}/{total_fix_correctness} ({percent_fix_correct:.2f}%)  ")

    fix_correct_all_warnings = (
        evaluation_results_file_df["soraldFixCorrectness"] == "Correct").sum()
    fix_not_correct_all_warnings = (
        evaluation_results_file_df["soraldFixCorrectness"] == "Not correct").sum()
    total_fix_correctness_all_warnings = fix_correct_all_warnings + \
        fix_not_correct_all_warnings

    percent_fix_correct = 100 * fix_correct / \
        total_fix_correctness_all_warnings if total_fix_correctness_all_warnings else 0
    mdFile.new_line(
        f"Sorald fix correct (and fix created) (of all manually inspected warnings): {fix_correct}/{total_fix_correctness_all_warnings} ({percent_fix_correct:.2f}%)  ")

    percent_fix_correct_all_warnings = 100 * fix_correct / \
        total_fix_correctness_all_warnings if total_fix_correctness_all_warnings else 0
    mdFile.new_line(
        f"Sorald fix correct (not only for instances where fix was created (no fix => incorrect fix) (of all manually inspected warnings)): {fix_correct_all_warnings}/{total_fix_correctness_all_warnings} ({percent_fix_correct_all_warnings:.2f}%)  ")

    mdFile.new_line()

    # Sorald code smell outside of SonarQube introduced
    code_smell_outside = evaluation_results_file_df["codeSmellOutsideOfSonarQubeIntroduced"].astype(
        str).str.startswith("True").sum()
    percent_code_smell_outside = 100 * code_smell_outside / \
        total_fix_correctness_all_warnings if total_fix_correctness_all_warnings else 0
    mdFile.new_line(
        f"Sorald further code smell introduced not reported by SonarQube (of all manually inspected warnings): {code_smell_outside}/{total_fix_correctness_all_warnings} ({percent_code_smell_outside:.2f}%)  ")

    code_smell_outside_and_correct_fix = ((evaluation_results_file_df["codeSmellOutsideOfSonarQubeIntroduced"].astype(str).str.startswith("True")) & (evaluation_results_file_df["soraldFixCorrectness"] == "Correct") &
                                          (evaluation_results_file_df["soraldFixCreated"] == True)).sum()
    percent_code_smell_outside = 100 * code_smell_outside_and_correct_fix / \
        fix_correct if fix_correct else 0
    mdFile.new_line(
        f"Sorald further code smell introduced not reported by SonarQube (of all warnings where all other stats pass): {code_smell_outside_and_correct_fix}/{fix_correct} ({percent_code_smell_outside:.2f}%)  ")

    mdFile.new_line()
    mdFile.new_header(level=3, title="Combined Performance Stats (Different checks are added together to form a final total performance of Sorald)",
                      add_table_of_contents="n")

    # Sorald fix successful
    mdFile.new_line(
        f"Sorald fix created: {sorald_fix_created}/{total_rule_violations} ({percent_sorald_fix_created:.2f}%)  ")

    # Sorald fix successful + build successful
    mdFile.new_line(
        f"Sorald fix created + build successful: {build_successful_and_fix_created}/{total_rule_violations} ({percent_build_successful_of_all_warnings:.2f}%)  ")

    # Sorald fix successful + build successful + test successful

    test_successful_build_successful_and_fix_created = ((evaluation_results_file_df["soraldFixCreated"] == True) &
                                                        (evaluation_results_file_df["soraldBuildSuccessful"] == True) &
                                                        (evaluation_results_file_df["soraldTestSuccessful"] == True)).sum()

    percent_test_successful_build_successful_and_fix_created_of_all_warnings = 100 * test_successful_build_successful_and_fix_created / \
        total_rule_violations if total_rule_violations else 0

    mdFile.new_line(
        f"Sorald fix created + build successful + test successful: {test_successful_build_successful_and_fix_created}/{total_rule_violations} ({percent_test_successful_build_successful_and_fix_created_of_all_warnings:.2f}%)  ")

    # Sorald fix successful + build successful + test successful + target warning removed
    fix_created_build_test_target_removed = ((evaluation_results_file_df["soraldFixCreated"] == True) &
                                             (evaluation_results_file_df["soraldBuildSuccessful"] == True) &
                                             (evaluation_results_file_df["soraldTestSuccessful"] == True) &
                                             (evaluation_results_file_df["soraldNumberOfTargetWarningsRemoved"].fillna(0).astype(int) >= 1)).sum()

    percent_fix_created_build_test_target_removed = 100 * fix_created_build_test_target_removed / \
        total_rule_violations if total_rule_violations else 0

    mdFile.new_line(
        f"Sorald fix created + build successful + test successful + target warning removed: {fix_created_build_test_target_removed}/{total_rule_violations} ({percent_fix_created_build_test_target_removed:.2f}%)  ")

    # Sorald fix successful + build successful + test successful + target warning removed + no other warning introduced
    fix_created_build_test_target_removed_no_other_introduced = (
        (evaluation_results_file_df["soraldFixCreated"] == True) &
        (evaluation_results_file_df["soraldBuildSuccessful"] == True) &
        (evaluation_results_file_df["soraldTestSuccessful"] == True) &
        (evaluation_results_file_df["soraldNumberOfTargetWarningsRemoved"].fillna(0).astype(int) >= 1) &
        (evaluation_results_file_df["soraldNoNewWarningIntroduced"] == True)
    ).sum()

    percent_fix_created_build_test_target_removed_no_other_removed = 100 * fix_created_build_test_target_removed_no_other_introduced / \
        total_rule_violations if total_rule_violations else 0

    mdFile.new_line(
        f"Sorald fix created + build successful + test successful + target warning removed + no other warning introduced: {fix_created_build_test_target_removed_no_other_introduced}/{total_rule_violations} ({percent_fix_created_build_test_target_removed_no_other_removed:.2f}%)  ")

    # Sorald fix successful + build successful + test successful + target warning removed + no other warning introduced + fix correct
    fix_created_build_test_target_removed_no_other_introduced_fix_correct = (
        (evaluation_results_file_df["soraldFixCreated"] == True) &
        (evaluation_results_file_df["soraldBuildSuccessful"] == True) &
        (evaluation_results_file_df["soraldTestSuccessful"] == True) &
        (evaluation_results_file_df["soraldNumberOfTargetWarningsRemoved"].fillna(0).astype(int) >= 1) &
        (evaluation_results_file_df["soraldNoNewWarningIntroduced"] == True) &
        (evaluation_results_file_df["soraldFixCorrectness"] == "Correct")
    ).sum()

    percent_fix_created_build_test_target_removed_no_other_introduced_fix_correct = 100 * fix_created_build_test_target_removed_no_other_introduced_fix_correct / \
        total_fix_correctness_all_warnings if total_fix_correctness_all_warnings else 0

    mdFile.new_line(
        f"Sorald fix created + build successful + test successful + target warning removed + no other warning introduced + fix correct: {fix_created_build_test_target_removed_no_other_introduced_fix_correct}/{total_fix_correctness_all_warnings} ({percent_fix_created_build_test_target_removed_no_other_introduced_fix_correct:.2f}%)  ")

    # Sorald fix successful + build successful + test successful + target warning removed + no other warning introduced + fix correct + no code smell outside introduced
    fix_created_build_test_target_removed_no_other_introduced_fix_correct_no_code_smell_outside = (
        (evaluation_results_file_df["soraldFixCreated"] == True) &
        (evaluation_results_file_df["soraldBuildSuccessful"] == True) &
        (evaluation_results_file_df["soraldTestSuccessful"] == True) &
        (evaluation_results_file_df["soraldNumberOfTargetWarningsRemoved"].fillna(0).astype(int) >= 1) &
        (evaluation_results_file_df["soraldNoNewWarningIntroduced"] == True) &
        (evaluation_results_file_df["soraldFixCorrectness"] == "Correct") &
        (~evaluation_results_file_df["codeSmellOutsideOfSonarQubeIntroduced"].astype(
            str).str.startswith("True"))
    ).sum()

    percent_fix_created_build_test_target_removed_no_other_introduced_fix_correct_no_code_smell_outside = 100 * fix_created_build_test_target_removed_no_other_introduced_fix_correct_no_code_smell_outside / \
        total_fix_correctness_all_warnings if total_fix_correctness_all_warnings else 0

    mdFile.new_line(
        f"Sorald fix created + build successful + test successful + target warning removed + no other warning introduced + fix correct + no code smell outside introduced: {fix_created_build_test_target_removed_no_other_introduced_fix_correct_no_code_smell_outside}/{total_fix_correctness_all_warnings} ({percent_fix_created_build_test_target_removed_no_other_introduced_fix_correct_no_code_smell_outside:.2f}%)  ")

    mdFile.new_header(level=3, title="Time Efficiency",
                      add_table_of_contents="n")

    # Sorald fixing time (mean/median)
    fixing_times = evaluation_results_file_df["soraldFixingTimeInMs"
                                              ].dropna().astype(float)
    mean_fixing_time = fixing_times.mean() if not fixing_times.empty else 0
    median_fixing_time = fixing_times.median() if not fixing_times.empty else 0
    mdFile.new_line(
        f"Sorald fixing time: mean={milli_to_second(mean_fixing_time):.2f}s, median={milli_to_second(median_fixing_time):.2f}s")

    mdFile.create_md_file()


def milli_to_second(time_in_milli: float):
    return round(time_in_milli / 1_000, 4)


if __name__ == "__main__":
    sorald_comparison_calculate_stats_from_evaluation_results()

import os
from agent_core.agents.base import BaseAgent
from agent_core.commands import repository_operations
from agent_core.commands.repository_operations import BuildError
from agent_core.commands import sonar_qube_analysis
import subprocess
from agent_core.logs import logger
from agent_core.utils.write_fix_utils.change_tracking import FileChanges, BeforeAfterMapping
from agent_core.app.main import shutdown
from agent_core.utils.path_utils.path_utils import find_all_folders, preprocess_paths, sanitize_and_shorten_file_path

import json
import re


def approve_changes(changes_dicts: list[dict], all_file_changes: list[FileChanges], agent: BaseAgent) -> str:
    """
    Implements the ChangeApprover functionality. 
    First tries to rebuild the project. 
    If successful SonarQube is rerun and we check if the targeted violation was removed and no new violations introduced. 
    Finally if also successful a query to a Reviewer LLM is made that either approves or reject. 
    The ChangeApprover gives appropriate feedback to the agent for each of the validation steps.
    """

    accepted = True

    accepted, build_message = try_to_build_changed_project(
        all_file_changes, agent)

    if not accepted:
        return reject([build_message], changes_dicts, agent)

    # Check if a suppression was actually added if we are in the task of suppressing the violation
    # Else the agent could potentially pass the tests by only removing the target line (accidentally)
    if agent.current_state == "fix_fp":
        accepted, suppression_inserted_message = check_if_suppression_literal_inserted(
            changes_dicts, agent)

        # Only give the agent feedback about this check if the check failed
        if not accepted:
            return reject([build_message, suppression_inserted_message], changes_dicts, agent)

    accepted, sonar_qube_message = check_sonar_qube_report(
        all_file_changes, agent)

    if not accepted:
        return reject([build_message, sonar_qube_message], changes_dicts, agent)

    accepted, tests_message = try_to_run_tests(agent)

    if not accepted:
        return reject([build_message, sonar_qube_message, tests_message], changes_dicts, agent)

    if accepted:
        return approve([build_message, sonar_qube_message, tests_message], changes_dicts, agent)
    else:
        return reject([build_message, sonar_qube_message, tests_message], changes_dicts, agent)


def try_to_build_changed_project(all_file_changes: list[FileChanges], agent: BaseAgent) -> tuple[bool, str]:
    """
    Tries to build the project with the applied changes.
    If the maven build wasn't successful we return a cleaned build output.

    Returns:
        (accepted, message): 
        accepted: (bool) Flag if the build was accepted (successful)\n
        message: (str) The cleaned build message if there was a build failure, else a success message
    """

    try:
        repository_operations.build_project(agent)

        return True, "Project was successfully built with the applied changes."

    except BuildError as build_error:
        return False, "Building the project failed! There were new compilation errors introduced by your fix attempt.  \n" + extract_build_error_information(build_error, agent) + "\n" + show_changed_code(all_file_changes)

    except subprocess.TimeoutExpired as timeout_error:
        return False, f"Building the project failed with a timeout after {timeout_error.timeout / 60} minutes."


def show_changed_code(all_file_changes: list[FileChanges]) -> str:
    """
    Show the changed lines of code of the changed files to give the agent an idea about what it has done and what it maybe has done wrong.
    """

    changed_code_message = "To help you understand what you did wrong, below you are provided with the relevant lines of code after applying your proposed changes (with made insertions and deletions):  "

    for file_changes in all_file_changes:
        changed_code_message += f"\n\nFile {file_changes.file_path}:\n"

        first_changed_line = -1
        last_changed_line = -1

        for mapping in file_changes.change_tracked_lines.map_line_indices_before_after_change:
            if mapping.inserted or mapping.deleted or mapping.modified:
                if first_changed_line == -1 or first_changed_line > mapping.after_line:
                    first_changed_line = mapping.after_line
                if last_changed_line < mapping.after_line:
                    last_changed_line = mapping.after_line

        if first_changed_line == -1 or last_changed_line == -1:
            logger.error("A FileChanges object had no first and/or last line of changed lines.",
                         "This should only happen if a changes_dict was empty.")
            continue
        first_line_to_read = max(first_changed_line - 5, 1)
        last_line_to_read = min(last_changed_line + 5,
                                len(file_changes.change_tracked_lines))

        map_line_indices_before_after_change = file_changes.change_tracked_lines.map_line_indices_before_after_change

        for i in range(first_line_to_read - 1, last_line_to_read):

            line_mappings_of_line = list(filter(
                lambda line_mapping, index=i: line_mapping.after_line == index + 1, map_line_indices_before_after_change))

            for line_mapping_of_line in line_mappings_of_line:
                if line_mapping_of_line.inserted:
                    changed_code_message += "inserted line:" + \
                        file_changes.change_tracked_lines[i].strip(
                            "\n") + "\n"
                elif line_mapping_of_line.modified:
                    changed_code_message += "modified line: before:'" + \
                        file_changes.change_tracked_lines.lines_before_change[line_mapping_of_line.before_line - 1].strip(
                            "\n") + "' after:'" + file_changes.change_tracked_lines[i].strip("\n") + "'\n"
                elif line_mapping_of_line.deleted:
                    changed_code_message += "deleted line:" + \
                        file_changes.change_tracked_lines.lines_before_change[
                            line_mapping_of_line.before_line - 1].strip("\n") + "\n"
                else:
                    changed_code_message += "unchanged line:" + \
                        file_changes.change_tracked_lines[i].strip(
                            "\n") + "\n"

    changed_code_message = reduce_long_sections_of_unchanged_lines_in_changed_code_message(
        changed_code_message)

    return changed_code_message + "\n"


def reduce_long_sections_of_unchanged_lines_in_changed_code_message(changed_code_message: str) -> str:
    '''
    If there are long sections of unchanged code lines in the changed code that is displayed to the agent, then the long sections are reduced.
    This prevents very long outputs that might distract the agent or might even be truncated and therefore might not show the relevant information.
    '''
    reduction_threshold = 120

    changed_code_message_split = changed_code_message.splitlines(keepends=True)

    # list of two element lists that denote the section of unchanged code in the message.
    # The second element of the lists is exclusive.
    sections_with_unchanged_code = []
    currently_in_section_of_unchanged_code = False

    for i, line in enumerate(changed_code_message_split):
        if line.startswith("unchanged line:"):
            if not currently_in_section_of_unchanged_code:
                currently_in_section_of_unchanged_code = True
                sections_with_unchanged_code.append([i, i + 1])
            else:
                sections_with_unchanged_code[-1][1] = i + 1
        else:
            currently_in_section_of_unchanged_code = False

    for section in reversed(sections_with_unchanged_code):
        section_length = section[1] - section[0]
        if section_length > reduction_threshold:
            for j in reversed(range(section[0] + 20, section[1] - 20)):
                changed_code_message_split.pop(j)
            changed_code_message_split.insert(
                section[0] + 20, f"... not showing {str(section_length - 40)} more unchanged lines ...\n")

    return "".join(changed_code_message_split)


def extract_build_error_information(build_error: BuildError, agent: BaseAgent) -> str:
    """
    Cleans the potentially long maven build output and only extracts the lines, where the relevant compilation errors are described.
    """

    build_output: str = build_error.stdout

    reactor_summary_start_index = build_output.rfind("""[INFO] -------------------------------------------------------------
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for""")
    first_index_after_error_messages = build_output[:reactor_summary_start_index].rfind(
        "[INFO]")

    build_output_without_postamble = build_output[:
                                                  first_index_after_error_messages]

    start_of_compilation_errors_section = build_output_without_postamble.find(
        "[ERROR] COMPILATION ERROR :")

    if start_of_compilation_errors_section != -1:

        compilation_errors_section = build_output_without_postamble[
            start_of_compilation_errors_section:]

        error_lines = compilation_errors_section.splitlines()

        # Truncate the error output if it has more than 10 error lines (first two lines are headers)
        if len(error_lines) > 12:
            error_lines = error_lines[:10]

        error_lines = add_problem_context_information(
            error_lines, agent)

        error_lines = clean_absolute_paths_in_output(error_lines, agent)

        return "Build failed with the following maven error output:   \n\n" + "  \n".join(error_lines) + "  \n\n" + "The project had no compilation errors before. So they occurred due to your fix attempt! Try a new fix that doesn't introduce the compilation errors.  \n"

    else:
        logger.error("Failed extracting error info from maven log",
                     "The list of extracted error messages was empty in extract_build_error_information(), but the build did fail." +
                     "Possibly the failure was due to another reason than compilation failure. Printing the full maven error log to the agent.")
        return "Build failed with the following maven error output:  \n\n" + build_output + "  \n"


def add_problem_context_information(output_lines: list[str], agent: BaseAgent) -> list[str]:
    """
    Try to add the referenced line of code in the error output, if it can be found.
    """

    augmented_output_lines = []

    # The line and column specifier of the error
    line_id_pattern = re.compile(r"(\[[0-9]+,[0-9]+\])")

    for output_line in output_lines:
        match_in_line = re.search(line_id_pattern, output_line)

        if match_in_line is not None:
            match_span = match_in_line.span(1)
            line_id_string = match_in_line.string[match_span[0]: match_span[1]]

            # Get the line number from the match
            error_line_number = line_id_string[1:line_id_string.find(
                ",")]

            repo_path = os.path.join(agent.config.workspace_path,
                                     agent.ai_config.warning_repository_name)

            path_start_index = output_line.find(repo_path)

            if path_start_index != -1:
                path_end_index = output_line.find(":", path_start_index)

                file_full_path = output_line[path_start_index: path_end_index]
                if os.path.isfile(file_full_path):
                    with open(file_full_path, 'r') as file:
                        lines = file.readlines()
                    line_content = lines[int(error_line_number) - 1]
                    line_content = line_content.strip()
                    output_line = output_line.replace(
                        # TODO: Maybe don't show the line number at all here, to prevent confusions if the line number changed due to insertions/deletions
                        line_id_string, f" In line {error_line_number}: '{line_content}' Problem:")

        augmented_output_lines.append(output_line)

    return augmented_output_lines


def clean_absolute_paths_in_output(output_lines: list[str], agent: BaseAgent) -> list[str]:
    """
    Clean any occurences of the absolute file path from the remaining output into relative paths starting from the repo.
    This prevents leaking the full absolute path which might confuse the agent into not using the relative path anymore.
    """

    repo_path = os.path.join(agent.config.workspace_path,
                             agent.ai_config.warning_repository_name)

    cleaned_path_lines = [output_line.replace(f"{repo_path}/", "").replace(
        f"{agent.config.workspace_path}/", "").replace(f"{agent.config.workspace_path}", "") for output_line in output_lines]

    return cleaned_path_lines


def check_if_suppression_literal_inserted(changes_dicts: list[dict], agent: BaseAgent) -> tuple[bool, str]:
    """
    Check if a suppression was actually added if we are in the task of suppressing the violation
    Else the agent could potentially pass the tests by only removing the target line (accidentally)
    Returns:
        (accepted, message): 
        accepted: (bool) Flag if the check was successful\n
        message: (str) Info message on the failure if it failed
    """

    logger.info("", "Checking if a suppression literal was inserted.")

    for changes_dict in changes_dicts:
        insertions: list[dict] = changes_dict.get("insertions", [])

        for insertion in insertions:
            inserted_lines: list[str] = insertion.get("new_lines", [])
            for inserted_line in inserted_lines:
                # Check if a NOSONAR was added. However, also @SuppressWarnings("java:S...") is a possible suppression syntax.
                # Checking directly against this is however also not save, as there might be very rare cases, where such an annotation is already present and distributed over multiple lines.
                # Then it might not always be necessary to delete and re-add the annotation.
                # Therefore we instead only check if the rule key was added. This (or NOSONAR) is always a necessary insertion, when trying to suppress the warning.
                if inserted_line.lower().find("nosonar") != -1 or inserted_line.lower().find(agent.ai_config.warning_rule_key.lower()) != -1:
                    logger.info("", "Suppression literal found.")
                    return True, ""

    return False, "However, you failed to insert a suppression. Neither a `// NOSONAR` nor a `@SuppressWarnings('java:S...')` was found in any of your insertions (where S... is the rule key)."


def check_sonar_qube_report(all_file_changes: list[FileChanges], agent: BaseAgent) -> tuple[bool, str]:
    """
    Checks if in the new SonarQube report with the made changes, the targeted violation is removed.
    Then checks that no new violations have been introduced.
    If both checks succeed returns true and a success message. Else false and a failure message.

    Returns:
        (accepted, message): 
        accepted: (bool) Flag if the check was successful\n
        message: (str) Info message on the success or failure
    """

    # Run SonarQube analysis for all changed files

    sonar_qube_reports_after_changes = {}
    for file_changes in all_file_changes:

        sanitized_checked_file_path = sanitize_and_shorten_file_path(
            file_changes.file_path)
        sonar_qube_report_after_changes = sonar_qube_analysis.analyze_file_and_parse_report(file_changes.file_path, agent.sonar_qube_rules_in_active_profile, agent.ai_config.warning_repository_name,
                                                                                            f"{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{agent.ai_config.warning_file_name}_line_{str(agent.ai_config.warning_start_line)}_analysis_report_attempt_no_{str(agent.write_fix_attempts)}_file_{sanitized_checked_file_path}.json", agent)
        sonar_qube_reports_after_changes[file_changes.file_path] = sonar_qube_report_after_changes

    # If there was no modification of the file with the target violation, add the report anyway (maybe there are cases where making changes to other files fixes a violation in the target file?)
    if agent.ai_config.warning_file_path not in sonar_qube_reports_after_changes:
        logger.warn(title="Write_fix made no changes to the target file.",
                    message="This might not be a problem, if changing another file fixes the warning in the target file (unlikely).")
        sanitized_warning_file_path = sanitize_and_shorten_file_path(
            agent.ai_config.warning_file_path)
        sonar_qube_report_after_changes_target_file = sonar_qube_analysis.analyze_file_and_parse_report(agent.ai_config.warning_file_path, agent.sonar_qube_rules_in_active_profile, agent.ai_config.warning_repository_name,
                                                                                                        f"{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{agent.ai_config.warning_file_name}_line_{str(agent.ai_config.warning_start_line)}_analysis_report_attempt_no_{str(agent.write_fix_attempts)}_file_{sanitized_warning_file_path}.json", agent)
        sonar_qube_reports_after_changes[agent.ai_config.warning_file_path] = sonar_qube_report_after_changes_target_file

        # Add a unchange FileChanges object to the list
        file_full_path = os.path.join(
            agent.config.workspace_path, agent.ai_config.warning_repository_name, agent.ai_config.warning_file_path)
        with open(file_full_path, 'r') as file:
            lines = file.readlines()

        file_changes_object_with_target_file_path = FileChanges(
            agent.ai_config.warning_file_path, lines)
        all_file_changes.append(file_changes_object_with_target_file_path)

    violation_removed = is_target_violation_removed(
        all_file_changes, sonar_qube_reports_after_changes[agent.ai_config.warning_file_path], agent)

    if violation_removed:
        new_violations_introduced, introduced_violations_info = any_new_violations_introduced(
            all_file_changes, sonar_qube_reports_after_changes, agent)

        if new_violations_introduced:
            return False, "Rerunning the SonarQube analysis found the following new rule violations that weren't present before, or that have moved:  \n" + introduced_violations_info \
                + "\nYou must not introduce any new rule violations. They need to be prevented/resolved, even if you think the rule violations were already present in the project before. If you think the listed rule violations were already present before and you are certain that they are false positives, then you can also suppress them with '// NOSONAR'."
        else:
            return True, "Rerunning the SonarQube analysis confirmed that your fix successfully removed the targeted rule violation and didn't introduce any new violations."
    else:
        return False, "Rerunning the SonarQube analysis found that the targeted rule violation has not yet been removed by your fix. It was still present in the SonarQube report."


def is_target_violation_removed(all_file_changes: list[FileChanges], sonar_qube_report_of_target_file: dict, agent: BaseAgent) -> bool:
    """
    Checks if the target violation is removed from the new report. 
    For that it retrieves the FileChanges object and translates the startline from before the change to after the change. 
    It then checks if this changed startline together with the rulekey are present in the report.
    """

    file_changes_objects_with_target_file_path = list(filter(
        lambda fileChanges: fileChanges.file_path == agent.ai_config.warning_file_path, all_file_changes))

    # Should always be exactly one file_changes object
    file_changes_of_target = file_changes_objects_with_target_file_path[0]
    map_line_indices_before_after_change = file_changes_of_target.change_tracked_lines.map_line_indices_before_after_change

    found_items_with_matching_before_line = list(filter(
        lambda line_mapping: line_mapping.before_line == agent.ai_config.warning_start_line, map_line_indices_before_after_change))

    if len(found_items_with_matching_before_line) != 1:
        logger.error("Aborting. Line of the target violation was not found in the before of the BeforeAfterMapping. This should never happen.",
                     f"Target violation line: {str(agent.ai_config.warning_start_line)}, Problematic FileChanges object: {repr(file_changes_of_target)}")
        shutdown(agent, 1)

    found_item_with_matching_before_line = found_items_with_matching_before_line[0]

    if found_item_with_matching_before_line.deleted:
        # Look for a matching insertion for the deletion, that is inserted at the same place (and therefore might hold the same rule violation that was in the deleted line before)
        found_insertions_paired_to_deletion = [
            pair[0] for pair in file_changes_of_target.change_tracked_lines.paired_insertions_and_deletions if pair[1] is found_item_with_matching_before_line]

        if len(found_insertions_paired_to_deletion) != 0:
            found_insertion = found_insertions_paired_to_deletion[0]

            target_violation_expected_changed_start_line = found_insertion.after_line

        else:
            logger.warn(title="The line with the target violation was removed by using write_fix and no corresponding line readded.",
                        message="We accept this as removing the target violation, but it is likely that there are semantic changes due to this, which should be catched by the LLM Reviewer Prompt.")
            return True
    else:
        target_violation_expected_changed_start_line = found_item_with_matching_before_line.after_line

    # Check if the Warning is in the new report
    return not sonar_qube_analysis.rule_violation_present_in_analysis_report(sonar_qube_report_of_target_file, agent.ai_config.warning_rule_key, target_violation_expected_changed_start_line)


def any_new_violations_introduced(all_file_changes: list[FileChanges], sonar_qube_reports_after_changes: dict[str, dict], agent: BaseAgent) -> tuple[bool, str]:
    """
    Finds any rule violations that are newly introduced and creates a formated output string, that lists all the unmatche violations for the agent.
    """

    newly_introduced_violations = find_newly_introduced_violations(
        all_file_changes, sonar_qube_reports_after_changes, agent)

    if len(newly_introduced_violations) == 0:
        return False, ""
    else:
        # Format all the found violations for the output string
        if len(newly_introduced_violations) > 10:
            logger.warn(title="Lots of newly introduce rule violations",
                        message=f"There were a total of {len(newly_introduced_violations)} new rule violations introduced by the write_fix. This are unexpectedly many")
            newly_introduced_violations = newly_introduced_violations[: 9]

        newly_introduced_violations_string = ""

        last_items_file_path = ""
        for newly_introduced_violation in newly_introduced_violations:
            if last_items_file_path != newly_introduced_violation[0]:
                last_items_file_path = newly_introduced_violation[0]
                newly_introduced_violations_string += f"In file {last_items_file_path}:  \n"

            start_line_newly_introduced_violation = newly_introduced_violation[3]["startLine"]
            file_full_path = os.path.join(
                agent.config.workspace_path, agent.ai_config.warning_repository_name, last_items_file_path)
            with open(file_full_path, 'r') as file:
                lines = file.readlines()
            line_content = lines[int(
                start_line_newly_introduced_violation) - 1]
            line_content = line_content.strip()

            specific_message = newly_introduced_violation[3]["specificMessage"]
            newly_introduced_violations_string += f"Rule {str(newly_introduced_violation[1])}: '{str(newly_introduced_violation[2])}' (Context-specific message: '{str(specific_message)}') at line {str(start_line_newly_introduced_violation)}: '{line_content}'  \n"
        return True, newly_introduced_violations_string


def find_newly_introduced_violations(all_file_changes: list[FileChanges], sonar_qube_reports_after_changes: dict[str, dict], agent: BaseAgent) -> list[tuple[str, str, str, dict]]:
    """
    Goes through all the rule violations in the sonar_qube_reports after the change and tries to match them with a rule violation in the report before the change.
    If none is found the info of the rule violation is added to the list of newly introduced violations.
    """
    newly_introduced_violations = []

    for file_changes in all_file_changes:
        sonar_qube_report_after_change_for_file = sonar_qube_reports_after_changes[
            file_changes.file_path]
        sonar_qube_report_before_change_for_file = agent.initial_analysis_reports[
            file_changes.file_path]

        for mined_rule_after in sonar_qube_report_after_change_for_file["minedRules"]:
            rule_key = mined_rule_after["ruleKey"]
            rule_name = mined_rule_after["ruleName"]

            matched_mined_rules_before = list(filter(
                lambda mined_rule_before, rule_key=rule_key: mined_rule_before["ruleKey"] == rule_key, sonar_qube_report_before_change_for_file["minedRules"]))

            # No rules with rule_key are present in the before report
            if len(matched_mined_rules_before) == 0:
                for warning_location_after in mined_rule_after["warningLocations"]:
                    newly_introduced_violations.append(
                        (file_changes.file_path, rule_key, rule_name, warning_location_after))
            else:
                mined_rule_before = matched_mined_rules_before[0]
                for warning_location_after in mined_rule_after["warningLocations"]:
                    start_line_warning_after = warning_location_after["startLine"]

                    found_line_mappings_start_line_after_before: list[BeforeAfterMapping] = list(filter(
                        lambda line_mapping, start_line_warning_after=start_line_warning_after: not line_mapping.deleted and line_mapping.after_line == start_line_warning_after, file_changes.change_tracked_lines.map_line_indices_before_after_change))

                    if len(found_line_mappings_start_line_after_before) != 1:
                        logger.error(f"Aborting. Line {str(start_line_warning_after)} was not found in the after of the BeforeAfterMapping, but there was a warning at that line. This should never happen.",
                                     f"Violation line after: {str(start_line_warning_after)}, Problematic FileChanges object: {repr(file_changes)}")
                        shutdown(agent, 1)

                    found_line_mapping_start_line_after_before = found_line_mappings_start_line_after_before[
                        0]

                    if found_line_mapping_start_line_after_before.inserted:
                        # The start_line_warning_after currently looked at belongs to an insertion,
                        # so look for a corresponding deletion of the same line and use this deletion's before_line for matching the warning.
                        found_deletions_paired_to_insertions = [
                            pair[1] for pair in file_changes.change_tracked_lines.paired_insertions_and_deletions if pair[0] is found_line_mapping_start_line_after_before]

                        if len(found_deletions_paired_to_insertions) != 0:
                            found_deletion = found_deletions_paired_to_insertions[0]

                            start_line_warning_before = found_deletion.before_line

                        else:
                            # Warning was in a newly added line, which had no corresponding deletion of the same line
                            newly_introduced_violations.append(
                                (file_changes.file_path, rule_key, rule_name, warning_location_after))
                            continue
                    else:
                        start_line_warning_before = found_line_mapping_start_line_after_before.before_line

                    matched_warning_locations_before = list(filter(
                        lambda warning_location_before, start_line_warning_before=start_line_warning_before: warning_location_before["startLine"] == start_line_warning_before, mined_rule_before["warningLocations"]))

                    # The rule violation could not be resolved to a violation in the initial report
                    if len(matched_warning_locations_before) == 0:
                        newly_introduced_violations.append(
                            (file_changes.file_path, rule_key, rule_name, warning_location_after))

    return newly_introduced_violations


def try_to_run_tests(agent: BaseAgent) -> tuple[bool, str]:
    """
    Runs the tests via mvn clean test.
    If there were failing test cases then the failing test reports are extracted and formatted appropriately for the agent.
    This expects that the project is successfully buildable (ensured by running try_to_build_changed_project first in the ChangeApprover).
    """

    try:
        test_result = repository_operations.run_tests(agent)

    except subprocess.TimeoutExpired as te:
        logger.warn("Ignoring the test in the ChangeApprover.")
        # Tests are just counted as accepted and no info is printed to the agent about the tests.
        return True, ""

    if test_result.returncode == 0:
        return True, "All tests in the project have been run and passed successfully."
    else:
        return False, "However, running the tests in the project failed with the following failing tests:  \n" + extract_test_failure_information(test_result, agent)


def extract_test_failure_information(test_result: subprocess.CompletedProcess[str], agent: BaseAgent) -> str:
    """
    Finds all the test reports in "target/surefire-reports" folders anywhere in the project.
    Extracts the ones that have either failures or errors and formats the stacktraces so that package identifiers 
    are replaced by paths that the agent can use and references outside the project are removed.
    """

    test_report_folders = find_all_folders(
        agent.config.workspace_path, agent.ai_config.warning_repository_name, "target/surefire-reports")

    if len(test_report_folders) == 0:
        logger.error("Extracting test reports failed",
                     "There should have been test reports created. However no 'target/surefire-reports' folders could be found. Fallback to uncleaned printing of the output.")
        return naive_test_extraction(test_result, agent)

    test_output = ""

    # Go through all the test report files
    for test_report_folder in test_report_folders:
        for root, _, files in os.walk(test_report_folder):
            for file_name in files:
                full_file_path = os.path.join(root, file_name)
                if full_file_path.endswith(".txt"):
                    with open(full_file_path, "r") as report_file:
                        report_content = report_file.read()

                    summary_pattern = re.compile(
                        r"Tests run: \d+, Failures: (\d+), Errors: (\d+), Skipped: \d+, Time elapsed:")
                    test_summary = re.search(summary_pattern, report_content)
                    if test_summary is not None:
                        number_of_failures = int(test_summary.group(1))
                        number_of_errors = int(test_summary.group(2))
                        # Include the report if there was a failure or error
                        if number_of_failures > 0 or number_of_errors > 0:
                            failing_test_file_path = ""
                            package_path_failing_test: str = file_name.removesuffix(
                                ".txt")
                            package_path_failing_test_converted_to_slash = package_path_failing_test.replace(
                                ".", "/")
                            try:
                                failing_test_file_path = preprocess_paths(
                                    agent.config.workspace_path, agent.ai_config.warning_repository_name, package_path_failing_test_converted_to_slash + ".java")
                            except ValueError as ve:
                                logger.warn(title="Couldn't resolve package path when formatting test failure output",
                                            message=f"package_path was {package_path_failing_test_converted_to_slash}. Error was {str(ve)}")

                            if failing_test_file_path != "":
                                report_content = report_content.replace(
                                    f"Test set: {package_path_failing_test}", f"Test file with failure: {failing_test_file_path}")

                            report_lines = report_content.splitlines(
                                keepends=True)
                            report_cleaned = []
                            for line in report_lines:
                                report_cleaned.append(
                                    clean_stacktrace_line(line, package_path_failing_test, agent))

                            test_output += "".join(report_cleaned) + "  \n"

    return test_output


def clean_stacktrace_line(line: str, package_path_failing_test: str, agent: BaseAgent) -> str:
    """
    If the line is not part of the stacktrace it is returned as is, 
    or if it is the info line about the failing method (before the stacktrace), then this is cleaned appropriately.
    If it is matched as a stacktrace line then it is cleaned like the following example:
    "at net.sourceforge.argparse4j.internal.ArgumentParserImpl.parseArgs(ArgumentParserImpl.java:829)"
    becomes
    "at main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: parseArgs (line 829)"
    If the stacktrace line references a file outside the project it is dropped from the stacktrace.
    """
    if line.startswith("\tat "):
        stack_trace_item_pattern = re.compile(
            r"at (?P<package_path>([^\.]+\.)+)(?P<method>[^\.]+)(?P<info_in_braces>\(.+:(?P<line_number>\d+)\))")
        matched_stack_trace_line = re.search(
            stack_trace_item_pattern, line)
        cleaned_line = ""
        if matched_stack_trace_line is not None:
            package_path_of_line_unclean = matched_stack_trace_line.group(
                "package_path")
            # the package of the file might end with a $2 or similar, so remove that
            package_path_of_line_converted_to_slash = package_path_of_line_unclean.removesuffix(".").split("$")[
                0].replace(".", "/")
            try:
                file_path_of_line = preprocess_paths(
                    agent.config.workspace_path, agent.ai_config.warning_repository_name, package_path_of_line_converted_to_slash + ".java")

            except ValueError as ve:
                logger.debug("Package path in Stacktrace not resolved.",
                             f"Likely it was a path outside the project so its excluded. Problem was: {str(ve)}")
                # don't add the line, as the file is not project-local
                return ""

            cleaned_line = line.replace(
                package_path_of_line_unclean, file_path_of_line + " Method: ")

            method_line_number = matched_stack_trace_line.group(
                "line_number")
            info_in_braces = matched_stack_trace_line.group(
                "info_in_braces")
            cleaned_line = cleaned_line.replace(
                info_in_braces, f" (line {str(method_line_number)})")

            return cleaned_line

        else:
            # Failed to match the stacktrace line, ignore it
            return ""
    else:
        # Not a stacktrace line

        # Check if its the failing method's introduction
        failing_method_intro_pattern = re.compile(
            rf"\({package_path_failing_test}\)  Time elapsed: ")
        matched_intro_pattern = re.search(failing_method_intro_pattern, line)
        if matched_intro_pattern is not None:
            cleaned_line = "Failing test method: " + \
                line.replace(
                    f"({package_path_failing_test})  Time elapsed: ", "  Time elapsed: ")
            return cleaned_line

        else:
            return line


def naive_test_extraction(test_result: subprocess.CompletedProcess[str], agent: BaseAgent):
    index_start_test_section = test_result.stdout.find("""-------------------------------------------------------
 T E S T S
-------------------------------------------------------""")
    index_end_of_test_section = test_result.stdout.rfind("""[INFO] -------------------------------------------------------------
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for""")
    test_section = test_result.stdout[index_start_test_section:index_end_of_test_section]
    repo_path = os.path.join(agent.config.workspace_path,
                             agent.ai_config.warning_repository_name)
    return test_section.replace(f"{repo_path}/", "").replace(
        f"{agent.config.workspace_path}/", "").replace(f"{agent.config.workspace_path}", "")


def approve(messages: list[str], changes_dicts: list[dict], agent: BaseAgent) -> str:
    """
    Adds a "APPROVED" to the start of the feedback and saves the changes_dicts to the plausible_patches file.
    """
    with open(os.path.join("experimental_setups", agent.exps[-1], agent.current_state, "plausible_patches",
                           f"{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{agent.ai_config.warning_file_name}_line_{str(agent.ai_config.warning_start_line)}_plausible_patches.json"), "a+") as exps:
        exps.write(
            f"  \n### PLAUSIBLE FIX (fix no. {str(agent.write_fix_attempts)})\n{json.dumps(changes_dicts, indent=4)}\n\n ###CHANGE APPROVER FEEDBACK:  \n" + "  \n".join(messages))

    agent.plausible_fixes += 1

    return "APPROVED  \n" + "  \n".join(messages) + "  \nThe repository has been restored to its original state. \nIf you think that your write_fix solved the problem then use the command goals_accomplished to conclude the task."


def reject(messages: list[str], changes_dicts: list[dict], agent: BaseAgent) -> str:
    """
    Adds a "REJECTED" to the start of the feedback and saves the changes_dicts to the implausible_patches file.
    """
    with open(os.path.join("experimental_setups", agent.exps[-1], agent.current_state, "implausible_patches",
                           f"{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{agent.ai_config.warning_file_name}_line_{str(agent.ai_config.warning_start_line)}_implausible_patches.json"), "a+") as exps:
        exps.write(
            f"  \n### IMPLAUSIBLE FIX (fix no. {str(agent.write_fix_attempts)})\n{json.dumps(changes_dicts, indent=4)}\n\n ###CHANGE APPROVER FEEDBACK:  \n" + "  \n".join(messages))

    return "REJECTED  \nIMPORTANT: The repository has been restored to its original state! You need to start applying changes from scratch again.\n" + "  \n".join(messages)

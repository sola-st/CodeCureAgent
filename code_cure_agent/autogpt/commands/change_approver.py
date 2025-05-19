import os
from autogpt.agents.base import BaseAgent
from autogpt.commands import repository_operations
from autogpt.commands.repository_operations import BuildError
import subprocess
from autogpt.logs import logger
from autogpt.utils.write_fix_utils.change_tracking import FileChanges

import re


def approve_changes(changes_dicts: list[dict], all_file_changes: list[FileChanges], agent: BaseAgent) -> str:
    """
    Implements the ChangeApprover functionality. 
    First tries to rebuild the project. 
    If successful SonarQube is rerun and we check if the targeted violation was removed and no new violations introduced. 
    Finally if also successful a query to a Reviewer LLM is made that either approves or reject. 
    The ChangeApprover gives appropriate feedback to the agent for each of the validation steps.
    """

    rejected = False

    rejected, build_message = try_to_build_changed_project(agent)

    if rejected:
        return reject([build_message], changes_dicts, agent)

    rejected, sonar_qube_message = check_sonar_qube_report(
        all_file_changes, agent)

    if rejected:
        return reject([build_message, sonar_qube_message], changes_dicts, agent)

    rejected, reviewer_llm_message = ask_reviewer_llm(
        changes_dicts, all_file_changes, agent)

    if rejected:
        return reject([build_message, sonar_qube_message, reviewer_llm_message], changes_dicts, agent)
    else:
        return approve([build_message, sonar_qube_message, reviewer_llm_message], changes_dicts, agent)


def try_to_build_changed_project(agent: BaseAgent) -> tuple[bool, str]:
    """
    Tries to build the project with the applied changes.
    If the maven build wasn't successful we return a cleaned build output.
    """

    try:
        repository_operations.build_project(agent)

        return False, "Project was successfully built with the applied changes."

    except BuildError as build_error:
        return True, extract_build_error_information(build_error, agent)

    except subprocess.TimeoutExpired as timeout_error:
        return True, f"Building the project failed with a timeout after {timeout_error.timeout / 60} minutes."


def extract_build_error_information(build_error: BuildError, agent: BaseAgent):
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

        error_lines = add_problem_context_information(
            error_lines, agent)

        error_lines = clean_absolute_paths_in_output(error_lines, agent)

        return "Build failed with the following maven error output:   \n\n" + "  \n".join(error_lines) + "  \n"

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


def check_sonar_qube_report(all_file_changes: list[FileChanges], agent: BaseAgent) -> tuple[bool, str]:
    return False, ""


def ask_reviewer_llm(changes_dicts: list[dict], all_files_with_changes: list[dict], agent: BaseAgent) -> tuple[bool, str]:
    return False, ""


def approve(messages: list[str], changes_dicts: list[dict], agent: BaseAgent) -> str:
    sanitized_warning_file_path = agent.ai_config.warning_file_path.replace(
        "/", ".")
    with open(os.path.join("experimental_setups", agent.exps[-1], "plausible_patches",
                           f"plausible_patches_{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{sanitized_warning_file_path}_line_{str(agent.ai_config.warning_start_line)}.json"), "a+") as exps:
        exps.write("  \n### PLAUSIBLE FIX\n{}\n\n ###CHANGE APPROVER FEEDBACK: {}".format(
            str(changes_dicts), "  \n".join(messages)))

    return "APPROVED  \n" + "  \n".join(messages)


def reject(messages: list[str], changes_dicts: list[dict], agent: BaseAgent) -> str:
    sanitized_warning_file_path = agent.ai_config.warning_file_path.replace(
        "/", ".")
    with open(os.path.join("experimental_setups", agent.exps[-1], "implausible_patches",
                           f"implausible_patches_{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{sanitized_warning_file_path}_line_{str(agent.ai_config.warning_start_line)}.json"), "a+") as exps:
        exps.write("  \n### PLAUSIBLE FIX\n{}\n\n ###CHANGE APPROVER FEEDBACK: {}".format(
            str(changes_dicts), "  \n".join(messages)))

    return "REJECTED  \n" + "  \n".join(messages)

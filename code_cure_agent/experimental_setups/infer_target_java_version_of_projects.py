import sys
from pathlib import Path
sys.path.append(str(Path(__file__).parent.parent))
import subprocess
from agent_core.utils.agent_utils.agent_mock import AgentMock
from agent_core.commands import repository_operations
from agent_core.utils.path_utils.path_utils import find_all_folders
from git.exc import GitError
import os
import csv
import click


@click.command("Based on a created repos list with the repo commits to work with, creates a new csv file with the target java version used when building the projects. "
               "This tool expects the projects to be buildable with maven, so check this first. Run from the code_cure_agent folder.")
@click.argument(
    "repos_list_with_commits_file",
    type=click.File()
)
@click.option(
    "--target-file-path",
    "-t",
    default="./experimental_setups/repos_list_with_commits_and_java_target_versions.csv",
    help="Path where the txt file with java versions should be written to."
)
def infer_target_java_version_of_projects(repos_list_with_commits_file: click.File, target_file_path):
    cca_workspace = os.path.join(
        str(Path(__file__).parent.parent), "cca_workspace")
    repository_operations.remove_folder_if_exists(cca_workspace)
    os.mkdir(cca_workspace)

    csv_reader = csv.reader(repos_list_with_commits_file)

    with open(target_file_path, "w") as out_file:
        csv_writer = csv.writer(
            out_file, dialect=csv.unix_dialect, quoting=csv.QUOTE_MINIMAL)

        # csv has columns repositoryURL, commit and target Java version

        for row in csv_reader:
            repository_url = row[0]
            commit = row[1]

            agent = AgentMock(repository_url, commit, "irrelevant", repository_url.split(
                "/")[-1].removesuffix(".git"), "irrelevant", "irrelevant", "irrelevant", "irrelevant", workspace_path=cca_workspace)

            error = None

            build_successful = False

            # First build the project
            try:
                repository_operations.checkout_project(agent)

                repository_operations.build_project(
                    agent, time_monitoring=False)

                build_successful = True

            except GitError as ge:
                error = str(ge)
            except repository_operations.BuildError as be:
                error = be.stdout
            except subprocess.TimeoutExpired as te:
                error = f"TimeoutExpired exception: Build timed out after {te.timeout / 60} minutes. \nThe stdout was the following: \n\n{te.stdout.decode('utf-8')}"
            except Exception as e:
                error = str(e)

            if error is not None:
                with open("infer_target_java_version_error.log", "a+") as log:
                    log.write(str(error))
                    log.write("\n")

                csv_writer.writerow(
                    [repository_url, commit, ""])

            # If successfully built, then find the lowest common java version in the .class files
            if build_successful:
                infer_java_error = None
                lowest_found_target_major_java_version = sys.maxsize

                build_target_folders = find_all_folders(
                    agent.config.workspace_path, agent.ai_config.warning_repository_name, "target")

                if len(build_target_folders) == 0:
                    infer_java_error = f"No target folders found for {agent.ai_config.warning_repository_name}. Setting java version to ''"

                at_least_one_class_file_with_major_version_found = False

                max_checked_files = 20

                checked_files = 0

                for build_target_folder in build_target_folders:
                    for root, _, files in os.walk(build_target_folder):
                        for file_name in files:
                            if checked_files >= max_checked_files:
                                break
                            full_file_path = os.path.join(root, file_name)
                            if full_file_path.endswith(".class"):
                                checked_files += 1
                                result = subprocess.run(
                                    f"javap -verbose {full_file_path} | grep 'major version' | awk '{{print $3}}'",
                                    capture_output=True,
                                    encoding="utf8",
                                    shell=True)
                                if result.returncode == 0:
                                    try:
                                        major_java_version = int(result.stdout)
                                        at_least_one_class_file_with_major_version_found = True
                                    except ValueError:
                                        continue
                                    if lowest_found_target_major_java_version > major_java_version:
                                        lowest_found_target_major_java_version = major_java_version

                if infer_java_error is None and not at_least_one_class_file_with_major_version_found:
                    infer_java_error = f"No .class files were found in the target folders, or no major version could be found in them after checking at most 20, for {agent.ai_config.warning_repository_name}. Setting java version to ''"

                if infer_java_error is not None:
                    with open("infer_target_java_version_error.log", "a+") as log:
                        log.write(str(infer_java_error))
                        log.write("\n")

                    csv_writer.writerow(
                        [repository_url, commit, ""])
                else:
                    # Transform the major version to the java version format
                    java_version_known_format = lowest_found_target_major_java_version - 44

                    if java_version_known_format <= 4:
                        java_version_known_format_str = "1." + \
                            str(java_version_known_format)
                    else:
                        java_version_known_format_str = str(
                            java_version_known_format)

                    csv_writer.writerow(
                        [repository_url, commit, java_version_known_format_str])


if __name__ == "__main__":
    infer_target_java_version_of_projects()

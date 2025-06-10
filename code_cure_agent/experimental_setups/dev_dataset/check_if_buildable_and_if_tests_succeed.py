import subprocess
from agent_core.commands import repository_operations
from git.exc import GitError
import os
import csv
import sys
from os import path
sys.path.append(path.dirname(path.dirname(
    path.dirname(path.abspath(__file__)))))


# This script goes through all of the reposiotries in the sorald list of repos and tries to build them


SORALD_JAR_PATH = "/workspaces/master-thesis-pascal-joos/code_cure_agent/sorald/sorald.jar"


class Config():
    def __init__(self, workspace_path):
        self.workspace_path = workspace_path
        self.sorald_jar_path = SORALD_JAR_PATH


class AIConfig():
    def __init__(self, warning_repository_URL, warning_repository_commit, warning_file_path, warning_repository_name, warning_rule_key, warning_start_line, warning_rule_name, warning_specific_message):
        self.warning_repository_URL = warning_repository_URL
        self.warning_repository_commit = warning_repository_commit
        self.warning_file_path = warning_file_path
        self.warning_rule_key = warning_rule_key
        self.warning_start_line = warning_start_line
        self.warning_rule_name = warning_rule_name
        self.warning_specific_message = warning_specific_message

        self.warning_repository_name = warning_repository_name


class Agent():
    def __init__(self, warning_repository_URL, warning_repository_commit, warning_file_path, warning_repository_name, warning_rule_key, warning_start_line, warning_rule_name, warning_specific_message):
        self.config = Config("./../../cca_workspace")
        self.ai_config = AIConfig(warning_repository_URL, warning_repository_commit, warning_file_path,
                                  warning_repository_name, warning_rule_key, warning_start_line, warning_rule_name, warning_specific_message)
        self.exps = ["experiment_test"]


if __name__ == "__main__":

    cca_workspace = "./../../cca_workspace"
    repository_operations.remove_folder_if_exists(cca_workspace)
    os.mkdir(cca_workspace)

    with open("original_sorald_considered_repos_stats.csv", "r") as repos_file:
        csv_reader = csv.reader(repos_file)

        with open("check_if_buildable_results.csv", "w") as out_file:
            csv_writer = csv.writer(out_file, dialect=csv.unix_dialect)

            csv_writer.writerow(
                ["repositoryURL", "commit", "Build successful", "Tests successful"])

            for i, row in enumerate(csv_reader):
                if i > 0:
                    repository_url = row[0]
                    commit = row[1]

                    agent = Agent(repository_url, commit, "irrelevant", repository_url.split(
                        "/")[-1].removesuffix(".git"), "irrelevant", "irrelevant", "irrelevant", "irrelevant")

                    error = None

                    build_successful = False

                    tests_successful = False

                    try:
                        repository_operations.checkout_project(agent)

                        repository_operations.build_project(agent)

                        build_successful = True

                    except GitError as ge:
                        error = str(ge)
                    except repository_operations.BuildError as be:
                        error = be.stdout
                    except subprocess.TimeoutExpired as te:
                        error = f"TimeoutExpired exception: Build timed out after {te.timeout / 60} minutes. \nThe stdout was the following: \n\n{te.stdout.decode('utf-8')}"
                    except Exception as e:
                        error = str(e)

                    # Run the tests
                    if error is None:
                        try:
                            timeout_five_minutes = 5*60
                            repo_path = os.path.join(agent.config.workspace_path,
                                                     agent.ai_config.warning_repository_name)

                            print("Running tests for target project " +
                                  agent.ai_config.warning_repository_name)

                            result = subprocess.run(
                                ["mvn", "clean", "test",
                                    "--no-transfer-progress", "--batch-mode"],
                                capture_output=True,
                                encoding="utf8",
                                cwd=repo_path,
                                shell=False,
                                timeout=timeout_five_minutes
                            )

                            if result.returncode == 0:
                                tests_successful = True

                                print("Running tests was successful.")

                            else:
                                print("Running tests failed with: " +
                                      result.stdout)
                                raise repository_operations.BuildError(
                                    result.returncode, result.stdout)

                        except subprocess.TimeoutExpired as te:
                            error = f"TimeoutExpired exception: Tests timed out after {te.timeout / 60} minutes. \nThe stdout was the following: \n\n{te.stdout.decode('utf-8')}"
                        except repository_operations.BuildError as be:
                            error = be.stdout
                        except Exception as e:
                            error = str(e)

                        with open(os.path.join(cca_workspace, f"{agent.ai_config.warning_repository_name}_test_info.log"), "w") as log:
                            log.write(result.stdout)

                    if error is not None:
                        with open(os.path.join(cca_workspace, f"{agent.ai_config.warning_repository_name}_error.log"), "w") as log:
                            log.write(str(error))

                    csv_writer.writerow(
                        [repository_url, commit, build_successful, tests_successful])

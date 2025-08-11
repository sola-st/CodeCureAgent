import sys
from pathlib import Path
sys.path.append(str(Path(__file__).parent.parent.parent))
import subprocess
from agent_core.utils.agent_utils.agent_mock import AgentMock
from agent_core.commands import repository_operations
from git.exc import GitError
import os
import csv


# This script goes through all of the reposiotries in the sorald list of repos and tries to build them


if __name__ == "__main__":

    cca_workspace = os.path.join(
        str(Path(__file__).parent.parent.parent), "cca_workspace")
    repository_operations.remove_folder_if_exists(cca_workspace)
    os.mkdir(cca_workspace)

    with open("experimental_setups/dev_dataset/original_sorald_considered_repos_stats.csv", "r") as repos_file:
        csv_reader = csv.reader(repos_file)

        with open("experimental_setups/dev_dataset/check_if_buildable_results.csv", "w") as out_file:
            csv_writer = csv.writer(out_file, dialect=csv.unix_dialect)

            csv_writer.writerow(
                ["repositoryURL", "commit", "Build successful", "Tests successful"])

            for i, row in enumerate(csv_reader):
                if i > 0:
                    repository_url = row[0]
                    commit = row[1]

                    agent = AgentMock(repository_url, commit, "irrelevant", repository_url.split(
                        "/")[-1].removesuffix(".git"), "irrelevant", "irrelevant", "irrelevant", "irrelevant", workspace_path=cca_workspace)

                    error = None

                    build_successful = False

                    tests_successful = False

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

                    # Run the tests
                    if error is None:
                        try:
                            timeout_five_minutes = 5 * 60
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

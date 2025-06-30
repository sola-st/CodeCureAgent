
import git
from git.exc import GitError
from agent_core.agents.base import BaseAgent
import os
import shutil
import time
from agent_core.logs.logger import logger
import subprocess

from agent_core.utils.path_utils.path_utils import sanitize_and_shorten_file_path


def checkout_project(agent: BaseAgent, overwrite_target_workspace_path: str = None, overwrite_target_folder_name: str = None) -> None:
    """
    Clone targeted repository and checkout targeted commit.
    If the target project folder already exists it is first removed.
    Throws a GitError if cloning or checking out fails.
    """
    if overwrite_target_workspace_path is None:
        target_workspace_path = agent.config.workspace_path
    else:
        target_workspace_path = overwrite_target_workspace_path

    if overwrite_target_folder_name is None:
        target_folder_name = agent.ai_config.warning_repository_name
    else:
        target_folder_name = overwrite_target_folder_name

    repo_path = os.path.join(target_workspace_path,
                             target_folder_name)

    logger.info("", "Project checkout procedure starting.")

    remove_folder_if_exists(repo_path)

    logger.info(
        "", f"Cloning from URL '{agent.ai_config.warning_repository_URL}' to path '{repo_path}'.")
    try:
        repo = git.Repo.clone_from(
            agent.ai_config.warning_repository_URL, to_path=repo_path)

    except GitError as e:
        logger.error("Git Cloning failed", f"Error: {e}")
        raise

    if agent.ai_config.warning_repository_commit.lower() == "master":
        logger.debug(
            "", f"Skipped checking out a commit, because ai_config.warning_repository_commit is '{agent.ai_config.warning_repository_commit}'.")
    else:
        logger.info(
            "", f"Checking out commit '{agent.ai_config.warning_repository_commit}'.")
        try:
            repo.git.checkout(
                agent.ai_config.warning_repository_commit, "--force")

        except GitError as e:
            logger.error("Git Checkout failed", f"Error: {e}")
            raise


def remove_folder_if_exists(folder: str) -> None:
    if os.path.exists(folder):
        logger.debug(
            "", f"Folder '{folder}' already exists. Deleting the folder.")
        try:
            shutil.rmtree(folder)

            # shutil.rmtree can be very flakey (maybe due to using dev container).
            # Parts of the folder sometimes reappear after a short time, so os.path.exists is briefly false and then true again.
            # Therefore sleep 2 seconds and then retry removing, if the folder reappeared
            time.sleep(2)
            # Wait for a maximum of 60 seconds for folder being deleted
            timeout = 60
            timer_start = time.time()
            while os.path.exists(folder):
                # Retrigger deletion
                shutil.rmtree(folder)
                if time.time() - timer_start > timeout:
                    logger.error(
                        "Failed removing folder", f"Removing ran into a timeout after {str(timeout)} seconds.")
                    raise TimeoutError(
                        "Failed removing folder. Removing ran into a timeout.")
                time.sleep(1)

        except TimeoutError as te:
            raise te
        except OSError as e:
            logger.error("Failed removing folder", "Error: %s - %s." %
                         (e.filename, e.strerror))


def build_project(agent: BaseAgent) -> None:
    """
    Try to build the target project. 
    Expects that the project is already checked out.
    Expects the project to be a maven project compatible with JDK 11 and with the pom.xml in the root folder of the project.
    If there was an exception in the subcommand a BuildError is thrown.
    """
    repo_path = os.path.join(agent.config.workspace_path,
                             agent.ai_config.warning_repository_name)

    logger.info("",
                f"Building the target project {agent.ai_config.warning_repository_name} with maven."
                )

    timeout_ten_minutes = 10 * 60

    # Write the build start to the execution info file
    with open(os.path.join("experimental_setups", agent.exps[-1], agent.current_state, "execution_info", f"{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{agent.ai_config.warning_file_name}_line_{str(agent.ai_config.warning_start_line)}_execution_info"), "a+") as patf:
        patf.write(
            f"!! Maven build startup timestamp: " + str(time.time_ns()) + "\n")

    try:
        result = subprocess.run(
            ["mvn", "clean", "package", "-DskipTests",
                "--no-transfer-progress", "--batch-mode"],
            capture_output=True,
            encoding="utf8",
            cwd=repo_path,
            shell=False,
            timeout=timeout_ten_minutes
        )

        # Write the build end to the execution info file
        with open(os.path.join("experimental_setups", agent.exps[-1], agent.current_state, "execution_info", f"{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{agent.ai_config.warning_file_name}_line_{str(agent.ai_config.warning_start_line)}_execution_info"), "a+") as patf:
            patf.write(
                f"!! Maven build end timestamp: " + str(time.time_ns()) + "\n")

    except subprocess.TimeoutExpired as te:
        logger.error("TimeoutExpired",
                     f"Build ran into timeout after {te.timeout / 60} minutes.")
        # Write the build end to the execution info file
        with open(os.path.join("experimental_setups", agent.exps[-1], agent.current_state, "execution_info", f"{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{agent.ai_config.warning_file_name}_line_{str(agent.ai_config.warning_start_line)}_execution_info"), "a+") as patf:
            patf.write(
                f"!! Maven build end timestamp: " + str(time.time_ns()) + "\n")

        raise

    if result.returncode == 0:
        logger.info("", "Build was successful.")
    else:
        logger.info("", "Build failed.")
        logger.debug(
            "Error", f"Build failed with returncode {result.returncode}, stdout: \n{result.stdout}\n stderr: {result.stderr}")
        raise BuildError(result.returncode, result.stdout)


class BuildError(Exception):
    def __init__(self, returncode, stdout):
        self.returncode = returncode
        self.stdout = stdout
        super().__init__(stdout)


def run_tests(agent: BaseAgent) -> subprocess.CompletedProcess[str]:
    """
    Run the test-suite of the project.
    Expects that the project is already checked out and can be built.
    Expects the project to be a maven project compatible with JDK 11 and with the pom.xml in the root folder of the project.
    """
    repo_path = os.path.join(agent.config.workspace_path,
                             agent.ai_config.warning_repository_name)

    logger.info("",
                f"Running tests on the target project {agent.ai_config.warning_repository_name} with maven."
                )

    timeout_five_minutes = 5 * 60

    # Write the test start to the execution info file
    with open(os.path.join("experimental_setups", agent.exps[-1], agent.current_state, "execution_info", f"{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{agent.ai_config.warning_file_name}_line_{str(agent.ai_config.warning_start_line)}_execution_info"), "a+") as patf:
        patf.write(
            f"!! Maven test startup timestamp: " + str(time.time_ns()) + "\n")

    try:
        result = subprocess.run(
            ["mvn", "clean", "test",
                "--no-transfer-progress", "--batch-mode"],
            capture_output=True,
            encoding="utf8",
            cwd=repo_path,
            shell=False,
            timeout=timeout_five_minutes
        )

        # Write the test end to the execution info file
        with open(os.path.join("experimental_setups", agent.exps[-1], agent.current_state, "execution_info", f"{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{agent.ai_config.warning_file_name}_line_{str(agent.ai_config.warning_start_line)}_execution_info"), "a+") as patf:
            patf.write(
                f"!! Maven test end timestamp: " + str(time.time_ns()) + "\n")

    except subprocess.TimeoutExpired as te:
        logger.error("TimeoutExpired",
                     f"Test ran into timeout after {te.timeout / 60} minutes.")
        # Write the test end to the execution info file
        with open(os.path.join("experimental_setups", agent.exps[-1], agent.current_state, "execution_info", f"{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{agent.ai_config.warning_file_name}_line_{str(agent.ai_config.warning_start_line)}_execution_info"), "a+") as patf:
            patf.write(
                f"!! Maven test end timestamp: " + str(time.time_ns()) + "\n")
        raise

    return result

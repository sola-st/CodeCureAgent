
import git
from git.exc import GitError
from autogpt.agents.base import BaseAgent
import os
import shutil
from autogpt.logs.logger import logger
import subprocess

'''
Clone targeted repository and checkout targeted commit.
If the target project folder already exists it is first removed.
Throws a GitError if cloning or checking out fails.
'''
def checkout_project(agent: BaseAgent) -> None:
    repo_path = os.path.join(agent.config.workspace_path, agent.ai_config.warning_repository_name)

    logger.info("", "Project checkout procedure starting.")

    if os.path.exists(repo_path):
        logger.debug("", f"Folder '{repo_path}' already exists. Deleting the folder.")
        try:
            shutil.rmtree(repo_path)
        except OSError as e:
            logger.error("Failed removing folder", "Error: %s - %s." % (e.filename, e.strerror))

    logger.info("", f"Cloning from URL '{agent.ai_config.warning_repository_URL}' to path '{repo_path}'.")
    try:
        repo = git.Repo.clone_from(agent.ai_config.warning_repository_URL, to_path=repo_path)

    except GitError as e:
        logger.error("Git Cloning failed", f"Error: {e}")
        raise

    if agent.ai_config.warning_repository_commit.lower() == "master":
        logger.debug("", f"Skipped checking out a commit, because ai_config.warning_repository_commit is '{agent.ai_config.warning_repository_commit}'.")
    else:
        logger.info("", f"Checking out commit '{agent.ai_config.warning_repository_commit}'.")
        try:
            repo.git.checkout(agent.ai_config.warning_repository_commit)

        except GitError as e:
            logger.error("Git Checkout failed", f"Error: {e}")
            raise

'''
Try to build the target project. 
Expects that the project is already checked out.
Expects the project to be a maven project compatible with JDK 11 and with the pom.xml in the root folder of the project.
If there was an exception in the subcommand a BuildError is thrown.
'''
def build_project(agent: BaseAgent) -> None:
    repo_path = os.path.join(agent.config.workspace_path, agent.ai_config.warning_repository_name)
    

    logger.info("",
            f"Building the target project {agent.ai_config.warning_repository_name} with maven."
        )  


    timeout_ten_minutes = 10*60

    try:
        result = subprocess.run(
                ["mvn", "clean", "package", "-DskipTests", "--no-transfer-progress", "--batch-mode"],
                capture_output=True,
                encoding="utf8",
                cwd=repo_path,
                shell=False,
                timeout=timeout_ten_minutes
            )
        
    except subprocess.TimeoutExpired as te:
        logger.error("TimeoutExpired", f"Build ran into timeout after {te.timeout / 60} minutes.")
        raise
    
    if result.returncode == 0:
        logger.info("", f"Build was successful.")
    else:
        logger.error("Error", f"Build failed with returncode {result.returncode}, stdout: \n{result.stdout}\n stderr: {result.stderr}")
        raise BuildError(result.returncode, result.stdout)
    


class BuildError(Exception):
    def __init__(self, returncode, stderr):
        self.returncode = returncode
        self.stderr = stderr
        super().__init__(stderr)

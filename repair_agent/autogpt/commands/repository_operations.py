
import git
from git.exc import GitError
from autogpt.agents.base import BaseAgent
import os
import shutil
from autogpt.logs.logger import logger

'''
Clone targeted repository and checkout targeted commit.
If the target project folder already exists it is first removed.
Throws a GitError if cloning or checking out fails.
'''
def checkout_project(agent: BaseAgent):
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

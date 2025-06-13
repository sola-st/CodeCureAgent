"""A module that contains the AIConfig class object that contains the configuration"""
from __future__ import annotations

import platform
from pathlib import Path
from typing import TYPE_CHECKING, Optional

import distro
import yaml

if TYPE_CHECKING:
    from agent_core.models.command_registry import CommandRegistry

from agent_core.logs.logger import logger


class AIConfig:
    """
    A class object that contains the configuration information for the AI

    Attributes:
        ai_name (str): The name of the AI.
        warning_ID (int): A unique ID for the warning (the specific warning instance the agent runs on)
        warning_repository_URL (str): The Git repository with the SonarQube warning to fix
        warning_repository_commit (str): The commit of the Git repo to fix the warning on. Can be a commitId or "MASTER" for the most current commit.
        warning_file_path (str): The file path to the file with the SonarQube warning
        warning_rule_key (str): The rule identifier of the SonarQube warning to fix
        warning_start_line (int): The line where the rule violation is located
        warning_rule_name (str): The name of the SonarQube warning to fix (short description)
        warning_specific_messgage (str): The context-specific message of the rule violation
    """

    def __init__(
        self,
        ai_name: str = "",
        warning_ID: int = -1,
        warning_repository_URL: str = "",
        warning_repository_commit: str = "",
        warning_file_path: str = "",
        warning_rule_key: str = "",
        warning_start_line: int = -1,
        warning_rule_name: str = "",
        warning_specific_message: str = "",
    ) -> None:
        """
        Initialize a class instance

        Parameters:
            ai_name (str): The name of the AI.
            warning_ID (int): A unique ID for the warning (the specific warning instance the agent runs on)
            warning_repository_URL (str): The Git repository with the SonarQube warning to fix
            warning_repository_commit (str): The commit of the Git repo to fix the warning on. Can be a commitId or "MASTER" for the most current commit.
            warning_file_path (str): The file path to the file with the SonarQube warning
            warning_rule_key (str): The rule identifier of the SonarQube warning to fix
            warning_start_line (int): The line where the rule violation is located
            warning_rule_name (str): The name of the SonarQube warning to fix (short description)
            warning_specific_messgage (str): The context-specific message of the rule violation
        Returns:
            None
        """
        self.ai_name = ai_name
        self.warning_ID = int(warning_ID)
        self.warning_repository_URL = warning_repository_URL
        self.warning_repository_commit = warning_repository_commit
        self.warning_file_path = warning_file_path
        self.warning_rule_key = warning_rule_key
        self.warning_start_line = int(warning_start_line)
        self.warning_rule_name = warning_rule_name
        self.warning_specific_message = warning_specific_message
        self.command_registry: CommandRegistry | None = None

        # Retrieve the repository name from the repository URL
        if warning_repository_URL:
            last_forward_slash_index = warning_repository_URL.rfind("/")
            suffix_index = warning_repository_URL.rfind(".git")

            if suffix_index < 0:
                suffix_index = len(warning_repository_URL)

            if last_forward_slash_index < 0 or suffix_index <= last_forward_slash_index:
                logger.error("Couldn't extract repository name from warning_repository_URL",
                             f"The ai_config.warning_repository_URL was {warning_repository_URL}. The last forward slash was at {last_forward_slash_index}. The suffix_index at {suffix_index}.")
                logger.warn(
                    "Falling back to warning_repository_name 'unknown_repo'. However cloning the repo will likely also fail.")
                self.warning_repository_name = "unknown_repo"

            else:
                self.warning_repository_name = warning_repository_URL[
                    last_forward_slash_index + 1:suffix_index]

        # Retrieve the target file name from the file path
        if warning_file_path:
            self.warning_file_name = warning_file_path.split("/")[-1]

    @staticmethod
    def load(ai_settings_file: str | Path) -> "AIConfig":
        """
        Returns class object with parameters loaded from yaml file if yaml file exists, else returns class with no parameters.

        Parameters:
            ai_settings_file (Path): The path to the config yaml file.

        Returns:
            cls (object): An instance of given cls object
        """

        try:
            with open(ai_settings_file, encoding="utf-8") as file:
                config_params = yaml.load(file, Loader=yaml.FullLoader) or {}
        except FileNotFoundError:
            config_params = {}
        ai_name = config_params.get("ai_name", "CodeCureAgent")
        warning_ID = config_params.get("warning_ID", -1)
        warning_repository_URL = config_params.get(
            "warning_repository_URL", "")
        warning_repository_commit = config_params.get(
            "warning_repository_commit", "")
        warning_file_path = config_params.get("warning_file_path", "")
        warning_rule_key = config_params.get("warning_rule_key", "")
        warning_start_line = config_params.get("warning_start_line", "")
        warning_rule_name = config_params.get("warning_rule_name", "")
        warning_specific_message = config_params.get(
            "warning_specific_message", "")

        return AIConfig(ai_name, warning_ID, warning_repository_URL, warning_repository_commit, warning_file_path, warning_rule_key, warning_start_line, warning_rule_name, warning_specific_message)

    def save(self, ai_settings_file: str | Path) -> None:
        """
        Saves the class parameters to the specified file yaml file path as a yaml file.

        Parameters:
            ai_settings_file (Path): The path to the config yaml file.

        Returns:
            None
        """

        config = {
            "warning_ID": self.warning_ID,
            "warning_repository_URL": self.warning_repository_URL,
            "warning_repository_commit": self.warning_repository_commit,
            "warning_file_path": self.warning_file_path,
            "warning_rule_key": self.warning_rule_key,
            "warning_start_line": self.warning_start_line,
            "warning_rule_name": self.warning_rule_name,
            "warning_specific_message": self.warning_specific_message,
        }
        with open(ai_settings_file, "w", encoding="utf-8") as file:
            yaml.dump(config, file, allow_unicode=True)

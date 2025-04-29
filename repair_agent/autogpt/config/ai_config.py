"""A module that contains the AIConfig class object that contains the configuration"""
from __future__ import annotations

import platform
from pathlib import Path
from typing import TYPE_CHECKING, Optional

import distro
import yaml

if TYPE_CHECKING:
    from autogpt.models.command_registry import CommandRegistry
    from autogpt.prompts.generator import PromptGenerator

    from .config import Config

from autogpt.logs.logger import logger


class AIConfig:
    """
    A class object that contains the configuration information for the AI

    Attributes:
        ai_name (str): The name of the AI.
        ai_role (str): The description of the AI's role.
        ai_goals (list): The list of objectives the AI is supposed to complete.
        warning_repository_URL (str): The Git repository with the SonarQube warning to fix
        warning_repository_commit (str): The commit of the Git repo to fix the warning on. Can be a commitId or "MASTER" for the most current commit.
        warning_file_path (str): The file path to the file with the SonarQube warning
        warning_rule_key (str): The rule identifier of the SonarQube warning to fix
        warning_rule_name (str): The name of the SonarQube warning to fix (short description)
        api_budget (float): The maximum dollar value for API calls (0.0 means infinite)
    """

    def __init__(
        self,
        ai_name: str = "",
        ai_role: str = "",
        ai_goals: list[str] = [],
        warning_repository_URL: str = "",
        warning_repository_commit: str = "",
        warning_file_path: str = "",
        warning_rule_key: str = "",
        warning_rule_name: str = "",
        api_budget: float = 0.0,
    ) -> None:
        """
        Initialize a class instance

        Parameters:
            ai_name (str): The name of the AI.
            ai_role (str): The description of the AI's role.
            ai_goals (list): The list of objectives the AI is supposed to complete.
            warning_repository_URL (str): The Git repository with the SonarQube warning to fix
            warning_repository_commit (str): The commit of the Git repo to fix the warning on. Can be a commitId or "MASTER" for the most current commit.
            warning_file_path (str): The file path to the file with the SonarQube warning
            warning_rule_key (str): The rule identifier of the SonarQube warning to fix
            warning_rule_name (str): The name of the SonarQube warning to fix (short description)
            api_budget (float): The maximum dollar value for API calls (0.0 means infinite)
        Returns:
            None
        """
        self.ai_name = ai_name
        self.ai_role = ai_role
        self.ai_goals = ai_goals
        self.warning_repository_URL = warning_repository_URL
        self.warning_repository_commit = warning_repository_commit
        self.warning_file_path = warning_file_path
        self.warning_rule_key = warning_rule_key
        self.warning_rule_name = warning_rule_name
        self.api_budget = api_budget
        self.prompt_generator: PromptGenerator | None = None
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
                logger.warn("Falling back to warning_repository_name 'unknown_repo'. However cloning the repo will likely also fail.")
                self.warning_repository_name = "unknown_repo"
            
            else:
                self.warning_repository_name = warning_repository_URL[last_forward_slash_index + 1:suffix_index]

    @staticmethod
    def load(ai_settings_file: str | Path) -> "AIConfig":
        """
        Returns class object with parameters (ai_name, ai_role, ai_goals, api_budget)
        loaded from yaml file if yaml file exists, else returns class with no parameters.

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

        ai_name = config_params.get("ai_name", "")
        ai_role = config_params.get("ai_role", "")
        ai_goals = [
            str(goal).strip("{}").replace("'", "").replace('"', "")
            if isinstance(goal, dict)
            else str(goal)
            for goal in config_params.get("ai_goals", [])
        ]
        warning_repository_URL = config_params.get("warning_repository_URL", "")
        warning_repository_commit = config_params.get("warning_repository_commit", "")
        warning_file_path = config_params.get("warning_file_path", "")
        warning_rule_key = config_params.get("warning_rule_key", "")
        warning_rule_name = config_params.get("warning_rule_name", "")
        api_budget = config_params.get("api_budget", 0.0)

        return AIConfig(ai_name, ai_role, ai_goals, warning_repository_URL, warning_repository_commit, warning_file_path, warning_rule_key, warning_rule_name, api_budget)

    def save(self, ai_settings_file: str | Path) -> None:
        """
        Saves the class parameters to the specified file yaml file path as a yaml file.

        Parameters:
            ai_settings_file (Path): The path to the config yaml file.

        Returns:
            None
        """

        config = {
            "ai_name": self.ai_name,
            "ai_role": self.ai_role,
            "ai_goals": self.ai_goals,
            "warning_repository_URL" : self.warning_repository_URL,
            "warning_repository_commit" : self.warning_repository_commit,
            "warning_file_path" : self.warning_file_path,
            "warning_rule_key" : self.warning_rule_key,
            "warning_rule_name" : self.warning_rule_name,
            "api_budget": self.api_budget,
        }
        with open(ai_settings_file, "w", encoding="utf-8") as file:
            yaml.dump(config, file, allow_unicode=True)

    def construct_full_prompt(
        self, config: Config, prompt_generator: Optional[PromptGenerator] = None
    ) -> str:
        """
        Returns a prompt to the user with the class information in an organized fashion.

        Parameters:
            None

        Returns:
            full_prompt (str): A string containing the initial prompt for the user
              including the ai_name, ai_role, ai_goals, and api_budget.
        """

        from autogpt.prompts.prompt import build_default_prompt_generator

        prompt_generator = prompt_generator or self.prompt_generator
        if prompt_generator is None:
            prompt_generator = build_default_prompt_generator(config)
            prompt_generator.command_registry = self.command_registry
            self.prompt_generator = prompt_generator

        for plugin in config.plugins:
            if not plugin.can_handle_post_prompt():
                continue
            prompt_generator = plugin.post_prompt(prompt_generator)

        # Construct full prompt
        full_prompt_parts = {
            
            "role": f"You are {self.ai_name}, {self.ai_role.rstrip('.')}." +\
            "Your decisions must always be made independently without seeking " +\
            "user assistance. Play to your strengths as an LLM and pursue " +\
            "simple strategies with no legal complications."
        }

        if config.execute_local_commands:
            # add OS info to prompt
            os_name = platform.system()
            os_info = (
                platform.platform(terse=True)
                if os_name != "Linux"
                else distro.name(pretty=True)
            )

            full_prompt_parts.append(f"The OS you are running on is: {os_info}")

        if self.ai_goals:
            full_prompt_parts["goals"] = [
                        "## Goals",
                        "For your task, you must fulfill the following goals:",
                        *[f"{i+1}. {goal}" for i, goal in enumerate(self.ai_goals)],
                    ]
            
        additional_constraints: list[str] = []
        if self.api_budget > 0.0:
            additional_constraints["additional constraints"] = (
                f"It takes money to let you run. "
                f"Your API budget is ${self.api_budget:.3f}"
            )

        full_prompt_parts.update(
            prompt_generator.generate_prompt_string(
            )
        )

        return full_prompt_parts

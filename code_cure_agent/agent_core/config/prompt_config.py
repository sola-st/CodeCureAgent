# sourcery skip: do-not-use-staticmethod
"""
A module that contains the PromptConfig class object that contains the configuration
"""
import yaml
from colorama import Fore

from agent_core.utils.yaml_utils import yaml_utils
from agent_core.logs import logger
import logging


class PromptConfig:
    """
    A class object that contains the configuration information for the prompt, which will be used by the prompt generator

    Attributes:
        constraints (list): Constraints list for the prompt generator.
        resources (list): Resources list for the prompt generator.
        performance_evaluations (list): Performance evaluation list for the prompt generator.
    """

    def __init__(self, prompt_settings_file: str) -> None:
        """
        Initialize a class instance with parameters (constraints, resources, performance_evaluations) loaded from
          yaml file if yaml file exists,
        else raises error.

        Parameters:
            constraints (list): Constraints list for the prompt generator.
            resources (list): Resources list for the prompt generator.
            performance_evaluations (list): Performance evaluation list for the prompt generator.
        Returns:
            None
        """
        # Validate file
        (validated, message) = yaml_utils.validate_yaml_file(prompt_settings_file)
        if not validated:
            logger.error(title="FAILED FILE VALIDATION", message=message)
            logger.double_check()
            exit(1)

        with open(prompt_settings_file, encoding="utf-8") as file:
            config_params = yaml.load(file, Loader=yaml.FullLoader)

        self.general_guidelines = config_params.get("general_guidelines", [])
        # self.simple_patterns = config_params.get("simple_patterns", [])

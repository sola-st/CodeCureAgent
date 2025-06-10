"""Set up the AI and its goals"""
import re
from typing import Optional

from colorama import Fore, Style
from jinja2 import Template

from agent_core.app import utils
from agent_core.config import Config
from agent_core.config.ai_config import AIConfig
from agent_core.llm.base import ChatSequence, Message
from agent_core.llm.utils import create_chat_completion
from agent_core.logs import logger
from agent_core.prompts.default_prompts import (
    DEFAULT_SYSTEM_PROMPT_AICONFIG_AUTOMATIC,
    DEFAULT_TASK_PROMPT_AICONFIG_AUTOMATIC,
    DEFAULT_USER_DESIRE_PROMPT,
)


def prompt_user(
    config: Config, ai_config_template: Optional[AIConfig] = None
) -> AIConfig:
    """Prompt the user for input

    Params:
        config (Config): The Config object
        ai_config_template (AIConfig): The AIConfig object to use as a template

    Returns:
        AIConfig: The AIConfig object tailored to the user's input
    """

    # Construct the prompt
    logger.info(
        title="Welcome to Auto-GPT! ",
        title_color=Fore.GREEN,
        message="run with '--help' for more information."
    )

    ai_config_template_provided = ai_config_template is not None and any(
        [
            ai_config_template.ai_goals,
            ai_config_template.ai_name,
            ai_config_template.ai_role,
            ai_config_template.warning_ID,
            ai_config_template.warning_repository_URL,
            ai_config_template.warning_repository_commit,
            ai_config_template.warning_file_path,
            ai_config_template.warning_rule_key,
            ai_config_template.warning_start_line,
            ai_config_template.warning_rule_name,
            ai_config_template.warning_specific_message
        ]
    )

    user_desire = ""
    if not ai_config_template_provided:
        # Get user desire if command line overrides have not been passed in
        logger.info(
            title="Create an AI-Assistant:",
            title_color=Fore.GREEN,
            message="input '--manual' to enter manual mode."
        )

        user_desire = utils.clean_input(
            config, f"{Fore.LIGHTBLUE_EX}I want Auto-GPT to{Style.RESET_ALL}: "
        )

    if user_desire.strip() == "":
        user_desire = DEFAULT_USER_DESIRE_PROMPT  # Default prompt

    # If user desire contains "--manual" or we have overridden any of the AI configuration
    if "--manual" in user_desire or ai_config_template_provided:
        logger.info(
            title="Manual Mode Selected",
            title_color=Fore.GREEN
        )
        return generate_aiconfig_manual(config, ai_config_template)

    else:
        try:
            return generate_aiconfig_automatic(user_desire, config)
        except Exception as e:
            logger.info(
                title="Unable to automatically generate AI Config based on user desire.",
                title_color=Fore.RED,
                message="Falling back to manual mode."
            )
            logger.debug(f"Error during AIConfig generation: {e}")

            return generate_aiconfig_manual(config)


def generate_aiconfig_manual(
    config: Config, ai_config_template: Optional[AIConfig] = None
) -> AIConfig:
    """
    Interactively create an AI configuration by prompting the user to provide the name, role, and goals of the AI.

    This function guides the user through a series of prompts to collect the necessary information to create
    an AIConfig object. The user will be asked to provide a name and role for the AI, as well as up to five
    goals. If the user does not provide a value for any of the fields, default values will be used.

    Params:
        config (Config): The Config object
        ai_config_template (AIConfig): The AIConfig object to use as a template

    Returns:
        AIConfig: An AIConfig object containing the user-defined or default AI name, role, and goals.
    """

    # Manual Setup Intro
    logger.info(
        title="Create an AI-Assistant:",
        title_color=Fore.GREEN,
        message="Enter the name of your AI and its role below. Entering nothing will load"
        " defaults."
    )

    if ai_config_template and ai_config_template.ai_name:
        ai_name = ai_config_template.ai_name
    else:
        ai_name = ""
        # Get AI Name from User
        logger.info(
            title="Name your AI: ", title_color=Fore.GREEN, message="For example, 'Entrepreneur-GPT'"
        )
        ai_name = utils.clean_input(config, "AI Name: ")
    if ai_name == "":
        ai_name = "Entrepreneur-GPT"

    logger.info(
        title=f"{ai_name} here!", title_color=Fore.LIGHTBLUE_EX, message="I am at your service."
    )

    if ai_config_template and ai_config_template.ai_role:
        ai_role = ai_config_template.ai_role
    else:
        # Get AI Role from User
        logger.info(
            title="Describe your AI's role: ",
            title_color=Fore.GREEN,
            message="For example, 'an AI designed to autonomously develop and run businesses with"
            " the sole goal of increasing your net worth.'",
        )
        ai_role = utils.clean_input(config, f"{ai_name} is: ")
    if ai_role == "":
        ai_role = "an AI designed to autonomously develop and run businesses with the"
        " sole goal of increasing your net worth."

    if ai_config_template and ai_config_template.ai_goals:
        ai_goals = ai_config_template.ai_goals
    else:
        # Enter up to 5 goals for the AI
        logger.info(
            title="Enter up to 5 goals for your AI: ",
            title_color=Fore.GREEN,
            message="For example: \nIncrease net worth, Grow Twitter Account, Develop and manage"
            " multiple businesses autonomously'",
        )
        logger.info(
            "Enter nothing to load defaults, enter nothing when finished.")
        ai_goals = []
        for i in range(5):
            ai_goal = utils.clean_input(
                config, f"{Fore.LIGHTBLUE_EX}Goal{Style.RESET_ALL} {i+1}: "
            )
            if ai_goal == "":
                break
            ai_goals.append(ai_goal)
    if not ai_goals:
        ai_goals = [
            "Increase net worth",
            "Grow Twitter Account",
            "Develop and manage multiple businesses autonomously",
        ]

    if ai_config_template and ai_config_template.warning_ID:
        warning_ID = ai_config_template.warning_ID
    else:
        # Get repository URL from User
        logger.info(
            title="Give a unique ID for the agent run identifying the specific warning: ",
            title_color=Fore.GREEN
        )
        warning_ID = int(utils.clean_input(
            config, "Warning ID is: "))

    if ai_config_template and ai_config_template.warning_repository_URL:
        warning_repository_URL = ai_config_template.warning_repository_URL
    else:
        # Get repository URL from User
        logger.info(
            title="Give the Git repository URL of the project with the warning to fix: ",
            title_color=Fore.GREEN
        )
        warning_repository_URL = utils.clean_input(
            config, "Repository URL is: ")

    if ai_config_template and ai_config_template.warning_repository_commit:
        warning_repository_commit = ai_config_template.warning_repository_commit
    else:
        # Get repository Commit from User
        logger.info(
            title="Give the Git repository Commit of the project to use: ",
            title_color=Fore.GREEN
        )
        warning_repository_commit = utils.clean_input(
            config, "Repository Commit is: ")

    if ai_config_template and ai_config_template.warning_file_path:
        warning_file_path = ai_config_template.warning_file_path
    else:
        # Get warning file path from User
        logger.info(
            title="Give the File path of the file with warning to fix: ",
            title_color=Fore.GREEN
        )
        warning_file_path = utils.clean_input(config, "File path: ")

    if ai_config_template and ai_config_template.warning_rule_key:
        warning_rule_key = ai_config_template.warning_rule_key
    else:
        # Get rule key from User
        logger.info(
            title="Give the SonarQube rule key of the warning to fix: ",
            title_color=Fore.GREEN
        )
        warning_rule_key = utils.clean_input(config, "Rule key: ")

    if ai_config_template and ai_config_template.warning_start_line:
        warning_start_line = ai_config_template.warning_start_line
    else:
        # Get repository URL from User
        logger.info(
            title="Give the start line of the rule violation: ",
            title_color=Fore.GREEN
        )
        warning_start_line = int(utils.clean_input(
            config, "Rule violation start line: "))

    if ai_config_template and ai_config_template.warning_rule_name:
        warning_rule_name = ai_config_template.warning_rule_name
    else:
        # Get repository URL from User
        logger.info(
            title="Give the rule name (short description) of the warning to fix: ",
            title_color=Fore.GREEN
        )
        warning_rule_name = utils.clean_input(config, "Rule name: ")

    if ai_config_template and ai_config_template.warning_specific_message:
        warning_specific_message = ai_config_template.warning_specific_message
    else:
        # Get repository URL from User
        logger.info(
            title="Give the rule violations specific message: ",
            title_color=Fore.GREEN
        )
        warning_specific_message = utils.clean_input(
            config, "Specific message: ")

    # Get API Budget from User
    logger.info(
        title="Enter your budget for API calls: ",
        title_color=Fore.GREEN,
        message="For example: $1.50",
    )
    logger.info("Enter nothing to let the AI run without monetary limit")
    api_budget_input = utils.clean_input(
        config, f"{Fore.LIGHTBLUE_EX}Budget{Style.RESET_ALL}: $"
    )
    if api_budget_input == "":
        api_budget = 0.0
    else:
        try:
            api_budget = float(api_budget_input.replace("$", ""))
        except ValueError:
            logger.error(
                title="Invalid budget input. Setting budget to unlimited."
            )
            api_budget = 0.0

    return AIConfig(ai_name, ai_role, ai_goals, warning_ID, warning_repository_URL, warning_repository_commit, warning_file_path, warning_rule_key, warning_start_line, warning_rule_name, warning_specific_message, api_budget)


def generate_aiconfig_automatic(user_prompt: str, config: Config) -> AIConfig:
    """Generates an AIConfig object from the given string.

    Returns:
    AIConfig: The AIConfig object tailored to the user's input
    """

    system_prompt = DEFAULT_SYSTEM_PROMPT_AICONFIG_AUTOMATIC
    prompt_ai_config_automatic = Template(
        DEFAULT_TASK_PROMPT_AICONFIG_AUTOMATIC
    ).render(user_prompt=user_prompt)
    # Call LLM with the string as user input
    output = create_chat_completion(
        ChatSequence.for_model(
            config.fast_llm,
            [
                Message("system", system_prompt),
                Message("user", prompt_ai_config_automatic),
            ],
        ),
        config,
    ).content

    # Debug LLM Output
    logger.debug(f"AI Config Generator Raw Output: {output}")

    # Parse the output
    ai_name = re.search(r"Name(?:\s*):(?:\s*)(.*)",
                        output, re.IGNORECASE).group(1)
    ai_role = (
        re.search(
            r"Description(?:\s*):(?:\s*)(.*?)(?:(?:\n)|Goals)",
            output,
            re.IGNORECASE | re.DOTALL,
        )
        .group(1)
        .strip()
    )
    ai_goals = re.findall(r"(?<=\n)-\s*(.*)", output)
    api_budget = 0.0  # TODO: parse api budget using a regular expression

    return AIConfig(ai_name, ai_role, ai_goals, api_budget)

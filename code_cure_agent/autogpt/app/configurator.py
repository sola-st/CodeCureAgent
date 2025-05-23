"""Configurator module."""
from __future__ import annotations

from typing import Literal

import click
from colorama import Back, Fore, Style
import logging

from autogpt.utils.yaml_utils import yaml_utils
from autogpt.config import Config
from autogpt.llm.api_manager import ApiManager
from autogpt.logs import logger
from autogpt.memory.vector import get_supported_memory_backends

from pathlib import Path


def create_config(
    config: Config,
    continuous: bool,
    continuous_limit: int,
    ai_settings_file: str,
    prompt_settings_file: str,
    skip_reprompt: bool,
    none_interactive: bool,
    speak: bool,
    debug: bool,
    model_version: str,
    memory_type: str,
    browser_name: str,
    allow_downloads: bool,
    skip_news: bool,
    sorald_jar_path: str | None,
) -> None:
    """Updates the config object with the given arguments.

    Args:
        continuous (bool): Whether to run in continuous mode
        continuous_limit (int): The number of times to run in continuous mode
        ai_settings_file (str): The path to the ai_settings.yaml file
        prompt_settings_file (str): The path to the prompt_settings.yaml file
        skip_reprompt (bool): Whether to skip the re-prompting messages at the beginning of the script
        none_interactive (bool): If set the user will not be prompted for input when the last cycle was reached. This allows for running the agent without needing manual user feedback.
        speak (bool): Whether to enable speak mode
        debug (bool): Whether to enable debug mode
        model_version (str): The GPT model to use. Has to be passed as command line argument
        memory_type (str): The type of memory backend to use
        browser_name (str): The name of the browser to use when using selenium to scrape the web
        allow_downloads (bool): Whether to allow Auto-GPT to download files natively
        skips_news (bool): Whether to suppress the output of latest news on startup
        sorald_jar_path: (Path): The path to the sorald executable for mining SonarQube warnings
    """
    config.debug_mode = False
    config.continuous_mode = False
    config.speak_mode = False

    if debug:
        logger.typewriter_log("Debug Mode: ", Fore.GREEN, "ENABLED")
        config.debug_mode = True

    if continuous:
        logger.typewriter_log("Continuous Mode: ", Fore.RED, "ENABLED")
        logger.typewriter_log(
            "WARNING: ",
            Fore.RED,
            "Continuous mode is not recommended. It is potentially dangerous and may"
            " cause your AI to run forever or carry out actions you would not usually"
            " authorise. Use at your own risk.",
        )
        config.continuous_mode = True

        if continuous_limit:
            logger.typewriter_log(
                "Continuous Limit: ", Fore.GREEN, f"{continuous_limit}"
            )
            config.continuous_limit = continuous_limit

    # Check if continuous limit is used without continuous mode
    if continuous_limit and not continuous:
        raise click.UsageError(
            "--continuous-limit can only be used with --continuous")

    if speak:
        logger.typewriter_log("Speak Mode: ", Fore.GREEN, "ENABLED")
        config.speak_mode = True

    # Set the used LLM model
    # If --model_version set, try to use this version of GPT model if available
    if (check_model(model_version, model_type="fast_llm", config=config) == model_version):
        logger.typewriter_log(
            f"LLM set to: {model_version}", Fore.GREEN, "ENABLED")
        config.fast_llm = model_version
        config.smart_llm = model_version

    else:
        # Defaults to gpt-3.5-turbo-0125
        config.fast_llm = check_model(
            config.fast_llm, "fast_llm", config=config)
        config.smart_llm = check_model(
            config.smart_llm, "smart_llm", config=config)

    if memory_type:
        supported_memory = get_supported_memory_backends()
        chosen = memory_type
        if chosen not in supported_memory:
            logger.typewriter_log(
                "ONLY THE FOLLOWING MEMORY BACKENDS ARE SUPPORTED: ",
                Fore.RED,
                f"{supported_memory}",
            )
            logger.typewriter_log(
                "Defaulting to: ", Fore.YELLOW, config.memory_backend)
        else:
            config.memory_backend = chosen

    if skip_reprompt:
        logger.typewriter_log("Skip Re-prompt: ", Fore.GREEN, "ENABLED")
        config.skip_reprompt = True

    if none_interactive:
        logger.typewriter_log("None-Interactive mode: ", Fore.GREEN, "ENABLED")
        config.none_interactive = True

    if ai_settings_file:
        file = ai_settings_file

        # Validate file
        (validated, message) = yaml_utils.validate_yaml_file(file)
        if not validated:
            logger.typewriter_log("FAILED FILE VALIDATION",
                                  Fore.RED, message, level=logging.ERROR)
            logger.double_check()
            exit(1)

        logger.typewriter_log("Using AI Settings File:", Fore.GREEN, file)
        config.ai_settings_file = file
        config.skip_reprompt = True

    if prompt_settings_file:
        file = prompt_settings_file

        # Validate file
        (validated, message) = yaml_utils.validate_yaml_file(file)
        if not validated:
            logger.typewriter_log("FAILED FILE VALIDATION",
                                  Fore.RED, message, level=logging.ERROR)
            logger.double_check()
            exit(1)

        logger.typewriter_log("Using Prompt Settings File:", Fore.GREEN, file)
        config.prompt_settings_file = file

    if browser_name:
        config.selenium_web_browser = browser_name

    if allow_downloads:
        logger.typewriter_log("Native Downloading:", Fore.GREEN, "ENABLED")
        logger.typewriter_log(
            "WARNING: ",
            Fore.YELLOW,
            f"{Back.LIGHTYELLOW_EX}Auto-GPT will now be able to download and save files to your machine.{Back.RESET} "
            + "It is recommended that you monitor any files it downloads carefully.",
            level=logging.WARNING)
        logger.typewriter_log(
            "WARNING: ",
            Fore.YELLOW,
            f"{Back.RED + Style.BRIGHT}ALWAYS REMEMBER TO NEVER OPEN FILES YOU AREN'T SURE OF!{Style.RESET_ALL}", level=logging.WARNING
        )
        config.allow_downloads = True

    if skip_news:
        config.skip_news = True

    if sorald_jar_path:
        config.sorald_jar_path = Path(sorald_jar_path)


def check_model(
    model_name: str,
    model_type: Literal["smart_llm", "fast_llm"],
    config: Config,
) -> str:
    """Check if model is available for use. If not, return gpt-3.5-turbo."""
    openai_credentials = config.get_openai_credentials(model_name)
    api_manager = ApiManager()
    models = api_manager.get_models(**openai_credentials)

    if any(model_name in m["id"] for m in models):
        return model_name

    logger.typewriter_log(
        "WARNING: ",
        Fore.YELLOW,
        f"You do not have access to {model_name}. Setting {model_type} to "
        f"gpt-3.5-turbo.", level=logging.WARNING
    )
    return "gpt-3.5-turbo"

"""Configurator module."""
from __future__ import annotations

from typing import Literal

import click
from colorama import Back, Fore, Style
import logging

from agent_core.utils.yaml_utils import yaml_utils
from agent_core.config import Config
from agent_core.llm.api_manager import ApiManager
from agent_core.logs import logger
from agent_core.memory.vector import get_supported_memory_backends

from pathlib import Path


def create_config(
    config: Config,
    ai_settings_file: str,
    skip_reprompt: bool,
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
        config (Config): The already defined parts of the configuration
        ai_settings_file (str): The path to the ai_settings.yaml file
        skip_reprompt (bool): Whether to skip the re-prompting messages at the beginning of the script
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
    config.speak_mode = False

    if debug:
        logger.info(title="Debug Mode: ",
                    title_color=Fore.GREEN, message="ENABLED")
        config.debug_mode = True

    if speak:
        logger.info(title="Speak Mode: ",
                    title_color=Fore.GREEN, message="ENABLED")
        config.speak_mode = True

    # Set the used LLM model
    # If --model_version set, try to use this version of GPT model if available
    if (check_model(model_version, model_type="fast_llm", config=config) == model_version):
        logger.info(
            title="LLM set to", message=model_version, title_color=Fore.GREEN)
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
            logger.info(
                title="ONLY THE FOLLOWING MEMORY BACKENDS ARE SUPPORTED: ",
                title_color=Fore.RED,
                message=f"{supported_memory}",
            )
            logger.info(
                title="Defaulting to: ", title_color=Fore.YELLOW, message=config.memory_backend)
        else:
            config.memory_backend = chosen

    if skip_reprompt:
        logger.info(title="Skip Re-prompt: ",
                    title_color=Fore.GREEN, message="ENABLED")
        config.skip_reprompt = True

    if ai_settings_file:
        file = ai_settings_file

        # Validate file
        (validated, message) = yaml_utils.validate_yaml_file(file)
        if not validated:
            logger.error(title="FAILED FILE VALIDATION", message=message)
            logger.double_check()
            exit(1)

        logger.info(title="Using AI Settings File:",
                    title_color=Fore.GREEN, message=file)
        config.ai_settings_file = file
        config.skip_reprompt = True

    if browser_name:
        config.selenium_web_browser = browser_name

    if allow_downloads:
        logger.info(title="Native Downloading:",
                    title_color=Fore.GREEN, message="ENABLED")
        logger.warn(
            title="WARNING: ",
            title_color=Fore.YELLOW,
            message=f"{Back.LIGHTYELLOW_EX}Auto-GPT will now be able to download and save files to your machine.{Back.RESET} "
            + "It is recommended that you monitor any files it downloads carefully.")
        logger.warn(
            title="WARNING: ",
            title_color=Fore.YELLOW,
            message=f"{Back.RED + Style.BRIGHT}ALWAYS REMEMBER TO NEVER OPEN FILES YOU AREN'T SURE OF!{Style.RESET_ALL}"
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

    logger.warn(
        title="WARNING: ",
        title_color=Fore.YELLOW,
        message=f"You do not have access to {model_name}. Setting {model_type} to "
        f"gpt-3.5-turbo."
    )
    return "gpt-3.5-turbo"

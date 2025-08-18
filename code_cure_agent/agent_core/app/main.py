"""The application entry point.  Can be invoked by a CLI or any other front end application."""
from __future__ import annotations

import logging
import signal
import sys
from pathlib import Path
from types import FrameType
from typing import Optional
import os
import time


from colorama import Fore, Style
import yaml

from agent_core.agents import Agent, AgentThoughts, CommandArgs, CommandName
from agent_core.app.configurator import create_config
from agent_core.app.spinner import Spinner
from agent_core.app.utils import (
    get_latest_bulletin,
    get_legal_warning,
    markdown_to_ansi_style,
)
from agent_core.commands import COMMAND_CATEGORIES
from agent_core.config import AIConfig, Config, ConfigBuilder, check_openai_api_key
from agent_core.llm.api_manager import ApiManager
from agent_core.logs import logger
from agent_core.memory.vector import get_memory
from agent_core.models.command_registry import CommandRegistry
from agent_core.plugins import scan_plugins
from agent_core.speech import say_text
from agent_core.workspace import Workspace
from agent_core.utils.path_utils.path_utils import sanitize_and_shorten_file_path
from scripts.install_plugin_deps import install_plugin_dependencies

from agent_core.commands import sonar_qube_analysis, repository_operations, classification_tasks
from git.exc import GitError
import subprocess

from typing import TYPE_CHECKING
if TYPE_CHECKING:
    from agent_core.commands import system

DEFAULT_SORALD_JAR_PATH = "sorald/sorald.jar"


def run_auto_gpt(
    ai_settings: str,
    skip_reprompt: bool,
    speak: bool,
    debug: bool,
    model_version: str,
    memory_type: str,
    browser_name: str,
    allow_downloads: bool,
    skip_news: bool,
    working_directory: Path,
    workspace_directory: str | Path,
    sorald_jar_path: str | None,
    install_plugin_deps: bool,
    start_up_timestamp: int,
    ai_name: str = "",
    warning_ID: Optional[int] = -1,
    warning_repository_URL: Optional[str] = None,
    warning_repository_commit: Optional[str] = None,
    warning_repository_target_java_version: Optional[str] = "",
    warning_file_path: Optional[str] = None,
    warning_rule_key: Optional[str] = None,
    warning_start_line: Optional[int] = None,
    warning_rule_name: Optional[str] = None,
    warning_specific_message: Optional[str] = None,
    warning_rule_type: Optional[str] = None,
    experiment_file: str = None
):
    if not experiment_file:
        raise ValueError("Cannot proceed without experiment file")
    # Configure logging before we do anything else.
    logger.set_level(logging.DEBUG if debug else logging.INFO)

    config = ConfigBuilder.build_config_from_env(workdir=working_directory)

    # HACK: This is a hack to allow the config into the logger without having to pass it around everywhere
    # or import it directly.
    logger.config = config

    check_openai_api_key(config)

    create_config(
        config,
        ai_settings,
        skip_reprompt,
        speak,
        debug,
        model_version,
        memory_type,
        browser_name,
        allow_downloads,
        skip_news,
        sorald_jar_path,
    )

    for line in get_legal_warning().split("\n"):
        logger.warn(markdown_to_ansi_style(line), "LEGAL:", Fore.RED)

    if not config.skip_news:
        motd, is_new_motd = get_latest_bulletin()
        if motd:
            motd = markdown_to_ansi_style(motd)
            for motd_line in motd.split("\n"):
                logger.info(motd_line, "NEWS:", Fore.GREEN)
            if is_new_motd and not config.chat_messages_enabled:
                input(
                    Fore.MAGENTA
                    + Style.BRIGHT
                    + "NEWS: Bulletin was updated! Press Enter to continue..."
                    + Style.RESET_ALL
                )

        if sys.version_info < (3, 10):
            logger.warn(
                title="WARNING: ",
                title_color=Fore.RED,
                message="You are running on an older version of Python. "
                "Some people have observed problems with certain "
                "parts of Auto-GPT with this version. "
                "Please consider upgrading to Python 3.10 or higher."
            )

    if install_plugin_deps:
        install_plugin_dependencies()

    config.workspace_path = Workspace.init_workspace_directory(
        config, workspace_directory
    )

    # HACK: doing this here to collect some globals that depend on the workspace.
    config.file_logger_path = Workspace.build_file_logger_path(
        config.workspace_path)

    # Set sorald_jar_path to default if not specified via the cli or environment variable
    if not config.sorald_jar_path:
        config.sorald_jar_path = os.path.join(
            config.workdir, DEFAULT_SORALD_JAR_PATH)

    config.plugins = scan_plugins(config, config.debug_mode)

    # Create a CommandRegistry instance and scan default folder
    command_registry = CommandRegistry.with_command_modules(
        COMMAND_CATEGORIES, config)

    ai_config = construct_main_ai_config(
        config,
        name=ai_name,
        warning_ID=warning_ID,
        warning_repository_URL=warning_repository_URL,
        warning_repository_commit=warning_repository_commit,
        warning_repository_target_java_version=warning_repository_target_java_version,
        warning_file_path=warning_file_path,
        warning_rule_key=warning_rule_key,
        warning_start_line=warning_start_line,
        warning_rule_name=warning_rule_name,
        warning_specific_message=warning_specific_message,
        warning_rule_type=warning_rule_type
    )
    ai_config.command_registry = command_registry
    # print(prompt)

    # add chat plugins capable of report to logger
    if config.chat_messages_enabled:
        for plugin in config.plugins:
            if hasattr(plugin, "can_handle_report") and plugin.can_handle_report():
                logger.info(
                    f"Loaded plugin into logger: {plugin.__class__.__name__}")
                logger.chat_plugins.append(plugin)

    # Initialize memory and make sure it is empty.
    # this is particularly important for indexing and referencing pinecone memory
    memory = get_memory(config)
    memory.clear()
    logger.info(
        title="Using memory of type:", title_color=Fore.GREEN, message=f"{memory.__class__.__name__}"
    )
    logger.info(title="Using Browser:", title_color=Fore.GREEN,
                message=config.selenium_web_browser)

    agent = Agent(
        memory=memory,
        command_registry=command_registry,
        ai_config=ai_config,
        config=config,
        experiment_file=experiment_file
    )

    # Write the saved startup time to the execution_info file
    with open(os.path.join("experimental_setups", agent.exps[-1], agent.current_state, "execution_info", f"{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{agent.ai_config.warning_file_name}_line_{str(agent.ai_config.warning_start_line)}_execution_info"), "a+") as patf:
        patf.write("!! Start up timestamp: " + str(start_up_timestamp) + "\n")

    # Save task info
    with open(os.path.join("experimental_setups", agent.exps[-1], "tasks", f"{str(agent.ai_config.warning_ID)}.yaml"), "w") as taskf:
        taskf.write(yaml.dump({"warning_ID": agent.ai_config.warning_ID, "warning_repository_URL": agent.ai_config.warning_repository_URL,
                               "warning_repository_commit": agent.ai_config.warning_repository_commit,
                               "warning_repository_target_java_version": agent.ai_config.warning_repository_target_java_version,
                               "warning_repository_name": agent.ai_config.warning_repository_name,
                               "warning_file_path": agent.ai_config.warning_file_path, "warning_file_name": agent.ai_config.warning_file_name,
                               "warning_rule_key": agent.ai_config.warning_rule_key, "warning_start_line": agent.ai_config.warning_start_line,
                               "warning_rule_name": agent.ai_config.warning_rule_name, "warning_specific_message": agent.ai_config.warning_specific_message, "warning_rule_type": agent.ai_config.warning_rule_type}))

    prepare_target_project(agent)

    # Write end time of initialization
    with open(os.path.join("experimental_setups", agent.exps[-1], agent.current_state, "execution_info", f"{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{agent.ai_config.warning_file_name}_line_{str(agent.ai_config.warning_start_line)}_execution_info"), "a+") as patf:
        patf.write("!! Initialization complete time: " +
                   str(time.time_ns()) + "\n")

    run_interaction_loop(agent)


def prepare_target_project(agent: Agent) -> None:
    '''
    Clone and checkout the target project.
    Then run the initial analysis on the target file.
    Validate that the expected rule is present in the analysis report.
    Then build the project to validate that it can be built succesfully.
    '''

    try:
        repository_operations.checkout_project(agent)

        # Create the initial analysis report of the target file.
        # If reports for further files are needed later (if the agent writes to some other file then the target file), then they are added on demand in write_fix.
        sanitized_warning_file_path = sanitize_and_shorten_file_path(
            agent.ai_config.warning_file_path)
        initial_analysis_report_target_file = sonar_qube_analysis.analyze_file_and_parse_report(agent.ai_config.warning_file_path, agent.sonar_qube_rules_in_active_profile, agent.ai_config.warning_repository_name,
                                                                                                f"{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{agent.ai_config.warning_file_name}_line_{str(agent.ai_config.warning_start_line)}_initial_analysis_report_file_{sanitized_warning_file_path}.json", agent)

        agent.initial_analysis_reports[agent.ai_config.warning_file_path] = initial_analysis_report_target_file

        # Validate that the expected rule violation is present in the analysis report
        if not sonar_qube_analysis.rule_violation_present_in_analysis_report(initial_analysis_report_target_file, agent.ai_config.warning_rule_key, agent.ai_config.warning_start_line):
            logger.error(
                "Error", f"The rule {agent.ai_config.warning_rule_key} at line {str(agent.ai_config.warning_start_line)} which was to fix wasn't part of the analysis report created by running Sorald on the file.")
            logger.error(
                "Aborting", "Preparing the target project failed. Therefore aborting the execution.")
            shutdown(agent, 1)

        repository_operations.build_project(agent)

    except repository_operations.BuildError as be:
        logger.error(
            "Error", f"Build failed with returncode {be.returncode}, stdout: \n{be.stdout}")
        logger.error(
            "Aborting", "Preparing the target project failed. Therefore aborting the execution.")
        shutdown(agent, 1)
    except (sonar_qube_analysis.AnalysisError, GitError, subprocess.TimeoutExpired, TimeoutError):
        logger.error(
            "Aborting", "Preparing the target project failed. Therefore aborting the execution.")
        shutdown(agent, 1)


def run_interaction_loop(
    agent: Agent,
) -> None:
    """Run the main interaction loop for the agent.

    Args:
        agent: The agent to run the interaction loop for.

    Returns:
        None
    """

    spinner = Spinner("Thinking...", plain_output=agent.config.plain_output)

    def agent_interrupt(signum: int, frame: Optional[FrameType]) -> None:
        logger.warn(message="",
                    title="Interrupt signal received. Stopping continuous command execution immediately.",
                    title_color=Fore.RED
                    )
        shutdown(agent, 1)

    # Set up an interrupt signal for the agent.
    signal.signal(signal.SIGINT, agent_interrupt)

    logger.info(
        title="CodeCureAgent is now running the WarningClassifyAgent.", title_color=Fore.GREEN, message="This is the subagent dealing with the preliminary task of classifying the violation as TP or FP.")

    #########################
    # Application Main Loop #
    #########################

    while agent.cycle_count < agent.config.cycle_limit:
        logger.debug(
            f"Cycle budget: {agent.cycle_count}; remaining: {str(agent.config.cycle_limit - agent.cycle_count)}")

        # Print authorized commands left value
        logger.info(
            title="AUTHORISED COMMANDS LEFT: ", title_color=Fore.CYAN, message=f"{str(agent.config.cycle_limit - agent.cycle_count)}"
        )

        ########
        # Plan #
        ########
        # Have the agent determine the next action to take.
        with spinner:
            command_name, command_args, assistant_reply_dict = agent.think()

        ###############
        # Update User #
        ###############
        # Print the assistant's thoughts and the next command to the user.
        update_user(agent.config, agent.ai_config, command_name,
                    command_args, assistant_reply_dict)

        user_input = None
        # First log new-line so user can differentiate sections better in console
        logger.info(message="\n")

        agent.cycle_count += 1

        ###################
        # Execute Command #
        ###################
        # If in state "classification" and the command was "give_final_verdict" and running it was successful,
        # then agent.execute automatically switches to the next state ("fix_tp" or "fix_fp") and resets the cycle_count, commands_limit and agent history.
        agent.execute(command_name, command_args, user_input)

        # If the cycle budget is exhausted and we are still in the state "classification",
        # then the agent failed to call "give_final_verdict" before exhausting the commands, eventhough it was urged to do so in the last cycle.
        # In this case we default to counting the violation as TP.
        if agent.cycle_count == agent.config.cycle_limit and agent.current_state == "classification":
            agent.final_verdict_reason = "No reason given."
            # Call the command normally called by the agent directly, for logging of the verdict
            classification_tasks.give_final_verdict(
                "TP", agent.final_verdict_reason, agent)
            agent.update_prompt_state(final_verdict_is_true_positive=True)

    # This section is only reached if no plausible fixes were created or no goals_accomplished was called after all cycles

    # Reapply fix if there was a plausible fix but goals_accomplished wasn't called before cycle exhaustion
    if agent.plausible_fixes > 0:
        system.reapply_last_plausible_fix(agent)
    # Clean shutdown when all cycles are exhausted in the second agent phase
    shutdown(agent, 0)


def shutdown(agent: Agent, signal: int):
    # Save history one more time
    with open(os.path.join("experimental_setups", agent.exps[-1], agent.current_state, "all_messages", f"{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{agent.ai_config.warning_file_name}_line_{str(agent.ai_config.warning_start_line)}_all_messages"), "w") as patf:
        patf.write(agent.history.dump())

    # Write the time of shutdown to the execution info file
    with open(os.path.join("experimental_setups", agent.exps[-1], agent.current_state, "execution_info", f"{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{agent.ai_config.warning_file_name}_line_{str(agent.ai_config.warning_start_line)}_execution_info"), "a+") as patf:
        patf.write("!! Shutdown timestamp: " + str(time.time_ns()) + "\n")

    exit(signal)


def update_user(
    config: Config,
    ai_config: AIConfig,
    command_name: CommandName | None,
    command_args: CommandArgs | None,
    assistant_reply_dict: AgentThoughts,
) -> None:
    """Prints the assistant's thoughts and the next command to the user.

    Args:
        config: The program's configuration.
        ai_config: The AI's configuration.
        command_name: The name of the command to execute.
        command_args: The arguments for the command.
        assistant_reply_dict: The assistant's reply.
    """

    print_assistant_thoughts(ai_config.ai_name, assistant_reply_dict, config)

    if command_name is not None:
        if command_name.lower().startswith("error"):
            logger.error(
                title="ERROR: ",
                message=f"The Agent failed to select an action. Error message: {command_name}"
            )
        else:
            if config.speak_mode:
                say_text(f"I want to execute {command_name}", config)

            # First log new-line so user can differentiate sections better in console
            logger.info(message="\n")
            logger.info(
                title="NEXT ACTION: ",
                title_color=Fore.CYAN,
                message=f"COMMAND = {Fore.CYAN}{remove_ansi_escape(command_name)}{Style.RESET_ALL}  "
                f"ARGUMENTS = {Fore.CYAN}{command_args}{Style.RESET_ALL}",
            )
    else:
        logger.error(
            title="NO ACTION SELECTED: ",
            message="The Agent failed to select an action."
        )


def construct_main_ai_config(
    config: Config,
    name: Optional[str] = None,
    warning_ID: Optional[int] = -1,
    warning_repository_URL: Optional[str] = None,
    warning_repository_commit: Optional[str] = None,
    warning_repository_target_java_version: Optional[str] = None,
    warning_file_path: Optional[str] = None,
    warning_rule_key: Optional[str] = None,
    warning_start_line: Optional[int] = None,
    warning_rule_name: Optional[str] = None,
    warning_specific_message: Optional[str] = None,
    warning_rule_type: Optional[str] = None
) -> AIConfig:
    """Construct the prompt for the AI to respond to

    Returns:
        str: The prompt string
    """
    ai_config = AIConfig.load(config.workdir / config.ai_settings_file)

    # Apply overrides
    if name:
        ai_config.ai_name = name
    else:
        ai_config.ai_name = "CodeCureAgent"
    if warning_ID:
        ai_config.warning_ID = int(warning_ID)
    if warning_repository_URL:
        ai_config.warning_repository_URL = warning_repository_URL
    if warning_repository_commit:
        ai_config.warning_repository_commit = warning_repository_commit
    if warning_repository_target_java_version:
        ai_config.warning_repository_target_java_version = str(
            warning_repository_target_java_version)
    if warning_file_path:
        ai_config.warning_file_path = warning_file_path
    if warning_rule_key:
        ai_config.warning_rule_key = warning_rule_key
    if warning_start_line:
        ai_config.warning_start_line = warning_start_line
    if warning_rule_name:
        ai_config.warning_rule_name = warning_rule_name
    if warning_specific_message:
        ai_config.warning_specific_message = warning_specific_message
    if warning_rule_type:
        ai_config.warning_rule_type = warning_rule_type
    if (
        all([ai_config.ai_name, ai_config.warning_ID, ai_config.warning_repository_URL, ai_config.warning_repository_commit, ai_config.warning_repository_target_java_version, ai_config.warning_file_path,
            ai_config.warning_rule_key, ai_config.warning_start_line, ai_config.warning_rule_name, ai_config.warning_specific_message, ai_config.warning_rule_type])
    ):
        logger.info(title="ai_config found: ", title_color=Fore.GREEN,
                    message="The complete ai_config was successfully loaded from the ai_settings_file.")
    else:
        logger.error(
            "There were missing configuration parameters in the ai_settings_file.")
        exit(1)

    if config.restrict_to_workspace:
        logger.info(
            title="NOTE:All files/directories created by this agent can be found inside its workspace at:",
            title_color=Fore.YELLOW,
            message=f"{config.workspace_path}",
        )
    # set the total api budget
    api_manager = ApiManager()
    api_manager.set_total_budget(0)

    # Agent Created, print message
    logger.info(
        title=ai_config.ai_name,
        title_color=Fore.LIGHTBLUE_EX,
        message="has been created with the following details:"
    )

    # Print the ai_config details
    logger.info(title="Name :", title_color=Fore.GREEN,
                message=ai_config.ai_name)
    logger.info(title="Warning ID: ",
                title_color=Fore.GREEN, message=str(ai_config.warning_ID))
    logger.info(title="Warning Repository URL: ",
                title_color=Fore.GREEN, message=ai_config.warning_repository_URL)
    logger.info(title="Warning Repository Commit: ",
                title_color=Fore.GREEN, message=ai_config.warning_repository_commit)
    logger.info(title="Warning Repository Target Java Version: ",
                title_color=Fore.GREEN, message=ai_config.warning_repository_target_java_version)
    logger.info(title="Warning Repository Name",
                title_color=Fore.GREEN, message=ai_config.warning_repository_name)
    logger.info(title="Warning File Path: ",
                title_color=Fore.GREEN, message=ai_config.warning_file_path)
    logger.info(title="Warning File Name: ",
                title_color=Fore.GREEN, message=ai_config.warning_file_name)
    logger.info(title="Warning Rule Key: ", title_color=Fore.GREEN,
                message=ai_config.warning_rule_key)
    logger.info(title="Warning Start Line: ", title_color=Fore.GREEN,
                message=str(ai_config.warning_start_line))
    logger.info(title="Warning Rule Name: ",
                title_color=Fore.GREEN, message=ai_config.warning_rule_name)
    logger.info(title="Warning Specific Message: ",
                title_color=Fore.GREEN, message=ai_config.warning_specific_message)
    logger.info(title="Warning Rule Type: ",
                title_color=Fore.GREEN, message=ai_config.warning_rule_type)
    return ai_config


def print_assistant_thoughts(
    ai_name: str,
    assistant_reply_json_valid: dict,
    config: Config,
) -> None:

    assistant_thoughts = assistant_reply_json_valid.get("thoughts", {})
    if assistant_thoughts:
        logger.info(
            title=f"{ai_name.upper()} THOUGHTS:", title_color=Fore.YELLOW, message=str(assistant_thoughts)
        )


def remove_ansi_escape(s: str) -> str:
    return s.replace("\x1B", "")

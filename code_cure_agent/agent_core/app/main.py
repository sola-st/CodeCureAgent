"""The application entry point.  Can be invoked by a CLI or any other front end application."""
import enum
import logging
import math
import signal
import sys
from pathlib import Path
from types import FrameType
from typing import Optional
import os


from colorama import Fore, Style

from agent_core.agents import Agent, AgentThoughts, CommandArgs, CommandName
from agent_core.app.configurator import create_config
from agent_core.app.setup import prompt_user
from agent_core.app.spinner import Spinner
from agent_core.app.utils import (
    clean_input,
    get_current_git_branch,
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
from agent_core.prompts.prompt import DEFAULT_TRIGGERING_PROMPT
from agent_core.speech import say_text
from agent_core.workspace import Workspace
from scripts.install_plugin_deps import install_plugin_dependencies

DEFAULT_SORALD_JAR_PATH = "sorald/sorald.jar"


def run_auto_gpt(
    continuous: bool,
    continuous_limit: int,
    ai_settings: str,
    prompt_settings: str,
    skip_reprompt: bool,
    none_interactive: bool,
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
    ai_name: Optional[str] = None,
    ai_role: Optional[str] = None,
    warning_ID: Optional[int] = -1,
    warning_repository_URL: Optional[str] = None,
    warning_repository_commit: Optional[str] = None,
    warning_file_path: Optional[str] = None,
    warning_rule_key: Optional[str] = None,
    warning_start_line: Optional[int] = None,
    warning_rule_name: Optional[str] = None,
    warning_specific_message: Optional[str] = None,
    ai_goals: tuple[str] = tuple(),
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

    # TODO: fill in llm values here
    check_openai_api_key(config)

    create_config(
        config,
        continuous,
        continuous_limit,
        ai_settings,
        prompt_settings,
        skip_reprompt,
        none_interactive,
        speak,
        debug,
        model_version,
        memory_type,
        browser_name,
        allow_downloads,
        skip_news,
        sorald_jar_path,
    )

    if config.continuous_mode:
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

    # TODO: have this directory live outside the repository (e.g. in a user's
    #   home directory) and have it come in as a command line argument or part of
    #   the env file.
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
        role=ai_role,
        warning_ID=warning_ID,
        warning_repository_URL=warning_repository_URL,
        warning_repository_commit=warning_repository_commit,
        warning_file_path=warning_file_path,
        warning_rule_key=warning_rule_key,
        warning_start_line=warning_start_line,
        warning_rule_name=warning_rule_name,
        warning_specific_message=warning_specific_message,
        goals=ai_goals,
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
        triggering_prompt=DEFAULT_TRIGGERING_PROMPT,
        ai_config=ai_config,
        config=config,
        experiment_file=experiment_file
    )

    agent.prepare_target_project()

    run_interaction_loop(agent)


def _get_cycle_budget(continuous_mode: bool, continuous_limit: int) -> int | None:
    # Translate from the continuous_mode/continuous_limit config
    # to a cycle_budget (maximum number of cycles to run without checking in with the
    # user) and a count of cycles_remaining before we check in..
    if continuous_mode:
        cycle_budget = continuous_limit if continuous_limit else math.inf
    else:
        cycle_budget = 1

    return cycle_budget


class UserFeedback(str, enum.Enum):
    """Enum for user feedback."""

    AUTHORIZE = "GENERATE NEXT COMMAND JSON"
    EXIT = "EXIT"
    TEXT = "TEXT"


def run_interaction_loop(
    agent: Agent,
) -> None:
    """Run the main interaction loop for the agent.

    Args:
        agent: The agent to run the interaction loop for.

    Returns:
        None
    """
    # These contain both application config and agent config, so grab them here.
    config = agent.config
    ai_config = agent.ai_config
    logger.debug(
        f"{ai_config.ai_name} System Prompt: {str(agent.prompt_dictionary)}")

    cycle_budget = cycles_remaining = _get_cycle_budget(
        config.continuous_mode, config.continuous_limit
    )
    spinner = Spinner("Thinking...", plain_output=config.plain_output)

    def graceful_agent_interrupt(signum: int, frame: Optional[FrameType]) -> None:
        nonlocal cycle_budget, cycles_remaining, spinner
        if cycles_remaining in [0, 1, math.inf]:
            logger.warn(
                title="Interrupt signal received. Stopping continuous command execution immediately.",
                title_color=Fore.RED
            )
            shutdown(agent, 1)
        else:
            restart_spinner = spinner.running
            if spinner.running:
                spinner.stop()

            logger.warn(
                title="Interrupt signal received. Stopping continuous command execution.",
                title_color=Fore.RED
            )
            cycles_remaining = 0
            if restart_spinner:
                spinner.start()

    # Set up an interrupt signal for the agent.
    signal.signal(signal.SIGINT, graceful_agent_interrupt)

    #########################
    # Application Main Loop #
    #########################

    while cycles_remaining > 0:
        logger.debug(
            f"Cycle budget: {cycle_budget}; remaining: {cycles_remaining}")

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
        update_user(config, ai_config, command_name,
                    command_args, assistant_reply_dict)

        if cycles_remaining == 1:  # Last cycle

            ##################
            # Get User Input #
            ##################

            if not config.none_interactive:
                user_feedback, user_input, new_cycles_remaining = get_user_feedback(
                    config,
                    ai_config,
                )

                if user_feedback == UserFeedback.AUTHORIZE:
                    if new_cycles_remaining is not None:
                        # Case 1: User is altering the cycle budget.
                        if cycle_budget > 1:
                            cycle_budget = new_cycles_remaining + 1
                        # Case 2: User is running iteratively and
                        #   has initiated a one-time continuous cycle
                        cycles_remaining = new_cycles_remaining + 1
                    else:
                        # Case 1: Continuous iteration was interrupted -> resume
                        if cycle_budget > 1:
                            logger.info(
                                title="RESUMING CONTINUOUS EXECUTION: ",
                                title_color=Fore.MAGENTA,
                                message=f"The cycle budget is {cycle_budget}.",
                            )
                        # Case 2: The agent used up its cycle budget -> reset
                        cycles_remaining = cycle_budget + 1
                    logger.info(
                        title="-=-=-=-=-=-=-= COMMAND AUTHORISED BY USER -=-=-=-=-=-=-=",
                        title_color=Fore.MAGENTA,
                        message="",
                    )
                elif user_feedback == UserFeedback.EXIT:
                    logger.info(title="Exiting...", title_color=Fore.YELLOW)
                    shutdown(agent, 0)
                else:  # user_feedback == UserFeedback.TEXT
                    command_name = "human_feedback"

                logger.info(
                    title="AUTHORISED COMMANDS LEFT: ", title_color=Fore.CYAN, message=f"{cycles_remaining}"
                )
        else:
            user_input = None
            # First log new-line so user can differentiate sections better in console
            logger.info(message="\n")
            if cycles_remaining != math.inf:
                # Print authorized commands left value
                logger.info(
                    title="AUTHORISED COMMANDS LEFT: ", title_color=Fore.CYAN, message=f"{cycles_remaining}"
                )

        ###################
        # Execute Command #
        ###################
        # Decrement the cycle counter first to reduce the likelihood of a SIGINT
        # happening during command execution, setting the cycles remaining to 1,
        # and then having the decrement set it to 0, exiting the application.
        if command_name != "human_feedback":
            cycles_remaining -= 1
        result = agent.execute(command_name, command_args, user_input)

        if result is not None:
            logger.info(title="SYSTEM: ",
                        title_color=Fore.YELLOW, message=result)
        else:
            logger.warn(title="SYSTEM: ", title_color=Fore.YELLOW,
                        message="Unable to execute command")


def shutdown(agent: Agent, signal: int):
    # Save history one more time
    sanitized_warning_file_path = agent.ai_config.warning_file_path.replace(
        "/", ".")
    with open(os.path.join("experimental_setups", agent.exps[-1], "all_messages", f"{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{sanitized_warning_file_path}_line_{str(agent.ai_config.warning_start_line)}_all_messages"), "w") as patf:
        patf.write(agent.history.dump())

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


def get_user_feedback(
    config: Config,
    ai_config: AIConfig,
) -> tuple[UserFeedback, str, int | None]:
    """Gets the user's feedback on the assistant's reply.

    Args:
        config: The program's configuration.
        ai_config: The AI's configuration.

    Returns:
        A tuple of the user's feedback, the user's input, and the number of
        cycles remaining if the user has initiated a continuous cycle.
    """
    # ### GET USER AUTHORIZATION TO EXECUTE COMMAND ###
    # Get key press: Prompt the user to press enter to continue or escape
    # to exit
    logger.info(
        f"Enter '{config.authorise_key}' to authorise command, "
        f"'{config.authorise_key} -N' to run N continuous commands, "
        f"'{config.exit_key}' to exit program, or enter feedback for "
        f"{ai_config.ai_name}..."
    )

    user_feedback = None
    user_input = ""
    new_cycles_remaining = None

    while user_feedback is None:
        # Get input from user
        if config.chat_messages_enabled:
            console_input = clean_input(config, "Waiting for your response...")
        else:
            console_input = clean_input(
                config, Fore.MAGENTA + "Input:" + Style.RESET_ALL
            )

        # Parse user input
        if console_input.lower().strip() == config.authorise_key:
            user_feedback = UserFeedback.AUTHORIZE
        elif console_input.lower().strip() == "":
            logger.warn("Invalid input format.")
        elif console_input.lower().startswith(f"{config.authorise_key} -"):
            try:
                user_feedback = UserFeedback.AUTHORIZE
                new_cycles_remaining = abs(int(console_input.split(" ")[1]))
            except ValueError:
                logger.warn(
                    f"Invalid input format. "
                    f"Please enter '{config.authorise_key} -N'"
                    " where N is the number of continuous tasks."
                )
        elif console_input.lower() in [config.exit_key, "exit"]:
            user_feedback = UserFeedback.EXIT
        else:
            user_feedback = UserFeedback.TEXT
            user_input = console_input

    return user_feedback, user_input, new_cycles_remaining


def construct_main_ai_config(
    config: Config,
    name: Optional[str] = None,
    role: Optional[str] = None,
    warning_ID: Optional[int] = -1,
    warning_repository_URL: Optional[str] = None,
    warning_repository_commit: Optional[str] = None,
    warning_file_path: Optional[str] = None,
    warning_rule_key: Optional[str] = None,
    warning_start_line: Optional[int] = None,
    warning_rule_name: Optional[str] = None,
    warning_specific_message: Optional[str] = None,
    goals: tuple[str] = tuple(),
) -> AIConfig:
    """Construct the prompt for the AI to respond to

    Returns:
        str: The prompt string
    """
    ai_config = AIConfig.load(config.workdir / config.ai_settings_file)

    # Apply overrides
    if name:
        ai_config.ai_name = name
    if role:
        ai_config.ai_role = role
    if warning_ID:
        ai_config.warning_ID = int(warning_ID)
    if warning_repository_URL:
        ai_config.warning_repository_URL = warning_repository_URL
    if warning_repository_commit:
        ai_config.warning_repository_commit = warning_repository_commit
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
    if goals:
        ai_config.ai_goals = list(goals)

    if (
        all([name, role, goals, ai_config.warning_ID, ai_config.warning_repository_URL, ai_config.warning_repository_commit, ai_config.warning_file_path,
            ai_config.warning_rule_key, ai_config.warning_start_line, ai_config.warning_rule_name, ai_config.warning_specific_message])
        or (config.skip_reprompt
            and all([ai_config.ai_name, ai_config.ai_role, ai_config.ai_goals, ai_config.warning_ID, ai_config.warning_repository_URL, ai_config.warning_repository_commit, ai_config.warning_file_path, ai_config.warning_rule_key, ai_config.warning_start_line, ai_config.warning_rule_name, ai_config.warning_specific_message]))
    ):
        logger.info(title="ai_config found: ", title_color=Fore.GREEN,
                    message="The complete ai_config was successfully loaded from the ai_settings_file.")

    elif all([ai_config.ai_name, ai_config.ai_role, ai_config.ai_goals, ai_config.warning_ID, ai_config.warning_repository_URL, ai_config.warning_repository_commit, ai_config.warning_file_path, ai_config.warning_rule_key, ai_config.warning_start_line, ai_config.warning_rule_name, ai_config.warning_specific_message]):
        logger.info(
            title="Welcome back! ",
            title_color=Fore.GREEN,
            message=f"Would you like me to return to being {ai_config.ai_name}?"
        )
        should_continue = clean_input(
            config,
            f"""Continue with the last settings?
Name:  {ai_config.ai_name}
Role:  {ai_config.ai_role}
Goals: {ai_config.ai_goals}
Warning ID: {str(ai_config.warning_ID)}
Warning Repository URL: {ai_config.warning_repository_URL}
Warning Repository Commit: {ai_config.warning_repository_commit}
Warning File Path: {ai_config.warning_file_path}
Warning Rule Key: {ai_config.warning_rule_key}
Warning Start Line: {str(ai_config.warning_start_line)}
Warning Rule Name: {ai_config.warning_rule_name}
Warning Specific Message: {ai_config.warning_specific_message}
API Budget: {"infinite" if ai_config.api_budget <= 0 else f"${ai_config.api_budget}"}
Continue ({config.authorise_key}/{config.exit_key}): """,
        )
        if should_continue.lower() == config.exit_key:
            ai_config = AIConfig()

    if any([not ai_config.ai_name, not ai_config.ai_role, not ai_config.ai_goals, not ai_config.warning_ID, not ai_config.warning_repository_URL, not ai_config.warning_repository_commit, not ai_config.warning_file_path, not ai_config.warning_rule_key, not ai_config.warning_start_line, not ai_config.warning_rule_name, not ai_config.warning_specific_message]):
        ai_config = prompt_user(config, ai_config)
        ai_config.save(config.workdir / config.ai_settings_file)

    if config.restrict_to_workspace:
        logger.info(
            title="NOTE:All files/directories created by this agent can be found inside its workspace at:",
            title_color=Fore.YELLOW,
            message=f"{config.workspace_path}",
        )
    # set the total api budget
    api_manager = ApiManager()
    api_manager.set_total_budget(ai_config.api_budget)

    # Agent Created, print message
    logger.info(
        title=ai_config.ai_name,
        title_color=Fore.LIGHTBLUE_EX,
        message="has been created with the following details:"
    )

    # Print the ai_config details
    logger.info(title="Name :", title_color=Fore.GREEN,
                message=ai_config.ai_name)
    logger.info(title="Role :", title_color=Fore.GREEN,
                message=ai_config.ai_role)
    logger.info(title="Goals:", title_color=Fore.GREEN, message="")
    for goal in ai_config.ai_goals:
        logger.info(title="-", title_color=Fore.GREEN, message=goal)
    logger.info(title="Warning ID: ",
                title_color=Fore.GREEN, message=str(ai_config.warning_ID))
    logger.info(title="Warning Repository URL: ",
                title_color=Fore.GREEN, message=ai_config.warning_repository_URL)
    logger.info(title="Warning Repository Commit: ",
                title_color=Fore.GREEN, message=ai_config.warning_repository_commit)
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
    logger.info(
        title="API Budget:",
        title_color=Fore.GREEN,
        message="infinite" if ai_config.api_budget <= 0 else f"${ai_config.api_budget}",
    )

    return ai_config


def print_assistant_thoughts(
    ai_name: str,
    assistant_reply_json_valid: dict,
    config: Config,
) -> None:
    from agent_core.speech import say_text

    assistant_thoughts = assistant_reply_json_valid.get("thoughts", {})
    if assistant_thoughts:
        logger.info(
            title=f"{ai_name.upper()} THOUGHTS:", title_color=Fore.YELLOW, message=str(assistant_thoughts)
        )


def remove_ansi_escape(s: str) -> str:
    return s.replace("\x1B", "")

from __future__ import annotations

import json
import os
from datetime import datetime
from typing import TYPE_CHECKING, Any

from colorama import Fore

if TYPE_CHECKING:
    from agent_core.config import AIConfig, Config
    from agent_core.llm.base import ChatModelResponse
    from agent_core.memory.vector import VectorMemory
    from agent_core.models.command_registry import CommandRegistry

from agent_core.utils.json_utils.json_utilities import extract_dict_from_response, validate_dict
from agent_core.llm.utils import count_string_tokens
from agent_core.logs import logger
from agent_core.logs.log_cycle import LogCycleHandler
from agent_core.memory.message_history import MessageHistory
from agent_core.llm.base import Message

from agent_core.workspace import Workspace

from .base import AgentThoughts, BaseAgent, CommandArgs, CommandName


class Agent(BaseAgent):
    """Agent class for interacting with Auto-GPT."""

    def __init__(
        self,
        ai_config: AIConfig,
        command_registry: CommandRegistry,
        memory: VectorMemory,
        config: Config,
        experiment_file: str = None
    ):
        super().__init__(
            ai_config=ai_config,
            command_registry=command_registry,
            config=config,
            experiment_file=experiment_file
        )

        self.memory = memory
        """VectorMemoryProvider used to manage the agent's context (TODO)"""

        self.workspace = Workspace(
            config.workspace_path, config.restrict_to_workspace)
        """Workspace that the agent has access to, e.g. for reading/writing files."""

        self.created_at = datetime.now().strftime("%Y%m%d_%H%M%S")
        """Timestamp the agent was created; only used for structured debug logging."""

        self.log_cycle_handler = LogCycleHandler()
        """LogCycleHandler for structured debug logging."""

    def execute(
        self,
        command_name: str | None,
        command_args: dict[str, str] | None,
        user_input: str | None,
    ) -> None:
        # Execute command
        if command_name is not None and command_name.lower() == "error_when_parsing":
            result = command_args["error"]
        elif command_name is not None and command_name.lower().startswith("error"):
            result = f"Could not execute command: {command_name}{command_args}"
        elif command_name == "human_feedback":
            result = f"Human feedback: {user_input}"
        else:
            for plugin in self.config.plugins:
                if not plugin.can_handle_pre_command():
                    continue
                command_name, _ = plugin.pre_command(
                    command_name, command_args)
            command_result = execute_command(
                command_name=command_name,
                arguments=command_args,
                agent=self,
            )
            if len(str(command_result)) < self.truncation_limit:
                result = f"Command `{command_name}` returned:  \n{command_result}"
            else:
                result = f"Command {command_name} returned a lengthy response, we truncated it to the first {str(self.truncation_limit)} characters: \n{str(command_result)[:self.truncation_limit]}"
            result_tlength = count_string_tokens(
                str(command_result), self.llm.name)
            memory_tlength = count_string_tokens(
                str(self.history.summary_message()), self.llm.name
            )
            if result_tlength + memory_tlength > self.send_token_limit:
                result = f"Failure: command {command_name} returned too much output. \
                    Do not execute this command again with the same arguments."

            for plugin in self.config.plugins:
                if not plugin.can_handle_post_command():
                    continue
                result = plugin.post_command(command_name, result)
        # Check if there's a result from the command append it to the message
        if result is None:
            self.history.add(
                "user", "Unable to execute command", "action_result")
        else:
            self.history.add("user", result, "action_result")

        sanitized_warning_file_path = self.ai_config.warning_file_path.replace(
            "/", ".")
        with open(os.path.join("experimental_setups", self.exps[-1], self.current_state, "responses", f"{str(self.ai_config.warning_ID)}_{self.ai_config.warning_repository_name}_{self.ai_config.warning_rule_key}_{sanitized_warning_file_path}_line_{str(self.ai_config.warning_start_line)}_model_responses"), "a+") as patf:
            patf.write(
                "\nCommand execution based on model response:\n" + str(result) + "\n")

        if result is not None:
            logger.info(title="SYSTEM: ",
                        title_color=Fore.YELLOW, message=result)
        else:
            logger.warn(title="SYSTEM: ", title_color=Fore.YELLOW,
                        message="Unable to execute command")

        # Switch state if we were in the classification state and a final verdict was given
        if command_name == "give_final_verdict" and str(command_result).startswith("Final verdict submitted:"):
            self.update_prompt_state(str(command_result).endswith("TP"))

    def parse_and_process_response(self, llm_response: ChatModelResponse) -> tuple[CommandName | None, CommandArgs | None, AgentThoughts]:
        if not llm_response.content:
            raise SyntaxError("Assistant response has no text content")

        # Raises a SyntaxError, if it is not a valid json
        assistant_reply_dict = extract_dict_from_response(llm_response.content)

        # Validate the dictionary before assuming presence of any keys
        valid, errors = validate_dict(assistant_reply_dict, self.config)

        if not valid:
            raise SyntaxError(
                ";\n".join([str(e) for e in errors])
            )

        command_dict = assistant_reply_dict["command"]

        with open("agent_config_and_prompt_files/commands_interface.json") as cif:
            commands_interface = json.load(cif)

        if command_dict.get("name", "") in list(commands_interface.keys()):
            ref_args = commands_interface[command_dict["name"]]
            command_args = list(command_dict["args"].keys())
            new_command_dict = {"name": command_dict["name"], "args": {}}
            for k in command_args:
                if k in ref_args:
                    new_command_dict["args"][k] = command_dict["args"][k]

            unmatched_args = [
                arg for arg in command_args if arg not in ref_args]
            unmatched_ref = [arg for arg in ref_args if arg not in list(
                new_command_dict["args"].keys())]

            for uarg in unmatched_args:
                for uref in unmatched_ref:
                    if uarg in uref:
                        new_command_dict["args"][uref] = command_dict["args"][uarg]
                        break

            assistant_reply_dict["command"] = new_command_dict

        for plugin in self.config.plugins:
            if not plugin.can_handle_post_planning():
                continue
            assistant_reply_dict = plugin.post_planning(assistant_reply_dict)

        response = None, None, assistant_reply_dict

        if assistant_reply_dict != {}:
            # Get command name and arguments
            try:
                command_name, arguments = extract_command(
                    assistant_reply_dict, llm_response, self.config
                )
                response = command_name, arguments, assistant_reply_dict
            except Exception as e:
                logger.error("Error: \n", str(e))

        return response


def extract_command(
    assistant_reply_json: dict, assistant_reply: ChatModelResponse, config: Config
) -> tuple[str, dict[str, str]]:
    """Parse the response and return the command name and arguments

    Args:
        assistant_reply_json (dict): The response object from the AI
        assistant_reply (ChatModelResponse): The model response from the AI
        config (Config): The config object

    Returns:
        tuple: The command name and arguments

    Raises:
        json.decoder.JSONDecodeError: If the response is not valid JSON

        Exception: If any other error occurs
    """
    if config.openai_functions:
        if assistant_reply.function_call is None:
            return "Error:", {"message": "No 'function_call' in assistant reply"}
        assistant_reply_json["command"] = {
            "name": assistant_reply.function_call.name,
            "args": json.loads(assistant_reply.function_call.arguments),
        }
    try:
        if "command" not in assistant_reply_json:
            return "Error:", {"message": "Missing 'command' object in JSON"}

        if not isinstance(assistant_reply_json, dict):
            return (
                "Error:",
                {
                    "message": f"The message was not a dictionary {assistant_reply_json}"
                },
            )

        command = assistant_reply_json["command"]
        if not isinstance(command, dict):
            return "Error:", {"message": "'command' object is not a dictionary"}

        if "name" not in command:
            return "Error:", {"message": "Missing 'name' field in 'command' object"}

        command_name = command["name"]

        # Use an empty dictionary if 'args' field is not present in 'command' object
        arguments = command.get("args", {})

        return command_name, arguments
    except json.decoder.JSONDecodeError:
        return "Error:", {"message": "Invalid JSON"}
    # All other errors, return "Error: + error message"
    except Exception as e:
        return "Error:", {"message": str(e)}


def execute_command(
    command_name: str,
    arguments: dict[str, str],
    agent: Agent,
) -> Any:
    """Execute the command and return the result

    Args:
        command_name (str): The name of the command to execute
        arguments (dict): The arguments for the command
        agent (Agent): The agent that is executing the command

    Returns:
        str: The result of the command
    """
    try:
        # Execute a native command with the same name or alias, if it exists
        if command := agent.command_registry.get_command(command_name):
            return command(**arguments, agent=agent)

        # Handle non-native commands (e.g. from plugins)
        for command in agent.ai_config.prompt_generator.commands:
            if (
                command_name == command.label.lower()
                or command_name == command.name.lower()
            ):
                return command.function(**arguments)

        # The command_name was unknown. So add it to the list of unknown commands.

        agent.unknown_commands.append(str(command_name))
        raise RuntimeError(
            f"Cannot execute '{command_name}': unknown command."
            " Do not try to use this command again."
        )
    except Exception as e:
        return f"Error: {str(e)}"

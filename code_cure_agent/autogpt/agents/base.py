from __future__ import annotations

from abc import ABCMeta, abstractmethod
from typing import TYPE_CHECKING, Any, Literal, Optional
from colorama import Fore
import json
import os

if TYPE_CHECKING:
    from autogpt.config import AIConfig, Config

    from autogpt.models.command_registry import CommandRegistry

from autogpt.llm.base import ChatModelResponse, ChatSequence, Message
from autogpt.llm.providers.openai import OPEN_AI_CHAT_MODELS, get_openai_command_specs
from autogpt.llm.utils import count_message_tokens, create_chat_completion
from autogpt.logs import logger
from autogpt.memory.message_history import MessageHistory
from autogpt.prompts.prompt import DEFAULT_TRIGGERING_PROMPT
from autogpt.utils.json_utils.json_utilities import extract_dict_from_response

CommandName = str
CommandArgs = dict[str, str]
AgentThoughts = dict[str, Any]


class BaseAgent(metaclass=ABCMeta):
    """Base class for all Auto-GPT agents."""

    ThoughtProcessID = Literal["one-shot"]

    def __init__(
        self,
        ai_config: AIConfig,
        command_registry: CommandRegistry,
        config: Config,
        big_brain: bool = True,
        default_cycle_instruction: str = DEFAULT_TRIGGERING_PROMPT,
        cycle_budget: Optional[int] = 1,
        send_token_limit: Optional[int] = None,
        summary_max_tlength: Optional[int] = None,
        experiment_file: str = None
    ):
        self.experiment_file = experiment_file
        self.ai_config = ai_config
        """The AIConfig or "personality" object associated with this agent."""

        self.command_registry = command_registry
        """The registry containing all commands available to the agent."""

        self.config = config
        """The applicable application configuration."""

        self.big_brain = big_brain
        """
        Whether this agent uses the configured smart LLM (default) to think,
        as opposed to the configured fast LLM.
        """

        self.default_cycle_instruction = default_cycle_instruction
        """The default instruction passed to the AI for a thinking cycle."""

        self.cycle_budget = cycle_budget
        """
        The number of cycles that the agent is allowed to run unsupervised.

        `None` for unlimited continuous execution,
        `1` to require user approval for every step,
        `0` to stop the agent.
        """

        self.cycle_count = 0
        """The number of cycles that the agent has run since its initialization."""

        with open(experiment_file) as hper:
            self.hyperparams = json.load(hper)

        # Overwrite the continuos_limit with the commands_limit specified in hyperparams.json
        self.config.continuous_limit = self.hyperparams["commands_limit"]
        logger.typewriter_log(
            "Continuous Limit: ", Fore.GREEN, f"{self.config.continuous_limit}"
        )

        with open("agent_config_and_prompt_files/commands_by_state.json") as cbs:
            self.cmds_by_state = json.load(cbs)

        with open("agent_config_and_prompt_files/states_description.json") as sdj:
            self.descriptions = json.load(sdj)

        with open("sonarqube_quality_profile/quality_profile_rule_keys.txt") as rule_keys_file:
            self.sonar_qube_rules_in_active_profile = rule_keys_file.read().split(",")

        # Change this to the initial state if a state machine is to be used.
        # Also need to add info about the states in "prepare_ai_settings.py"

        # By setting to no_state_machine, no state machine will
        # be used but only a single state with all commands
        self.current_state = "no_state_machine"

        self.prompt_dictionary = ai_config.construct_full_prompt(config)

        self.prompt_dictionary["commands"][2] = self.cmds_by_state[self.current_state]
        self.prompt_dictionary["current state"] = self.descriptions[self.current_state]

        llm_name = self.config.smart_llm if self.big_brain else self.config.fast_llm
        self.llm = OPEN_AI_CHAT_MODELS[llm_name]
        """The LLM that the agent uses to think."""

        self.send_token_limit = send_token_limit or self.llm.max_tokens * 3 / 4
        """
        The token limit for prompt construction. Should leave room for the completion;
        defaults to 75% of `llm.max_tokens`.
        """

        # Number of characters that command responses are truncated to.
        # This is a safe bound to ensure not exceeding the 128000 token GPT-4o context window
        self.truncation_limit = 12500

        self.history = MessageHistory(
            self.llm,
            max_summary_tlength=summary_max_tlength or self.send_token_limit // 6,
        )

        self.plans = []

        self.unknown_commands = []

        self.write_fix_attempts = 0

        # Holds all the initial analysis reports of relevant files, with no changes to the project
        self.initial_analysis_reports = {

        }

        experiments_list = "experimental_setups/experiments_list.txt"

        # Create experiments_list.txt if not yet created
        if not os.path.isfile(experiments_list):
            try:
                with open(experiments_list, "x"):
                    pass
            except FileExistsError:
                pass

        with open(experiments_list) as eht:
            self.exps = eht.read().splitlines()

    def save_context(self,):
        context = {
            "cycle_budget": self.cycle_budget,
            "cycle_count": self.cycle_count,
            "current_state": self.current_state,
            "prompt_dictionary": self.prompt_dictionary,
            "plans": self.plans,
            "unknown_commands": self.unknown_commands,
            "write_fix_attempts": self.write_fix_attempts,
            "experiment_file": self.experiment_file,
            "hyperparams": self.hyperparams,
            "history": [msg for _, msg in enumerate(self.history)],
            "initial_analysis_reports": self.initial_analysis_reports
        }

        # with open("experimental_setups/experiments_list.txt") as eht:
        exps = self.exps

        sanitized_warning_file_path = self.ai_config.warning_file_path.replace(
            "/", ".")
        with open(os.path.join("experimental_setups", exps[-1], "saved_contexts", f"saved_context_{str(self.ai_config.warning_ID)}_{self.ai_config.warning_repository_name}_{self.ai_config.warning_rule_key}_{sanitized_warning_file_path}_line_{str(self.ai_config.warning_start_line)}"), "w") as patf:
            json.dump(context, patf)

    def load_context(self):
        exps = self.exps
        sanitized_warning_file_path = self.ai_config.warning_file_path.replace(
            "/", ".")
        with open(os.path.join("experimental_setups", exps[-1], "saved_contexts", f"saved_context_{str(self.ai_config.warning_ID)}_{self.ai_config.warning_repository_name}_{self.ai_config.warning_rule_key}_{sanitized_warning_file_path}_line_{str(self.ai_config.warning_start_line)}"), "r") as patf:
            context = json.load(patf)

        self.cycle_budget = context["cycle_budget"]
        self.cycle_count = context["cycle_count"]
        self.current_state = context["current_state"]
        self.prompt_dictionary = context["prompt_dictionary"]
        self.plans = context["plans"]
        self.unknown_commands = context["unknown_commands"]
        self.write_fix_attempts = context["write_fix_attempts"]
        self.experiment_file = context["experiment_file"]
        self.hyperparams = context["hyperparams"]
        self.history = context["history"]
        self.initial_analysis_reports = context["initial_analysis_reports"]

    def think(
        self,
        instruction: Optional[str] = None,
        thought_process_id: ThoughtProcessID = "one-shot",
    ) -> tuple[CommandName | None, CommandArgs | None, AgentThoughts]:
        """Runs the agent for one cycle.

        Params:
            instruction: The instruction to put at the end of the prompt.

        Returns:
            The command name and arguments, if any, and the agent's thoughts.
        """

        instruction = instruction or self.default_cycle_instruction

        prompt: ChatSequence = self.construct_prompt(
            instruction, thought_process_id)
        prompt = self.on_before_think(prompt, thought_process_id, instruction)

        # Save prompts at each step
        sanitized_warning_file_path = self.ai_config.warning_file_path.replace(
            "/", ".")
        with open(os.path.join("experimental_setups", self.exps[-1], "prompt_history", f"{str(self.ai_config.warning_ID)}_{self.ai_config.warning_repository_name}_{self.ai_config.warning_rule_key}_{sanitized_warning_file_path}_line_{str(self.ai_config.warning_start_line)}_prompt_history"), "a+") as patf:
            patf.write(prompt.dump())

        sanitized_warning_file_path = self.ai_config.warning_file_path.replace(
            "/", ".")
        with open(os.path.join("experimental_setups", self.exps[-1], "all_messages", f"{str(self.ai_config.warning_ID)}_{self.ai_config.warning_repository_name}_{self.ai_config.warning_rule_key}_{sanitized_warning_file_path}_line_{str(self.ai_config.warning_start_line)}_all_messages"), "w") as patf:
            patf.write(self.history.dump())

        raw_response = create_chat_completion(
            prompt,
            self.config,
            functions=get_openai_command_specs(self.command_registry)
            if self.config.openai_functions
            else None,
        )

        self.cycle_count += 1

        if self.hyperparams["repetition_handling"] != "NONE":
            try:
                response_dict = extract_dict_from_response(
                    raw_response.content
                )
                repetition = self.detect_command_repetition(response_dict)
                if repetition:
                    logger.info("WARNING: REPETITION DETECTED!\n")
                    logger.info(str(self.handle_command_repitition(
                        response_dict, self.hyperparams["repetition_handling"])) + "\n\n")
                    # A separate message is added to the prompt, then the prompt is executed again with the added message
                    prompt.extend([Message("user", self.handle_command_repitition(
                        response_dict, self.hyperparams["repetition_handling"]))])
                    new_response = create_chat_completion(
                        prompt,
                        self.config,
                        functions=get_openai_command_specs(
                            self.command_registry)
                        if self.config.openai_functions
                        else None,
                    )
                    if self.hyperparams["repetition_handling"] == "TOP3":
                        top3_list = json.loads(new_response.content)
                        for r in top3_list:
                            repetition = self.detect_command_repetition(r)
                            if not repetition:
                                raw_response = Message("assistant", str(r))
                    elif self.hyperparams["repetition_handling"] == "RESTRICT":
                        raw_response = new_response

            except SyntaxError as e:
                logger.error("Error in repetition handling: " + e.msg)

        return self.on_response(raw_response, thought_process_id, prompt, instruction)

    def construct_prompt(
        self,
        cycle_instruction: str,
        thought_process_id: ThoughtProcessID,
    ) -> ChatSequence:
        """Constructs and returns a prompt with the following structure:
        1. System prompt
        2. Message history of the agent, truncated & prepended with running summary as needed
        3. `cycle_instruction`

        Params:
            cycle_instruction: The final instruction for a thinking cycle
        """

        if not cycle_instruction:
            raise ValueError("No instruction given")

        cycle_instruction_tlength = 0

        append_messages: list[Message] = []

        prompt = self.construct_base_prompt(
            thought_process_id,
            append_messages=append_messages,
            reserve_tokens=cycle_instruction_tlength,
        )

        return prompt

    def construct_base_prompt(
        self,
        thought_process_id: ThoughtProcessID,
        prepend_messages: list[Message] = [],
        append_messages: list[Message] = [],
        reserve_tokens: int = 0,
    ) -> ChatSequence:
        """Constructs and returns a prompt with the following structure:
        1. System prompt
        2. `prepend_messages`
        3. Message history of the agent, truncated & prepended with running summary as needed
        4. `append_messages`

        Params:
            prepend_messages: Messages to insert between the system prompt and message history
            append_messages: Messages to insert after the message history
            reserve_tokens: Number of tokens to reserve for content that is added later
        """

        # self.save_context()

        with open("agent_config_and_prompt_files/cycle_instruction_text.md") as cit:
            cycle_instruction = cit.read()

        if self.hyperparams["budget_control"]["name"] == "NO-TRACK":
            pass
        elif self.hyperparams["budget_control"]["name"] == "FULL-TRACK" and self.hyperparams["budget_control"]["params"] == {}:
            cycle_instruction += "\nYou have, so far, executed {} commands, you have only {} commands left.\n".format(
                self.cycle_count, self.hyperparams["commands_limit"]-self.cycle_count)
        elif self.hyperparams["budget_control"]["name"] == "FULL-TRACK" and self.hyperparams["budget_control"]["params"] != {}:
            minimum_number_fixes = self.hyperparams["budget_control"]["params"]["#fixes"]
            cycle_instruction += "\nYou have, so far, executed, {} commands and suggested {} fixes. You have {} commands left. However, you need to suggest at least {} fixes before consuming all the left commands.\n".format(
                self.cycle_count, self.write_fix_attempts, self.hyperparams["commands_limit"] - self.cycle_count, max(minimum_number_fixes - self.write_fix_attempts, 0))
        elif self.hyperparams["budget_control"]["name"] == "FORCED" and self.current_state != "no_state_machine":
            t1 = self.hyperparams["budget_control"]["T1"]
            t2 = self.hyperparams["budget_control"]["T2"]
            if self.cycle_count >= t2:
                self.update_prompt_state("Trying out Fix Candidates")
                cycle_instruction += "\nBecause of budget constaints, you were forced to transition to the state `Trying out Fix Candidates`"
            elif self.cycle_count >= t1:
                self.update_prompt_state("Gathering Context for a Fix")
                cycle_instruction += "\nBecause of budget constaints, you were forced to transition to the state `Gathering Context for a Fix`"

        context_prompt = self.construct_context_prompt()
        prompt = ChatSequence.for_model(
            self.llm.name,
            [Message("system", self.prompt_dictionary["role"])])

        definitions_prompt = ""
        static_sections_names = ["goals", "commands", "general guidelines"]
        if self.current_state != "no_state_machine":
            static_sections_names.append("current state")
        if self.current_state in ["no_state_machine", "Gathering Context for a Fix", "Trying out Fix Candidates"]:
            static_sections_names.append("fix format")
        for key in static_sections_names:
            if isinstance(self.prompt_dictionary[key], list):
                definitions_prompt += "\n".join(
                    self.prompt_dictionary[key]) + "\n\n"
            elif isinstance(self.prompt_dictionary[key], str):
                definitions_prompt += self.prompt_dictionary[key] + "\n\n"
            else:
                raise TypeError("For now we only support list and str types.")

        prompt.extend(ChatSequence.for_model(
            self.llm.name,
            [Message("user", definitions_prompt + "\n" + context_prompt +
                     "\n\n" + cycle_instruction)] + prepend_messages,
        ))

        return prompt

    # Methods for creating the relevant prompt contexts

    def construct_task_context(self):
        with open("agent_config_and_prompt_files/task_section.md") as task_section_file:
            task_section = task_section_file.read().format(project_name=self.ai_config.warning_repository_name, file_path=self.ai_config.warning_file_path, rule_key=self.ai_config.warning_rule_key,
                                                           rule_name=self.ai_config.warning_rule_name, warning_start_line=self.ai_config.warning_start_line, warning_specific_message=self.ai_config.warning_specific_message)
        return task_section

    def construct_plan_context(self) -> str:
        plan_section = "## Your current plan for approaching the task\n\n"
        if self.plans:
            plan_section += self.plans[-1]
        else:
            plan_section += "No plan made yet."

        return plan_section

    def construct_agent_history_context(self) -> str:

        history_section = ""

        with open("agent_config_and_prompt_files/agent_history_preamble.md") as history_section_file:
            history_section = history_section_file.read()

        cycle = 0
        for message in self.history:
            if message.type == "ai_response":
                cycle += 1

                # Add thoughts
                history_section += f"\n\n### Step {cycle}\n\nYour thoughts:  \n"
                if message.agent_thoughts is None:
                    history_section += "`No thoughts given.`"
                else:
                    history_section += message.agent_thoughts

                # Add command call information
                history_section += self.construct_command_call_history_subsection(
                    message)

            # Add command call result
            # We expect the "action_result" type method to always follow its predecessor "ai_response"
            if message.type == "action_result":
                history_section += "\n\n" + message.content

        if cycle == 0:
            history_section += "\n\nNo steps taken yet."

        return history_section

    def construct_command_call_history_subsection(self, message: Message) -> str:
        command_call_subsection = "\n\nCalled command:  \n"

        # Handle exceptional commands
        if message.command is None:
            command_call_subsection += "The command was missing."
        elif message.command.lower() == "error_when_parsing":
            command_call_subsection += "Command could not be parsed."
        elif message.command.lower().startswith("error"):
            command_call_subsection += f"Could not execute command: {message.command}{str(message.args)}"
        else:
            with open("agent_config_and_prompt_files/commands_interface.json") as cif:
                commands_interface = json.load(cif)

            if message.command in list(commands_interface.keys()):
                command_call_subsection += self.format_command_information(
                    message)
            else:
                command_call_subsection += f"Unknown command named: {message.command}"

        return command_call_subsection

    # TODO: potentially create dedicated formatings for each command
    def format_command_information(self, message: Message) -> str:
        command_information = f"`{message.command}` with arguments "
        if message.args is None:
            command_information += "`none`"
        else:
            for key, value in message.args.items():
                command_information += f"`{key}`: `{str(value)}`; "
        return command_information

    def construct_forbidden_commands_context(self) -> str:
        forbidden_commands_section = ""
        if self.unknown_commands:
            forbidden_commands_section = "## Forbidden Commands\n\nDO NOT ATTEMPT TO CALL ANY OF THE FOLLOWING COMMANDS UNDER ANY CIRCUMSTANCES:  \n" + \
                "  \n".join(self.unknown_commands)
        return forbidden_commands_section

    def construct_context_prompt(self) -> str:
        '''Constructs the context parts in the prompt including task description and history.
        Returns:
            str: The context prompt string
        '''
        task_section = self.construct_task_context()
        plan_section = self.construct_plan_context()
        agent_history_section = self.construct_agent_history_context()
        forbidden_commands_section = self.construct_forbidden_commands_context()

        # Join the different parts together with a space inbetween. If one of the sections is None or an empty string then it is ignored.
        return "\n\n".join(filter(None, [task_section, plan_section, agent_history_section, forbidden_commands_section]))

    def on_before_think(
        self,
        prompt: ChatSequence,
        thought_process_id: ThoughtProcessID,
        instruction: str,
    ) -> ChatSequence:
        """Called after constructing the prompt but before executing it.

        Calls the `on_planning` hook of any enabled and capable plugins, adding their
        output to the prompt.

        Params:
            instruction: The instruction for the current cycle, also used in constructing the prompt

        Returns:
            The prompt to execute
        """
        current_tokens_used = prompt.token_length
        plugin_count = len(self.config.plugins)
        for i, plugin in enumerate(self.config.plugins):
            if not plugin.can_handle_on_planning():
                continue
            plugin_response = plugin.on_planning(
                self.ai_config.prompt_generator, prompt.raw()
            )
            if not plugin_response or plugin_response == "":
                continue
            message_to_add = Message("system", plugin_response)
            tokens_to_add = count_message_tokens(message_to_add, self.llm.name)
            if current_tokens_used + tokens_to_add > self.send_token_limit:
                logger.debug(
                    f"Plugin response too long, skipping: {plugin_response}")
                logger.debug(f"Plugins remaining at stop: {plugin_count - i}")
                break
            prompt.insert(
                -1, message_to_add
            )  # HACK: assumes cycle instruction to be at the end
            current_tokens_used += tokens_to_add
        return prompt

    def on_response(
        self,
        llm_response: ChatModelResponse,
        thought_process_id: ThoughtProcessID,
        prompt: ChatSequence,
        instruction: str,
    ) -> tuple[CommandName | None, CommandArgs | None, AgentThoughts]:
        """Called upon receiving a response from the chat model.

        Adds the messages in the prompt and the response to `history`,
        and calls `self.parse_and_process_response()` to do the rest.

        Params:
            llm_response: The raw response from the chat model
            prompt: The prompt that was executed
            instruction: The instruction for the current cycle, also used in constructing the prompt

        Returns:
            The parsed command name and command args, if any, and the agent thoughts.
        """

        # Save all parts of the prompt to message history
        for msg in prompt:
            self.history.append(msg)

        try:
            command_name, command_args, assistant_reply_dict = self.parse_and_process_response(
                llm_response, thought_process_id, prompt, instruction
            )
        except SyntaxError as e:
            logger.error(f"Response could not be parsed: {e}")
            with open(f"experimental_setups/{self.exps[-1]}/parsing_erros_responses.txt", "a") as pers:
                pers.write(llm_response.content+"\n")

            command_name, command_args, assistant_reply_dict = "error_when_parsing", {"error": "Your response could not be parsed."
                                                                                      f"\nTrying to parse the response failed with the following error message: {e}"
                                                                                      "\n\nRemember to only respond using the specified json schema!"}, {}

        # Save assistant reply to message history
        response_message = Message(
            "assistant", llm_response.content, "ai_response", command=command_name, args=command_args, agent_thoughts=assistant_reply_dict.get("thoughts", "No thoughts given."))
        self.history.append(response_message)

        return command_name, command_args, assistant_reply_dict

    @abstractmethod
    def execute(
        self,
        command_name: str | None,
        command_args: dict[str, str] | None,
        user_input: str | None,
    ) -> str:
        """Executes the given command, if any, and returns the agent's response.

        Params:
            command_name: The name of the command to execute, if any.
            command_args: The arguments to pass to the command, if any.
            user_input: The user's input, if any.

        Returns:
            The results of the command.
        """
        ...

    @abstractmethod
    def parse_and_process_response(
        self,
        llm_response: ChatModelResponse,
        thought_process_id: ThoughtProcessID,
        prompt: ChatSequence,
        instruction: str,
    ) -> tuple[CommandName | None, CommandArgs | None, AgentThoughts]:
        """Validate, parse & process the LLM's response.

        Must be implemented by derivative classes: no base implementation is provided,
        since the implementation depends on the role of the derivative Agent.

        Params:
            llm_response: The raw response from the chat model
            prompt: The prompt that was executed
            instruction: The instruction for the current cycle, also used in constructing the prompt

        Returns:
            The parsed command name and command args, if any, and the agent thoughts.
        """
        pass

    def detect_command_repetition(self, ref_cmd):

        assistant_outputs = [{"name": msg.command, "args": msg.args} for msg in self.history if msg.type == "ai_response" and not msg.command.lower(
        ).startswith("error")]
        if isinstance(ref_cmd, dict) and "command" in ref_cmd.keys():
            if ref_cmd["command"] in assistant_outputs:
                logger.info("WARNING: REPETITION DETECTED!\n\n")
                return True
        return False

    def handle_command_repitition(self, repeated_command: dict, handling_strategy: str = ""):
        if handling_strategy == "":
            return ""
        elif handling_strategy == "RESTRICT":
            return "Your next command should be totally different from this command: {}".format(repeated_command["command"])
        elif handling_strategy == "TOP3":
            return "Suggest three commands that would make sense to execute given your current input. Give the full json object of each command with all attributes, put the three commands in a list, i.e, [{...}, {...}, {...}]. Do not add any text explanataion before or after the list of the three commands."
        else:
            raise ValueError(
                "The value given to the param handling_strategy is unsuported: {}".format(handling_strategy))

    def update_prompt_state(self, state_name):
        """
        Given a state name, this function would update the prompt dictionary to include the right description of the state 
        and also the corresponding set of commands.
        """
        self.prompt_dictionary["current state"] = self.descriptions[state_name]
        self.prompt_dictionary["commands"][2] = self.cmds_by_state[state_name]
        self.current_state = state_name

from __future__ import annotations

from abc import ABCMeta, abstractmethod
import time
from typing import TYPE_CHECKING, Any, Literal, Optional
from colorama import Fore
import json
import os

if TYPE_CHECKING:
    from agent_core.config import AIConfig, Config
    from agent_core.app.main import shutdown
    from agent_core.models.command_registry import CommandRegistry

from agent_core.llm.base import ChatModelResponse, ChatSequence, Message
from agent_core.llm.providers.openai import OPEN_AI_CHAT_MODELS, get_openai_command_specs
from agent_core.llm.utils import count_message_tokens, create_chat_completion
from agent_core.logs import logger
from agent_core.memory.message_history import MessageHistory
from agent_core.utils.json_utils.json_utilities import extract_dict_from_response
from agent_core.utils.file_operation_utils.read_file import read_file

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
        send_token_limit: Optional[int] = None,
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

        self.cycle_count = 0
        """The number of cycles that the (sub-)agent has run since its initialization."""

        # Initializes the logger with the agent object.
        logger.agent = self

        with open(experiment_file) as hper:
            self.hyperparams = json.load(hper)

        # Set the commands_limit to the classification_commands_limit specified in hyperparams.json
        self.config.commands_limit = self.hyperparams["classification_commands_limit"]
        logger.info(
            title="Commands Limit Classification: ", title_color=Fore.GREEN, message=f"{self.config.commands_limit}"
        )

        with open("agent_config_and_prompt_files/commands_by_state.json") as cbs:
            self.cmds_by_state = json.load(cbs)

        with open("sonarqube_quality_profile/quality_profile_rule_keys.txt") as rule_keys_file:
            self.sonar_qube_rules_in_active_profile = rule_keys_file.read().split(",")

        # Can be "classification", "fix_tp", or "fix_fp"
        # Initially it is "classification"
        self.current_state = "classification"

        # We have questions 1 to 3 for the classification
        self.current_question = 1
        self.question_answers = []
        self.number_of_questions = 3

        self.final_verdict_reason = ""

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
            self.llm
        )

        self.plans = []

        self.unknown_commands = []

        self.write_fix_attempts = 0
        self.plausible_fixes = 0

        # Holds all the initial analysis reports of relevant files, with no changes to the project
        self.initial_analysis_reports = {

        }

        self.lsp_server_initialized = False

        # Experiments_list is expected to exist, because "increment_experiment.py" creates it if it doesn't exist.
        experiments_list = "experimental_setups/experiments_list.txt"

        with open(experiments_list) as eht:
            self.exps = eht.read().splitlines()

    def think(self) -> tuple[CommandName | None, CommandArgs | None, AgentThoughts]:
        """Runs the agent for one cycle.

        Params:
            instruction: The instruction to put at the end of the prompt.

        Returns:
            The command name and arguments, if any, and the agent's thoughts.
        """

        prompt: ChatSequence = self.construct_prompt()
        prompt = self.on_before_think(prompt)

        # Save prompts at each step
        sanitized_warning_file_path = self.ai_config.warning_file_path.replace(
            "/", ".")
        with open(os.path.join("experimental_setups", self.exps[-1], self.current_state, "prompt_history", f"{str(self.ai_config.warning_ID)}_{self.ai_config.warning_repository_name}_{self.ai_config.warning_rule_key}_{sanitized_warning_file_path}_line_{str(self.ai_config.warning_start_line)}_prompt_history"), "a+") as patf:
            patf.write(prompt.dump())

        sanitized_warning_file_path = self.ai_config.warning_file_path.replace(
            "/", ".")
        with open(os.path.join("experimental_setups", self.exps[-1], self.current_state, "all_messages", f"{str(self.ai_config.warning_ID)}_{self.ai_config.warning_repository_name}_{self.ai_config.warning_rule_key}_{sanitized_warning_file_path}_line_{str(self.ai_config.warning_start_line)}_all_messages"), "w") as patf:
            patf.write(self.history.dump())

        raw_response = create_chat_completion(
            prompt,
            self.config,
            functions=get_openai_command_specs(self.command_registry)
            if self.config.openai_functions
            else None,
            agent=self
        )

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
                        agent=self
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

        return self.on_response(raw_response, prompt)

    def construct_prompt(
        self,
    ) -> ChatSequence:
        """Constructs and returns a prompt.
        """

        prompt_content = self.create_prompt_content()

        prompt = ChatSequence.for_model(
            self.llm.name,
            [Message("user", prompt_content)])

        return prompt

    def create_prompt_content(self) -> str:
        """
        Creates a different prompt depending on the state.
        """
        if self.current_state == "classification":
            return self.create_prompt_classification()
        elif self.current_state == "fix_tp":
            return self.create_prompt_fix()
        elif self.current_state == "fix_fp":
            return self.create_prompt_suppress()
        else:
            logger.error(f"Transitioned to unknown state '{self.current_state}' during execution.",
                         "Only allowed are: 'classification', 'fix_tp' and 'fix_fp'.")
            shutdown(self, 1)

    def create_prompt_classification(self) -> str:
        """
        Creates all the parts of the classification task prompt
        """

        problem_introduction = read_file(
            "agent_config_and_prompt_files/classification_prompt_files/classification_problem_introduction.md")

        commands = self.construct_commands_context()

        general_guidelines = read_file(
            "agent_config_and_prompt_files/classification_prompt_files/classification_general_guidelines.md")
        task = read_file(
            "agent_config_and_prompt_files/classification_prompt_files/classification_task.md").format(project_name=self.ai_config.warning_repository_name, file_path=self.ai_config.warning_file_path, rule_key=self.ai_config.warning_rule_key,
                                                                                                       rule_name=self.ai_config.warning_rule_name, warning_start_line=self.ai_config.warning_start_line, warning_specific_message=self.ai_config.warning_specific_message)
        agent_history = self.construct_agent_history_context()
        forbidden_commands_section = self.construct_forbidden_commands_context()
        question_and_answer_section = self.construct_question_and_answer_context()
        cycle_instruction_section = self.construct_cycle_instruction_classification()

        # Join the different parts together with a space inbetween. If one of the sections is None or an empty string then it is ignored.
        return "\n\n".join(filter(None, [problem_introduction, commands, general_guidelines, task, agent_history, forbidden_commands_section, question_and_answer_section, cycle_instruction_section]))

    def create_prompt_fix(self) -> str:
        """
        Creates all the parts of the fix task prompt
        """

        # TODO: add classification info and reason to the fix_tp prompt

        problem_introduction = read_file(
            "agent_config_and_prompt_files/fix_violation_prompt_parts/fix_problem_introduction.md")

        commands = self.construct_commands_context()

        fix_format_section = read_file(
            "agent_config_and_prompt_files/fix_violation_prompt_parts/fix_format.md")
        general_guidelines_section = read_file(
            "agent_config_and_prompt_files/fix_violation_prompt_parts/fix_general_guidelines.md")
        task_section = read_file(
            "agent_config_and_prompt_files/fix_violation_prompt_parts/fix_task_section.md").format(project_name=self.ai_config.warning_repository_name, file_path=self.ai_config.warning_file_path, rule_key=self.ai_config.warning_rule_key,
                                                                                                   rule_name=self.ai_config.warning_rule_name, warning_start_line=self.ai_config.warning_start_line, warning_specific_message=self.ai_config.warning_specific_message)
        plan_section = self.construct_plan_context()
        agent_history_section = self.construct_agent_history_context()
        forbidden_commands_section = self.construct_forbidden_commands_context()
        cycle_instruction_section = self.construct_cycle_instruction_fix_or_suppress()

        # Join the different parts together with a space inbetween. If one of the sections is None or an empty string then it is ignored.
        return "\n\n".join(filter(None, [problem_introduction, commands, fix_format_section, general_guidelines_section, task_section, plan_section, agent_history_section, forbidden_commands_section, cycle_instruction_section]))

    def create_prompt_suppress(self) -> str:
        """
        Creates all the parts of the suppress task prompt
        """

        problem_introduction = read_file(
            "agent_config_and_prompt_files/suppress_violation_prompt_parts/suppress_problem_introduction.md")

        commands = self.construct_commands_context()

        fix_format_section = read_file(
            "agent_config_and_prompt_files/suppress_violation_prompt_parts/suppress_fix_format.md")
        general_guidelines_section = read_file(
            "agent_config_and_prompt_files/suppress_violation_prompt_parts/suppress_general_guidelines.md")
        task_section = read_file(
            "agent_config_and_prompt_files/suppress_violation_prompt_parts/suppress_task.md").format(project_name=self.ai_config.warning_repository_name, file_path=self.ai_config.warning_file_path, rule_key=self.ai_config.warning_rule_key,
                                                                                                     rule_name=self.ai_config.warning_rule_name, warning_start_line=self.ai_config.warning_start_line, warning_specific_message=self.ai_config.warning_specific_message)
        classification_explanation_section = self.construct_classification_explanation_for_fp_context()
        agent_history_section = self.construct_agent_history_context()
        forbidden_commands_section = self.construct_forbidden_commands_context()
        cycle_instruction_section = self.construct_cycle_instruction_fix_or_suppress()

        # Join the different parts together with a space inbetween. If one of the sections is None or an empty string then it is ignored.
        return "\n\n".join(filter(None, [problem_introduction, commands, fix_format_section, general_guidelines_section, task_section, classification_explanation_section, agent_history_section, forbidden_commands_section, cycle_instruction_section]))

    # Methods for creating the relevant prompt contexts

    def construct_commands_context(self) -> str:
        return "## Commands\n\nYou have access to the following commands (EXCLUSIVELY):\n\n" + \
            self.cmds_by_state[self.current_state]

    def construct_question_and_answer_context(self) -> str:
        """
        Used for the classification state. Creates the section of answered questions and the next question to answer (or final verdict to give).
        """

        answered_question_text = ""

        # Answered questions section
        if self.current_question > 1:
            answered_question_text = "## Answered Questions\n\n"

            for question_number in range(1, min(self.current_question, self.number_of_questions + 1)):
                with open(f"agent_config_and_prompt_files/classification_prompt_files/questions_prompt_parts/question_{str(question_number)}_text.md") as question_file:
                    question = question_file.readline()
                answered_question_text += f"Question {str(question_number)}: {question}Answer: {self.question_answers[question_number - 1]}  \n\n"

        # Next question/ final verdict section
        if self.current_question <= self.number_of_questions:
            with open(f"agent_config_and_prompt_files/classification_prompt_files/questions_prompt_parts/question_{str(self.current_question)}_text.md") as question_file:
                current_question_text = f"## Current Question to answer\n\nQuestion {str(self.current_question)}: " + \
                    question_file.read()
        else:
            with open("agent_config_and_prompt_files/classification_prompt_files/questions_prompt_parts/final_verdict_text.md") as verdict_file:
                current_question_text = "## Give a final verdict\n\n" + verdict_file.read()

        return answered_question_text + current_question_text

    def construct_cycle_instruction_fix_or_suppress(self) -> str:
        if self.current_state == "fix_tp":
            # If we have only 5 commands left use a different cycle instruction that forces towards using the write_fix command more often.
            cycles_left = self.config.commands_limit - self.cycle_count
            if cycles_left <= 5:
                cycle_instruction = read_file(
                    "agent_config_and_prompt_files/fix_violation_prompt_parts/fix_cycle_instruction_text_force_write_fix.md").format(cycles_left=str(cycles_left))
            else:
                cycle_instruction = read_file(
                    "agent_config_and_prompt_files/fix_violation_prompt_parts/fix_cycle_instruction_text.md")
        else:
            cycle_instruction = read_file(
                "agent_config_and_prompt_files/suppress_violation_prompt_parts/suppress_cycle_instruction_text.md")

        if self.hyperparams["budget_control"]["name"] == "NO-TRACK":
            pass
        elif self.hyperparams["budget_control"]["name"] == "FULL-TRACK" and self.hyperparams["budget_control"]["params"] == {}:
            cycle_instruction += "\nYou have, so far, executed {} commands, you have only {} commands left.\n".format(
                self.cycle_count, self.hyperparams["fix_commands_limit"] - self.cycle_count)
        elif self.hyperparams["budget_control"]["name"] == "FULL-TRACK" and self.hyperparams["budget_control"]["params"] != {}:
            minimum_number_fixes = self.hyperparams["budget_control"]["params"]["#fixes"]
            number_of_fixes_to_still_propose = max(
                minimum_number_fixes - self.write_fix_attempts, 0)
            if self.plausible_fixes > 0:
                number_of_fixes_to_still_propose = 0
            cycle_instruction += "\nYou have, so far, executed, {} commands and suggested {} fixes. You have {} commands left. However, you need to suggest at least {} fixes before consuming all the left commands.\n".format(
                self.cycle_count, self.write_fix_attempts, self.hyperparams["fix_commands_limit"] - self.cycle_count, number_of_fixes_to_still_propose)
        return cycle_instruction

    def construct_cycle_instruction_classification(self) -> str:
        # If we have only one command left use a different cycle instruction that forces towards calling the give_final_verdict command.
        if self.cycle_count == self.config.commands_limit - 1:
            cycle_instruction = read_file(
                "agent_config_and_prompt_files/classification_prompt_files/classification_cycle_instruction_text_force_final_verdict.md")
        # If we are in the first cycle, instruct to call the read_sonarqube_docu command, to collect some initial info about the rule (to reduce hallucination from the LLMs potentially wrong knowledge about the rule)
        elif self.cycle_count == 0:
            cycle_instruction = read_file(
                "agent_config_and_prompt_files/classification_prompt_files/classification_cycle_instruction_text_first_cycle_force_read_sonarqube_docu.md")
        else:
            cycle_instruction = read_file(
                "agent_config_and_prompt_files/classification_prompt_files/classification_cycle_instruction_text.md")

        if self.hyperparams["budget_control"]["name"] == "NO-TRACK":
            pass
        elif self.hyperparams["budget_control"]["name"] == "FULL-TRACK":
            cycle_instruction += "\nYou have so far executed {} commands. You have {} commands left and must give a final verdict before exhausting the commands.\n".format(
                self.cycle_count, self.hyperparams["classification_commands_limit"] - self.cycle_count)

        return cycle_instruction

    def construct_plan_context(self) -> str:
        plan_section = "## Your current plan for approaching the task\n\n"
        if self.plans:
            plan_section += self.plans[-1]
        else:
            plan_section += "No plan made yet."

        return plan_section

    def construct_classification_explanation_for_fp_context(self) -> str:
        return f"## Possible explanation why this needs to be suppressed (instead of fixed) (given by a LLM)\n\n{self.final_verdict_reason}"

    def construct_agent_history_context(self) -> str:

        history_section = read_file(
            "agent_config_and_prompt_files/agent_history_preamble.md")

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

    def on_before_think(
        self,
        prompt: ChatSequence
    ) -> ChatSequence:
        """Called after constructing the prompt but before executing it.

        Calls the `on_planning` hook of any enabled and capable plugins, adding their
        output to the prompt.

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
        prompt: ChatSequence,
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
                llm_response)
        except SyntaxError as e:
            logger.error(f"Response could not be parsed: {e}")
            with open(f"experimental_setups/{self.exps[-1]}/{self.current_state}/parsing_errors_responses.txt", "a") as pers:
                pers.write(llm_response.content + "\n")

            command_name, command_args, assistant_reply_dict = "error_when_parsing", {"error": "Your response could not be parsed."
                                                                                      f"\nTrying to parse the response failed with the following error message: {e}"
                                                                                      "\n\nRemember to only respond using the specified json schema!"}, {}

        # Save assistant reply to message history
        response_message = Message(
            "assistant", llm_response.content, "ai_response", command=command_name, args=command_args, agent_thoughts=assistant_reply_dict.get("thoughts", "No thoughts given."))
        self.history.append(response_message)

        sanitized_warning_file_path = self.ai_config.warning_file_path.replace(
            "/", ".")
        with open(os.path.join("experimental_setups", self.exps[-1], self.current_state, "responses", f"{str(self.ai_config.warning_ID)}_{self.ai_config.warning_repository_name}_{self.ai_config.warning_rule_key}_{sanitized_warning_file_path}_line_{str(self.ai_config.warning_start_line)}_model_responses"), "a+") as patf:
            patf.write(MessageHistory(
                self.llm, [response_message]).dump())

        return command_name, command_args, assistant_reply_dict

    @abstractmethod
    def execute(
        self,
        command_name: str | None,
        command_args: dict[str, str] | None,
        user_input: str | None,
    ) -> None:
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
        llm_response: ChatModelResponse
    ) -> tuple[CommandName | None, CommandArgs | None, AgentThoughts]:
        """Validate, parse & process the LLM's response.

        Must be implemented by derivative classes: no base implementation is provided,
        since the implementation depends on the role of the derivative Agent.

        Params:
            llm_response: The raw response from the chat model
            prompt: The prompt that was executed

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
            if "name" in repeated_command["command"] and repeated_command["command"]["name"] == "write_fix":

                return "You have already tried the following write_fix command before: '{}'  ".format(repeated_command["command"]) \
                    + "\n Try to change something in your write_fix to resolve the problems that led to rejection of the fix attempt."

            return "Your next command should be totally different from this command: {}".format(repeated_command["command"])

        elif handling_strategy == "TOP3":
            return "Suggest three commands that would make sense to execute given your current input. Give the full json object of each command with all attributes, put the three commands in a list, i.e, [{...}, {...}, {...}]. Do not add any text explanataion before or after the list of the three commands."
        else:
            raise ValueError(
                "The value given to the param handling_strategy is unsuported: {}".format(handling_strategy))

    def update_prompt_state(self, final_verdict_is_true_positive: bool):
        """
        Go to the next state of the agent. Either to fix_tp or fix_fp. 
        Prepare everything for the new state.
        """

        # Write the time of end of classification to the execution info file
        current_time = time.time_ns()
        sanitized_warning_file_path = self.ai_config.warning_file_path.replace(
            "/", ".")
        with open(os.path.join("experimental_setups", self.exps[-1], self.current_state, "execution_info", f"{str(self.ai_config.warning_ID)}_{self.ai_config.warning_repository_name}_{self.ai_config.warning_rule_key}_{sanitized_warning_file_path}_line_{str(self.ai_config.warning_start_line)}_execution_info"), "a+") as patf:
            patf.write("!! Shutdown timestamp: " + str(current_time) + "\n")

        if final_verdict_is_true_positive:
            self.current_state = "fix_tp"
            classification = "TP"
        else:
            self.current_state = "fix_fp"
            classification = "FP"

        # Write the time of start of fixing phase to the execution info file
        with open(os.path.join("experimental_setups", self.exps[-1], self.current_state, "execution_info", f"{str(self.ai_config.warning_ID)}_{self.ai_config.warning_repository_name}_{self.ai_config.warning_rule_key}_{sanitized_warning_file_path}_line_{str(self.ai_config.warning_start_line)}_execution_info"), "a+") as patf:
            patf.write("!! Start up timestamp: " + str(current_time) + "\n")

        # Reset the history
        self.history = MessageHistory(self.llm)

        # Reset the cycle count, limit and remaining cycles
        self.cycle_count = 0
        self.config.commands_limit = self.hyperparams["fix_commands_limit"]

        # Inform about state transition
        logger.info(title="Classification subtask is completed.",
                    title_color=Fore.GREEN, message="")
        logger.info(title="Classification result is: ",
                    title_color=Fore.GREEN, message=classification + "\n" + self.final_verdict_reason)
        logger.info(title=f"Transitioned to state {self.current_state}", title_color=Fore.GREEN,
                    message="All history is discarded. Agent will now tackle task of fixing (or suppressing) the violation.")
        logger.info(
            title="Commands Limit set for fixing violation to: ", title_color=Fore.GREEN, message=f"{self.config.commands_limit}"
        )
        logger.info(
            title="CodeCureAgent is now running its main fixing loop.", title_color=Fore.GREEN, message="")

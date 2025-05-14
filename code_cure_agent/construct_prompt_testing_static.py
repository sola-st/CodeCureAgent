from autogpt.agents.base import BaseAgent
import json

SORALD_JAR_PATH = "/workspaces/master-thesis-pascal-joos/code_cure_agent/sorald/sorald.jar"


class Config():
    def __init__(self, workspace_path):
        self.workspace_path = workspace_path
        self.sorald_jar_path = SORALD_JAR_PATH


class AIConfig():
    def __init__(self, warning_repository_URL, warning_repository_commit, warning_file_path, warning_repository_name, warning_rule_key, warning_start_line, warning_rule_name, warning_specific_message):
        self.warning_repository_URL = warning_repository_URL
        self.warning_repository_commit = warning_repository_commit
        self.warning_file_path = warning_file_path
        self.warning_rule_key = warning_rule_key
        self.warning_start_line = warning_start_line
        self.warning_rule_name = warning_rule_name
        self.warning_specific_message = warning_specific_message

        self.warning_repository_name = warning_repository_name


class Agent():
    def __init__(self, warning_repository_URL, warning_repository_commit, warning_file_path, warning_repository_name, warning_rule_key, warning_start_line, warning_rule_name, warning_specific_message, plans, unknown_commands):
        self.config = Config("auto_gpt_workspace/")
        self.ai_config = AIConfig(warning_repository_URL, warning_repository_commit, warning_file_path,
                                  warning_repository_name, warning_rule_key, warning_start_line, warning_rule_name, warning_specific_message)
        self.exps = ["experiment_test"]
        with open("/workspaces/master-thesis-pascal-joos/code_cure_agent/auto_gpt_workspace/argparse4j_S2142_main.src.main.java.net.sourceforge.argparse4j.internal.TerminalWidth.java_94_initial_analysis_report.json") as file:
            self.initial_analysis_report = json.load(file)

        self.plans = plans
        self.unknown_commands = unknown_commands

    def construct_task_context(self,):
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

    # TODO: implement the history
    def construct_agent_history_context(self) -> str:
        return ""

    def construct_forbidden_commands_context(self) -> str:
        forbidden_commands_section = ""
        if self.unknown_commands:
            forbidden_commands_section = "## Forbidden Commands\n\nDO NOT ATTEMPT TO CALL ANY OF THE FOLLOWING COMMANDS UNDER ANY CIRCUMSTANCES:  \n" + \
                "  \n".join(self.unknown_commands)
        return forbidden_commands_section

    def construct_context_prompt_code_cure_agent(self) -> str:
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


if __name__ == '__main__':
    warning_repository_URL = "https://github.com/argparse4j/argparse4j.git"
    warning_repository_commit = "MASTER"
    warning_repository_name = "argparse4j"
    warning_file_path = "main/src/main/java/net/sourceforge/argparse4j/internal/TerminalWidth.java"
    warning_rule_key = "S2142"
    warning_start_line = 94
    warning_rule_name = "'InterruptedException' should not be ignored"
    warning_specific_message = "Either re-interrupt this method or rethrow the ""InterruptedException"" that can be caught here."
    plans = ["Some plan"]
    unknown_commands = ["do_not_call", "not_either"]

    agent = Agent(warning_repository_URL, warning_repository_commit, warning_file_path, warning_repository_name,
                  warning_rule_key, warning_start_line, warning_rule_name, warning_specific_message, plans, unknown_commands)
    print(agent.construct_context_prompt_code_cure_agent())

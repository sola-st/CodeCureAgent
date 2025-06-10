
from agent_core.logs import logger
import logging

SORALD_JAR_PATH = "/workspaces/master-thesis-pascal-joos/code_cure_agent/sorald/sorald.jar"


class Config():
    def __init__(self, workspace_path):
        self.workspace_path = workspace_path
        self.sorald_jar_path = SORALD_JAR_PATH


class AIConfig():
    def __init__(self, warning_ID, warning_repository_URL, warning_repository_commit, warning_file_path, warning_repository_name, warning_rule_key, warning_start_line, warning_rule_name, warning_specific_message):
        self.warning_ID = warning_ID
        self.warning_repository_URL = warning_repository_URL
        self.warning_repository_commit = warning_repository_commit
        self.warning_file_path = warning_file_path
        self.warning_rule_key = warning_rule_key
        self.warning_start_line = warning_start_line
        self.warning_rule_name = warning_rule_name
        self.warning_specific_message = warning_specific_message

        self.warning_repository_name = warning_repository_name


class AgentMock():
    def __init__(self, warning_repository_URL, warning_repository_commit, warning_file_path, warning_repository_name, warning_rule_key, warning_start_line, warning_rule_name, warning_specific_message, current_state="no_state_machine", logger_level=logging.INFO):
        logger.set_level(logger_level)
        self.config = Config(
            "/workspaces/master-thesis-pascal-joos/code_cure_agent/cca_workspace/")
        self.ai_config = AIConfig(-1, warning_repository_URL, warning_repository_commit, warning_file_path,
                                  warning_repository_name, warning_rule_key, warning_start_line, warning_rule_name, warning_specific_message)
        self.exps = ["experiment_test"]

        logger.agent = self

        self.current_state = current_state
        with open("sonarqube_quality_profile/quality_profile_rule_keys.txt") as rule_keys_file:
            self.sonar_qube_rules_in_active_profile = rule_keys_file.read().split(",")

        self.write_fix_attempts = 0
        self.initial_analysis_reports = {}
        self.lsp_server_initialized = False

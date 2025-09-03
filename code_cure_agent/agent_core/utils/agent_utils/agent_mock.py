
import os
from agent_core.logs import logger
import logging

from agent_core.memory.message_history import MessageHistory
from agent_core.llm.providers.openai import OPEN_AI_CHAT_MODELS

SORALD_JAR_PATH = os.path.join(
    os.path.dirname(__file__), "../../../sorald/sorald.jar")


class Config():
    def __init__(self, workspace_path):
        self.workspace_path = workspace_path
        self.sorald_jar_path = SORALD_JAR_PATH


class AIConfig():
    def __init__(self, warning_ID, warning_repository_URL, warning_repository_commit, warning_repository_target_java_version, warning_file_path, warning_repository_name, warning_rule_key, warning_start_line, warning_rule_name, warning_specific_message):
        self.warning_ID = warning_ID
        self.warning_repository_URL = warning_repository_URL
        self.warning_repository_commit = warning_repository_commit
        self.warning_repository_target_java_version = warning_repository_target_java_version
        self.warning_file_path = warning_file_path
        self.warning_rule_key = warning_rule_key
        self.warning_start_line = warning_start_line
        self.warning_rule_name = warning_rule_name
        self.warning_specific_message = warning_specific_message

        self.warning_repository_name = warning_repository_name
        if self.warning_file_path:
            self.warning_file_name = warning_file_path.split("/")[-1]


class AgentMock():
    def __init__(self, warning_repository_URL, warning_repository_commit, warning_file_path, warning_repository_name, warning_rule_key, warning_start_line, warning_rule_name, warning_specific_message, workspace_path=os.path.join(os.path.dirname(__file__), "../../../cca_workspace/"), current_state="fix_tp", logger_level=logging.INFO, warning_ID=-1):
        logger.set_level(logger_level)
        warning_repository_target_java_version = "8"
        self.config = Config(workspace_path)
        self.ai_config = AIConfig(warning_ID, warning_repository_URL, warning_repository_commit, warning_repository_target_java_version, warning_file_path,
                                  warning_repository_name, warning_rule_key, warning_start_line, warning_rule_name, warning_specific_message)
        self.exps = ["experiment_test"]

        logger.agent = self

        self.current_state = current_state
        with open("sonarqube_quality_profile/quality_profile_rule_keys.txt") as rule_keys_file:
            self.sonar_qube_rules_in_active_profile = rule_keys_file.read().split(",")

        self.write_fix_attempts = 0
        self.plausible_fixes = 0
        self.initial_analysis_reports = {}
        self.lsp_server_initialized = False
        self.history = MessageHistory(
            OPEN_AI_CHAT_MODELS["gpt-4.1-mini-2025-04-14"]
        )

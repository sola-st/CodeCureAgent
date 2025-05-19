import unittest

import os
import shutil

from tests.agent_mock import AgentMock

from autogpt.commands.repository_operations import checkout_project
from autogpt.commands import write_fix
from autogpt.commands import change_approver


class ChangeApproverTestCase(unittest.TestCase):
    def setUp(self):
        auto_gpt_workspace = "auto_gpt_workspace"
        if os.path.exists(auto_gpt_workspace):
            shutil.rmtree(auto_gpt_workspace)
        os.mkdir(auto_gpt_workspace)

        if os.path.exists("experimental_setups/experiment_test"):
            shutil.rmtree("experimental_setups/experiment_test")
        os.mkdir("experimental_setups/experiment_test")
        os.mkdir("experimental_setups/experiment_test/initial_analysis_reports")
        os.mkdir("experimental_setups/experiment_test/plausible_patches")
        os.mkdir("experimental_setups/experiment_test/implausible_patches")

        warning_repository_URL = "https://github.com/argparse4j/argparse4j.git"
        warning_repository_commit = "a0cef432451487d513382297cec2c5b14c147a30"
        warning_repository_name = "argparse4j"
        warning_file_path = "main/src/main/java/net/sourceforge/argparse4j/internal/TerminalWidth.java"
        warning_rule_key = "S2142"
        warning_start_line = 94
        warning_rule_name = "'InterruptedException' should not be ignored"
        warning_specific_message = "Either re-interrupt this method or rethrow the ""InterruptedException"" that can be caught here."

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, warning_file_path,
                               warning_repository_name, warning_rule_key, warning_start_line, warning_rule_name, warning_specific_message)

        checkout_project(self.agent)

        with open(os.path.join(self.agent.config.workspace_path, self.agent.ai_config.warning_repository_name, self.agent.ai_config.warning_file_path)) as orginal_file:
            self.file_without_changes = orginal_file.readlines()

    def tearDown(self):
        auto_gpt_workspace = "auto_gpt_workspace"
        if os.path.exists(auto_gpt_workspace):
            shutil.rmtree(auto_gpt_workspace)
        os.mkdir(auto_gpt_workspace)

    def test_write_fix_with_compile_errors(self):
        changes_dict = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "modifications": [{
                "line_number": 43,
                "modified_line": "    public String getTerminalWidth() {\n"
            }]
        }]
        print(write_fix.write_fix(changes_dict, self.agent))

    def test_try_to_build_changed_project_succeeds(self):
        change_dict_list = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "modifications": [{
                "line_number": 43,
                "modified_line": "    public int getTerminalWidth() \n{\n"
            }]
        }]
        file_relative_path = self.agent.ai_config.warning_file_path

        project_dir = os.path.join(
            self.agent.config.workspace_path, self.agent.ai_config.warning_repository_name)

        file_full_path = os.path.join(project_dir, file_relative_path)

        write_fix.apply_changes(
            change_dict_list, file_relative_path, file_full_path)
        rejected, build_message = change_approver.try_to_build_changed_project(
            self.agent)
        rejected, build_message = change_approver.try_to_build_changed_project(
            self.agent)
        print(build_message)
        self.assertFalse(rejected)

    def test_try_to_build_changed_project_fails(self):
        change_dict_list = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "modifications": [{
                "line_number": 43,
                "modified_line": "    public String getTerminalWidth() {\n"
            }]
        }]
        file_relative_path = self.agent.ai_config.warning_file_path

        project_dir = os.path.join(
            self.agent.config.workspace_path, self.agent.ai_config.warning_repository_name)

        file_full_path = os.path.join(project_dir, file_relative_path)

        write_fix.apply_changes(
            change_dict_list, file_relative_path, file_full_path)
        rejected, build_message = change_approver.try_to_build_changed_project(
            self.agent)
        print(build_message)
        self.assertTrue(rejected)
        self.assertNotEqual(build_message.find(
            "[ERROR] COMPILATION ERROR :"), -1)
        self.assertNotEqual(build_message.find(
            "main/src/main/java/net/sourceforge/argparse4j/internal/TerminalWidth.java"), -1)
        self.assertNotEqual(build_message.find("47"), -1)

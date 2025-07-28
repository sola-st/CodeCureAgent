import unittest

import os
import shutil

from agent_core.utils.agent_utils.agent_mock import AgentMock

from agent_core.commands.repository_operations import checkout_project
from agent_core.commands.system import goals_accomplished
from agent_core.commands import change_approver, write_fix


class GoalsAccomplishedTestCase(unittest.TestCase):
    def setUp(self):
        cca_workspace = "cca_workspace"
        if os.path.exists(cca_workspace):
            shutil.rmtree(cca_workspace)
        os.mkdir(cca_workspace)

        if os.path.exists("experimental_setups/experiment_test"):
            shutil.rmtree("experimental_setups/experiment_test")
        os.mkdir("experimental_setups/experiment_test")
        os.mkdir("experimental_setups/experiment_test/fix_tp")
        os.mkdir("experimental_setups/experiment_test/fix_tp/analysis_reports")
        os.mkdir("experimental_setups/experiment_test/fix_tp/plausible_patches")
        os.mkdir("experimental_setups/experiment_test/fix_tp/implausible_patches")
        os.mkdir("experimental_setups/experiment_test/fix_tp/execution_info")
        os.mkdir("experimental_setups/experiment_test/fix_tp/all_messages")
        os.mkdir("experimental_setups/experiment_test/fix_fp")
        os.mkdir("experimental_setups/experiment_test/fix_fp/analysis_reports")
        os.mkdir("experimental_setups/experiment_test/fix_fp/plausible_patches")
        os.mkdir("experimental_setups/experiment_test/fix_fp/implausible_patches")
        os.mkdir("experimental_setups/experiment_test/fix_fp/execution_info")
        os.mkdir("experimental_setups/experiment_test/fix_fp/all_messages")

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

    def tearDown(self):
        cca_workspace = "cca_workspace"
        if os.path.exists(cca_workspace):
            shutil.rmtree(cca_workspace)
        os.mkdir(cca_workspace)
        if os.path.exists("experimental_setups/experiment_test"):
            shutil.rmtree("experimental_setups/experiment_test")

    def test_goals_accomplished_no_plausible_fix_yet(self):
        goal_return = goals_accomplished("Some reason", self.agent)
        print(goal_return)
        self.assertEqual(goal_return, """Trying to set the goals as accomplished failed! You have not yet accomplished the goal!  
None of your previous calls to write_fix have been approved. All of them have been labeled as 'REJECTED'.  
Your goals are only accomplished after one of your write_fix calls returns 'APPROVED'.  
Adhere to the information given to you about your failed write_fix attempts and propose a fix that resolves the issues.  
Only call this command again after one of your write_fix attempts returns 'APPROVED'.""")

    def test_goals_accomplished_plausible_fix(self):
        self.agent.current_state = "fix_fp"

        change_dict_list = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 94,
                "new_lines": ["        } catch (InterruptedException e) { //NOSONAR\n"]
            }],
            "deletions": [94],
            "modifications": []
        }]

        all_file_changes = write_fix.execute_write_range(
            change_dict_list, self.agent)
        change_approver_result = change_approver.approve_changes(
            change_dict_list, all_file_changes, self.agent)

        with self.assertRaises(SystemExit) as se:
            goals_accomplished("Some reason", self.agent)
        self.assertEqual(se.exception.code, 0)

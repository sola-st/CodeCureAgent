import unittest

import os
import shutil

from tests.agent_mock import AgentMock

from autogpt.commands.repository_operations import checkout_project
from autogpt.commands import write_fix


class WriteFixTestCase(unittest.TestCase):
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

    def test_write_fix(self):
        changes_dict = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 10,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [1],
            "modifications": [{
                "line_number": 4,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 11,
                "modified_line": "    modified_there\n"
            }]
        },
            {
            "file_name": "main/src/main/java/net/sourceforge/argparse4j/internal/SubparsersImpl.java",
            "insertions": [{
                "line_number": 10,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [1],
            "modifications": [{
                "line_number": 4,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 11,
                "modified_line": "    modified_there\n"
            }]
        }]
        print(write_fix.write_fix(changes_dict, self.agent))

    def test_write_fix_apply_changes_error_handled(self):
        changes_dict = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 10,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [1],
            "modifications": [{
                "line_number": 4,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 140,
                "modified_line": "    modified_there\n"
            }]
        },
            {
            "file_name": "main/src/main/java/net/sourceforge/argparse4j/internal/SubparsersImpl.java",
            "insertions": [{
                "line_number": 10,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [1],
            "modifications": [{
                "line_number": 4,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 11,
                "modified_line": "    modified_there\n"
            }]
        }]
        print(write_fix.write_fix(changes_dict, self.agent))

    def test_write_fix_wrong_file_name_key_in_second_dict(self):
        changes_dict = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 10,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [1],
            "modifications": [{
                "line_number": 4,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 11,
                "modified_line": "    modified_there\n"
            }]
        },
            {
            "file_path": "main/src/main/java/net/sourceforge/argparse4j/internal/SubparsersImpl.java",
            "insertions": [{
                "line_number": 10,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [1],
            "modifications": [{
                "line_number": 4,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 11,
                "modified_line": "    modified_there\n"
            }]
        }]
        result = write_fix.write_fix(changes_dict, self.agent)

        self.assertEqual(
            result, "Failure when trying to apply the fix: The write_fix command was in a wrong format. Couldn't find `file_name` in the change_dict.")

    def test_write_fix_no_insertions_deletions_and_modification_keys_in_second_dict(self):
        changes_dict = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 10,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [1],
            "modifications": [{
                "line_number": 4,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 11,
                "modified_line": "    modified_there\n"
            }]
        },
            {
            "file_name": "main/src/main/java/net/sourceforge/argparse4j/internal/SubparsersImpl.java"
        }]
        result = write_fix.write_fix(changes_dict, self.agent)

        self.assertEqual(
            result, "Failure when trying to apply the fix: The write_fix command was in a wrong format. Neither `insertions`, `deletions` nor `modifications` was given in the change_dict.")

    def test_apply_changes_no_changes(self):
        change_dict = {
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [],
            "deletions": [],
            "modifications": []
        }

        change_result = write_fix.apply_changes(change_dict, self.agent)
        self.assertEqual(
            change_result["file_content"], self.file_without_changes)

    def test_apply_changes_delete_line_before_a_modified_line_and_insert_line_before_modified_line_changes(self):
        change_dict = {
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 10,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [1],
            "modifications": [{
                "line_number": 4,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 11,
                "modified_line": "    modified_there\n"
            }]
        }

        change_result = write_fix.apply_changes(change_dict, self.agent)

        with open("tests/test_write_fix_expected_outputs/expected_apply_changes_delete_line_before_a_modified_line_and_insert_line_before_modified_line_changes.txt") as expected_file:
            expected_result = expected_file.readlines()

        self.assertEqual(change_result["file_content"], expected_result)

    def test_apply_changes_delete_insert_and_modify_last_line(self):
        change_dict = {
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 133,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [133],
            "modifications": [{
                "line_number": 133,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 133,
                "modified_line": "    modified_there\n"
            }]
        }

        change_result = write_fix.apply_changes(change_dict, self.agent)

        with open("tests/test_write_fix_expected_outputs/expected_apply_changes_delete_insert_and_modify_last_line.txt") as expected_file:
            expected_result = expected_file.readlines()

        self.assertEqual(change_result["file_content"], expected_result)

    def test_apply_changes_delete_out_of_bounds_line(self):
        change_dict = {
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 10,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [0],
            "modifications": [{
                "line_number": 4,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 11,
                "modified_line": "    modified_there\n"
            }]
        }
        with self.assertRaises(write_fix.ApplyChangesError) as ace:
            change_result = write_fix.apply_changes(change_dict, self.agent)
        self.assertEqual(
            ace.exception.msg, f"Line 0 to delete was out of range for the file {change_dict['file_name']}. The file only has 133 lines.")

    def test_apply_changes_modify_out_of_bounds_line(self):
        change_dict = {
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 10,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [1],
            "modifications": [{
                "line_number": 2,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 134,
                "modified_line": "    modified_there\n"
            }]
        }
        with self.assertRaises(write_fix.ApplyChangesError) as ace:
            change_result = write_fix.apply_changes(change_dict, self.agent)

        self.assertEqual(
            ace.exception.msg, f"Line 134 to modify was out of range for the file {change_dict['file_name']}. The file only has 133 lines.")

    def test_apply_changes_insert_after_last_line(self):
        change_dict = {
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 134,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [133],
            "modifications": [{
                "line_number": 133,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 133,
                "modified_line": "    modified_there\n"
            }]
        }

        change_result = write_fix.apply_changes(change_dict, self.agent)

        with open("tests/test_write_fix_expected_outputs/expected_apply_changes_insert_after_last_line.txt") as expected_file:
            expected_result = expected_file.readlines()

        self.assertEqual(change_result["file_content"], expected_result)

    def test_apply_changes_insert_multiple_lines_after_last_line(self):
        change_dict = {
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 138,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [133],
            "modifications": [{
                "line_number": 133,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 133,
                "modified_line": "    modified_there\n"
            }]
        }

        change_result = write_fix.apply_changes(change_dict, self.agent)

        with open("tests/test_write_fix_expected_outputs/expected_apply_changes_insert_multiple_lines_after_last_line.txt") as expected_file:
            expected_result = expected_file.readlines()

        self.assertEqual(change_result["file_content"], expected_result)

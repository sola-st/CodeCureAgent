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
        os.mkdir("experimental_setups/experiment_test/analysis_reports")
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
        if os.path.exists("experimental_setups/experiment_test"):
            shutil.rmtree("experimental_setups/experiment_test")

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

        all_file_changes = [write_fix.apply_changes(
            change_dict_list, file_relative_path, file_full_path)]
        accepted, build_message = change_approver.try_to_build_changed_project(all_file_changes,
                                                                               self.agent)
        print(build_message)
        self.assertTrue(accepted)

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

        all_file_changes = [write_fix.apply_changes(
            change_dict_list, file_relative_path, file_full_path)]
        accepted, build_message = change_approver.try_to_build_changed_project(all_file_changes,
                                                                               self.agent)
        print(build_message)
        self.assertFalse(accepted)
        self.assertNotEqual(build_message.find(
            "[ERROR] COMPILATION ERROR :"), -1)
        self.assertNotEqual(build_message.find(
            "main/src/main/java/net/sourceforge/argparse4j/internal/TerminalWidth.java"), -1)
        self.assertNotEqual(build_message.find("47"), -1)

    def test_show_changed_code(self):
        change_dict_list = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 3,
                "new_lines": [" * Added lines\n", " * Added lines\n", " * Added lines\n"]
            }],
            "deletions": [11, 12, 13, 14, 15],
            "modifications": [{
                "line_number": 94,
                "modified_line": "        } catch (InterruptedException e) { //NOSONAR\n"
            }]
        }]
        file_relative_path = self.agent.ai_config.warning_file_path

        project_dir = os.path.join(
            self.agent.config.workspace_path, self.agent.ai_config.warning_repository_name)

        file_full_path = os.path.join(project_dir, file_relative_path)

        all_file_changes = [write_fix.apply_changes(
            change_dict_list, file_relative_path, file_full_path)]
        changed_code_message = change_approver.show_changed_code(all_file_changes,
                                                                 self.agent)
        print(changed_code_message)

        self.assertEqual(len(changed_code_message.splitlines()), 106)

    def test_show_changed_code_full_file_no_overrun(self):
        change_dict_list = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 1,
                "new_lines": ["First line\n", " * Added lines\n", " * Added lines\n"]
            }],
            "deletions": [11, 12, 13, 14, 15],
            "modifications": [{
                "line_number": 133,
                "modified_line": "last line\n"
            }]
        }]
        file_relative_path = self.agent.ai_config.warning_file_path

        project_dir = os.path.join(
            self.agent.config.workspace_path, self.agent.ai_config.warning_repository_name)

        file_full_path = os.path.join(project_dir, file_relative_path)

        all_file_changes = [write_fix.apply_changes(
            change_dict_list, file_relative_path, file_full_path)]
        changed_code_message = change_approver.show_changed_code(all_file_changes,
                                                                 self.agent)
        print(changed_code_message)

        self.assertEqual(changed_code_message.splitlines(
        )[-2], "modified line: before:'}' after:'last line'")
        self.assertEqual(changed_code_message.splitlines()
                         [3], "inserted line:First line")

    def test_show_changed_code_complex_example(self):
        change_dict_list = [
            {
                "file_name": "main/src/test/java/net/sourceforge/argparse4j/impl/type/FileVerificationOrTest.java",
                "insertions": [
                    {
                        "line_number": 5,
                        "new_lines": [
                            "import java.nio.file.Files;",
                            "import java.io.IOException;"
                        ]
                    },
                    {
                        "line_number": 38,
                        "new_lines": [
                            "    public static void deleteTestFiles() {",
                            "        writableFile.delete();",
                            "",
                            "        nonWritableFile.setWritable(true);",
                            "        try {",
                            "            Files.delete(nonWritableFile.toPath());",
                            "        } catch (IOException e) {",
                            "            throw new RuntimeException(e);",
                            "        }"
                        ]
                    }
                ],
                "deletions": [39, 42, 43]
            }
        ]
        file_relative_path = self.agent.ai_config.warning_file_path

        project_dir = os.path.join(
            self.agent.config.workspace_path, self.agent.ai_config.warning_repository_name)

        file_full_path = os.path.join(project_dir, file_relative_path)

        all_file_changes = [write_fix.apply_changes(
            change_dict_list, file_relative_path, file_full_path)]
        changed_code_message = change_approver.show_changed_code(all_file_changes,
                                                                 self.agent)
        print(changed_code_message)

    def test_check_sonar_qube_report_target_warning_removed_no_new_warnings(self):
        change_dict_list = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 3,
                "new_lines": [" * Added lines\n", " * Added lines\n", " * Added lines\n"]
            }],
            "deletions": [11, 12, 13, 14, 15],
            "modifications": [{
                "line_number": 94,
                "modified_line": "        } catch (InterruptedException e) { //NOSONAR\n"
            }]
        }]

        all_file_changes = write_fix.execute_write_range(
            change_dict_list, self.agent)
        accepted, sonar_qube_message = change_approver.check_sonar_qube_report(
            all_file_changes, self.agent)

        print(sonar_qube_message)
        self.assertTrue(accepted)
        self.assertEqual(
            sonar_qube_message, "Rerunning the SonarQube analysis confirmed that your fix successfully removed the targeted rule violation and didn't introduce any new violations.")

    def test_check_sonar_qube_report_target_warning_removed_new_warning_introduced(self):
        change_dict_list = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "modifications": [{
                "line_number": 94,
                "modified_line": "        } catch (InterruptedException e) { //NOSONAR\n"
            }],
            "insertions": [{
                "line_number": 94,
                "new_lines": ["            if (1 == 1) {\n",
                              "                return UNKNOWN_WIDTH;\n",
                              "            }\n"]
            }]
        }]

        all_file_changes = write_fix.execute_write_range(
            change_dict_list, self.agent)
        accepted, sonar_qube_message = change_approver.check_sonar_qube_report(
            all_file_changes, self.agent)

        print(sonar_qube_message)
        self.assertFalse(accepted)
        self.assertEqual(sonar_qube_message, """Rerunning the SonarQube analysis found the following new rule violations that weren't present before:  
In file main/src/main/java/net/sourceforge/argparse4j/internal/TerminalWidth.java:  
Rule S1764: 'Identical expressions should not be used on both sides of a binary operator' (Context-specific message: 'Correct one of the identical sub-expressions on both sides of operator "=="') at line 94: 'if (1 == 1) {'  

You must not introduce any new rule violations.""")

    def test_check_sonar_qube_report_target_warning_not_removed(self):
        change_dict_list = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "modifications": [],
            "insertions": [{
                "line_number": 94,
                "new_lines": ["            if (1 == 1) {\n",
                              "                return UNKNOWN_WIDTH;\n",
                              "            }\n"]
            }]
        }]

        all_file_changes = write_fix.execute_write_range(
            change_dict_list, self.agent)
        accepted, sonar_qube_message = change_approver.check_sonar_qube_report(
            all_file_changes, self.agent)

        print(sonar_qube_message)
        self.assertFalse(accepted)
        self.assertEqual(
            sonar_qube_message, "Rerunning the SonarQube analysis found that the targeted rule violation has not yet been removed by your fix. It was still present in the SonarQube report.")

    def test_check_sonar_qube_report_target_warning_file_had_no_changes(self):
        change_dict_list = [{
            "file_name": "NEWS",
            "modifications": [{
                "line_number": 2,
                "modified_line": "Some change\n"
            },],
            "insertions": [{
                "line_number": 94,
                "new_lines": ["Other change\n",
                              "New line\n",
                              "here\n"]
            }]
        }]

        all_file_changes = write_fix.execute_write_range(
            change_dict_list, self.agent)
        accepted, sonar_qube_message = change_approver.check_sonar_qube_report(
            all_file_changes, self.agent)

        print(sonar_qube_message)
        self.assertFalse(accepted)
        self.assertEqual(
            sonar_qube_message, "Rerunning the SonarQube analysis found that the targeted rule violation has not yet been removed by your fix. It was still present in the SonarQube report.")

    # The "paired_insertions_and_deletions" insertion-deletion pairing functionality, recognizes deletion and insertion of the same line as a kind of modification
    # and therefore resolves the warning of the changed file in this test to the targeted warning in the initial analysis report.
    def test_check_sonar_qube_report_target_warning_line_was_removed_and_added_at_new_line_again(self):
        change_dict_list = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 3,
                "new_lines": [" * Added line 1\n", " * Added line 2\n", " * Added line 3\n"]
            }, {
                "line_number": 94,
                "new_lines": ["        } catch (InterruptedException e) { \n"]
            }],
            "deletions": [11, 12, 13, 14, 15, 94]
        }]

        all_file_changes = write_fix.execute_write_range(
            change_dict_list, self.agent)
        accepted, sonar_qube_message = change_approver.check_sonar_qube_report(
            all_file_changes, self.agent)

        print(sonar_qube_message)
        self.assertFalse(accepted)
        self.assertEqual(
            sonar_qube_message, "Rerunning the SonarQube analysis found that the targeted rule violation has not yet been removed by your fix. It was still present in the SonarQube report.")

    # Also in this case the deletion and insertion of the same line is recognized and therefore the inserted warning is resolved to before and isn't treated as newly introduced warning.
    def test_check_sonar_qube_report_target_warning_removed_and_a_unrelated_warning_was_deleted_and_reinserted_at_same_line(self):
        self.agent.ai_config.warning_file_path = "main/src/test/java/net/sourceforge/argparse4j/impl/type/FileVerificationOrTest.java"
        self.agent.ai_config.warning_rule_key = "S4042"
        self.agent.ai_config.warning_rule_name = "\"java.nio.Files#delete\" should be preferred"
        self.agent.ai_config.warning_specific_message = "Use \"java.nio.file.Files#delete\" here for better messages on error conditions."
        self.agent.ai_config.warning_start_line = 42

        # This is a real world example that occured.
        # "nonWritableFile.setWritable(true);" is the line with the unrelated warning. This line is first delted and then inserted again at the same line.
        # This is catched by the change_approver and recognized as the same warning. Therfore this fix is now accepted.
        change_dict_list = [
            {
                "file_name": "main/src/test/java/net/sourceforge/argparse4j/impl/type/FileVerificationOrTest.java",
                "insertions": [
                    {
                        "line_number": 4,
                        "new_lines": [
                            "import java.nio.file.Files;",
                            "import java.nio.file.Path;"
                        ]
                    },
                    {
                        "line_number": 38,
                        "new_lines": [
                            "    public static void deleteTestFiles() throws IOException {",
                            "        Files.delete(writableFile.toPath());",
                            "",
                            "        nonWritableFile.setWritable(true);",
                            "        Files.delete(nonWritableFile.toPath());",
                            "    }"
                        ]
                    }
                ],
                "deletions": [
                    38,
                    39,
                    40,
                    41,
                    42,
                    43
                ]
            }
        ]

        all_file_changes = write_fix.execute_write_range(
            change_dict_list, self.agent)
        accepted, sonar_qube_message = change_approver.check_sonar_qube_report(
            all_file_changes, self.agent)

        print(sonar_qube_message)
        self.assertTrue(accepted)
        self.assertEqual(
            sonar_qube_message, "Rerunning the SonarQube analysis confirmed that your fix successfully removed the targeted rule violation and didn't introduce any new violations.")

    def test_check_sonar_qube_report_multiple_files(self):
        change_dict_list = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 3,
                "new_lines": [" * Added line 1\n", " * Added line 2\n", " * Added line 3\n"]
            }],
            "deletions": [11, 12, 13, 14, 15],
            "modifications": [{
                "line_number": 94,
                "modified_line": "        } catch (InterruptedException e) { //NOSONAR\n"
            }]
        },
            {
            "file_name": "main/src/main/java/net/sourceforge/argparse4j/internal/SubparsersImpl.java",
            "insertions": [{
                "line_number": 3,
                "new_lines": [" * Added line 1\n", " * Added line 2\n", " * Added line 3\n"]
            }]
        }]

        all_file_changes = write_fix.execute_write_range(
            change_dict_list, self.agent)
        accepted, sonar_qube_message = change_approver.check_sonar_qube_report(
            all_file_changes, self.agent)

        print(sonar_qube_message)
        self.assertTrue(accepted)
        self.assertEqual(
            sonar_qube_message, "Rerunning the SonarQube analysis confirmed that your fix successfully removed the targeted rule violation and didn't introduce any new violations.")

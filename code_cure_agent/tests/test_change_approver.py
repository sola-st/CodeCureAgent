import unittest

import os
import shutil
import re

from agent_core.utils.agent_utils.agent_mock import AgentMock

from agent_core.commands.repository_operations import checkout_project
from agent_core.commands import write_fix
from agent_core.commands import change_approver
from agent_core.commands import sonar_qube_analysis
from agent_core.utils.path_utils.path_utils import sanitize_and_shorten_file_path


class ChangeApproverTestCase(unittest.TestCase):
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
        os.mkdir("experimental_setups/experiment_test/fix_fp")
        os.mkdir("experimental_setups/experiment_test/fix_fp/analysis_reports")
        os.mkdir("experimental_setups/experiment_test/fix_fp/plausible_patches")
        os.mkdir("experimental_setups/experiment_test/fix_fp/implausible_patches")
        os.mkdir("experimental_setups/experiment_test/fix_fp/execution_info")

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
        cca_workspace = "cca_workspace"
        if os.path.exists(cca_workspace):
            shutil.rmtree(cca_workspace)
        os.mkdir(cca_workspace)
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
        changed_code_message = change_approver.show_changed_code(
            all_file_changes)
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
        changed_code_message = change_approver.show_changed_code(
            all_file_changes)
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
        changed_code_message = change_approver.show_changed_code(
            all_file_changes)
        print(changed_code_message)

    def test_show_changed_code_long_section_of_unchanged_code_reduced(self):
        change_dict_list = [{
            "file_name": "main/src/main/java/net/sourceforge/argparse4j/ArgumentParsers.java",
            "insertions": [{
                "line_number": 3,
                "new_lines": [" * Added lines\n", " * Added lines\n", " * Added lines\n"]
            }],
            "deletions": [11, 12, 13, 14, 15],
            "modifications": [{
                "line_number": 352,
                "modified_line": "        return somethingElse;\n"
            }]
        }]
        file_relative_path = "main/src/main/java/net/sourceforge/argparse4j/ArgumentParsers.java"

        project_dir = os.path.join(
            self.agent.config.workspace_path, self.agent.ai_config.warning_repository_name)

        file_full_path = os.path.join(project_dir, file_relative_path)

        all_file_changes = [write_fix.apply_changes(
            change_dict_list, file_relative_path, file_full_path)]
        changed_code_message = change_approver.show_changed_code(
            all_file_changes)
        print(changed_code_message)

        self.assertEqual(len(changed_code_message.splitlines()), 67)
        self.assertFalse(changed_code_message.find(
            "... not showing 296 more unchanged lines ...") == -1)

    def test_check_if_suppression_literal_inserted_NOSONAR_is_inserted(self):
        self.agent.current_state = "fix_fp"

        change_dict_list = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 94,
                "new_lines": ["//something", "//something else", "        } catch (InterruptedException e) { //NOSONAR\n"]
            }],
            "deletions": [94],
            "modifications": []
        }]

        all_file_changes = write_fix.execute_write_range(
            change_dict_list, self.agent)
        change_approver_result = change_approver.approve_changes(
            change_dict_list, all_file_changes, self.agent)

        print(change_approver_result)
        self.assertEqual(
            change_approver_result, """APPROVED  
Project was successfully built with the applied changes.  
Rerunning the SonarQube analysis confirmed that your fix successfully removed the targeted rule violation and didn't introduce any new violations.  
All tests in the project have been run and passed successfully.  
The repository has been restored to its original state. 
If you think that your write_fix solved the problem then use the command goals_accomplished to conclude the task.""")

    def test_check_if_suppression_literal_inserted_SuppressWarnings_is_inserted_somewhere_with_correct_rule_key(self):
        self.agent.current_state = "fix_fp"

        change_dict_list = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 94,
                "new_lines": ["//something", "//something else", "        } catch (InterruptedException e) { \n"]
            }, {
                "line_number": 62,
                "new_lines": ["@SuppressWarnings({\"java:S2142\"})"]
            }],
            "deletions": [94],
            "modifications": []
        }]

        all_file_changes = write_fix.execute_write_range(
            change_dict_list, self.agent)
        change_approver_result = change_approver.approve_changes(
            change_dict_list, all_file_changes, self.agent)

        print(change_approver_result)
        self.assertEqual(
            change_approver_result, """APPROVED  
Project was successfully built with the applied changes.  
Rerunning the SonarQube analysis confirmed that your fix successfully removed the targeted rule violation and didn't introduce any new violations.  
All tests in the project have been run and passed successfully.  
The repository has been restored to its original state. 
If you think that your write_fix solved the problem then use the command goals_accomplished to conclude the task.""")

    def test_check_if_suppression_literal_inserted_no_correct_suppression_inserted(self):
        self.agent.current_state = "fix_fp"

        change_dict_list = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 94,
                "new_lines": ["//something", "//something else", "        } catch (InterruptedException e) { // NOSONAMI\n"]
            }, {
                "line_number": 62,
                # a different rule
                "new_lines": ["@SuppressWarnings({\"java:S5201\"})"]
            }],
            "deletions": [94],
            "modifications": []
        }]

        all_file_changes = write_fix.execute_write_range(
            change_dict_list, self.agent)
        change_approver_result = change_approver.approve_changes(
            change_dict_list, all_file_changes, self.agent)

        print(change_approver_result)
        self.assertEqual(
            change_approver_result, """REJECTED  
IMPORTANT: The repository has been restored to its original state! You need to start applying changes from scratch again.
Project was successfully built with the applied changes.  
However, you failed to insert a suppression. Neither a `// NOSONAR` nor a `@SuppressWarnings('java:S...')` was found in any of your insertions (where S... is the rule key).""")

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
        self.assertEqual(sonar_qube_message, """Rerunning the SonarQube analysis found the following new rule violations that weren't present before, or that have moved:  
In file main/src/main/java/net/sourceforge/argparse4j/internal/TerminalWidth.java:  
Rule S1764: 'Identical expressions should not be used on both sides of a binary operator' (Context-specific message: 'Correct one of the identical sub-expressions on both sides of operator "=="') at line 94: 'if (1 == 1) {'  

You must not introduce any new rule violations. They need to be prevented/resolved, even if you think the rule violations were already present in the project before. If you think the listed rule violations were already present before and you are certain that they are false positives, then you can also suppress them with '// NOSONAR'.""")

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

        sanitized_warning_file_path = sanitize_and_shorten_file_path(
            self.agent.ai_config.warning_file_path)
        initial_analysis_report_target_file = sonar_qube_analysis.analyze_file_and_parse_report(self.agent.ai_config.warning_file_path, self.agent.sonar_qube_rules_in_active_profile, self.agent.ai_config.warning_repository_name,
                                                                                                f"{str(self.agent.ai_config.warning_ID)}_{self.agent.ai_config.warning_repository_name}_{self.agent.ai_config.warning_rule_key}_{self.agent.ai_config.warning_file_name}_line_{str(self.agent.ai_config.warning_start_line)}_initial_analysis_report_file_{sanitized_warning_file_path}.json", self.agent)

        self.agent.initial_analysis_reports[self.agent.ai_config.warning_file_path] = initial_analysis_report_target_file

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

    def test_run_tests_passes_on_unchanged_project(self):
        success, test_message = change_approver.try_to_run_tests(self.agent)
        print(test_message)
        self.assertTrue(success)
        self.assertEqual(
            test_message, "All tests in the project have been run and passed successfully.")

    def test_run_tests_failing_due_to_introducing_NullPointerException(self):
        # Real world example that passed previous checks but introduced a NullPointerException catched by a test.
        change_dict_list = [
            {
                "file_name": "main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java",
                "insertions": [
                    {
                        "line_number": 891,
                        "new_lines": [
                            "        // Extract nested ternary operator into a separate variable to comply with S3358",
                            "        String prefixCharsStr = config_.fromFilePrefixPattern_.getPrefixChars().length() == 1 ?",
                            "                config_.fromFilePrefixPattern_.getPrefixChars() :",
                            "                \"[\" + config_.fromFilePrefixPattern_.getPrefixChars() + \"]\";"
                        ]
                    }
                ],
                "deletions": [],
                "modifications": [
                    {
                        "line_number": 895,
                        "modified_line": "                        state.index > state.lastFromFileArgIndex ? \"\" : String.format(TextHelper.LOCALE_ROOT, localize(\"trailingWhiteSpacesInFileTip\"), prefixCharsStr));"
                    },
                    {
                        "line_number": 896,
                        "modified_line": ""
                    },
                    {
                        "line_number": 897,
                        "modified_line": ""
                    },
                    {
                        "line_number": 898,
                        "modified_line": ""
                    },
                    {
                        "line_number": 899,
                        "modified_line": ""
                    },
                    {
                        "line_number": 900,
                        "modified_line": ""
                    },
                    {
                        "line_number": 901,
                        "modified_line": ""
                    },
                    {
                        "line_number": 902,
                        "modified_line": ""
                    },
                    {
                        "line_number": 903,
                        "modified_line": ""
                    }
                ]
            }
        ]

        all_file_changes = write_fix.execute_write_range(
            change_dict_list, self.agent)
        success, test_message = change_approver.try_to_run_tests(self.agent)
        print(test_message)
        self.assertFalse(success)
        expected = """However, running the tests in the project failed with the following failing tests:  
-------------------------------------------------------------------------------
Test file with failure: main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java
-------------------------------------------------------------------------------
Tests run: 83, Failures: 0, Errors: 5, Skipped: 0, Time elapsed: 0.081 sec <<< FAILURE!
Failing test method: testParseArgsWithNegativeNumberLikeFlag  Time elapsed: 0.008 sec  <<< ERROR!
java.lang.NullPointerException
\tat main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: formatUnrecognizedArgumentErrorMessage (line 892)
\tat main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: parseArgs (line 829)
\tat main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: parseArgsAtOffsetZero (line 703)
\tat main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: parseArgs (line 572)
\tat main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: parseArgs (line 557)
\tat main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java Method: testParseArgsWithNegativeNumberLikeFlag (line 198)

Failing test method: testParseArgsWithCommandAfterSeparator  Time elapsed: 0.006 sec  <<< ERROR!
java.lang.NullPointerException
\tat main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: formatUnrecognizedArgumentErrorMessage (line 892)
\tat main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: parseArgs (line 861)
\tat main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: parseArgsAtOffsetZero (line 703)
\tat main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: parseArgs (line 572)
\tat main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: parseArgs (line 557)
\tat main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java Method: testParseArgsWithCommandAfterSeparator (line 773)

Failing test method: testParseArgsWithUnrecognizedArgs  Time elapsed: 0.002 sec  <<< ERROR!
java.lang.NullPointerException
\tat main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: formatUnrecognizedArgumentErrorMessage (line 892)
\tat main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: parseArgs (line 861)
\tat main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: parseArgsAtOffsetZero (line 703)
\tat main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: parseArgs (line 572)
\tat main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: parseArgs (line 557)
\tat main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java Method: testParseArgsWithUnrecognizedArgs (line 634)

Failing test method: testSubparserWithoutAddHelp  Time elapsed: 0.001 sec  <<< ERROR!
java.lang.NullPointerException
\tat main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: formatUnrecognizedArgumentErrorMessage (line 892)
\tat main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: parseArgs (line 829)
\tat main/src/main/java/net/sourceforge/argparse4j/internal/SubparserImpl.java Method: parseArgs (line 266)
\tat main/src/main/java/net/sourceforge/argparse4j/internal/SubparsersImpl.java Method: parseArg (line 198)
\tat main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: parseArgs (line 857)
\tat main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: parseArgsAtOffsetZero (line 703)
\tat main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: parseArgs (line 572)
\tat main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: parseArgs (line 557)
\tat main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java Method: testSubparserWithoutAddHelp (line 989)

Failing test method: testArgumentParserWithoutAddHelp  Time elapsed: 0.001 sec  <<< ERROR!
java.lang.NullPointerException
\tat main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: formatUnrecognizedArgumentErrorMessage (line 892)
\tat main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: parseArgs (line 829)
\tat main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: parseArgsAtOffsetZero (line 703)
\tat main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: parseArgs (line 572)
\tat main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java Method: parseArgs (line 557)
\tat main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java Method: testArgumentParserWithoutAddHelp (line 977)

  
"""
        self.assertEqual(re.sub(
            r'Time elapsed: [\d\.]+ sec', 'Time elapsed: <ignored>', test_message), re.sub(
            r'Time elapsed: [\d\.]+ sec', 'Time elapsed: <ignored>', expected))

    def test_run_tests_multiple_test_files_with_failures(self):
        # The jolokia project is excluded, as it has failing tests in our environment
        warning_repository_URL = "https://github.com/rhuss/jolokia.git"
        warning_repository_commit = "913a35138235e8f53b31662b11b12992cf4bf702"
        warning_repository_name = "jolokia"

        warning_file_path = "some/path.java"

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, warning_file_path,
                               warning_repository_name, None, None, None, None)

        checkout_project(self.agent)

        success, test_message = change_approver.try_to_run_tests(self.agent)
        print(test_message)
        expected = """However, running the tests in the project failed with the following failing tests:  
-------------------------------------------------------------------------------
Test file with failure: agent/jvm/src/test/java/org/jolokia/jvmagent/security/DelegatingAuthenticatorTest.java
-------------------------------------------------------------------------------
Tests run: 11, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 4.798 sec <<< FAILURE! - in org.jolokia.jvmagent.security.DelegatingAuthenticatorTest
Failing test method: invalidProtocol  Time elapsed: 2.046 sec  <<< FAILURE!
java.lang.AssertionError: expected [401] but found [503]
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/security/DelegatingAuthenticatorTest.java Method: invalidProtocol (line 135)

  
-------------------------------------------------------------------------------
Test file with failure: agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java
-------------------------------------------------------------------------------
Tests run: 27, Failures: 11, Errors: 0, Skipped: 0, Time elapsed: 2.386 sec <<< FAILURE! - in org.jolokia.jvmagent.JolokiaServerTest
Failing test method: sslWithSpecialHttpsSettings  Time elapsed: 0.092 sec  <<< FAILURE!
java.lang.AssertionError: Expected at least one connection to succeed on TLSv1.1
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: sslWithSpecialHttpsSettings (line 334)

Failing test method: t_22_signed_client_cert  Time elapsed: 0.072 sec  <<< FAILURE!
java.net.SocketException: Broken pipe (Write failed)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: checkServer (line 498)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: checkServer (line 456)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: httpsRoundtrip (line 390)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: t_22_signed_client_cert (line 147)

Failing test method: t_231_with_extended_client_key_usage  Time elapsed: 0.053 sec  <<< FAILURE!
java.net.SocketException: Broken pipe (Write failed)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: checkServer (line 498)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: checkServer (line 456)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: httpsRoundtrip (line 390)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: t_231_with_extended_client_key_usage (line 154)

Failing test method: t_2331_without_extended_client_key_usage  Time elapsed: 0.05 sec  <<< FAILURE!
org.testng.TestException: 
The exception was thrown with the wrong message: expected ".*403.*" but got "Broken pipe (Write failed)"
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: checkServer (line 498)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: checkServer (line 456)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: httpsRoundtrip (line 390)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: t_2331_without_extended_client_key_usage (line 168)

Failing test method: t_2332_without_extended_client_key_usage_allowed  Time elapsed: 0.047 sec  <<< FAILURE!
java.net.SocketException: Broken pipe (Write failed)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: checkServer (line 498)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: checkServer (line 456)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: httpsRoundtrip (line 390)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: t_2332_without_extended_client_key_usage_allowed (line 175)

Failing test method: t_241_with_client_principal  Time elapsed: 0.048 sec  <<< FAILURE!
java.net.SocketException: Broken pipe (Write failed)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: checkServer (line 498)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: checkServer (line 456)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: httpsRoundtrip (line 390)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: t_241_with_client_principal (line 189)

Failing test method: t_242_with_wrong_client_principal  Time elapsed: 0.048 sec  <<< FAILURE!
org.testng.TestException: 
The exception was thrown with the wrong message: expected ".*403.*" but got "Broken pipe (Write failed)"
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: checkServer (line 498)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: checkServer (line 456)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: httpsRoundtrip (line 390)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: t_242_with_wrong_client_principal (line 197)

Failing test method: t_261_with_client_principal  Time elapsed: 0.046 sec  <<< FAILURE!
java.net.SocketException: Broken pipe (Write failed)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: checkServer (line 498)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: checkServer (line 456)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: httpsRoundtrip (line 390)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: t_261_with_client_principal (line 215)

Failing test method: t_262_with_wrong_client_principal  Time elapsed: 0.047 sec  <<< FAILURE!
org.testng.TestException: 
The exception was thrown with the wrong message: expected ".*401.*" but got "Broken pipe (Write failed)"
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: checkServer (line 498)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: checkServer (line 456)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: httpsRoundtrip (line 390)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: t_262_with_wrong_client_principal (line 223)

Failing test method: t_263_with_basic_auth  Time elapsed: 0.049 sec  <<< FAILURE!
java.net.SocketException: Broken pipe (Write failed)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: checkServer (line 498)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: httpsRoundtrip (line 396)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: t_263_with_basic_auth (line 231)

Failing test method: t_264_with_wrong_basic_auth  Time elapsed: 0.046 sec  <<< FAILURE!
org.testng.TestException: 
The exception was thrown with the wrong message: expected ".*401.*" but got "Broken pipe (Write failed)"
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: checkServer (line 498)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: httpsRoundtrip (line 396)
\tat agent/jvm/src/test/java/org/jolokia/jvmagent/JolokiaServerTest.java Method: t_264_with_wrong_basic_auth (line 240)

  
"""
        self.assertEqual(re.sub(
            r'Time elapsed: [\d\.]+ sec', 'Time elapsed: <ignored>', test_message), re.sub(
            r'Time elapsed: [\d\.]+ sec', 'Time elapsed: <ignored>', expected))

    def test_run_tests_multiple_test_folders_and_files_with_failures(self):
        # The project is composed of multiple modules with there separate test output folders that need to be found
        warning_repository_URL = "https://github.com/rhuss/jolokia.git"
        warning_repository_commit = "913a35138235e8f53b31662b11b12992cf4bf702"
        warning_repository_name = "jolokia"
        warning_file_path = "agent/jvm/src/test/java/org/jolokia/jvmagent/security/DelegatingAuthenticatorTest.java"

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, warning_file_path,
                               warning_repository_name, None, None, None, None)

        checkout_project(self.agent)

        # Artifical changes to introduce further failure in a different sub-module
        change_dict_list = [
            {
                "file_name": "agent/osgi/src/test/java/org/jolokia/osgi/security/DelegatingRestrictorTest.java",
                "insertions": [
                    {
                        "line_number": 74,
                        "new_lines": [
                            "        // Inserted test failure",
                            "        assertTrue(false);"
                        ]
                    }
                ],
                "deletions": [],
                "modifications": []
            }
        ]

        all_file_changes = write_fix.execute_write_range(
            change_dict_list, self.agent)

        success, test_message = change_approver.try_to_run_tests(self.agent)
        print(test_message)

        # Here the test suite is not named by file but only one larger "TestSuite",
        # so resolving the test class name in "Test set: TestSuite" is not possible
        # and by extension also not in the "nullRestrictor(org.jolokia.osgi.security.DelegatingRestrictorTest)" line
        expected = """However, running the tests in the project failed with the following failing tests:  
-------------------------------------------------------------------------------
Test set: TestSuite
-------------------------------------------------------------------------------
Tests run: 37, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.712 sec <<< FAILURE! - in TestSuite
nullRestrictor(org.jolokia.osgi.security.DelegatingRestrictorTest)  Time elapsed: 0.003 sec  <<< FAILURE!
java.lang.AssertionError: expected [true] but found [false]
\tat agent/osgi/src/test/java/org/jolokia/osgi/security/DelegatingRestrictorTest.java Method: nullRestrictor (line 75)

  
"""
        self.assertEqual(re.sub(
            r'Time elapsed: [\d\.]+ sec', 'Time elapsed: <ignored>', test_message), re.sub(
            r'Time elapsed: [\d\.]+ sec', 'Time elapsed: <ignored>', expected))

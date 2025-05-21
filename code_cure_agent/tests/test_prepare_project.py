from autogpt.commands.sonar_qube_analysis import analyze_file_and_parse_report
from autogpt.commands.repository_operations import checkout_project

import unittest
import os
import shutil

from git.exc import GitCommandError

from tests.agent_mock import AgentMock


class CheckoutProjectTestCase(unittest.TestCase):
    def setUp(self):
        auto_gpt_workspace = "auto_gpt_workspace"
        if os.path.exists(auto_gpt_workspace):
            shutil.rmtree(auto_gpt_workspace)
        os.mkdir(auto_gpt_workspace)

    def tearDown(self):
        auto_gpt_workspace = "auto_gpt_workspace"
        if os.path.exists(auto_gpt_workspace):
            shutil.rmtree(auto_gpt_workspace)
        os.mkdir(auto_gpt_workspace)

    def test_checkout_project_master_branch(self):

        warning_repository_URL = "https://github.com/argparse4j/argparse4j.git"
        warning_repository_commit = "MASTER"
        warning_repository_name = "argparse4j"
        warning_file_path = "main/src/main/java/net/sourceforge/argparse4j/internal/TerminalWidth.java"
        warning_rule_key = "S2142"
        warning_start_line = 94
        warning_rule_name = "'InterruptedException' should not be ignored"
        warning_specific_message = "Either re-interrupt this method or rethrow the ""InterruptedException"" that can be caught here."

        agent = AgentMock(warning_repository_URL, warning_repository_commit, warning_file_path, warning_repository_name,
                          warning_rule_key, warning_start_line, warning_rule_name, warning_specific_message)

        self.assertFalse(os.path.exists(os.path.join(
            agent.config.workspace_path, warning_repository_name)))

        checkout_project(agent)

        self.assertTrue(os.path.exists(os.path.join(agent.config.workspace_path, warning_repository_name)),
                        f"Expected project folder '{warning_repository_name}' was not created.")
        self.assertTrue(os.path.exists(os.path.join(agent.config.workspace_path, warning_repository_name, warning_file_path)),
                        f"Expected file '{warning_file_path} was not present in the checked out repository '{warning_repository_name}'")

    def test_checkout_project_specific_commit(self):

        warning_repository_URL = "https://github.com/argparse4j/argparse4j.git"
        warning_repository_commit = "a0cef432451487d513382297cec2c5b14c147a30"
        warning_repository_name = "argparse4j"
        warning_file_path = "main/src/main/java/net/sourceforge/argparse4j/internal/TerminalWidth.java"
        warning_rule_key = "S2142"
        warning_start_line = 94
        warning_rule_name = "'InterruptedException' should not be ignored"
        warning_specific_message = "Either re-interrupt this method or rethrow the ""InterruptedException"" that can be caught here."

        agent = AgentMock(warning_repository_URL, warning_repository_commit, warning_file_path, warning_repository_name,
                          warning_rule_key, warning_start_line, warning_rule_name, warning_specific_message)

        self.assertFalse(os.path.exists(os.path.join(
            agent.config.workspace_path, warning_repository_name)))

        checkout_project(agent)

        self.assertTrue(os.path.exists(os.path.join(agent.config.workspace_path, warning_repository_name)),
                        f"Expected project folder '{warning_repository_name}' was not created.")
        self.assertTrue(os.path.exists(os.path.join(agent.config.workspace_path, warning_repository_name, warning_file_path)),
                        f"Expected file '{warning_file_path} was not present in the checked out repository '{warning_repository_name}'")

    def test_checkout_project_unknown_repo(self):

        warning_repository_URL = "https://github.com/argparse4j/argparse4j.git/"
        warning_repository_commit = "a0cef432451487d513382297cec2c5b14c147a30"
        warning_repository_name = "unknown_repo"
        warning_file_path = "main/src/main/java/net/sourceforge/argparse4j/internal/TerminalWidth.java"
        warning_rule_key = "S2142"
        warning_start_line = 94
        warning_rule_name = "'InterruptedException' should not be ignored"
        warning_specific_message = "Either re-interrupt this method or rethrow the ""InterruptedException"" that can be caught here."

        agent = AgentMock(warning_repository_URL, warning_repository_commit, warning_file_path, warning_repository_name,
                          warning_rule_key, warning_start_line, warning_rule_name, warning_specific_message)

        self.assertFalse(os.path.exists(os.path.join(
            agent.config.workspace_path, warning_repository_name)))

        checkout_project(agent)

        self.assertTrue(os.path.exists(os.path.join(agent.config.workspace_path, warning_repository_name)),
                        f"Expected project folder '{warning_repository_name}' was not created.")
        self.assertTrue(os.path.exists(os.path.join(agent.config.workspace_path, warning_repository_name, warning_file_path)),
                        f"Expected file '{warning_file_path} was not present in the checked out repository '{warning_repository_name}'")

    def test_checkout_project_wrong_url(self):

        warning_repository_URL = "https://github.com/argparse4j/argparse4"
        warning_repository_commit = "a0cef432451487d513382297cec2c5b14c147a30"
        warning_repository_name = "argparse4"
        warning_file_path = "main/src/main/java/net/sourceforge/argparse4j/internal/TerminalWidth.java"
        warning_rule_key = "S2142"
        warning_start_line = 94
        warning_rule_name = "'InterruptedException' should not be ignored"
        warning_specific_message = "Either re-interrupt this method or rethrow the ""InterruptedException"" that can be caught here."

        agent = AgentMock(warning_repository_URL, warning_repository_commit, warning_file_path, warning_repository_name,
                          warning_rule_key, warning_start_line, warning_rule_name, warning_specific_message)

        self.assertFalse(os.path.exists(os.path.join(
            agent.config.workspace_path, warning_repository_name)))
        with self.assertRaises(GitCommandError, msg="Should have raised a GitError, because the repository to check out doesn't exist.") as se:
            checkout_project(agent)
        self.assertEqual(se.exception.status, 128)

        self.assertFalse(os.path.exists(os.path.join(agent.config.workspace_path, warning_repository_name)),
                         f"Project folder '{warning_repository_name}' was created, but shouldn't have.")
        self.assertFalse(os.path.exists(os.path.join(agent.config.workspace_path, warning_repository_name, warning_file_path)),
                         f"File '{warning_file_path} was present in the checked out repository '{warning_repository_name}', but shouldn't have.")

    def test_checkout_project_wrong_commit(self):

        warning_repository_URL = "https://github.com/argparse4j/argparse4j.git"
        warning_repository_commit = "not_a_commit"
        warning_repository_name = "argparse4"
        warning_file_path = "main/src/main/java/net/sourceforge/argparse4j/internal/TerminalWidth.java"
        warning_rule_key = "S2142"
        warning_start_line = 94
        warning_rule_name = "'InterruptedException' should not be ignored"
        warning_specific_message = "Either re-interrupt this method or rethrow the ""InterruptedException"" that can be caught here."

        agent = AgentMock(warning_repository_URL, warning_repository_commit, warning_file_path, warning_repository_name,
                          warning_rule_key, warning_start_line, warning_rule_name, warning_specific_message)

        self.assertFalse(os.path.exists(os.path.join(
            agent.config.workspace_path, warning_repository_name)))
        with self.assertRaises(GitCommandError, msg="Should have raised a GitError, because the repository to check out doesn't exist.") as se:
            checkout_project(agent)
        self.assertEqual(se.exception.status, 1)

        self.assertTrue(os.path.exists(os.path.join(agent.config.workspace_path, warning_repository_name)),
                        f"Expected project folder '{warning_repository_name}' was not created.")
        self.assertTrue(os.path.exists(os.path.join(agent.config.workspace_path, warning_repository_name, warning_file_path)),
                        f"Expected file '{warning_file_path} was not present in the checked out repository '{warning_repository_name}'")


class AnalyzeFileTestCase(unittest.TestCase):
    def setUp(self):
        auto_gpt_workspace = "auto_gpt_workspace"
        if os.path.exists(auto_gpt_workspace):
            shutil.rmtree(auto_gpt_workspace)
        os.mkdir(auto_gpt_workspace)

        if os.path.exists("experimental_setups/experiment_test"):
            shutil.rmtree("experimental_setups/experiment_test")
        os.mkdir("experimental_setups/experiment_test")
        os.mkdir("experimental_setups/experiment_test/analysis_reports")

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
        auto_gpt_workspace = "auto_gpt_workspace"
        if os.path.exists(auto_gpt_workspace):
            shutil.rmtree(auto_gpt_workspace)
        os.mkdir(auto_gpt_workspace)
        shutil.rmtree("experimental_setups/experiment_test")

    def test_analyze_file_and_parser_report_single_rule(self):
        rules = ["S2142"]
        analysis_report_relative_path = "analysis_report.json"

        # Call the analyze_file function
        report = analyze_file_and_parse_report(self.agent.ai_config.warning_file_path, rules,
                                               self.agent.ai_config.warning_repository_name, analysis_report_relative_path, self.agent)

        self.assertIsNotNone(report)
        self.assertIsNotNone(report["minedRules"])
        self.assertTrue(len(report["minedRules"]) == 1)
        self.assertIsNotNone(report["minedRules"][0]["ruleKey"])
        self.assertEqual(report["minedRules"][0]["ruleKey"], "S2142")
        self.assertIsNotNone(report["minedRules"][0]["warningLocations"])
        self.assertEqual(len(report["minedRules"][0]["warningLocations"]), 1)
        self.assertIsNotNone(report["minedRules"][0]
                             ["warningLocations"][0]["startLine"])
        self.assertEqual(report["minedRules"][0]["warningLocations"]
                         [0]["startLine"], self.agent.ai_config.warning_start_line)

    def test_analyze_file_and_parser_report_all_rules(self):
        rules = None
        analysis_report_relative_path = "analysis_report.json"

        # Call the analyze_file function
        report = analyze_file_and_parse_report(self.agent.ai_config.warning_file_path, rules,
                                               self.agent.ai_config.warning_repository_name, analysis_report_relative_path, self.agent)

        self.assertIsNotNone(report)
        self.assertIsNotNone(report["minedRules"])
        self.assertTrue(len(report["minedRules"]) > 1)
        self.assertIsNotNone(report["minedRules"][0]["ruleKey"])
        self.assertIsNotNone(report["minedRules"][0]["warningLocations"])

    def test_analyze_file_and_parser_report_quality_profile_rules(self):

        analysis_report_relative_path = "analysis_report.json"

        # Call the analyze_file function
        report = analyze_file_and_parse_report(self.agent.ai_config.warning_file_path, self.agent.sonar_qube_rules_in_active_profile,
                                               self.agent.ai_config.warning_repository_name, analysis_report_relative_path, self.agent)

        self.assertIsNotNone(report)
        self.assertIsNotNone(report["minedRules"])
        self.assertTrue(len(report["minedRules"]) > 1)
        self.assertIsNotNone(report["minedRules"][0]["ruleKey"])
        self.assertIsNotNone(report["minedRules"][0]["warningLocations"])

    def test_analyze_file_and_parser_wrong_path(self):
        rules = ["S2142"]
        analysis_report_relative_path = "analysis_report.json"

        # Call the analyze_file function
        with self.assertRaises(ValueError) as e:
            report = analyze_file_and_parse_report("main/src/main/java/net/sourceforge/argparse4j/internal/SomeNonExistingFile.java",
                                                   rules, self.agent.ai_config.warning_repository_name, analysis_report_relative_path, self.agent)

        self.assertEqual(str(e.exception),
                         "The file_path main/src/main/java/net/sourceforge/argparse4j/internal/SomeNonExistingFile.java does not exist.")

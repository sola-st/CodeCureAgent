import os
import shutil
import unittest
from tests.agent_mock import AgentMock
from agent_core.commands import sonar_qube_docu


class SonarQubeDocuTestCase(unittest.TestCase):
    def setUp(self):
        cca_workspace = "cca_workspace"
        if os.path.exists(cca_workspace):
            shutil.rmtree(cca_workspace)
        os.mkdir(cca_workspace)

        if os.path.exists("experimental_setups/experiment_test"):
            shutil.rmtree("experimental_setups/experiment_test")
        os.mkdir("experimental_setups/experiment_test")
        os.mkdir("experimental_setups/experiment_test/fix_tp")
        os.mkdir("experimental_setups/experiment_test/fix_tp/docu_tool_outputs")
        os.mkdir("experimental_setups/experiment_test/fix_tp/execution_info")

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

    def tearDown(self):
        cca_workspace = "cca_workspace"
        if os.path.exists(cca_workspace):
            shutil.rmtree(cca_workspace)
        os.mkdir(cca_workspace)
        if os.path.exists("experimental_setups/experiment_test"):
            shutil.rmtree("experimental_setups/experiment_test")

    def test_docu_tool_bug_type_rule(self):
        rule_key = "S2142"
        docu_tool_result = sonar_qube_docu.read_sonarqube_docu(
            rule_key, self.agent)
        print(docu_tool_result)
        with open("tests/test_sonar_qube_docu_expected_outputs/expected_bug_type.txt") as expected_file:
            expected = expected_file.read()
        self.assertEqual(docu_tool_result, expected)

    def test_docu_tool_code_smell_type_rule(self):
        rule_key = "S864"
        docu_tool_result = sonar_qube_docu.read_sonarqube_docu(
            rule_key, self.agent)
        print(docu_tool_result)
        with open("tests/test_sonar_qube_docu_expected_outputs/expected_code_smell_type.txt") as expected_file:
            expected = expected_file.read()
        self.assertEqual(docu_tool_result, expected)

    # Hotspots use the desciptionSections instead of the htmlDescription in the docu object
    def test_docu_tool_security_hotspot_type_rule(self):
        rule_key = "S5852"
        docu_tool_result = sonar_qube_docu.read_sonarqube_docu(
            rule_key, self.agent)
        print(docu_tool_result)
        with open("tests/test_sonar_qube_docu_expected_outputs/expected_security_hotspot_type.txt") as expected_file:
            expected = expected_file.read()
        self.assertEqual(docu_tool_result, expected)

    def test_docu_vulnerability_type_rule(self):
        rule_key = "S6437"
        docu_tool_result = sonar_qube_docu.read_sonarqube_docu(
            rule_key, self.agent)
        print(docu_tool_result)
        with open("tests/test_sonar_qube_docu_expected_outputs/expected_vulnerability_type.txt") as expected_file:
            expected = expected_file.read()
        self.assertEqual(docu_tool_result, expected)

    def test_docu_none_existing_rule(self):
        rule_key = "S214200"
        docu_tool_result = sonar_qube_docu.read_sonarqube_docu(
            rule_key, self.agent)
        print(docu_tool_result)
        self.assertEqual(docu_tool_result, "Error: Reading the SonarQube docu for rule S214200 failed with error:  \npicocli.CommandLine$ExecutionException: The rule S214200 could not be found. Maybe you mistyped the rule key.")

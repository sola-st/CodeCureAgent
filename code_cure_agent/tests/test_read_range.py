import unittest

import os
import shutil

from tests.agent_mock import AgentMock

from agent_core.commands.repository_operations import checkout_project
from agent_core.commands.repository_reading_tools import read_range


class ReadRangeTestCase(unittest.TestCase):
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

        checkout_project(self.agent)

    def tearDown(self):
        cca_workspace = "cca_workspace"
        if os.path.exists(cca_workspace):
            shutil.rmtree(cca_workspace)
        os.mkdir(cca_workspace)
        if os.path.exists("experimental_setups/experiment_test"):
            shutil.rmtree("experimental_setups/experiment_test")

    def test_reading_existing_lines_from_warning_file_path_file(self):
        range_read: str = read_range(
            self.agent.ai_config.warning_file_path, 1, 50, self.agent)
        self.assertEqual(range_read.rfind("Line 50:            }"), 2193)

    def test_reading_line_zero_from_warning_file_path_file(self):
        range_read: str = read_range(
            self.agent.ai_config.warning_file_path, 0, 50, self.agent)
        print(range_read)
        self.assertEqual(range_read.rfind("Line 50:            }"), 2193)

    def test_reading_line_minus_one_from_warning_file_path_file(self):
        range_read = read_range(
            self.agent.ai_config.warning_file_path, -1, 50, self.agent)
        self.assertEqual(
            range_read, "Reading lines failed. start_line must be greater or equal 0.")

    def test_reading_inverted_line_range_from_warning_file_path_file(self):
        range_read = read_range(
            self.agent.ai_config.warning_file_path, 50, 1, self.agent)
        self.assertEqual(
            range_read, "Reading lines failed. end_line must be greater or equal than start_line.")

    def test_reading_lines_exceeding_file_from_warning_file_path_file(self):
        range_read = read_range(
            self.agent.ai_config.warning_file_path, 1, 200, self.agent)
        self.assertEqual(range_read[-3:], "EOF")

    def test_reading_lines_exceeding_file_by_one_from_warning_file_path_file(self):
        range_read = read_range(
            self.agent.ai_config.warning_file_path, 1, 134, self.agent)
        self.assertEqual(range_read[-3:], "EOF")

    def test_reading_lines_not_exceeding_file_by_one_from_warning_file_path_file(self):
        range_read = read_range(
            self.agent.ai_config.warning_file_path, 1, 133, self.agent)
        self.assertNotEqual(range_read[-3:], "EOF")

    def test_reading_single_line_from_warning_file_path_file(self):
        range_read: str = read_range(
            self.agent.ai_config.warning_file_path, 2, 2, self.agent)
        self.assertEqual(range_read.strip(
        ), "Line 2: * Licensed to the Apache Software Foundation (ASF) under one or more")

    def test_reading_two_lines_from_warning_file_path_file(self):
        range_read: str = read_range(
            self.agent.ai_config.warning_file_path, 2, 3, self.agent)
        self.assertEqual(range_read.strip(
        ), "Line 2: * Licensed to the Apache Software Foundation (ASF) under one or more\nLine 3: * contributor license agreements.  See the NOTICE file distributed with")

    def test_reading_lines_from_other_file_in_repo(self):
        other_file_path = "NEWS"
        range_read: str = read_range(other_file_path, 1, 8, self.agent)
        self.assertEqual(range_read.strip(
        ), "Line 1:argparse4j 0.8.0\nLine 2:================\nLine 3:\nLine 4:Release Note\nLine 5:------------\nLine 6:\nLine 7:New features have been added, and things have been improved on a lot\nLine 8:of fronts. See below.")

    def test_reading_lines_from_none_existing_file(self):
        other_file_path = "NEWS_none_existing"
        range_read: str = read_range(other_file_path, 1, 8, self.agent)

        self.assertEqual(
            range_read, "Reading lines failed. The file_path 'NEWS_none_existing' does not exist.")

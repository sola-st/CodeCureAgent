import unittest

import os
import shutil

from tests.agent_mock import AgentMock

from agent_core.commands.repository_operations import checkout_project
from agent_core.commands.repository_reading_tools import search_for_patterns


class SearchForPatternsTestCase(unittest.TestCase):
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

    def test_search_for_patterns_single_pattern(self):

        result = search_for_patterns(["command_"], self.agent)
        print(result)
        self.assertEqual(result, """Found 13 search results:

main/src/main/java/net/sourceforge/argparse4j/internal/UnrecognizedCommandException.java:35:    private final String command_;
main/src/main/java/net/sourceforge/argparse4j/internal/UnrecognizedCommandException.java:40:        command_ = command;
main/src/main/java/net/sourceforge/argparse4j/internal/UnrecognizedCommandException.java:44:        return command_;
main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java:58:    private final String command_;
main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java:78:        this.command_ = command;
main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java:355:        if (command_ != null) {
main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java:356:            opts.add(command_);
main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java:436:        if (parser.command_ != null) {
main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java:437:            opts.add(parser.command_);
main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java:1474:        return command_;
main/src/main/java/net/sourceforge/argparse4j/internal/SubparserImpl.java:48:    private final String command_;
main/src/main/java/net/sourceforge/argparse4j/internal/SubparserImpl.java:56:        command_ = command;
main/src/main/java/net/sourceforge/argparse4j/internal/SubparserImpl.java:271:            String title = "  " + command_;""")

    def test_search_for_patterns_single_pattern_no_result_found(self):

        result = search_for_patterns(["command_NotFound"], self.agent)
        print(result)
        self.assertEqual(result, """No search results found.""")

    def test_search_for_patterns_multiple_patterns(self):
        result = search_for_patterns(["command_", "try"], self.agent)
        print(result)
        self.assertEqual(result, """Found 106 search results. Only showing the first 50 results:

main/src/test/java/net/sourceforge/argparse4j/impl/type/CaseInsensitiveEnumStringArgumentTypeTest.java:74:        try {
main/src/test/java/net/sourceforge/argparse4j/impl/type/CaseInsensitiveEnumStringArgumentTypeTest.java:91:        try {
main/src/test/java/net/sourceforge/argparse4j/impl/type/BooleanArgumentTypeTest.java:58:        try {
main/src/test/java/net/sourceforge/argparse4j/impl/type/BooleanArgumentTypeTest.java:72:        try {
main/src/test/java/net/sourceforge/argparse4j/impl/type/CaseInsensitiveEnumNameArgumentTypeTest.java:69:        try {
main/src/test/java/net/sourceforge/argparse4j/impl/type/CaseInsensitiveEnumNameArgumentTypeTest.java:85:        try {
main/src/test/java/net/sourceforge/argparse4j/impl/type/EnumStringArgumentTypeTest.java:62:        try {
main/src/test/java/net/sourceforge/argparse4j/impl/type/EnumArgumentTypeTest.java:49:        try {
main/src/test/java/net/sourceforge/argparse4j/impl/type/ReflectArgumentTypeTest.java:60:        try {
main/src/test/java/net/sourceforge/argparse4j/impl/type/ReflectArgumentTypeTest.java:78:        try {
main/src/test/java/net/sourceforge/argparse4j/impl/type/ReflectArgumentTypeTest.java:93:        try {
main/src/test/java/net/sourceforge/argparse4j/impl/type/FileArgumentTypeTest.java:48:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/SubparsersImplTest.java:57:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/SubparsersImplTest.java:73:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentImplTest.java:158:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentImplTest.java:190:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentImplTest.java:196:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:105:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:116:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:129:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:139:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:150:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:197:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:203:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:309:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:323:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:335:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:349:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:360:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:377:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:389:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:396:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:407:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:418:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:433:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:505:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:633:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:676:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:714:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:736:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:750:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:772:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:784:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:799:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:806:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:829:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:842:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:954:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:976:        try {
main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java:988:        try {""")

    def test_search_for_patterns_no_patterns(self):
        result = search_for_patterns([], self.agent)
        print(result)
        self.assertEqual(
            result, "Error in search_for_patterns: The 'patterns' list was empty. Provide at least one pattern to search for.")

    def test_search_for_patterns_regular_expression_pattern(self):
        result = search_for_patterns(["command_+ !*= [a-z]+"], self.agent)
        print(result)
        self.assertEqual(result, """Found 5 search results:

main/src/main/java/net/sourceforge/argparse4j/internal/UnrecognizedCommandException.java:40:        command_ = command;
main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java:78:        this.command_ = command;
main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java:355:        if (command_ != null) {
main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java:436:        if (parser.command_ != null) {
main/src/main/java/net/sourceforge/argparse4j/internal/SubparserImpl.java:56:        command_ = command;""")

    def test_search_for_patterns_single_quote_in_pattern_handled(self):
        result = search_for_patterns(["'C'"], self.agent)
        print(result)
        self.assertEqual(result, """Found 1 search results:

main/src/test/java/net/sourceforge/argparse4j/impl/type/ReflectArgumentTypeTest.java:82:            assertEquals("argument null: could not convert 'C' (choose from {PYTHON,CPP,JAVA})",""")

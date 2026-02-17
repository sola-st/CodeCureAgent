import unittest

import os
import shutil

from agent_core.utils.agent_utils.agent_mock import AgentMock

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

    def test_search_for_patterns_in_one_file(self):

        checkout_project(self.agent)

        result = search_for_patterns(
            ['Terminal'], "main/src/main/java/net/sourceforge/argparse4j/internal/TerminalWidth.java", self.agent)
        print(result)
        self.assertEqual(result, """Found 8 search results:

main/src/main/java/net/sourceforge/argparse4j/internal/TerminalWidth.java:27: * Returns the column width of the command line terminal from which this program
main/src/main/java/net/sourceforge/argparse4j/internal/TerminalWidth.java:34:public class TerminalWidth {
main/src/main/java/net/sourceforge/argparse4j/internal/TerminalWidth.java:39:        System.out.println("terminalWidth: "
main/src/main/java/net/sourceforge/argparse4j/internal/TerminalWidth.java:40:                + new TerminalWidth().getTerminalWidth());
main/src/main/java/net/sourceforge/argparse4j/internal/TerminalWidth.java:43:    public int getTerminalWidth() {
main/src/main/java/net/sourceforge/argparse4j/internal/TerminalWidth.java:54:            return getTerminalWidth2();
main/src/main/java/net/sourceforge/argparse4j/internal/TerminalWidth.java:61:    // http://grokbase.com/t/gg/clojure/127qwgscvc/how-do-you-determine-terminal-console-width-in-%60lein-repl%60
main/src/main/java/net/sourceforge/argparse4j/internal/TerminalWidth.java:62:    private int getTerminalWidth2() throws IOException {""")

    def test_search_for_patterns_single_pattern(self):

        result = search_for_patterns(["command_"], "*.java", self.agent)
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

        result = search_for_patterns(
            ["command_NotFound"], "*.java", self.agent)
        print(result)
        self.assertEqual(result, """No search results found.""")

    def test_search_for_patterns_multiple_patterns(self):
        result = search_for_patterns(["command_", "try"], "*.java", self.agent)
        print(result)
        self.assertRegex(result, r"Found 106 search results. Only showing the first 50 results:")

    def test_search_for_patterns_no_patterns(self):
        result = search_for_patterns([], "*.java", self.agent)
        print(result)
        self.assertEqual(
            result, "Error in search_for_patterns: The 'patterns' list was empty. Provide at least one pattern to search for.")

    def test_search_for_patterns_regular_expression_pattern(self):
        result = search_for_patterns(
            ["command_+ !*= [a-z]+"], "*.java", self.agent)
        print(result)
        self.assertEqual(result, """Found 5 search results:

main/src/main/java/net/sourceforge/argparse4j/internal/UnrecognizedCommandException.java:40:        command_ = command;
main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java:78:        this.command_ = command;
main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java:355:        if (command_ != null) {
main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java:436:        if (parser.command_ != null) {
main/src/main/java/net/sourceforge/argparse4j/internal/SubparserImpl.java:56:        command_ = command;""")

    def test_search_for_patterns_single_quote_in_pattern_handled(self):
        result = search_for_patterns(["'C'"], "*.java", self.agent)
        print(result)
        self.assertEqual(result, """Found 1 search results:

main/src/test/java/net/sourceforge/argparse4j/impl/type/ReflectArgumentTypeTest.java:82:            assertEquals("argument null: could not convert 'C' (choose from {PYTHON,CPP,JAVA})",""")

    def test_search_for_patterns_in_all_files(self):
        result = search_for_patterns(["command_"], "*", self.agent)
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

    def test_search_for_patterns_in_all_files_occurences_in_non_java_file(self):
        result = search_for_patterns(["<phase>"], "*", self.agent)
        print(result)
        self.assertEqual(result, """Found 12 search results:

pom.xml:196:                        <phase>verify</phase>
main/pom.xml:83:                        <phase>site</phase>
main/pom.xml:149:                        <phase>none</phase>
main/pom.xml:171:                        <phase>compile</phase>
main/pom.xml:185:                        <phase>test-compile</phase>
main/pom.xml:211:                        <phase>verify</phase>
main/pom.xml:258:                        <phase>verify</phase>
extensions/hadoop/pom.xml:144:                        <phase>none</phase>
extensions/hadoop/pom.xml:166:                        <phase>compile</phase>
extensions/hadoop/pom.xml:180:                        <phase>test-compile</phase>
extensions/hadoop/pom.xml:206:                        <phase>verify</phase>
extensions/pom.xml:30:                        <phase>verify</phase>""")

import unittest

import os
import shutil

from tests.agent_mock import AgentMock

from autogpt.commands.repository_operations import checkout_project, remove_folder_if_exists
from autogpt.commands.method_lookup import find_definition, find_references


class MethodLookupTestCase(unittest.TestCase):
    def setUp(self):
        auto_gpt_workspace = "auto_gpt_workspace"
        remove_folder_if_exists(auto_gpt_workspace)
        os.mkdir(auto_gpt_workspace)

        if os.path.exists("experimental_setups/experiment_test"):
            shutil.rmtree("experimental_setups/experiment_test")
        os.mkdir("experimental_setups/experiment_test")
        os.mkdir("experimental_setups/experiment_test/analysis_reports")

    def tearDown(self):
        auto_gpt_workspace = "auto_gpt_workspace"
        if os.path.exists(auto_gpt_workspace):
            shutil.rmtree(auto_gpt_workspace)
        os.mkdir(auto_gpt_workspace)

    def test_go_to_definition_jpass(self):
        warning_repository_URL = "https://github.com/gaborbata/jpass.git"
        warning_repository_commit = "f52b8ae1bf2e1154aa800c784c244d3fbb63c643"
        warning_repository_name = "jpass"

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, None,
                               warning_repository_name, None, None, None, None)

        checkout_project(self.agent)

        print(find_definition(
            "src/main/java/jpass/data/DocumentRepository.java", "Entries", 114, self.agent))

    def test_go_to_definition_http_proxy_servlet(self):
        warning_repository_URL = "https://github.com/mitre/HTTP-Proxy-Servlet.git"
        warning_repository_commit = "34edf588ad9a02fecffe5efbad0d42ec592838ae"
        warning_repository_name = "HTTP-Proxy-Servlet"

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, None,
                               warning_repository_name, None, None, None, None)

        checkout_project(self.agent)

        print(find_definition(
            "src/main/java/org/mitre/dsmiley/httpproxy/URITemplateProxyServlet.java", "ATTR_QUERY_STRING", 145, self.agent))

    def test_go_to_definition_junit4(self):
        warning_repository_URL = "https://github.com/junit-team/junit4.git"
        warning_repository_commit = "7852b90cfe1cea1e0cdaa19d490c83f0d8684b50"
        warning_repository_name = "junit4"

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, None,
                               warning_repository_name, None, None, None, None)

        checkout_project(self.agent)

        print(find_definition(
            "src/test/java/junit/tests/framework/AssertTest.java", "AssertionFailedError", 26, self.agent))

    def test_go_to_definition_argparse4j_failing_due_to_broken_setup(self):
        warning_repository_URL = "https://github.com/argparse4j/argparse4j.git"
        warning_repository_commit = "a0cef432451487d513382297cec2c5b14c147a30"
        warning_repository_name = "argparse4j"

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, None,
                               warning_repository_name, None, None, None, None)

        checkout_project(self.agent)

        print(find_definition(
            "main/src/main/java/net/sourceforge/argparse4j/internal/SubparserImpl.java", "addArgument", 62, self.agent))

    def test_go_to_references_junit4(self):
        warning_repository_URL = "https://github.com/junit-team/junit4.git"
        warning_repository_commit = "7852b90cfe1cea1e0cdaa19d490c83f0d8684b50"
        warning_repository_name = "junit4"

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, None,
                               warning_repository_name, None, None, None, None)

        checkout_project(self.agent)

        print(find_references(
            "src/main/java/junit/framework/AssertionFailedError.java", "AssertionFailedError", 13, self.agent))

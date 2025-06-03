import unittest

import os
import shutil

from tests.agent_mock import AgentMock

from autogpt.commands.repository_operations import checkout_project, remove_folder_if_exists
from autogpt.commands.symbol_lookup import find_definition, find_references


class SymbolLookupTestCase(unittest.TestCase):
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

        definition_result = find_definition(
            "src/main/java/jpass/data/DocumentRepository.java", "Entries", 114, self.agent)
        print(definition_result)
        self.assertEqual(definition_result, """The definition of 'Entries' was found in file 'src/main/java/jpass/xml/bind/Entries.java' starting at line 28.  
The code of the definition is the following:  
Line 8: * <p>
Line 9: * Java class for anonymous complex type.
Line 10: *
Line 11: * <p>
Line 12: * The following schema fragment specifies the expected content contained within this class.
Line 13: *
Line 14: * <pre>
Line 15: * &lt;complexType&gt;
Line 16: *   &lt;complexContent&gt;
Line 17: *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
Line 18: *       &lt;sequence&gt;
Line 19: *         &lt;element name="entry" type="{}entry" maxOccurs="unbounded" minOccurs="0"/&gt;
Line 20: *       &lt;/sequence&gt;
Line 21: *     &lt;/restriction&gt;
Line 22: *   &lt;/complexContent&gt;
Line 23: * &lt;/complexType&gt;
Line 24: * </pre>
Line 25: *
Line 26: */
Line 27:@JacksonXmlRootElement(localName = "entries")
Line 28:public class Entries {
Line 29:
Line 30:    protected List<Entry> entry;
Line 31:
Line 32:    /**
Line 33:     * Gets the value of the entry property.
Line 34:     *
Line 35:     * <p>
Line 36:     * This accessor method returns a reference to the live list, not a snapshot. Therefore any
Line 37:     * modification you make to the returned list will be present inside the object. This is
Line 38:     * why there is not a {@code set} method for the entry property.
Line 39:     *
Line 40:     * <p>
Line 41:     * For example, to add a new item, do as follows:
Line 42:     * <pre>
Line 43:     *    getEntry().add(newItem);
Line 44:     * </pre>
Line 45:     *
Line 46:     *
Line 47:     * <p>
Line 48:     * Objects of the following type(s) are allowed in the list {@link Entry}
Line 49:     *
Line 50:     * @return list of {@link Entry} objects
Line 51:     */
Line 52:    public List<Entry> getEntry() {
Line 53:        if (entry == null) {
Line 54:            entry = new ArrayList<>();
Line 55:        }
Line 56:        return this.entry;
Line 57:    }
Line 58:
Line 59:}
""")

    def test_go_to_definition_http_proxy_servlet(self):
        warning_repository_URL = "https://github.com/mitre/HTTP-Proxy-Servlet.git"
        warning_repository_commit = "34edf588ad9a02fecffe5efbad0d42ec592838ae"
        warning_repository_name = "HTTP-Proxy-Servlet"

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, None,
                               warning_repository_name, None, None, None, None)

        checkout_project(self.agent)

        definition_result = find_definition(
            "src/main/java/org/mitre/dsmiley/httpproxy/URITemplateProxyServlet.java", "ATTR_QUERY_STRING", 145, self.agent)
        print(definition_result)

        self.assertEqual(definition_result, """The definition of 'ATTR_QUERY_STRING' was found in file 'src/main/java/org/mitre/dsmiley/httpproxy/URITemplateProxyServlet.java' starting at line 59.  
The code of the definition is the following:  
Line 59:  private static final String ATTR_QUERY_STRING =
Line 60:          URITemplateProxyServlet.class.getSimpleName() + ".queryString";
Line 61:
Line 62:  protected String targetUriTemplate;//has {name} parts
""")

    def test_go_to_definition_junit4(self):
        warning_repository_URL = "https://github.com/junit-team/junit4.git"
        warning_repository_commit = "7852b90cfe1cea1e0cdaa19d490c83f0d8684b50"
        warning_repository_name = "junit4"

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, None,
                               warning_repository_name, None, None, None, None)

        checkout_project(self.agent)

        definition_result = find_definition(
            "src/test/java/junit/tests/framework/AssertTest.java", "AssertionFailedError", 26, self.agent)
        print(definition_result)
        self.assertEqual(definition_result, """The definition of 'AssertionFailedError' was found in file 'src/main/java/junit/framework/AssertionFailedError.java' starting at line 13.  
The code of the definition is the following:  
Line 10:    /**
Line 11:     * Constructs a new AssertionFailedError without a detail message.
Line 12:     */
Line 13:    public AssertionFailedError() {
Line 14:    }
Line 15:
Line 16:    /**
Line 17:     * Constructs a new AssertionFailedError with the specified detail message.
Line 18:     * A null message is replaced by an empty String.
Line 19:     * @param message the detail message. The detail message is saved for later 
Line 20:     * retrieval by the {@code Throwable.getMessage()} method.
Line 21:     */
Line 22:    public AssertionFailedError(String message) {
""")

    def test_go_to_definition_argparse4j_failing_due_to_broken_setup(self):
        warning_repository_URL = "https://github.com/argparse4j/argparse4j.git"
        warning_repository_commit = "a0cef432451487d513382297cec2c5b14c147a30"
        warning_repository_name = "argparse4j"

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, None,
                               warning_repository_name, None, None, None, None)

        checkout_project(self.agent)

        definition_result = find_definition(
            "main/src/main/java/net/sourceforge/argparse4j/internal/SubparserImpl.java", "addArgument", 62, self.agent)
        print(definition_result)
        self.assertEqual(definition_result, """No definition could be found for 'addArgument' at line 62 in file 'main/src/main/java/net/sourceforge/argparse4j/internal/SubparserImpl.java'.  
Don't call the command with the same arguments again.""")

    def test_go_to_references_http_proxy_servlet_2_references_with_code(self):
        warning_repository_URL = "https://github.com/mitre/HTTP-Proxy-Servlet.git"
        warning_repository_commit = "34edf588ad9a02fecffe5efbad0d42ec592838ae"
        warning_repository_name = "HTTP-Proxy-Servlet"

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, None,
                               warning_repository_name, None, None, None, None)

        checkout_project(self.agent)

        references_result = find_references(
            "src/main/java/org/mitre/dsmiley/httpproxy/URITemplateProxyServlet.java", "ATTR_QUERY_STRING", 59, self.agent)
        print(references_result)
        self.assertEqual(references_result, """Found 2 references to the symbol 'ATTR_QUERY_STRING'. They are listed in the following:  
References in file 'src/main/java/org/mitre/dsmiley/httpproxy/URITemplateProxyServlet.java':  
At line 138:  
Code context:  
Line 133:        newQueryBuf.append('&');
Line 134:      newQueryBuf.append(nameVal.getKey()).append('=');
Line 135:      if (nameVal.getValue() != null)
Line 136:        newQueryBuf.append(nameVal.getValue());
Line 137:    }
Line 138:    servletRequest.setAttribute(ATTR_QUERY_STRING, newQueryBuf.toString());
Line 139:
Line 140:    super.service(servletRequest, servletResponse);
Line 141:  }
Line 142:
Line 143:  @Override
  
At line 145:  
Code context:  
Line 140:    super.service(servletRequest, servletResponse);
Line 141:  }
Line 142:
Line 143:  @Override
Line 144:  protected String rewriteQueryStringFromRequest(HttpServletRequest servletRequest, String queryString) {
Line 145:    return (String) servletRequest.getAttribute(ATTR_QUERY_STRING);
Line 146:  }
Line 147:}
  
""")

    def test_go_to_references_junit4_7_references_without_code(self):
        warning_repository_URL = "https://github.com/junit-team/junit4.git"
        warning_repository_commit = "7852b90cfe1cea1e0cdaa19d490c83f0d8684b50"
        warning_repository_name = "junit4"

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, None,
                               warning_repository_name, None, None, None, None)

        checkout_project(self.agent)

        references_result = find_references(
            "src/main/java/junit/framework/AssertionFailedError.java", "AssertionFailedError", 13, self.agent)
        print(references_result)
        self.assertEqual(references_result, """Found 7 references to the symbol 'AssertionFailedError'. They are listed in the following:  
References in file 'src/main/java/junit/framework/Assert.java':  
Line 55  
References in file 'src/test/java/junit/tests/framework/AssertTest.java':  
Line 26  
Line 38  
Line 50  
References in file 'src/test/java/junit/tests/framework/AssertionFailedErrorTest.java':  
Line 10  
References in file 'src/test/java/junit/tests/runner/TextFeedbackTest.java':  
Line 90  
References in file 'src/test/java/org/junit/tests/junit3compatibility/OldTestClassAdaptingListenerTest.java':  
Line 25  

If you want to look at the code of a reference you can use the read_range command.  """)

    def test_go_to_references_junit4_non_existing_path(self):
        warning_repository_URL = "https://github.com/junit-team/junit4.git"
        warning_repository_commit = "7852b90cfe1cea1e0cdaa19d490c83f0d8684b50"
        warning_repository_name = "junit4"

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, None,
                               warning_repository_name, None, None, None, None)

        checkout_project(self.agent)

        references_result = find_references(
            "src/main/java/junit/framework/AssertionFailedErrorNonExisting.java", "AssertionFailedError", 13, self.agent)
        print(references_result)
        self.assertEqual(
            references_result, """Lookup of references failed. The file_path src/main/java/junit/framework/AssertionFailedErrorNonExisting.java does not exist.""")

    def test_go_to_references_junit4_wrong_name_of_symbol(self):
        warning_repository_URL = "https://github.com/junit-team/junit4.git"
        warning_repository_commit = "7852b90cfe1cea1e0cdaa19d490c83f0d8684b50"
        warning_repository_name = "junit4"

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, None,
                               warning_repository_name, None, None, None, None)

        checkout_project(self.agent)

        references_result = find_references(
            "src/main/java/junit/framework/AssertionFailedError.java", "AssertionFailedErrorNonExistingName", 13, self.agent)
        print(references_result)
        self.assertEqual(references_result, """Lookup of references failed. The symbol 'AssertionFailedErrorNonExistingName' was not found in line 13 of file 'src/main/java/junit/framework/AssertionFailedError.java'.""")

    def test_go_to_references_junit4_wrong_line_of_symbol(self):
        warning_repository_URL = "https://github.com/junit-team/junit4.git"
        warning_repository_commit = "7852b90cfe1cea1e0cdaa19d490c83f0d8684b50"
        warning_repository_name = "junit4"

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, None,
                               warning_repository_name, None, None, None, None)

        checkout_project(self.agent)

        references_result = find_references(
            "src/main/java/junit/framework/AssertionFailedError.java", "AssertionFailedError", 12, self.agent)
        print(references_result)
        self.assertEqual(
            references_result, """Lookup of references failed. The symbol 'AssertionFailedError' was not found in line 12 of file 'src/main/java/junit/framework/AssertionFailedError.java'.""")

    def test_go_to_references_junit4_line_of_symbol_smaller_1(self):
        warning_repository_URL = "https://github.com/junit-team/junit4.git"
        warning_repository_commit = "7852b90cfe1cea1e0cdaa19d490c83f0d8684b50"
        warning_repository_name = "junit4"

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, None,
                               warning_repository_name, None, None, None, None)

        checkout_project(self.agent)

        references_result = find_references(
            "src/main/java/junit/framework/AssertionFailedError.java", "AssertionFailedError", 0, self.agent)
        print(references_result)
        self.assertEqual(
            references_result, """Lookup of references failed. The symbol_line was 0, but must be greater than 0.""")

    def test_go_to_references_junit4_line_of_symbol_greater_file_size(self):
        warning_repository_URL = "https://github.com/junit-team/junit4.git"
        warning_repository_commit = "7852b90cfe1cea1e0cdaa19d490c83f0d8684b50"
        warning_repository_name = "junit4"

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, None,
                               warning_repository_name, None, None, None, None)

        checkout_project(self.agent)

        references_result = find_references(
            "src/main/java/junit/framework/AssertionFailedError.java", "AssertionFailedError", 30, self.agent)
        print(references_result)
        self.assertEqual(
            references_result, """Lookup of references failed. The symbol_line 30 was out of range for the file 'src/main/java/junit/framework/AssertionFailedError.java' with 29 lines.""")

    def test_go_to_definition_junit4_non_project_symbol(self):
        warning_repository_URL = "https://github.com/junit-team/junit4.git"
        warning_repository_commit = "7852b90cfe1cea1e0cdaa19d490c83f0d8684b50"
        warning_repository_name = "junit4"

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, None,
                               warning_repository_name, None, None, None, None)

        checkout_project(self.agent)

        references_result = find_references(
            "src/main/java/junit/runner/BaseTestRunner.java", "BufferedReader", 284, self.agent)
        print(references_result)
        self.assertEqual(
            references_result, """No references could be found for 'BufferedReader' at line 284 in file 'src/main/java/junit/runner/BaseTestRunner.java'.  
Don't call the command with the same arguments again.""")

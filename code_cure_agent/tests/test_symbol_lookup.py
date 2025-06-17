import unittest

import time
import random
import os
import shutil

from tests.agent_mock import AgentMock

from agent_core.commands.repository_operations import checkout_project, remove_folder_if_exists
from agent_core.commands.symbol_lookup import find_definition, find_references


class SymbolLookupTestCase(unittest.TestCase):
    def setUp(self):
        cca_workspace = "cca_workspace"
        remove_folder_if_exists(cca_workspace)
        os.mkdir(cca_workspace)

        if os.path.exists("experimental_setups/experiment_test"):
            shutil.rmtree("experimental_setups/experiment_test")
        os.mkdir("experimental_setups/experiment_test")
        os.mkdir("experimental_setups/experiment_test/fix_tp")
        os.mkdir("experimental_setups/experiment_test/fix_tp/analysis_reports")

    def tearDown(self):
        cca_workspace = "cca_workspace"
        remove_folder_if_exists(cca_workspace)
        os.mkdir(cca_workspace)

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

    def test_go_to_definition_argparse4j_failing_due_to_broken_setup_fallback_shows_potential_defs(self):
        warning_repository_URL = "https://github.com/argparse4j/argparse4j.git"
        warning_repository_commit = "a0cef432451487d513382297cec2c5b14c147a30"
        warning_repository_name = "argparse4j"

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, None,
                               warning_repository_name, None, None, None, None)

        checkout_project(self.agent)

        definition_result = find_definition(
            "main/src/main/java/net/sourceforge/argparse4j/internal/SubparserImpl.java", "addArgument", 62, self.agent)
        print(definition_result)
        self.assertEqual(definition_result, """Searching the project for 'addArgument' found the following 5 candidate declarations of symbols (Only one of them will be the true definition you were searching for):  

In file 'main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java':  
MethodDeclaration at line 98: '    public ArgumentImpl addArgument(String... nameOrFlags) {'  
MethodDeclaration at line 102: '    public ArgumentImpl addArgument(ArgumentGroupImpl group,'  

In file 'main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentGroupImpl.java':  
MethodDeclaration at line 72: '    public ArgumentImpl addArgument(String... nameOrFlags) {'  

In file 'main/src/main/java/net/sourceforge/argparse4j/internal/SubparserImpl.java':  
MethodDeclaration at line 61: '    public Argument addArgument(String... nameOrFlags) {'  

In file 'main/src/main/java/net/sourceforge/argparse4j/inf/ArgumentContainer.java':  
MethodDeclaration at line 23: '    Argument addArgument(String... nameOrFlags);'  

You can inspect the relevant declaration (the one you think is the matching one) by using read_range.  \n""")

    def test_go_to_definition_argparse4j_field_definition_failing_due_to_broken_setup_fallback_shows_potential_defs(self):
        warning_repository_URL = "https://github.com/argparse4j/argparse4j.git"
        warning_repository_commit = "a0cef432451487d513382297cec2c5b14c147a30"
        warning_repository_name = "argparse4j"

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, None,
                               warning_repository_name, None, None, None, None)

        checkout_project(self.agent)

        definition_result = find_definition(
            "main/src/main/java/net/sourceforge/argparse4j/internal/SubparserImpl.java", "command_", 271, self.agent)
        print(definition_result)
        self.assertEqual(definition_result, """Searching the project for 'command_' found the following 3 candidate declarations of symbols (Only one of them will be the true definition you were searching for):  

In file 'main/src/main/java/net/sourceforge/argparse4j/internal/UnrecognizedCommandException.java':  
FieldDeclaration at line 35: '    private final String command_;'  

In file 'main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java':  
FieldDeclaration at line 58: '    private final String command_;'  

In file 'main/src/main/java/net/sourceforge/argparse4j/internal/SubparserImpl.java':  
FieldDeclaration at line 48: '    private final String command_;'  

You can inspect the relevant declaration (the one you think is the matching one) by using read_range.  \n""")

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
At line 55: '            throw new AssertionFailedError();'  

References in file 'src/test/java/junit/tests/framework/AssertTest.java':  
At line 26: '        throw new AssertionFailedError();'  
At line 38: '        throw new AssertionFailedError();'  
At line 50: '        throw new AssertionFailedError();'  

References in file 'src/test/java/junit/tests/framework/AssertionFailedErrorTest.java':  
At line 10: '        AssertionFailedError error = new AssertionFailedError();'  

References in file 'src/test/java/junit/tests/runner/TextFeedbackTest.java':  
At line 90: '                throw new AssertionFailedError();'  

References in file 'src/test/java/org/junit/tests/junit3compatibility/OldTestClassAdaptingListenerTest.java':  
At line 25: '        adaptingListener.addFailure(testCase, new AssertionFailedError());'  

If you want to look at the code of a reference you can use the read_range command.  """)

    def test_go_to_references_argparse4j_failing_due_to_broken_setup_fallback_shows_potential_refs(self):
        warning_repository_URL = "https://github.com/argparse4j/argparse4j.git"
        warning_repository_commit = "a0cef432451487d513382297cec2c5b14c147a30"
        warning_repository_name = "argparse4j"

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, None,
                               warning_repository_name, None, None, None, None)

        checkout_project(self.agent)

        references_result = find_references(
            "main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java", "addArgument", 98, self.agent)
        print(references_result)
        self.assertEqual(references_result, """Searching the project for 'addArgument' found the following 175 candidate references of the symbol by searching for the symbol name (Not all of them are necessarily true references to the symbol):  

In file 'main/src/test/java/net/sourceforge/argparse4j/ArgumentParsersTest.java':  
MethodInvocation at line 43: '        ap.addArgument("+h");'  

In file 'main/src/test/java/net/sourceforge/argparse4j/impl/type/FileVerificationOrTest.java':  
MethodInvocation at line 18: '    private static final Argument SOME_ARGUMENT = SOME_PARSER'  

In file 'main/src/test/java/net/sourceforge/argparse4j/internal/ArgumentParserImplTest.java':  
MethodInvocation at line 87: '        ap.addArgument("+h");'  
MethodInvocation at line 92: '        ap.addArgument("--foo");'  
MethodInvocation at line 93: '        ap.addArgument("--bar").nargs("?").setConst("c");'  
MethodInvocation at line 94: '        ap.addArgument("suites").nargs("*");'  
MethodInvocation at line 104: '        ap.addArgument("--foo").required(true);'  
MethodInvocation at line 115: '        ap.addArgument("--foo").nargs("+").choices("bar", "baz");'  
MethodInvocation at line 127: '        ap.addArgument("--foo").required(true);'  
MethodInvocation at line 138: '        ap.addArgument("foo");'  
MethodInvocation at line 149: '        ap.addArgument("foo").nargs(3);'  
MethodInvocation at line 160: '        ap.addArgument("--foo");'  
MethodInvocation at line 161: '        ap.addArgument("--foo");'  
MethodInvocation at line 166: '        ap.addArgument("foo");'  
MethodInvocation at line 167: '        ap.addArgument("foo");'  
MethodInvocation at line 173: '        ap.addArgument("foo").nargs("*");'  
MethodInvocation at line 181: '        ap.addArgument("-x");'  
MethodInvocation at line 182: '        ap.addArgument("foo").nargs("?");'  
MethodInvocation at line 193: '        ap.addArgument("-1").dest("one");'  
MethodInvocation at line 194: '        ap.addArgument("foo").nargs("?");'  
MethodInvocation at line 214: '        ap.addArgument("--foo").action(storeTrue());'  
MethodInvocation at line 215: '        ap.addArgument("--bar").action(storeFalse());'  
MethodInvocation at line 216: '        ap.addArgument("--baz").action(storeFalse());'  
MethodInvocation at line 217: '        ap.addArgument("--sid").action(storeTrue());'  
MethodInvocation at line 227: '        ap.addArgument("--foo").action(storeConst()).setConst("const");'  
MethodInvocation at line 228: '        ap.addArgument("bar");'  
MethodInvocation at line 236: '        ap.addArgument("--foo").action(append()).nargs("*");'  
MethodInvocation at line 237: '        ap.addArgument("--bar").action(append());'  
MethodInvocation at line 246: '        ap.addArgument("--foo").action(appendConst()).setConst("X");'  
MethodInvocation at line 247: '        ap.addArgument("bar");'  
MethodInvocation at line 255: '        ap.addArgument("-v", "--verbose").action(count());'  
MethodInvocation at line 256: '        ap.addArgument("--foo");'  
MethodInvocation at line 263: '        ap.addArgument("--foo").setConst("X").nargs("?");'  
MethodInvocation at line 270: '        ap.addArgument("--foo").nargs("?").setConst("c").setDefault("d");'  
MethodInvocation at line 271: '        ap.addArgument("bar").nargs("?").setDefault("d");'  
MethodInvocation at line 286: '        ap.addArgument("-1").action(storeTrue());'  
MethodInvocation at line 287: '        ap.addArgument("-2");'  
MethodInvocation at line 288: '        ap.addArgument("-3");'  
MethodInvocation at line 289: '        ap.addArgument("-ff");'  
MethodInvocation at line 290: '        ap.addArgument("-f");'  
MethodInvocation at line 291: '        ap.addArgument("-c").action(appendConst()).setConst(true);'  
MethodInvocation at line 320: '        ap.addArgument("--foo").choices("chocolate", "icecream", "froyo");'  
MethodInvocation at line 333: '        ap.addArgument("--foo").nargs(2);'  
MethodInvocation at line 334: '        ap.addArgument("bar");'  
MethodInvocation at line 345: '        ap.addArgument("--port").type(Integer.class)'  
MethodInvocation at line 359: '        ap.addArgument("--input").type(FileInputStream.class);'  
MethodInvocation at line 373: '        ap.addArgument("-a").action(Arguments.storeTrue());'  
MethodInvocation at line 374: '        ap.addArgument("-b").action(Arguments.storeTrue());'  
MethodInvocation at line 375: '        ap.addArgument("-c").action(Arguments.storeTrue());'  
MethodInvocation at line 376: '        ap.addArgument("-d").action(Arguments.storeTrue());'  
MethodInvocation at line 432: '        ap.addArgument("-a").action(Arguments.storeTrue());'  
MethodInvocation at line 450: '        ap.addArgument("-f");'  
MethodInvocation at line 451: '        ap.addArgument("--baz").nargs(2);'  
MethodInvocation at line 452: '        ap.addArgument("x");'  
MethodInvocation at line 453: '        ap.addArgument("y").nargs(2);'  
MethodInvocation at line 456: '        subparser.addArgument("--foo");'  
MethodInvocation at line 457: '        subparser.addArgument("--bar").action(Arguments.storeTrue());'  
MethodInvocation at line 469: '        ap.addArgument("-f");'  
MethodInvocation at line 472: '        parserA.addArgument("pkg1");'  
MethodInvocation at line 475: '        parserB.addArgument("pkg2");'  
MethodInvocation at line 516: '        ap.addArgument("-f").setDefault("foo");'  
MethodInvocation at line 517: '        ap.addArgument("-g").setDefault("bar");'  
MethodInvocation at line 518: '        ap.addArgument("-i").setDefault("alpha");'  
MethodInvocation at line 528: '        ap.addArgument("--foo").nargs("*");'  
MethodInvocation at line 529: '        ap.addArgument("--bar").nargs("*").setDefault("bar");'  
MethodInvocation at line 530: '        ap.addArgument("--baz").nargs("*").action(append());'  
MethodInvocation at line 531: '        ap.addArgument("--buzz").nargs("*").action(append()).setDefault("buzz");'  
MethodInvocation at line 564: '        ap.addArgument("foo").nargs("*");'  
MethodInvocation at line 569: '        ap.addArgument("foo").nargs("*").setDefault("foo");'  
MethodInvocation at line 578: '        ap.addArgument("foo").nargs("*").action(append());'  
MethodInvocation at line 584: '        ap.addArgument("foo").nargs("*").action(append()).setDefault("foo");'  
MethodInvocation at line 612: '        ap.addArgument("f").nargs("*").setDefault(singletonList("default"));'  
MethodInvocation at line 614: '        ap.addArgument("b").nargs("*").setDefault(false).action(action);'  
MethodInvocation at line 623: '        ap.addArgument("-f");'  
MethodInvocation at line 624: '        ap.addArgument("-g").setDefault(SUPPRESS);'  
MethodInvocation at line 632: '        ap.addArgument("foo");'  
MethodInvocation at line 644: '        ap.addArgument("--foo");'  
MethodInvocation at line 645: '        ap.addArgument("-2");'  
MethodInvocation at line 646: '        ap.addArgument("bar");'  
MethodInvocation at line 647: '        ap.addArgument("car");'  
MethodInvocation at line 655: '        ap.addArgument("a");'  
MethodInvocation at line 656: '        ap.addArgument("b").nargs("*");'  
MethodInvocation at line 657: '        ap.addArgument("c").nargs(2);'  
MethodInvocation at line 658: '        ap.addArgument("d").nargs("?");'  
MethodInvocation at line 659: '        ap.addArgument("e");'  
MethodInvocation at line 660: '        ap.addArgument("f").nargs("*").setDefault("f1", "f2");'  
MethodInvocation at line 674: '        ap.addArgument("a");'  
MethodInvocation at line 675: '        ap.addArgument("b").nargs("+");'  
MethodInvocation at line 686: '        ap.addArgument("foo").nargs("*").type(int.class);'  
MethodInvocation at line 693: '        ap.addArgument("--foo");'  
MethodInvocation at line 694: '        ap.addArgument("bar").nargs("*");'  
MethodInvocation at line 703: '        group.addArgument("--foo");'  
MethodInvocation at line 704: '        group.addArgument("--bar");'  
MethodInvocation at line 712: '        group.addArgument("--foo");'  
MethodInvocation at line 713: '        group.addArgument("--bar");'  
MethodInvocation at line 725: '        group.addArgument("--foo");'  
MethodInvocation at line 726: '        group.addArgument("--bar");'  
MethodInvocation at line 734: '        group.addArgument("--foo");'  
MethodInvocation at line 735: '        group.addArgument("--bar");'  
MethodInvocation at line 747: '        group.addArgument("-a").action(Arguments.storeTrue());'  
MethodInvocation at line 748: '        group.addArgument("-b").action(Arguments.storeTrue());'  
MethodInvocation at line 749: '        ap.addArgument("-c").action(Arguments.storeTrue());'  
MethodInvocation at line 760: '        group.addArgument("-a").action(Arguments.storeTrue());'  
MethodInvocation at line 761: '        group.addArgument("-b").action(Arguments.storeTrue());'  
MethodInvocation at line 762: '        ap.addArgument("-c");'  
MethodInvocation at line 796: '        mutex1.addArgument("-a").help(Arguments.SUPPRESS);'  
MethodInvocation at line 797: '        Argument b = mutex1.addArgument("-b");'  
MethodInvocation at line 817: '        ap.addArgument("-a").action(storeTrue());'  
MethodInvocation at line 818: '        ap.addArgument("-b");'  
MethodInvocation at line 819: '        ap.addArgument("-aaa").action(storeTrue());'  
MethodInvocation at line 820: '        ap.addArgument("-bbb").action(storeTrue());'  
MethodInvocation at line 854: '        ap.addArgument("-a").required(true);'  
MethodInvocation at line 855: '        ap.addArgument("b");'  
MethodInvocation at line 856: '        ap.addMutuallyExclusiveGroup().required(true).addArgument("-c");'  
MethodInvocation at line 867: '        ap.addArgument("+a").action(Arguments.storeTrue());'  
MethodInvocation at line 868: '        ap.addArgument("+b");'  
MethodInvocation at line 878: '        ap.addArgument("-f");'  
MethodInvocation at line 889: '        ap.addArgument("-f");'  
MethodInvocation at line 890: '        ap.addArgument("-g");'  
MethodInvocation at line 891: '        ap.addArgument("+a").action(Arguments.storeTrue());'  
MethodInvocation at line 892: '        ap.addArgument("path");'  
MethodInvocation at line 939: '        install.addArgument("-f");'  
MethodInvocation at line 952: '        ap.addSubparsers().addParser("install").addArgument("+f");'  
MethodInvocation at line 953: '        ap.addSubparsers().addParser("check", true, "-").addArgument("-f");'  
MethodInvocation at line 955: '            ap.addSubparsers().addParser("test", true, "-").addArgument("+f", "++f");'  
MethodInvocation at line 966: '        subparsers.addParser("install").addArgument("--command")'  
MethodInvocation at line 998: '        ap.addArgument("--foo").setDefault("alpha");'  
MethodInvocation at line 1035: '        ap.addArgument("--username");'  
MethodInvocation at line 1036: '        ap.addArgument("--host");'  
MethodInvocation at line 1037: '        ap.addArgument("--attrs").nargs("*").type(Integer.class);'  
MethodInvocation at line 1050: '        ap.addArgument("--port").type(Integer.class);'  
MethodInvocation at line 1065: '        ap.addArgument("-a");'  
MethodInvocation at line 1066: '        ap.addArgument("-b").required(true);'  
MethodInvocation at line 1068: '        group.addArgument("-c").required(true);'  
MethodInvocation at line 1069: '        group.addArgument("-d").required(true);'  
MethodInvocation at line 1070: '        ap.addArgument("file");'  
MethodInvocation at line 1076: '        fooSub.addArgument("hash");'  
MethodInvocation at line 1106: '        group.addArgument("--foo");'  
MethodInvocation at line 1128: '        group.addArgument("--foo");'  
MethodInvocation at line 1150: '        group1.addArgument("foo").help("foo help");'  
MethodInvocation at line 1153: '        group2.addArgument("--bar").help("bar help");'  
MethodInvocation at line 1176: '        group.addArgument("--foo");'  
MethodInvocation at line 1177: '        group.addArgument("--bar");'  
MethodInvocation at line 1201: '        group.addArgument("--foo");'  
MethodInvocation at line 1202: '        ap.addArgument("-b").action(Arguments.storeTrue());'  
MethodInvocation at line 1237: '        ap.defaultHelp(true).addArgument("--foo").setDefault("alpha");'  
MethodInvocation at line 1251: '        group.addArgument("--foo");'  
MethodInvocation at line 1252: '        group.addArgument("--bar").help(Arguments.SUPPRESS);'  
MethodInvocation at line 1253: '        ap.addArgument("-a").help(Arguments.SUPPRESS).required(true);'  
MethodInvocation at line 1254: '        ap.addArgument("-b");'  
MethodInvocation at line 1256: '        mutex1.addArgument("-c").help(Arguments.SUPPRESS);'  
MethodInvocation at line 1257: '        mutex1.addArgument("-d");'  
MethodInvocation at line 1260: '        mutex2.addArgument("-e").help(Arguments.SUPPRESS);'  
MethodInvocation at line 1261: '        mutex2.addArgument("-f");'  
MethodInvocation at line 1262: '        mutex2.addArgument("-g");'  
MethodInvocation at line 1263: '        ap.addArgument("s");'  
MethodInvocation at line 1264: '        ap.addArgument("t");'  
MethodInvocation at line 1265: '        ap.addArgument("u").help(Arguments.SUPPRESS);'  
MethodInvocation at line 1268: '        sap.addArgument("-i").help(Arguments.SUPPRESS);'  
MethodInvocation at line 1269: '        sap.addArgument("-j");'  
MethodInvocation at line 1309: '        ap.addArgument("--bar");'  
MethodInvocation at line 1314: '        parser.addArgument("--foo");'  
MethodInvocation at line 1331: '        ap.addArgument("--bar");'  
MethodInvocation at line 1334: '        parser.addArgument("--foo").setDefault("alpha");'  
MethodInvocation at line 1463: '        ap.addArgument("bar").help(h);'  
MethodInvocation at line 1464: '        ap.addArgument("verylonglongpositionalargument").help(h);'  
MethodInvocation at line 1465: '        ap.addArgument("-f", "--foo").help(h);'  
MethodInvocation at line 1466: '        ap.addArgument("-1", "--1").metavar("X").nargs(2).help(h);'  
MethodInvocation at line 1467: '        ap.addArgument("-2").metavar("X").nargs("*").help(h);'  
MethodInvocation at line 1468: '        ap.addArgument("-3").metavar("X").nargs("+").help(h);'  
MethodInvocation at line 1469: '        ap.addArgument("-4").metavar("X").nargs("?").help(h);'  

In file 'main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java':  
MethodInvocation at line 90: '            addArgument(prefix + "h", prefix + prefix + "help")'  
MethodInvocation at line 99: '        return addArgument(null, nameOrFlags);'  

In file 'main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentGroupImpl.java':  
MethodInvocation at line 73: '        ArgumentImpl arg = argumentParser_.addArgument(this, nameOrFlags);'  

In file 'main/src/main/java/net/sourceforge/argparse4j/internal/SubparserImpl.java':  
MethodInvocation at line 62: '        return parser_.addArgument(nameOrFlags);'  

You can inspect the relevant references (the ones you think are true matches) by using read_range.  \n""")

    def test_go_to_references_argparse4j_field_lookup_failing_due_to_broken_setup_fallback_shows_potential_refs(self):
        warning_repository_URL = "https://github.com/argparse4j/argparse4j.git"
        warning_repository_commit = "a0cef432451487d513382297cec2c5b14c147a30"
        warning_repository_name = "argparse4j"

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, None,
                               warning_repository_name, None, None, None, None)

        checkout_project(self.agent)

        references_result = find_references(
            "main/src/main/java/net/sourceforge/argparse4j/internal/SubparserImpl.java", "command_", 48, self.agent)
        print(references_result)
        self.assertEqual(references_result, """Searching the project for 'command_' found the following 10 candidate references of the symbol by searching for the symbol name (Not all of them are necessarily true references to the symbol):  

In file 'main/src/main/java/net/sourceforge/argparse4j/internal/UnrecognizedCommandException.java':  
MemberReference at line 40: '        command_ = command;'  
MemberReference at line 44: '        return command_;'  

In file 'main/src/main/java/net/sourceforge/argparse4j/internal/ArgumentParserImpl.java':  
MemberReference at line 78: '        this.command_ = command;'  
MemberReference at line 355: '        if (command_ != null) {'  
MemberReference at line 356: '            opts.add(command_);'  
MemberReference at line 436: '        if (parser.command_ != null) {'  
MemberReference at line 437: '            opts.add(parser.command_);'  
MemberReference at line 1474: '        return command_;'  

In file 'main/src/main/java/net/sourceforge/argparse4j/internal/SubparserImpl.java':  
MemberReference at line 56: '        command_ = command;'  
MemberReference at line 271: '            String title = "  " + command_;'  

You can inspect the relevant references (the ones you think are true matches) by using read_range.  \n""")

    def test_multiple_go_to_ref_and_go_to_def_in_one_run(self):
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

        time.sleep(random.randint(0, 15))

        references_result = find_references(
            "src/main/java/junit/framework/AssertionFailedError.java", "AssertionFailedError", 13, self.agent)
        print(references_result)

        self.assertEqual(references_result, """Found 7 references to the symbol 'AssertionFailedError'. They are listed in the following:  

References in file 'src/main/java/junit/framework/Assert.java':  
At line 55: '            throw new AssertionFailedError();'  

References in file 'src/test/java/junit/tests/framework/AssertTest.java':  
At line 26: '        throw new AssertionFailedError();'  
At line 38: '        throw new AssertionFailedError();'  
At line 50: '        throw new AssertionFailedError();'  

References in file 'src/test/java/junit/tests/framework/AssertionFailedErrorTest.java':  
At line 10: '        AssertionFailedError error = new AssertionFailedError();'  

References in file 'src/test/java/junit/tests/runner/TextFeedbackTest.java':  
At line 90: '                throw new AssertionFailedError();'  

References in file 'src/test/java/org/junit/tests/junit3compatibility/OldTestClassAdaptingListenerTest.java':  
At line 25: '        adaptingListener.addFailure(testCase, new AssertionFailedError());'  

If you want to look at the code of a reference you can use the read_range command.  """)

        time.sleep(random.randint(0, 15))

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

        time.sleep(random.randint(0, 15))

        references_result = find_references(
            "src/main/java/junit/framework/AssertionFailedError.java", "AssertionFailedError", 13, self.agent)
        print(references_result)

        self.assertEqual(references_result, """Found 7 references to the symbol 'AssertionFailedError'. They are listed in the following:  

References in file 'src/main/java/junit/framework/Assert.java':  
At line 55: '            throw new AssertionFailedError();'  

References in file 'src/test/java/junit/tests/framework/AssertTest.java':  
At line 26: '        throw new AssertionFailedError();'  
At line 38: '        throw new AssertionFailedError();'  
At line 50: '        throw new AssertionFailedError();'  

References in file 'src/test/java/junit/tests/framework/AssertionFailedErrorTest.java':  
At line 10: '        AssertionFailedError error = new AssertionFailedError();'  

References in file 'src/test/java/junit/tests/runner/TextFeedbackTest.java':  
At line 90: '                throw new AssertionFailedError();'  

References in file 'src/test/java/org/junit/tests/junit3compatibility/OldTestClassAdaptingListenerTest.java':  
At line 25: '        adaptingListener.addFailure(testCase, new AssertionFailedError());'  

If you want to look at the code of a reference you can use the read_range command.  """)

        time.sleep(random.randint(0, 15))

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
            references_result, """Error in find_references. The file_path src/main/java/junit/framework/AssertionFailedErrorNonExisting.java does not exist.""")

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
        self.assertEqual(references_result, """Error in find_references. There is no symbol 'AssertionFailedErrorNonExistingName' in line 13 of file 'src/main/java/junit/framework/AssertionFailedError.java'. Maybe you accidentally used a wrong symbol_line or used a wrong symbol. An occurence of your given symbol, which you want to find references for, must exist at your given line of the file. Else the find_references command doesn't work.""")

    def test_go_to_references_junit4_wrong_line_of_symbol(self):
        warning_repository_URL = "https://github.com/junit-team/junit4.git"
        warning_repository_commit = "7852b90cfe1cea1e0cdaa19d490c83f0d8684b50"
        warning_repository_name = "junit4"

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, None,
                               warning_repository_name, None, None, None, None)

        checkout_project(self.agent)

        references_result = find_references(
            "src/main/java/junit/framework/AssertionFailedError.java", "AssertionFailedError", 15, self.agent)
        print(references_result)
        self.assertEqual(
            references_result, """Error in find_references. There is no symbol 'AssertionFailedError' in line 15 of file 'src/main/java/junit/framework/AssertionFailedError.java'. Maybe you accidentally used a wrong symbol_line or used a wrong symbol. An occurence of your given symbol, which you want to find references for, must exist at your given line of the file. Else the find_references command doesn't work.""")

    def test_go_to_references_junit4_line_of_symbol_off_by_one_so_also_found(self):
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
            references_result, """Found 7 references to the symbol 'AssertionFailedError'. They are listed in the following:  

References in file 'src/main/java/junit/framework/Assert.java':  
At line 55: '            throw new AssertionFailedError();'  

References in file 'src/test/java/junit/tests/framework/AssertTest.java':  
At line 26: '        throw new AssertionFailedError();'  
At line 38: '        throw new AssertionFailedError();'  
At line 50: '        throw new AssertionFailedError();'  

References in file 'src/test/java/junit/tests/framework/AssertionFailedErrorTest.java':  
At line 10: '        AssertionFailedError error = new AssertionFailedError();'  

References in file 'src/test/java/junit/tests/runner/TextFeedbackTest.java':  
At line 90: '                throw new AssertionFailedError();'  

References in file 'src/test/java/org/junit/tests/junit3compatibility/OldTestClassAdaptingListenerTest.java':  
At line 25: '        adaptingListener.addFailure(testCase, new AssertionFailedError());'  

If you want to look at the code of a reference you can use the read_range command.  """)

    def test_go_to_references_junit4_line_of_symbol_leads_to_timeout_then_fallback(self):
        warning_repository_URL = "https://github.com/junit-team/junit4.git"
        warning_repository_commit = "7852b90cfe1cea1e0cdaa19d490c83f0d8684b50"
        warning_repository_name = "junit4"

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, None,
                               warning_repository_name, None, None, None, None)

        checkout_project(self.agent)

        references_result = find_references(
            "src/main/java/junit/framework/AssertionFailedError.java", "AssertionFailedError", 11, self.agent)
        print(references_result)
        self.assertEqual(
            references_result, """Searching the project for 'AssertionFailedError' found the following 10 candidate references of the symbol by searching for the symbol name (Not all of them are necessarily true references to the symbol):  

In file 'src/test/java/junit/tests/framework/AssertTest.java':  
ClassCreator at line 26: '        throw new AssertionFailedError();'  
ClassCreator at line 38: '        throw new AssertionFailedError();'  
ClassCreator at line 50: '        throw new AssertionFailedError();'  

In file 'src/test/java/junit/tests/framework/AssertionFailedErrorTest.java':  
ClassCreator at line 10: '        AssertionFailedError error = new AssertionFailedError();'  
ClassCreator at line 15: '        AssertionFailedError error = new AssertionFailedError(ARBITRARY_MESSAGE);'  
ClassCreator at line 20: '        AssertionFailedError error = new AssertionFailedError(null);'  

In file 'src/test/java/junit/tests/runner/TextFeedbackTest.java':  
ClassCreator at line 90: '                throw new AssertionFailedError();'  

In file 'src/test/java/org/junit/tests/junit3compatibility/OldTestClassAdaptingListenerTest.java':  
ClassCreator at line 25: '        adaptingListener.addFailure(testCase, new AssertionFailedError());'  

In file 'src/main/java/junit/framework/Assert.java':  
ClassCreator at line 55: '            throw new AssertionFailedError();'  
ClassCreator at line 57: '        throw new AssertionFailedError(message);'  

You can inspect the relevant references (the ones you think are true matches) by using read_range.  \n""")

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
            references_result, """Error in find_references. The symbol_line was 0, but must be greater than 0.""")

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
            references_result, """Error in find_references. The symbol_line 30 was out of range for the file 'src/main/java/junit/framework/AssertionFailedError.java' with 29 lines.""")

    def test_go_to_references_junit4_line_of_symbol_last_line_and_wrong(self):
        warning_repository_URL = "https://github.com/junit-team/junit4.git"
        warning_repository_commit = "7852b90cfe1cea1e0cdaa19d490c83f0d8684b50"
        warning_repository_name = "junit4"

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, None,
                               warning_repository_name, None, None, None, None)

        checkout_project(self.agent)

        references_result = find_references(
            "src/main/java/junit/framework/AssertionFailedError.java", "AssertionFailedError", 29, self.agent)
        print(references_result)
        self.assertEqual(
            references_result, """Error in find_references. There is no symbol 'AssertionFailedError' in line 29 of file 'src/main/java/junit/framework/AssertionFailedError.java'. Maybe you accidentally used a wrong symbol_line or used a wrong symbol. An occurence of your given symbol, which you want to find references for, must exist at your given line of the file. Else the find_references command doesn't work.""")

    def test_go_to_references_junit4_non_project_symbol(self):
        warning_repository_URL = "https://github.com/junit-team/junit4.git"
        warning_repository_commit = "7852b90cfe1cea1e0cdaa19d490c83f0d8684b50"
        warning_repository_name = "junit4"

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, None,
                               warning_repository_name, None, None, None, None)

        checkout_project(self.agent)

        references_result = find_references(
            "src/main/java/junit/framework/AssertionFailedError.java", "super", 23, self.agent)
        print(references_result)
        self.assertEqual(
            references_result, """No references could be found for 'super' at line 23 in file 'src/main/java/junit/framework/AssertionFailedError.java'.  
Don't call the command with the same arguments again.""")

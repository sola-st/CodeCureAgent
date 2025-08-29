/**
 * Copyright (C) 2008 Mycila (mathieu.carbou@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mycila.maven.plugin.license;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.mycila.maven.plugin.license.util.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.junit.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class UpdateMojoTest {

    private static final String TEST_RESOURCES_UPDATE_DOC1_TXT = "src/test/resources/update/doc1.txt";
    private static final String TEST_RESOURCES_UPDATE_DOC2_TXT = "src/test/resources/update/doc2.txt";
    private static final String TEST_RESOURCES_UPDATE_HEADER_TXT = "src/test/resources/update/header.txt";
    private static final String FILE_ENCODING_PROPERTY = System.getProperty("file.encoding");
    private static final String UTF_8 = "utf-8";
    private static final String TEST_RESOURCES_UPDATE_ISSUE50_TEST1_PROPERTIES = "src/test/resources/update/issue50/test1.properties";
    private static final String TEST_RESOURCES_UPDATE_ISSUE50_TEST2_PROPERTIES = "src/test/resources/update/issue50/test2.properties";
    private static final String TEST_RESOURCES_UPDATE_ISSUE50_TEST3_PROPERTIES = "src/test/resources/update/issue50/test3.properties";
    private static final String TEST_RESOURCES_UPDATE_ISSUE48_TEST1_PHP = "src/test/resources/update/issue48/test1.php";
    private static final String TEST_RESOURCES_UPDATE_ISSUE48_TEST2_PHP = "src/test/resources/update/issue48/test2.php";
    private static final String TEST_RESOURCES_UPDATE_ISSUE44_ISSUE44_3_RB = "src/test/resources/update/issue44/issue44-3.rb";
    private static final String TEST_RESOURCES_UPDATE_ISSUE44_TEST_ASP = "src/test/resources/update/issue44/test.asp";
    private static final String TEST_RESOURCES_UPDATE_ISSUE14_TEST_PROPERTIES = "src/test/resources/update/issue14/test.properties";
    private static final String TEST_RESOURCES_ISSUES_ISSUE_71_ISSUE_71_TXT_EXTENDED = "src/test/resources/issues/issue-71/issue-71.txt.extended";
    private static final String TEST_RESOURCES_ISSUES_ISSUE_71_ISSUE_71_HEADER_TXT = "src/test/resources/issues/issue-71/issue-71-header.txt";
    private static final String TEST_RESOURCES_UPDATE_ISSUE37_XWIKI_XML = "src/test/resources/update/issue37/xwiki.xml";
    private static final String TEST_RESOURCES_UPDATE_ISSUE37_XWIKI_LICENSE_TXT = "src/test/resources/update/issue37/xwiki-license.txt";
    private static final String TEST_RESOURCES_UPDATE_ISSUE30_ONE_LINE_COMMENT_FTL = "src/test/resources/update/issue30/one-line-comment.ftl";
    private static final String TEST_RESOURCES_SINGLE_LINE_HEADER_TXT = "src/test/resources/single-line-header.txt";
    public static final String LS = "\n";
    
    @Test
    public void test_update() throws Exception {
        File tmp = new File("target/test/update");
        tmp.mkdirs();
        FileUtils.copyFileToFolder(new File(TEST_RESOURCES_UPDATE_DOC1_TXT), tmp);
        FileUtils.copyFileToFolder(new File(TEST_RESOURCES_UPDATE_DOC2_TXT), tmp);

        LicenseFormatMojo updater = new LicenseFormatMojo();
        updater.defaultBasedir = tmp;
        updater.legacyConfigHeader = TEST_RESOURCES_UPDATE_HEADER_TXT;
        updater.project = new MavenProjectStub();
        updater.defaultProperties = ImmutableMap.of("year", "2008");
        updater.execute();

        assertEquals(FileUtils.read(new File(tmp, "doc1.txt"), FILE_ENCODING_PROPERTY), "====\r\n    My @Copyright license 2 with my-custom-value and 2008 and doc1.txt\r\n====\r\n\r\nsome data\r\n");
        assertEquals(FileUtils.read(new File(tmp, "doc2.txt"), FILE_ENCODING_PROPERTY), "====\r\n    My @Copyright license 2 with my-custom-value and 2008 and doc2.txt\r\n====\r\n\r\nsome data\r\n");
    }

    @Test
    public void test_update_inlineHeader() throws Exception {
        File tmp = new File("target/test/update-inlineHeader");
        tmp.mkdirs();
        FileUtils.copyFileToFolder(new File(TEST_RESOURCES_UPDATE_DOC1_TXT), tmp);
        FileUtils.copyFileToFolder(new File(TEST_RESOURCES_UPDATE_DOC2_TXT), tmp);

        LicenseFormatMojo updater = new LicenseFormatMojo();
        updater.defaultBasedir = tmp;
        updater.legacyConfigInlineHeader = FileUtils.read(new File(TEST_RESOURCES_UPDATE_HEADER_TXT), UTF_8);
        updater.project = new MavenProjectStub();
        updater.defaultProperties = ImmutableMap.of("year", "2008");
        updater.execute();

        assertEquals(FileUtils.read(new File(tmp, "doc1.txt"), FILE_ENCODING_PROPERTY), "====\r\n    My @Copyright license 2 with my-custom-value and 2008 and doc1.txt\r\n====\r\n\r\nsome data\r\n");
        assertEquals(FileUtils.read(new File(tmp, "doc2.txt"), FILE_ENCODING_PROPERTY), "====\r\n    My @Copyright license 2 with my-custom-value and 2008 and doc2.txt\r\n====\r\n\r\nsome data\r\n");
    }

    @Test
    public void test_skipExistingHeaders() throws Exception {
        File tmp = new File("target/test/test_skipExistingHeaders");
        tmp.mkdirs();
        FileUtils.copyFileToFolder(new File(TEST_RESOURCES_UPDATE_DOC1_TXT), tmp);
        FileUtils.copyFileToFolder(new File(TEST_RESOURCES_UPDATE_DOC2_TXT), tmp);

        // only update those files without a copyright header
        LicenseFormatMojo updater = new LicenseFormatMojo();
        updater.defaultBasedir = tmp;
        updater.legacyConfigHeader = TEST_RESOURCES_UPDATE_HEADER_TXT;
        updater.project = new MavenProjectStub();
        updater.defaultProperties = ImmutableMap.of("year", "2008");
        updater.skipExistingHeaders = true;
        updater.execute();

        assertEquals(FileUtils.read(new File(tmp, "doc1.txt"), FILE_ENCODING_PROPERTY), "====\r\n    My @Copyright license 2 with my-custom-value and 2008 and doc1.txt\r\n====\r\n\r\nsome data\r\n");
        assertEquals(FileUtils.read(new File(tmp, "doc2.txt"), FILE_ENCODING_PROPERTY), "====\r\n    Copyright license\r\n====\r\n\r\nsome data\r\n");

        // expect unchanged header to fail check against new header
        LicenseCheckMojo check = new LicenseCheckMojo();
        check.defaultBasedir = tmp;
        check.legacyConfigHeader = TEST_RESOURCES_UPDATE_HEADER_TXT;
        check.project = new MavenProjectStub();
        check.defaultProperties = ImmutableMap.of("year", "2008");
        check.skipExistingHeaders = false;

        try {
            check.execute();
            fail();
        } catch (MojoExecutionException e) {
            assertEquals("Some files do not have the expected license header", e.getMessage());
            assertEquals(1, check.missingHeaders.size());
        }

        // check again ignoring unchanged headers, should not fail
        check.skipExistingHeaders = true;
        check.execute();
    }

    @Test
    public void test_issue50() throws Exception {
        File tmp = new File("target/test/update/issue50");
        tmp.mkdirs();
        FileUtils.copyFileToFolder(new File(TEST_RESOURCES_UPDATE_ISSUE50_TEST1_PROPERTIES), tmp);
        FileUtils.copyFileToFolder(new File(TEST_RESOURCES_UPDATE_ISSUE50_TEST2_PROPERTIES), tmp);
        FileUtils.copyFileToFolder(new File(TEST_RESOURCES_UPDATE_ISSUE50_TEST3_PROPERTIES), tmp);

        LicenseFormatMojo updater = new LicenseFormatMojo();
        updater.defaultBasedir = tmp;
        updater.legacyConfigHeader = TEST_RESOURCES_UPDATE_HEADER_TXT;
        updater.defaultProperties = ImmutableMap.of("year", "2008");
        updater.mapping = new LinkedHashMap<String, String>() {{
            put("properties", "SCRIPT_STYLE");
        }};
        updater.project = new MavenProjectStub();
        updater.execute();

        String test1 = FileUtils.read(new File(tmp, "test1.properties"), FILE_ENCODING_PROPERTY).replaceAll("\\n", LS);
        String test2 = FileUtils.read(new File(tmp, "test2.properties"), FILE_ENCODING_PROPERTY);
        String test3 = FileUtils.read(new File(tmp, "test3.properties"), FILE_ENCODING_PROPERTY);

        assertEquals(test1, test2.replace("test2.properties", "test1.properties"));
        assertEquals(test1, test3.replace("test3.properties", "test1.properties"));
    }

    @Test
    public void test_issue48() throws Exception {
        File tmp = new File("target/test/update/issue48");
        tmp.mkdirs();
        FileUtils.copyFileToFolder(new File(TEST_RESOURCES_UPDATE_ISSUE48_TEST1_PHP), tmp);
        FileUtils.copyFileToFolder(new File(TEST_RESOURCES_UPDATE_ISSUE48_TEST2_PHP), tmp);

        LicenseFormatMojo updater = new LicenseFormatMojo();
        updater.defaultBasedir = tmp;
        updater.legacyConfigHeader = TEST_RESOURCES_UPDATE_HEADER_TXT;
        updater.defaultProperties = ImmutableMap.of("year", "2008");
        updater.mapping = new LinkedHashMap<String, String>() {{
            put("properties", "SCRIPT_STYLE");
        }};
        updater.project = new MavenProjectStub();
        updater.execute();

        assertEquals(FileUtils.read(new File(tmp, "test1.php"), FILE_ENCODING_PROPERTY), "\r\n" +
            "\r\n" +
            "<?php\r\n" +
            "/*\r\n" +
            " * My @Copyright license 2 with my-custom-value and 2008 and test1.php\r\n" +
            " */\r\n" +
            "\r\n" +
            "class Conference extends Service {}\r\n" +
            "\r\n" +
            "?>\r\n");
        assertEquals(FileUtils.read(new File(tmp, "test2.php"), FILE_ENCODING_PROPERTY), "\r\n" +
            "\r\n" +
            "<?php\r\n" +
            "/*\r\n" +
            " * My @Copyright license 2 with my-custom-value and 2008 and test2.php\r\n" +
            " */\r\n" +
            "\r\n" +
            "class Conference extends Service {}\r\n" +
            "\r\n" +
            "?>\r\n");
    }

    @Test
    public void test_issue44() throws Exception {
        File tmp = new File("target/test/update/issue44");
        tmp.mkdirs();
        FileUtils.copyFileToFolder(new File(TEST_RESOURCES_UPDATE_ISSUE44_ISSUE44_3_RB), tmp);
        FileUtils.copyFileToFolder(new File(TEST_RESOURCES_UPDATE_ISSUE44_TEST_ASP), tmp);

        LicenseFormatMojo updater = new LicenseFormatMojo();
        updater.defaultBasedir = tmp;
        updater.legacyConfigHeader = TEST_RESOURCES_UPDATE_HEADER_TXT;
        updater.defaultProperties = ImmutableMap.of("year", "2008");
        updater.project = new MavenProjectStub();
        updater.execute();

        assertEquals(FileUtils.read(new File(tmp, "issue44-3.rb"), FILE_ENCODING_PROPERTY), "#" + LS + "" +
            "# My @Copyright license 2 with my-custom-value and 2008 and issue44-3.rb" + LS + "" +
            "#" + LS + "" +
            "" + LS + "" +
            "# code comment" + LS + "" +
            "ruby code here" + LS + "");

        assertEquals(FileUtils.read(new File(tmp, "test.asp"), FILE_ENCODING_PROPERTY), "<%\n" +
            "    My @Copyright license 2 with my-custom-value and 2008 and test.asp\n" +
            "%>" +
            "\n" +
            "asp code");
    }

    @Test
    public void test_issue_14() throws Exception {
        File tmp = new File("target/test/update/issue14");
        tmp.mkdirs();
        FileUtils.copyFileToFolder(new File(TEST_RESOURCES_UPDATE_ISSUE14_TEST_PROPERTIES), tmp);

        LicenseFormatMojo updater = new LicenseFormatMojo();
        updater.defaultBasedir = tmp;
        updater.legacyConfigHeader = "src/test/resources/update/issue14/header.txt";
        updater.project = new MavenProjectStub();
        updater.execute();
        final String expectedString = "#" + LS + "" +
                "# Copyright (C) 2013 Salzburg Research." + LS + "" +
                "#" + LS + "" +
                "# Licensed under the Apache License, Version 2.0 (the \"License\");" + LS + "" +
                "# you may not use this file except in compliance with the License." + LS + "" +
                "# You may obtain a copy of the License at" + LS + "" +
                "#" + LS + "" +
                "#         http://www.apache.org/licenses/LICENSE-2.0" + LS + "" +
                "#" + LS + "" +
                "# Unless required by applicable law or agreed to in writing, software" + LS + "" +
                "# distributed under the License is distributed on an \"AS IS\" BASIS," + LS + "" +
                "# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied." + LS + "" +
                "# See the License for the specific language governing permissions and" + LS + "" +
                "# limitations under the License." + LS + "" +
                "#" + LS + "" +
                "" + LS + "" +
                "meta.tables            = SHOW TABLES;" + LS + "" +
                "meta.version           = SELECT mvalue FROM metadata WHERE mkey = 'version';" + LS + "" +
                "" + LS + "" +
                "# get sequence numbers" + LS + "" +
                "seq.nodes              = SELECT nextval('seq_nodes')" + LS + "" +
                "seq.triples            = SELECT nextval('seq_triples')" + LS + "" +
                "seq.namespaces         = SELECT nextval('seq_namespaces')" + LS + "";
        final String readModifiedContent = FileUtils.read(new File(tmp, "test.properties"), FILE_ENCODING_PROPERTY);

        assertEquals(expectedString, readModifiedContent);
    }
    
    @Test
    public void test_issue71_canSkipSeveralLines() throws Exception {
        File tmp = new File("target/test/update/issue71");
        tmp.mkdirs();
        FileUtils.copyFileToFolder(new File(TEST_RESOURCES_ISSUES_ISSUE_71_ISSUE_71_TXT_EXTENDED), tmp);

        LicenseFormatMojo updater = new LicenseFormatMojo();
        updater.defaultBasedir = tmp;
        updater.legacyConfigHeader = TEST_RESOURCES_ISSUES_ISSUE_71_ISSUE_71_HEADER_TXT;
        updater.project = new MavenProjectStub();
        updater.mapping = new LinkedHashMap<String, String>() {{
            put("txt.extended", "EXTENDED_STYLE");
        }};
        updater.defaultHeaderDefinitions = new String[]{"/issues/issue-71/issue-71-additionalHeaderDefinitions.xml"};
        updater.execute();


        // Check that all the skipable header has been correctly skipped
        List<String> linesOfModifiedFile = Files.readLines(new File(tmp, "issue-71.txt.extended"), Charset.defaultCharset());
        assertThat(linesOfModifiedFile.get(0 /* line 1 */), is("|||"));
        assertThat(linesOfModifiedFile.get(8) /* line 9 */, is("|||"));
    }
    
    @Test
    public void test_issue37_RunningUpdaterTwiceMustNotChangeTheFile() throws Exception {
        File tmp = new File("target/test/update/issue37");
        tmp.mkdirs();
        FileUtils.copyFileToFolder(new File(TEST_RESOURCES_UPDATE_ISSUE37_XWIKI_XML), tmp);
        
        LicenseFormatMojo execution1 = new LicenseFormatMojo();
        execution1.defaultBasedir = tmp;
        execution1.legacyConfigHeader = TEST_RESOURCES_UPDATE_ISSUE37_XWIKI_LICENSE_TXT;
        execution1.project = new MavenProjectStub();
        execution1.execute();
        
        String execution1FileContent = FileUtils.read(new File(tmp, "xwiki.xml"), FILE_ENCODING_PROPERTY);
        
        LicenseFormatMojo execution2 = new LicenseFormatMojo();
        execution2.defaultBasedir = tmp;
        execution2.legacyConfigHeader = TEST_RESOURCES_UPDATE_ISSUE37_XWIKI_LICENSE_TXT;
        execution2.project = new MavenProjectStub();
        execution2.execute();
        
        String execution2FileContent = FileUtils.read(new File(tmp, "xwiki.xml"), FILE_ENCODING_PROPERTY);
        
        assertThat(execution1FileContent, is(execution2FileContent));
    }

    @Test
    public void test_UpdateWorksHasExpectedOnAOneLineCommentFile_relatesTo_issue30() throws Exception {
            File tmp = new File("target/test/update/issue30");
            tmp.mkdirs();
            FileUtils.copyFileToFolder(new File(TEST_RESOURCES_UPDATE_ISSUE30_ONE_LINE_COMMENT_FTL), tmp);
    
            LicenseFormatMojo updater = new LicenseFormatMojo();
            updater.defaultBasedir = tmp;
            updater.legacyConfigHeader = TEST_RESOURCES_SINGLE_LINE_HEADER_TXT;
            updater.project = new MavenProjectStub();
            updater.execute();
            
            List<String> linesOfOriginFile = Files.readLines(new File(TEST_RESOURCES_UPDATE_ISSUE30_ONE_LINE_COMMENT_FTL), Charset.defaultCharset());
            List<String> linesOfUpdatedFile = Files.readLines(new File(tmp, "one-line-comment.ftl"), Charset.defaultCharset());
            
            // check that the original line is kept as the latest one even when introducing a license header
            assertThat(linesOfOriginFile.get(0), is(linesOfUpdatedFile.get(linesOfUpdatedFile.size() - 1)));
    }
}
package org.wikidata.wdtk.dumpfiles;

/*
 * #%L
 * Wikidata Toolkit Dump File Handling
 * %%
 * Copyright (C) 2014 - 2015 Wikidata Toolkit Developers
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;
import org.wikidata.wdtk.testing.MockDirectoryManager;
import org.wikidata.wdtk.util.CompressionType;
import org.wikidata.wdtk.util.DirectoryManagerFactory;

import static org.junit.Assert.*;

public class MwLocalDumpFileTest {
	private static final String TESTDUMP_JSON_GZ = "testdump-20150512.json.gz";
	private static final String TESTDUMP_DATE = "20150815";
	private static final String TESTDUMP_PROJECT = "wikidatawiki";
	private static final String TEST_SQL_GZ = "test.sql.gz";
	private static final String TEST_XML_BZ2 = "test.xml.bz2";
	private static final String DAILY_DUMP_XML_BZ2 = "daily-dump.xml.bz2";
	private static final String CURRENT_DUMP_XML_BZ2 = "current-dump.xml.bz2";
	private static final String CURRENT_DUMP = "current-dump";

	MockDirectoryManager dm;
	Path dmPath;

	@Before
	public void setUp() throws Exception {
		DirectoryManagerFactory
				.setDirectoryManagerClass(MockDirectoryManager.class);

		this.dmPath = Paths.get("/").toAbsolutePath();
		this.dm = new MockDirectoryManager(this.dmPath, true, true);
	}

	@Test
	public void missingDumpFile() {
		MwLocalDumpFile df = new MwLocalDumpFile(
				"/non-existing-dump-file.json.gz");
		assertFalse(df.isAvailable());
	}

	@Test
	public void missingDumpFileDirectory() {
		MwLocalDumpFile df = new MwLocalDumpFile(
				"/nonexisting-directory/non-existing-file.json.gz");
		assertFalse(df.isAvailable());
	}

	@Test
	public void testExplicitGetters() throws IOException {
		this.dm.setFileContents(this.dmPath
				.resolve(TESTDUMP_JSON_GZ), "");
		MwLocalDumpFile df = new MwLocalDumpFile(
				"/" + TESTDUMP_JSON_GZ,
				DumpContentType.SITES, TESTDUMP_DATE,
				TESTDUMP_PROJECT);

		assertEquals(TESTDUMP_DATE, df.getDateStamp());
		assertEquals(TESTDUMP_PROJECT, df.getProjectName());
		assertEquals(DumpContentType.SITES, df.getDumpContentType());
		String toString = df.toString();

		assertEquals(this.dmPath.resolve(TESTDUMP_JSON_GZ),
				df.getPath());

		assertTrue(toString.contains(TESTDUMP_DATE));
		assertTrue(toString.contains(TESTDUMP_PROJECT));
		assertTrue(toString.toLowerCase().contains(
				DumpContentType.SITES.toString().toLowerCase()));
	}

	@Test
	public void testGuessJsonDumpAndDate() throws IOException {
		this.dm.setFileContents(this.dmPath
				.resolve(TESTDUMP_JSON_GZ), "");
		MwLocalDumpFile df = new MwLocalDumpFile(
				"/" + TESTDUMP_JSON_GZ);
		assertTrue(df.isAvailable());
		assertEquals("20150512", df.getDateStamp());
		assertEquals("LOCAL", df.getProjectName());
		assertEquals(df.getDumpContentType(), DumpContentType.JSON);
	}

	@Test
	public void testJsonReader() throws IOException {
		this.dm.setFileContents(this.dmPath
				.resolve(TESTDUMP_JSON_GZ),
				"Test contents", CompressionType.GZIP);
		MwLocalDumpFile df = new MwLocalDumpFile(
				"/" + TESTDUMP_JSON_GZ);
		BufferedReader br = df.getDumpFileReader();
		assertEquals("Test contents", br.readLine());
		assertNull(br.readLine());
	}

	@Test(expected = IOException.class)
	public void testUnavailableReader() throws IOException {
		MwLocalDumpFile df = new MwLocalDumpFile(
				"/" + TESTDUMP_JSON_GZ);
		df.getDumpFileReader();
	}

	@Test
	public void testGuessSitesDump() throws IOException {
		this.dm.setFileContents(this.dmPath.resolve(TEST_SQL_GZ), "");
		MwLocalDumpFile df = new MwLocalDumpFile("/" + TEST_SQL_GZ);
		assertTrue(df.isAvailable());
		assertEquals("YYYYMMDD", df.getDateStamp());
		assertEquals(df.getDumpContentType(), DumpContentType.SITES);
	}

	@Test
	public void testGuessFullDump() throws IOException {
		this.dm.setFileContents(this.dmPath.resolve(TEST_XML_BZ2), "");
		MwLocalDumpFile df = new MwLocalDumpFile("/" + TEST_XML_BZ2);
		assertTrue(df.isAvailable());
		assertEquals(df.getDumpContentType(), DumpContentType.FULL);
	}

	@Test
	public void testGuessDailyDump() throws IOException {
		this.dm.setFileContents(
				this.dmPath.resolve(DAILY_DUMP_XML_BZ2), "");
		MwLocalDumpFile df = new MwLocalDumpFile("/" + DAILY_DUMP_XML_BZ2);
		assertTrue(df.isAvailable());
		assertEquals(df.getDumpContentType(), DumpContentType.DAILY);
	}

	@Test
	public void testGuessCurrentDump() throws IOException {
		this.dm.setFileContents(
				this.dmPath.resolve(CURRENT_DUMP_XML_BZ2), "");
		MwLocalDumpFile df = new MwLocalDumpFile(
				"/" + CURRENT_DUMP_XML_BZ2);
		assertTrue(df.isAvailable());
		assertEquals(df.getDumpContentType(), DumpContentType.CURRENT);
	}

	@Test
	public void testGuessUnknownDumpType() throws IOException {
		this.dm.setFileContents(this.dmPath.resolve(CURRENT_DUMP), "");
		MwLocalDumpFile df = new MwLocalDumpFile("/" + CURRENT_DUMP);
		assertTrue(df.isAvailable());
		assertEquals(df.getDumpContentType(), DumpContentType.JSON);
	}

}
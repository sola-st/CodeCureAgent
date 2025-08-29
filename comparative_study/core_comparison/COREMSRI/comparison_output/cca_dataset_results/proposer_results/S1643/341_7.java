package org.wikidata.wdtk.dumpfiles;

/*
 * #%L
 * Wikidata Toolkit Dump File Handling
 * %%
 * Copyright (C) 2014 Wikidata Toolkit Developers
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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentProcessor;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.dumpfiles.wmf.WmfDumpFile;
import org.wikidata.wdtk.dumpfiles.wmf.WmfLocalDumpFile;
import org.wikidata.wdtk.testing.MockDirectoryManager;
import org.wikidata.wdtk.testing.MockStringContentFactory;
import org.wikidata.wdtk.util.DirectoryManagerFactory;

public class MwDumpFileProcessingTest {

	/**
	 * Helper class that stores all information passed to it for later testing.
	 *
	 * @author Markus Kroetzsch
	 *
	 */
	static class TestMwRevisionProcessor implements MwRevisionProcessor {

		final List<MwRevision> revisions = new ArrayList<>();
		String siteName;
		String baseUrl;
		Map<Integer, String> namespaces;

		@Override
		public void startRevisionProcessing(String siteName, String baseUrl,
				Map<Integer, String> namespaces) {
			this.siteName = siteName;
			this.baseUrl = baseUrl;
			this.namespaces = namespaces;
		}

		@Override
		public void processRevision(MwRevision mwRevision) {
			this.revisions.add(new MwRevisionImpl(mwRevision));
		}

		@Override
		public void finishRevisionProcessing() {
		}

	}

	/**
	 * Helper class that counts how many items it gets.
	 *
	 * @author Markus Kroetzsch
	 *
	 */
	static class TestEntityDocumentProcessor implements EntityDocumentProcessor {

		int itemCount = 0;
		int propCount = 0;

		@Override
		public void processItemDocument(ItemDocument itemDocument) {
			this.itemCount++;
		}

		@Override
		public void processPropertyDocument(PropertyDocument propertyDocument) {
			this.propCount++;
		}

	}

	@Before
	public void configureDirectoryManager() {
		DirectoryManagerFactory
				.setDirectoryManagerClass(MockDirectoryManager.class);
	}

	/**
	 * Generates a simple item revision for testing purposes.
	 *
	 * @param number
	 */
	private MwRevision getItemRevision(int number) {
		MwRevisionImpl result = new MwRevisionImpl();
		result.prefixedTitle = "Q1";
		result.namespace = 0;
		result.pageId = 32;
		result.revisionId = number;
		result.parentRevisionId = number - 1;
		result.timeStamp = "2014-02-19T23:34:1" + (number % 10) + "Z";
		result.format = "application/json";
		result.model = MwRevision.MODEL_WIKIBASE_ITEM;
		result.comment = "Test comment " + number;
		result.text = "{\"id\":\"Q1\",\"type\":\"item\",\"labels\":{\"en\":{\"language\":\"en\",\"value\":\"Revision "
				+ number + "\"}}}";
		result.contributor = "127.0.0." + (number % 256);
		result.contributorId = -1;
		return result;
	}

	/**
	 * Generates a simple property revision for testing purposes.
	 *
	 * @param number
	 */
	private MwRevision getPropertyRevision(int number) {
		MwRevisionImpl result = new MwRevisionImpl();
		result.prefixedTitle = "Property:P1";
		result.namespace = 120;
		result.pageId = 12345;
		result.revisionId = number + 10000;
		result.parentRevisionId = number + 9999;
		result.timeStamp = "2014-02-19T23:34:1" + (number % 10) + "Z";
		result.format = "application/json";
		result.model = MwRevision.MODEL_WIKIBASE_PROPERTY;
		result.comment = "Test comment " + (number + 10000);
		result.text = "{\"id\":\"P1\",\"type\":\"property\",\"labels\":{\"en\":{\"language\":\"en\",\"value\":\"Revision "
				+ (number + 10000) + "\"}},\"datatype\":\"wikibase-item\"}";
```java
			StringBuilder dumpContentsBuilder = new StringBuilder();
			dumpContentsBuilder.append(MockStringContentFactory.getStringFromUrl(resourceUrl));
			for (int pageId = baseId; pageId < baseId + 3; pageId++) {
				dumpContentsBuilder.append("  <page>\n");
				dumpContentsBuilder.append("    <title>Q").append(pageId).append("</title>\n");
				dumpContentsBuilder.append("    <ns>0</ns>\n");
				dumpContentsBuilder.append("    <id>").append(pageId + 1000).append("</id>\n");
				for (int revId = pageId * 1000 + baseId + 1; revId < pageId * 1000 + baseId + 4; revId++) {
					dumpContentsBuilder.append("    <revision>\n");
					dumpContentsBuilder.append("      <id>").append(revId).append("</id>\n");
					dumpContentsBuilder.append("      <parentid>").append(revId - 1).append("</parentid>\n");
					dumpContentsBuilder.append("      <timestamp>2014-02-19T23:34:0").append(revId % 10).append("</timestamp>\n");
					dumpContentsBuilder.append("      <contributor>");
					dumpContentsBuilder.append("        <ip>127.0.0.").append(revId % 256).append("</ip>\n");
					dumpContentsBuilder.append("      </contributor>\n");
					dumpContentsBuilder.append("      <comment>Test comment ").append(revId).append("</comment>\n");
					dumpContentsBuilder.append("      <text xml:space=\"preserve\">{&quot;label&quot;:{&quot;en&quot;:&quot;Revision ")
									   .append(revId).append("&quot;}}</text>\n");
					dumpContentsBuilder.append("      <sha1>ignored</sha1>");
					dumpContentsBuilder.append("      <model>wikibase-item</model>");
					dumpContentsBuilder.append("      <format>application/json</format>");
					dumpContentsBuilder.append("    </revision>\n");
				}
				dumpContentsBuilder.append("  </page>\n");
			}
			dumpContentsBuilder.append("</mediawiki>\n");

			Path filePath = thisDumpPath.resolve("wikidatawiki-" + dateStamp
					+ WmfDumpFile.getDumpFilePostfix(dumpContentType));
			dm.setFileContents(filePath, dumpContentsBuilder.toString(), WmfDumpFile.getDumpFileCompressionType(filePath.toString()));

		mockLocalDumpFile("20140418", 2, DumpContentType.FULL, dm);

		DumpProcessingController dpc = new DumpProcessingController(
				"wikidatawiki");
		dpc.downloadDirectoryManager = dm;
		dpc.setOfflineMode(true);

		StatisticsMwRevisionProcessor mwrpStats = new StatisticsMwRevisionProcessor(
				"stats", 2);
		dpc.registerMwRevisionProcessor(mwrpStats, null, false);

		dpc.processAllRecentRevisionDumps();

		assertEquals(19, mwrpStats.getTotalRevisionCount());
		assertEquals(5, mwrpStats.getCurrentRevisionCount());
	}

	@Test
	public void testMwMostRecentFullDumpFileProcessing() throws IOException {
		Path dmPath = Paths.get(System.getProperty("user.dir"));
		MockDirectoryManager dm = new MockDirectoryManager(dmPath, true, true);
		mockLocalDumpFile("20140418", 2, DumpContentType.FULL, dm);

		DumpProcessingController dpc = new DumpProcessingController(
				"wikidatawiki");
		dpc.downloadDirectoryManager = dm;
		dpc.setOfflineMode(true);

		StatisticsMwRevisionProcessor mwrpStats = new StatisticsMwRevisionProcessor(
				"stats", 2);
		dpc.registerMwRevisionProcessor(mwrpStats, null, false);

		dpc.processMostRecentMainDump();

		assertEquals(9, mwrpStats.getTotalRevisionCount());
		assertEquals(9, mwrpStats.getCurrentRevisionCount());
	}

}
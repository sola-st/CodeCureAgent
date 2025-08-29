package org.wikidata.wdtk.wikibaseapi;

/*
 * #%L
 * Wikidata Toolkit Wikibase API
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.util.CompressionType;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

public class WbGetEntitiesActionTest {

	private static final String ACTION_PARAM = "action";
	private static final String FORMAT_PARAM = "format";
	private static final String IDS_PARAM = "ids";
	private static final String PROPS_PARAM = "props";
	private static final String LANGUAGES_PARAM = "languages";
	private static final String SITEFILTER_PARAM = "sitefilter";

	private static final String ACTION_VALUE = "wbgetentities";
	private static final String FORMAT_VALUE = "json";
	private static final String IDS_VALUE_1 = "Q32063953";
	private static final String IDS_VALUE_2 = "Q6|Q42|P31";
	private static final String PROPS_VALUE = "datatype|labels|aliases|descriptions|claims|sitelinks";
	private static final String LANGUAGES_VALUE = "en";
	private static final String SITEFILTER_VALUE = "enwiki";

	MockBasicApiConnection con;
	WbGetEntitiesAction action;

	@Before
	public void setUp() throws Exception {

		this.con = new MockBasicApiConnection();
		Map<String, String> params = new HashMap<>();
		params.put(ACTION_PARAM, ACTION_VALUE);
		params.put(FORMAT_PARAM, FORMAT_VALUE);
		params.put(IDS_PARAM, IDS_VALUE_1);
		this.con.setWebResourceFromPath(params, getClass(),
				"/wbgetentities-Q32063953.json", CompressionType.NONE);
		params.put(IDS_PARAM, IDS_VALUE_2);
		this.con.setWebResourceFromPath(params, getClass(),
				"/wbgetentities-Q6-Q42-P31.json", CompressionType.NONE);
		params.put(PROPS_PARAM, PROPS_VALUE);
		this.con.setWebResourceFromPath(params, getClass(),
				"/wbgetentities-Q6-Q42-P31.json", CompressionType.NONE);
		params.put(LANGUAGES_PARAM, LANGUAGES_VALUE);
		params.put(SITEFILTER_PARAM, SITEFILTER_VALUE);
		this.con.setWebResourceFromPath(params, getClass(),
				"/wbgetentities-Q6-Q42-P31.json", CompressionType.NONE);

		this.action = new WbGetEntitiesAction(this.con, Datamodel.SITE_WIKIDATA);

	}

	@Test
	public void testWbGetEntitiesWithProps() throws MediaWikiApiErrorException, IOException {
		WbGetEntitiesActionData properties = new WbGetEntitiesActionData();
		properties.ids = IDS_VALUE_2;
		properties.props = PROPS_VALUE;
		Map<String, EntityDocument> result1 = action.wbGetEntities(properties);
		Map<String, EntityDocument> result2 = action.wbGetEntities(
				properties.ids, null, null, properties.props, null, null);

		assertTrue(result1.containsKey("Q42"));
		assertEquals(result1, result2);
	}

	@Test
	public void testWbGetEntitiesNoProps() throws MediaWikiApiErrorException, IOException {
		WbGetEntitiesActionData properties = new WbGetEntitiesActionData();
		properties.ids = IDS_VALUE_2;
		Map<String, EntityDocument> result1 = action.wbGetEntities(properties);
		Map<String, EntityDocument> result2 = action.wbGetEntities(
				properties.ids, null, null, properties.props, null, null);

		assertTrue(result1.containsKey("Q42"));
		assertEquals(result1, result2);
	}
	
	@Test
	public void testWbGetEntitiesRedirected() throws MediaWikiApiErrorException, IOException {
		WbGetEntitiesActionData properties = new WbGetEntitiesActionData();
		properties.ids = IDS_VALUE_1;
		Map<String, EntityDocument> result = action.wbGetEntities(properties);
		
		assertTrue(result.containsKey(IDS_VALUE_1));
	}

	@Test
	public void testWbGetEntitiesPropsFilters()
			throws MediaWikiApiErrorException, IOException {
		WbGetEntitiesActionData properties = new WbGetEntitiesActionData();
		properties.ids = IDS_VALUE_2;
		properties.props = PROPS_VALUE;
		properties.languages = LANGUAGES_VALUE;
		properties.sitefilter = SITEFILTER_VALUE;
		Map<String, EntityDocument> result1 = action.wbGetEntities(properties);
		Map<String, EntityDocument> result2 = action.wbGetEntities(
				properties.ids, null, null, properties.props, null, null);

		assertTrue(result1.containsKey("Q42"));
		assertEquals(result1, result2);
	}

	@Test(expected = IOException.class)
	public void testWbGetEntitiesIoError() throws MediaWikiApiErrorException, IOException {
		WbGetEntitiesActionData properties = new WbGetEntitiesActionData();
		properties.ids = "Q6|Q42|notmocked";
		action.wbGetEntities(properties);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIdsAndTitles() throws MediaWikiApiErrorException, IOException {
		action.wbGetEntities("Q42", null, "Tim Berners Lee", null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIdsAndSites() throws MediaWikiApiErrorException, IOException {
		action.wbGetEntities("Q42", "enwiki", null, null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTitlesNoSites() throws MediaWikiApiErrorException, IOException {
		action.wbGetEntities(null, null, "Tim Berners Lee", null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoTitlesOrIds() throws MediaWikiApiErrorException, IOException {
		action.wbGetEntities(null, "enwiki", null, null, null, null);
	}

}
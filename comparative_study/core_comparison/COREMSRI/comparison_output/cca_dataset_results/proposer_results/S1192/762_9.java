/*
 * Copyright 2013-2015 Urs Wolfer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.urswolfer.gerrit.client.rest.http.changes;

import com.google.common.truth.Truth;
import com.google.gerrit.extensions.api.changes.DraftInput;
import com.google.gerrit.extensions.common.CommentInfo;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.urswolfer.gerrit.client.rest.http.GerritRestClient;
import com.urswolfer.gerrit.client.rest.http.common.AbstractParserTest;
import com.urswolfer.gerrit.client.rest.http.common.GerritRestClientBuilder;
import org.easymock.EasyMock;
import org.testng.annotations.Test;

/**
 * @author Urs Wolfer
 */
public class DraftsApiRestClientTest extends AbstractParserTest {
    private static final String DRAFT_ID = "89233d9c_56013406";
    private static final String REVISION_ID = "ec047590bc7fb8db7ae03ebac336488bfc1c5e12";
    private static final String CHANGE_ID = "myProject~master~I8473b95934b5732ac55d26311a706c9c2bde9940";

    private CommentsParser commentsParser = new CommentsParser(getGson());

    @Test
    public void testGettingDraftById() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", DRAFT_ID);
        GerritRestClient gerritRestClient = new GerritRestClientBuilder()
            .expectGet("/changes/" + CHANGE_ID + "/revisions/" + REVISION_ID + "/drafts/" + DRAFT_ID, jsonObject)
            .get();

        ChangeApiRestClient changeApiRestClient = new ChangeApiRestClient(gerritRestClient, null, null, commentsParser, null,
            null, null, null, null, null, null,
            null, null, null, CHANGE_ID);
        RevisionApiRestClient revisionApiRestClient = new RevisionApiRestClient(gerritRestClient, changeApiRestClient, commentsParser, null, null, null,
            null, null, REVISION_ID);

        CommentInfo commentInfo = revisionApiRestClient.draft(DRAFT_ID).get();

        Truth.assertThat(commentInfo.id).isEqualTo(DRAFT_ID);
        EasyMock.verify(gerritRestClient);
    }

    @Test
    public void testGettingDraftByCommentInfo() throws Exception {
        GerritRestClient gerritRestClient = new GerritRestClientBuilder().get();

        CommentInfo expectedCommentInfo = new CommentInfo();
        expectedCommentInfo.id = DRAFT_ID;

        ChangeApiRestClient changeApiRestClient = new ChangeApiRestClient(gerritRestClient, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, CHANGE_ID);
        RevisionApiRestClient revisionApiRestClient = new RevisionApiRestClient(gerritRestClient, changeApiRestClient, null, null, null,
            null, null, null, REVISION_ID);
        DraftApiRestClient draftApiRestClient = new DraftApiRestClient(gerritRestClient, changeApiRestClient,
            revisionApiRestClient, null, expectedCommentInfo);

        CommentInfo commentInfo = draftApiRestClient.get();

        Truth.assertThat(commentInfo).isSameAs(expectedCommentInfo);
        EasyMock.verify(gerritRestClient);
    }

    @Test
    public void testUpdateDraft() throws Exception {
        JsonElement jsonElement = EasyMock.createMock(JsonElement.class);
        GerritRestClient gerritRestClient = new GerritRestClientBuilder()
            .expectPut("/changes/" + CHANGE_ID + "/revisions/" + REVISION_ID + "/drafts/" + DRAFT_ID, "{}", jsonElement)
            .expectGetGson()
            .get();

        ChangeApiRestClient changeApiRestClient = new ChangeApiRestClient(gerritRestClient, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, CHANGE_ID);
        RevisionApiRestClient revisionApiRestClient = new RevisionApiRestClient(gerritRestClient, changeApiRestClient, null, null, null,
            null, null, null, REVISION_ID);
        DraftApiRestClient draftApiRestClient = new DraftApiRestClient(gerritRestClient, changeApiRestClient,
            revisionApiRestClient, commentsParser, DRAFT_ID);

        draftApiRestClient.update(new DraftInput());

        EasyMock.verify(gerritRestClient);
    }

    @Test
    public void testDeleteDraft() throws Exception {
        GerritRestClient gerritRestClient = new GerritRestClientBuilder()
            .expectDelete("/changes/" + CHANGE_ID + "/revisions/" + REVISION_ID + "/drafts/" + DRAFT_ID)
            .get();

        ChangeApiRestClient changeApiRestClient = new ChangeApiRestClient(gerritRestClient, null, null, null, null,
            null, null,null, null, null,
            null, null, null, null, CHANGE_ID);
        RevisionApiRestClient revisionApiRestClient = new RevisionApiRestClient(gerritRestClient, changeApiRestClient, null, null, null,
            null, null, null, REVISION_ID);
        DraftApiRestClient draftApiRestClient = new DraftApiRestClient(gerritRestClient, changeApiRestClient,
            revisionApiRestClient, null, DRAFT_ID);

        draftApiRestClient.delete();

        EasyMock.verify(gerritRestClient);
    }
}
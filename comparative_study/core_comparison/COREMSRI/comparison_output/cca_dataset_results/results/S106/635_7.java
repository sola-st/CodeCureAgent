/*
 * Copyright 2013-2014 Urs Wolfer
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

package com.urswolfer.gerrit.client.rest.http;

import com.google.common.base.Charsets;
import com.google.common.truth.Truth;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.ProjectInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.urswolfer.gerrit.client.rest.GerritAuthData;
import com.urswolfer.gerrit.client.rest.GerritRestApi;
import com.urswolfer.gerrit.client.rest.GerritRestApiFactory;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.FileResource;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServlet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.List;

/**
 * @author Urs Wolfer
 */
public class GerritRestClientTest {
    private String jettyUrl;
    private String githubOAuthJettyUrl;

    @BeforeClass
    public void startJetty() throws Exception {
        jettyUrl = startJetty(LoginSimulationServlet.class);
        githubOAuthJettyUrl = startJetty(GitHubOAuthLoginSimulationServlet.class);
    }

    public String startJetty(Class<? extends HttpServlet> loginServletClass) throws Exception {
        Server server = new Server(0);

        ResourceHandler resourceHandler = new ResourceHandler();
        MimeTypes mimeTypes = new MimeTypes();
        mimeTypes.addMimeMapping("json", "application/json");
        resourceHandler.setMimeTypes(mimeTypes);
        URL url = this.getClass().getResource(".");
        resourceHandler.setBaseResource(new FileResource(url));
        resourceHandler.setWelcomeFiles(new String[] {"changes.json", "projects.json", "account.json"});

        ServletContextHandler servletContextHandler = new ServletContextHandler();
        servletContextHandler.addServlet(loginServletClass, "/login/");

        ServletContextHandler basicAuthContextHandler = new ServletContextHandler(ServletContextHandler.SECURITY);
        basicAuthContextHandler.setSecurityHandler(basicAuth("foo", "bar", "Gerrit Auth"));
        basicAuthContextHandler.setContextPath("/a");

        HandlerCollection handlers = new HandlerCollection();
        handlers.setHandlers(new Handler[] {
            servletContextHandler,
            resourceHandler,
            basicAuthContextHandler
        });
        server.setHandler(handlers);

        server.start();

        Connector connector = server.getConnectors()[0];
        String host = "localhost";
        int port = connector.getLocalPort();
        return String.format("http://%s:%s", host, port);
    }

    private static SecurityHandler basicAuth(String username, String password, String realm) {
        HashLoginService loginService = new HashLoginService();
        loginService.putUser(username, Credential.getCredential(password), new String[]{"user"});
        loginService.setName(realm);

        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__DIGEST_AUTH);
        constraint.setRoles(new String[]{"user"});
        constraint.setAuthenticate(true);

        ConstraintMapping constraintMapping = new ConstraintMapping();
        constraintMapping.setConstraint(constraint);
        constraintMapping.setPathSpec("/*");

        ConstraintSecurityHandler csh = new ConstraintSecurityHandler();
        csh.setAuthenticator(new BasicAuthenticator());
        csh.setRealmName("realm");
        csh.addConstraintMapping(constraintMapping);
        csh.setLoginService(loginService);
        return csh;
    }

    @Test(expectedExceptions = RestApiException.class)
    public void testInvalidHost() throws Exception {
        GerritRestApiFactory gerritRestApiFactory = new GerritRestApiFactory();
        GerritAuthData.Basic authData = new GerritAuthData.Basic("http://averyinvaliddomainforgerritresttest.com:8089");
        GerritApi gerritClient = gerritRestApiFactory.create(authData);
        gerritClient.changes().query().get();
    }

    private GerritRestApi getGerritApiWithJettyHost() {
        GerritRestApiFactory gerritRestApiFactory = new GerritRestApiFactory();
        return gerritRestApiFactory.create(new GerritAuthData.Basic(jettyUrl));
    }

    @Test
    public void testGetChanges() throws Exception {
        GerritApi gerritClient = getGerritApiWithJettyHost();
        List<ChangeInfo> changes = gerritClient.changes().query().get();
        Truth.assertThat(changes.size()).isEqualTo(3);
    }

    @Test
    public void testGetSelfAccount() throws Exception {
        GerritApi gerritClient = getGerritApiWithJettyHost();
        AccountInfo accountInfo = gerritClient.accounts().self().get();
        Truth.assertThat(accountInfo.name).isEqualTo("John Doe");
    }

    @Test(expectedExceptions = HttpStatusException.class)
    public void testGetInvalidAccount() throws Exception {
        GerritApi gerritClient = getGerritApiWithJettyHost();
        gerritClient.accounts().id("invalid").get();
    }

    @Test
    public void testGetProjects() throws Exception {
        GerritApi gerritClient = getGerritApiWithJettyHost();
        List<ProjectInfo> projects = gerritClient.projects().list().get();
        Truth.assertThat(projects.size()).isEqualTo(3);
    }

    @Test
    public void testGetCommitMsgHook() throws Exception {
        GerritRestApi gerritClient = getGerritApiWithJettyHost();
        InputStream commitMessageHook = gerritClient.tools().getCommitMessageHook();
        String result = new BufferedReader(new InputStreamReader(commitMessageHook, Charsets.UTF_8)).readLine();
        Truth.assertThat(result).isEqualTo("dummy-commit-msg-hook");
    }

    @Test(expectedExceptions = HttpStatusException.class)
    public void testStarNotLoggedIn() throws Exception {
        GerritApi gerritClient = getGerritApiWithJettyHost();
        gerritClient.accounts().self().starChange("1");
    }

    @Test(expectedExceptions = HttpStatusException.class)
    public void testUnstarNotLoggedIn() throws Exception {
        GerritApi gerritClient = getGerritApiWithJettyHost();
        gerritClient.accounts().self().unstarChange("1");
    }

    @Test(expectedExceptions = HttpStatusException.class)
    public void testAbandonNotLoggedIn() throws Exception {
        GerritApi gerritClient = getGerritApiWithJettyHost();
        gerritClient.changes().id(1).abandon();
    }

    @Test(expectedExceptions = RestApiException.class)
    public void testInvalidJson() throws Exception {
        GerritApi gerritClient = getGerritApiWithJettyHost();
        gerritClient.accounts().id("invalid_json").get();
    }

    @Test(expectedExceptions = RestApiException.class)
    public void testNullJson() throws Exception {
        GerritApi gerritClient = getGerritApiWithJettyHost();
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YourTestClassName {

    private static final Logger logger = LoggerFactory.getLogger(YourTestClassName.class);

    // ... (other code parts unchanged)

    @Test(enabled = false) // requires running Gerrit instance
    public void testBasicRestCallToLocalhost() throws Exception {
        GerritRestApiFactory gerritRestApiFactory = new GerritRestApiFactory();
        GerritApi gerritClient = gerritRestApiFactory.create(new GerritAuthData.Basic("http://localhost:8080"));
        List<ChangeInfo> changes = gerritClient.changes().query().get();
        logger.info(String.format("Got %s changes.", changes.size()));
        logger.info("{}", changes);
    }

    @Test(enabled = false) // requires running Gerrit instance
    public void testBasicRestCallToLocalhostProjects() throws Exception {
        GerritRestApiFactory gerritRestApiFactory = new GerritRestApiFactory();
        GerritApi gerritClient = gerritRestApiFactory.create(new GerritAuthData.Basic("http://localhost:8080"));
        List<ProjectInfo> projects = gerritClient.projects().list().get();
        logger.info(String.format("Got %s projects.", projects.size()));
        logger.info("{}", projects);
    }

    @Test(enabled = false) // requires running Gerrit instance
    public void testBasicRestCallToLocalhostProjectsQuery() throws Exception {
        GerritRestApiFactory gerritRestApiFactory = new GerritRestApiFactory();
        GerritApi gerritClient = gerritRestApiFactory.create(new GerritAuthData.Basic("http://localhost:8080"));
        List<ProjectInfo> projects = gerritClient.projects().list().withLimit(1).withDescription(true).get();
        logger.info(String.format("Got %s projects.", projects.size()));
        logger.info("{}", projects);
    }

    // ... (rest of the class)
}
```
Explanation:
- Added a `Logger` instance using SLF4J.
- Replaced all `System.out.println` calls with `logger.info` calls.
- Kept the code formatting and unchanged sections intact.
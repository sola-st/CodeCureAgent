/*
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

import com.mycila.maven.plugin.license.document.Document;
import com.mycila.maven.plugin.license.document.DocumentFactory;
import com.mycila.maven.plugin.license.document.DocumentPropertiesLoader;
import com.mycila.maven.plugin.license.document.DocumentType;
import com.mycila.maven.plugin.license.header.AdditionalHeaderDefinition;
import com.mycila.maven.plugin.license.header.Header;
import com.mycila.maven.plugin.license.header.HeaderDefinition;
import com.mycila.maven.plugin.license.header.HeaderSource;
import com.mycila.maven.plugin.license.header.HeaderType;
import com.mycila.maven.plugin.license.util.Selection;
import com.mycila.maven.plugin.license.util.resource.ResourceFinder;
import com.mycila.xmltool.XMLDoc;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.mycila.maven.plugin.license.document.DocumentType.defaultMapping;
import static com.mycila.maven.plugin.license.util.FileUtils.asPath;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Arrays.deepToString;

/**
 * <b>Date:</b> 18-Feb-2008<br> <b>Author:</b> Mathieu Carbou
 * (mathieu.carbou@gmail.com)
 */
public abstract class AbstractLicenseMojo extends AbstractMojo {

    @Parameter
    public LicenseSet[] licenseSets;

    /**
     * The base directory, in which to search for project files.
     *
     * This is named `defaultBaseDirectory` as it will be used as the default
     * value for the base directory. This default value can be overridden
     * in each LicenseSet by setting {@link LicenseSet#basedir}.
     */
    @Parameter(property = "license.basedir", defaultValue = "${basedir}", alias = "basedir", required = true)
    public File defaultBasedir;

    /**
     * Location of the header. It can be a relative path, absolute path,
     * classpath resource, any URL. The plugin first check if the name specified
     * is a relative file, then an absolute file, then in the classpath. If not
     * found, it tries to construct a URL from the location.
     *
     * @deprecated use {@link LicenseSet#header}
     */
    @Deprecated
    @Parameter(property = "license.header", alias = "header")
    public String legacyConfigHeader;

    /**
     * Header, as text, directly in pom file. Using a CDATA section is strongly recommended.
     *
     * @deprecated use {@link LicenseSet#inlineHeader}
     */
    @Deprecated
    @Parameter(property = "license.inlineHeader", alias="inlineHeader")
    public String legacyConfigInlineHeader;

    /**
     * Specifies additional header files to use when checking for the presence
     * of a valid header in your sources.
     * <br>
     * When using format goal, this property will be used to detect all valid
     * headers that don't need formatting.
     * <br>
     * When using remove goal, this property will be used to detect all valid
     * headers that also must be removed.
     *
     * @deprecated use {@link LicenseSet#validHeaders}
     */
    @Deprecated
    @Parameter(alias = "validHeaders")
    public String[] legacyConfigValidHeaders = new String[0];

    /**
     * Alternative to `header`, `inlineHeader`, or `validHeaders`
     * for use when code is multi-licensed.
     * Whilst you could create a concatenated header yourself,
     * a cleaner approach may be to specify more than one header
     * and have them concatenated together by the plugin. This
     * allows you to maintain each distinct license header in
     * its own file and combined them in different ways.
     *
     * @deprecated use {@link LicenseSet#multi}
     */
    @Deprecated
    @Parameter
    public Multi legacyConfigMulti;

    /**
     * Allows the use of external header definitions files. These files are
     * properties like files.
     *
     * This is named `defaultHeaderDefinitions` as it will be used as the default
     * value for the header definitions. This default value can be overridden
     * in each LicenseSet by setting {@link LicenseSet#headerDefinitions}.
     */
    @Parameter(alias = "headerDefinitions")
    public String[] defaultHeaderDefinitions = new String[0];

    /**
     * HeadSections define special regions of a header that allow for dynamic
     * substitution and validation
     *
     * @deprecated use {@link LicenseSet#headerSections}
     */
    @Deprecated
    @Parameter(alias = "headerSections")
    public HeaderSection[] legacyConfigHeaderSections = new HeaderSection[0];

    /**
     * You can set here some properties that you want to use when reading the
     * header file. You can use in your header file some properties like
     * ${year}, ${owner} or whatever you want for the name. They will be
     * replaced when the header file is read by those you specified in the
     * command line, in the POM and in system environment.
     *
     * This is named `defaultProperties` as it will be used as the default
     * value for the properties. This default value can be overridden
     * in each LicenseSet by setting {@link LicenseSet#properties}.
     */
    @Parameter(alias = "properties")
    public Map<String, String> defaultProperties = new HashMap<String, String>();

    /**
     * Specifies files, which are included in the check. By default, all files
     * are included.
     *
     * @deprecated use {@link LicenseSet#includes}
     */
    @Deprecated
    @Parameter(alias = "includes", property = "license.includes")
    public String[] legacyConfigIncludes = new String[0];

    /**
     * Specifies files, which are excluded in the check. By default, only the
     * files matching the default exclude patterns are excluded.
     *
     * @deprecated use {@link LicenseSet#excludes}
     */
    @Deprecated
    @Parameter(alias = "excludes", property = "license.excludes")
    public String[] legacyConfigExcludes = new String[0];

    /**
     * Specify the list of keywords to use to detect a header. A header must
     * include all keywords to be valid. By default, the word 'copyright' is
     * used. Detection is done case insensitive.
     *
     * @deprecated use {@link LicenseSet#keywords}
     */
    @Deprecated
    @Parameter(alias = "keywords")
    public String[] legacyConfigKeywords = new String[]{"copyright"};

    /**
     * Specify if you want to use default exclusions besides the files you have
     * excluded. Default exclusions exclude CVS and SVN folders, IDE descriptors
     * and so on.
     *
     * This is named `defaultUseDefaultExcludes` as it will be used as the default
     * value for whether to use default excludes. This default value can be overridden
     * in each LicenseSet by setting {@link LicenseSet#useDefaultExcludes}.
     */
    @Parameter(property = "license.useDefaultExcludes", defaultValue = "true", alias = "useDefaultExcludes")
    public boolean defaultUseDefaultExcludes = true;

    /**
     * You can set this flag to true if you want to check the headers for all
     * modules of your project. Only used for multi-modules projects, to check
     * for example the header licenses from the parent module for all sub
     * modules.
     */
    @Parameter(property = "license.aggregate", defaultValue = "false")
    public boolean aggregate = false;

    /**
     * Set mapping between document mapping and a supported type to use. This
     * section is very useful when you want to customize the supported
     * extensions. If your project is using file extensions not supported by
     * default by this plugin, you can add a mapping to attach the extension to
     * an existing type of comment. The tag name is the new extension name to
     * support, and the value is the name of the comment type to use.
     */
    @Parameter
    public LinkedHashMap<String, String> mapping = new LinkedHashMap<String, String>();

    /**
     * Whether to use the default mapping between file extensions and comment
     * types, or only the one your provide.
     */
    @Parameter(property = "license.useDefaultMapping", defaultValue = "true")
    public boolean useDefaultMapping = true;

    /**
     * Maven license plugin uses concurrency to check license headers. This
     * factor is used to control the number of threads used to check. The rule
     * is:
     * <br> {@code <nThreads> = <number of cores> *  concurrencyFactor}
     * <br>
     * The default is 1.5.
     */
    @Parameter(property = "license.concurrencyFactor", defaultValue = "1.5")
    public float concurrencyFactor = 1.5f;


    /**
     * Maven license plugin uses concurrency to check license headers. With this
     * option the number of threads used to check can be specified. If given
     * it take precedence over <code>concurrencyFactor</code>
     *
     * The default is 0 which implies that the default for <code>concurrencyFactor</code>
     * is used.
     */
    @Parameter(property = "license.nThreads", defaultValue = "0")
    public int nThreads;

    /**
     * Whether to skip the plugin execution
     */
    @Parameter(property = "license.skip", defaultValue = "false")
    public boolean skip = false;

    /**
     * If you do not want to see the list of file having a missing header, you
     * can add the quiet flag that will shorten the output
     */
    @Parameter(property = "license.quiet", defaultValue = "false")
    public boolean quiet = false;

    /**
     * Set to true if you need a strict check against the headers. By default,
     * the existence of a header is verified by taking the top portion of a file
     * and checking if it contains the headers text, not considering special
     * characters (spaces, tabs, ...).
     * <br>
     * We highly recommend to keep this option set to {@code true}.
     */
    @Parameter(property = "license.strictCheck", defaultValue = "true")
    public boolean strictCheck = true;

    /**
     * Specify the encoding of your files. Default to the project source
     * encoding property (project.build.sourceEncoding).
     */
    @Parameter(property = "license.encoding", defaultValue = "${project.build.sourceEncoding}")
    public String encoding = "UTF-8";

    /**
     * You can set this flag to false if you do not want the build to fail when
     * some headers are missing.
     */
    @Parameter(property = "license.failIfMissing", defaultValue = "true")
    public boolean failIfMissing = true;

    /**
     * You can leave this flag on {@code false} if you do not want the build to
     * fail for files that do not have an implicit or explicit comment style
     * definition. Setting this explicitly to {@code true} is a safe way to make
     * sure that the effective file type mappings cover all files included from
     * your project.
     * <p>
     * Default is {@code false} for backwards compatibility reasons.
     *
     * @since 2.8
     */
    @Parameter(property = "license.failIfUnknown", defaultValue = "false")
    public boolean failIfUnknown = false;

    /**
     * If dryRun is enabled, calls to license:format and license:remove will not
     * overwrite the existing file but instead write the result to a new file
     * with the same name but ending with `.licensed`.
     */
    @Parameter(property = "license.dryRun", defaultValue = "false")
    public boolean dryRun = false;

    /**
     * Skip the formatting of files which already contain a detected header.
     */
    @Parameter(property = "license.skipExistingHeaders", defaultValue = "false")
    public boolean skipExistingHeaders = false;

    @Component
    public MavenProject project;

    /**
     * Maven settings.
     */
    @Component
    private Settings settings;
    /**
     * The decrypter for passwords.
     */
    @Component
    private SettingsDecrypter settingsDecrypter;

    protected abstract class AbstractCallback implements Callback {

        /**
         * Related to {@link #failIfUnknown}.
         */
        private final Collection<File> unknownFiles = new ConcurrentLinkedQueue<File>();

        @Override
        public void onUnknownFile(Document document, Header header) {
            warn("Unknown file extension: %s", document.getFilePath());
            unknownFiles.add(document.getFile());
        }

        public void checkUnknown() throws MojoExecutionException {
            if (!unknownFiles.isEmpty()) {
                String msg = "Unable to find a comment style definition for some "
                        + "files. You may want to add a custom mapping for the relevant file extensions.";
                if (failIfUnknown) {
                    throw new MojoExecutionException(msg);
                }
                getLog().warn(msg);
            }
        }

    }

    @SuppressWarnings({"unchecked"})
    public final void execute(final Callback callback) throws MojoExecutionException, MojoFailureException {
        if (!skip) {

            // make default base dir canonical
            this.defaultBasedir = this.getCanonicalFile(this.defaultBasedir, "license.basedir");

            // collect all the license sets together
            final LicenseSet[] allLicenseSets;

            // if we abandon the legacy config this contiguous block can be removed
            final LicenseSet legacyLicenseSet = convertLegacyConfigToLicenseSet();
            if (legacyLicenseSet != null) {
                if (licenseSets == null) {
                    allLicenseSets = new LicenseSet[]{legacyLicenseSet};
                } else {
                    allLicenseSets = Arrays.copyOf(licenseSets, licenseSets.length + 1);
                    allLicenseSets[licenseSets.length] = legacyLicenseSet;
                }
            } else {
                allLicenseSets = licenseSets;
            }

            // execute
            executeForLicenseSets(allLicenseSets, callback);
        }
    }

    private File getCanonicalFile(final File file, final String description) throws MojoFailureException {
        if (file == null) {
            return null;
        }
        try {
            return file.getCanonicalFile();
        } catch (final IOException e) {
            throw new MojoFailureException("Could not get canonical path of " + description, e);
        }
    }

    private void executeForLicenseSets(final LicenseSet[] licenseSets, final Callback callback) throws MojoFailureException, MojoExecutionException {
        if (licenseSets == null || licenseSets.length == 0) {
            warn("At least one licenseSet must be specified");
            return;
        }

        // need to perform validation first
        for (int i = 0 ; i < licenseSets.length; i++) {
            final LicenseSet licenseSet = licenseSets[i];
            if (!hasHeader(licenseSet)) {
                warn("No header file specified to check for license in licenseSet: " + i);
                return;
            }
            // make licenseSet baseDir canonical
            licenseSet.basedir = this.getCanonicalFile(licenseSet.basedir, "licenseSet[" + i + "].basedir");
        }
        if (!strictCheck) {
            warn("Property 'strictCheck' is not enabled. Please consider adding <strictCheck>true</strictCheck> in your pom.xml file.");
            warn("See http://mycila.github.io/license-maven-plugin for more information.");
        }

        // then execute each license set
        for (final LicenseSet licenseSet : licenseSets) {
            executeForLicenseSet(licenseSet, callback);
        }
    }

    private LicenseSet convertLegacyConfigToLicenseSet() {
        if (legacyConfigHeader == null && (this.legacyConfigInlineHeader == null || this.legacyConfigInlineHeader.isEmpty())) {
            return null;
        }

        final LicenseSet legacyLicenseSet = new LicenseSet();
        legacyLicenseSet.header = legacyConfigHeader;
        legacyLicenseSet.inlineHeader = legacyConfigInlineHeader;
        legacyLicenseSet.validHeaders = legacyConfigValidHeaders;
        legacyLicenseSet.multi = legacyConfigMulti;
        legacyLicenseSet.headerSections = legacyConfigHeaderSections;
        legacyLicenseSet.includes = legacyConfigIncludes;
        legacyLicenseSet.excludes = legacyConfigExcludes;
        legacyLicenseSet.keywords = legacyConfigKeywords;
        return legacyLicenseSet;
    }

    private void executeForLicenseSet(final LicenseSet licenseSet, final Callback callback) throws MojoExecutionException, MojoFailureException {
        final ResourceFinder finder = new ResourceFinder(firstNonNull(asPath(licenseSet.basedir), asPath(defaultBasedir)));
        try {
            finder.setCompileClassPath(project.getCompileClasspathElements());
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        finder.setPluginClassPath(getClass().getClassLoader());

        final HeaderSource headerSource = HeaderSource.of(licenseSet.multi, licenseSet.inlineHeader, licenseSet.header, this.encoding, finder);
        final Header h = new Header(headerSource, licenseSet.headerSections);
        debug("Header: %s", h.getLocation());

        if (licenseSet.validHeaders == null) {
            licenseSet.validHeaders = new String[0];
        }
        final List<Header> validHeaders = new ArrayList<Header>(licenseSet.validHeaders.length);
        for (final String validHeader : licenseSet.validHeaders) {
            final HeaderSource validHeaderSource = HeaderSource.of(null, null, validHeader, this.encoding, finder);
            validHeaders.add(new Header(validHeaderSource, licenseSet.headerSections));
        }

        final List<PropertiesProvider> propertiesProviders = new LinkedList<PropertiesProvider>();
        for (final PropertiesProvider provider : ServiceLoader.load(PropertiesProvider.class, Thread.currentThread().getContextClassLoader())) {
            propertiesProviders.add(provider);
        }
        final DocumentPropertiesLoader propertiesLoader = new DocumentPropertiesLoader() {
            @Override
            public Properties load(final Document document) {
                final Properties props = new Properties();

                for (final Map.Entry<String, String> entry : mergeProperties(licenseSet, document).entrySet()) {
                    if (entry.getValue() != null) {
                        props.setProperty(entry.getKey(), entry.getValue());
                    } else {
                        props.remove(entry.getKey());
                    }
                }
                for (final PropertiesProvider provider : propertiesProviders) {
                    try {
                        final Map<String, String> providerProperties = provider.getAdditionalProperties(AbstractLicenseMojo.this, props, document);
                        if (getLog().isDebugEnabled()) {
                            getLog().debug("provider: " + provider.getClass() + " brought new properties\n" + providerProperties);
                        }
                        for (Map.Entry<String, String> entry : providerProperties.entrySet()) {
                            if (entry.getValue() != null) {
                                props.setProperty(entry.getKey(), entry.getValue());
                            } else {
                                props.remove(entry.getKey());
                            }
                        }
                    } catch (Exception e) {
                        getLog().warn("failure occurred while calling " + provider.getClass(), e);
                    }
                }
                return props;
            }
        };

        final DocumentFactory documentFactory = new DocumentFactory(firstNonNull(licenseSet.basedir, defaultBasedir), buildMapping(), buildHeaderDefinitions(licenseSet, finder), encoding, licenseSet.keywords, propertiesLoader);

        int nThreads = getNumberOfExecutorThreads();
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        CompletionService completionService = new ExecutorCompletionService(executorService);
        int count = 0;
        debug("Number of execution threads: %s", nThreads);
```java
        return str.replaceAll("\\.", "*");

/*
 * Copyright 2010 Henry Coles
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package org.pitest.mutationtest.commandline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.pitest.mutationtest.config.ReportOptions.DEFAULT_CHILD_JVM_ARGS;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pitest.mutationtest.config.ConfigOption;
import org.pitest.mutationtest.config.PluginServices;
import org.pitest.mutationtest.config.ReportOptions;
import org.pitest.mutationtest.engine.gregor.GregorMutationEngine;
import org.pitest.mutationtest.engine.gregor.mutators.ConditionalsBoundaryMutator;
import org.pitest.mutationtest.engine.gregor.mutators.MathMutator;

public class OptionsParserTest {

  private static final String JAVA_PATH_SEPARATOR      = "/";
  private static final String JAVA_CLASS_PATH_PROPERTY = "java.class.path";

  private static final String TEST_PLUGIN_ARG          = "--testPlugin";
  private static final String REPORT_DIR_ARG           = "--reportDir";
  private static final String TARGET_CLASSES_ARG       = "--targetClasses";
  private static final String SOURCE_DIRS_ARG          = "--sourceDirs";
  private static final String DEPENDENCY_DISTANCE_ARG  = "--dependencyDistance";
  private static final String JVM_ARGS_ARG             = "--jvmArgs";
  private static final String MUTATORS_ARG             = "--mutators";
  private static final String FEATURES_ARG             = "--features";
  private static final String DETECT_INLINED_CODE_ARG  = "--detectInlinedCode";
  private static final String TIMESTAMPED_REPORTS_ARG  = "--timestampedReports";
  private static final String THREADS_ARG              = "--threads";
  private static final String TIMEOUT_FACTOR_ARG       = "--timeoutFactor";
  private static final String TIMEOUT_CONST_ARG        = "--timeoutConst";
  private static final String TARGET_TEST_ARG          = "--targetTest";
  private static final String TARGET_TESTS_ARG         = "--targetTests";
  private static final String EXCLUDED_TEST_CLASSES_ARG= "--excludedTestClasses";
  private static final String EXCLUDED_CLASSES_ARG     = "--excludedClasses";
  private static final String AVOID_CALLS_TO_ARG       = "--avoidCallsTo";
  private static final String EXCLUDED_METHODS_ARG     = "--excludedMethods";
  private static final String VERBOSE_ARG              = "--verbose";
  private static final String OUTPUT_FORMATS_ARG       = "--outputFormats";
  private static final String CLASS_PATH_ARG           = "--classPath";
  private static final String CLASS_PATH_FILE_ARG      = "--classPathFile";
  private static final String FAIL_WHEN_NO_MUTATIONS_ARG = "--failWhenNoMutations";
  private static final String SKIP_FAILING_TESTS_ARG   = "--skipFailingTests";
  private static final String MUTABLE_CODE_PATHS_ARG   = "--mutableCodePaths";
  private static final String EXCLUDED_GROUPS_ARG      = "--excludedGroups";
  private static final String INCLUDED_GROUPS_ARG      = "--includedGroups";
  private static final String INCLUDED_TEST_METHODS_ARG= "--includedTestMethods";
  private static final String MUTATION_UNIT_SIZE_ARG   = "--mutationUnitSize";
  private static final String HISTORY_INPUT_LOCATION_ARG = "--historyInputLocation";
  private static final String HISTORY_OUTPUT_LOCATION_ARG = "--historyOutputLocation";
  private static final String MUTATION_THRESHOLD_ARG   = "--mutationThreshold";
  private static final String TEST_STRENGTH_THRESHOLD_ARG = "--testStrengthThreshold";
  private static final String MAX_SURVIVING_ARG        = "--maxSurviving";
  private static final String COVERAGE_THRESHOLD_ARG   = "--coverageThreshold";

  private OptionsParser       testee;

  @Mock
  private Predicate<String>   filter;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(this.filter.test(any(String.class))).thenReturn(true);
    this.testee = new OptionsParser(this.filter);
  }

  @Test
  public void shouldParseTestPlugin() {
    final String value = "foo";
    final ReportOptions actual = parseAddingRequiredArgs(TEST_PLUGIN_ARG, value);
    assertEquals(value, actual.getTestPlugin());
  }

  @Test
  public void shouldParseReportDir() {
    final String value = "foo";
    final ReportOptions actual = parseAddingRequiredArgs(REPORT_DIR_ARG, value);
    assertEquals(value, actual.getReportDir());
  }

  @Test
  public void shouldCreatePredicateFromCommaSeparatedListOfTargetClassGlobs() {
    final ReportOptions actual = parseAddingRequiredArgs(TARGET_CLASSES_ARG,
        "foo*,bar*");
    final Predicate<String> actualPredicate = actual.getTargetClassesFilter();
    assertTrue(actualPredicate.test("foo_anything"));
    assertTrue(actualPredicate.test("bar_anything"));
    assertFalse(actualPredicate.test("notfoobar"));
  }

  @Test
  public void shouldParseCommaSeparatedListOfSourceDirectories() {
    final ReportOptions actual = parseAddingRequiredArgs(SOURCE_DIRS_ARG,
        "foo/bar,bar/far");
    assertEquals(Arrays.asList(new File("foo/bar"), new File("bar/far")), actual.getSourceDirs());
  }

  @Test
  public void shouldParseMaxDepenencyDistance() {
    final ReportOptions actual = parseAddingRequiredArgs(
        DEPENDENCY_DISTANCE_ARG, "42");
    assertEquals(42, actual.getDependencyAnalysisMaxDistance());
  }

  @Test
  public void shouldParseCommaSeparatedListOfJVMArgs() {
    final ReportOptions actual = parseAddingRequiredArgs(JVM_ARGS_ARG, "foo,bar");

    List<String> expected = new ArrayList<>();
    expected.addAll(DEFAULT_CHILD_JVM_ARGS);
    expected.add("foo");
    expected.add("bar");
    assertEquals(expected, actual.getJvmArgs());
  }

  @Test
  public void shouldParseCommaSeparatedListOfMutationOperators() {
    final ReportOptions actual = parseAddingRequiredArgs(MUTATORS_ARG,
        ConditionalsBoundaryMutator.CONDITIONALS_BOUNDARY_MUTATOR.name() + ","
            + MathMutator.MATH_MUTATOR.name());
    assertEquals(Arrays.asList(
        ConditionalsBoundaryMutator.CONDITIONALS_BOUNDARY_MUTATOR.name(),
        MathMutator.MATH_MUTATOR.name()), actual.getMutators());
  }

  @Test
  public void shouldParseCommaSeparatedListOfFeatures() {
    final ReportOptions actual = parseAddingRequiredArgs(FEATURES_ARG, "+FOO(),-BAR(value=1 & value=2)");
    assertThat(actual.getFeatures()).contains("+FOO()", "-BAR(value=1 & value=2)");
  }

  @Test
  public void shouldNotDetectInlinedCodeByDefault() {
    final ReportOptions actual = parseAddingRequiredArgs("");
    assertFalse(actual.isDetectInlinedCode());
  }

  @Test
  public void shouldDetermineIfInlinedCodeFlagIsSet() {
    final ReportOptions actual = parseAddingRequiredArgs(DETECT_INLINED_CODE_ARG);
    assertTrue(actual.isDetectInlinedCode());
  }

  @Test
  public void shouldDetermineIfInlinedCodeFlagIsSetWhenFalseSupplied() {
    final ReportOptions actual = parseAddingRequiredArgs(DETECT_INLINED_CODE_ARG + "=false");
    assertFalse(actual.isDetectInlinedCode());
  }

  @Test
  public void shouldCreateTimestampedReportsByDefault() {
    final ReportOptions actual = parseAddingRequiredArgs();
    assertTrue(actual.shouldCreateTimeStampedReports());
  }

  @Test
  public void shouldDetermineIfSuppressTimestampedReportsFlagIsSet() {
    final ReportOptions actual = parseAddingRequiredArgs(TIMESTAMPED_REPORTS_ARG);
    assertTrue(actual.shouldCreateTimeStampedReports());
  }

  @Test
  public void shouldDetermineIfSuppressTimestampedReportsFlagIsSetWhenFalseSupplied() {
    final ReportOptions actual = parseAddingRequiredArgs(TIMESTAMPED_REPORTS_ARG + "=false");
    assertFalse(actual.shouldCreateTimeStampedReports());
  }

  @Test
  public void shouldParseNumberOfThreads() {
    final ReportOptions actual = parseAddingRequiredArgs(THREADS_ARG, "42");
    assertEquals(42, actual.getNumberOfThreads());
  }

  @Test
  public void shouldParseTimeOutFactor() {
    final ReportOptions actual = parseAddingRequiredArgs(TIMEOUT_FACTOR_ARG,
        "1.32");
    assertEquals(1.32f, actual.getTimeoutFactor(), 0.1);
  }

  @Test
  public void shouldParseTimeOutConstant() {
    final ReportOptions actual = parseAddingRequiredArgs(TIMEOUT_CONST_ARG, "42");
    assertEquals(42, actual.getTimeoutConstant());
  }

  @Test
  public void shouldParseCommaSeparatedListOfTargetTestClassGlobs() {
    final ReportOptions actual = parseAddingRequiredArgs(TARGET_TEST_ARG,
        "foo*,bar*");
    final Predicate<String> actualPredicate = actual.getTargetTestsFilter();
    assertTrue(actualPredicate.test("foo_anything"));
    assertTrue(actualPredicate.test("bar_anything"));
    assertFalse(actualPredicate.test("notfoobar"));
  }

  @Test
  public void shouldParseCommaSeparatedListOfTargetTestClassGlobAsRegex() {
    ReportOptions actual = parseAddingRequiredArgs(TARGET_TEST_ARG,
            "~foo\\w*,~bar.*");
    Predicate<String> actualPredicate = actual.getTargetTestsFilter();
    assertTrue(actualPredicate.test("foo_anything"));
    assertTrue(actualPredicate.test("bar_anything"));
    assertFalse(actualPredicate.test("notfoobar"));
    actual = parseAddingRequiredArgs(TARGET_TEST_ARG,
            "~.*?foo\\w*,~bar.*");
    actualPredicate = actual.getTargetTestsFilter();
    assertTrue(actualPredicate.test("notfoobar"));
  }

  @Test
  public void shouldUseTargetClassesFilterForTestsWhenNoTargetTestsFilterSupplied() {
    final ReportOptions actual = parseAddingRequiredArgs(TARGET_CLASSES_ARG,
        "foo*,bar*");
    final Predicate<String> actualPredicate = actual.getTargetTestsFilter();
    assertTrue(actualPredicate.test("foo_anything"));
    assertTrue(actualPredicate.test("bar_anything"));
    assertFalse(actualPredicate.test("notfoobar"));
  }

  @Test
  public void shouldParseCommaSeparatedListOfExcludedTestClassGlobs() {
    final ReportOptions actual = parseAddingRequiredArgs(EXCLUDED_TEST_CLASSES_ARG,
        "foo*", TARGET_TESTS_ARG, "foo*,bar*", TARGET_CLASSES_ARG, "foo*,bar*");
    final Predicate<String> testPredicate = actual.getTargetTestsFilter();
    assertFalse(testPredicate.test("foo_anything"));
    assertTrue(testPredicate.test("bar_anything"));
  }

  @Test
  public void shouldParseCommaSeparatedListOfExcludedClassGlobsAndApplyTheseToTargets() {
    final ReportOptions actual = parseAddingRequiredArgs(EXCLUDED_CLASSES_ARG,
        "foo*", TARGET_TESTS_ARG, "foo*,bar*", TARGET_CLASSES_ARG, "foo*,bar*");

    final Predicate<String> targetPredicate = actual.getTargetClassesFilter();
    assertFalse(targetPredicate.test("foo_anything"));
    assertTrue(targetPredicate.test("bar_anything"));
  }

  @Test
  public void shouldDefaultLoggingPackagesToDefaultsDefinedByDefaultMutationConfigFactory() {
    final ReportOptions actual = parseAddingRequiredArgs();
    assertEquals(ReportOptions.LOGGING_CLASSES, actual.getLoggingClasses());
  }

  @Test
  public void shouldParseCommaSeparatedListOfClassesToAvoidCallTo() {
    final ReportOptions actual = parseAddingRequiredArgs(AVOID_CALLS_TO_ARG,
        "foo,bar,foo.bar");
    assertEquals(Arrays.asList("foo", "bar", "foo.bar"),
        actual.getLoggingClasses());
  }

  @Test
  public void shouldParseCommaSeparatedListOfExcludedMethods() {
    final ReportOptions actual = parseAddingRequiredArgs(EXCLUDED_METHODS_ARG,
        "foo*,bar*,car");
    final Collection<String> actualPredicate = actual
        .getExcludedMethods();
    assertThat(actualPredicate).containsExactlyInAnyOrder("foo*", "bar*", "car");
  }

  @Test
  public void shouldParseVerboseFlag() {
    final ReportOptions actual = parseAddingRequiredArgs(VERBOSE_ARG);
    assertTrue(actual.isVerbose());
  }

  @Test
  public void shouldDefaultToHtmlReportWhenNoOutputFormatsSpecified() {
    final ReportOptions actual = parseAddingRequiredArgs();
    assertEquals(new HashSet<>(Arrays.asList("HTML")),
        actual.getOutputFormats());
  }

  @Test
  public void shouldParseCommaSeparatedListOfOutputFormatsWhenSupplied() {
    final ReportOptions actual = parseAddingRequiredArgs(OUTPUT_FORMATS_ARG,
        "HTML,CSV");
    assertEquals(new HashSet<>(Arrays.asList("HTML", "CSV")),
        actual.getOutputFormats());
  }

  @Test
  public void shouldAcceptCommaSeparatedListOfAdditionalClassPathElements() {
    final ReportOptions ro = parseAddingRequiredArgs(CLASS_PATH_ARG,
        "/foo/bar,./boo");
    final Collection<String> actual = ro.getClassPathElements();
    assertTrue(actual.contains("/foo/bar"));
    assertTrue(actual.contains("./boo"));
  }

  @Test
  public void shouldAcceptFileWithListOfAdditionalClassPathElements() {
    final ClassLoader classLoader = getClass().getClassLoader();
    final File classPathFile = new File(classLoader.getResource("testClassPathFile.txt").getFile());
    final ReportOptions ro = parseAddingRequiredArgs(CLASS_PATH_FILE_ARG,
        classPathFile.getAbsolutePath());
    final Collection<String> actual = ro.getClassPathElements();
    assertTrue(actual.contains("C:/foo"));
    assertTrue(actual.contains("/etc/bar"));
  }

  @Test
  public void shouldDetermineIfFailWhenNoMutationsFlagIsSet() {
    assertTrue(parseAddingRequiredArgs(FAIL_WHEN_NO_MUTATIONS_ARG, "true")
        .shouldFailWhenNoMutations());
    assertFalse(parseAddingRequiredArgs(FAIL_WHEN_NO_MUTATIONS_ARG, "false")
        .shouldFailWhenNoMutations());
  }

  @Test
  public void shouldFailWhenNoMutationsSetByDefault() {
    assertTrue(parseAddingRequiredArgs("").shouldFailWhenNoMutations());
  }

  @Test
  public void shouldDetermineIfSkipFailingTestsFlagIsSet() {
    assertTrue(parseAddingRequiredArgs(SKIP_FAILING_TESTS_ARG, "true")
        .skipFailingTests());
    assertFalse(parseAddingRequiredArgs(SKIP_FAILING_TESTS_ARG, "false")
        .skipFailingTests());
  }

  @Test
  public void shouldSkipFailingTestsNotSetByDefault() {
    assertFalse(parseAddingRequiredArgs("").skipFailingTests());
  }
  
  @Test
  public void shouldParseCommaSeparatedListOfMutableCodePaths() {
    final ReportOptions actual = parseAddingRequiredArgs(MUTABLE_CODE_PATHS_ARG,
        "foo,bar");
    assertEquals(Arrays.asList("foo", "bar"), actual.getCodePaths());
  }

  @Test
  public void shouldParseCommaSeparatedListOfExcludedTestGroups() {
    final ReportOptions actual = parseAddingRequiredArgs(EXCLUDED_GROUPS_ARG,
        "foo,bar");
    assertEquals(Arrays.asList("foo", "bar"), actual.getGroupConfig()
        .getExcludedGroups());
  }

  @Test
  public void shouldParseCommaSeparatedListOfIncludedTestGroups() {
    final ReportOptions actual = parseAddingRequiredArgs(INCLUDED_GROUPS_ARG,
        "foo,bar");
    assertEquals(Arrays.asList("foo", "bar"), actual.getGroupConfig()
        .getIncludedGroups());
  }

  @Test
  public void shouldParseCommaSeparatedListOfIncludedTestMethods() {
    final ReportOptions actual = parseAddingRequiredArgs(INCLUDED_TEST_METHODS_ARG,
            "foo,bar");
    assertEquals(Arrays.asList("foo", "bar"), actual
        .getIncludedTestMethods());
  }

  @Test
  public void shouldParseMutationUnitSize() {
    final ReportOptions actual = parseAddingRequiredArgs(MUTATION_UNIT_SIZE_ARG,
        "50");
    assertEquals(50, actual.getMutationUnitSize());
  }

  @Test
  public void shouldDefaultMutationUnitSizeToCorrectValue() {
    final ReportOptions actual = parseAddingRequiredArgs("");
    assertEquals(
        (int) ConfigOption.MUTATION_UNIT_SIZE.getDefault(Integer.class),
        actual.getMutationUnitSize());
  }

  @Test
  public void shouldDefaultToNoHistory() {
    final ReportOptions actual = parseAddingRequiredArgs("");
    assertNull(actual.getHistoryInputLocation());
    assertNull(actual.getHistoryOutputLocation());
  }

  @Test
  public void shouldParseHistoryInputLocation() {
    final ReportOptions actual = parseAddingRequiredArgs(
        HISTORY_INPUT_LOCATION_ARG, "foo");
    assertEquals(new File("foo"), actual.getHistoryInputLocation());
  }

  @Test
  public void shouldParseHistoryOutputLocation() {
    final ReportOptions actual = parseAddingRequiredArgs(
        HISTORY_OUTPUT_LOCATION_ARG, "foo");
    assertEquals(new File("foo"), actual.getHistoryOutputLocation());
  }

  @Test
  public void shouldParseMutationThreshold() {
    final ReportOptions actual = parseAddingRequiredArgs(MUTATION_THRESHOLD_ARG,
        "42");
    assertEquals(42, actual.getMutationThreshold());
  }

  @Test
  public void shouldParseTestStrengthThreshold() {
    final ReportOptions actual = parseAddingRequiredArgs(TEST_STRENGTH_THRESHOLD_ARG,
            "50");
    assertEquals(50, actual.getTestStrengthThreshold());
  }

  @Test
  public void shouldParseMaximumAllowedSurvivingMutants() {
    final ReportOptions actual = parseAddingRequiredArgs(MAX_SURVIVING_ARG,
        "42");
    assertEquals(42, actual.getMaximumAllowedSurvivors());
  }

  @Test
  public void shouldParseCoverageThreshold() {
    final ReportOptions actual = parseAddingRequiredArgs(COVERAGE_THRESHOLD_ARG,
        "42");
    assertEquals(
    assertEquals("gregor", actual.getMutationEngine());
  }

  @Test
  public void shouldParseMutationEnigne() {
    final ReportOptions actual = parseAddingRequiredArgs("--mutationEngine",
        "foo");
    assertEquals("foo", actual.getMutationEngine());
  }

  @Test
  public void shouldDefaultJVMToNull() {
    final ReportOptions actual = parseAddingRequiredArgs();
    assertEquals(null, actual.getJavaExecutable());
  }

  @Test
  public void shouldParseJVM() {
    final ReportOptions actual = parseAddingRequiredArgs("--jvmPath", "foo");
    assertEquals("foo", actual.getJavaExecutable());
  }

  @Test
  public void shouldParseExportLineCoverageFlag() {
    final ReportOptions actual = parseAddingRequiredArgs("--exportLineCoverage");
    assertTrue(actual.shouldExportLineCoverage());
  }

  @Test
  public void shouldNotExportLineCoverageWhenFlagNotSet() {
    final ReportOptions actual = parseAddingRequiredArgs("");
    assertFalse(actual.shouldExportLineCoverage());
  }

  @Test
  public void shouldIncludeLaunchClasspathByDefault() {
    final ReportOptions actual = parseAddingRequiredArgs("");
    assertTrue(actual.isIncludeLaunchClasspath());
  }

  @Test
  public void shouldNotIncludeLaunchClasspathWhenFlagUnset() {
    final ReportOptions actual = parseAddingRequiredArgs("--includeLaunchClasspath=false");
    assertFalse(actual.isIncludeLaunchClasspath());
  }

  @Test
  public void shouldIncludeLaunchClasspathWhenFlag() {
    final ReportOptions actual = parseAddingRequiredArgs("--includeLaunchClasspath=true");
    assertTrue(actual.isIncludeLaunchClasspath());
  }

  @Test
  public void shouldHandleNotCanonicalLaunchClasspathElements() {
    final String oldClasspath = System.getProperty(JAVA_CLASS_PATH_PROPERTY);
    try {
      // given
      final PluginServices plugins = PluginServices.makeForContextLoader();
      this.testee = new OptionsParser(new PluginFilter(plugins));
      // and
      System.setProperty(JAVA_CLASS_PATH_PROPERTY,
          getNonCanonicalGregorEngineClassPath());
      // when
      final ReportOptions actual = parseAddingRequiredArgs("--includeLaunchClasspath=false");
      // then
      assertThat(actual.getClassPath().findClasses(gregorClass())).hasSize(1);
    } finally {
      System.setProperty(JAVA_CLASS_PATH_PROPERTY, oldClasspath);
    }
  }

  @Test
  public void shouldCreateEmptyPluginPropertiesWhenNoneSupplied() {
    final ReportOptions actual = parseAddingRequiredArgs("");
    assertNotNull(actual.getFreeFormProperties());
  }

  @Test
  public void shouldIncludePluginPropertyValuesWhenSingleKey() {
    final ReportOptions actual = parseAddingRequiredArgs("-pluginConfiguration=foo=1");
    assertEquals("1", actual.getFreeFormProperties().getProperty("foo"));
  }

  @Test
  public void shouldIncludePluginPropertyValuesWhenMultipleKeys() {
    final ReportOptions actual = parseAddingRequiredArgs(
        "-pluginConfiguration=foo=1", "-pluginConfiguration=bar=2");
    assertEquals("1", actual.getFreeFormProperties().getProperty("foo"));
    assertEquals("2", actual.getFreeFormProperties().getProperty("bar"));
  }

  @Test
  public void shouldDefaultToNotUsingAClasspathJar() {
    final ReportOptions actual = parseAddingRequiredArgs("");
    assertFalse(actual.useClasspathJar());
  }
  
  @Test
  public void shouldUseClasspathJarWhenFlagSet() {
    final ReportOptions actual = parseAddingRequiredArgs("--useClasspathJar=true");
    assertTrue(actual.useClasspathJar());
  }
  
  @Test
  public void shouldDefaultMatrixFlagToFalse() {
    final ReportOptions actual = parseAddingRequiredArgs("");
    assertFalse(actual.isFullMutationMatrix());
  }
  
  @Test
  public void shouldParseMatrixFlag() {
    final ReportOptions actual = parseAddingRequiredArgs("--fullMutationMatrix=true");
    assertTrue(actual.isFullMutationMatrix());
  }
  
  private String getNonCanonicalGregorEngineClassPath() {
    final String gregorEngineClassPath = GregorMutationEngine.class
        .getProtectionDomain().getCodeSource().getLocation().getFile();
    final int lastOccurrenceOfFileSeparator = gregorEngineClassPath
        .lastIndexOf(JAVA_PATH_SEPARATOR);
    return new StringBuilder(gregorEngineClassPath).replace(
        lastOccurrenceOfFileSeparator, lastOccurrenceOfFileSeparator + 1,
        JAVA_PATH_SEPARATOR + "." + JAVA_PATH_SEPARATOR).toString();
  }

  private Predicate<String> gregorClass() {
    return s -> GregorMutationEngine.class.getName().equals(s);
  }

  private ReportOptions parseAddingRequiredArgs(final String... args) {

    final List<String> a = new ArrayList<>();
    a.addAll(Arrays.asList(args));
    addIfNotPresent(a, "--targetClasses");
    addIfNotPresent(a, "--reportDir");
    addIfNotPresent(a, "--sourceDirs");
    return parse(a.toArray(new String[a.size()]));
  }

  private void addIfNotPresent(final List<String> uniqueArgs, final String value) {
    if (!uniqueArgs.contains(value)) {
      uniqueArgs.add(value);
      uniqueArgs.add("ignore");
    }
  }

  private ReportOptions parse(final String... args) {
    return this.testee.parse(args).getOptions();
  }

}
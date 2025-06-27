package sorald.sonar;

import com.google.auto.service.AutoService;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueListener;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration;
import org.sonarsource.sonarlint.core.commons.RuleKey;
import sorald.SoraldConfig;
import sorald.cli.CLIConfigForStaticAnalyzer;
import sorald.rule.Rule;
import sorald.rule.RuleViolation;
import sorald.rule.StaticAnalyzer;

@AutoService(StaticAnalyzer.class)
public class SonarStaticAnalyzer implements StaticAnalyzer {

        @Override
        public Collection<RuleViolation> findViolations(
                        File projectRoot,
                        List<File> files,
                        List<Rule> rules,
                        CLIConfigForStaticAnalyzer cliOptions, String targetJavaVersion, String javaProjectInfo) {
                return analyze(projectRoot, files, rules, cliOptions, targetJavaVersion, javaProjectInfo);
        }

        private Collection<RuleViolation> analyze(
                        File projectRoot,
                        List<File> files,
                        List<Rule> rules,
                        CLIConfigForStaticAnalyzer cliOptions, String targetJavaVersion, String javaProjectInfo) {

                List<JavaInputFile> inputFiles = files.stream()
                                .map(File::toPath)
                                .map(JavaInputFile::new)
                                .collect(Collectors.toList());
                StandaloneAnalysisConfiguration config;
                if (cliOptions == null) {
                        config = getAnalysisConfigurationWithoutCliOptions(projectRoot, inputFiles, rules,
                                        targetJavaVersion, javaProjectInfo);
                } else {
                        config = getAnalysisConfigurationWithCliOptions(
                                        projectRoot, inputFiles, rules, cliOptions, targetJavaVersion, javaProjectInfo);
                }

                SonarLintEngine sonarLint = SonarLintEngine.getInstance();
                var issueHandler = new IssueHandler();
                sonarLint.analyze(config, issueHandler, null, null);
                sonarLint.stop();
                return issueHandler.issues.stream()
                                .filter(issue -> issue.getTextRange() != null)
                                .map(ScannedViolation::new)
                                .collect(Collectors.toList());
        }

        private static StandaloneAnalysisConfiguration getAnalysisConfigurationWithCliOptions(
                        File projectRoot,
                        List<JavaInputFile> inputFiles,
                        List<Rule> rules,
                        CLIConfigForStaticAnalyzer cliOptions, String targetJavaVersion, String javaProjectInfo) {
                StandaloneAnalysisConfiguration.Builder builder = StandaloneAnalysisConfiguration.builder()
                                .setBaseDir(projectRoot.toPath())
                                // SonarLint takes classpath as a comma separated string to make it OS
                                // independent.
                                // See:
                                // https://github.com/SonarSource/sonar-java/blob/6050868761069bc5ff965a149f2fd9a64d6319e0/sonar-java-plugin/src/main/resources/static/documentation.md#java-analysis-and-bytecode
                                .putExtraProperty(
                                                "sonar.java.libraries", String.join(",", cliOptions.getClasspath()))
                                .addIncludedRules(
                                                rules.stream()
                                                                .map(rule -> RuleKey.parse(String.format("java:%s",
                                                                                rule.getKey())))
                                                                .collect(Collectors.toList()))
                                .addRuleParameters(
                                                putRuleParameters(((SoraldConfig) cliOptions).getRuleParameters()))
                                .addInputFiles(inputFiles);

                setSonarJavaSource(builder, targetJavaVersion, projectRoot, javaProjectInfo);

                return builder.build();
        }

        private static StandaloneAnalysisConfiguration getAnalysisConfigurationWithoutCliOptions(
                        File projectRoot, List<JavaInputFile> inputFiles, List<Rule> rules, String targetJavaVersion,
                        String javaProjectInfo) {
                StandaloneAnalysisConfiguration.Builder builder = StandaloneAnalysisConfiguration.builder()
                                .setBaseDir(projectRoot.toPath())
                                .putExtraProperty("sonar.java.source", "5")
                                .addIncludedRules(
                                                rules.stream()
                                                                .map(rule -> RuleKey.parse(String.format("java:%s",
                                                                                rule.getKey())))
                                                                .collect(Collectors.toList()))
                                .addInputFiles(inputFiles);

                setSonarJavaSource(builder, targetJavaVersion, projectRoot, javaProjectInfo);

                return builder.build();
        }

        private static void setSonarJavaSource(StandaloneAnalysisConfiguration.Builder builder,
                        String targetJavaVersion, File projectRoot, String javaProjectInfo) {
                if (targetJavaVersion != null && !targetJavaVersion.equals("") && !targetJavaVersion.equals("None")) {
                        System.out.println("'sonar.java.source' has been set to " + targetJavaVersion + " for project "
                                        + javaProjectInfo);
                        builder.putExtraProperty("sonar.java.source", targetJavaVersion);
                } else {
                        System.out.println("No targetJavaVersion given for " + javaProjectInfo
                                        + ". 'sonar.java.source' has been set to default of 6.");
                        builder.putExtraProperty("sonar.java.source", "6");
                }
        }

        private static Map<RuleKey, Map<String, String>> putRuleParameters(
                        Map<Rule, Map<String, String>> passedRuleParameters) {
                Map<RuleKey, Map<String, String>> parsedRuleParameters = new HashMap<>();
                passedRuleParameters
                                .keySet()
                                .forEach(
                                                rule -> parsedRuleParameters.put(
                                                                RuleKey.parse(String.format("java:%s", rule.getKey())),
                                                                passedRuleParameters.get(rule)));
                return parsedRuleParameters;
        }

        private static class IssueHandler implements IssueListener {
                private final List<Issue> issues = new ArrayList<>();

                @Override
                public void handle(@Nonnull Issue issue) {
                        issues.add(issue);
                }
        }
}

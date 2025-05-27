package sorald.cli;

import java.util.HashMap;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import sorald.sonar.SonarLintEngine;
import org.sonarsource.sonarlint.core.rule.extractor.SonarLintRuleDefinition;

import picocli.CommandLine;
import sorald.Constants;
import sorald.FileUtils;

/** CLI Command for reading the docu of a SonarQube rule */
@Mojo(name = Constants.DOCU_COMMAND_NAME)
@CommandLine.Command(name = Constants.DOCU_COMMAND_NAME, mixinStandardHelpOptions = true, description = "Read the docu of a SonarQube rule.")
public class ReadRuleDocuCommand extends BaseCommand {
    private static final String SONAR_JAVA_PREFIX = "java:";

    @CommandLine.Option(names = {
            Constants.ARG_RULE_KEY }, description = "The rule key of the rule whos docu to read.", required = true)
    String ruleKey;

    @Override
    public Integer call() throws Exception {

        SonarLintRuleDefinition sonarRuleDefinition =  SonarLintEngine.getAllRulesDefinitionsByKey().get(withLanguage(ruleKey));

        if (sonarRuleDefinition == null) {
            // The rule has not been found (not a valid rule)
            throw new CommandLine.ExecutionException(spec.commandLine(), String.format("The rule %s could not be found. Maybe you mistyped the rule key.", ruleKey));
        }


        // If no output file is given it is printed to the System.out, else to the specified file
        if (statsOutputFile == null) {
            FileUtils.writeJSON(System.out, sonarRuleDefinition, new HashMap<String, Object>());

        } else {
            FileUtils.writeJSON(statsOutputFile, sonarRuleDefinition, new HashMap<String, Object>());
        }

        return 0;
    }

    private static String withLanguage(String ruleKey) {
        if (ruleKey.contains(SONAR_JAVA_PREFIX)) {
            return ruleKey;
        }
        return SONAR_JAVA_PREFIX + ruleKey;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        mavenArgs = getMavenArgs();
        try {
            call();
        } catch (Exception e) {
            getLog().error(e);
        }
    }

}

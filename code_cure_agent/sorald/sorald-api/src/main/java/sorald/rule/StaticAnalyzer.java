package sorald.rule;

import java.io.File;
import java.util.Collection;
import java.util.List;
import sorald.cli.CLIConfigForStaticAnalyzer;

/** A static analyzer for Java source code */
public interface StaticAnalyzer {

    /**
     * Scan files for violations of some rules.
     *
     * @param projectRoot       the root folder of the project.
     * @param files             The files to analyze.
     * @param rule              The rules to use.
     * @param cliOptions        Options for the static analyzer.
     * @param targetJavaVersion The java version that the project compiles to and is
     *                          to be set as sonar.java.source. If null, "" or
     *                          "None", sonar.java.source defaults to 6.
     * @param javaProjectInfo   Info on the project (repo url or path of analyized
     *                          project)
     * @return All violations of the rules found in the files.
     */
    Collection<RuleViolation> findViolations(
            File projectRoot,
            List<File> files,
            List<Rule> rule,
            CLIConfigForStaticAnalyzer cliOptions, String targetJavaVersion, String javaProjectInfo);
}

package sorald.miner;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import org.eclipse.jgit.api.Git;
import sorald.FileUtils;
import sorald.cli.CLIConfigForStaticAnalyzer;
import sorald.event.EventHelper;
import sorald.event.EventType;
import sorald.event.SoraldEventHandler;
import sorald.event.models.miner.MinedViolationEvent;
import sorald.rule.Rule;
import sorald.rule.RuleViolation;
import sorald.sonar.ProjectScanner;
import sorald.sonar.SonarRule;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class MineSonarWarnings {
    final List<SoraldEventHandler> eventHandlers;
    private final CLIConfigForStaticAnalyzer cliOptions;

    public MineSonarWarnings(
            List<? extends SoraldEventHandler> eventHandlers,
            CLIConfigForStaticAnalyzer cliOptions) {
        this.eventHandlers = Collections.unmodifiableList(eventHandlers);
        this.cliOptions = cliOptions;
    }

    /**
     * Mines a single git repository. For that it clones the repository,
     * checks out the specified commit if the commit isn't empty
     * and then runs the analysis if cloning was successful.
     * 
     * @param rules      list of rules to analyze
     * @param outputPath Path where the overview of found warnings is written
     * @param repo       A Pair of repoPath and commitId. The repoPath must be a
     *                   correct URL to the repository.
     *                   The commitId must be a valid commit in the tree of the
     *                   repository,
     *                   or it can be an empty string. Then the master branch is
     *                   used.
     * @param repoDir    The working directory where the repository can be cloned to
     *                   temporarily
     * @throws IOException
     */
    public void mineGitRepo(
            List<Rule> rules, String outputPath, GitRepo repo, File repoDir)
            throws IOException {

        String repoPath = repo.getRepoURL();
        String repoName = repoPath.substring(repoPath.lastIndexOf('/') + 1, repoPath.lastIndexOf("."));

        org.apache.commons.io.FileUtils.cleanDirectory(repoDir);

        boolean isCloned = false;

        try {
            Git git = Git.cloneRepository().setURI(repoPath).setDirectory(repoDir).call();

            // If a commitId was provided checkout the respective commit
            String commitId = repo.getCommit();
            if (!commitId.isEmpty() && !commitId.equals("MASTER")) {
                git.checkout().setName(commitId).call();
            }

            git.close();
            isCloned = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        Map<String, Integer> warnings = null;
        // Only analyze the repoDir if the new repo was succesfully cloned
        if (isCloned) {

            warnings = extractWarnings(repoDir.getAbsolutePath(), rules, repo.getTargetJavaVersion(),
                    repo.getRepoURL());
        }

        PrintWriter pw = new PrintWriter(new FileWriter(outputPath, true));

        if (isCloned) {
            pw.println("RepoName: " + repoName);

            warnings.entrySet().stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .forEach(pw::println);
        } else {
            pw.println("RepoName: " + repoName + " not_cloned");
        }

        pw.flush();
        pw.close();

    }

    public void mineLocalProject(List<Rule> rules, String projectPath, String targetJavaVersion) {
        Map<String, Integer> warnings = extractWarnings(projectPath, rules, targetJavaVersion, projectPath);

        warnings.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .forEach(System.out::println);
    }

    /**
     * @param projectPath       The root path to a Java project
     * @param rules             Rules to find violations of in the Java files in the
     *                          project
     * @param targetJavaVersion The java version that the project compiles to and is
     *                          to be set as sonar.java.source. If null, "" or
     *                          "None", sonar.java.source defaults to 6.
     * @param javaProjectInfo   Info on the project (repo url or path of analyized
     *                          project)
     * @return A mapping (checkClassName<ruleKey> -> numViolations)
     */
    Map<String, Integer> extractWarnings(String projectPath, List<Rule> rules, String targetJavaVersion,
            String javaProjectInfo) {
        final Map<Rule, Integer> warnings = new HashMap<>();
        final var target = new File(projectPath);

        rules.forEach(ruleName -> warnings.put(ruleName, 0));

        Consumer<Rule> incrementWarningCount = (rule) -> warnings.put(rule, warnings.get(rule) + 1);

        EventHelper.fireEvent(EventType.MINING_START, eventHandlers);
        Set<RuleViolation> ruleViolations = ProjectScanner.scanProject(
                target, FileUtils.getClosestDirectory(target), rules, cliOptions, targetJavaVersion, javaProjectInfo);
        EventHelper.fireEvent(EventType.MINING_END, eventHandlers);

        ruleViolations.stream()
                .map(RuleViolation::getRuleKey)
                .map(SonarRule::new)
                .forEach(incrementWarningCount);

        ruleViolations.forEach(
                v -> EventHelper.fireEvent(
                        new MinedViolationEvent(v, Paths.get(projectPath)), eventHandlers));

        Map<String, Integer> warningsWithUpdateKeys = new HashMap<>();
        warnings.forEach((rule, count) -> warningsWithUpdateKeys.put(rule.getKey(), count));

        return warningsWithUpdateKeys;
    }
}

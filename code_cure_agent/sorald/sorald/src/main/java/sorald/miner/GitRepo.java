package sorald.miner;

public class GitRepo {

    private final String repoURL;
    private final String commit;
    private final String targetJavaVersion;

    public GitRepo(String repoURL, String commit, String targetJavaVersion) {
        this.repoURL = repoURL;
        this.commit = commit;
        this.targetJavaVersion = targetJavaVersion;
    }

    public String getRepoURL() {
        return repoURL;
    }

    public String getCommit() {
        return commit;
    }

    public String getTargetJavaVersion() {
        return targetJavaVersion;
    }

}
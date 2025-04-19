#!/bin/bash

# Mine the small devdataset with four different configurations


cd "$(dirname "$0")"

SORALD_JAR_PATH="/workspaces/master-thesis-pascal-joos/repair_agent/sorald/sorald.jar"


# Use the commit that was used for Soralds experiments.
# Only consider rules handled by Sorald.
echo Mining start: Specific commit, handled rules
rm ./mining_results/specific_commit_handled_rules_out.txt
java -jar $SORALD_JAR_PATH mine --git-repos-list ./sampled_repos_specific_commit.txt --miner-output-file ./mining_results/specific_commit_handled_rules_out.txt \
    --stats-output-file ./mining_results/specific_commit_handled_rules_mining_result.json --temp-dir ./temp --stats-on-git-repos --handled-rules
echo Mining end: Specific commit, handled rules

# Use the commit that was used for Soralds experiments.
# Consider all rules supported by SonarQube.
echo Mining start: Specific commit, all rules
rm ./mining_results/specific_commit_all_rules_out.txt
java -jar $SORALD_JAR_PATH mine --git-repos-list ./sampled_repos_specific_commit.txt --miner-output-file ./mining_results/specific_commit_all_rules_out.txt \
    --stats-output-file ./mining_results/specific_commit_all_rules_mining_result.json --temp-dir ./temp --stats-on-git-repos
echo Mining end: Specific commit, all rules

# Use the newest commit for each repo.
# Only consider rules handled by Sorald.
echo Mining start: Newest commit, handled rules
rm ./mining_results/newest_commit_handled_rules_out.txt
java -jar $SORALD_JAR_PATH mine --git-repos-list ./sampled_repos.txt --miner-output-file ./mining_results/newest_commit_handled_rules_out.txt \
    --stats-output-file ./mining_results/newest_commit_handled_rules_mining_result.json --temp-dir ./temp --stats-on-git-repos --handled-rules
echo Mining end: Newest commit, handled rules

# Use the newest commit for each repo.
# Consider all rules supported by SonarQube.
echo Mining start: Newest commit, all rules
rm ./mining_results/newest_commit_all_rules_out.txt
java -jar $SORALD_JAR_PATH mine --git-repos-list ./sampled_repos.txt --miner-output-file ./mining_results/newest_commit_all_rules_out.txt \
    --stats-output-file ./mining_results/newest_commit_all_rules_mining_result.json --temp-dir ./temp --stats-on-git-repos
echo Mining end: Newest commit, all rules
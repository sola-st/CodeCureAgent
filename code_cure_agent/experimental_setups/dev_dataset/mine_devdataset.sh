#!/bin/bash

# Mine the small devdataset with four different configurations. 
# From the mining result create the experiment input files.


cd "$(dirname "$0")"

SORALD_JAR_PATH="/workspaces/master-thesis-pascal-joos/code_cure_agent/sorald/sorald.jar"

qualtiy_profile_rules=$(head -n 1 ./../../sonarqube_quality_profile/quality_profile_rule_keys.txt)

# Use the commit that was used for Soralds experiments.
# Only consider rules handled by Sorald.
echo Mining start: Specific commit, handled rules
rm ./mining_results/specific_commit_handled_rules_out.txt
java -jar $SORALD_JAR_PATH mine --git-repos-list ./sampled_repos_specific_commit.txt --miner-output-file ./mining_results/specific_commit_handled_rules_out.txt \
    --stats-output-file ./mining_results/specific_commit_handled_rules_mining_result.json --temp-dir ./temp --stats-on-git-repos --rule-parameters ./../../sonarqube_quality_profile/quality_profile_rule_parameters.json --handled-rules

python3 ./../prepare_experiment_input_file.py ./mining_results/specific_commit_handled_rules_mining_result.json --target-csv-file-path ./mining_results/specific_commit_handled_rules_input_file_aggregated_rule_violations.csv --rule-violations-mode all
python3 ./../prepare_experiment_input_file.py ./mining_results/specific_commit_handled_rules_mining_result.json --target-csv-file-path ./mining_results/specific_commit_handled_rules_input_file_single_rule_violations.csv --rule-violations-mode single
echo Mining end: Specific commit, handled rules

# Use the commit that was used for Soralds experiments.
# Consider all rules supported by SonarQube.
echo Mining start: Specific commit, all rules
rm ./mining_results/specific_commit_all_rules_out.txt
java -jar $SORALD_JAR_PATH mine --git-repos-list ./sampled_repos_specific_commit.txt --miner-output-file ./mining_results/specific_commit_all_rules_out.txt \
    --stats-output-file ./mining_results/specific_commit_all_rules_mining_result.json --temp-dir ./temp --stats-on-git-repos --rule-parameters ./../../sonarqube_quality_profile/quality_profile_rule_parameters.json

python3 ./../prepare_experiment_input_file.py ./mining_results/specific_commit_all_rules_mining_result.json --target-csv-file-path ./mining_results/specific_commit_all_rules_input_file_aggregated_rule_violations.csv --rule-violations-mode all
python3 ./../prepare_experiment_input_file.py ./mining_results/specific_commit_all_rules_mining_result.json --target-csv-file-path ./mining_results/specific_commit_all_rules_input_file_single_rule_violations.csv --rule-violations-mode single
echo Mining end: Specific commit, all rules

# Use the commit that was used for Soralds experiments.
# Consider rules part of the default SonarQube quality profile.
echo Mining start: Specific commit, quality profile rules
rm ./mining_results/specific_commit_all_rules_out.txt
java -jar $SORALD_JAR_PATH mine --git-repos-list ./sampled_repos_specific_commit.txt --miner-output-file ./mining_results/specific_commit_quality_profile_out.txt \
    --stats-output-file ./mining_results/specific_commit_quality_profile_rules_mining_result.json --temp-dir ./temp --stats-on-git-repos --rule-keys "$qualtiy_profile_rules" --rule-parameters ./../../sonarqube_quality_profile/quality_profile_rule_parameters.json

python3 ./../prepare_experiment_input_file.py ./mining_results/specific_commit_quality_profile_rules_mining_result.json --target-csv-file-path ./mining_results/specific_commit_quality_profile_rules_input_file_aggregated_rule_violations.csv --rule-violations-mode all
python3 ./../prepare_experiment_input_file.py ./mining_results/specific_commit_quality_profile_rules_mining_result.json --target-csv-file-path ./mining_results/specific_commit_quality_profile_rules_input_file_single_rule_violations.csv --rule-violations-mode single
echo Mining end: Specific commit, quality profile rules

# Use the newest commit for each repo.
# Only consider rules handled by Sorald.
echo Mining start: Newest commit, handled rules
rm ./mining_results/newest_commit_handled_rules_out.txt
java -jar $SORALD_JAR_PATH mine --git-repos-list ./sampled_repos.txt --miner-output-file ./mining_results/newest_commit_handled_rules_out.txt \
    --stats-output-file ./mining_results/newest_commit_handled_rules_mining_result.json --temp-dir ./temp --stats-on-git-repos --rule-parameters ./../../sonarqube_quality_profile/quality_profile_rule_parameters.json --handled-rules

python3 ./../prepare_experiment_input_file.py ./mining_results/newest_commit_handled_rules_mining_result.json --target-csv-file-path ./mining_results/newest_commit_handled_rules_input_file_aggregated_rule_violations.csv --rule-violations-mode all
python3 ./../prepare_experiment_input_file.py ./mining_results/newest_commit_handled_rules_mining_result.json --target-csv-file-path ./mining_results/newest_commit_handled_rules_input_file_single_rule_violations.csv --rule-violations-mode single
echo Mining end: Newest commit, handled rules

# Use the newest commit for each repo.
# Consider all rules supported by SonarQube.
echo Mining start: Newest commit, all rules
rm ./mining_results/newest_commit_all_rules_out.txt
java -jar $SORALD_JAR_PATH mine --git-repos-list ./sampled_repos.txt --miner-output-file ./mining_results/newest_commit_all_rules_out.txt \
    --stats-output-file ./mining_results/newest_commit_all_rules_mining_result.json --temp-dir ./temp --stats-on-git-repos --rule-parameters ./../../sonarqube_quality_profile/quality_profile_rule_parameters.json

python3 ./../prepare_experiment_input_file.py ./mining_results/newest_commit_all_rules_mining_result.json --target-csv-file-path ./mining_results/newest_commit_all_rules_input_file_aggregated_rule_violations.csv --rule-violations-mode all
python3 ./../prepare_experiment_input_file.py ./mining_results/newest_commit_all_rules_mining_result.json --target-csv-file-path ./mining_results/newest_commit_all_rules_input_file_single_rule_violations.csv --rule-violations-mode single
echo Mining end: Newest commit, all rules

# Use the newest commit for each repo.
# Consider rules part of the default SonarQube quality profile.
echo Mining start: Newest commit, quality profile rules
rm ./mining_results/newest_commit_all_rules_out.txt
java -jar $SORALD_JAR_PATH mine --git-repos-list ./sampled_repos.txt --miner-output-file ./mining_results/newest_commit_quality_profile_out.txt \
    --stats-output-file ./mining_results/newest_commit_quality_profile_rules_mining_result.json --temp-dir ./temp --stats-on-git-repos --rule-keys "$qualtiy_profile_rules" --rule-parameters ./../../sonarqube_quality_profile/quality_profile_rule_parameters.json

python3 ./../prepare_experiment_input_file.py ./mining_results/newest_commit_quality_profile_rules_mining_result.json --target-csv-file-path ./mining_results/newest_commit_quality_profile_rules_input_file_aggregated_rule_violations.csv --rule-violations-mode all
python3 ./../prepare_experiment_input_file.py ./mining_results/newest_commit_quality_profile_rules_mining_result.json --target-csv-file-path ./mining_results/newest_commit_quality_profile_rules_input_file_single_rule_violations.csv --rule-violations-mode single
echo Mining end: Newest commit, quality profile rules
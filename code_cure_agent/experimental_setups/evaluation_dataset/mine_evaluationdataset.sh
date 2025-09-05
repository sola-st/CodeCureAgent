#!/bin/bash

# Mine the evaluation dataset.
# From the mining result create the experiment input file.

cd "$(dirname "$0")"

SORALD_JAR_PATH="/workspaces/xxxx/code_cure_agent/sorald/sorald.jar"

quality_profile_rules=$(head -n 1 ./../../sonarqube_quality_profile/quality_profile_rule_keys.txt)

# Make the ./temp dir if it does not exist.
mkdir -p ./temp

# Use the commit that was used for Soralds experiments.
# Consider rules part of the default SonarQube quality profile.
echo Mining start: Evaluation dataset, specific commit, quality profile rules
rm ./mining_results/evaluation_dataset_out.txt
java -jar $SORALD_JAR_PATH mine --git-repos-list ./evaluation_dataset_repos_list_with_java_versions.csv --miner-output-file ./mining_results/evaluation_dataset_out.txt \
    --stats-output-file ./mining_results/evaluation_dataset_mining_result.json --temp-dir ./temp --stats-on-git-repos --rule-keys "$quality_profile_rules" --rule-parameters ./../../sonarqube_quality_profile/quality_profile_rule_parameters.json

python3 ./../prepare_experiment_input_file.py ./mining_results/evaluation_dataset_mining_result.json --target-csv-file-path ./mining_results/evaluation_dataset_input_file_all_violations.csv --rule-violations-mode single
echo Mining end: Evaluation dataset, specific commit, quality profile rules

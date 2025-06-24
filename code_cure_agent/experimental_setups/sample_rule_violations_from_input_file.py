import click
import numpy as np
import pandas as pd
import csv

# Script for uniformly sampling rule violations from a previously created input file.
# Samples 'samples_per_rule' many samples from each of the rules that occur in the input file (at most max_number_of_considered_rules rules).
# Renumbers the sampled instances


@click.command()
@click.argument(
    "csv-input-file",
    type=click.File()
)
@click.option(
    "--target-csv-file-path",
    "-t",
    default="./new_sampled_experiment_input_file.csv",
    help="Path where the sampled csv file should be written to."
)
@click.option(
    "--samples-per-rule",
    type=int,
    default=2,
    help="Number of samples that are sampled per distinct rule."
)
@click.option(
    "--max-number-of-considered-rules",
    type=int,
    default=-1,
    help="Number of rules that are at most considered. If not specified, then all rules with at least samples_per_rule samples are considered."
)
def sample_rule_violations_from_input_file(csv_input_file: click.File, target_csv_file_path: str, samples_per_rule: int, max_number_of_considered_rules: int):

    number_generator = np.random.Generator(np.random.PCG64(seed=42))

    input_file_df = pd.read_csv(csv_input_file)

    number_of_different_rules = input_file_df.groupby("ruleKey").ngroups

    # filter rules with enough violation instances
    df_only_rules_with_sufficient_instances = input_file_df.groupby("ruleKey").filter(lambda x: len(x) >=
                                                                                      samples_per_rule)
    number_of_rules_with_at_least_samples_per_rule_instances = df_only_rules_with_sufficient_instances.groupby(
        "ruleKey").ngroups
    print(f"Number of samples in the csv file: {input_file_df.shape[0]}")
    print(
        f"Number of distinct rules in the csv file: {str(number_of_different_rules)}")
    print(
        f"Number of rules with at least {str(samples_per_rule)} occurences: {str(number_of_rules_with_at_least_samples_per_rule_instances)}")

    # Randomly select max_number_of_considered_rules many rules
    if max_number_of_considered_rules != -1 and max_number_of_considered_rules < number_of_rules_with_at_least_samples_per_rule_instances:
        unique_rule_keys = df_only_rules_with_sufficient_instances["ruleKey"].unique(
        )
        number_generator.shuffle(unique_rule_keys)
        selected_rule_keys = unique_rule_keys[:max_number_of_considered_rules]
        df_only_rules_with_sufficient_instances = df_only_rules_with_sufficient_instances[
            df_only_rules_with_sufficient_instances["ruleKey"].isin(
                selected_rule_keys)
        ]

    # Sample samples_per_rule many rules per sampled rule
    df_sampled_rule_instances = (
        df_only_rules_with_sufficient_instances.groupby("ruleKey")
        .sample(n=samples_per_rule, random_state=number_generator)
        .reset_index(drop=True)
    )

    # Change the istanceID to consecutive numbers
    df_sampled_rule_instances['instanceID'] = range(
        1, len(df_sampled_rule_instances) + 1)

    # output to csv
    df_sampled_rule_instances.to_csv(
        target_csv_file_path, encoding="utf-8", index=False, header=True, quoting=csv.QUOTE_ALL, quotechar='"',
        doublequote=True)
    print(f"Sampled data saved to {target_csv_file_path}")


if __name__ == "__main__":
    sample_rule_violations_from_input_file()

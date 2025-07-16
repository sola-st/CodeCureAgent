

import click
import numpy as np
import pandas as pd
import csv


@click.command()
@click.argument(
    "csv-input-evaluation-dataset-file",
    type=click.File()
)
@click.option(
    "--target-csv-file-path",
    "-t",
    default="./comparative_study/sorald_comparison/comparison_dataset_sorald.csv",
    help="Path where thecsv file with covered instances by sorald should be written to."
)
def extract_instances_covered_by_sorald(csv_input_evaluation_dataset_file: click.File, target_csv_file_path: str):

    sorald_supported_rules = ["S1068", "S1118", "S1132", "S1155", "S1217", "S1444", "S14810", "S1596", "S1656", "S1854", "S1860", "S1948", "S2057", "S2095",
                              "S2097", "S2111", "S2116", "S2142", "S2164", "S2167", "S2184", "S2204", "S2225", "S2272", "S2755", "S3032", "S3067", "S3984", "S4065", "S4973"]

    input_evaluation_file_df = pd.read_csv(csv_input_evaluation_dataset_file)

    covered_evaluation_file_df = input_evaluation_file_df[
        input_evaluation_file_df["ruleKey"].isin(sorald_supported_rules)]

    covered_evaluation_file_df.to_csv(
        target_csv_file_path, encoding="utf-8", index=False, header=True, quoting=csv.QUOTE_ALL, quotechar='"',
        doublequote=True)

    input_instances_count = input_evaluation_file_df["instanceID"].count()
    covered_instances_count = covered_evaluation_file_df["instanceID"].count()
    print(
        f"Covered instances: {covered_instances_count} / {input_instances_count}")


if __name__ == "__main__":
    extract_instances_covered_by_sorald()

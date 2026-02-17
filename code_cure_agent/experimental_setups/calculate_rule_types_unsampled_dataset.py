import os
import re
import click
import mdutils
import pandas as pd
import csv

import sys
from pathlib import Path

sys.path.append(str(Path(__file__).parent.parent))
from agent_core.llm.providers.openai import OPEN_AI_CHAT_MODELS


@click.command()
@click.argument(
    "evaluation_dataset_file",
    type=click.File()
)
@click.option(
    "--target-md-file-path",
    "-t",
    default="./evaluation_results/whole_95k_dataset_warning_types.md",
    help="Path where the stats file should be written."
)
def calculate_rule_types_unsampled_dataset(evaluation_dataset_file: click.File, target_md_file_path: str):

    evaluation_dataset_df = pd.read_csv(evaluation_dataset_file)

    mdFile = mdutils.MdUtils(
        file_name=target_md_file_path, title='Whole 95k dataset warning types', title_header_style="atx")
    mdFile.new_header(level=3, title="Total rule violations",
                      add_table_of_contents="n")

    total_rule_violations = evaluation_dataset_df["instanceID"].count()

    mdFile.new_line(
        str(total_rule_violations))

    # Warning type section
    mdFile.new_header(level=3, title="Warning types",
                      add_table_of_contents="n")
    warning_type_counts = evaluation_dataset_df["ruleType"].value_counts()
    for warning_type, count in warning_type_counts.items():
        percent = 100 * count / total_rule_violations if total_rule_violations else 0
        mdFile.new_line(f"{warning_type}: {count} ({percent:.3f}%)  ")
    mdFile.new_line()


    mdFile.create_md_file()

if __name__ == "__main__":
    calculate_rule_types_unsampled_dataset()
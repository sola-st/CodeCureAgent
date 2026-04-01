# Sorald comparison to CodeCureAgent

## Replicating the Sorald comparison

Run all scripts from this folder.

1. (Optional) The script [extract_instances_covered_by_sorald.py](extract_instances_covered_by_sorald.py) can be used to create a csv file that only contains warnings of rules that are part of the 30 currently supported rules by Sorald.  
    Pass as the first argument the csv input file of 1000 warnings: [../../code_cure_agent/experimental_setups/evaluation_dataset/evaluation_dataset_filled_up_to_1000_input_file.csv](../../code_cure_agent/experimental_setups/evaluation_dataset/evaluation_dataset_filled_up_to_1000_input_file.csv).

2. (Optional) Run the script [add_violation_specifier_to_sorald_input_file.py](add_violation_specifier_to_sorald_input_file.py) to create an augmented version of the csv file that contains the violation_specifier needed as input to Sorald.  
    Adapt the constants in the file as needed.

3. Run Sorald on the 62 warnings supported by Sorald and automatically check the results with the three oracle steps of CodeCureAgent and save the results to a csv file via the script [run_sorald_comparison.py](run_sorald_comparison.py).  
    Logs of the Sorald runs are saved to [sorald_run_outputs](sorald_run_outputs). Empty the sub-folders inside this folder first if you want to do a fresh run.  
    Adds the results to the csv file [sorald_comparison_results.csv](sorald_comparison_results.csv). Delete or rename the file first for a fresh run.

4. Summarize stats from the created results csv file via [sorald_comparison_calculate_stats_from_evaluation_results.py](sorald_comparison_calculate_stats_from_evaluation_results.py) by passing the [sorald_comparison_results.csv](sorald_comparison_results.csv) file as argument.


## Relevant Comparison Result Files

- The Sorald comparison result csv file [sorald_comparison_results.csv](sorald_comparison_results.csv). This contains relevant stats for each warning extracted from the Sorald run logs. The file contains stats from both the CodeCureAgent run on the warnings and the Sorald run on the warnings.
- Aggregated comparison results showing both stats for CodeCureAgent and Sorald. Files [analysis_results_overview_all_samples.md](analysis_results_overview_all_samples.md) and [analysis_results_overview_first_21_samples_(manually_inspected).md](analysis_results_overview_first_21_samples_(manually_inspected).md).
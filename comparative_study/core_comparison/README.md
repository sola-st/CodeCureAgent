# CORE comparison to CodeCureAgent

## Replicating the CORE comparison


1. (Optional) Create the CORE comparison dataset with [create_core_comparison_dataset.py](create_core_comparison_dataset.py) (requires the CodeCureAgent python environment (via the `requirments.txt` in the `code_cure_agent` folder))  

2. Run CORE on the dataset using the script [COREMSRI/scripts/run_CORE_pipeline.sh](COREMSRI/scripts/run_CORE_pipeline.sh) (this requires the python environment of CORE (the `requirements.txt` in `COREMSRI`))  
    Run this script from [COREMSRI](COREMSRI).  
    This runs all stages of CORE including the stage 4 that we needed to implement to map from the prompter to the ranker stages for SonarQube.  
    Logs are saved to [COREMSRI/comparison_output/cca_dataset_results](COREMSRI/comparison_output/cca_dataset_results).  
3. Evaluate the results (apply the three CodeCureAgent oracle steps to all created fixes) using [evaluate_core_run_results.py](evaluate_core_run_results.py) (requires the CodeCureAgent environment (the requirments.txt in the code_cure_agent folder)).  
    Results are appended to file [core_comparison_results.csv](core_comparison_results.csv) per default. Delete or rename it first if you want to write a new file.
4. Summarize stats from the created results csv file via [calculate_stats_from_evaluation_results.py](calculate_stats_from_evaluation_results.py) by passing the [core_comparison_results.csv](core_comparison_results.csv) file as argument.  
    Run this script from this folder.

## Relevant Comparison Result Files

- The CORE comparison result csv files [core_comparison_results.csv](core_comparison_results.csv) and [core_comparison_results_21_manually_inspected_warnings.csv](core_comparison_results_21_manually_inspected_warnings.csv). These contain relevant stats for each warning extracted from the CORE run logs. They contain stats from both the CodeCureAgent run on the warnings and the CORE run on the warnings.
- Aggregated comparison results showing both stats for CodeCureAgent and CORE. Files [analysis_results_overview_all_1000.md](analysis_results_overview_all_1000.md), [analysis_results_overview_291.md](analysis_results_overview_291.md) and [analysis_results_overview_21_manually_inspected_warnings.md](analysis_results_overview_21_manually_inspected_warnings.md).
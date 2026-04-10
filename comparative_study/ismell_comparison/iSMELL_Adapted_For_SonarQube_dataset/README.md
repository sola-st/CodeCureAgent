# iSMELL comparison to CodeCureAgent

## Replicating the iSMELL comparison


1. (Optional) Create the iSMELL comparison dataset with [evaluation_scripts/create_ismell_comparison_dataset.py](evaluation_scripts/create_ismell_comparison_dataset.py) (requires the CodeCureAgent python environment (via the `requirements.txt` in the `code_cure_agent` folder)). Run it from here.  
2. Add a file `.env` to this folder with the following content:  

    ```env
    OPENAI_API_KEY=your_api_key_here
    ```  

    Replace `your_api_key_here` with your actual OpenAI API key.
3. Run iSMELL on the dataset using the script [refactor_warnings.py](refactor_warnings.py).
    This requires the python environment of iSMELL (the `requirements.txt` in this folder). Uninstall the CodeCureAgent python environment first, then install the iSMELL environment. Ignore the pip error when installing.  
    Run this script from here.  
    Logs are saved to the dataset folder `cca_dataset`.  
4. Evaluate the results (apply the three CodeCureAgent oracle steps to all created fixes) using [evaluation_scripts/evaluate_ismell_run_results.py](evaluation_scripts/evaluate_ismell_run_results.py) (requires the CodeCureAgent environment (the requirements.txt in the code_cure_agent folder)). Run it from here.  
    Results are appended to file [ismell_comparison_results.csv](ismell_comparison_results.csv) per default. Delete or rename it first if you want to write a new file.
5. Summarize stats from the created results csv file via [evaluation_scripts/calculate_ismell_stats_from_evaluation_results.py](evaluation_scripts/calculate_ismell_stats_from_evaluation_results.py) by passing the [ismell_comparison_results.csv](ismell_comparison_results.csv) file as argument.  
    Run this script from this folder.

## Relevant Comparison Result Files

- The iSMELL comparison result csv files [ismell_comparison_results.csv](ismell_comparison_results.csv) and [ismell_manual_inspection_21.csv](ismell_manual_inspection_21.csv). These contain relevant stats for each warning extracted from the iSMELL run logs. They contain stats from both the CodeCureAgent run on the warnings and the iSMELL run on the warnings.
- Aggregated comparison results showing both stats for CodeCureAgent and iSMELL. Files [analysis_results_overview_all_1000_ismell.md](analysis_results_overview_all_1000_ismell.md), [analysis_results_overview_291_ismell.md](analysis_results_overview_291_ismell.md) and [analysis_results_overview_21_manually_inspected_warnings_ismell.md](analysis_results_overview_21_manually_inspected_warnings_ismell.md).
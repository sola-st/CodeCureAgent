

# Running the CORE comparison to CodeCureAgent

1. Create the CORE comparison dataset with create_core_comparison_dataset.py (requires the CodeCureAgent environment (the requirments.txt in the code_cure_agent folder))  
2. Run CORE on it (use run_CORE_pipeline.sh) (this requires the environment of CORE (the requirements.txt))
3. Automatically runs the stage 4 that we implemented to map from the prompter to the ranker stages
4. Evaluate the results using evaluate_core_run_results.py (requires the CodeCureAgent environment (the requirments.txt in the code_cure_agent folder))
5. Summarize stats from the created results csv file via calculate_stats_from_evaluation_results.py
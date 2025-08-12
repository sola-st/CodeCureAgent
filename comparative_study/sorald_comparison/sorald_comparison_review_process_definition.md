# Definition of Sorald Comparison Review Process

1. Extract the instances covered by Sorald (both from the input file, as well as from the evaluation results file) (via [extract_instances_covered_by_sorald.py](extract_instances_covered_by_sorald.py))
2. Run script [add_violation_specifier_to_sorald_input_file.py](add_violation_specifier_to_sorald_input_file.py) to extend the input file with the violation specifiers Sorald uses
3. Run Sorald on the rule violations (via [run_sorald_comparison.py](run_sorald_comparison.py))
4. Output file sorald_comparison_results.csv is created, relevant logs and diffs are added in the folder [sorald_run_outputs](./sorald_run_outputs).
5. To the file, add the following rows: "soraldTPAssumptionSoundness","soraldFixCorrectness","codeSmellOutsideOfSonarQubeIntroduced","soraldTPAssumptionSoundnessExplanation","soraldFixCorrectnessExplanation"
6. Manual inspection:
   1. Check soundness of treating the warning as a TP  
      - If it makes sense to fix it => soraldTPAssumptionSoundness set to "Sound"
      - If there is no way of meaningully fixing it as a TP (and Sorald couldn't create a meaningful fix) => soraldTPAssumptionSoundness set to "Not sound"
      - Add explanation to soraldTPAssumptionSoundnessExplanation
   2. Check correctness of the fix (If TP assumption was not sound then the fix must be non-correct)
      1. Firstly the fix is not correct if the build failed, test failed or if the warning wasn't removed or new warnings were introduced (automatically checked and added to the results file) 
      (fixing warnings by introducing other warnings is not correct (for CodeCureAgent this is checked as part of the agent))
      2. If this is not the case: Inspect the diffs of all files that had changes
      3. Check for all changes:
         1. Is the changed version semantically equivalent to the unchanged version (no change in behavior)? If yes for all changes => Correct
         2. If there are semantic differences:
            1. Do they change the externally observable behavior in a way that violates the expected functionality of the code? If yes => Not correct; If no => Correct
            2. Are the semantic differences due to the changes fixing some wrong behavior of the code (a bug where the code didn't have the expected functionality before)? If yes => Correct
      4. Based on the previous steps, make a final decision of "Correct" or "Not correct", add to soraldFixCorrectness and an explanation to soraldFixCorrectnessExplanation
   3. Check if the fix introduced any code smells (duplicate statements etc.) that were not recognized by SonarQube yet => add to codeSmellOutsideOfSonarQubeIntroduced


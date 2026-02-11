# Definition of Review Process

1. (Run CodeCureAgent on the rule violations ([run_on_dataset.sh](../run_on_dataset.sh)))
2. (Automatically extract relevant information from the run logs into a csv file (via [experimental_setups/write_experiment_results_to_csv_file.py](write_experiment_results_to_csv_file.py))
    - Info about the sample: ID, projectName, ruleKey, ruleName, ruleType, experimentNumber
    - Classification (by the agent)
    - Plausible fix created (passed the ChangeApprover steps)
    - Fix Complexity (Single Line, Multi Line, Multi File fix))
3. (Automatically copy, reapply fix and open all relevant files for the next uninspected sample in the csv file (via [experimental_setups/show_next_warning_for_manual_inspection.py](show_next_warning_for_manual_inspection.py)))
4. Check Classification Soundness: For the sample, decide if the classification by the agent is sound by following these steps:
   1. Read the description of the violated rule (see `sonar_rule_x_docu.md`) to understand what it is about.
   2. Check the specific details of the rule violation raised (specific message, the file and line where it occurs etc. in file IDx_summary.diff or IDx_task_info.yaml).
   3. Inspect the code file and the relevant lines where the rule was raised to understand the specific rule violation and its context.
   4. Read the IDx_classification_result file that contains the agent's answers to the questions and the final verdict with reasoning to understand the agent's rationale for its decision.
   5. If some further background knowledge is needed, look up relevant info on the web.
   6. Finally, decide whether the agent's reasoning for the different questions, and based on the reasoning the final classification, is "Sound" or "Not sound". Do this based on the gathered information about the code and the rule violation.
   7. (Add the decision to the csv file created before (in column classificationSoundness either add "Sound" or "Not sound". You can add an explanation in column classificationSoundnessExplanation if the instance is a complicated case that needs explanation).)

5. Check Correctness of Fix (Only if the classification was sound): For the sample, decide if the created fix by the agent is correct by following these steps:
   1. If it is a TP:
      1. Inspect the diffs of all files that had changes
      2. Check for all changes:
         1. Is the changed version semantically equivalent to the unchanged version (no change in behavior)? If yes for all changes => Correct
         2. If there are semantic differences:
            1. Do they change the externally observable behavior in a way that violates the expected functionality of the code? If yes => Not correct; If no => Correct
            2. Are the semantic differences due to the changes fixing some wrong behavior of the code (a bug where the code didn't have the expected functionality before)? If yes => Correct
      3. Optional: Build the project with changes, run tests and check if the rule violation is removed (already checked by the ChangeApprover)
      4. Based on the previous steps, make a final decision of "Correct" or "Not correct"
   2. If it is a FP:
      1. Inspect the diff of the file that had changes
      2. Check:
         1. Is a suppression inserted at the correct line, without removing any code?
      3. Optional: Check if the rule violation is removed (already checked by the ChangeApprover)
      4. Based on the previous steps, make a final decision of "Correct" or "Not correct"
   3. (Add the decision to the csv file created before (in column fixCorrectness either add "Correct" or "Not correct". You can add an explanation in column fixCorrectnessExplanation if the instance is a complicated case that needs explanation).)

For more detailed instructions on running the scripts, see [README](../../../README.md).

## What makes a classification "Sound"?

The agent for classification has three questions that it has to answer. Based on these questions, it then gives a final verdict.  
The agent can decide for itself how to weight the different questions in its final decision of TP or FP. Depending on the specific rule violation, some questions might be more important than others (e.g., whether the developer intentionally violated the rule (Q2) might be more important for CodeSmells than for other types of rules).  

This classification is then sound if the following holds:

- A developer might come to the same conclusion (deciding that the problem is to be fixed or not to be fixed, or isn't a problem at all)
- The agent's reasoning about the classification aligns with the reviewer's interpretation of the code and the rule violation
- The final verdict (TP or FP) aligns with this interpretation of the reviewer.

Special cases:

- If a CodeSmell is systematically violated in a file/project out of a meaningful style consideration, then classifying it as FP makes sense.
- If there is a way of fixing the rule violation without breaking relevant functionality or design choices, even if the rule was intentionally violated by the developer, then fixing it is sound. But not fixing it might still be sound too, if the developer violated the rule for a good reason.
- A rule violation might require making a breaking change about the projects public API. Such a violation is still sound and can correctly be fixed, eventhough it might introduce breaking changes for downstream consumers of the software. However, when considering to apply changes in a pull request one should reconsider if such breaking changes are wanted and if so create a new version of the software and document the API change properly. Then this would be a new version of the software that consumers need to migrate to if they want to upgrade.

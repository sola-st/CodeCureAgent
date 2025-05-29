# Stats first run with the 15 selected rule violations from dev_dataset

## Overall stats

### Time

Execution time: (2025-05-23 10:48:52 - 2025-05-23 11:40:29)  + (2025-05-23 13:23:01 - 2025-05-23 13:27:49)  
=> ca. 56 min  
56 min / 15 = 3.73 min per rule violation (3 minutes and 45 seconds)  

### Cost (very approximate)

ca. 0.85 dollar with GPT 4.1 mini  
0.85 / 15 = 0.056 -> 5 cent per violation  
less then 372 requests  
less than 4.336 million input tokens  
less than 104.000 output tokens  

### Patches

Created any patch: 15/15  
Created implausible patch: 8/15  
Created plausible patch: 13/15  

### Cycle counts

Plausible patch generated:  
8 (ID 8)  
40 (ID 28) (suppressed it in the second to last cycle)  
6 (ID 179)  
9 (ID 198)  
5 (ID 268)  
6 (ID 362)  
21 (ID 404)  
9 (ID 406)  
18 (ID 449)  
7 (ID 542)  
18 (ID 817)  
22 (ID 1470) Did 20 cycles of reading lines in a very unstructured way, before finally doing a write_fix (read similar lines multiple times)  
29 (ID 2030)  

Average: 15.2 cycles  

No plausible patch:  
40 (ID 10) Also lots of time wasted with read_lines. Many of them are the same. Check this one in detail, why the write_fix didn't work  
40 (ID 205)  

Average: 40 cycles  

### Proposed fixes

With plausible patch found:  
1  (ID 8)  
11 (ID 28)  
1  (ID 179)  
3  (ID 198)  
1  (ID 268)  
1  (ID 362)  
7  (ID 404)  
1  (ID 406)  
6  (ID 449)  
1  (ID 542)  
2  (ID 817)  
1  (ID 1470)  
14 (ID 2030)  

Average 3.8 write_fix attempts needed  

Without plausible patch found:  
8 (ID 10)  
5 (ID 205)  

Average 6.5 write_fix attempts made  

## Errors  

Sample with ID 449 did not start. AutoGPT wasn't created.  
Problem: Input item had a hyphen '. This wrongly terminated a string in ai_settings.yaml  
Fixed  

apply_changes failed The path couldn't be processed with error: The file_path src/main/java/org/junit/internal/runners/model/InternalMultipleFailureException.java does not exist.  
Problem: Agent tried to create a new file, that didn't exist before (it tried to rename the file actually). Later it pivoted from this plan as it didn't work  

literal_eval error could not parse  
GPT repeats the same pattern multiple times before it breaks of.  
Problem: This is LLM repetition degeneration. So a problem on the LLM side.  
Aliviation: Better prompt design, with less repetitions can help (maybe giving the history as normal separate messages instead of adding it to the main prompt)  
For now: Track how regularly it occurs  

## Correct fixes?

ID 8:  
Correct fix (I think), but some unnecessary imports.  

ID 10:  
No plausible fix, due to problems using modifications correctly.  

ID 28:  
"Correct", it suppressed the warning which is correct as it is a False Positive. However, it did do that because it messed up creating a proper fix before and then finally fell back to that solution.  

ID 179:  
Incorrect. Evaluating the string first leads to NullPointerException when running tests. 

ID 198:  
Incorrect: Should have been a False Positive, as it is a test. The valueOf() method is needed internally when calling at.convert(). This changes the semantics and doesn't test the expected thing anymore.  

ID 205:  
No plausible fix. Doesn't understand what it did wrong. The added feedback should help with this in the future.  

ID 268:  
Correct fix (very easy)  

ID 362:  
Incorrect: Returning an empty byte array instead of null would need changes in 2 other files (3 references total). The agent doesn't have the idea, that checking that might be necessary, but it also has no tool yet that could help with that.
Therefore, currently it just changes the one line which is incorrect.  

ID 404:  
Correct fix  

ID 406:  
Correct fix  

ID 449:  
Incorrect fix: This is a FP. The fix doesn't test what it should anymore. (Arithmetic Exception not escalated anymore). The FP is not recognized, but this could be due to supressing warnings being strongly discouraged in the prompt.  

ID 542:  
Incorrect fix: Didn't recognize that it is a FP. But treating as FP is discouraged by the prompt.  

ID 817:  
Correct fix: Suppressed the FP (but only due to struggling with transitivly arising rule violations)  

ID 1470:  
Correct fix: Nicely solves the problem without any semantic changes  

ID 2030:  
Correct fix: Suppressing it is the best choice, because it is a deprecated class that should still be usable for compatibility. The agent first tried to rename the file which was not possible.  

## Summary of correctness

Total correct fixes: 8/15  
Non-suppressing correct fixes: 5/15  
## Test case coverage

ID 8:  
Covered (because it is code in a test case itself)  
Passes

ID 10:  
Covered  
No fix given

ID 28:  
Not directly covered (class is tested but not the method)

ID 179:
Covered  
Doesn't pass. Proposed fix had a NullpointerException.

ID 198:
Covered (because it is code in a test case itself)  
Passes (The test doesn't recognized the incorrect change)

ID 205:  
Covered  
No fix given

ID 268:  
Covered  
Passes

ID 362:  
Not covered  
=> Passes (eventhough incorrect)

ID 404:  
Not covered  
=> Passes

ID 406:  
Not covered  
=> Passes

ID 449:  
Not covered (It is code in a test case but the test case is not added to the test suite)  

ID 542:  
Covered (because it is code in a test case itself)  
Passes (eventhough incorrect (should be FP))

ID 817:  
Covered  
Passes  

ID 1470:
Not covered
=> Passes  

ID 2030:  
Covered
Passes (Suppressed)

### Test summary

Covered: 9/15  
Problem exposing: 1/3 (of the incorrect fixes that are covered)  
Correctness asserting (given that covered and the fix is correct => does the test pass): 4

## Improvement vectors

- Better feedback: Show to the agent how its failing write_fix attempt looked like
- SonarQube docu tool: See examples on how to resolve a warning
- Reference lookup: Find dependent code that needs to change
- Separate subtask of TP/FP classification: Agent decides itself whether the violation is a problem

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
1  
11  
1  
3  
1  
1  
7  
1  
6  
1  
2  
1  
14  

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
Problem: maybe: when single hyphens are used in the agent's answer the literal_eval fails. But there are other cases where it doesn't.  

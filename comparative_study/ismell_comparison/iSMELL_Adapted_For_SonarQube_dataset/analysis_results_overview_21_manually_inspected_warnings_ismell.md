
# iSMELL Comparison Analysis Results

## Total warnings in comparison

21
## CodeCureAgent stats on the compared warnings

### CodeCureAgent Classification

TP: 12 (57.14%)  
FP: 9 (42.86%)  
Unclassified: 0 (0.00%)
### CodeCureAgent Plausible Fixes

Total plausible fixes: 21/21 (100.00%)  
TP plausible fixes: 12/12 (100.00%)  
FP plausible fixes: 9/9 (100.00%)  
### CodeCureAgent Soundness of classification

Total sound classifications: 20/21 (95.24%)  
Sound TP classifications: 12/12 (100.00%)  
Sound FP classifications: 8/9 (88.89%)  
Precision: 1.00  
Recall: 0.92  
F1 Score: 0.96  
### CodeCureAgent Correctness of fix

Total correct fixes (sound and correct / sound and fixed): 20/20 (100.00%)  
Correct TP fixes  (sound and correct / sound and fixed): 12/12 (100.00%)  
Correct FP fixes  (sound and correct / sound and fixed): 8/8 (100.00%)  
### CodeCureAgent End-to-end performance (fixed, sound and correct)

End-to-end total: 20/21 (95.24%)  
End-to-end TP: 12/12 (100.00%)  
End-to-end FP: 8/9 (88.89%)  
### CodeCureAgent Fix Complexity

Single Line problems: 13 (61.90%)  
Multi Line problems: 8 (38.10%)  
Multi File problems: 0 (0.00%)  

Fix complexity split by type of fix:  
TP - Single Line: 4 (33.33%)  
TP - Multi Line: 8 (66.67%)  
TP - Multi File: 0 (0.00%)  
FP - Single Line: 9 (100.00%)  
FP - Multi Line: 0 (0.00%)  
FP - Multi File: 0 (0.00%)  

Number of plausible fixes created per fixComplexity:  
Single Line: 13 / 13 (100.00%)  
Multi Line: 8 / 8 (100.00%)  
Multi File: 0 / 0 (0.00%)  

Number of correct fixes created per fixComplexity (of the 20 sound and correct fixes (only for inspected samples)):  
Single Line: 12 (60.00%)  
Multi Line: 8 (40.00%)  
Multi File: 0 (0.00%)  
## iSMELL stats

### Performance Stats

iSMELL fix created: 21/21 (100.00%)  
iSMELL build successful: 20/21 (95.24%)  
iSMELL warning removed: 18/21 (85.71%)  
iSMELL no new warning introduced: 19/21 (90.48%)  
iSMELL test successful: 18/21 (85.71%)  

iSMELL build successful + warning removed: 17/21 (80.95%)  
iSMELL build successful + warning removed + no new warning: 15/21 (71.43%)  
iSMELL all checks (build + warning removed + no new warning + test): 14/21 (66.67%)  

Performance breakdown by classification:
iSMELL TP all checks: 9/12 (75.00%)  
iSMELL FP all checks: 5/9 (55.56%)  

iSMELL TP assumption sound (not in conjunction with if a fix was created): 17/20 (85.00%)  
iSMELL instances where TP assumption was not sound: 3 (15.00%)  

iSMELL fix correct (and fix created) (of instances where fix was created (and manually inspected)): 13/21 (61.90%)  
iSMELL fix correct (and fix created) (of all manually inspected warnings): 13/21 (61.90%)  
iSMELL fix correct (not only for instances where fix was created (no fix => incorrect fix) (of all manually inspected warnings)): 13/21 (61.90%)  

iSMELL further code smell introduced not reported by SonarQube (of all manually inspected warnings): 0/1 (0.00%)  
iSMELL further code smell introduced not reported by SonarQube (of all warnings where all other stats pass): 0/0 (0.00%)  
### Time Efficiency

iSMELL total time (instances with fixes): mean=52.4029s, median=42.5703s  
Number of instances with time data: 21/21  
### Token Consumption and Cost

**Total across all instances with token data (21 instances):**  
Total uncached input tokens: 72,822  
Total cached input tokens: 0  
Total output tokens: 62,473  
Total tokens: 135,295  
Total cost: $0.1291  

**Average per instance:**  
Average uncached input tokens: 3467.7  
Average cached input tokens: 0.0  
Average output tokens: 2974.9  
Average total tokens: 6442.6  
Average cost per instance: $0.0061  

**Cost efficiency:**  
Cost per successful fix (all checks passed): $0.0092  
Cost per fix created: $0.0061  
## Comparison Summary

**End-to-end performance comparison:**  
CodeCureAgent end-to-end: 20/21 (95.24%)  
iSMELL end-to-end 13/21 (61.90%)  
Performance gap (CCA - iSMELL): 33.33 percentage points  
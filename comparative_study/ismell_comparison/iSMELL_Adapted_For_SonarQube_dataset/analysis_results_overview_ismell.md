
# iSMELL Comparison Analysis Results

## Total warnings in comparison

50
## CodeCureAgent stats on the compared warnings

### CodeCureAgent Classification

TP: 35 (70.00%)  
FP: 15 (30.00%)  
Unclassified: 0 (0.00%)
### CodeCureAgent Plausible Fixes

Total plausible fixes: 49/50 (98.00%)  
TP plausible fixes: 34/35 (97.14%)  
FP plausible fixes: 15/15 (100.00%)  
### CodeCureAgent Soundness of classification

Total sound classifications: 45/50 (90.00%)  
Sound TP classifications: 33/35 (94.29%)  
Sound FP classifications: 12/15 (80.00%)  
Precision: 0.94  
Recall: 0.92  
F1 Score: 0.93  
### CodeCureAgent Correctness of fix

Total correct fixes (sound and correct / sound and fixed): 43/45 (95.56%)  
Correct TP fixes  (sound and correct / sound and fixed): 31/33 (93.94%)  
Correct FP fixes  (sound and correct / sound and fixed): 12/12 (100.00%)  
### CodeCureAgent End-to-end performance (fixed, sound and correct)

End-to-end total: 43/50 (86.00%)  
End-to-end TP: 31/35 (88.57%)  
End-to-end FP: 12/15 (80.00%)  
### CodeCureAgent Fix Complexity

Single Line problems: 29 (58.00%)  
Multi Line problems: 20 (40.00%)  
Multi File problems: 1 (2.00%)  

Fix complexity split by type of fix:  
TP - Single Line: 15 (42.86%)  
TP - Multi Line: 19 (54.29%)  
TP - Multi File: 1 (2.86%)  
FP - Single Line: 14 (93.33%)  
FP - Multi Line: 1 (6.67%)  
FP - Multi File: 0 (0.00%)  

Number of plausible fixes created per fixComplexity:  
Single Line: 28 / 29 (96.55%)  
Multi Line: 20 / 20 (100.00%)  
Multi File: 1 / 1 (100.00%)  

Number of correct fixes created per fixComplexity (of the 43 sound and correct fixes (only for inspected samples)):  
Single Line: 24 (55.81%)  
Multi Line: 18 (41.86%)  
Multi File: 1 (2.33%)  
## iSMELL stats

### Performance Stats

iSMELL fix created: 50/50 (100.00%)  
iSMELL build successful: 34/50 (68.00%)  
iSMELL warning removed: 46/50 (92.00%)  
iSMELL no new warning introduced: 40/50 (80.00%)  
iSMELL test successful: 32/50 (64.00%)  

iSMELL build successful + warning removed: 31/50 (62.00%)  
iSMELL build successful + warning removed + no new warning: 25/50 (50.00%)  
iSMELL all checks (build + warning removed + no new warning + test): 24/50 (48.00%)  

Performance breakdown by classification:
iSMELL TP all checks: 22/35 (62.86%)  
iSMELL FP all checks: 2/15 (13.33%)  

iSMELL TP assumption sound (not in conjunction with if a fix was created): 0/0 (0.00%)  
iSMELL instances where TP assumption was not sound: 0 (0.00%)  

iSMELL fix correct (and fix created) (of instances where fix was created (and manually inspected)): 0/0 (0.00%)  
iSMELL fix correct (and fix created) (of all manually inspected warnings): 0/0 (0.00%)  
iSMELL fix correct (not only for instances where fix was created (no fix => incorrect fix) (of all manually inspected warnings)): 0/0 (0.00%)  

iSMELL further code smell introduced not reported by SonarQube (of all manually inspected warnings): 0/0 (0.00%)  
iSMELL further code smell introduced not reported by SonarQube (of all warnings where all other stats pass): 0/0 (0.00%)  
### Time Efficiency

iSMELL total time (instances with fixes): mean=87.004s, median=23.3475s  
Number of instances with time data: 50/50  
### Token Consumption and Cost

**Total across all instances with token data (50 instances):**  
Total uncached input tokens: 375,809  
Total cached input tokens: 20,480  
Total output tokens: 158,103  
Total tokens: 554,392  
Total cost: $0.4053  

**Average per instance:**  
Average uncached input tokens: 7516.2  
Average cached input tokens: 409.6  
Average output tokens: 3162.1  
Average total tokens: 11087.8  
Average cost per instance: $0.0081  

**Cost efficiency:**  
Cost per successful fix (all checks passed): $0.0169  
Cost per fix created: $0.0081  
## Comparison Summary

**End-to-end performance comparison:**  
CodeCureAgent end-to-end: 43/50 (86.00%)  
iSMELL end-to-end 0/0 (0.00%)  
Performance gap (CCA - iSMELL): 86.00 percentage points  
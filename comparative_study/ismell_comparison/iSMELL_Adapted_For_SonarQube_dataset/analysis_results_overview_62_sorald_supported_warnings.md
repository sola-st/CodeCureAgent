
# iSMELL Comparison Analysis Results

## Total warnings in comparison

62
## CodeCureAgent stats on the compared warnings

### CodeCureAgent Classification

TP: 44 (70.97%)  
FP: 18 (29.03%)  
Unclassified: 0 (0.00%)
### CodeCureAgent Plausible Fixes

Total plausible fixes: 62/62 (100.00%)  
TP plausible fixes: 44/44 (100.00%)  
FP plausible fixes: 18/18 (100.00%)  
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

Single Line problems: 32 (51.61%)  
Multi Line problems: 30 (48.39%)  
Multi File problems: 0 (0.00%)  

Fix complexity split by type of fix:  
TP - Single Line: 14 (31.82%)  
TP - Multi Line: 30 (68.18%)  
TP - Multi File: 0 (0.00%)  
FP - Single Line: 18 (100.00%)  
FP - Multi Line: 0 (0.00%)  
FP - Multi File: 0 (0.00%)  

Number of plausible fixes created per fixComplexity:  
Single Line: 32 / 32 (100.00%)  
Multi Line: 30 / 30 (100.00%)  
Multi File: 0 / 0 (0.00%)  

Number of correct fixes created per fixComplexity (of the 20 sound and correct fixes (only for inspected samples)):  
Single Line: 12 (60.00%)  
Multi Line: 8 (40.00%)  
Multi File: 0 (0.00%)  
## iSMELL stats

### Performance Stats

iSMELL fix created: 62/62 (100.00%)  
iSMELL build successful: 58/62 (93.55%)  
iSMELL warning removed: 57/62 (91.94%)  
iSMELL no new warning introduced: 56/62 (90.32%)  
iSMELL test successful: 55/62 (88.71%)  

iSMELL build successful + warning removed: 53/62 (85.48%)  
iSMELL build successful + warning removed + no new warning: 50/62 (80.65%)  
iSMELL all checks (build + warning removed + no new warning + test): 48/62 (77.42%)  

Performance breakdown by classification:
iSMELL TP all checks: 36/44 (81.82%)  
iSMELL FP all checks: 12/18 (66.67%)  

iSMELL TP assumption sound (not in conjunction with if a fix was created): 0/0 (0.00%)  
iSMELL instances where TP assumption was not sound: 0 (0.00%)  

iSMELL fix correct (and fix created) (of instances where fix was created (and manually inspected)): 0/0 (0.00%)  
iSMELL fix correct (and fix created) (of all manually inspected warnings): 0/0 (0.00%)  
iSMELL fix correct (not only for instances where fix was created (no fix => incorrect fix) (of all manually inspected warnings)): 0/0 (0.00%)  

iSMELL further code smell introduced not reported by SonarQube (of all manually inspected warnings): 0/0 (0.00%)  
iSMELL further code smell introduced not reported by SonarQube (of all warnings where all other stats pass): 0/0 (0.00%)  
### Time Efficiency

iSMELL total time (instances with fixes): mean=53.3727s, median=28.2274s  
Number of instances with time data: 62/62  
### Token Consumption and Cost

**Total across all instances with token data (62 instances):**  
Total uncached input tokens: 564,414  
Total cached input tokens: 0  
Total output tokens: 219,758  
Total tokens: 784,172  
Total cost: $0.5774  

**Average per instance:**  
Average uncached input tokens: 9103.5  
Average cached input tokens: 0.0  
Average output tokens: 3544.5  
Average total tokens: 12647.9  
Average cost per instance: $0.0093  

**Cost efficiency:**  
Cost per successful fix (all checks passed): $0.0120  
Cost per fix created: $0.0093  
## Comparison Summary

**End-to-end performance comparison:**  
CodeCureAgent end-to-end: 20/21 (95.24%)  
iSMELL end-to-end 0/0 (0.00%)  
Performance gap (CCA - iSMELL): 95.24 percentage points  
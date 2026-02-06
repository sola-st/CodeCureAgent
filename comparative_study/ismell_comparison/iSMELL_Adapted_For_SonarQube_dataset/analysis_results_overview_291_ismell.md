
# iSMELL Comparison Analysis Results

## Total warnings in comparison

291
## CodeCureAgent stats on the compared warnings

### CodeCureAgent Classification

TP: 191 (65.64%)  
FP: 100 (34.36%)  
Unclassified: 0 (0.00%)
### CodeCureAgent Plausible Fixes

Total plausible fixes: 282/291 (96.91%)  
TP plausible fixes: 182/191 (95.29%)  
FP plausible fixes: 100/100 (100.00%)  
### CodeCureAgent Soundness of classification

Total sound classifications: 267/291 (91.75%)  
Sound TP classifications: 186/191 (97.38%)  
Sound FP classifications: 81/100 (81.00%)  
Precision: 0.97  
Recall: 0.91  
F1 Score: 0.94  
### CodeCureAgent Correctness of fix

Total correct fixes (sound and correct / sound and fixed): 251/259 (96.91%)  
Correct TP fixes  (sound and correct / sound and fixed): 170/178 (95.51%)  
Correct FP fixes  (sound and correct / sound and fixed): 81/81 (100.00%)  
### CodeCureAgent End-to-end performance (fixed, sound and correct)

End-to-end total: 251/291 (86.25%)  
End-to-end TP: 170/191 (89.01%)  
End-to-end FP: 81/100 (81.00%)  
### CodeCureAgent Fix Complexity

Single Line problems: 181 (62.20%)  
Multi Line problems: 101 (34.71%)  
Multi File problems: 9 (3.09%)  

Fix complexity split by type of fix:  
TP - Single Line: 86 (45.03%)  
TP - Multi Line: 96 (50.26%)  
TP - Multi File: 9 (4.71%)  
FP - Single Line: 95 (95.00%)  
FP - Multi Line: 5 (5.00%)  
FP - Multi File: 0 (0.00%)  

Number of plausible fixes created per fixComplexity:  
Single Line: 179 / 181 (98.90%)  
Multi Line: 98 / 101 (97.03%)  
Multi File: 5 / 9 (55.56%)  

Number of correct fixes created per fixComplexity (of the 251 sound and correct fixes (only for inspected samples)):  
Single Line: 154 (61.35%)  
Multi Line: 94 (37.45%)  
Multi File: 3 (1.20%)  
## iSMELL stats

### Performance Stats

iSMELL fix created: 291/291 (100.00%)  
iSMELL build successful: 241/291 (82.82%)  
iSMELL warning removed: 270/291 (92.78%)  
iSMELL no new warning introduced: 247/291 (84.88%)  
iSMELL test successful: 217/291 (74.57%)  

iSMELL build successful + warning removed: 221/291 (75.95%)  
iSMELL build successful + warning removed + no new warning: 192/291 (65.98%)  
iSMELL all checks (build + warning removed + no new warning + test): 172/291 (59.11%)  

Performance breakdown by classification:
iSMELL TP all checks: 123/191 (64.40%)  
iSMELL FP all checks: 49/100 (49.00%)  

iSMELL TP assumption sound (not in conjunction with if a fix was created): 0/0 (0.00%)  
iSMELL instances where TP assumption was not sound: 0 (0.00%)  

iSMELL fix correct (and fix created) (of instances where fix was created (and manually inspected)): 0/0 (0.00%)  
iSMELL fix correct (and fix created) (of all manually inspected warnings): 0/0 (0.00%)  
iSMELL fix correct (not only for instances where fix was created (no fix => incorrect fix) (of all manually inspected warnings)): 0/0 (0.00%)  

iSMELL further code smell introduced not reported by SonarQube (of all manually inspected warnings): 0/0 (0.00%)  
iSMELL further code smell introduced not reported by SonarQube (of all warnings where all other stats pass): 0/0 (0.00%)  
### Time Efficiency

iSMELL total time (instances with fixes): mean=52.1608s, median=35.3179s  
Number of instances with time data: 291/291  
### Token Consumption and Cost

**Total across all instances with token data (291 instances):**  
Total uncached input tokens: 1,608,312  
Total cached input tokens: 17,408  
Total output tokens: 932,150  
Total tokens: 2,557,870  
Total cost: $2.1365  

**Average per instance:**  
Average uncached input tokens: 5526.8  
Average cached input tokens: 59.8  
Average output tokens: 3203.3  
Average total tokens: 8789.9  
Average cost per instance: $0.0073  

**Cost efficiency:**  
Cost per successful fix (all checks passed): $0.0124  
Cost per fix created: $0.0073  
## Comparison Summary

**End-to-end performance comparison:**  
CodeCureAgent end-to-end: 251/291 (86.25%)  
iSMELL end-to-end 0/0 (0.00%)  
Performance gap (CCA - iSMELL): 86.25 percentage points  
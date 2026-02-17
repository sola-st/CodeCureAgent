
# iSMELL Comparison Analysis Results

## Total warnings in comparison

1000
## CodeCureAgent stats on the compared warnings

### CodeCureAgent Classification

TP: 696 (69.60%)  
FP: 304 (30.40%)  
Unclassified: 0 (0.00%)
### CodeCureAgent Plausible Fixes

Total plausible fixes: 968/1000 (96.80%)  
TP plausible fixes: 665/696 (95.55%)  
FP plausible fixes: 303/304 (99.67%)  
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

Single Line problems: 520 (52.00%)  
Multi Line problems: 444 (44.40%)  
Multi File problems: 36 (3.60%)  

Fix complexity split by type of fix:  
TP - Single Line: 231 (33.19%)  
TP - Multi Line: 429 (61.64%)  
TP - Multi File: 36 (5.17%)  
FP - Single Line: 289 (95.07%)  
FP - Multi Line: 15 (4.93%)  
FP - Multi File: 0 (0.00%)  

Number of plausible fixes created per fixComplexity:  
Single Line: 517 / 520 (99.42%)  
Multi Line: 424 / 444 (95.50%)  
Multi File: 27 / 36 (75.00%)  

Number of correct fixes created per fixComplexity (of the 251 sound and correct fixes (only for inspected samples)):  
Single Line: 154 (61.35%)  
Multi Line: 94 (37.45%)  
Multi File: 3 (1.20%)  
## iSMELL stats

### Performance Stats

iSMELL fix created: 1000/1000 (100.00%)  
iSMELL build successful: 807/1000 (80.70%)  
iSMELL warning removed: 921/1000 (92.10%)  
iSMELL no new warning introduced: 867/1000 (86.70%)  
iSMELL test successful: 754/1000 (75.40%)  

iSMELL build successful + warning removed: 741/1000 (74.10%)  
iSMELL build successful + warning removed + no new warning: 667/1000 (66.70%)  
iSMELL all checks (build + warning removed + no new warning + test): 628/1000 (62.80%)  

Performance breakdown by classification:
iSMELL TP all checks: 469/696 (67.39%)  
iSMELL FP all checks: 159/304 (52.30%)  

iSMELL TP assumption sound (not in conjunction with if a fix was created): 0/0 (0.00%)  
iSMELL instances where TP assumption was not sound: 0 (0.00%)  

iSMELL fix correct (and fix created) (of instances where fix was created (and manually inspected)): 0/0 (0.00%)  
iSMELL fix correct (and fix created) (of all manually inspected warnings): 0/0 (0.00%)  
iSMELL fix correct (not only for instances where fix was created (no fix => incorrect fix) (of all manually inspected warnings)): 0/0 (0.00%)  

iSMELL further code smell introduced not reported by SonarQube (of all manually inspected warnings): 0/0 (0.00%)  
iSMELL further code smell introduced not reported by SonarQube (of all warnings where all other stats pass): 0/0 (0.00%)  
### Time Efficiency

iSMELL total time (instances with fixes): mean=64.2807s, median=37.7031s  
Number of instances with time data: 1000/1000  
### Token Consumption and Cost

**Total across all instances with token data (1000 instances):**  
Total uncached input tokens: 6,795,323  
Total cached input tokens: 19,456  
Total output tokens: 4,082,390  
Total tokens: 10,897,169  
Total cost: $9.2519  

**Average per instance:**  
Average uncached input tokens: 6795.3  
Average cached input tokens: 19.5  
Average output tokens: 4082.4  
Average total tokens: 10897.2  
Average cost per instance: $0.0093  

**Cost efficiency:**  
Cost per successful fix (all checks passed): $0.0147  
Cost per fix created: $0.0093  
## Comparison Summary

**End-to-end performance comparison:**  
CodeCureAgent end-to-end: 251/291 (86.25%)  
iSMELL end-to-end 0/0 (0.00%)  
Performance gap (CCA - iSMELL): 86.25 percentage points  
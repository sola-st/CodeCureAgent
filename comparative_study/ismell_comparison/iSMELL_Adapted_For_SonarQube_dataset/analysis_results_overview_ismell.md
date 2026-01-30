
# iSMELL Comparison Analysis Results

## Total warnings in comparison

500
## CodeCureAgent stats on the compared warnings

### CodeCureAgent Classification

TP: 350 (70.00%)  
FP: 150 (30.00%)  
Unclassified: 0 (0.00%)
### CodeCureAgent Plausible Fixes

Total plausible fixes: 484/500 (96.80%)  
TP plausible fixes: 334/350 (95.43%)  
FP plausible fixes: 150/150 (100.00%)  
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

Single Line problems: 279 (55.80%)  
Multi Line problems: 203 (40.60%)  
Multi File problems: 18 (3.60%)  

Fix complexity split by type of fix:  
TP - Single Line: 136 (38.86%)  
TP - Multi Line: 196 (56.00%)  
TP - Multi File: 18 (5.14%)  
FP - Single Line: 143 (95.33%)  
FP - Multi Line: 7 (4.67%)  
FP - Multi File: 0 (0.00%)  

Number of plausible fixes created per fixComplexity:  
Single Line: 277 / 279 (99.28%)  
Multi Line: 195 / 203 (96.06%)  
Multi File: 12 / 18 (66.67%)  

Number of correct fixes created per fixComplexity (of the 251 sound and correct fixes (only for inspected samples)):  
Single Line: 154 (61.35%)  
Multi Line: 94 (37.45%)  
Multi File: 3 (1.20%)  
## iSMELL stats

### Performance Stats

iSMELL fix created: 500/500 (100.00%)  
iSMELL build successful: 405/500 (81.00%)  
iSMELL warning removed: 461/500 (92.20%)  
iSMELL no new warning introduced: 435/500 (87.00%)  
iSMELL test successful: 371/500 (74.20%)  

iSMELL build successful + warning removed: 368/500 (73.60%)  
iSMELL build successful + warning removed + no new warning: 330/500 (66.00%)  
iSMELL all checks (build + warning removed + no new warning + test): 303/500 (60.60%)  

Performance breakdown by classification:
iSMELL TP all checks: 231/350 (66.00%)  
iSMELL FP all checks: 72/150 (48.00%)  

iSMELL TP assumption sound (not in conjunction with if a fix was created): 0/0 (0.00%)  
iSMELL instances where TP assumption was not sound: 0 (0.00%)  

iSMELL fix correct (and fix created) (of instances where fix was created (and manually inspected)): 0/0 (0.00%)  
iSMELL fix correct (and fix created) (of all manually inspected warnings): 0/0 (0.00%)  
iSMELL fix correct (not only for instances where fix was created (no fix => incorrect fix) (of all manually inspected warnings)): 0/0 (0.00%)  

iSMELL further code smell introduced not reported by SonarQube (of all manually inspected warnings): 0/0 (0.00%)  
iSMELL further code smell introduced not reported by SonarQube (of all warnings where all other stats pass): 0/0 (0.00%)  
### Time Efficiency

iSMELL total time (instances with fixes): mean=60.4748s, median=36.7448s  
Number of instances with time data: 500/500  
### Token Consumption and Cost

**Total across all instances with token data (500 instances):**  
Total uncached input tokens: 2,974,935  
Total cached input tokens: 17,408  
Total output tokens: 1,788,811  
Total tokens: 4,781,154  
Total cost: $4.0538  

**Average per instance:**  
Average uncached input tokens: 5949.9  
Average cached input tokens: 34.8  
Average output tokens: 3577.6  
Average total tokens: 9562.3  
Average cost per instance: $0.0081  

**Cost efficiency:**  
Cost per successful fix (all checks passed): $0.0134  
Cost per fix created: $0.0081  
## Comparison Summary

**End-to-end performance comparison:**  
CodeCureAgent end-to-end: 251/291 (86.25%)  
iSMELL end-to-end 0/0 (0.00%)  
Performance gap (CCA - iSMELL): 86.25 percentage points  
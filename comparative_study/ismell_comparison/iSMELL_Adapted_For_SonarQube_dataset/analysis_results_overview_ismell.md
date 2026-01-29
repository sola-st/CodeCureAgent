
# iSMELL Comparison Analysis Results

## Total warnings in comparison

200
## CodeCureAgent stats on the compared warnings

### CodeCureAgent Classification

TP: 137 (68.50%)  
FP: 63 (31.50%)  
Unclassified: 0 (0.00%)
### CodeCureAgent Plausible Fixes

Total plausible fixes: 193/200 (96.50%)  
TP plausible fixes: 130/137 (94.89%)  
FP plausible fixes: 63/63 (100.00%)  
### CodeCureAgent Soundness of classification

Total sound classifications: 186/200 (93.00%)  
Sound TP classifications: 133/137 (97.08%)  
Sound FP classifications: 53/63 (84.13%)  
Precision: 0.97  
Recall: 0.93  
F1 Score: 0.95  
### CodeCureAgent Correctness of fix

Total correct fixes (sound and correct / sound and fixed): 172/180 (95.56%)  
Correct TP fixes  (sound and correct / sound and fixed): 119/127 (93.70%)  
Correct FP fixes  (sound and correct / sound and fixed): 53/53 (100.00%)  
### CodeCureAgent End-to-end performance (fixed, sound and correct)

End-to-end total: 172/200 (86.00%)  
End-to-end TP: 119/137 (86.86%)  
End-to-end FP: 53/63 (84.13%)  
### CodeCureAgent Fix Complexity

Single Line problems: 122 (61.00%)  
Multi Line problems: 69 (34.50%)  
Multi File problems: 9 (4.50%)  

Fix complexity split by type of fix:  
TP - Single Line: 64 (46.72%)  
TP - Multi Line: 64 (46.72%)  
TP - Multi File: 9 (6.57%)  
FP - Single Line: 58 (92.06%)  
FP - Multi Line: 5 (7.94%)  
FP - Multi File: 0 (0.00%)  

Number of plausible fixes created per fixComplexity:  
Single Line: 120 / 122 (98.36%)  
Multi Line: 68 / 69 (98.55%)  
Multi File: 5 / 9 (55.56%)  

Number of correct fixes created per fixComplexity (of the 172 sound and correct fixes (only for inspected samples)):  
Single Line: 105 (61.05%)  
Multi Line: 64 (37.21%)  
Multi File: 3 (1.74%)  
## iSMELL stats

### Performance Stats

iSMELL fix created: 200/200 (100.00%)  
iSMELL build successful: 159/200 (79.50%)  
iSMELL warning removed: 185/200 (92.50%)  
iSMELL no new warning introduced: 166/200 (83.00%)  
iSMELL test successful: 143/200 (71.50%)  

iSMELL build successful + warning removed: 145/200 (72.50%)  
iSMELL build successful + warning removed + no new warning: 123/200 (61.50%)  
iSMELL all checks (build + warning removed + no new warning + test): 109/200 (54.50%)  

Performance breakdown by classification:
iSMELL TP all checks: 81/137 (59.12%)  
iSMELL FP all checks: 28/63 (44.44%)  

iSMELL TP assumption sound (not in conjunction with if a fix was created): 0/0 (0.00%)  
iSMELL instances where TP assumption was not sound: 0 (0.00%)  

iSMELL fix correct (and fix created) (of instances where fix was created (and manually inspected)): 0/0 (0.00%)  
iSMELL fix correct (and fix created) (of all manually inspected warnings): 0/0 (0.00%)  
iSMELL fix correct (not only for instances where fix was created (no fix => incorrect fix) (of all manually inspected warnings)): 0/0 (0.00%)  

iSMELL further code smell introduced not reported by SonarQube (of all manually inspected warnings): 0/0 (0.00%)  
iSMELL further code smell introduced not reported by SonarQube (of all warnings where all other stats pass): 0/0 (0.00%)  
### Time Efficiency

iSMELL total time (instances with fixes): mean=47.9465s, median=27.8533s  
Number of instances with time data: 200/200  
### Token Consumption and Cost

**Total across all instances with token data (200 instances):**  
Total uncached input tokens: 1,070,022  
Total cached input tokens: 16,384  
Total output tokens: 616,789  
Total tokens: 1,703,195  
Total cost: $1.4165  

**Average per instance:**  
Average uncached input tokens: 5350.1  
Average cached input tokens: 81.9  
Average output tokens: 3083.9  
Average total tokens: 8516.0  
Average cost per instance: $0.0071  

**Cost efficiency:**  
Cost per successful fix (all checks passed): $0.0130  
Cost per fix created: $0.0071  
## Comparison Summary

**End-to-end performance comparison:**  
CodeCureAgent end-to-end: 172/200 (86.00%)  
iSMELL end-to-end 0/0 (0.00%)  
Performance gap (CCA - iSMELL): 86.00 percentage points  
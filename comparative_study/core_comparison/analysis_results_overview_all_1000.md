
# CORE Comparison Analysis Results

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
## CORE stats

### Separate Performance Stats

CORE fix created: 926/1000 (92.60%)  

CORE build successful (in at least one fix): 748/926 (80.78%)  
CORE build successful (in at least one fix) (of all instances): 748/1000 (74.80%)  

CORE removal of target warning (removed >=1 warnings in at least one fix): 925/926 (99.89%)  
CORE removal of target warning (removed >=1 warnings in at least one fix) (of all instances): 925/1000 (92.50%)  

CORE no new warning introduced (in at least one fix): 861/926 (92.98%)  
CORE no new warning introduced (in at least one fix) (of all instances): 861/1000 (86.10%)  

CORE test successful (in at least one fix) (of instances where fix created and build successful): 724/748 (96.79%)  
CORE test successful (in at least one fix) (of all instances): 724/1000 (72.40%)  

CORE TP assumption sound (not in conjunction with if a fix was created): 1000/1000 (100.00%)  
CORE instances where TP assumption was not sound: 0 (0.00%)  

CORE fix correct (and fix created) (of instances where fix was created (and manually inspected)): 0/0 (0.00%)  
CORE fix correct (and fix created) (of all manually inspected warnings): 0/0 (0.00%)  
CORE fix correct (not only for instances where fix was created (no fix => incorrect fix) (of all manually inspected warnings)): 0/0 (0.00%)  

CORE further code smell introduced not reported by SonarQube (of all manually inspected warnings): 0/0 (0.00%)  
CORE further code smell introduced not reported by SonarQube (of all warnings where all other stats pass): 0/0 (0.00%)  

### Averages of number of fixes

CORE average number of fixes created: 4.287  
CORE average number of oracle passing fixes created: 2.534  
CORE average number of fixes not passing oracle: 1.753  
CORE percentage of fixes passing oracle: 59.11%  
CORE average number of fixes to look at until fix passing oracle found: 1.691791633780584

CORE average number of correct fixes created: nan  
(Valid only if run only on manually inspected instances):  
CORE percentage of correct fixes: nan%  
CORE average number of fixes to look at until correct fix found: nan  

### Combined Performance Stats (Different checks are added together to form a final total performance of CORE)

CORE fix created: 926/1000 (92.60%)  
CORE fix created + build successful: 748/1000 (74.80%)  
CORE fix created + build successful + target warning removed: 746/1000 (74.60%)  
CORE fix created + build successful + target warning removed + no other warning introduced: 696/1000 (69.60%)  
CORE fix created + build successful + target warning removed + no other warning introduced + test successful: 676/1000 (67.60%)  

Margin of improvement from CORE's performance after oracle (all oracle steps applied) to CCA's plausible fix performance: 29.20%  
Margin of improvement from CORE's performance after oracle (all oracle steps applied) to CCA's end-to-end performance: 18.65%  

CORE Only for warnings classified as TP in CCA : fix created + build successful + target warning removed + no other warning introduced + test successful: 526/696 (75.57%)  
CORE Only for warnings classified as FP in CCA : fix created + build successful + target warning removed + no other warning introduced + test successful: 150/304 (49.34%)  
CORE fix created + build successful + target warning removed + no other warning introduced + test successful + fix correct: 0/0 (0.00%)  
CORE fix created + build successful + target warning removed + no other warning introduced + test successful + fix correct + no code smell outside introduced: 0/0 (0.00%)  

Margin of improvement from CORE's performance after oracle (all oracle steps applied) and correctness manual inspection to CCA's end-to-end performance: 86.25%  
### CORE Plausible Fixes per Fix Complexity

Number of plausible fixes (all checks applied) created per fixComplexity:  
Single Line: 335 / 520 (64.42%)  
Multi Line: 335 / 444 (75.45%)  
Multi File: 6 / 36 (16.67%)  
### Time Efficiency

CORE prompting time: mean=112.78s, median=110.62s  
CORE stage 4 time: mean=13.68s, median=12.55s  
CORE ranking time: mean=11.19s, median=8.09s  
CORE total time: mean=137.65s, median=133.02s  
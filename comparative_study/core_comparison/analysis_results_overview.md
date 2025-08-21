
# CORE Comparison Analysis Results

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
## CORE stats

### Separate Performance Stats

CORE fix created: 267/291 (91.75%)  

CORE build successful (in at least one fix): 233/267 (87.27%)  
CORE build successful (in at least one fix) (of all instances): 233/291 (80.07%)  

CORE removal of target warning (removed >=1 warnings in at least one fix): 266/267 (99.63%)  
CORE removal of target warning (removed >=1 warnings in at least one fix) (of all instances): 266/291 (91.41%)  

CORE no new warning introduced (in at least one fix): 250/267 (93.63%)  
CORE no new warning introduced (in at least one fix) (of all instances): 250/291 (85.91%)  

CORE test successful (in at least one fix) (of instances where fix created and build successful): 220/233 (94.42%)  
CORE test successful (in at least one fix) (of all instances): 220/291 (75.60%)  

CORE TP assumption sound (not in conjunction with if a fix was created): 291/291 (100.00%)  
CORE instances where TP assumption was not sound: 0 (0.00%)  

CORE fix correct (and fix created) (of instances where fix was created (and manually inspected)): 0/0 (0.00%)  
CORE fix correct (and fix created) (of all manually inspected warnings): 0/0 (0.00%)  
CORE fix correct (not only for instances where fix was created (no fix => incorrect fix) (of all manually inspected warnings)): 0/0 (0.00%)  

CORE further code smell introduced not reported by SonarQube (of all manually inspected warnings): 0/0 (0.00%)  
CORE further code smell introduced not reported by SonarQube (of all warnings where all other stats pass): 0/0 (0.00%)  

### Averages of number of fixes

CORE average number of fixes created: 4.446735395189004  
CORE average number of oracle passing fixes created: 2.536082474226804  
CORE average number of fixes not passing oracle: 1.9106529209621994  
CORE percentage of fixes passing oracle: 57.03%  
CORE average number of fixes to look at until fix passing oracle found: 1.753387533875339

CORE average number of correct fixes created: nan  
(Valid only if run only on manually inspected instances):  
CORE percentage of correct fixes: nan%  
CORE average number of fixes to look at until correct fix found: nan  

### Combined Performance Stats (Different checks are added together to form a final total performance of CORE)

CORE fix created: 267/291 (91.75%)  
CORE fix created + build successful: 233/291 (80.07%)  
CORE fix created + build successful + target warning removed: 231/291 (79.38%)  
CORE fix created + build successful + target warning removed + no other warning introduced: 210/291 (72.16%)  
CORE fix created + build successful + target warning removed + no other warning introduced + test successful: 197/291 (67.70%)  
CORE Only for warnings classified as TP in CCA : fix created + build successful + target warning removed + no other warning introduced + test successful: 146/191 (76.44%)  
CORE fix created + build successful + target warning removed + no other warning introduced + test successful + fix correct: 0/0 (0.00%)  
CORE fix created + build successful + target warning removed + no other warning introduced + test successful + fix correct + no code smell outside introduced: 0/0 (0.00%)  
### Time Efficiency

CORE prompting time: mean=106.33s, median=92.45s  
CORE stage 4 time: mean=14.07s, median=13.92s  
CORE ranking time: mean=15.75s, median=11.07s  
CORE total time: mean=136.13s, median=122.22s  
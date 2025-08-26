
# CORE Comparison Analysis Results

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
## CORE stats

### Separate Performance Stats

CORE fix created: 19/21 (90.48%)  

CORE build successful (in at least one fix): 18/19 (94.74%)  
CORE build successful (in at least one fix) (of all instances): 18/21 (85.71%)  

CORE removal of target warning (removed >=1 warnings in at least one fix): 19/19 (100.00%)  
CORE removal of target warning (removed >=1 warnings in at least one fix) (of all instances): 19/21 (90.48%)  

CORE no new warning introduced (in at least one fix): 19/19 (100.00%)  
CORE no new warning introduced (in at least one fix) (of all instances): 19/21 (90.48%)  

CORE test successful (in at least one fix) (of instances where fix created and build successful): 17/18 (94.44%)  
CORE test successful (in at least one fix) (of all instances): 17/21 (80.95%)  

CORE TP assumption sound (not in conjunction with if a fix was created): 19/21 (90.48%)  
CORE instances where TP assumption was not sound: 2 (9.52%)  

CORE fix correct (and fix created) (of instances where fix was created (and manually inspected)): 14/19 (73.68%)  
CORE fix correct (and fix created) (of all manually inspected warnings): 14/21 (66.67%)  
CORE fix correct (not only for instances where fix was created (no fix => incorrect fix) (of all manually inspected warnings)): 14/21 (66.67%)  

CORE further code smell introduced not reported by SonarQube (of all manually inspected warnings): 0/21 (0.00%)  
CORE further code smell introduced not reported by SonarQube (of all warnings where all other stats pass): 0/14 (0.00%)  

### Averages of number of fixes

CORE average number of fixes created: 3.619047619047619  
CORE average number of oracle passing fixes created: 2.1904761904761907  
CORE average number of fixes not passing oracle: 1.4285714285714286  
CORE percentage of fixes passing oracle: 60.53%  
CORE average number of fixes to look at until fix passing oracle found: 1.652173913043478

CORE average number of correct fixes created: 1.8095238095238095  
(Valid only if run only on manually inspected instances):  
CORE percentage of correct fixes: 50.00%  
CORE average number of fixes to look at until correct fix found: 2.0  

### Combined Performance Stats (Different checks are added together to form a final total performance of CORE)

CORE fix created: 19/21 (90.48%)  
CORE fix created + build successful: 18/21 (85.71%)  
CORE fix created + build successful + target warning removed: 18/21 (85.71%)  
CORE fix created + build successful + target warning removed + no other warning introduced: 18/21 (85.71%)  
CORE fix created + build successful + target warning removed + no other warning introduced + test successful: 17/21 (80.95%)  

Margin of improvement from CORE's performance after oracle (all oracle steps applied) to CCA's plausible fix performance: 19.05%  
Margin of improvement from CORE's performance after oracle (all oracle steps applied) to CCA's end-to-end performance: 14.29%  

CORE Only for warnings classified as TP in CCA : fix created + build successful + target warning removed + no other warning introduced + test successful: 11/12 (91.67%)  
CORE fix created + build successful + target warning removed + no other warning introduced + test successful + fix correct: 14/21 (66.67%)  
CORE fix created + build successful + target warning removed + no other warning introduced + test successful + fix correct + no code smell outside introduced: 14/21 (66.67%)  

Margin of improvement from CORE's performance after oracle (all oracle steps applied) and correctness manual inspection to CCA's end-to-end performance: 28.57%  
### Time Efficiency

CORE prompting time: mean=106.64s, median=93.54s  
CORE stage 4 time: mean=10.96s, median=8.66s  
CORE ranking time: mean=14.00s, median=8.80s  
CORE total time: mean=131.61s, median=103.88s  
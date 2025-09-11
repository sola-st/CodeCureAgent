
# CORE Comparison Analysis Results

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
## CORE stats

### Separate Performance Stats

CORE fix created: 57/62 (91.94%)  

CORE build successful (in at least one fix): 49/57 (85.96%)  
CORE build successful (in at least one fix) (of all instances): 49/62 (79.03%)  

CORE removal of target warning (removed >=1 warnings in at least one fix): 57/57 (100.00%)  
CORE removal of target warning (removed >=1 warnings in at least one fix) (of all instances): 57/62 (91.94%)  

CORE no new warning introduced (in at least one fix): 56/57 (98.25%)  
CORE no new warning introduced (in at least one fix) (of all instances): 56/62 (90.32%)  

CORE test successful (in at least one fix) (of instances where fix created and build successful): 48/49 (97.96%)  
CORE test successful (in at least one fix) (of all instances): 48/62 (77.42%)  

CORE TP assumption sound (not in conjunction with if a fix was created): 60/62 (96.77%)  
CORE instances where TP assumption was not sound: 2 (3.23%)  

CORE fix correct (and fix created) (of instances where fix was created (and manually inspected)): 14/19 (73.68%)  
CORE fix correct (and fix created) (of all manually inspected warnings): 14/21 (66.67%)  
CORE fix correct (not only for instances where fix was created (no fix => incorrect fix) (of all manually inspected warnings)): 14/21 (66.67%)  

CORE further code smell introduced not reported by SonarQube (of all manually inspected warnings): 0/21 (0.00%)  
CORE further code smell introduced not reported by SonarQube (of all warnings where all other stats pass): 0/14 (0.00%)  

### Averages of number of fixes

CORE average number of fixes created: 3.596774193548387  
CORE average number of oracle passing fixes created: 2.6129032258064515  
CORE average number of fixes not passing oracle: 0.9838709677419355  
CORE percentage of fixes passing oracle: 72.65%  
CORE average number of fixes to look at until fix passing oracle found: 1.3765432098765435

CORE average number of correct fixes created: 1.8095238095238095  
(Valid only if run only on manually inspected instances):  
CORE percentage of correct fixes: 50.31%  
CORE average number of fixes to look at until correct fix found: 1.9876910016977927  

### Combined Performance Stats (Different checks are added together to form a final total performance of CORE)

CORE fix created: 57/62 (91.94%)  
CORE fix created + build successful: 49/62 (79.03%)  
CORE fix created + build successful + target warning removed: 49/62 (79.03%)  
CORE fix created + build successful + target warning removed + no other warning introduced: 48/62 (77.42%)  
CORE fix created + build successful + target warning removed + no other warning introduced + test successful: 47/62 (75.81%)  

Margin of improvement from CORE's performance after oracle (all oracle steps applied) to CCA's plausible fix performance: 24.19%  
Margin of improvement from CORE's performance after oracle (all oracle steps applied) to CCA's end-to-end performance: 19.43%  

CORE Only for warnings classified as TP in CCA : fix created + build successful + target warning removed + no other warning introduced + test successful: 38/44 (86.36%)  
CORE fix created + build successful + target warning removed + no other warning introduced + test successful + fix correct: 14/21 (66.67%)  
CORE fix created + build successful + target warning removed + no other warning introduced + test successful + fix correct + no code smell outside introduced: 14/21 (66.67%)  

Margin of improvement from CORE's performance after oracle (all oracle steps applied) and correctness manual inspection to CCA's end-to-end performance: 28.57%  
### Time Efficiency

CORE prompting time: mean=96.42s, median=77.74s  
CORE stage 4 time: mean=12.77s, median=11.46s  
CORE ranking time: mean=9.97s, median=6.53s  
CORE total time: mean=119.16s, median=97.25s  
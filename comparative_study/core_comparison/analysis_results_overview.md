
# CORE Comparison Analysis Results

## Total warnings in comparison

10
## CodeCureAgent stats on the compared warnings

### CodeCureAgent Classification

TP: 6 (60.00%)  
FP: 4 (40.00%)  
Unclassified: 0 (0.00%)
### CodeCureAgent Plausible Fixes

Total plausible fixes: 9/10 (90.00%)  
TP plausible fixes: 5/6 (83.33%)  
FP plausible fixes: 4/4 (100.00%)  
### CodeCureAgent Soundness of classification

Total sound classifications: 9/10 (90.00%)  
Sound TP classifications: 5/6 (83.33%)  
Sound FP classifications: 4/4 (100.00%)  
Precision: 0.83  
Recall: 1.00  
F1 Score: 0.91  
### CodeCureAgent Correctness of fix

Total correct fixes (sound and correct / sound and fixed): 9/9 (100.00%)  
Correct TP fixes  (sound and correct / sound and fixed): 5/5 (100.00%)  
Correct FP fixes  (sound and correct / sound and fixed): 4/4 (100.00%)  
### CodeCureAgent End-to-end performance (fixed, sound and correct)

End-to-end total: 9/10 (90.00%)  
End-to-end TP: 5/6 (83.33%)  
End-to-end FP: 4/4 (100.00%)  
### CodeCureAgent Fix Complexity

Single Line problems: 8 (80.00%)  
Multi Line problems: 1 (10.00%)  
Multi File problems: 1 (10.00%)  

Fix complexity split by type of fix:  
TP - Single Line: 4 (66.67%)  
TP - Multi Line: 1 (16.67%)  
TP - Multi File: 1 (16.67%)  
FP - Single Line: 4 (100.00%)  
FP - Multi Line: 0 (0.00%)  
FP - Multi File: 0 (0.00%)  

Number of plausible fixes created per fixComplexity:  
Single Line: 7 / 8 (87.50%)  
Multi Line: 1 / 1 (100.00%)  
Multi File: 1 / 1 (100.00%)  

Number of correct fixes created per fixComplexity (of the 9 sound and correct fixes (only for inspected samples)):  
Single Line: 7 (77.78%)  
Multi Line: 1 (11.11%)  
Multi File: 1 (11.11%)  
## CORE stats

### Separate Performance Stats

CORE fix created: 9/10 (90.00%)  

CORE build successful (in at least one fix): 6/9 (66.67%)  
CORE build successful (in at least one fix) (of all instances): 6/10 (60.00%)  

CORE removal of target warning (removed >=1 warnings in at least one fix): 9/9 (100.00%)  
CORE removal of target warning (removed >=1 warnings in at least one fix) (of all instances): 9/10 (90.00%)  

CORE no new warning introduced (in at least one fix): 8/9 (88.89%)  
CORE no new warning introduced (in at least one fix) (of all instances): 8/10 (80.00%)  

CORE test successful (in at least one fix) (of instances where fix created and build successful): 5/6 (83.33%)  
CORE test successful (in at least one fix) (of all instances): 5/10 (50.00%)  

CORE TP assumption sound (not in conjunction with if a fix was created): 10/10 (100.00%)  
CORE instances where TP assumption was not sound: 0 (0.00%)  

CORE fix correct (and fix created) (of instances where fix was created (and manually inspected)): 0/0 (0.00%)  
CORE fix correct (and fix created) (of all manually inspected warnings): 0/0 (0.00%)  
CORE fix correct (not only for instances where fix was created (no fix => incorrect fix) (of all manually inspected warnings)): 0/0 (0.00%)  

CORE further code smell introduced not reported by SonarQube (of all manually inspected warnings): 0/0 (0.00%)  
CORE further code smell introduced not reported by SonarQube (of all warnings where all other stats pass): 0/0 (0.00%)  

### Averages of number of fixes

CORE average number of fixes created: 4.6  
CORE average number of oracle passing fixes created: 2.5  
CORE average number of fixes not passing oracle: 2.1  
CORE percentage of fixes passing oracle: 54.35%  
CORE average number of fixes to look at until fix passing oracle found: 1.84

CORE average number of correct fixes created: nan  
(Valid only if run only on manually inspected instances):  
CORE percentage of correct fixes: nan%  
CORE average number of fixes to look at until correct fix found: nan  

### Combined Performance Stats (Different checks are added together to form a final total performance of CORE)

CORE fix created: 9/10 (90.00%)  
CORE fix created + build successful: 6/10 (60.00%)  
CORE fix created + build successful + target warning removed: 6/10 (60.00%)  
CORE fix created + build successful + target warning removed + no other warning introduced: 6/10 (60.00%)  
CORE fix created + build successful + target warning removed + no other warning introduced + test successful: 5/10 (50.00%)  
CORE fix created + build successful + target warning removed + no other warning introduced + test successful + fix correct: 0/0 (0.00%)  
CORE fix created + build successful + target warning removed + no other warning introduced + test successful + fix correct + no code smell outside introduced: 0/0 (0.00%)  
### Time Efficiency

CORE prompting time: mean=116.94s, median=114.06s  
CORE stage 4 time: mean=14.31s, median=14.44s  
CORE ranking time: mean=16.91s, median=14.30s  
CORE total time: mean=147.56s, median=144.89s  
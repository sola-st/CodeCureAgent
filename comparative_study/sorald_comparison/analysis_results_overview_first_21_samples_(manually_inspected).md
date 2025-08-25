
# Sorald Comparison Analysis Results

## Total warnings covered by Sorald

21
## CodeCureAgent stats on the Sorald-covered warnings

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
## Sorald stats on the Sorald-covered warnings

### Separate Performance Stats

Sorald fix created: 19/21 (90.48%)  

Sorald build successful (of instances where fix was created): 18/19 (94.74%)  
Sorald build successful (after creating a fix) (of all instances): 18/21 (85.71%)  

Sorald removal of target warning (removed >=1 warnings) (of instances where fix was created): 19/19 (100.00%)  
Sorald removal of target warning (removed >=1 warnings) (after creating a fix) (of all instances): 19/21 (90.48%)  

Sorald no new warning introduced (of instances where fix was created): 18/19 (94.74%)  
Sorald no new warning introduced (after creating a fix) (of all instances): 18/21 (85.71%)  

Sorald test successful (of instances where fix created and build successful): 18/18 (100.00%)  
Sorald test successful (after creating a fix and passing build) (of all instances): 18/21 (85.71%)  

Sorald TP assumption sound (not in conjunction with if a fix was created): 19/21 (90.48%)  
Sorald instances where TP assumption was not sound: 2 (9.52%)  

Sorald fix correct (and fix created) (of instances where fix was created (and manually inspected)): 16/19 (84.21%)  
Sorald fix correct (and fix created) (of all manually inspected warnings): 16/21 (76.19%)  
Sorald fix correct (not only for instances where fix was created (no fix => incorrect fix) (of all manually inspected warnings)): 16/21 (76.19%)  

Sorald further code smell introduced not reported by SonarQube (of all manually inspected warnings): 2/21 (9.52%)  
Sorald further code smell introduced not reported by SonarQube (of all warnings where all other stats pass): 1/16 (6.25%)  

### Combined Performance Stats (Different checks are added together to form a final total performance of Sorald)

Sorald fix created: 19/21 (90.48%)  
Sorald fix created + build successful: 18/21 (85.71%)  
Sorald fix created + build successful + target warning removed: 18/21 (85.71%)  
Sorald fix created + build successful + target warning removed + no other warning introduced: 17/21 (80.95%)  
Sorald fix created + build successful + target warning removed + no other warning introduced + test successful: 17/21 (80.95%)  

Margin of improvement from Sorald's performance after oracle (all oracle steps applied) to CCA's plausible fix performance: 19.05%  
 
Margin of improvement from Sorald's performance after oracle (all oracle steps applied) to CCA's end-to-end performance: 14.29%  


#### Most relevant here: 
Sorald fix created + build successful + target warning removed + no other warning introduced + test successful + fix correct: 16/21 (76.19%)  
Margin of improvement from Sorald's performance after oracle (all oracle steps applied) and manual inspection to CCA's end-to-end performance: 19.05%  

Sorald fix created + build successful + target warning removed + no other warning introduced + test successful + fix correct + no code smell outside introduced: 15/21 (71.43%)  
### Time Efficiency

Sorald fixing time: mean=16.89s, median=15.53s

# Sorald Comparison Analysis Results

## Total warnings covered by Sorald

62
## CodeCureAgent stats on the Sorald-covered warnings

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
## Sorald stats on the Sorald-covered warnings

### Separate Performance Stats

Sorald fix created: 55/62 (88.71%)  

Sorald build successful (of instances where fix was created): 46/55 (83.64%)  
Sorald build successful (after creating a fix) (of all instances): 46/62 (74.19%)  

Sorald removal of target warning (removed >=1 warnings) (of instances where fix was created): 54/55 (98.18%)  
Sorald removal of target warning (removed >=1 warnings) (after creating a fix) (of all instances): 54/62 (87.10%)  

Sorald no new warning introduced (of instances where fix was created): 50/55 (90.91%)  
Sorald no new warning introduced (after creating a fix) (of all instances): 50/62 (80.65%)  

Sorald test successful (of instances where fix created and build successful): 45/46 (97.83%)  
Sorald test successful (after creating a fix and passing build) (of all instances): 45/62 (72.58%)  

Sorald TP assumption sound (not in conjunction with if a fix was created): 55/62 (88.71%)  
Sorald instances where TP assumption was not sound: 7 (11.29%)  

These are biased as we manually inspected the further FPs (check the file with only the CCA manually inspected samples for correct values):  
(Sorald fix correct (and fix created) (of instances where fix was created (and manually inspected)): 19/28 (67.86%)  
Sorald fix correct (and fix created) (of all manually inspected warnings): 19/30 (63.33%)  
Sorald fix correct (not only for instances where fix was created (no fix => incorrect fix) (of all manually inspected warnings)): 19/30 (63.33%))

Sorald further code smell introduced not reported by SonarQube (of all manually inspected warnings): 2/30 (6.67%)  
Sorald further code smell introduced not reported by SonarQube (of all warnings where all other stats pass): 1/19 (5.26%)  

### Combined Performance Stats (Different checks are added together to form a final total performance of Sorald)

Sorald fix created: 55/62 (88.71%)  
Sorald fix created + build successful: 46/62 (74.19%)  
Sorald fix created + build successful + target warning removed: 46/62 (74.19%)  
Sorald fix created + build successful + target warning removed + no other warning introduced: 44/62 (70.97%)  

#### Most relevant here:  
Sorald fix created + build successful + target warning removed + no other warning introduced + test successful: 43/62 (69.35%)  
Margin of improvement from Sorald's performance after oracle (all oracle steps applied) to CCA's plausible fix performance: 30.65%  



#### Not applicable here: 
Margin of improvement from Sorald's performance after oracle (all oracle steps applied) to CCA's end-to-end performance: 25.88%  
CCA's end-to-end is only over 21 samples, the Sorald's performance over the 62 => see file on 21 samples


These are biased as we manually inspected the further FPs (check the file with only the CCA manually inspected samples for correct values):  
(
Sorald fix created + build successful + target warning removed + no other warning introduced + test successful + fix correct: 19/30 (63.33%)  
Margin of improvement from Sorald's performance after oracle (all oracle steps applied) and manual inspection to CCA's end-to-end performance: 31.90%  

Sorald fix created + build successful + target warning removed + no other warning introduced + test successful + fix correct + no code smell outside introduced: 18/30 (60.00%)
)  
### Time Efficiency

Sorald fixing time: mean=31.59s, median=16.12s
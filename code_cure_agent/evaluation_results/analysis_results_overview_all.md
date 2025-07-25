
# Experiment Analysis Results

## Overall stats

### Total rule violations

1000
### Classification

TP: 696 (69.60%)  
FP: 304 (30.40%)  
Unclassified: 0 (0.00%)
### Plausible Fixes

Total plausible fixes: 968/1000 (96.80%)  
TP plausible fixes: 665/696 (95.55%)  
FP plausible fixes: 303/304 (99.67%)  
#### Passed previous steps

Total compilation step passed: 984/1000 (98.40%)  
TP compilation step passed: 680/696 (97.70%)  
FP compilation step passed: 304/304 (100.00%)  
Total SonarQube check step passed: 970/1000 (97.00%)  
TP SonarQube check step passed: 667/696 (95.83%)  
FP SonarQube check step passed: 303/304 (99.67%)  
### Soundness of classification

Total sound classifications: 268/291 (92.10%)  
Sound TP classifications: 186/191 (97.38%)  
Sound FP classifications: 82/100 (82.00%)  
Precision: 0.97  
Recall: 0.91  
F1 Score: 0.94  
### Correctness of fix

Total correct fixes (sound and correct / sound and fixed): 252/260 (96.92%)  
Correct TP fixes  (sound and correct / sound and fixed): 170/178 (95.51%)  
Correct FP fixes  (sound and correct / sound and fixed): 82/82 (100.00%)  
### End-to-end performance (fixed, sound and correct)

End-to-end total: 252/291 (86.60%)  
End-to-end TP: 170/191 (89.01%)  
End-to-end FP: 82/100 (82.00%)  
### Fix Complexity

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

Number of correct fixes created per fixComplexity (of the 252 sound and correct fixes (only for inspected samples)):  
Single Line: 155 (61.51%)  
Multi Line: 94 (37.30%)  
Multi File: 3 (1.19%)  
### Iterations

Total iterations: 17495  
Iterations by sub-agent:  
Classification: 9954  
Fix_TP: 7102  
Fix_FP: 439  

Mean iterations: 17.50  
Mean iterations by sub-agent:  
Classification: 9.95  
Fix_TP: 7.10  
Fix_FP: 0.44  

Median iterations: 19.00  
Median iterations by sub-agent:  
Classification: 9.00  
Fix_TP: 7.00  
Fix_FP: 3.00  
### Number of Plausible and Implausible Fixes created

Mean number of implausible fixes: 0.95  
Mean number of implausible fixes (TP): 1.26  
Mean number of implausible fixes (FP): 0.26  
Mean number of plausible fixes: 0.97  
Mean number of plausible fixes (TP): 0.96  
Mean number of plausible fixes (FP): 1.00  

Median number of implausible fixes: 0.00  
Median number of implausible fixes (TP): 0.00  
Median number of implausible fixes (FP): 0.00  
Median number of plausible fixes: 1.00  
Median number of plausible fixes (TP): 1.00  
Median number of plausible fixes (FP): 1.00  
### Ablation of the ChangeApprover

#### No ChangeApprover (accepts more fixes than with ChangeApprover (all the fixed ones + the unfixed ones) => how many of these are still plausible (would pass the full ChangeApprover) => all the other ones would falsely be labeled as plausible)

Still plausible fixes / accepted fixes: 751 / 1000 (75.10%)  
Still plausible fixes / accepted fixes (TP): 486 / 696 (69.83%)  
Still plausible fixes / accepted fixes (FP): 265 / 304 (87.17%)  
#### Only build step (no SonarQube check and test steps)

Still plausible fixes / accepted fixes: 861 / 984 (87.50%)  
Still plausible fixes / accepted fixes (TP): 582 / 680 (85.59%)  
Still plausible fixes / accepted fixes (FP): 279 / 304 (91.78%)  
#### Only build and SonarQube check steps (no test step)

Still plausible fixes / accepted fixes: 961 / 970 (99.07%)  
Still plausible fixes / accepted fixes (TP): 658 / 667 (98.65%)  
Still plausible fixes / accepted fixes (FP): 303 / 303 (100.00%)  
### Execution time

Total execution time: 3427.21 minutes  
Execution time by sub-agent:  
Classification: 1359.39 minutes  
Fix_TP: 1708.13 minutes  
Fix_FP: 359.7 minutes  

Mean execution time: 3.43 minutes  
Mean execution time by sub-agent:  
Classification: 1.36 minutes  
Fix_TP: 2.45 minutes  
Fix_FP: 1.18 minutes  

Median execution time: 2.75 minutes  
Median execution time by sub-agent:  
Classification: 1.21 minutes  
Fix_TP: 1.61 minutes  
Fix_FP: 0.93 minutes  
### Cost

#### Tokens Count

Total tokens count: 139584923  
Total tokens input uncached: 28638064  
Total tokens input cached: 106711424  
Total tokens input: 135349488  
Total tokens output: 4235435  
Tokens by sub-agent:  
Classification: 50783633 (input uncached: 16233572, input cached: 32649600, input: 48883172, output: 1900461)  
Fix_TP: 85393950 (input uncached: 11327114, input cached: 71926272, input: 83253386, output: 2140564)  
Fix_FP: 3407340 (input uncached: 1077378, input cached: 2135552, input: 3212930, output: 194410)  
#### Mean Tokens Count

Mean total tokens count: 139584.92  
Mean total tokens input uncached: 28638.06  
Mean total tokens input cached: 106711.42  
Mean total tokens input: 135349.49  
Mean total tokens output: 4235.43  
Mean tokens by sub-agent:  
Classification: 50783.63 (input uncached: 16233.57, input cached: 32649.60, input: 48883.17, output: 1900.46)  
Fix_TP: 85393.95 (input uncached: 11327.11, input cached: 71926.27, input: 83253.39, output: 2140.56)  
Fix_FP: 3407.34 (input uncached: 1077.38, input cached: 2135.55, input: 3212.93, output: 194.41)  
#### Median Tokens Count

Median total tokens count: 79303.50  
Median total tokens input uncached: 25743.50  
Median total tokens input cached: 50048.00  
Median total tokens input: 75791.50  
Median total tokens output: 3512.00  
Median tokens by sub-agent:  
Classification: 42315.50 (input uncached: 14587.00, input cached: 25984.00, input: 40571.00, output: 1744.50)  
Fix_TP: 29315.50 (input uncached: 8061.50, input cached: 19968.00, input: 28029.50, output: 1286.00)  
Fix_FP: 7672.50 (input uncached: 3095.00, input cached: 4096.00, input: 7191.00, output: 481.50)  
#### Tokens Cost

Total Cost: 28.903064 USD  
Cost by sub-agent:  
Classification: 12.799126 USD  
Fix_TP: 15.148375 USD  
Fix_FP: 0.955562 USD  
#### Average Tokens Cost

Average Total Cost: 0.028903 USD  
Average cost by sub-agent:  
Classification: 0.012799 USD  
Fix_TP: 0.015148 USD  
Fix_FP: 0.000956 USD  
#### Median Tokens Cost

Median Total Cost: 0.020956 USD  
Median cost by sub-agent:  
Classification: 0.011208 USD  
Fix_TP: 0.007391 USD  
Fix_FP: 0.002357 USD  
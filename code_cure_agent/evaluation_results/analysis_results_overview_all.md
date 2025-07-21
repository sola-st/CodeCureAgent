
# Experiment Analysis Results

## Overall stats

### Total rule violations

741
### Classification

TP: 514 (69.37%)  
FP: 227 (30.63%)  
Unclassified: 0 (0.00%)
### Plausible Fixes

Total plausible fixes: 714/741 (96.36%)  
TP plausible fixes: 487/514 (94.75%)  
FP plausible fixes: 227/227 (100.00%)  
#### Passed previous steps

Total compilation step passed: 727/741 (98.11%)  
TP compilation step passed: 500/514 (97.28%)  
FP compilation step passed: 227/227 (100.00%)  
Total SonarQube check step passed: 717/741 (96.76%)  
TP SonarQube check step passed: 490/514 (95.33%)  
FP SonarQube check step passed: 227/227 (100.00%)  
### Soundness of classification

Total sound classifications: 268/291 (92.10%)  
Sound TP classifications: 186/191 (97.38%)  
Sound FP classifications: 82/100 (82.00%)  
### Correctness of fix

Total correct fixes (sound and correct / sound and fixed): 252/260 (96.92%)  
Correct TP fixes  (sound and correct / sound and fixed): 170/178 (95.51%)  
Correct FP fixes  (sound and correct / sound and fixed): 82/82 (100.00%)  
### End-to-end performance (fixed, sound and correct)

End-to-end total: 252/291 (86.60%)  
End-to-end TP: 170/191 (89.01%)  
End-to-end FP: 82/100 (82.00%)  
### Fix Complexity

Single Line problems: 395 (53.31%)  
Multi Line problems: 319 (43.05%)  
Multi File problems: 27 (3.64%)  

Fix complexity split by type of fix:  
TP - Single Line: 178 (34.63%)  
TP - Multi Line: 309 (60.12%)  
TP - Multi File: 27 (5.25%)  
FP - Single Line: 217 (95.59%)  
FP - Multi Line: 10 (4.41%)  
FP - Multi File: 0 (0.00%)  

Number of plausible fixes created per fixComplexity:  
Single Line: 393 / 395 (99.49%)  
Multi Line: 302 / 319 (94.67%)  
Multi File: 19 / 27 (70.37%)  

Number of correct fixes created per fixComplexity (of the 252 sound and correct fixes (only for inspected samples)):  
Single Line: 155 (61.51%)  
Multi Line: 94 (37.30%)  
Multi File: 3 (1.19%)  
### Iterations

Total iterations: 13268  
Iterations by sub-agent:  
Classification: 7447  
Fix_TP: 5502  
Fix_FP: 319  

Mean iterations: 17.91  
Mean iterations by sub-agent:  
Classification: 10.05  
Fix_TP: 7.43  
Fix_FP: 0.43  

Median iterations: 19.00  
Median iterations by sub-agent:  
Classification: 9.00  
Fix_TP: 7.00  
Fix_FP: 3.00  
### Number of Plausible and Implausible Fixes created

Mean number of implausible fixes: 1.02  
Mean number of implausible fixes (TP): 1.39  
Mean number of implausible fixes (FP): 0.19  
Mean number of plausible fixes: 0.97  
Mean number of plausible fixes (TP): 0.95  
Mean number of plausible fixes (FP): 1.00  

Median number of implausible fixes: 0.00  
Median number of implausible fixes (TP): 0.00  
Median number of implausible fixes (FP): 0.00  
Median number of plausible fixes: 1.00  
Median number of plausible fixes (TP): 1.00  
Median number of plausible fixes (FP): 1.00  
### Ablation of the ChangeApprover

#### No ChangeApprover (accepts more fixes than with ChangeApprover (all the fixed ones + the unfixed ones) => how many of these are still plausible (would pass the full ChangeApprover) => all the other ones would falsely be labeled as plausible)

Still plausible fixes / accepted fixes: 556 / 741 (75.03%)  
Still plausible fixes / accepted fixes (TP): 356 / 514 (69.26%)  
Still plausible fixes / accepted fixes (FP): 200 / 227 (88.11%)  
#### Only build step (no SonarQube check and test steps)

Still plausible fixes / accepted fixes: 637 / 727 (87.62%)  
Still plausible fixes / accepted fixes (TP): 425 / 500 (85.00%)  
Still plausible fixes / accepted fixes (FP): 212 / 227 (93.39%)  
#### Only build and SonarQube check steps (no test step)

Still plausible fixes / accepted fixes: 709 / 717 (98.88%)  
Still plausible fixes / accepted fixes (TP): 482 / 490 (86.73%)  
Still plausible fixes / accepted fixes (FP): 227 / 227 (93.39%)  
### Execution time

Total execution time: 2574.49 minutes  
Execution time by sub-agent:  
Classification: 998.56 minutes  
Fix_TP: 1318.52 minutes  
Fix_FP: 257.41 minutes  

Mean execution time: 3.47 minutes  
Mean execution time by sub-agent:  
Classification: 1.35 minutes  
Fix_TP: 2.57 minutes  
Fix_FP: 1.13 minutes  

Median execution time: 2.79 minutes  
Median execution time by sub-agent:  
Classification: 1.2 minutes  
Fix_TP: 1.64 minutes  
Fix_FP: 0.93 minutes  
### Cost

#### Tokens Count

Total tokens count: 110721178  
Total tokens input uncached: 21897795  
Total tokens input cached: 85557760  
Total tokens input: 107455555  
Total tokens output: 3265623  
Tokens by sub-agent:  
Classification: 38144437 (input uncached: 12129654, input cached: 24592768, input: 36722422, output: 1422015)  
Fix_TP: 70236295 (input uncached: 8967050, input cached: 59564672, input: 68531722, output: 1704573)  
Fix_FP: 2340446 (input uncached: 801091, input cached: 1400320, input: 2201411, output: 139035)  
#### Mean Tokens Count

Mean total tokens count: 149421.29  
Mean total tokens input uncached: 29551.68  
Mean total tokens input cached: 115462.56  
Mean total tokens input: 145014.24  
Mean total tokens output: 4407.05  
Mean tokens by sub-agent:  
Classification: 51476.97 (input uncached: 16369.30, input cached: 33188.62, input: 49557.92, output: 1919.05)  
Fix_TP: 94785.82 (input uncached: 12101.28, input cached: 80384.17, input: 92485.45, output: 2300.37)  
Fix_FP: 3158.50 (input uncached: 1081.09, input cached: 1889.77, input: 2970.87, output: 187.63)  
#### Median Tokens Count

Median total tokens count: 79174.00  
Median total tokens input uncached: 25476.00  
Median total tokens input cached: 50176.00  
Median total tokens input: 75652.00  
Median total tokens output: 3522.00  
Median tokens by sub-agent:  
Classification: 42443.00 (input uncached: 14587.00, input cached: 26112.00, input: 40699.00, output: 1744.00)  
Fix_TP: 29047.00 (input uncached: 7782.00, input cached: 19968.00, input: 27750.00, output: 1297.00)  
Fix_FP: 7684.00 (input uncached: 3107.00, input cached: 4096.00, input: 7203.00, output: 481.00)  
#### Tokens Cost

Total Cost: 22.539891 USD  
Cost by sub-agent:  
Classification: 9.586362 USD  
Fix_TP: 12.270604 USD  
Fix_FP: 0.682924 USD  
#### Average Tokens Cost

Average Total Cost: 0.030418 USD  
Average cost by sub-agent:  
Classification: 0.012937 USD  
Fix_TP: 0.016560 USD  
Fix_FP: 0.000922 USD  
#### Median Tokens Cost

Median Total Cost: 0.021065 USD  
Median cost by sub-agent:  
Classification: 0.011280 USD  
Fix_TP: 0.007420 USD  
Fix_FP: 0.002366 USD  
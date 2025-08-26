
# Experiment Analysis Results

## Overall stats

### Total rule violations

291
### Classification

TP: 191 (65.64%)  
FP: 100 (34.36%)  
Unclassified: 0 (0.00%)
### Plausible Fixes

Total plausible fixes: 282/291 (96.91%)  
TP plausible fixes: 182/191 (95.29%)  
FP plausible fixes: 100/100 (100.00%)  
#### Passed previous steps

Total compilation step passed: 288/291 (98.97%)  
TP compilation step passed: 188/191 (98.43%)  
FP compilation step passed: 100/100 (100.00%)  
Total SonarQube check step passed: 284/291 (97.59%)  
TP SonarQube check step passed: 184/191 (96.34%)  
FP SonarQube check step passed: 100/100 (100.00%)  
### Soundness of classification

Total sound classifications: 267/291 (91.75%)  
Sound TP classifications: 186/191 (97.38%)  
Sound FP classifications: 81/100 (81.00%)  
Precision: 0.97  
Recall: 0.91  
F1 Score: 0.94  
### Correctness of fix

Total correct fixes (sound and correct / sound and fixed): 251/259 (96.91%)  
Correct TP fixes  (sound and correct / sound and fixed): 170/178 (95.51%)  
Correct FP fixes  (sound and correct / sound and fixed): 81/81 (100.00%)  
### End-to-end performance (fixed, sound and correct)

End-to-end total: 251/291 (86.25%)  
End-to-end TP: 170/191 (89.01%)  
End-to-end FP: 81/100 (81.00%)  
### Fix Complexity

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
### Iterations

Total iterations: 5420  
Iterations by sub-agent:  
Classification: 3042  
Fix_TP: 2023  
Fix_FP: 355  

Mean iterations: 18.63  
Mean iterations by sub-agent:  
Classification: 10.45  
Fix_TP: 10.59  
Fix_FP: 3.55  

Median iterations: 20.00  
Median iterations by sub-agent:  
Classification: 10.00  
Fix_TP: 7.00  
Fix_FP: 3.00  
### Number of Plausible and Implausible Fixes created

Mean number of implausible fixes: 0.97  
Mean number of implausible fixes (TP): 1.40  
Mean number of implausible fixes (FP): 0.15  
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

Still plausible fixes / accepted fixes: 216 / 291 (74.23%)  
Still plausible fixes / accepted fixes (TP): 128 / 191 (67.02%)  
Still plausible fixes / accepted fixes (FP): 88 / 100 (88.00%)  
#### Only build step (no SonarQube check and test steps)

Still plausible fixes / accepted fixes: 251 / 288 (87.15%)  
Still plausible fixes / accepted fixes (TP): 156 / 188 (82.98%)  
Still plausible fixes / accepted fixes (FP): 95 / 100 (95.00%)  
#### Only build and SonarQube check steps (no test step)

Still plausible fixes / accepted fixes: 278 / 284 (97.89%)  
Still plausible fixes / accepted fixes (TP): 178 / 184 (96.74%)  
Still plausible fixes / accepted fixes (FP): 100 / 100 (100.00%)  
### Execution time

Total execution time: 1013.92 minutes  
Execution time by sub-agent:  
Classification: 418.54 minutes  
Fix_TP: 472.51 minutes  
Fix_FP: 122.86 minutes  

Mean execution time: 3.48 minutes  
Mean execution time by sub-agent:  
Classification: 1.44 minutes  
Fix_TP: 2.47 minutes  
Fix_FP: 1.23 minutes  

Median execution time: 2.9 minutes  
Median execution time by sub-agent:  
Classification: 1.27 minutes  
Fix_TP: 1.65 minutes  
Fix_FP: 1.04 minutes  
### Maven Build, Test, SonarQube Analysis Time and LLM Time

Total Maven Build Time: 248.73 minutes  
Total Maven Test Time: 145.69 minutes  
Total SonarQube Analysis Time: 36.27 minutes  
Total LLM Time: 398.4 minutes  

Mean Maven Build Time (per warning): 0.85 minutes  
Mean Maven Test Time (per warning): 0.5 minutes  
Mean SonarQube Analysis Time (per warning): 0.12 minutes  
Mean LLM Time (per warning): 1.37 minutes  

Median Maven Build Time (per warning): 0.58 minutes  
Median Maven Test Time (per warning): 0.3 minutes  
Median SonarQube Analysis Time (per warning): 0.1 minutes  
Median LLM Time (per warning): 1.02 minutes  

Total Time outside of CCA: 430.69 minutes  
Mean Time outside of CCA: 1.48 minutes  
Median Time outside of CCA: 1.08 minutes  

Percentage of Time outside of CCA: 42.48%  
Percentage of Time in LLM: 39.29%  
Percentage of Time executing tools and middleware (everything else): 18.23%  
#### Execution time in subparts for unfixed warnings only  

Mean Time outside of CCA for unfixed warnings: 2.84 minutes  
Mean Time in LLM for unfixed warnings: 6.16 minutes  
Percentage of Time outside of CCA for unfixed warnings: 24.56%  
Percentage of Time in LLM for unfixed warnings: 53.26%  
Percentage of Time executing tools and middleware (everything else) for unfixed warnings: 22.18%  
### Cost

#### Tokens Count

Total tokens count: 37353656  
Total tokens input uncached: 7999592  
Total tokens input cached: 28193536  
Total tokens input: 36193128  
Total tokens output: 1160528  
Tokens by sub-agent:  
Classification: 15976692 (input uncached: 4922842, input cached: 10467200, input: 15390042, output: 586650)  
Fix_TP: 20464035 (input uncached: 2727789, input cached: 17221376, input: 19949165, output: 514870)  
Fix_FP: 912929 (input uncached: 348961, input cached: 504960, input: 853921, output: 59008)  
#### Mean Tokens Count

Mean total tokens count: 128363.08  
Mean total tokens input uncached: 27490.01  
Mean total tokens input cached: 96885.00  
Mean total tokens input: 124375.01  
Mean total tokens output: 3988.07  
Mean tokens by sub-agent:  
Classification: 54902.72 (input uncached: 16916.98, input cached: 35969.76, input: 52886.74, output: 2015.98)  
Fix_TP: 107141.54 (input uncached: 14281.62, input cached: 90164.27, input: 104445.89, output: 2695.65)  
Fix_FP: 9129.29 (input uncached: 3489.61, input cached: 5049.60, input: 8539.21, output: 590.08)  
#### Median Tokens Count

Median total tokens count: 71995.00  
Median total tokens input uncached: 23105.00  
Median total tokens input cached: 46208.00  
Median total tokens input: 69045.00  
Median total tokens output: 2950.00  
Median tokens by sub-agent:  
Classification: 46078.00 (input uncached: 15172.00, input cached: 29056.00, input: 44228.00, output: 1850.00)  
Fix_TP: 29349.00 (input uncached: 7612.00, input cached: 20480.00, input: 28092.00, output: 1257.00)  
Fix_FP: 7218.00 (input uncached: 3154.00, input cached: 3584.00, input: 6738.00, output: 480.00)  
#### Tokens Cost

Total Cost: 7.876035 USD  
Cost by sub-agent:  
Classification: 3.954497 USD  
Fix_TP: 3.637045 USD  
Fix_FP: 0.284493 USD  
#### Average Tokens Cost

Average Total Cost: 0.027065 USD  
Average cost by sub-agent:  
Classification: 0.013589 USD  
Fix_TP: 0.019042 USD  
Fix_FP: 0.002845 USD  
#### Median Tokens Cost

Median Total Cost: 0.018717 USD  
Median cost by sub-agent:  
Classification: 0.012052 USD  
Fix_TP: 0.007179 USD  
Fix_FP: 0.002385 USD  
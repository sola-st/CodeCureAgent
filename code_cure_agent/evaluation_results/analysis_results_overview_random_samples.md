
# Experiment Analysis Results

## Overall stats

### Total rule violations

709
### Warning types

Code_Smell: 680 (95.91%)  
Bug: 21 (2.96%)  
Security_Hotspot: 7 (0.99%)  
Vulnerability: 1 (0.14%)  

### Classification

TP: 505 (71.23%)  
FP: 204 (28.77%)  
Unclassified: 0 (0.00%)
### Plausible Fixes

Total plausible fixes: 686/709 (96.76%)  
TP plausible fixes: 483/505 (95.64%)  
FP plausible fixes: 203/204 (99.51%)  

Plausible fixes per warning type:  
Code_Smell: 657/680 (96.62%)  
Bug: 21/21 (100.00%)  
Security_Hotspot: 7/7 (100.00%)  
Vulnerability: 1/1 (100.00%)  
#### Passed previous steps

Total compilation step passed: 696/709 (98.17%)  
TP compilation step passed: 492/505 (97.43%)  
FP compilation step passed: 204/204 (100.00%)  
Total SonarQube check step passed: 686/709 (96.76%)  
TP SonarQube check step passed: 483/505 (95.64%)  
FP SonarQube check step passed: 203/204 (99.51%)  
### Soundness of classification

Total sound classifications: 0/0 (0.00%)  
Sound TP classifications: 0/0 (0.00%)  
Sound FP classifications: 0/0 (0.00%)  
Precision: 0.00  
Recall: 0.00  
F1 Score: 0.00  
### Correctness of fix

Total correct fixes (sound and correct / sound and fixed): 0/0 (0.00%)  
Correct TP fixes  (sound and correct / sound and fixed): 0/0 (0.00%)  
Correct FP fixes  (sound and correct / sound and fixed): 0/0 (0.00%)  
### End-to-end performance (fixed, sound and correct)

End-to-end total: 0/0 (0.00%)  
End-to-end TP: 0/0 (0.00%)  
End-to-end FP: 0/0 (0.00%)  
### Fix Complexity

Single Line problems: 339 (47.81%)  
Multi Line problems: 343 (48.38%)  
Multi File problems: 27 (3.81%)  

Fix complexity split by type of fix:  
TP - Single Line: 145 (28.71%)  
TP - Multi Line: 333 (65.94%)  
TP - Multi File: 27 (5.35%)  
FP - Single Line: 194 (95.10%)  
FP - Multi Line: 10 (4.90%)  
FP - Multi File: 0 (0.00%)  

Number of plausible fixes created per fixComplexity:  
Single Line: 338 / 339 (99.71%)  
Multi Line: 326 / 343 (95.04%)  
Multi File: 22 / 27 (81.48%)  

Number of correct fixes created per fixComplexity (of the 0 sound and correct fixes (only for inspected samples)):  
Single Line: 0 (0.00%)  
Multi Line: 0 (0.00%)  
Multi File: 0 / 0 (0.00%)  
### Iterations

Total iterations: 13075  
Iterations by sub-agent:  
Classification: 6912  
Fix_TP: 5383  
Fix_FP: 780  

Mean iterations: 18.44  
Mean iterations by sub-agent:  
Classification: 9.75  
Fix_TP: 10.66  
Fix_FP: 3.82  

Median iterations: 19.00  
Median iterations by sub-agent:  
Classification: 9.00  
Fix_TP: 7.00  
Fix_FP: 3.00  
### Number of Plausible and Implausible Fixes created

Mean number of implausible fixes: 0.95  
Mean number of implausible fixes (TP): 1.21  
Mean number of implausible fixes (FP): 0.31  
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

Still plausible fixes / accepted fixes: 535 / 709 (75.46%)  
Still plausible fixes / accepted fixes (TP): 358 / 505 (70.89%)  
Still plausible fixes / accepted fixes (FP): 177 / 204 (86.76%)  
#### Only build step (no SonarQube check and test steps)

Still plausible fixes / accepted fixes: 610 / 696 (87.64%)  
Still plausible fixes / accepted fixes (TP): 426 / 492 (86.59%)  
Still plausible fixes / accepted fixes (FP): 184 / 204 (90.20%)  
#### Only build and SonarQube check steps (no test step)

Still plausible fixes / accepted fixes: 683 / 686 (99.56%)  
Still plausible fixes / accepted fixes (TP): 480 / 483 (99.38%)  
Still plausible fixes / accepted fixes (FP): 203 / 203 (100.00%)  
### Execution time

Total execution time: 2413.3 minutes  
Execution time by sub-agent:  
Classification: 940.85 minutes  
Fix_TP: 1235.62 minutes  
Fix_FP: 236.83 minutes  

Mean execution time: 3.4 minutes  
Mean execution time by sub-agent:  
Classification: 1.33 minutes  
Fix_TP: 2.45 minutes  
Fix_FP: 1.16 minutes  

Median execution time: 2.67 minutes  
Median execution time by sub-agent:  
Classification: 1.19 minutes  
Fix_TP: 1.6 minutes  
Fix_FP: 0.83 minutes  
### Maven Build, Test, SonarQube Analysis Time and LLM Time

Total Maven Build Time: 609.38 minutes  
Total Maven Test Time: 268.65 minutes  
Total SonarQube Analysis Time: 98.04 minutes  
Total LLM Time: 946.35 minutes  

Mean Maven Build Time (per warning): 0.86 minutes  
Mean Maven Test Time (per warning): 0.38 minutes  
Mean SonarQube Analysis Time (per warning): 0.14 minutes  
Mean LLM Time (per warning): 1.33 minutes  

Median Maven Build Time (per warning): 0.58 minutes  
Median Maven Test Time (per warning): 0.27 minutes  
Median SonarQube Analysis Time (per warning): 0.1 minutes  
Median LLM Time (per warning): 0.89 minutes  

Total Time outside of CCA: 976.07 minutes  
Mean Time outside of CCA: 1.38 minutes  
Median Time outside of CCA: 1.05 minutes  

Percentage of Time outside of CCA: 40.45%  
Percentage of Time in LLM: 39.21%  
Percentage of Time executing tools and middleware (everything else): 20.34%  
#### Execution time in subparts for unfixed warnings only  

Mean Time outside of CCA for unfixed warnings: 2.12 minutes  
Mean Time in LLM for unfixed warnings: 8.98 minutes  
Percentage of Time outside of CCA for unfixed warnings: 14.13%  
Percentage of Time in LLM for unfixed warnings: 59.86%  
Percentage of Time executing tools and middleware (everything else) for unfixed warnings: 26.01%  
### Cost

#### Tokens Count

Total tokens count: 102231267  
Total tokens input uncached: 20638472  
Total tokens input cached: 78517888  
Total tokens input: 99156360  
Total tokens output: 3074907  
Tokens by sub-agent:  
Classification: 34806941 (input uncached: 11310730, input cached: 22182400, input: 33493130, output: 1313811)  
Fix_TP: 64929915 (input uncached: 8599325, input cached: 54704896, input: 63304221, output: 1625694)  
Fix_FP: 2494411 (input uncached: 728417, input cached: 1630592, input: 2359009, output: 135402)  
#### Mean Tokens Count

Mean total tokens count: 144190.79  
Mean total tokens input uncached: 29109.27  
Mean total tokens input cached: 110744.55  
Mean total tokens input: 139853.82  
Mean total tokens output: 4336.96  
Mean tokens by sub-agent:  
Classification: 49093.01 (input uncached: 15953.07, input cached: 31286.88, input: 47239.96, output: 1853.05)  
Fix_TP: 128574.09 (input uncached: 17028.37, input cached: 108326.53, input: 125354.89, output: 3219.20)  
Fix_FP: 12227.50 (input uncached: 3570.67, input cached: 7993.10, input: 11563.77, output: 663.74)  
#### Median Tokens Count

Median total tokens count: 66355.00  
Median total tokens input uncached: 21553.00  
Median total tokens input cached: 42240.00  
Median total tokens input: 63537.00  
Median total tokens output: 2818.00  
Median tokens by sub-agent:  
Classification: 41341.00 (input uncached: 14408.00, input cached: 25216.00, input: 39624.00, output: 1717.00)  
Fix_TP: 29481.00 (input uncached: 8218.00, input cached: 19968.00, input: 28186.00, output: 1295.00)  
Fix_FP: 8038.50 (input uncached: 3075.00, input cached: 4480.00, input: 7555.00, output: 483.50)  
#### Tokens Cost

Total Cost: 21.027029 USD  
Cost by sub-agent:  
Classification: 8.844630 USD  
Fix_TP: 11.511330 USD  
Fix_FP: 0.671069 USD  
#### Average Tokens Cost

Average Total Cost: 0.029657 USD  
Average cost by sub-agent:  
Classification: 0.012475 USD  
Fix_TP: 0.022795 USD  
Fix_FP: 0.003290 USD  
#### Median Tokens Cost

Median Total Cost: 0.017431 USD  
Median cost by sub-agent:  
Classification: 0.011041 USD  
Fix_TP: 0.007448 USD  
Fix_FP: 0.002338 USD  
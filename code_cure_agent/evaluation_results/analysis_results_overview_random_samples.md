
# Experiment Analysis Results

## Overall stats

### Total rule violations

450
### Classification

TP: 323 (71.78%)  
FP: 127 (28.22%)  
Unclassified: 0 (0.00%)
### Plausible Fixes

Total plausible fixes: 432/450 (96.00%)  
TP plausible fixes: 305/323 (94.43%)  
FP plausible fixes: 127/127 (100.00%)  
#### Passed previous steps

Total compilation step passed: 439/450 (97.56%)  
TP compilation step passed: 312/323 (96.59%)  
FP compilation step passed: 127/127 (100.00%)  
Total SonarQube check step passed: 433/450 (96.22%)  
TP SonarQube check step passed: 306/323 (94.74%)  
FP SonarQube check step passed: 127/127 (100.00%)  
### Soundness of classification

Total sound classifications: 0/0 (0.00%)  
Sound TP classifications: 0/0 (0.00%)  
Sound FP classifications: 0/0 (0.00%)  
### Correctness of fix

Total correct fixes (sound and correct / sound and fixed): 0/0 (0.00%)  
Correct TP fixes  (sound and correct / sound and fixed): 0/0 (0.00%)  
Correct FP fixes  (sound and correct / sound and fixed): 0/0 (0.00%)  
### End-to-end performance (fixed, sound and correct)

End-to-end total: 0/0 (0.00%)  
End-to-end TP: 0/0 (0.00%)  
End-to-end FP: 0/0 (0.00%)  
### Fix Complexity

Single Line problems: 214 (47.56%)  
Multi Line problems: 218 (48.44%)  
Multi File problems: 18 (4.00%)  

Fix complexity split by type of fix:  
TP - Single Line: 92 (28.48%)  
TP - Multi Line: 213 (65.94%)  
TP - Multi File: 18 (5.57%)  
FP - Single Line: 122 (96.06%)  
FP - Multi Line: 5 (3.94%)  
FP - Multi File: 0 (0.00%)  

Number of plausible fixes created per fixComplexity:  
Single Line: 214 / 214 (100.00%)  
Multi Line: 204 / 218 (93.58%)  
Multi File: 14 / 18 (77.78%)  

Number of correct fixes created per fixComplexity (of the 0 sound and correct fixes (only for inspected samples)):  
Single Line: 0 (0.00%)  
Multi Line: 0 (0.00%)  
Multi File: 0 / 0 (0.00%)  
### Iterations

Total iterations: 8139  
Iterations by sub-agent:  
Classification: 4405  
Fix_TP: 3579  
Fix_FP: 155  

Mean iterations: 18.09  
Mean iterations by sub-agent:  
Classification: 9.79  
Fix_TP: 7.95  
Fix_FP: 0.34  

Median iterations: 19.00  
Median iterations by sub-agent:  
Classification: 9.00  
Fix_TP: 7.00  
Fix_FP: 3.00  
### Number of Plausible and Implausible Fixes created

Mean number of implausible fixes: 1.05  
Mean number of implausible fixes (TP): 1.38  
Mean number of implausible fixes (FP): 0.21  
Mean number of plausible fixes: 0.96  
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

Still plausible fixes / accepted fixes: 340 / 450 (75.56%)  
Still plausible fixes / accepted fixes (TP): 228 / 323 (70.59%)  
Still plausible fixes / accepted fixes (FP): 112 / 127 (88.19%)  
#### Only build step (no SonarQube check and test steps)

Still plausible fixes / accepted fixes: 386 / 439 (87.93%)  
Still plausible fixes / accepted fixes (TP): 269 / 312 (86.22%)  
Still plausible fixes / accepted fixes (FP): 117 / 127 (92.13%)  
#### Only build and SonarQube check steps (no test step)

Still plausible fixes / accepted fixes: 431 / 433 (99.54%)  
Still plausible fixes / accepted fixes (TP): 304 / 306 (87.91%)  
Still plausible fixes / accepted fixes (FP): 127 / 127 (92.13%)  
### Execution time

Total execution time: 1560.57 minutes  
Execution time by sub-agent:  
Classification: 580.01 minutes  
Fix_TP: 846.01 minutes  
Fix_FP: 134.54 minutes  

Mean execution time: 3.47 minutes  
Mean execution time by sub-agent:  
Classification: 1.29 minutes  
Fix_TP: 2.62 minutes  
Fix_FP: 1.06 minutes  

Median execution time: 2.7 minutes  
Median execution time by sub-agent:  
Classification: 1.15 minutes  
Fix_TP: 1.63 minutes  
Fix_FP: 0.82 minutes  
### Cost

#### Tokens Count

Total tokens count: 73367522  
Total tokens input uncached: 13898203  
Total tokens input cached: 57364224  
Total tokens input: 71262427  
Total tokens output: 2105095  
Tokens by sub-agent:  
Classification: 22167745 (input uncached: 7206812, input cached: 14125568, input: 21332380, output: 835365)  
Fix_TP: 49772260 (input uncached: 6239261, input cached: 42343296, input: 48582557, output: 1189703)  
Fix_FP: 1427517 (input uncached: 452130, input cached: 895360, input: 1347490, output: 80027)  
#### Mean Tokens Count

Mean total tokens count: 163038.94  
Mean total tokens input uncached: 30884.90  
Mean total tokens input cached: 127476.05  
Mean total tokens input: 158360.95  
Mean total tokens output: 4677.99  
Mean tokens by sub-agent:  
Classification: 49261.66 (input uncached: 16015.14, input cached: 31390.15, input: 47405.29, output: 1856.37)  
Fix_TP: 110605.02 (input uncached: 13865.02, input cached: 94096.21, input: 107961.24, output: 2643.78)  
Fix_FP: 3172.26 (input uncached: 1004.73, input cached: 1989.69, input: 2994.42, output: 177.84)  
#### Median Tokens Count

Median total tokens count: 78338.00  
Median total tokens input uncached: 25443.00  
Median total tokens input cached: 49408.00  
Median total tokens input: 74851.00  
Median total tokens output: 3487.00  
Median tokens by sub-agent:  
Classification: 40828.00 (input uncached: 14304.00, input cached: 24832.00, input: 39136.00, output: 1692.00)  
Fix_TP: 29346.00 (input uncached: 8068.00, input cached: 19968.00, input: 28036.00, output: 1310.00)  
Fix_FP: 8164.00 (input uncached: 3071.00, input cached: 4608.00, input: 7679.00, output: 485.00)  
#### Tokens Cost

Total Cost: 14.663856 USD  
Cost by sub-agent:  
Classification: 5.631866 USD  
Fix_TP: 8.633559 USD  
Fix_FP: 0.398431 USD  
#### Average Tokens Cost

Average Total Cost: 0.032586 USD  
Average cost by sub-agent:  
Classification: 0.012515 USD  
Fix_TP: 0.019186 USD  
Fix_FP: 0.000885 USD  
#### Median Tokens Cost

Median Total Cost: 0.020780 USD  
Median cost by sub-agent:  
Classification: 0.010878 USD  
Fix_TP: 0.007549 USD  
Fix_FP: 0.002353 USD  

# Experiment Analysis Results

## Overall stats

### Total rule violations

291
### Classification

TP: 191  
FP: 100  
Unclassified: 0
### Plausible Fixes

Total plausible fixes: 282/291  
TP plausible fixes: 182/191  
FP plausible fixes: 100/100  
#### Passed previous steps

Total compilation step passed: 288/291  
TP compilation step passed: 188/191  
FP compilation step passed: 100/100  
Total SonarQube check step passed: 284/291  
TP SonarQube check step passed: 184/191  
FP SonarQube check step passed: 100/100  
### Soundness of classification

Total sound classifications: 204/220  
Sound TP classifications: 145/149  
Sound FP classifications: 59/71  
### Correctness of fix

Total correct fixes (sound and correct / sound and fixed): 190/198  
Correct TP fixes  (sound and correct / sound and fixed): 131/139  
Correct FP fixes  (sound and correct / sound and fixed): 59/59  
### Fix Complexity

Single Line problems: 181  
Multi Line problems: 99  
Multi File problems: 9  

Fix complexity split by type of fix:  
TP - Single Line: 86  
TP - Multi Line: 94  
TP - Multi File: 9  
FP - Single Line: 95  
FP - Multi Line: 5  
FP - Multi File: 0  

Number of plausible fixes created per fixComplexity:  
Single Line: 179 / 181  
Multi Line: 98 / 99  
Multi File: 5 / 9  

Number of correct fixes created per fixComplexity (of the 190 sound and correct fixes (only for inspected samples)):  
Single Line: 117  
Multi Line: 70  
Multi File: 3  
### Iterations

Total iterations: 5129  
Iterations by sub-agent:  
Classification: 3042  
Fix_TP: 1923  
Fix_FP: 164  

Mean iterations: 17.63  
Mean iterations by sub-agent:  
Classification: 10.45  
Fix_TP: 6.61  
Fix_FP: 0.56  

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

#### No ChangeApprover

Still plausible fixes / plausible fixes: 216 / 282  
Still plausible fixes / plausible fixes (TP): 128 / 182  
Still plausible fixes / plausible fixes (FP): 88 / 100  
#### Only build step (no SonarQube check and test steps)

Still plausible fixes / plausible fixes: 251 / 282  
Still plausible fixes / plausible fixes (TP): 156 / 182  
Still plausible fixes / plausible fixes (FP): 95 / 100  
#### Only build and SonarQube check steps (no test step)

Still plausible fixes / plausible fixes: 278 / 282  
Still plausible fixes / plausible fixes (TP): 178 / 182  
Still plausible fixes / plausible fixes (FP): 100 / 100  
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
Fix_TP: 70323.14 (input uncached: 9373.85, input cached: 59179.99, input: 68553.83, output: 1769.31)  
Fix_FP: 3137.21 (input uncached: 1199.18, input cached: 1735.26, input: 2934.44, output: 202.78)  
#### Median Tokens Count

Median total tokens count: 82645.00  
Median total tokens input uncached: 25938.00  
Median total tokens input cached: 53120.00  
Median total tokens input: 79058.00  
Median total tokens output: 3587.00  
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
Fix_TP: 0.012498 USD  
Fix_FP: 0.000978 USD  
#### Median Tokens Cost

Median Total Cost: 0.021616 USD  
Median cost by sub-agent:  
Classification: 0.012052 USD  
Fix_TP: 0.007179 USD  
Fix_FP: 0.002385 USD  
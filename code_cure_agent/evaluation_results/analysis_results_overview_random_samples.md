
# Experiment Analysis Results

## Overall stats

### Total rule violations

109
### Classification

TP: 91  
FP: 18  
Unclassified: 0
### Plausible Fixes

Total plausible fixes: 105/109  
TP plausible fixes: 87/91  
FP plausible fixes: 18/18  
#### Passed previous steps

Total compilation step passed: 107/109  
TP compilation step passed: 89/91  
FP compilation step passed: 18/18  
Total SonarQube check step passed: 105/109  
TP SonarQube check step passed: 87/91  
FP SonarQube check step passed: 18/18  
### Soundness of classification

Total sound classifications: 0/0  
Sound TP classifications: 0/0  
Sound FP classifications: 0/0  
### Correctness of fix

Total correct fixes (sound and correct / sound and fixed): 0/0  
Correct TP fixes  (sound and correct / sound and fixed): 0/0  
Correct FP fixes  (sound and correct / sound and fixed): 0/0  
### Fix Complexity

Single Line problems: 51  
Multi Line problems: 51  
Multi File problems: 7  

Fix complexity split by type of fix:  
TP - Single Line: 33  
TP - Multi Line: 51  
TP - Multi File: 7  
FP - Single Line: 18  
FP - Multi Line: 0  
FP - Multi File: 0  

Number of plausible fixes created per fixComplexity:  
Single Line: 51 / 51  
Multi Line: 49 / 51  
Multi File: 5 / 7  

Number of correct fixes created per fixComplexity (of the 0 sound and correct fixes (only for inspected samples)):  
Single Line: 0  
Multi Line: 0  
Multi File: 0  
### Iterations

Total iterations: 2082  
Iterations by sub-agent:  
Classification: 1059  
Fix_TP: 1050  
Fix_FP: -27  

Mean iterations: 19.10  
Mean iterations by sub-agent:  
Classification: 9.72  
Fix_TP: 9.63  
Fix_FP: -0.25  

Median iterations: 19.00  
Median iterations by sub-agent:  
Classification: 9.00  
Fix_TP: 7.00  
Fix_FP: 3.00  
### Number of Plausible and Implausible Fixes created

Mean number of implausible fixes: 0.88  
Mean number of implausible fixes (TP): 1.04  
Mean number of implausible fixes (FP): 0.06  
Mean number of plausible fixes: 0.96  
Mean number of plausible fixes (TP): 0.96  
Mean number of plausible fixes (FP): 1.00  

Median number of implausible fixes: 0.00  
Median number of implausible fixes (TP): 0.00  
Median number of implausible fixes (FP): 0.00  
Median number of plausible fixes: 1.00  
Median number of plausible fixes (TP): 1.00  
Median number of plausible fixes (FP): 1.00  
### Ablation of the ChangeApprover

#### No ChangeApprover

Still plausible fixes / plausible fixes: 86 / 105  
Still plausible fixes / plausible fixes (TP): 69 / 87  
Still plausible fixes / plausible fixes (FP): 17 / 18  
#### Only build step (no SonarQube check and test steps)

Still plausible fixes / plausible fixes: 95 / 105  
Still plausible fixes / plausible fixes (TP): 78 / 87  
Still plausible fixes / plausible fixes (FP): 17 / 18  
#### Only build and SonarQube check steps (no test step)

Still plausible fixes / plausible fixes: 104 / 105  
Still plausible fixes / plausible fixes (TP): 86 / 87  
Still plausible fixes / plausible fixes (FP): 18 / 18  
### Execution time

Total execution time: 354.92 minutes  
Execution time by sub-agent:  
Classification: 132.72 minutes  
Fix_TP: 207.5 minutes  
Fix_FP: 14.7 minutes  

Mean execution time: 3.26 minutes  
Mean execution time by sub-agent:  
Classification: 1.22 minutes  
Fix_TP: 2.28 minutes  
Fix_FP: 0.82 minutes  

Median execution time: 2.62 minutes  
Median execution time by sub-agent:  
Classification: 1.05 minutes  
Fix_TP: 1.72 minutes  
Fix_FP: 0.7 minutes  
### Cost

#### Tokens Count

Total tokens count: 17944232  
Total tokens input uncached: 3284251  
Total tokens input cached: 14167296  
Total tokens input: 17451547  
Total tokens output: 492685  
Tokens by sub-agent:  
Classification: 5264898 (input uncached: 1668567, input cached: 3396480, input: 5065047, output: 199851)  
Fix_TP: 12518747 (input uncached: 1553532, input cached: 10682752, input: 12236284, output: 282463)  
Fix_FP: 160587 (input uncached: 62152, input cached: 88064, input: 150216, output: 10371)  
#### Mean Tokens Count

Mean total tokens count: 164625.98  
Mean total tokens input uncached: 30130.74  
Mean total tokens input cached: 129975.19  
Mean total tokens input: 160105.94  
Mean total tokens output: 4520.05  
Mean tokens by sub-agent:  
Classification: 48301.82 (input uncached: 15307.95, input cached: 31160.37, input: 46468.32, output: 1833.50)  
Fix_TP: 114850.89 (input uncached: 14252.59, input cached: 98006.90, input: 112259.49, output: 2591.40)  
Fix_FP: 1473.28 (input uncached: 570.20, input cached: 807.93, input: 1378.13, output: 95.15)  
#### Median Tokens Count

Median total tokens count: 76309.50  
Median total tokens input uncached: 24032.50  
Median total tokens input cached: 48896.00  
Median total tokens input: 72928.50  
Median total tokens output: 3381.00  
Median tokens by sub-agent:  
Classification: 41454.00 (input uncached: 13777.00, input cached: 25984.00, input: 39761.00, output: 1693.00)  
Fix_TP: 27766.00 (input uncached: 7109.00, input cached: 19456.00, input: 26565.00, output: 1201.00)  
Fix_FP: 7089.50 (input uncached: 3146.50, input cached: 3456.00, input: 6602.50, output: 487.00)  
#### Tokens Cost

Total Cost: 3.518726 USD  
Cost by sub-agent:  
Classification: 1.326836 USD  
Fix_TP: 2.141629 USD  
Fix_FP: 0.050261 USD  
#### Average Tokens Cost

Average Total Cost: 0.032282 USD  
Average cost by sub-agent:  
Classification: 0.012173 USD  
Fix_TP: 0.019648 USD  
Fix_FP: 0.000461 USD  
#### Median Tokens Cost

Median Total Cost: 0.020126 USD  
Median cost by sub-agent:  
Classification: 0.010940 USD  
Fix_TP: 0.006819 USD  
Fix_FP: 0.002367 USD  
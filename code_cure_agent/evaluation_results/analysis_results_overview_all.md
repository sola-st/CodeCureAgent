
# Experiment Analysis Results

## Overall stats

### Total rule violations

400
### Classification

TP: 282  
FP: 118  
Unclassified: 0
### Plausible Fixes

Total plausible fixes: 387/400  
TP plausible fixes: 269/282  
FP plausible fixes: 118/118  
#### Passed previous steps

Total compilation step passed: 395/400  
TP compilation step passed: 277/282  
FP compilation step passed: 118/118  
Total SonarQube check step passed: 389/400  
TP SonarQube check step passed: 271/282  
FP SonarQube check step passed: 118/118  
### Soundness of classification

Total sound classifications: 204/220  
Sound TP classifications: 145/149  
Sound FP classifications: 59/71  
### Correctness of fix

Total correct fixes (sound and correct / sound and fixed): 190/198  
Correct TP fixes  (sound and correct / sound and fixed): 131/139  
Correct FP fixes  (sound and correct / sound and fixed): 59/59  
### Fix Complexity

Single Line problems: 232  
Multi Line problems: 150  
Multi File problems: 16  

Fix complexity split by type of fix:  
TP - Single Line: 119  
TP - Multi Line: 145  
TP - Multi File: 16  
FP - Single Line: 113  
FP - Multi Line: 5  
FP - Multi File: 0  

Number of plausible fixes created per fixComplexity:  
Single Line: 230 / 232  
Multi Line: 147 / 150  
Multi File: 10 / 16  

Number of correct fixes created per fixComplexity (of the 190 sound and correct fixes (only for inspected samples)):  
Single Line: 117  
Multi Line: 70  
Multi File: 3  
### Iterations

Total iterations: 7211  
Iterations by sub-agent:  
Classification: 4101  
Fix_TP: 2973  
Fix_FP: 137  

Mean iterations: 18.03  
Mean iterations by sub-agent:  
Classification: 10.25  
Fix_TP: 7.43  
Fix_FP: 0.34  

Median iterations: 19.00  
Median iterations by sub-agent:  
Classification: 9.00  
Fix_TP: 7.00  
Fix_FP: 3.00  
### Number of Plausible and Implausible Fixes created

Mean number of implausible fixes: 0.94  
Mean number of implausible fixes (TP): 1.28  
Mean number of implausible fixes (FP): 0.14  
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

Still plausible fixes / accepted fixes: 302 / 400  
Still plausible fixes / accepted fixes (TP): 197 / 282  
Still plausible fixes / accepted fixes (FP): 105 / 118  
#### Only build step (no SonarQube check and test steps)

Still plausible fixes / accepted fixes: 346 / 395  
Still plausible fixes / accepted fixes (TP): 234 / 277  
Still plausible fixes / accepted fixes (FP): 112 / 118  
#### Only build and SonarQube check steps (no test step)

Still plausible fixes / accepted fixes: 382 / 389  
Still plausible fixes / accepted fixes (TP): 264 / 271  
Still plausible fixes / accepted fixes (FP): 118 / 118  
### Execution time

Total execution time: 1368.84 minutes  
Execution time by sub-agent:  
Classification: 551.26 minutes  
Fix_TP: 680.02 minutes  
Fix_FP: 137.57 minutes  

Mean execution time: 3.42 minutes  
Mean execution time by sub-agent:  
Classification: 1.38 minutes  
Fix_TP: 2.41 minutes  
Fix_FP: 1.17 minutes  

Median execution time: 2.82 minutes  
Median execution time by sub-agent:  
Classification: 1.23 minutes  
Fix_TP: 1.66 minutes  
Fix_FP: 0.98 minutes  
### Cost

#### Tokens Count

Total tokens count: 55297888  
Total tokens input uncached: 11283843  
Total tokens input cached: 42360832  
Total tokens input: 53644675  
Total tokens output: 1653213  
Tokens by sub-agent:  
Classification: 21241590 (input uncached: 6591409, input cached: 13863680, input: 20455089, output: 786501)  
Fix_TP: 32982782 (input uncached: 4281321, input cached: 27904128, input: 32185449, output: 797333)  
Fix_FP: 1073516 (input uncached: 411113, input cached: 593024, input: 1004137, output: 69379)  
#### Mean Tokens Count

Mean total tokens count: 138244.72  
Mean total tokens input uncached: 28209.61  
Mean total tokens input cached: 105902.08  
Mean total tokens input: 134111.69  
Mean total tokens output: 4133.03  
Mean tokens by sub-agent:  
Classification: 53103.97 (input uncached: 16478.52, input cached: 34659.20, input: 51137.72, output: 1966.25)  
Fix_TP: 82456.96 (input uncached: 10703.30, input cached: 69760.32, input: 80463.62, output: 1993.33)  
Fix_FP: 2683.79 (input uncached: 1027.78, input cached: 1482.56, input: 2510.34, output: 173.45)  
#### Median Tokens Count

Median total tokens count: 80997.00  
Median total tokens input uncached: 25559.50  
Median total tokens input cached: 51904.00  
Median total tokens input: 77463.50  
Median total tokens output: 3533.50  
Median tokens by sub-agent:  
Classification: 45156.00 (input uncached: 14877.00, input cached: 28480.00, input: 43357.00, output: 1799.00)  
Fix_TP: 28622.50 (input uncached: 7528.50, input cached: 19840.00, input: 27368.50, output: 1254.00)  
Fix_FP: 7218.50 (input uncached: 3154.00, input cached: 3584.00, input: 6738.00, output: 480.50)  
#### Tokens Cost

Total Cost: 11.394761 USD  
Cost by sub-agent:  
Classification: 5.281333 USD  
Fix_TP: 5.778674 USD  
Fix_FP: 0.334754 USD  
#### Average Tokens Cost

Average Total Cost: 0.028487 USD  
Average cost by sub-agent:  
Classification: 0.013203 USD  
Fix_TP: 0.014447 USD  
Fix_FP: 0.000837 USD  
#### Median Tokens Cost

Median Total Cost: 0.020927 USD  
Median cost by sub-agent:  
Classification: 0.011631 USD  
Fix_TP: 0.006920 USD  
Fix_FP: 0.002377 USD  
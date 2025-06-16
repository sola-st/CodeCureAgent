# Stats fourth run with the 15 selected rule violations from dev_dataset with classification

## Overall stats

### Time

Execution time: 33 min
33 min / 15 = 2.2 min per rule violation 

### Cost (approximatly)

ca. 0.38 dollar with GPT 4.1 mini  
0.38 / 15 = 0.025 -> 2.5 cent per violation  
1527447 input tokens (41.5 million)  
55128 output tokens  (55 k)  

### Classification

TP: 6/15  
FP: 9/15  

5/6 TP are clearly a TP  
ID 406 of the TPs is unclear. I fail to see how the NullPointerException might occur, but I'm also not certain it cannot.  
=> 5/6 clearly sound classifications

7/9 of the FP are clearly a FP (the argumentation is sound, matches manual classification)  
One more FP that is sound eventhough I had classified it as TP (ID 268).  
ID 362 I had also classified as TP, but the reasoning makes sense. (both might be acceptable)  
=> 9/9 sound classifications

**14/15 classifications deemed sound, possibly even 15/15**

But very high number of samples classified as FPs here

### Patches

Created any patch: 15/15  

Created implausible patch: 4/15  
Created plausible patch: 14/15

TP implausible patches: 3/6  
TP plausible patches: 5/6  

FP implausible patches: 1/9  
FP plausible patches: 9/9

## Errors  

Multiple failing lookups (references/definitions) with wrong line numbers (symbol not found at the line). Sometimes line is wrong by one.  
Inspect more, why this happens.

## Correct fixes?

TP fixes:

ID 8:  
Correct fix

ID 10:  
Correct fix. Very nice refactoring.

ID 179:  
No plausible fix.
Got very close in the 40th cycle, avoiding the NullPointerException, but had forgotten to delete one line.  
Lots of kind of unnecessary exploration.

ID 404:  
Correct fix.

ID 406:  
Correct fix.

ID 1470:  
Correct fix.


FP fixes:

ID 28:  
Correct fix

ID 198:  
Correct fix

ID 205:  
Correct fix

ID 268:  
Correct fix  
Interestingly here it first did an implausible fix, adding NOSONAR to the wrong linenumbers. It did not recognize the inline comment in the actual line and therefore thought the warning arises due to some other previous lines.  
In the second attempt it then used @SuppressWarnings on the class level which is not ideal.

ID 362:  
Correct fix

ID 449:  
Correct fix

ID 542:  
Correct fix

ID 817:  
Correct fix

ID 2030:  
Correct fix

## Summary of correctness

**Total correct fixes:**  
5/6 TPs (5/5 of the plausible fixes)  
9/9 FPs   

=> In total 14/15

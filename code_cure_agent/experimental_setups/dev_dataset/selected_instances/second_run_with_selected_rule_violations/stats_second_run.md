# Stats second run with the 15 selected rule violations from dev_dataset

## Overall stats

### Time

Execution time: 2025-06-03 20:16:13 - 2025-06-03 21:09:01
=> 52 min and 48 seconds  
53 min / 15 = 3.53 min per rule violation (3 minutes and 30 seconds)  

### Cost (approximatly)

ca. 0.78 dollar with GPT 4.1 mini  
0.78 / 15 = 0.052 -> 5 cent per violation  
4409325 input tokens (4.4 million)  
88363 output tokens  (88 k)  

### Patches

Created any patch: 15/15  
Created implausible patch: 11/15  
Created plausible patch: 10/15   (3 less then in first run)

### Cycle counts

(how many cylces did it run for)

Plausible patch generated:  
14 (ID 8)  
24 (ID 28)
10 (ID 198)  
9 (ID 268)  
7 (ID 362)  
9 (ID 404)  
12 (ID 406)  
38 (ID 449)  
7 (ID 542)  
12 (ID 1470)  

Average: 14.2 cycles  (small decrease to before)

No plausible patch:  
40 (ID 10)  
40 (ID 179)  
40 (ID 205)  
40 (ID 817)
40 (ID 2030)

Average: 40 cycles  

### Proposed fixes count

With plausible patch found:  
1 (ID 8)  
3 (ID 28)
2 (ID 198)  
2 (ID 268)  
1 (ID 362)  
1 (ID 404)  
2 (ID 406)  (interestingly it created two plausible fixes before calling goals_accomplished)
5 (ID 449)  
1 (ID 542)  
3 (ID 1470)  

Average 2.1 write_fix attempts needed  (vs. 3.8 in the first run)

Without plausible patch found:  
2 (ID 10)  (The agent got lost in using read_range and find_references etc. instead of trying better write_fixes)
5 (ID 179)  
8 (ID 205)  
3 (ID 817) (Also way to few attempts made)
8 (ID 2030)  

Average 5.2 write_fix attempts made  (vs. 6.5 in first run)
=> Indicater that agent is lost in exploring and can't figure out its problems in write_fix attempts (wants to try the same once again)

## Errors  

Parsing error: 
Agent did not correctly follow the json-format once.

find_references and find_definition:
Regularly misused by the agent, outside of the intended functionality.  
The description of the commands must be much clearer to prevent misusage. Maybe only allow method and class lookup for clearity.  
Used multiple times with the same arguments.
Feedback when no reference/definition was found is unclear (doesn't differentiate non found and failure in searching them)
=> fallback where we search for the method/class naively?

read_sonarqube_docu:
Agent once tried to use it to read the Java docs of "java.util.logging.Logger"


## Correct fixes? Tests pass?

ID 8:
Correct fix
Tests pass (covered)

ID 28:  
Correct fix (treated as a TP (eventhoug it is more of a FP), it should still behave correctly however (eventhough not precisely semantically equivalent))
Tests pass (Not directly covered (class is tested but not the method))

This was just suppressed in the first run (but only due to not being able to solve it)

ID 198:  
Incorrect: Should have been a False Positive, as it is a test. The valueOf() method is needed internally when calling at.convert(). This changes the semantics and doesn't test the expected thing anymore.
Test Passes (The test doesn't recognized the incorrect change) (Covered (because it is code in a test case itself))  

ID 268:  
Correct fix (easy)
Tests pass (covered)

ID 362:  
Incorrect: Returning an empty byte array instead of null would need changes in 2 other files (3 references total).  
The agent called find_references but it returned an empty result (maybe need to recreate the language server each time)  
Therefore, it changed it without updating the references.  
Tests pass (not covered)

ID 404:  
Correct fix
Tests pass (not covered)

ID 406:   
Correct fix
Tests pass (not covered)

ID 449:  
Incorrect fix: This is a FP. The fix doesn't test what it should anymore. (Arithmetic Exception not escalated anymore). The FP is not recognized, but this could be due to supressing warnings being strongly discouraged in the prompt.  
Tests pass (Not covered (It is code in a test case but the test case is not added to the test suite))

ID 542:   
Incorrect fix: Didn't recognize that it is a FP. Just deleted the line with the assertion. This is wrong, as it now doesn't test this corner case anymore.
Tests pass (covered) 

ID 1470:  
Correct fix: Nicely solves the problem without any semantic changes
Tests pass (not covered)


## Summary of correctness

Total correct fixes: 6/15  
Non-suppressing correct fixes: 6/15  

Comparison to first run:  
Less correct fixes in total, but one more correct fix without trivial suppression of warnings.  

There were tries where the very challenging IDs 10 and 362 were correctly solved (not in this evaluation though).  
It doesn't get it to work every time though. Sometimes it gets on the wrong track and won't recover from that.


### Correct fixes by TP/FP/Might fix

TP:  
3/5 (1 incorrect fix, 1 unfixed)  

Might fix:  
2/4  

FP:  
1/6 (Did not recognize as FPs. Once the change it made was ok and something one can accept. In all other cases it was changing some semantics.)  

=> It behaves especially bad for False Positives as it doesn't recognize them as such (as this is not part of the task)


### Correct fixes by difficulty

hard:  
3/6  

medium:  
2/8  

easy:  
1/1  



**A conditionally executed single line should be denoted by indentation**  

In the absence of enclosing curly braces, the line immediately after a conditional is the one that is conditionally executed. By both convention and good practice, such lines are indented. In the absence of both curly braces and indentation the intent of the original programmer is entirely unclear and perhaps not actually what is executed. Additionally, such code is highly likely to be confusing to maintainers.

Noncompliant Code Example
    
    
    if (condition)  // Noncompliant
    doTheThing();
    
    doTheOtherThing();
    somethingElseEntirely();
    
    foo();
    

Compliant Solution
    
    
    if (condition)
      doTheThing();
    
    doTheOtherThing();
    somethingElseEntirely();
    
    foo();
    
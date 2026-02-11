**Loops should not contain more than a single "break" or "continue" statement**  

Restricting the number of `break` and `continue` statements in a loop is done in the interest of good structured programming.

Only one `break` or one `continue` statement is acceptable in a loop, since it facilitates optimal coding. If there is more than one, the code should be refactored to increase readability.

Noncompliant Code Example
    
    
    for (int i = 1; i <= 10; i++) {     // Noncompliant - 2 continue - one might be tempted to add some logic in between
      if (i % 2 == 0) {
        continue;
      }
    
      if (i % 3 == 0) {
        continue;
      }
    
      System.out.println("i = " + i);
    }
    
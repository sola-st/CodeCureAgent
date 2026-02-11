**Nested code blocks should not be used**  

Nested code blocks can be used to create a new scope and restrict the visibility of the variables defined inside it. Using this feature in a method typically indicates that the method has too many responsibilities, and should be refactored into smaller methods.

Noncompliant Code Example
    
    
    public void evaluate(int operator) {
      // Do some computation...
      {
        int a = stack.pop();
        int b = stack.pop();
        int result = a + b;
        stack.push(result);
      }
    }
    

Compliant Solution
    
    
    public void evaluate(int operator) {
      // Do some computation...
      evaluateAdd();
    }
    
    private void evaluateAdd() {
      int a = stack.pop();
      int b = stack.pop();
      int result = a + b;
      stack.push(result);
    }
    
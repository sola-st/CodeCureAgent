**Map values should not be replaced unconditionally**  

It is highly suspicious when a value is saved for a key or index and then unconditionally overwritten. Such replacements are likely errors.

Noncompliant Code Example
    
    
    letters.put("a", "Apple");
    letters.put("a", "Boy");  // Noncompliant
    
    towns[i] = "London";
    towns[i] = "Chicago";  // Noncompliant
    
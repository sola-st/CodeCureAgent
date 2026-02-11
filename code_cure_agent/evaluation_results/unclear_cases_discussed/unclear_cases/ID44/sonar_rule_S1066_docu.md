**Collapsible "if" statements should be merged**  

Merging collapsible `if` statements increases the code’s readability.

Noncompliant Code Example
    
    
    if (file != null) {
      if (file.isFile() || file.isDirectory()) {
        /* ... */
      }
    }
    

Compliant Solution
    
    
    if (file != null && isFileOrDirectory(file)) {
      /* ... */
    }
    
    private static boolean isFileOrDirectory(File file) {
      return file.isFile() || file.isDirectory();
    }
    
**Insecure temporary file creation methods should not be used**  

Using `File.createTempFile` as the first step in creating a temporary directory causes a race condition and is inherently unreliable and insecure. Instead, `Files.createTempDirectory` (Java 7+) should be used.

This rule raises an issue when the following steps are taken in immediate sequence:

  * call to `File.createTempFile`
  * delete resulting file 
  * call `mkdir` on the File object 



**Note** that this rule is automatically disabled when the project’s `sonar.java.source` is lower than `7`.

Noncompliant Code Example
    
    
    File tempDir;
    tempDir = File.createTempFile("", ".");
    tempDir.delete();
    tempDir.mkdir();  // Noncompliant
    

Compliant Solution
    
    
    Path tempPath = Files.createTempDirectory("");
    File tempDir = tempPath.toFile();
    

See

  * [OWASP Top 10 2021 Category A1](https://owasp.org/Top10/A01_2021-Broken_Access_Control/) \- Broken Access Control 
  * [OWASP Top 10 2017 Category A9](https://owasp.org/www-project-top-ten/2017/A9_2017-Using_Components_with_Known_Vulnerabilities) \- Using Components with Known Vulnerabilities 
  * [MITRE, CWE-377](https://cwe.mitre.org/data/definitions/377) \- Insecure Temporary File 
  * [MITRE, CWE-379](https://cwe.mitre.org/data/definitions/379) \- Creation of Temporary File in Directory with Incorrect Permissions 
  * [OWASP, Insecure Temporary File](https://owasp.org/www-community/vulnerabilities/Insecure_Temporary_File)
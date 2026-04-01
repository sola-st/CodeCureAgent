**A secure password should be used when connecting to a database**  

When relying on the password authentication mode for the database connection, a secure password should be chosen.

This rule raises an issue when an empty password is used.

Noncompliant Code Example
    
    
    Connection conn = DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "login", "");
    

Compliant Solution
    
    
    String password = System.getProperty("database.password");
    Connection conn = DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "login", password);
    

See

  * [OWASP Top 10 2021 Category A7](https://owasp.org/Top10/A07_2021-Identification_and_Authentication_Failures/) \- Identification and Authentication Failures 
  * [OWASP Top 10 2017 Category A2](https://owasp.org/www-project-top-ten/2017/A2_2017-Broken_Authentication.html) \- Broken Authentication 
  * [OWASP Top 10 2017 Category A3](https://owasp.org/www-project-top-ten/2017/A3_2017-Sensitive_Data_Exposure) \- Sensitive Data Exposure 
  * [MITRE, CWE-521](https://cwe.mitre.org/data/definitions/521) \- Weak Password Requirements 
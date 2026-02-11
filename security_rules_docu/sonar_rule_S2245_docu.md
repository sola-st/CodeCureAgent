**Using pseudorandom number generators (PRNGs) is security-sensitive**  

Using pseudorandom number generators (PRNGs) is security-sensitive. For example, it has led in the past to the following vulnerabilities:

  * [CVE-2013-6386](http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2013-6386)
  * [CVE-2006-3419](http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2006-3419)
  * [CVE-2008-4102](http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2008-4102)



When software generates predictable values in a context requiring unpredictability, it may be possible for an attacker to guess the next value that will be generated, and use this guess to impersonate another user or access sensitive information.

As the `java.util.Random` class relies on a pseudorandom number generator, this class and relating `java.lang.Math.random()` method should not be used for security-critical applications or for protecting sensitive data. In such context, the `java.security.SecureRandom` class which relies on a cryptographically strong random number generator (RNG) should be used in place.

Ask Yourself Whether

  * the code using the generated value requires it to be unpredictable. It is the case for all encryption mechanisms or when a secret value, such as a password, is hashed. 
  * the function you use generates a value which can be predicted (pseudo-random). 
  * the generated value is used multiple times. 
  * an attacker can access the generated value. 



There is a risk if you answered yes to any of those questions.

Sensitive Code Example
    
    
    Random random = new Random(); // Sensitive use of Random
    byte bytes[] = new byte[20];
    random.nextBytes(bytes); // Check if bytes is used for hashing, encryption, etc...
    

Recommended Secure Coding Practices

  * Use a cryptographically strong random number generator (RNG) like "java.security.SecureRandom" in place of this PRNG. 
  * Use the generated random values only once. 
  * You should not expose the generated random value. If you have to store it, make sure that the database or file is secure. 



Compliant Solution
    
    
    SecureRandom random = new SecureRandom(); // Compliant for security-sensitive use cases
    byte bytes[] = new byte[20];
    random.nextBytes(bytes);
    

See

  * [OWASP Top 10 2021 Category A2](https://owasp.org/Top10/A02_2021-Cryptographic_Failures/) \- Cryptographic Failures 
  * [OWASP Top 10 2017 Category A3](https://www.owasp.org/www-project-top-ten/2017/A3_2017-Sensitive_Data_Exposure) \- Sensitive Data Exposure 
  * [Mobile AppSec Verification Standard](https://mobile-security.gitbook.io/masvs/security-requirements/0x08-v3-cryptography_verification_requirements) \- Cryptography Requirements 
  * [OWASP Mobile Top 10 2016 Category M5](https://owasp.org/www-project-mobile-top-10/2016-risks/m5-insufficient-cryptography) \- Insufficient Cryptography 
  * [MITRE, CWE-338](https://cwe.mitre.org/data/definitions/338) \- Use of Cryptographically Weak Pseudo-Random Number Generator (PRNG) 
  * [MITRE, CWE-330](https://cwe.mitre.org/data/definitions/330) \- Use of Insufficiently Random Values 
  * [MITRE, CWE-326](https://cwe.mitre.org/data/definitions/326) \- Inadequate Encryption Strength 
  * [MITRE, CWE-1241](https://cwe.mitre.org/data/definitions/1241) \- Use of Predictable Algorithm in Random Number Generator 
  * [CERT, MSC02-J.](https://wiki.sei.cmu.edu/confluence/x/oTdGBQ) \- Generate strong random numbers 
  * [CERT, MSC30-C.](https://wiki.sei.cmu.edu/confluence/x/UNcxBQ) \- Do not use the rand() function for generating pseudorandom numbers 
  * [CERT, MSC50-CPP.](https://wiki.sei.cmu.edu/confluence/x/2ns-BQ) \- Do not use std::rand() for generating pseudorandom numbers 
  * Derived from FindSecBugs rule [Predictable Pseudo Random Number Generator](https://h3xstream.github.io/find-sec-bugs/bugs.htm#PREDICTABLE_RANDOM)
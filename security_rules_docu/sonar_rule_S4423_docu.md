**Weak SSL/TLS protocols should not be used**  

This rule raises an issue when an insecure TLS protocol version (i.e. a protocol different from "TLSv1.2", "TLSv1.3", "DTLSv1.2", or "DTLSv1.3") is used or allowed.

It is recommended to enforce TLS 1.2 as the minimum protocol version and to disallow older versions like TLS 1.0. Failure to do so could open the door to downgrade attacks: a malicious actor who is able to intercept the connection could modify the requested protocol version and downgrade it to a less secure version.

In most cases, using the default system configuration is not compliant. Indeed, an application might get deployed on a wide range of systems with different configurations. While using a system’s default value might be safe on modern up-to-date systems, this might not be the case on older systems. It is therefore recommended to explicitly set a safe configuration in every case.

Noncompliant Code Example

`javax.net.ssl.SSLContext` library:
    
    
    context = SSLContext.getInstance("TLSv1.1"); // Noncompliant
    

[okhttp](https://square.github.io/okhttp/) library:
    
    
    ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
          .tlsVersions(TlsVersion.TLS_1_1) // Noncompliant
          .build();
    

Compliant Solution

`javax.net.ssl.SSLContext` library:
    
    
    context = SSLContext.getInstance("TLSv1.2"); // Compliant
    

[okhttp](https://square.github.io/okhttp/) library:
    
    
    ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
          .tlsVersions(TlsVersion.TLS_1_2) // Compliant
          .build();
    

See

  * [OWASP Top 10 2021 Category A2](https://owasp.org/Top10/A02_2021-Cryptographic_Failures/) \- Cryptographic Failures 
  * [OWASP Top 10 2021 Category A7](https://owasp.org/Top10/A07_2021-Identification_and_Authentication_Failures/) \- Identification and Authentication Failures 
  * [OWASP Top 10 2017 Category A3](https://www.owasp.org/www-project-top-ten/2017/A3_2017-Sensitive_Data_Exposure) \- Sensitive Data Exposure 
  * [OWASP Top 10 2017 Category A6](https://owasp.org/www-project-top-ten/2017/A6_2017-Security_Misconfiguration) \- Security Misconfiguration 
  * [MITRE, CWE-327](https://cwe.mitre.org/data/definitions/326) \- Inadequate Encryption Strength 
  * [MITRE, CWE-326](https://cwe.mitre.org/data/definitions/327) \- Use of a Broken or Risky Cryptographic Algorithm 
  * [SANS Top 25](https://www.sans.org/top25-software-errors/#cat3) \- Porous Defenses 
  * [Diagnosing TLS, SSL, and HTTPS](https://blogs.oracle.com/java-platform-group/diagnosing-tls,-ssl,-and-https)
  * [SSL and TLS Deployment Best Practices - Use secure protocols](https://github.com/ssllabs/research/wiki/SSL-and-TLS-Deployment-Best-Practices#22-use-secure-protocols)
**Delivering code in production with debug features activated is security-sensitive**  

Delivering code in production with debug features activated is security-sensitive. It has led in the past to the following vulnerabilities:

  * [CVE-2018-1999007](http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2018-1999007)
  * [CVE-2015-5306](http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2015-5306)
  * [CVE-2013-2006](http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2013-2006)



An application’s debug features enable developers to find bugs more easily and thus facilitate also the work of attackers. It often gives access to detailed information on both the system running the application and users.

Ask Yourself Whether

  * the code or configuration enabling the application debug features is deployed on production servers or distributed to end users. 
  * the application runs by default with debug features activated. 



There is a risk if you answered yes to any of those questions.

Sensitive Code Example

`Throwable.printStackTrace(...)` prints a Throwable and its stack trace to `System.Err` (by default) which is not easily parseable and can expose sensitive information:
    
    
    try {
      /* ... */
    } catch(Exception e) {
      e.printStackTrace(); // Sensitive
    }
    

[EnableWebSecurity](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/config/annotation/web/configuration/EnableWebSecurity.html) annotation for SpringFramework with `debug` to `true` enables debugging support:
    
    
    import org.springframework.context.annotation.Configuration;
    import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
    
    @Configuration
    @EnableWebSecurity(debug = true) // Sensitive
    public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
      // ...
    }
    

[WebView.setWebContentsDebuggingEnabled(true)](https://developer.android.com/reference/android/webkit/WebView#setWebContentsDebuggingEnabled\(boolean\)) for Android enables debugging support:
    
    
    import android.webkit.WebView;
    
    WebView.setWebContentsDebuggingEnabled(true); // Sensitive
    WebView.getFactory().getStatics().setWebContentsDebuggingEnabled(true); // Sensitive
    

Recommended Secure Coding Practices

Do not enable debug features on production servers or applications distributed to end users.

Compliant Solution

Loggers should be used (instead of `printStackTrace`) to print throwables:
    
    
    try {
      /* ... */
    } catch(Exception e) {
      LOGGER.log("context", e);
    }
    

[EnableWebSecurity](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/config/annotation/web/configuration/EnableWebSecurity.html) annotation for SpringFramework with `debug` to `false` disables debugging support:
    
    
    import org.springframework.context.annotation.Configuration;
    import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
    
    @Configuration
    @EnableWebSecurity(debug = false)
    public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
      // ...
    }
    

[WebView.setWebContentsDebuggingEnabled(false)](https://developer.android.com/reference/android/webkit/WebView#setWebContentsDebuggingEnabled\(boolean\)) for Android disables debugging support:
    
    
    import android.webkit.WebView;
    
    WebView.setWebContentsDebuggingEnabled(false);
    WebView.getFactory().getStatics().setWebContentsDebuggingEnabled(false);
    

See

  * [OWASP Top 10 2021 Category A5](https://owasp.org/Top10/A05_2021-Security_Misconfiguration/) \- Security Misconfiguration 
  * [OWASP Top 10 2017 Category A3](https://www.owasp.org/www-project-top-ten/2017/A3_2017-Sensitive_Data_Exposure) \- Sensitive Data Exposure 
  * [MITRE, CWE-489](https://cwe.mitre.org/data/definitions/489) \- Active Debug Code 
  * [MITRE, CWE-215](https://cwe.mitre.org/data/definitions/215) \- Information Exposure Through Debug Information 
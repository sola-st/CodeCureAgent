**Basic authentication should not be used**  

Basic authentication’s only means of obfuscation is Base64 encoding. Since Base64 encoding is easily recognized and reversed, it offers only the thinnest veil of protection to your users, and should not be used.

Noncompliant Code Example
    
    
    // Using HttpPost from Apache HttpClient
    String encoding = Base64Encoder.encode ("login:passwd");
    org.apache.http.client.methods.HttpPost httppost = new HttpPost(url);
    httppost.setHeader("Authorization", "Basic " + encoding);  // Noncompliant
    
    or
    
    // Using HttpURLConnection
    String encoding = Base64.getEncoder().encodeToString(("login:passwd").getBytes(‌"UTF‌​-8"​));
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setDoOutput(true);
    conn.setRequestProperty("Authorization", "Basic " + encoding); // Noncompliant
    

See

  * [OWASP Top 10 2021 Category A4](https://owasp.org/Top10/A04_2021-Insecure_Design/) \- Insecure Design 
  * [OWASP Top 10 2017 Category A3](https://www.owasp.org/www-project-top-ten/2017/A3_2017-Sensitive_Data_Exposure) \- Sensitive Data Exposure 
  * [OWASP Web Service Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Web_Service_Security_Cheat_Sheet.html#user-authentication)
  * [MITRE, CWE-522](https://cwe.mitre.org/data/definitions/522) \- Insufficiently Protected Credentials 
  * [SANS Top 25](https://www.sans.org/top25-software-errors/#cat3) \- Porous Defenses 
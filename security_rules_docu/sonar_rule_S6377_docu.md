**XML signatures should be validated securely**  

XML signature validations work by parsing third-party data that cannot be trusted until it is actually validated.

As with any other parsing process, unrestricted validation of third-party XML signatures can lead to security vulnerabilities. In this case, threats range from denial of service to confidentiality breaches.

By default, the Java XML Digital Signature API does not apply restrictions on XML signature validation, unless the application runs with a security manager.  
To protect the application from these vulnerabilities, set the `org.jcp.xml.dsig.secureValidation` attribute to `true` with the `javax.xml.crypto.dsig.dom.DOMValidateContext.setProperty` method.  
This attribute ensures that the code enforces the following restrictions:

  * Forbids the use of XSLT transforms 
  * Restricts the number of `SignedInfo` or `Manifest Reference` elements to 30 or less 
  * Restricts the number of `Reference` transforms to 5 or less 
  * Forbids the use of MD5-related signatures or MAC algorithms 
  * Ensures that `Reference` IDs are unique to help prevent signature wrapping attacks 
  * Forbids Reference URIs of type `http`, `https`, or `file`
  * Does not allow a `RetrievalMethod` element to reference another `RetrievalMethod` element 
  * Forbids RSA or DSA keys less than 1024 bits 



Noncompliant Code Example
    
    
    NodeList signatureElement = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
    
    XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
    DOMValidateContext valContext = new DOMValidateContext(new KeyValueKeySelector(), signatureElement.item(0)); // Noncompliant
    XMLSignature signature = fac.unmarshalXMLSignature(valContext);
    
    boolean signatureValidity = signature.validate(valContext);
    

Compliant Solution

In order to benefit from this secure validation mode, set the DOMValidateContext’s `org.jcp.xml.dsig.secureValidation` property to `TRUE`.
    
    
    NodeList signatureElement = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
    
    XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
    DOMValidateContext valContext = new DOMValidateContext(new KeyValueKeySelector(), signatureElement.item(0));
    valContext.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.TRUE);
    XMLSignature signature = fac.unmarshalXMLSignature(valContext);
    
    boolean signatureValidity = signature.validate(valContext);
    

See

  * [Oracle Java Documentation](https://docs.oracle.com/en/java/javase/14/security/java-xml-digital-signature-api-overview-and-tutorial.html#GUID-DB46A001-6DBD-4571-BDBC-1BBC394BF61E) \- XML Digital Signature API Overview and Tutorial 
  * [OWASP Top 10 2017 Category A3](https://owasp.org/www-project-top-ten/2017/A3_2017-Sensitive_Data_Exposure) \- Sensitive Data Exposure 
  * [MITRE, CWE-347](https://cwe.mitre.org/data/definitions/347) \- Improper Verification of Cryptographic Signature 
**XML parsers should not be vulnerable to XXE attacks**  

XML standard allows the use of entities, declared in the DOCTYPE of the document, which can be [internal](https://www.w3.org/TR/xml/#sec-internal-ent) or [external](https://www.w3.org/TR/xml/#sec-external-ent).

When parsing the XML file, the content of the external entities is retrieved from an external storage such as the file system or network, which may lead, if no restrictions are put in place, to arbitrary file disclosures or [server-side request forgery (SSRF)](https://owasp.org/www-community/attacks/Server_Side_Request_Forgery) vulnerabilities.

It’s recommended to limit resolution of external entities by using one of these solutions:

  * If DOCTYPE is not necessary, completely disable all DOCTYPE declarations. 
  * If external entities are not necessary, completely disable their declarations. 
  * If external entities are necessary then: 
    * Use XML processor features, if available, to authorize only required protocols (eg: https). 
    * And use an entity resolver (and optionally an XML Catalog) to resolve only trusted entities. == Noncompliant Code Example 



For [DocumentBuilder](https://docs.oracle.com/javase/9/docs/api/javax/xml/parsers/DocumentBuilderFactory.html), [SAXParser](https://docs.oracle.com/javase/9/docs/api/javax/xml/parsers/SAXParserFactory.html), [XMLInput](https://docs.oracle.com/javase/9/docs/api/javax/xml/stream/XMLInputFactory.html), [Transformer](https://docs.oracle.com/javase/9/docs/api/javax/xml/transform/TransformerFactory.html) and [Schema](https://docs.oracle.com/javase/9/docs/api/javax/xml/validation/SchemaFactory.html) JAPX factories:
    
    
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Noncompliant
    
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Noncompliant
    
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Noncompliant
    
    TransformerFactory factory = javax.xml.transform.TransformerFactory.newInstance();  // Noncompliant
    
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);  // Noncompliant
    

For [Dom4j](https://dom4j.github.io/) library:
    
    
    SAXReader xmlReader = new SAXReader(); // Noncompliant
    

For [Jdom2](http://www.jdom.org/) library:
    
    
    SAXBuilder builder = new SAXBuilder(); // Noncompliant
    

Compliant Solution

For [DocumentBuilder](https://docs.oracle.com/javase/9/docs/api/javax/xml/parsers/DocumentBuilderFactory.html), [SAXParser](https://docs.oracle.com/javase/9/docs/api/javax/xml/parsers/SAXParserFactory.html), [XMLInput](https://docs.oracle.com/javase/9/docs/api/javax/xml/stream/XMLInputFactory.html), [Transformer](https://docs.oracle.com/javase/9/docs/api/javax/xml/transform/TransformerFactory.html) and [Schema](https://docs.oracle.com/javase/9/docs/api/javax/xml/validation/SchemaFactory.html) JAPX factories:
    
    
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    // to be compliant, completely disable DOCTYPE declaration:
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    // or completely disable external entities declarations:
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    // or prohibit the use of all protocols by external entities:
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    // or disable entity expansion but keep in mind that this doesn't prevent fetching external entities
    // and this solution is not correct for OpenJDK < 13 due to a bug: https://bugs.openjdk.java.net/browse/JDK-8206132
    factory.setExpandEntityReferences(false);
    
    
    SAXParserFactory factory = SAXParserFactory.newInstance();
    // to be compliant, completely disable DOCTYPE declaration:
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    // or completely disable external entities declarations:
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    // or prohibit the use of all protocols by external entities:
    SAXParser parser = factory.newSAXParser(); // Noncompliant
    parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    
    XMLInputFactory factory = XMLInputFactory.newInstance();
    // to be compliant, completely disable DOCTYPE declaration:
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    // or completely disable external entities declarations:
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
    // or prohibit the use of all protocols by external entities:
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    
    TransformerFactory factory = javax.xml.transform.TransformerFactory.newInstance();
    // to be compliant, prohibit the use of all protocols by external entities:
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    // to be compliant, completely disable DOCTYPE declaration:
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    // or prohibit the use of all protocols by external entities:
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    

For [Dom4j](https://dom4j.github.io/) library:
    
    
    SAXReader xmlReader = new SAXReader();
    xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    

For [Jdom2](http://www.jdom.org/) library:
    
    
    SAXBuilder builder = new SAXBuilder();
    builder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    builder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    

See

  * [OWASP Top 10 2021 Category A5](https://owasp.org/Top10/A05_2021-Security_Misconfiguration/) \- Security Misconfiguration 
  * [Oracle Java Documentation](https://docs.oracle.com/en/java/javase/13/security/java-api-xml-processing-jaxp-security-guide.html#GUID-8CD65EF5-D113-4D5C-A564-B875C8625FAC) \- XML External Entity Injection Attack 
  * [OWASP Top 10 2017 Category A4](https://owasp.org/www-project-top-ten/2017/A4_2017-XML_External_Entities_\(XXE\)) \- XML External Entities (XXE) 
  * [OWASP XXE Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html#java)
  * [MITRE, CWE-611](https://cwe.mitre.org/data/definitions/611) \- Information Exposure Through XML External Entity Reference 
  * [MITRE, CWE-827](https://cwe.mitre.org/data/definitions/827) \- Improper Control of Document Type Definition 
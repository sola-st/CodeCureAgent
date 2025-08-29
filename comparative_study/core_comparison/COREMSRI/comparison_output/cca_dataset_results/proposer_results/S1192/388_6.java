```java
package com.adobe.epubcheck.util;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.adobe.epubcheck.api.EPUBLocation;
import com.adobe.epubcheck.reporting.CheckMessage;


public class XmlReportImpl extends XmlReportAbstract
{

  private static final String NAMESPACE_JHOVE = "http://schema.openpreservation.org/ois/xml/ns/jhove";
  private static final String PREFIX_XSI = "xsi";
  private static final String NAMESPACE_XSI = "http://www.w3.org/2001/XMLSchema-instance";
  private static final String SCHEMA_LOCATION = "http://schema.openpreservation.org/ois/xml/ns/jhove https://schema.openpreservation.org/ois/xml/xsd/jhove/jhove.xsd";
  private static final String ELEMENT_JHOVE = "jhove";
  private static final String ELEMENT_DATE = "date";
  private static final String ELEMENT_REPINFO = "repInfo";
  private static final String ATTR_URI = "uri";
  private static final String ELEMENT_CREATED = "created";
  private static final String ELEMENT_LASTMODIFIED = "lastModified";
  private static final String ELEMENT_FORMAT = "format";
  private static final String ELEMENT_VERSION = "version";
  private static final String ELEMENT_CUSTOM_MESSAGE_FILENAME = "customMessageFileName";
  private static final String ELEMENT_STATUS = "status";
  private static final String STATUS_WELL_FORMED = "Well-formed";
  private static final String STATUS_NOT_WELL_FORMED = "Not well-formed";
  private static final String ELEMENT_MESSAGES = "messages";
  private static final String ELEMENT_MESSAGE = "message";
  private static final String ATTR_ID = "id";
  private static final String ATTR_SEVERITY = "severity";
  private static final String SEVERITY_ERROR = "error";
  private static final String SEVERITY_WARNING = "warning";
  private static final String SEVERITY_INFO = "info";
  private static final String ELEMENT_MIMETYPE = "mimeType";
  private static final String ELEMENT_PROPERTIES = "properties";
  private static final String ELEMENT_PROPERTY = "property";
  private static final String ELEMENT_NAME = "name";
  private static final String ELEMENT_VALUES = "values";
  private static final String ATTR_ARITY = "arity";
  private static final String ATTR_TYPE = "type";
  private static final String ARITY_LIST = "List";
  private static final String ARITY_SCALAR = "Scalar";
  private static final String ARITY_ARRAY = "Array";
  private static final String TYPE_PROPERTY = "Property";
  private static final String TYPE_STRING = "String";
  private static final String TYPE_DATE = "Date";
  private static final String TYPE_LONG = "Long";
  private static final String TYPE_BOOLEAN = "Boolean";

  public XmlReportImpl(PrintWriter out, String ePubName, String versionEpubCheck)
  {
	  super(out, ePubName, versionEpubCheck);
  }

  @SuppressWarnings("unchecked")
  public int generateReport()
  {
	if (out == null) return 1;
	
    int returnCode = 1;
    
    generationDate = fromTime(System.currentTimeMillis());
    try
    {
      setNamespace(NAMESPACE_JHOVE);
      addPrefixNamespace(PREFIX_XSI, NAMESPACE_XSI);
	  List<KeyValue<String, String>> attrs = new ArrayList<KeyValue<String, String>>();
	  attrs.add(KeyValue.with("name", epubCheckName));
	  attrs.add(KeyValue.with("release", epubCheckVersion)); 
	  attrs.add(KeyValue.with("date", epubCheckDate));
	  attrs.add(KeyValue.with("xsi:schemaLocation", SCHEMA_LOCATION));
	  startElement(ELEMENT_JHOVE, attrs);

	  generateElement(ELEMENT_DATE, generationDate);
	  startElement(ELEMENT_REPINFO, KeyValue.with(ATTR_URI, getEpubFileName()));
      generateElement(ELEMENT_CREATED, creationDate);
      generateElement(ELEMENT_LASTMODIFIED, lastModifiedDate);
      if (formatName == null) {
        generateElement(ELEMENT_FORMAT, "application/octet-stream");
      } else {
        generateElement(ELEMENT_FORMAT, formatName); //application/epub+zip
      }
      generateElement(ELEMENT_VERSION, formatVersion);
      String customMessageFileName = this.getCustomMessageFile();
      if (customMessageFileName != null && !customMessageFileName.isEmpty())
      {
        generateElement(ELEMENT_CUSTOM_MESSAGE_FILENAME, customMessageFileName);
      }
      if (fatalErrors.isEmpty() && errors.isEmpty())
      {
        generateElement(ELEMENT_STATUS, STATUS_WELL_FORMED);
      }
      else
      {
        generateElement(ELEMENT_STATUS, STATUS_NOT_WELL_FORMED);
      }
      if (!warns.isEmpty() || !fatalErrors.isEmpty() || !errors.isEmpty() || !hints.isEmpty())
      {
        startElement(ELEMENT_MESSAGES);
        for (CheckMessage c : fatalErrors) {
        	String m = c.getID() + ", FATAL, [" + c.getMessage() + "], ";
        	for (EPUBLocation ml : c.getLocations()) {
			  String loc = "";
			  if (ml.getLine() > 0 || ml.getColumn() > 0) {
				loc = " (" + ml.getLine() + "-" + ml.getColumn() + ")";
			  }
              generateElement(ELEMENT_MESSAGE, m + PathUtil.removeWorkingDirectory(ml.getPath()) + loc,
            		  KeyValue.with(ATTR_ID, c.getID()), KeyValue.with(ATTR_SEVERITY, SEVERITY_ERROR));
        	}
        }
        for (CheckMessage c : errors) {
        	String m = c.getID() + ", ERROR, [" + c.getMessage() + "], ";
        	for (EPUBLocation ml : c.getLocations()) {
			  String loc = "";
			  if (ml.getLine() > 0 || ml.getColumn() > 0) {
				loc = " (" + ml.getLine() + "-" + ml.getColumn() + ")";
			  }
              generateElement(ELEMENT_MESSAGE, m + PathUtil.removeWorkingDirectory(ml.getPath()) + loc,
            		  KeyValue.with(ATTR_ID, c.getID()), KeyValue.with(ATTR_SEVERITY, SEVERITY_ERROR));
        	}
        }
        for (CheckMessage c : warns) {
        	String m = c.getID() + ", WARN, [" + c.getMessage() + "], ";
        	for (EPUBLocation ml : c.getLocations()) {
			  String loc = "";
			  if (ml.getLine() > 0 || ml.getColumn() > 0) {
				loc = " (" + ml.getLine() + "-" + ml.getColumn() + ")";
			  }
              generateElement(ELEMENT_MESSAGE, m + PathUtil.removeWorkingDirectory(ml.getPath()) + loc,
            		  KeyValue.with(ATTR_ID, c.getID()), KeyValue.with(ATTR_SEVERITY, SEVERITY_WARNING));
        	}
        }
        for (CheckMessage c : hints) {
        	String m = c.getID() + ", HINT, [" + c.getMessage() + "], ";
        	for (EPUBLocation ml : c.getLocations()) {
			  String loc = "";
			  if (ml.getLine() > 0 || ml.getColumn() > 0) {
				loc = " (" + ml.getLine() + "-" + ml.getColumn() + ")";
			  }
              generateElement(ELEMENT_MESSAGE, m + PathUtil.removeWorkingDirectory(ml.getPath()) + loc,
            		  KeyValue.with(ATTR_ID, c.getID()), KeyValue.with(ATTR_SEVERITY, SEVERITY_INFO));
        	}
        }
        endElement(ELEMENT_MESSAGES);
      }
      generateElement(ELEMENT_MIMETYPE, formatName);
      startElement(ELEMENT_PROPERTIES);

      generateProperty("FileName", getNameFromPath(getEpubFileName()), TYPE_STRING);
      generateProperty("PageCount", pagesCount);
      generateProperty("CharacterCount", charsCount);
      generateProperty("Language", language, TYPE_STRING);

  	  startElement(ELEMENT_PROPERTY);
      generateElement(ELEMENT_NAME, "Info");
      startElement(ELEMENT_VALUES, KeyValue.with(ATTR_ARITY, ARITY_LIST), KeyValue.with(ATTR_TYPE, TYPE_PROPERTY));

      generateProperty("Identifier", identifier, TYPE_STRING);
      generateProperty("CreationDate", creationDate, TYPE_DATE);
      generateProperty("ModDate", lastModifiedDate, TYPE_DATE);

      if (!titles.isEmpty())
      {
          String[] cs = titles.toArray(new String[titles.size()]);
          generateProperty("Title", cs, TYPE_STRING);
      }
      if (!creators.isEmpty())
      {
        String[] cs = creators.toArray(new String[creators.size()]);
        generateProperty("Creator", cs, TYPE_STRING);
      }
      if (!contributors.isEmpty())
      {
        String[] cs = contributors.toArray(new String[contributors.size()]);
        generateProperty("Contributor", cs, TYPE_STRING);
      }
      generateProperty("Date", date, TYPE_STRING);
      generateProperty("Publisher", publisher, TYPE_STRING);
      if (!subjects.isEmpty())
      {
        String[] cs = subjects.toArray(new String[subjects.size()]);
        generateProperty("Subject", cs, TYPE_STRING);
      }
      if (!rights.isEmpty())
      {
        String[] cs = rights.toArray(new String[rights.size()]);
        generateProperty("Rights", cs, TYPE_STRING);
      }
      endElement(ELEMENT_VALUES);
      endElement(ELEMENT_PROPERTY);

      if (!embeddedFonts.isEmpty() || !refFonts.isEmpty())
      {
 	    startElement(ELEMENT_PROPERTY);
        generateElement(ELEMENT_NAME, "Fonts");
        startElement(ELEMENT_VALUES, KeyValue.with(ATTR_ARITY, ARITY_LIST), KeyValue.with(ATTR_TYPE, TYPE_PROPERTY));

        for (String f : embeddedFonts)
        {
      	  startElement(ELEMENT_PROPERTY);
          generateElement(ELEMENT_NAME, "Font");
          startElement(ELEMENT_VALUES, KeyValue.with(ATTR_ARITY, ARITY_LIST), KeyValue.with(ATTR_TYPE, TYPE_PROPERTY));
          generateProperty("FontName", getNameFromPath(f), TYPE_STRING);
          generateProperty("FontFile", true);
          endElement(ELEMENT_VALUES);
          endElement(ELEMENT_PROPERTY);
        }
        for (String f : refFonts)
        {
          startElement(ELEMENT_PROPERTY);
          generateElement(ELEMENT_NAME, "Font");
          startElement(ELEMENT_VALUES, KeyValue.with(ATTR_ARITY, ARITY_LIST), KeyValue.with(ATTR_TYPE, TYPE_PROPERTY));
          generateProperty("FontName", getNameFromPath(f), TYPE_STRING);
          generateProperty("FontFile", false);
          endElement(ELEMENT_VALUES);
          endElement(ELEMENT_PROPERTY);
        }
        
        endElement(ELEMENT_VALUES);
        endElement(ELEMENT_PROPERTY);
      }

      if (!references.isEmpty())
      {
    	startElement(ELEMENT_PROPERTY);
    	generateElement(ELEMENT_NAME, "References");
    	startElement(ELEMENT_VALUES, KeyValue.with(ATTR_ARITY, ARITY_LIST), KeyValue.with(ATTR_TYPE, TYPE_PROPERTY));
        for (String r : references)
        {
          generateProperty("Reference", r, TYPE_STRING);
        }
        endElement(ELEMENT_VALUES);
        endElement(ELEMENT_PROPERTY);
      }
      if (!mediaTypes.isEmpty())
      {
          String[] cs = mediaTypes.toArray(new String[mediaTypes.size()]);
          generateProperty("MediaTypes", cs, TYPE_STRING);
      }

      if (hasEncryption)
      {
        generateProperty("hasEncryption", hasEncryption);
      }
      if (hasSignatures)
      {
        generateProperty("hasSignatures", hasSignatures);
      }
      if (hasAudio)
      {
        generateProperty("hasAudio", hasAudio);
      }
      if (hasVideo)
      {
        generateProperty("hasVideo", hasVideo);
      }
      if (hasFixedLayout)
      {
        generateProperty("hasFixedLayout", hasFixedLayout);
      }
      if (hasScripts)
      {
        generateProperty("hasScripts", hasScripts);
      }

      endElement(ELEMENT_PROPERTIES);
      endElement(ELEMENT_REPINFO);
      endElement(ELEMENT_JHOVE);
      returnCode = 0;
    }
    catch (Exception e)
    {
      System.err.println("Exception encountered: " + e.getMessage());
      returnCode = 1;
    }
    return returnCode;
  }

  @SuppressWarnings("unchecked")
  private void generateProperty(String name, String[] value, String type)
  {
    if (value == null || value.length == 0)
    {
      return;
    }
	startElement(ELEMENT_PROPERTY);
    generateElement(ELEMENT_NAME, name);
    startElement(ELEMENT_VALUES, KeyValue.with(ATTR_ARITY, value.length == 1 ? ARITY_SCALAR : ARITY_ARRAY), KeyValue.with(ATTR_TYPE, type));
    for (String v : value)
    {
      generateElement("value", v);
    }
    endElement(ELEMENT_VALUES);
    endElement(ELEMENT_PROPERTY);
  }

  @SuppressWarnings("unchecked")
  private void generateProperty(String name, String value, String type)
  {
    if (value == null || value.trim().length() == 0)
    {
      return;
    }
	startElement(ELEMENT_PROPERTY);
    generateElement(ELEMENT_NAME, name);
    startElement(ELEMENT_VALUES, KeyValue.with(ATTR_ARITY, ARITY_SCALAR), KeyValue.with(ATTR_TYPE, type));
    generateElement("value", value);
    endElement(ELEMENT_VALUES);
    endElement(ELEMENT_PROPERTY);
  }

  private void generateProperty(String name, long value)
  {
    if (value == 0)
    {
      return;
    }
    generateProperty(name, Long.toString(value), TYPE_LONG);
  }

  private void generateProperty(String name, boolean value)
  {
    generateProperty(name, value ? "true" : "false", TYPE_BOOLEAN);
  }

}

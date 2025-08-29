```java
package com.adobe.epubcheck.util;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.adobe.epubcheck.api.EPUBLocation;
import com.adobe.epubcheck.reporting.CheckMessage;


public class XmlReportImpl extends XmlReportAbstract
{
  private static final String NS_JHOVE = "http://schema.openpreservation.org/ois/xml/ns/jhove";
  private static final String PREFIX_XSI = "xsi";
  private static final String NS_XSI = "http://www.w3.org/2001/XMLSchema-instance";
  private static final String SCHEMA_LOCATION_KEY = "xsi:schemaLocation";
  private static final String SCHEMA_LOCATION_VALUE = "http://schema.openpreservation.org/ois/xml/ns/jhove https://schema.openpreservation.org/ois/xml/xsd/jhove/jhove.xsd";

  private static final String JHOVE = "jhove";
  private static final String DATE = "date";
  private static final String REP_INFO = "repInfo";
  private static final String URI = "uri";
  private static final String CREATED = "created";
  private static final String LAST_MODIFIED = "lastModified";
  private static final String FORMAT = "format";
  private static final String VERSION = "version";
  private static final String CUSTOM_MESSAGE_FILE_NAME = "customMessageFileName";
  private static final String STATUS = "status";
  private static final String STATUS_WELL_FORMED = "Well-formed";
  private static final String STATUS_NOT_WELL_FORMED = "Not well-formed";
  private static final String MESSAGES = "messages";
  private static final String MESSAGE = "message";
  private static final String ID = "id";
  private static final String SEVERITY_ERROR = "error";
  private static final String SEVERITY_WARNING = "warning";
  private static final String SEVERITY_INFO = "info";
  private static final String MIME_TYPE = "mimeType";
  private static final String PROPERTIES = "properties";
  private static final String PROPERTY = "property";
  private static final String NAME = "name";
  private static final String VALUES = "values";
  private static final String ARITY = "arity";
  private static final String TYPE = "type";
  private static final String LIST = "List";
  private static final String PROPERTY_TYPE = "Property";
  private static final String STRING = "String";
  private static final String DATE_TYPE = "Date";
  private static final String LONG_TYPE = "Long";
  private static final String BOOLEAN_TYPE = "Boolean";
  private static final String VALUE = "value";

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
      setNamespace(NS_JHOVE);
      addPrefixNamespace(PREFIX_XSI, NS_XSI);
	  List<KeyValue<String, String>> attrs = new ArrayList<KeyValue<String, String>>();
	  attrs.add(KeyValue.with("name", epubCheckName));
	  attrs.add(KeyValue.with("release", epubCheckVersion)); 
	  attrs.add(KeyValue.with("date", epubCheckDate));
	  attrs.add(KeyValue.with(SCHEMA_LOCATION_KEY, SCHEMA_LOCATION_VALUE));
	  startElement(JHOVE, attrs);

	  generateElement(DATE, generationDate);
	  startElement(REP_INFO, KeyValue.with(URI, getEpubFileName()));
      generateElement(CREATED, creationDate);
      generateElement(LAST_MODIFIED, lastModifiedDate);
      if (formatName == null) {
        generateElement(FORMAT, "application/octet-stream");
      } else {
        generateElement(FORMAT, formatName); //application/epub+zip
      }
      generateElement(VERSION, formatVersion);
      String customMessageFileName = this.getCustomMessageFile();
      if (customMessageFileName != null && !customMessageFileName.isEmpty())
      {
        generateElement(CUSTOM_MESSAGE_FILE_NAME, customMessageFileName);
      }
      if (fatalErrors.isEmpty() && errors.isEmpty())
      {
        generateElement(STATUS, STATUS_WELL_FORMED);
      }
      else
      {
        generateElement(STATUS, STATUS_NOT_WELL_FORMED);
      }
      if (!warns.isEmpty() || !fatalErrors.isEmpty() || !errors.isEmpty() || !hints.isEmpty())
      {
        startElement(MESSAGES);
        for (CheckMessage c : fatalErrors) {
        	String m = c.getID() + ", FATAL, [" + c.getMessage() + "], ";
        	for (EPUBLocation ml : c.getLocations()) {
			  String loc = "";
			  if (ml.getLine() > 0 || ml.getColumn() > 0) {
				loc = " (" + ml.getLine() + "-" + ml.getColumn() + ")";
			  }
              generateElement(MESSAGE, m + PathUtil.removeWorkingDirectory(ml.getPath()) + loc,
            		  KeyValue.with(ID, c.getID()), KeyValue.with("severity", SEVERITY_ERROR));
        	}
        }
        for (CheckMessage c : errors) {
        	String m = c.getID() + ", ERROR, [" + c.getMessage() + "], ";
        	for (EPUBLocation ml : c.getLocations()) {
			  String loc = "";
			  if (ml.getLine() > 0 || ml.getColumn() > 0) {
				loc = " (" + ml.getLine() + "-" + ml.getColumn() + ")";
			  }
              generateElement(MESSAGE, m + PathUtil.removeWorkingDirectory(ml.getPath()) + loc,
            		  KeyValue.with(ID, c.getID()), KeyValue.with("severity", SEVERITY_ERROR));
        	}
        }
        for (CheckMessage c : warns) {
        	String m = c.getID() + ", WARN, [" + c.getMessage() + "], ";
        	for (EPUBLocation ml : c.getLocations()) {
			  String loc = "";
			  if (ml.getLine() > 0 || ml.getColumn() > 0) {
				loc = " (" + ml.getLine() + "-" + ml.getColumn() + ")";
			  }
              generateElement(MESSAGE, m + PathUtil.removeWorkingDirectory(ml.getPath()) + loc,
            		  KeyValue.with(ID, c.getID()), KeyValue.with("severity", SEVERITY_WARNING));
        	}
        }
        for (CheckMessage c : hints) {
        	String m = c.getID() + ", HINT, [" + c.getMessage() + "], ";
        	for (EPUBLocation ml : c.getLocations()) {
			  String loc = "";
			  if (ml.getLine() > 0 || ml.getColumn() > 0) {
				loc = " (" + ml.getLine() + "-" + ml.getColumn() + ")";
			  }
              generateElement(MESSAGE, m + PathUtil.removeWorkingDirectory(ml.getPath()) + loc,
            		  KeyValue.with(ID, c.getID()), KeyValue.with("severity", SEVERITY_INFO));
        	}
        }
        endElement(MESSAGES);
      }
      generateElement(MIME_TYPE, formatName);
      startElement(PROPERTIES);

      generateProperty("FileName", getNameFromPath(getEpubFileName()), STRING);
      generateProperty("PageCount", pagesCount);
      generateProperty("CharacterCount", charsCount);
      generateProperty("Language", language, STRING);

  	  startElement(PROPERTY);
      generateElement(NAME, "Info");
      startElement(VALUES, KeyValue.with(ARITY, LIST), KeyValue.with(TYPE, PROPERTY_TYPE));

      generateProperty("Identifier", identifier, STRING);
      generateProperty("CreationDate", creationDate, DATE_TYPE);
      generateProperty("ModDate", lastModifiedDate, DATE_TYPE);

      if (!titles.isEmpty())
      {
          String[] cs = titles.toArray(new String[titles.size()]);
          generateProperty("Title", cs, STRING);
      }
      if (!creators.isEmpty())
      {
        String[] cs = creators.toArray(new String[creators.size()]);
        generateProperty("Creator", cs, STRING);
      }
      if (!contributors.isEmpty())
      {
        String[] cs = contributors.toArray(new String[contributors.size()]);
        generateProperty("Contributor", cs, STRING);
      }
      generateProperty("Date", date, STRING);
      generateProperty("Publisher", publisher, STRING);
      if (!subjects.isEmpty())
      {
        String[] cs = subjects.toArray(new String[subjects.size()]);
        generateProperty("Subject", cs, STRING);
      }
      if (!rights.isEmpty())
      {
        String[] cs = rights.toArray(new String[rights.size()]);
        generateProperty("Rights", cs, STRING);
      }
      endElement(VALUES);
      endElement(PROPERTY);

      if (!embeddedFonts.isEmpty() || !refFonts.isEmpty())
      {
 	    startElement(PROPERTY);
        generateElement(NAME, "Fonts");
        startElement(VALUES, KeyValue.with(ARITY, LIST), KeyValue.with(TYPE, PROPERTY_TYPE));

        for (String f : embeddedFonts)
        {
      	  startElement(PROPERTY);
          generateElement(NAME, "Font");
          startElement(VALUES, KeyValue.with(ARITY, LIST), KeyValue.with(TYPE, PROPERTY_TYPE));
          generateProperty("FontName", getNameFromPath(f), STRING);
          generateProperty("FontFile", true);
          endElement(VALUES);
          endElement(PROPERTY);
        }
        for (String f : refFonts)
        {
          startElement(PROPERTY);
          generateElement(NAME, "Font");
          startElement(VALUES, KeyValue.with(ARITY, LIST), KeyValue.with(TYPE, PROPERTY_TYPE));
          generateProperty("FontName", getNameFromPath(f), STRING);
          generateProperty("FontFile", false);
          endElement(VALUES);
          endElement(PROPERTY);
        }
        
        endElement(VALUES);
        endElement(PROPERTY);
      }

      if (!references.isEmpty())
      {
    	startElement(PROPERTY);
    	generateElement(NAME, "References");
    	startElement(VALUES, KeyValue.with(ARITY, LIST), KeyValue.with(TYPE, PROPERTY_TYPE));
        for (String r : references)
        {
          generateProperty("Reference", r, STRING);
        }
        endElement(VALUES);
        endElement(PROPERTY);
      }
      if (!mediaTypes.isEmpty())
      {
          String[] cs = mediaTypes.toArray(new String[mediaTypes.size()]);
          generateProperty("MediaTypes", cs, STRING);
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

      endElement(PROPERTIES);
      endElement(REP_INFO);
      endElement(JHOVE);
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
	startElement(PROPERTY);
    generateElement(NAME, name);
    startElement(VALUES, KeyValue.with(ARITY, value.length == 1 ? "Scalar" : "Array"), KeyValue.with(TYPE, type));
    for (String v : value)
    {
      generateElement(VALUE, v);
    }
    endElement(VALUES);
    endElement(PROPERTY);
  }

  @SuppressWarnings("unchecked")
  private void generateProperty(String name, String value, String type)
  {
    if (value == null || value.trim().length() == 0)
    {
      return;
    }
	startElement(PROPERTY);
    generateElement(NAME, name);
    startElement(VALUES, KeyValue.with(ARITY, "Scalar"), KeyValue.with(TYPE, type));
    generateElement(VALUE, value);
    endElement(VALUES);
    endElement(PROPERTY);
  }

  private void generateProperty(String name, long value)
  {
    if (value == 0)
    {
      return;
    }
    generateProperty(name, Long.toString(value), LONG_TYPE);
  }

  private void generateProperty(String name, boolean value)
  {
    generateProperty(name, value ? "true" : "false", BOOLEAN_TYPE);
  }

}

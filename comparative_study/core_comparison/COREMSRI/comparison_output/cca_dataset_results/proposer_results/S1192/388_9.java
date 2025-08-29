```java
package com.adobe.epubcheck.util;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.adobe.epubcheck.api.EPUBLocation;
import com.adobe.epubcheck.reporting.CheckMessage;


public class XmlReportImpl extends XmlReportAbstract
{

  private static final String JHOVE_NAMESPACE = "http://schema.openpreservation.org/ois/xml/ns/jhove";
  private static final String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";
  private static final String XSI_SCHEMA_LOCATION = "http://schema.openpreservation.org/ois/xml/ns/jhove https://schema.openpreservation.org/ois/xml/xsd/jhove/jhove.xsd";

  private static final String STATUS_WELL_FORMED = "Well-formed";
  private static final String STATUS_NOT_WELL_FORMED = "Not well-formed";

  private static final String SEVERITY_ERROR = "error";
  private static final String SEVERITY_WARNING = "warning";
  private static final String SEVERITY_INFO = "info";

  private static final String NAME = "name";
  private static final String RELEASE = "release";
  private static final String DATE = "date";
  private static final String XSI_SCHEMA_LOCATION_ATTR = "xsi:schemaLocation";

  private static final String ID = "id";
  private static final String SEVERITY = "severity";

  private static final String MESSAGE_ELEMENT = "message";

  private static final String PROPERTY_ELEMENT = "property";
  private static final String VALUES_ELEMENT = "values";
  private static final String NAME_ELEMENT = "name";
  private static final String VALUE_ELEMENT = "value";

  private static final String ARITY = "arity";
  private static final String TYPE = "type";

  private static final String JHOVE = "jhove";
  private static final String REP_INFO = "repInfo";
  private static final String CREATED = "created";
  private static final String LAST_MODIFIED = "lastModified";
  private static final String FORMAT = "format";
  private static final String VERSION = "version";
  private static final String CUSTOM_MESSAGE_FILE_NAME = "customMessageFileName";
  private static final String STATUS = "status";
  private static final String MESSAGES = "messages";
  private static final String MIME_TYPE = "mimeType";
  private static final String PROPERTIES = "properties";

  private static final String INFO = "Info";
  private static final String FONTS = "Fonts";
  private static final String FONT = "Font";
  private static final String FONT_NAME = "FontName";
  private static final String FONT_FILE = "FontFile";
  private static final String REFERENCES = "References";
  private static final String REFERENCE = "Reference";

  private static final String TRUE = "true";
  private static final String FALSE = "false";

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
      setNamespace(JHOVE_NAMESPACE);
      addPrefixNamespace("xsi", XSI_NAMESPACE);
	  List<KeyValue<String, String>> attrs = new ArrayList<KeyValue<String, String>>();
	  attrs.add(KeyValue.with(NAME, epubCheckName));
	  attrs.add(KeyValue.with(RELEASE, epubCheckVersion)); 
	  attrs.add(KeyValue.with(DATE, epubCheckDate));
	  attrs.add(KeyValue.with(XSI_SCHEMA_LOCATION_ATTR, XSI_SCHEMA_LOCATION));
	  startElement(JHOVE, attrs);

	  generateElement(DATE, generationDate);
	  startElement(REP_INFO, KeyValue.with("uri", getEpubFileName()));
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
              generateElement(MESSAGE_ELEMENT, m + PathUtil.removeWorkingDirectory(ml.getPath()) + loc,
            		  KeyValue.with(ID, c.getID()), KeyValue.with(SEVERITY, SEVERITY_ERROR));
        	}
        }
        for (CheckMessage c : errors) {
        	String m = c.getID() + ", ERROR, [" + c.getMessage() + "], ";
        	for (EPUBLocation ml : c.getLocations()) {
			  String loc = "";
			  if (ml.getLine() > 0 || ml.getColumn() > 0) {
				loc = " (" + ml.getLine() + "-" + ml.getColumn() + ")";
			  }
              generateElement(MESSAGE_ELEMENT, m + PathUtil.removeWorkingDirectory(ml.getPath()) + loc,
            		  KeyValue.with(ID, c.getID()), KeyValue.with(SEVERITY, SEVERITY_ERROR));
        	}
        }
        for (CheckMessage c : warns) {
        	String m = c.getID() + ", WARN, [" + c.getMessage() + "], ";
        	for (EPUBLocation ml : c.getLocations()) {
			  String loc = "";
			  if (ml.getLine() > 0 || ml.getColumn() > 0) {
				loc = " (" + ml.getLine() + "-" + ml.getColumn() + ")";
			  }
              generateElement(MESSAGE_ELEMENT, m + PathUtil.removeWorkingDirectory(ml.getPath()) + loc,
            		  KeyValue.with(ID, c.getID()), KeyValue.with(SEVERITY, SEVERITY_WARNING));
        	}
        }
        for (CheckMessage c : hints) {
        	String m = c.getID() + ", HINT, [" + c.getMessage() + "], ";
        	for (EPUBLocation ml : c.getLocations()) {
			  String loc = "";
			  if (ml.getLine() > 0 || ml.getColumn() > 0) {
				loc = " (" + ml.getLine() + "-" + ml.getColumn() + ")";
			  }
              generateElement(MESSAGE_ELEMENT, m + PathUtil.removeWorkingDirectory(ml.getPath()) + loc,
            		  KeyValue.with(ID, c.getID()), KeyValue.with(SEVERITY, SEVERITY_INFO));
        	}
        }
        endElement(MESSAGES);
      }
      generateElement(MIME_TYPE, formatName);
      startElement(PROPERTIES);

      generateProperty("FileName", getNameFromPath(getEpubFileName()), "String");
      generateProperty("PageCount", pagesCount);
      generateProperty("CharacterCount", charsCount);
      generateProperty("Language", language, "String");

  	  startElement(PROPERTY_ELEMENT);
      generateElement(NAME_ELEMENT, INFO);
      startElement(VALUES_ELEMENT, KeyValue.with(ARITY, "List"), KeyValue.with(TYPE, "Property"));

      generateProperty("Identifier", identifier, "String");
      generateProperty("CreationDate", creationDate, "Date");
      generateProperty("ModDate", lastModifiedDate, "Date");

      if (!titles.isEmpty())
      {
          String[] cs = titles.toArray(new String[titles.size()]);
          generateProperty("Title", cs, "String");
      }
      if (!creators.isEmpty())
      {
        String[] cs = creators.toArray(new String[creators.size()]);
        generateProperty("Creator", cs, "String");
      }
      if (!contributors.isEmpty())
      {
        String[] cs = contributors.toArray(new String[contributors.size()]);
        generateProperty("Contributor", cs, "String");
      }
      generateProperty("Date", date, "String");
      generateProperty("Publisher", publisher, "String");
      if (!subjects.isEmpty())
      {
        String[] cs = subjects.toArray(new String[subjects.size()]);
        generateProperty("Subject", cs, "String");
      }
      if (!rights.isEmpty())
      {
        String[] cs = rights.toArray(new String[rights.size()]);
        generateProperty("Rights", cs, "String");
      }
      endElement(VALUES_ELEMENT);
      endElement(PROPERTY_ELEMENT);

      if (!embeddedFonts.isEmpty() || !refFonts.isEmpty())
      {
 	    startElement(PROPERTY_ELEMENT);
        generateElement(NAME_ELEMENT, FONTS);
        startElement(VALUES_ELEMENT, KeyValue.with(ARITY, "List"), KeyValue.with(TYPE, "Property"));

        for (String f : embeddedFonts)
        {
      	  startElement(PROPERTY_ELEMENT);
          generateElement(NAME_ELEMENT, FONT);
          startElement(VALUES_ELEMENT, KeyValue.with(ARITY, "List"), KeyValue.with(TYPE, "Property"));
          generateProperty(FONT_NAME, getNameFromPath(f), "String");
          generateProperty(FONT_FILE, true);
          endElement(VALUES_ELEMENT);
          endElement(PROPERTY_ELEMENT);
        }
        for (String f : refFonts)
        {
          startElement(PROPERTY_ELEMENT);
          generateElement(NAME_ELEMENT, FONT);
          startElement(VALUES_ELEMENT, KeyValue.with(ARITY, "List"), KeyValue.with(TYPE, "Property"));
          generateProperty(FONT_NAME, getNameFromPath(f), "String");
          generateProperty(FONT_FILE, false);
          endElement(VALUES_ELEMENT);
          endElement(PROPERTY_ELEMENT);
        }
        
        endElement(VALUES_ELEMENT);
        endElement(PROPERTY_ELEMENT);
      }

      if (!references.isEmpty())
      {
    	startElement(PROPERTY_ELEMENT);
    	generateElement(NAME_ELEMENT, REFERENCES);
    	startElement(VALUES_ELEMENT, KeyValue.with(ARITY, "List"), KeyValue.with(TYPE, "Property"));
        for (String r : references)
        {
          generateProperty(REFERENCE, r, "String");
        }
        endElement(VALUES_ELEMENT);
        endElement(PROPERTY_ELEMENT);
      }
      if (!mediaTypes.isEmpty())
      {
          String[] cs = mediaTypes.toArray(new String[mediaTypes.size()]);
          generateProperty("MediaTypes", cs, "String");
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
	startElement(PROPERTY_ELEMENT);
    generateElement(NAME_ELEMENT, name);
    startElement(VALUES_ELEMENT, KeyValue.with(ARITY, value.length == 1 ? "Scalar" : "Array"), KeyValue.with(TYPE, type));
    for (String v : value)
    {
      generateElement(VALUE_ELEMENT, v);
    }
    endElement(VALUES_ELEMENT);
    endElement(PROPERTY_ELEMENT);
  }

  @SuppressWarnings("unchecked")
  private void generateProperty(String name, String value, String type)
  {
    if (value == null || value.trim().length() == 0)
    {
      return;
    }
	startElement(PROPERTY_ELEMENT);
    generateElement(NAME_ELEMENT, name);
    startElement(VALUES_ELEMENT, KeyValue.with(ARITY, "Scalar"), KeyValue.with(TYPE, type));
    generateElement(VALUE_ELEMENT, value);
    endElement(VALUES_ELEMENT);
    endElement(PROPERTY_ELEMENT);
  }

  private void generateProperty(String name, long value)
  {
    if (value == 0)
    {
      return;
    }
    generateProperty(name, Long.toString(value), "Long");
  }

  private void generateProperty(String name, boolean value)
  {
    generateProperty(name, value ? TRUE : FALSE, "Boolean");
  }

}

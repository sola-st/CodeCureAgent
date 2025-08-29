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
  private static final String FORMAT_APPLICATION_OCTET_STREAM = "application/octet-stream";
  private static final String NAME_JHOVE = "jhove";
  private static final String NAME_DATE = "date";
  private static final String NAME_REPINFO = "repInfo";
  private static final String ATTR_URI = "uri";
  private static final String NAME_CREATED = "created";
  private static final String NAME_LAST_MODIFIED = "lastModified";
  private static final String NAME_FORMAT = "format";
  private static final String NAME_VERSION = "version";
  private static final String NAME_CUSTOM_MESSAGE_FILE_NAME = "customMessageFileName";
  private static final String STATUS_WELL_FORMED = "Well-formed";
  private static final String STATUS_NOT_WELL_FORMED = "Not well-formed";
  private static final String NAME_STATUS = "status";
  private static final String NAME_MESSAGES = "messages";
  private static final String NAME_MESSAGE = "message";
  private static final String ATTR_ID = "id";
  private static final String ATTR_SEVERITY = "severity";
  private static final String NAME_MIME_TYPE = "mimeType";
  private static final String NAME_PROPERTIES = "properties";
  private static final String NAME_PROPERTY = "property";
  private static final String NAME_NAME = "name";
  private static final String NAME_VALUES = "values";
  private static final String ATTR_ARITY = "arity";
  private static final String ATTR_TYPE = "type";
  private static final String TYPE_PROPERTY = "Property";
  private static final String ARITY_LIST = "List";
  private static final String ARITY_SCALAR = "Scalar";
  private static final String ARITY_ARRAY = "Array";
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
	  startElement(NAME_JHOVE, attrs);

	  generateElement(NAME_DATE, generationDate);
	  startElement(NAME_REPINFO, KeyValue.with(ATTR_URI, getEpubFileName()));
      generateElement(NAME_CREATED, creationDate);
      generateElement(NAME_LAST_MODIFIED, lastModifiedDate);
      if (formatName == null) {
        generateElement(NAME_FORMAT, FORMAT_APPLICATION_OCTET_STREAM);
      } else {
        generateElement(NAME_FORMAT, formatName); //application/epub+zip
      }
      generateElement(NAME_VERSION, formatVersion);
      String customMessageFileName = this.getCustomMessageFile();
      if (customMessageFileName != null && !customMessageFileName.isEmpty())
      {
        generateElement(NAME_CUSTOM_MESSAGE_FILE_NAME, customMessageFileName);
      }
      if (fatalErrors.isEmpty() && errors.isEmpty())
      {
        generateElement(NAME_STATUS, STATUS_WELL_FORMED);
      }
      else
      {
        generateElement(NAME_STATUS, STATUS_NOT_WELL_FORMED);
      }
      if (!warns.isEmpty() || !fatalErrors.isEmpty() || !errors.isEmpty() || !hints.isEmpty())
      {
        startElement(NAME_MESSAGES);
        for (CheckMessage c : fatalErrors) {
        	String m = c.getID() + ", FATAL, [" + c.getMessage() + "], ";
        	for (EPUBLocation ml : c.getLocations()) {
			  String loc = "";
			  if (ml.getLine() > 0 || ml.getColumn() > 0) {
				loc = " (" + ml.getLine() + "-" + ml.getColumn() + ")";
			  }
              generateElement(NAME_MESSAGE, m + PathUtil.removeWorkingDirectory(ml.getPath()) + loc,
            		  KeyValue.with(ATTR_ID, c.getID()), KeyValue.with(ATTR_SEVERITY, "error"));
        	}
        }
        for (CheckMessage c : errors) {
        	String m = c.getID() + ", ERROR, [" + c.getMessage() + "], ";
        	for (EPUBLocation ml : c.getLocations()) {
			  String loc = "";
			  if (ml.getLine() > 0 || ml.getColumn() > 0) {
				loc = " (" + ml.getLine() + "-" + ml.getColumn() + ")";
			  }
              generateElement(NAME_MESSAGE, m + PathUtil.removeWorkingDirectory(ml.getPath()) + loc,
            		  KeyValue.with(ATTR_ID, c.getID()), KeyValue.with(ATTR_SEVERITY, "error"));
        	}
        }
        for (CheckMessage c : warns) {
        	String m = c.getID() + ", WARN, [" + c.getMessage() + "], ";
        	for (EPUBLocation ml : c.getLocations()) {
			  String loc = "";
			  if (ml.getLine() > 0 || ml.getColumn() > 0) {
				loc = " (" + ml.getLine() + "-" + ml.getColumn() + ")";
			  }
              generateElement(NAME_MESSAGE, m + PathUtil.removeWorkingDirectory(ml.getPath()) + loc,
            		  KeyValue.with(ATTR_ID, c.getID()), KeyValue.with(ATTR_SEVERITY, "warning"));
        	}
        }
        for (CheckMessage c : hints) {
        	String m = c.getID() + ", HINT, [" + c.getMessage() + "], ";
        	for (EPUBLocation ml : c.getLocations()) {
			  String loc = "";
			  if (ml.getLine() > 0 || ml.getColumn() > 0) {
				loc = " (" + ml.getLine() + "-" + ml.getColumn() + ")";
			  }
              generateElement(NAME_MESSAGE, m + PathUtil.removeWorkingDirectory(ml.getPath()) + loc,
            		  KeyValue.with(ATTR_ID, c.getID()), KeyValue.with(ATTR_SEVERITY, "info"));
        	}
        }
        endElement(NAME_MESSAGES);
      }
      generateElement(NAME_MIME_TYPE, formatName);
      startElement(NAME_PROPERTIES);

      generateProperty("FileName", getNameFromPath(getEpubFileName()), TYPE_STRING);
      generateProperty("PageCount", pagesCount);
      generateProperty("CharacterCount", charsCount);
      generateProperty("Language", language, TYPE_STRING);

  	  startElement(NAME_PROPERTY);
      generateElement(NAME_NAME, "Info");
      startElement(NAME_VALUES, KeyValue.with(ATTR_ARITY, ARITY_LIST), KeyValue.with(ATTR_TYPE, TYPE_PROPERTY));

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
      endElement(NAME_VALUES);
      endElement(NAME_PROPERTY);

      if (!embeddedFonts.isEmpty() || !refFonts.isEmpty())
      {
 	    startElement(NAME_PROPERTY);
        generateElement(NAME_NAME, "Fonts");
        startElement(NAME_VALUES, KeyValue.with(ATTR_ARITY, ARITY_LIST), KeyValue.with(ATTR_TYPE, TYPE_PROPERTY));

        for (String f : embeddedFonts)
        {
      	  startElement(NAME_PROPERTY);
          generateElement(NAME_NAME, "Font");
          startElement(NAME_VALUES, KeyValue.with(ATTR_ARITY, ARITY_LIST), KeyValue.with(ATTR_TYPE, TYPE_PROPERTY));
          generateProperty("FontName", getNameFromPath(f), TYPE_STRING);
          generateProperty("FontFile", true);
          endElement(NAME_VALUES);
          endElement(NAME_PROPERTY);
        }
        for (String f : refFonts)
        {
          startElement(NAME_PROPERTY);
          generateElement(NAME_NAME, "Font");
          startElement(NAME_VALUES, KeyValue.with(ATTR_ARITY, ARITY_LIST), KeyValue.with(ATTR_TYPE, TYPE_PROPERTY));
          generateProperty("FontName", getNameFromPath(f), TYPE_STRING);
          generateProperty("FontFile", false);
          endElement(NAME_VALUES);
          endElement(NAME_PROPERTY);
        }
        
        endElement(NAME_VALUES);
        endElement(NAME_PROPERTY);
      }

      if (!references.isEmpty())
      {
    	startElement(NAME_PROPERTY);
    	generateElement(NAME_NAME, "References");
    	startElement(NAME_VALUES, KeyValue.with(ATTR_ARITY, ARITY_LIST), KeyValue.with(ATTR_TYPE, TYPE_PROPERTY));
        for (String r : references)
        {
          generateProperty("Reference", r, TYPE_STRING);
        }
        endElement(NAME_VALUES);
        endElement(NAME_PROPERTY);
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

      endElement(NAME_PROPERTIES);
      endElement(NAME_REPINFO);
      endElement(NAME_JHOVE);
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
	startElement(NAME_PROPERTY);
    generateElement(NAME_NAME, name);
    startElement(NAME_VALUES, KeyValue.with(ATTR_ARITY, value.length == 1 ? ARITY_SCALAR : ARITY_ARRAY), KeyValue.with(ATTR_TYPE, type));
    for (String v : value)
    {
      generateElement("value", v);
    }
    endElement(NAME_VALUES);
    endElement(NAME_PROPERTY);
  }

  @SuppressWarnings("unchecked")
  private void generateProperty(String name, String value, String type)
  {
    if (value == null || value.trim().length() == 0)
    {
      return;
    }
	startElement(NAME_PROPERTY);
    generateElement(NAME_NAME, name);
    startElement(NAME_VALUES, KeyValue.with(ATTR_ARITY, ARITY_SCALAR), KeyValue.with(ATTR_TYPE, type));
    generateElement("value", value);
    endElement(NAME_VALUES);
    endElement(NAME_PROPERTY);
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

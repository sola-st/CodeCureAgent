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
  private static final String FORMAT_DEFAULT = "application/octet-stream";
  private static final String STATUS_WELL_FORMED = "Well-formed";
  private static final String STATUS_NOT_WELL_FORMED = "Not well-formed";
  private static final String SEVERITY_ERROR = "error";
  private static final String SEVERITY_WARNING = "warning";
  private static final String SEVERITY_INFO = "info";
  private static final String ARITY_LIST = "List";
  private static final String TYPE_PROPERTY = "Property";
  private static final String ARITY_LIST_OR_SCALAR = "arity";
  private static final String TYPE_STRING = "String";
  private static final String TYPE_DATE = "Date";
  private static final String TYPE_LONG = "Long";
  private static final String TYPE_BOOLEAN = "Boolean";
  private static final String TRUE_STR = "true";
  private static final String FALSE_STR = "false";
  private static final String PROPERTY_NAME_INFO = "Info";
  private static final String PROPERTY_NAME_FONTS = "Fonts";
  private static final String PROPERTY_NAME_FONT = "Font";
  private static final String PROPERTY_NAME_NAME = "name";
  private static final String PROPERTY_NAME_VALUES = "values";
  private static final String PROPERTY_NAME_VALUE = "value";

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
	  startElement("jhove", attrs);

	  generateElement("date", generationDate);
	  startElement("repInfo", KeyValue.with("uri", getEpubFileName()));
      generateElement("created", creationDate);
      generateElement("lastModified", lastModifiedDate);
      if (formatName == null) {
        generateElement("format", FORMAT_DEFAULT);
      } else {
        generateElement("format", formatName); //application/epub+zip
      }
      generateElement("version", formatVersion);
      String customMessageFileName = this.getCustomMessageFile();
      if (customMessageFileName != null && !customMessageFileName.isEmpty())
      {
        generateElement("customMessageFileName", customMessageFileName);
      }
      if (fatalErrors.isEmpty() && errors.isEmpty())
      {
        generateElement("status", STATUS_WELL_FORMED);
      }
      else
      {
        generateElement("status", STATUS_NOT_WELL_FORMED);
      }
      if (!warns.isEmpty() || !fatalErrors.isEmpty() || !errors.isEmpty() || !hints.isEmpty())
      {
        startElement("messages");
        for (CheckMessage c : fatalErrors) {
        	String m = c.getID() + ", FATAL, [" + c.getMessage() + "], ";
        	for (EPUBLocation ml : c.getLocations()) {
			  String loc = "";
			  if (ml.getLine() > 0 || ml.getColumn() > 0) {
				loc = " (" + ml.getLine() + "-" + ml.getColumn() + ")";
			  }
              generateElement("message", m + PathUtil.removeWorkingDirectory(ml.getPath()) + loc,
            		  KeyValue.with("id", c.getID()), KeyValue.with("severity", SEVERITY_ERROR));
        	}
        }
        for (CheckMessage c : errors) {
        	String m = c.getID() + ", ERROR, [" + c.getMessage() + "], ";
        	for (EPUBLocation ml : c.getLocations()) {
			  String loc = "";
			  if (ml.getLine() > 0 || ml.getColumn() > 0) {
				loc = " (" + ml.getLine() + "-" + ml.getColumn() + ")";
			  }
              generateElement("message", m + PathUtil.removeWorkingDirectory(ml.getPath()) + loc,
            		  KeyValue.with("id", c.getID()), KeyValue.with("severity", SEVERITY_ERROR));
        	}
        }
        for (CheckMessage c : warns) {
        	String m = c.getID() + ", WARN, [" + c.getMessage() + "], ";
        	for (EPUBLocation ml : c.getLocations()) {
			  String loc = "";
			  if (ml.getLine() > 0 || ml.getColumn() > 0) {
				loc = " (" + ml.getLine() + "-" + ml.getColumn() + ")";
			  }
              generateElement("message", m + PathUtil.removeWorkingDirectory(ml.getPath()) + loc,
            		  KeyValue.with("id", c.getID()), KeyValue.with("severity", SEVERITY_WARNING));
        	}
        }
        for (CheckMessage c : hints) {
        	String m = c.getID() + ", HINT, [" + c.getMessage() + "], ";
        	for (EPUBLocation ml : c.getLocations()) {
			  String loc = "";
			  if (ml.getLine() > 0 || ml.getColumn() > 0) {
				loc = " (" + ml.getLine() + "-" + ml.getColumn() + ")";
			  }
              generateElement("message", m + PathUtil.removeWorkingDirectory(ml.getPath()) + loc,
            		  KeyValue.with("id", c.getID()), KeyValue.with("severity", SEVERITY_INFO));
        	}
        }
        endElement("messages");
      }
      generateElement("mimeType", formatName);
      startElement("properties");

      generateProperty("FileName", getNameFromPath(getEpubFileName()), TYPE_STRING);
      generateProperty("PageCount", pagesCount);
      generateProperty("CharacterCount", charsCount);
      generateProperty("Language", language, TYPE_STRING);

  	  startElement("property");
      generateElement("name", PROPERTY_NAME_INFO);
      startElement(PROPERTY_NAME_VALUES, KeyValue.with(ARITY_LIST_OR_SCALAR, ARITY_LIST), KeyValue.with("type", TYPE_PROPERTY));

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
      endElement(PROPERTY_NAME_VALUES);
      endElement("property");

      if (!embeddedFonts.isEmpty() || !refFonts.isEmpty())
      {
 	    startElement("property");
        generateElement("name", PROPERTY_NAME_FONTS);
        startElement(PROPERTY_NAME_VALUES, KeyValue.with(ARITY_LIST_OR_SCALAR, ARITY_LIST), KeyValue.with("type", TYPE_PROPERTY));

        for (String f : embeddedFonts)
        {
      	  startElement("property");
          generateElement("name", PROPERTY_NAME_FONT);
          startElement(PROPERTY_NAME_VALUES, KeyValue.with(ARITY_LIST_OR_SCALAR, ARITY_LIST), KeyValue.with("type", TYPE_PROPERTY));
          generateProperty("FontName", getNameFromPath(f), TYPE_STRING);
          generateProperty("FontFile", true);
          endElement(PROPERTY_NAME_VALUES);
          endElement("property");
        }
        for (String f : refFonts)
        {
          startElement("property");
          generateElement("name", PROPERTY_NAME_FONT);
          startElement(PROPERTY_NAME_VALUES, KeyValue.with(ARITY_LIST_OR_SCALAR, ARITY_LIST), KeyValue.with("type", TYPE_PROPERTY));
          generateProperty("FontName", getNameFromPath(f), TYPE_STRING);
          generateProperty("FontFile", false);
          endElement(PROPERTY_NAME_VALUES);
          endElement("property");
        }
        
        endElement(PROPERTY_NAME_VALUES);
        endElement("property");
      }

      if (!references.isEmpty())
      {
    	startElement("property");
    	generateElement("name", "References");
    	startElement(PROPERTY_NAME_VALUES, KeyValue.with(ARITY_LIST_OR_SCALAR, ARITY_LIST), KeyValue.with("type", TYPE_PROPERTY));
        for (String r : references)
        {
          generateProperty("Reference", r, TYPE_STRING);
        }
        endElement(PROPERTY_NAME_VALUES);
        endElement("property");
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

      endElement("properties");
      endElement("repInfo");
      endElement("jhove");
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
	startElement("property");
    generateElement("name", name);
    startElement("values", KeyValue.with(ARITY_LIST_OR_SCALAR, value.length == 1 ? "Scalar" : "Array"), KeyValue.with("type", type));
    for (String v : value)
    {
      generateElement("value", v);
    }
    endElement("values");
    endElement("property");
  }

  @SuppressWarnings("unchecked")
  private void generateProperty(String name, String value, String type)
  {
    if (value == null || value.trim().length() == 0)
    {
      return;
    }
	startElement("property");
    generateElement("name", name);
    startElement("values", KeyValue.with(ARITY_LIST_OR_SCALAR, "Scalar"), KeyValue.with("type", type));
    generateElement("value", value);
    endElement("values");
    endElement("property");
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
    generateProperty(name, value ? TRUE_STR : FALSE_STR, TYPE_BOOLEAN);
  }

}


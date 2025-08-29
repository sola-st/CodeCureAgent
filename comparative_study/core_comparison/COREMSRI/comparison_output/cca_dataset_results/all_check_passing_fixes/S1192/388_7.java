package com.adobe.epubcheck.util;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.adobe.epubcheck.api.EPUBLocation;
import com.adobe.epubcheck.reporting.CheckMessage;


public class XmlReportImpl extends XmlReportAbstract
{

  private static final String JHOVE_NAMESPACE = "http://schema.openpreservation.org/ois/xml/ns/jhove";
  private static final String XSI_PREFIX = "xsi";
  private static final String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";
  private static final String XSI_SCHEMA_LOCATION_ATTR = "xsi:schemaLocation";
  private static final String JHOVE_SCHEMA_LOCATION = "http://schema.openpreservation.org/ois/xml/ns/jhove https://schema.openpreservation.org/ois/xml/xsd/jhove/jhove.xsd";

  private static final String ELEMENT_JHOVE = "jhove";
  private static final String ELEMENT_DATE = "date";
  private static final String ELEMENT_REPINFO = "repInfo";
  private static final String ATTR_URI = "uri";
  private static final String ELEMENT_CREATED = "created";
  private static final String ELEMENT_LAST_MODIFIED = "lastModified";
  private static final String ELEMENT_FORMAT = "format";
  private static final String ELEMENT_VERSION = "version";
  private static final String ELEMENT_CUSTOM_MESSAGE_FILE_NAME = "customMessageFileName";
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
  private static final String ELEMENT_MIME_TYPE = "mimeType";
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
  private static final String VALUE_TRUE = "true";
  private static final String VALUE_FALSE = "false";

  private static final String PROP_FILENAME = "FileName";
  private static final String PROP_PAGECOUNT = "PageCount";
  private static final String PROP_CHARACTERCOUNT = "CharacterCount";
  private static final String PROP_LANGUAGE = "Language";
  private static final String PROP_INFO = "Info";
  private static final String PROP_IDENTIFIER = "Identifier";
  private static final String PROP_CREATIONDATE = "CreationDate";
  private static final String PROP_MODDATE = "ModDate";
  private static final String PROP_TITLE = "Title";
  private static final String PROP_CREATOR = "Creator";
  private static final String PROP_CONTRIBUTOR = "Contributor";
  private static final String PROP_DATE = "Date";
  private static final String PROP_PUBLISHER = "Publisher";
  private static final String PROP_SUBJECT = "Subject";
  private static final String PROP_RIGHTS = "Rights";
  private static final String PROP_FONTS = "Fonts";
  private static final String PROP_FONT = "Font";
  private static final String PROP_FONTNAME = "FontName";
  private static final String PROP_FONTFILE = "FontFile";
  private static final String PROP_REFERENCES = "References";
  private static final String PROP_REFERENCE = "Reference";
  private static final String PROP_MEDIATYPES = "MediaTypes";

  private static final String PROP_HASENCRYPTION = "hasEncryption";
  private static final String PROP_HASSIGNATURES = "hasSignatures";
  private static final String PROP_HASAUDIO = "hasAudio";
  private static final String PROP_HASVIDEO = "hasVideo";
  private static final String PROP_HASFIXEDLAYOUT = "hasFixedLayout";
  private static final String PROP_HASSCRIPTS = "hasScripts";

  private static final String SEVERITY_FATAL = "FATAL";
  private static final String SEVERITY_ERROR_LABEL = "ERROR";
  private static final String SEVERITY_WARN = "WARN";
  private static final String SEVERITY_HINT = "HINT";

  private static final String COMMA_SPACE = ", ";
  private static final String COMMA_SPACE_OPEN_BRACKET = ", [";
  private static final String CLOSE_BRACKET_COMMA_SPACE = "], ";

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
      addPrefixNamespace(XSI_PREFIX, XSI_NAMESPACE);
	  List<KeyValue<String, String>> attrs = new ArrayList<KeyValue<String, String>>();
	  attrs.add(KeyValue.with("name", epubCheckName));
	  attrs.add(KeyValue.with("release", epubCheckVersion)); 
	  attrs.add(KeyValue.with("date", epubCheckDate));
	  attrs.add(KeyValue.with(XSI_SCHEMA_LOCATION_ATTR, JHOVE_SCHEMA_LOCATION));
	  startElement(ELEMENT_JHOVE, attrs);

	  generateElement(ELEMENT_DATE, generationDate);
	  startElement(ELEMENT_REPINFO, KeyValue.with(ATTR_URI, getEpubFileName()));
      generateElement(ELEMENT_CREATED, creationDate);
      generateElement(ELEMENT_LAST_MODIFIED, lastModifiedDate);
      if (formatName == null) {
        generateElement(ELEMENT_FORMAT, "application/octet-stream");
      } else {
        generateElement(ELEMENT_FORMAT, formatName); //application/epub+zip
      }
      generateElement(ELEMENT_VERSION, formatVersion);
      String customMessageFileName = this.getCustomMessageFile();
      if (customMessageFileName != null && !customMessageFileName.isEmpty())
      {
        generateElement(ELEMENT_CUSTOM_MESSAGE_FILE_NAME, customMessageFileName);
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
        	String m = c.getID() + COMMA_SPACE + SEVERITY_FATAL + COMMA_SPACE_OPEN_BRACKET + c.getMessage() + CLOSE_BRACKET_COMMA_SPACE;
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
        	String m = c.getID() + COMMA_SPACE + SEVERITY_ERROR_LABEL + COMMA_SPACE_OPEN_BRACKET + c.getMessage() + CLOSE_BRACKET_COMMA_SPACE;
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
        	String m = c.getID() + COMMA_SPACE + SEVERITY_WARN + COMMA_SPACE_OPEN_BRACKET + c.getMessage() + CLOSE_BRACKET_COMMA_SPACE;
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
        	String m = c.getID() + COMMA_SPACE + SEVERITY_HINT + COMMA_SPACE_OPEN_BRACKET + c.getMessage() + CLOSE_BRACKET_COMMA_SPACE;
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
      generateElement(ELEMENT_MIME_TYPE, formatName);
      startElement(ELEMENT_PROPERTIES);

      generateProperty(PROP_FILENAME, getNameFromPath(getEpubFileName()), TYPE_STRING);
      generateProperty(PROP_PAGECOUNT, pagesCount);
      generateProperty(PROP_CHARACTERCOUNT, charsCount);
      generateProperty(PROP_LANGUAGE, language, TYPE_STRING);

  	  startElement(ELEMENT_PROPERTY);
      generateElement(ELEMENT_NAME, PROP_INFO);
      startElement(ELEMENT_VALUES, KeyValue.with(ATTR_ARITY, ARITY_LIST), KeyValue.with(ATTR_TYPE, TYPE_PROPERTY));

      generateProperty(PROP_IDENTIFIER, identifier, TYPE_STRING);
      generateProperty(PROP_CREATIONDATE, creationDate, TYPE_DATE);
      generateProperty(PROP_MODDATE, lastModifiedDate, TYPE_DATE);

      if (!titles.isEmpty())
      {
          String[] cs = titles.toArray(new String[titles.size()]);
          generateProperty(PROP_TITLE, cs, TYPE_STRING);
      }
      if (!creators.isEmpty())
      {
        String[] cs = creators.toArray(new String[creators.size()]);
        generateProperty(PROP_CREATOR, cs, TYPE_STRING);
      }
      if (!contributors.isEmpty())
      {
        String[] cs = contributors.toArray(new String[contributors.size()]);
        generateProperty(PROP_CONTRIBUTOR, cs, TYPE_STRING);
      }
      generateProperty(PROP_DATE, date, TYPE_STRING);
      generateProperty(PROP_PUBLISHER, publisher, TYPE_STRING);
      if (!subjects.isEmpty())
      {
        String[] cs = subjects.toArray(new String[subjects.size()]);
        generateProperty(PROP_SUBJECT, cs, TYPE_STRING);
      }
      if (!rights.isEmpty())
      {
        String[] cs = rights.toArray(new String[rights.size()]);
        generateProperty(PROP_RIGHTS, cs, TYPE_STRING);
      }
      endElement(ELEMENT_VALUES);
      endElement(ELEMENT_PROPERTY);

      if (!embeddedFonts.isEmpty() || !refFonts.isEmpty())
      {
 	    startElement(ELEMENT_PROPERTY);
        generateElement(ELEMENT_NAME, PROP_FONTS);
        startElement(ELEMENT_VALUES, KeyValue.with(ATTR_ARITY, ARITY_LIST), KeyValue.with(ATTR_TYPE, TYPE_PROPERTY));

        for (String f : embeddedFonts)
        {
      	  startElement(ELEMENT_PROPERTY);
          generateElement(ELEMENT_NAME, PROP_FONT);
          startElement(ELEMENT_VALUES, KeyValue.with(ATTR_ARITY, ARITY_LIST), KeyValue.with(ATTR_TYPE, TYPE_PROPERTY));
          generateProperty(PROP_FONTNAME, getNameFromPath(f), TYPE_STRING);
          generateProperty(PROP_FONTFILE, true);
          endElement(ELEMENT_VALUES);
          endElement(ELEMENT_PROPERTY);
        }
        for (String f : refFonts)
        {
          startElement(ELEMENT_PROPERTY);
          generateElement(ELEMENT_NAME, PROP_FONT);
          startElement(ELEMENT_VALUES, KeyValue.with(ATTR_ARITY, ARITY_LIST), KeyValue.with(ATTR_TYPE, TYPE_PROPERTY));
          generateProperty(PROP_FONTNAME, getNameFromPath(f), TYPE_STRING);
          generateProperty(PROP_FONTFILE, false);
          endElement(ELEMENT_VALUES);
          endElement(ELEMENT_PROPERTY);
        }
        
        endElement(ELEMENT_VALUES);
        endElement(ELEMENT_PROPERTY);
      }

      if (!references.isEmpty())
      {
    	startElement(ELEMENT_PROPERTY);
    	generateElement(ELEMENT_NAME, PROP_REFERENCES);
    	startElement(ELEMENT_VALUES, KeyValue.with(ATTR_ARITY, ARITY_LIST), KeyValue.with(ATTR_TYPE, TYPE_PROPERTY));
        for (String r : references)
        {
          generateProperty(PROP_REFERENCE, r, TYPE_STRING);
        }
        endElement(ELEMENT_VALUES);
        endElement(ELEMENT_PROPERTY);
      }
      if (!mediaTypes.isEmpty())
      {
          String[] cs = mediaTypes.toArray(new String[mediaTypes.size()]);
          generateProperty(PROP_MEDIATYPES, cs, TYPE_STRING);
      }

      if (hasEncryption)
      {
        generateProperty(PROP_HASENCRYPTION, hasEncryption);
      }
      if (hasSignatures)
      {
        generateProperty(PROP_HASSIGNATURES, hasSignatures);
      }
      if (hasAudio)
      {
        generateProperty(PROP_HASAUDIO, hasAudio);
      }
      if (hasVideo)
      {
        generateProperty(PROP_HASVIDEO, hasVideo);
      }
      if (hasFixedLayout)
      {
        generateProperty(PROP_HASFIXEDLAYOUT, hasFixedLayout);
      }
      if (hasScripts)
      {
        generateProperty(PROP_HASSCRIPTS, hasScripts);
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
    generateProperty(name, value ? VALUE_TRUE : VALUE_FALSE, TYPE_BOOLEAN);
  }

}

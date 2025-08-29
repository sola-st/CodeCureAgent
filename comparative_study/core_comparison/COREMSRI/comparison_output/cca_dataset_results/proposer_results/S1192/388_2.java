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
  private static final String XSI_PREFIX = "xsi";
  private static final String XSI_SCHEMA_INSTANCE = "http://www.w3.org/2001/XMLSchema-instance";
  private static final String XSI_SCHEMA_LOCATION =
      "http://schema.openpreservation.org/ois/xml/ns/jhove https://schema.openpreservation.org/ois/xml/xsd/jhove/jhove.xsd";

  private static final String ELEMENT_JHOVE = "jhove";
  private static final String ELEMENT_DATE = "date";
  private static final String ELEMENT_REPINFO = "repInfo";
  private static final String ATTR_NAME = "name";
  private static final String ATTR_RELEASE = "release";
  private static final String ATTR_DATE = "date";
  private static final String ATTR_SCHEMA_LOCATION = "xsi:schemaLocation";

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

  private static final String ELEMENT_MIMETYPE = "mimeType";
  private static final String ELEMENT_PROPERTIES = "properties";
  private static final String ELEMENT_PROPERTY = "property";
  private static final String ELEMENT_VALUES = "values";
  private static final String ELEMENT_VALUE = "value";

  private static final String ARITY_LIST = "List";
  private static final String ARITY_SCALAR = "Scalar";
  private static final String ARITY_ARRAY = "Array";

  private static final String TYPE_PROPERTY = "Property";
  private static final String TYPE_STRING = "String";
  private static final String TYPE_DATE = "Date";
  private static final String TYPE_LONG = "Long";
  private static final String TYPE_BOOLEAN = "Boolean";

  private static final String PROPERTY_FILENAME = "FileName";
  private static final String PROPERTY_PAGECOUNT = "PageCount";
  private static final String PROPERTY_CHARACTERCOUNT = "CharacterCount";
  private static final String PROPERTY_LANGUAGE = "Language";
  private static final String PROPERTY_INFO = "Info";
  private static final String PROPERTY_IDENTIFIER = "Identifier";
  private static final String PROPERTY_CREATIONDATE = "CreationDate";
  private static final String PROPERTY_MODDATE = "ModDate";
  private static final String PROPERTY_TITLE = "Title";
  private static final String PROPERTY_CREATOR = "Creator";
  private static final String PROPERTY_CONTRIBUTOR = "Contributor";
  private static final String PROPERTY_DATE = "Date";
  private static final String PROPERTY_PUBLISHER = "Publisher";
  private static final String PROPERTY_SUBJECT = "Subject";
  private static final String PROPERTY_RIGHTS = "Rights";
  private static final String PROPERTY_FONTS = "Fonts";
  private static final String PROPERTY_FONT = "Font";
  private static final String PROPERTY_FONTNAME = "FontName";
  private static final String PROPERTY_FONTFILE = "FontFile";
  private static final String PROPERTY_REFERENCES = "References";
  private static final String PROPERTY_REFERENCE = "Reference";
  private static final String PROPERTY_MEDIATYPES = "MediaTypes";
  private static final String PROPERTY_HASENCRYPTION = "hasEncryption";
  private static final String PROPERTY_HASSIGNATURES = "hasSignatures";
  private static final String PROPERTY_HASAUDIO = "hasAudio";
  private static final String PROPERTY_HASVIDEO = "hasVideo";
  private static final String PROPERTY_HASFIXEDLAYOUT = "hasFixedLayout";
  private static final String PROPERTY_HASSCRIPTS = "hasScripts";

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
      addPrefixNamespace(XSI_PREFIX, XSI_SCHEMA_INSTANCE);
      List<KeyValue<String, String>> attrs = new ArrayList<KeyValue<String, String>>();
      attrs.add(KeyValue.with(ATTR_NAME, epubCheckName));
      attrs.add(KeyValue.with(ATTR_RELEASE, epubCheckVersion));
      attrs.add(KeyValue.with(ATTR_DATE, epubCheckDate));
      attrs.add(KeyValue.with(ATTR_SCHEMA_LOCATION, XSI_SCHEMA_LOCATION));
      startElement(ELEMENT_JHOVE, attrs);

      generateElement(ELEMENT_DATE, generationDate);
      startElement(ELEMENT_REPINFO, KeyValue.with("uri", getEpubFileName()));
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

      generateProperty(PROPERTY_FILENAME, getNameFromPath(getEpubFileName()), TYPE_STRING);
      generateProperty(PROPERTY_PAGECOUNT, pagesCount);
      generateProperty(PROPERTY_CHARACTERCOUNT, charsCount);
      generateProperty(PROPERTY_LANGUAGE, language, TYPE_STRING);

      startElement(ELEMENT_PROPERTY);
      generateElement("name", PROPERTY_INFO);
      startElement(ELEMENT_VALUES, KeyValue.with("arity", ARITY_LIST), KeyValue.with("type", TYPE_PROPERTY));

      generateProperty(PROPERTY_IDENTIFIER, identifier, TYPE_STRING);
      generateProperty(PROPERTY_CREATIONDATE, creationDate, TYPE_DATE);
      generateProperty(PROPERTY_MODDATE, lastModifiedDate, TYPE_DATE);

      if (!titles.isEmpty())
      {
        String[] cs = titles.toArray(new String[titles.size()]);
        generateProperty(PROPERTY_TITLE, cs, TYPE_STRING);
      }
      if (!creators.isEmpty())
      {
        String[] cs = creators.toArray(new String[creators.size()]);
        generateProperty(PROPERTY_CREATOR, cs, TYPE_STRING);
      }
      if (!contributors.isEmpty())
      {
        String[] cs = contributors.toArray(new String[contributors.size()]);
        generateProperty(PROPERTY_CONTRIBUTOR, cs, TYPE_STRING);
      }
      generateProperty(PROPERTY_DATE, date, TYPE_STRING);
      generateProperty(PROPERTY_PUBLISHER, publisher, TYPE_STRING);
      if (!subjects.isEmpty())
      {
        String[] cs = subjects.toArray(new String[subjects.size()]);
        generateProperty(PROPERTY_SUBJECT, cs, TYPE_STRING);
      }
      if (!rights.isEmpty())
      {
        String[] cs = rights.toArray(new String[rights.size()]);
        generateProperty(PROPERTY_RIGHTS, cs, TYPE_STRING);
      }
      endElement(ELEMENT_VALUES);
      endElement(ELEMENT_PROPERTY);

      if (!embeddedFonts.isEmpty() || !refFonts.isEmpty())
      {
        startElement(ELEMENT_PROPERTY);
        generateElement("name", PROPERTY_FONTS);
        startElement(ELEMENT_VALUES, KeyValue.with("arity", ARITY_LIST), KeyValue.with("type", TYPE_PROPERTY));

        for (String f : embeddedFonts)
        {
          startElement(ELEMENT_PROPERTY);
          generateElement("name", PROPERTY_FONT);
          startElement(ELEMENT_VALUES, KeyValue.with("arity", ARITY_LIST), KeyValue.with("type", TYPE_PROPERTY));
          generateProperty(PROPERTY_FONTNAME, getNameFromPath(f), TYPE_STRING);
          generateProperty(PROPERTY_FONTFILE, true);
          endElement(ELEMENT_VALUES);
          endElement(ELEMENT_PROPERTY);
        }
        for (String f : refFonts)
        {
          startElement(ELEMENT_PROPERTY);
          generateElement("name", PROPERTY_FONT);
          startElement(ELEMENT_VALUES, KeyValue.with("arity", ARITY_LIST), KeyValue.with("type", TYPE_PROPERTY));
          generateProperty(PROPERTY_FONTNAME, getNameFromPath(f), TYPE_STRING);
          generateProperty(PROPERTY_FONTFILE, false);
          endElement(ELEMENT_VALUES);
          endElement(ELEMENT_PROPERTY);
        }

        endElement(ELEMENT_VALUES);
        endElement(ELEMENT_PROPERTY);
      }

      if (!references.isEmpty())
      {
        startElement(ELEMENT_PROPERTY);
        generateElement("name", PROPERTY_REFERENCES);
        startElement(ELEMENT_VALUES, KeyValue.with("arity", ARITY_LIST), KeyValue.with("type", TYPE_PROPERTY));
        for (String r : references)
        {
          generateProperty(PROPERTY_REFERENCE, r, TYPE_STRING);
        }
        endElement(ELEMENT_VALUES);
        endElement(ELEMENT_PROPERTY);
      }
      if (!mediaTypes.isEmpty())
      {
        String[] cs = mediaTypes.toArray(new String[mediaTypes.size()]);
        generateProperty(PROPERTY_MEDIATYPES, cs, TYPE_STRING);
      }

      if (hasEncryption)
      {
        generateProperty(PROPERTY_HASENCRYPTION, hasEncryption);
      }
      if (hasSignatures)
      {
        generateProperty(PROPERTY_HASSIGNATURES, hasSignatures);
      }
      if (hasAudio)
      {
        generateProperty(PROPERTY_HASAUDIO, hasAudio);
      }
      if (hasVideo)
      {
        generateProperty(PROPERTY_HASVIDEO, hasVideo);
      }
      if (hasFixedLayout)
      {
        generateProperty(PROPERTY_HASFIXEDLAYOUT, hasFixedLayout);
      }
      if (hasScripts)
      {
        generateProperty(PROPERTY_HASSCRIPTS, hasScripts);
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
    generateElement("name", name);
    startElement(ELEMENT_VALUES, KeyValue.with("arity", value.length == 1 ? ARITY_SCALAR : ARITY_ARRAY), KeyValue.with("type", type));
    for (String v : value)
    {
      generateElement(ELEMENT_VALUE, v);
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
    generateElement("name", name);
    startElement(ELEMENT_VALUES, KeyValue.with("arity", ARITY_SCALAR), KeyValue.with("type", type));
    generateElement(ELEMENT_VALUE, value);
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

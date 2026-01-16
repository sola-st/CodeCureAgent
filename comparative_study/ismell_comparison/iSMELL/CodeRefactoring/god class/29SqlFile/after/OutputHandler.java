package org.hsqldb.cmdline;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.logging.Level;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import org.hsqldb.lib.AppendableException;
import org.hsqldb.lib.RCData;
import org.hsqldb.lib.StringUtil;
import org.hsqldb.lib.FrameworkLogger;
import org.hsqldb.cmdline.sqltool.Token;
import org.hsqldb.cmdline.sqltool.TokenList;
import org.hsqldb.cmdline.sqltool.TokenSource;
import org.hsqldb.cmdline.sqltool.SqlFileScanner;

class OutputHandler {
    private PrintStream stdOut;
    private PrintWriter queryOut;
    private boolean htmlMode;

    private void stdprint(String s) {
        stdprint(s, false);
    }

    private void stdprint(String s, boolean queryOutput) {
        if (shared.psStd != null)
            shared.psStd.print(htmlMode ? ("<P>" + s + "</P>") : s);

        if (queryOutput && pwQuery != null) {
            pwQuery.print(htmlMode ? ("<P>" + s + "</P>") : s);
            pwQuery.flush();
        }
    }

    private void stdprintln(String s) {
        stdprintln(s, false);
    }

    private void stdprintln(boolean queryOutput) {
        if (shared.psStd != null) if (htmlMode) {
            shared.psStd.println("<BR>");
        } else {
            shared.psStd.println();
        }

        if (queryOutput && pwQuery != null) {
            if (htmlMode) {
                pwQuery.println("<BR>");
            } else {
                pwQuery.println();
            }

            pwQuery.flush();
        }
    }

    private void stdprintln(String s, boolean queryOutput) {
        shared.psStd.println(htmlMode ? ("<P>" + s + "</P>")
                               : s);

        if (queryOutput && pwQuery != null) {
            pwQuery.println(htmlMode ? ("<P>" + s + "</P>")
                                     : s);
            pwQuery.flush();
        }
    }

    private void errprintln(String s) {
        if (shared.psStd != null && htmlMode) {
            shared.psStd.println("<DIV style='color:white; background: red; "
                       + "font-weight: bold'>" + s + "</DIV>");
        } else {
            logger.privlog(Level.SEVERE, s, null, 5, SqlFile.class);
            /* Only consistent way we can log source location is to log
             * the caller of SqlFile.
             * This seems acceptable, since the location being reported
             * here is not the source of the problem anyways.  */
        }
    }

    private void displayResultSet(Statement statement, ResultSet r,
                                   int[] incCols,
                                   String filterString) throws SQLException,
                                   SqlToolError {
         java.sql.Timestamp ts;
         int dotAt;
         int                updateCount = (statement == null) ? -1
                                                              : statement
                                                                  .getUpdateCount();
         boolean            silent      = silentFetch;
         boolean            binary      = fetchBinary;
         Pattern            filter = null;
 
         silentFetch = false;
         fetchBinary = false;
 
         if (filterString != null) try {
             filter = Pattern.compile(filterString);
         } catch (PatternSyntaxException pse) {
             throw new SqlToolError(
                     SqltoolRB.regex_malformat.getString(pse.getMessage()));
         }
 
         if (excludeSysSchemas) {
             stdprintln(SqltoolRB.vendor_nosup_sysschemas.getString());
         }
 
         switch (updateCount) {
             case -1 :
                 if (r == null) {
                     stdprintln(SqltoolRB.noresult.getString(), true);
 
                     break;
                 }
 
                 ResultSetMetaData m        = r.getMetaData();
                 int               cols     = m.getColumnCount();
                 int               incCount = (incCols == null) ? cols
                                                                : incCols
                                                                    .length;
                 String            val;
                 List<String[]>    rows        = new ArrayList<String[]>();
                 String[]          headerArray = null;
                 String[]          fieldArray;
                 int[]             maxWidth = new int[incCount];
                 int               insi;
                 boolean           skip;
                 boolean           isValNull;
 
                 // STEP 1: GATHER DATA
                 if (!htmlMode) {
                     for (int i = 0; i < maxWidth.length; i++) {
                         maxWidth[i] = 0;
                     }
                 }
 
                 boolean[] rightJust = new boolean[incCount];
                 int[]     dataType  = new int[incCount];
                 boolean[] autonulls = new boolean[incCount];
 
                 insi        = -1;
                 headerArray = new String[incCount];
 
                 for (int i = 1; i <= cols; i++) {
                     if (incCols != null) {
                         skip = true;
 
                         for (int j = 0; j < incCols.length; j++) {
                             if (i == incCols[j]) {
                                 skip = false;
                             }
                         }
 
                         if (skip) {
                             continue;
                         }
                     }
 
                     headerArray[++insi] = m.getColumnLabel(i);
                     dataType[insi]      = m.getColumnType(i);
                     rightJust[insi]     = false;
                     autonulls[insi]     = true;
                     // This is what we want for java.sql.Types.ARRAY :
 
                     switch (dataType[insi]) {
                         case java.sql.Types.BIGINT :
                         case java.sql.Types.BIT :
                         case java.sql.Types.DECIMAL :
                         case java.sql.Types.DOUBLE :
                         case java.sql.Types.FLOAT :
                         case java.sql.Types.INTEGER :
                         case java.sql.Types.NUMERIC :
                         case java.sql.Types.REAL :
                         case java.sql.Types.SMALLINT :
                         case java.sql.Types.TINYINT :
                             rightJust[insi] = true;
                             break;
 
                         case java.sql.Types.VARBINARY :
                         case java.sql.Types.VARCHAR :
                         case java.sql.Types.BLOB :
                         case java.sql.Types.CLOB :
                         case java.sql.Types.LONGVARBINARY :
                         case java.sql.Types.LONGVARCHAR :
                             autonulls[insi] = false;
                             break;
                     }
 
                     if (htmlMode) {
                         continue;
                     }
 
                     if (headerArray[insi] != null
                             && headerArray[insi].length() > maxWidth[insi]) {
                         maxWidth[insi] = headerArray[insi].length();
                     }
                 }
 
                 boolean filteredOut;
 
                 while (r.next()) {
                     fieldArray  = new String[incCount];
                     insi        = -1;
                     filteredOut = filter != null;
 
                     for (int i = 1; i <= cols; i++) {
                         // This is the only case where we can save a data
                         // read by recognizing we don't need this datum early.
                         if (incCols != null) {
                             skip = true;
 
                             for (int incCol : incCols) {
                                 if (i == incCol) {
                                     skip = false;
                                 }
                             }
 
                             if (skip) {
                                 continue;
                             }
                         }
 
                         // This row may still be ditched, but it is now
                         // certain that we need to increment the fieldArray
                         // index.
                         ++insi;
 
                         if (!SqlFile.canDisplayType(dataType[insi])) {
                             binary = true;
                         }
 
                         val = null;
                         isValNull = true;
 
                         if (!binary) {
                             /*
                              * The special formatting for all time-related
                              * fields is because the most popular current
                              * databases are extremely inconsistent about
                              * what resolution is returned for the same types.
                              * In my experience so far, Dates MAY have
                              * resolution down to second, but only TIMESTAMPs
                              * support sub-second res. (and always can).
                              * On top of that there is no consistency across
                              * getObject().toString().  Oracle doesn't even
                              * implement it for their custom TIMESTAMP type.
                              */
                             switch (dataType[insi]) {
                                 case org.hsqldb.types.Types.SQL_TIMESTAMP_WITH_TIME_ZONE:
                                 case org.hsqldb.types.Types.SQL_TIME_WITH_TIME_ZONE:
                                 case java.sql.Types.TIMESTAMP:
                                 case java.sql.Types.DATE:
                                 case java.sql.Types.TIME:
                                     ts  = r.getTimestamp(i);
                                     isValNull = r.wasNull();
                                     val = ((ts == null) ? null : ts.toString());
                                     // Following block truncates non-zero
                                     // sub-seconds from time types OTHER than
                                     // TIMESTAMP.
                                     if (dataType[insi]
                                             != java.sql.Types.TIMESTAMP
                                             && dataType[insi]
                                             != org.hsqldb.types.Types.SQL_TIMESTAMP_WITH_TIME_ZONE
                                             && val != null) {
                                         dotAt = val.lastIndexOf('.');
                                         for (int z = dotAt + 1;
                                                 z < val.length(); z++) {
                                             if (val.charAt(z) != '0') {
                                                 dotAt = 0;
                                                 break;
                                             }
                                         }
                                         if (dotAt > 1) {
                                             val = val.substring(0, dotAt);
                                         }
                                     }
                                     break;
                                 default:
                                     val = r.getString(i);
                                     isValNull = r.wasNull();
 
                                     // If we tried to get a String but it
                                     // failed, try getting it with a String
                                     // Stream
                                     if (val == null) {
                                         try {
                                             val = streamToString(
                                                 r.getAsciiStream(i),
                                                 shared.encoding);
                                             isValNull = r.wasNull();
                                         } catch (Exception e) {
                                             // This isn't an error.
                                             // We are attempting to do a stream
                                             // fetch if-and-only-if the column
                                             // supports it.
                                         }
                                     }
                             }
                         }
 
                         if (binary || (val == null &&!isValNull)) {
                             if (pwDsv != null) {
                                 throw new SqlToolError(
                                         SqltoolRB.dsv_bincol.getString());
                             }
 
                             // DB has a value but we either explicitly want
                             // it as binary, or we failed to get it as String.
                             try {
                                 binBuffer =
                                     SqlFile.streamToBytes(r.getBinaryStream(i));
                                 isValNull = r.wasNull();
                             } catch (IOException ioe) {
                                 throw new SqlToolError(
                                     "Failed to read value using stream",
                                     ioe);
                             }
 
                             stdprintln(SqltoolRB.binbuf_write.getString(
                                        Integer.toString(binBuffer.length),
                                        headerArray[insi],
                                        SqlFile.sqlTypeToString(dataType[insi])
                                     ));
 
                             return;
                         }
 
                         if (excludeSysSchemas && val != null && i == 2) {
                             for (String oracleSysSchema : oracleSysSchemas) {
                                 if (val.equals(oracleSysSchema)) {
                                     filteredOut = true;
 
                                     break;
                                 }
                             }
                         }
 
                         shared.userVars.put("?",
                                 ((val == null) ? nullRepToken : val));
                         if (fetchingVar != null) {
                             shared.userVars.put(
                                     fetchingVar, shared.userVars.get("?"));
                             updateUserSettings();
 
                             fetchingVar = null;
                         }
 
                         if (silent) {
                             return;
                         }
 
                         // We do not omit rows here.  We collect information
                         // so we can make the decision after all rows are
                         // read in.
                         if (filter != null
                             && (val == null || filter.matcher(val).find())) {
                             filteredOut = false;
                         }
 
                         ///////////////////////////////
                         // A little tricky here.  fieldArray[] MUST get set.
                         if (val == null && pwDsv == null) {
                             if (dataType[insi] == java.sql.Types.VARCHAR) {
                                 fieldArray[insi] = (htmlMode ? "<I>null</I>"
                                                              : nullRepToken);
                             } else {
                                 fieldArray[insi] = "";
                             }
                         } else {
                             fieldArray[insi] = val;
                         }
 
                         ///////////////////////////////
                         if (htmlMode || pwDsv != null) {
                             continue;
                         }
 
                         if (fieldArray[insi].length() > maxWidth[insi]) {
                             maxWidth[insi] = fieldArray[insi].length();
                         }
                     }
 
                     if (!filteredOut) {
                         rows.add(fieldArray);
                     }
                 }
 
                 // STEP 2: DISPLAY DATA  (= 2a OR 2b)
                 // STEP 2a (Non-DSV)
                 if (pwDsv == null) {
                     condlPrintln("<TABLE border='1'>", true);
 
                     if (incCount > 1) {
                         condlPrint(SqlFile.htmlRow(COL_HEAD) + LS + PRE_TD, true);
 
                         for (int i = 0; i < headerArray.length; i++) {
                             condlPrint("<TD>" + headerArray[i] + "</TD>",
                                        true);
                             condlPrint(((i > 0) ? "  " : "")
                                     + ((i < headerArray.length - 1
                                         || rightJust[i])
                                        ? StringUtil.toPaddedString(
                                          headerArray[i], maxWidth[i],
                                          ' ', !rightJust[i])
                                        : headerArray[i])
                                     , false);
                         }
 
                         condlPrintln(LS + PRE_TR + "</TR>", true);
                         condlPrintln("", false);
 
                         if (!htmlMode) {
                             for (int i = 0; i < headerArray.length; i++) {
                                 condlPrint(((i > 0) ? "  "
                                                     : "") + SqlFile.divider(
                                                         maxWidth[i]), false);
                             }
 
                             condlPrintln("", false);
                         }
                     }
 
                     for (int i = 0; i < rows.size(); i++) {
                         condlPrint(SqlFile.htmlRow(((i % 2) == 0) ? COL_EVEN
                                                           : COL_ODD) + LS
                                                           + PRE_TD, true);
 
                         fieldArray = rows.get(i);
 
                         for (int j = 0; j < fieldArray.length; j++) {
                             condlPrint("<TD>" + fieldArray[j] + "</TD>",
                                        true);
                             condlPrint(((j > 0) ? "  " : "")
                                     + ((j < fieldArray.length - 1
                                         || rightJust[j])
                                        ? StringUtil.toPaddedString(
                                          fieldArray[j], maxWidth[j],
                                          ' ', !rightJust[j])
                                        : fieldArray[j])
                                     , false);
                         }
 
                         condlPrintln(LS + PRE_TR + "</TR>", true);
                         condlPrintln("", false);
                     }
 
                     condlPrintln("</TABLE>", true);
 
                     if (interactive && rows.size() != 1) {
                         stdprintln(LS + SqltoolRB.rows_fetched.getString(
                                 rows.size()), true);
                     }
 
                     condlPrintln("<HR>", true);
 
                     break;
                 }
 
                 // STEP 2b (DSV)
                 if (incCount > 0) {
                     for (int i = 0; i < headerArray.length; i++) {
                         dsvSafe(headerArray[i]);
                         pwDsv.print(headerArray[i]);
 
                         if (i < headerArray.length - 1) {
                             pwDsv.print(dsvColDelim);
                         }
                     }
 
                     pwDsv.print(dsvRowDelim);
                 }
 
                 for (String[] fArray : rows) {
                     for (int j = 0; j < fArray.length; j++) {
                         dsvSafe(fArray[j]);
                         pwDsv.print((fArray[j] == null)
                                     ? (autonulls[j] ? ""
                                                     : nullRepToken)
                                     : fArray[j]);
 
                         if (j < fArray.length - 1) {
                             pwDsv.print(dsvColDelim);
                         }
                     }
 
                     pwDsv.print(dsvRowDelim);
                 }
 
                 stdprintln(SqltoolRB.rows_fetched_dsv.getString(rows.size()));
                 // Undecided about whether should display row count here when
                 // in non-interactive mode
                 break;
 
             default :
                 shared.userVars.put("?", Integer.toString(updateCount));
                 if (fetchingVar != null) {
                     shared.userVars.put(fetchingVar, shared.userVars.get("?"));
                     updateUserSettings();
                     fetchingVar = null;
                 }
 
                 if (updateCount != 0 && interactive) {
                     stdprintln((updateCount == 1)
                         ? SqltoolRB.row_update_singular.getString()
                         : SqltoolRB.row_update_multiple.getString(updateCount));
                 }
                 break;
         }
    }

    private static String htmlRow(int colType) {
        switch (colType) {
            case COL_HEAD :
                return PRE_TR + "<TR style='font-weight: bold;'>";

            case COL_ODD :
                return PRE_TR
                       + "<TR style='background: #94d6ef; font: normal "
                       + "normal 10px/10px Arial, Helvitica, sans-serif;'>";

            case COL_EVEN :
                return PRE_TR
                       + "<TR style='background: silver; font: normal "
                       + "normal 10px/10px Arial, Helvitica, sans-serif;'>";
        }

        return null;
    }

    private static String divider(int len) {
        return (len > DIVIDER.length()) ? DIVIDER
                                        : DIVIDER.substring(0, len);
    }

    private void closeQueryOutputStream() {
        if (pwQuery == null) {
            return;
        }

        try {
            if (htmlMode) {
                pwQuery.println("</BODY></HTML>");
                pwQuery.flush();
            }
        } finally {
            try {
                pwQuery.close();
            } finally {
                pwQuery = null; // Encourage GC of buffers
            }
        }
    }

    private void condlPrintln(String s, boolean printHtml) {
        if ((printHtml &&!htmlMode) || (htmlMode &&!printHtml)) {
            return;
        }

        if (shared.psStd != null) shared.psStd.println(s);

        if (pwQuery != null) {
            pwQuery.println(s);
            pwQuery.flush();
        }
    }

    private void condlPrint(String s, boolean printHtml) {
        if ((printHtml &&!htmlMode) || (htmlMode &&!printHtml)) {
            return;
        }

        if (shared.psStd != null) shared.psStd.print(s);

        if (pwQuery != null) {
            pwQuery.print(s);
            pwQuery.flush();
        }
    }

    protected static void appendLine(StringBuffer sb, String s) {
        sb.append(s + LS);
    }
}
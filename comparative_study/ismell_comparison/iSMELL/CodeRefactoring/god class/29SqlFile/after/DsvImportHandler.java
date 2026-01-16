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

class DsvImportHandler {
    private Connection connection;
    private String dsvColDelim;
    private String dsvRowDelim;
    private String nullRepToken;
    private String dsvSkipPrefix;
    private String dsvConstCols;
    private String dsvSkipCols;
    private int dsvRecordsPerCommit;
    private String dsvTargetTable;
    private String dsvRejectFile;
    private String dsvRejectReport;
    private String encoding;

    private void updateUserSettings() {
        dsvSkipPrefix = SqlFile.convertEscapes(
                shared.userVars.get("*DSV_SKIP_PREFIX"));
        if (dsvSkipPrefix == null) {
            dsvSkipPrefix = DEFAULT_SKIP_PREFIX;
        }
        dsvSkipCols = shared.userVars.get("*DSV_SKIP_COLS");
        dsvTrimAll = Boolean.parseBoolean(
                shared.userVars.get("*DSV_TRIM_ALL"));
        dsvColDelim = SqlFile.convertEscapes(
                shared.userVars.get("*DSV_COL_DELIM"));
        if (dsvColDelim == null) {
            dsvColDelim = SqlFile.convertEscapes(
                    shared.userVars.get("*CSV_COL_DELIM"));
        }
        if (dsvColDelim == null) {
            dsvColDelim = DEFAULT_COL_DELIM;
        }
        dsvColSplitter = shared.userVars.get("*DSV_COL_SPLITTER");
        if (dsvColSplitter == null) {
            dsvColSplitter = DEFAULT_COL_SPLITTER;
        }

        dsvRowDelim = SqlFile.convertEscapes(
                shared.userVars.get("*DSV_ROW_DELIM"));
        if (dsvRowDelim == null) {
            dsvRowDelim = SqlFile.convertEscapes(
                    shared.userVars.get("*CSV_ROW_DELIM"));
        }
        if (dsvRowDelim == null) {
            dsvRowDelim = DEFAULT_ROW_DELIM;
        }
        dsvRowSplitter = shared.userVars.get("*DSV_ROW_SPLITTER");
        if (dsvRowSplitter == null) {
            dsvRowSplitter = DEFAULT_ROW_SPLITTER;
        }

        dsvTargetFile = shared.userVars.get("*DSV_TARGET_FILE");
        if (dsvTargetFile == null) {
            dsvTargetFile = shared.userVars.get("*CSV_FILEPATH");
        }
        dsvTargetTable = shared.userVars.get("*DSV_TARGET_TABLE");
        if (dsvTargetTable == null) {
            dsvTargetTable = shared.userVars.get("*CSV_TABLENAME");
            // This just for legacy variable name.
        }

        dsvConstCols = shared.userVars.get("*DSV_CONST_COLS");
        dsvRejectFile = shared.userVars.get("*DSV_REJECT_FILE");
        dsvRejectReport = shared.userVars.get("*DSV_REJECT_REPORT");
        if (shared.userVars.get("*DSV_RECORDS_PER_COMMIT") != null) try {
            dsvRecordsPerCommit = Integer.parseInt(
                    shared.userVars.get("*DSV_RECORDS_PER_COMMIT"));
        } catch (NumberFormatException nfe) {
            logger.error(SqltoolRB.reject_rpc.getString(
                    shared.userVars.get("*DSV_RECORDS_PER_COMMIT")));
            shared.userVars.remove("*DSV_REJECT_REPORT");
            dsvRecordsPerCommit = 0;
        }

        nullRepToken = shared.userVars.get("*NULL_REP_TOKEN");
        if (nullRepToken == null) {
            nullRepToken = shared.userVars.get("*CSV_NULL_REP");
        }
        if (nullRepToken == null) {
            nullRepToken = DEFAULT_NULL_REP;
        }
    }

    public void dsvSafe(String s) throws SqlToolError {
        if (pwDsv == null || dsvColDelim == null || dsvRowDelim == null
                || nullRepToken == null) {
            throw new RuntimeException(
                "Assertion failed.  \n"
                + "dsvSafe called when DSV settings are incomplete");
        }

        if (s == null) {
            return;
        }

        if (s.indexOf(dsvColDelim) > 0) {
            throw new SqlToolError(
                    SqltoolRB.dsv_coldelim_present.getString(dsvColDelim));
        }

        if (s.indexOf(dsvRowDelim) > 0) {
            throw new SqlToolError(
                    SqltoolRB.dsv_rowdelim_present.getString(dsvRowDelim));
        }

        if (s.trim().equals(nullRepToken)) {
            // The trim() is to avoid the situation where the contents of a
            // field "looks like" the null-rep token.
            throw new SqlToolError(
                    SqltoolRB.dsv_nullrep_present.getString(nullRepToken));
        }
    }

    public void importDsv(String filePath, String skipPrefix)
             throws SqlToolError {
         requireConnection();
         /* To make string comparisons, contains() methods, etc. a little
          * simpler and concise, just switch all column names to lower-case.
          * This is ok since we acknowledge up front that DSV import/export
          * assume no special characters or escaping in column names. */
         Matcher matcher;
         byte[] bfr  = null;
         File   dsvFile = new File(filePath);
         SortedMap<String, String> constColMap = null;
         if (dsvConstCols != null) {
             // We trim col. names, but not values.  Must allow users to
             // specify values as spaces, empty string, null.
             constColMap = new TreeMap<String, String>();
             for (String constPair : dsvConstCols.split(dsvColSplitter, -1)) {
                 matcher = nameValPairPattern.matcher(constPair);
                 if (!matcher.matches()) {
                     throw new SqlToolError(
                             SqltoolRB.dsv_constcols_nullcol.getString());
                 }
                 constColMap.put(matcher.group(1).toLowerCase(),
                         ((matcher.groupCount() < 2 || matcher.group(2) == null)
                         ? "" : matcher.group(2)));
             }
         }
         Set<String> skipCols = null;
         if (dsvSkipCols != null) {
             skipCols = new HashSet<String>();
             for (String skipCol : dsvSkipCols.split(dsvColSplitter, -1)) {
                 skipCols.add(skipCol.trim().toLowerCase());
             }
         }
 
         if (!dsvFile.canRead()) {
             throw new SqlToolError(SqltoolRB.file_readfail.getString(
                     dsvFile.toString()));
         }
 
         try {
             bfr = new byte[(int) dsvFile.length()];
         } catch (RuntimeException re) {
             throw new SqlToolError(SqltoolRB.read_toobig.getString(), re);
         }
 
         int bytesread = 0;
         int retval;
         InputStream is = null;
 
         try {
             is = new FileInputStream(dsvFile);
             while (bytesread < bfr.length &&
                     (retval = is.read(bfr, bytesread, bfr.length - bytesread))
                     > 0) {
                 bytesread += retval;
             }
 
         } catch (IOException ioe) {
             throw new SqlToolError(ioe);
         } finally {
             if (is != null) try {
                 is.close();
             } catch (IOException ioe) {
                 errprintln(
                         SqltoolRB.inputfile_closefail.getString() + ": " + ioe);
             } finally {
                 is = null;  // Encourage GC of buffers
             }
         }
         if (bytesread != bfr.length) {
             throw new SqlToolError(SqltoolRB.read_partial.getString(
                     bytesread, bfr.length));
         }
 
         String dateString;
         String[] lines = null;
 
         try {
             String string = new String(bfr, (shared.encoding == null)
                     ? DEFAULT_FILE_ENCODING : shared.encoding);
             lines = string.split(dsvRowSplitter, -1);
         } catch (UnsupportedEncodingException uee) {
             throw new SqlToolError(uee);
         } catch (RuntimeException re) {
             throw new SqlToolError(SqltoolRB.read_convertfail.getString(), re);
         }
 
         List<String> headerList = new ArrayList<String>();
         String    tableName = dsvTargetTable;
 
         // First read one until we get one header line
         int lineCount = 0;
         String trimmedLine = null;
         boolean switching = false;
         int headerOffset = 0;  //  Used to offset read-start of header record
         String curLine = "dummy"; // Val will be replaced 4 lines down
                                   // This is just to quiet compiler warning
 
         while (true) {
             if (lineCount >= lines.length)
                 throw new SqlToolError(SqltoolRB.dsv_header_none.getString());
             curLine = lines[lineCount++];
             trimmedLine = curLine.trim();
             if (trimmedLine.length() < 1
                     || (skipPrefix != null
                             && trimmedLine.startsWith(skipPrefix))) {
                 continue;
             }
             if (trimmedLine.startsWith("targettable=")) {
                 if (tableName == null) {
                     tableName = trimmedLine.substring(
                             "targettable=".length()).trim();
                 }
                 continue;
             }
             if (trimmedLine.equals("headerswitch{")) {
                 if (tableName == null) {
                     throw new SqlToolError(
                             SqltoolRB.dsv_header_noswitchtarg.getString(
                             lineCount));
                 }
                 switching = true;
                 continue;
             }
             if (trimmedLine.equals("}")) {
                 throw new SqlToolError(
                         SqltoolRB.dsv_header_noswitchmatch.getString(lineCount));
             }
             if (!switching) {
                 break;
             }
             int colonAt = trimmedLine.indexOf(':');
             if (colonAt < 1 || colonAt == trimmedLine.length() - 1) {
                 throw new SqlToolError(
                         SqltoolRB.dsv_header_nonswitched.getString(lineCount));
             }
             String headerName = trimmedLine.substring(0, colonAt).trim();
             // Need to be sure here that tableName is not null (in
             // which case it would be determined later on by the file name).
             if (headerName.equals("*")
                     || headerName.equalsIgnoreCase(tableName)){
                 headerOffset = 1 + curLine.indexOf(':');
                 break;
             }
             // Skip non-matched header line
         }
 
         String headerLine = curLine.substring(headerOffset);
         String colName;
         String[] cols = headerLine.split(dsvColSplitter, -1);
 
         for (String col : cols) {
             if (col.length() < 1) {
                 throw new SqlToolError(SqltoolRB.dsv_nocolheader.getString(
                         headerList.size() + 1, lineCount));
             }
 
             colName = col.trim().toLowerCase();
             headerList.add(
                 (colName.equals("-")
                         || (skipCols != null
                                 && skipCols.remove(colName))
                         || (constColMap != null
                                 && constColMap.containsKey(colName))
                 )
                 ? ((String) null)
                 : colName);
         }
         if (skipCols != null && skipCols.size() > 0) {
             throw new SqlToolError(SqltoolRB.dsv_skipcols_missing.getString(
                     skipCols.toString()));
         }
 
         boolean oneCol = false;  // At least 1 non-null column
         for (String header : headerList) {
             if (header != null) {
                 oneCol = true;
                 break;
             }
         }
         if (oneCol == false) {
             // Difficult call, but I think in any real-world situation, the
             // user will want to know if they are inserting records with no
             // data from their input file.
             throw new SqlToolError(
                     SqltoolRB.dsv_nocolsleft.getString(dsvSkipCols));
         }
 
         int inputColHeadCount = headerList.size();
 
         if (constColMap != null) {
             headerList.addAll(constColMap.keySet());
         }
 
         String[]  headers   = headerList.toArray(new String[0]);
         // headers contains input headers + all constCols, some of these
         // values may be nulls.
 
         if (tableName == null) {
             tableName = dsvFile.getName();
 
             int i = tableName.lastIndexOf('.');
 
             if (i > 0) {
                 tableName = tableName.substring(0, i);
             }
         }
 
         StringBuffer tmpSb = new StringBuffer();
         List<String> tmpList = new ArrayList<String>();
 
         int skippers = 0;
         for (String header : headers) {
             if (header == null) {
                 skippers++;
                 continue;
             }
             if (tmpSb.length() > 0) {
                 tmpSb.append(", ");
             }
 
             tmpSb.append(header);
             tmpList.add(header);
         }
         boolean[] autonulls = new boolean[headers.length - skippers];
         boolean[] parseDate = new boolean[autonulls.length];
         boolean[] parseBool = new boolean[autonulls.length];
         char[] readFormat = new char[autonulls.length];
         String[] insertFieldName = tmpList.toArray(new String[] {});
         // Remember that the headers array has all columns in DSV file,
         // even skipped columns.
         // The autonulls array only has columns that we will insert into.
 
         StringBuffer sb = new StringBuffer("INSERT INTO " + tableName + " ("
                                            + tmpSb + ") VALUES (");
         StringBuffer typeQuerySb = new StringBuffer("SELECT " + tmpSb
             + " FROM " + tableName + " WHERE 1 = 2");
 
         try {
             ResultSetMetaData rsmd =
                     shared.jdbcConn.createStatement().executeQuery(
                     typeQuerySb.toString()).getMetaData();
 
             if (rsmd.getColumnCount() != autonulls.length) {
                 throw new SqlToolError(
                         SqltoolRB.dsv_metadata_mismatch.getString());
                 // Don't know if it's possible to get here.
                 // If so, it's probably a SqlTool problem, not a user or
                 // data problem.
                 // Should be researched and either return a user-friendly
                 // message or a RuntimeExceptin.
             }
 
             for (int i = 0; i < autonulls.length; i++) {
                 autonulls[i] = true;
                 parseDate[i] = false;
                 parseBool[i] = false;
                 readFormat[i] = 's'; // regular Strings
                 switch(rsmd.getColumnType(i + 1)) {
                     case java.sql.Types.BIT :
                         autonulls[i] = true;
                         readFormat[i] = 'b';
                         break;
                     case java.sql.Types.LONGVARBINARY :
                     case java.sql.Types.VARBINARY :
                     case java.sql.Types.BINARY :
                         autonulls[i] = true;
                         readFormat[i] = 'x';
                         break;
                     case java.sql.Types.BOOLEAN:
                         parseBool[i] = true;
                         break;
                     case java.sql.Types.ARRAY :
                         autonulls[i] = true;
                         readFormat[i] = 'a';
                         break;
                     case java.sql.Types.VARCHAR :
                     case java.sql.Types.BLOB :
                     case java.sql.Types.CLOB :
                     case java.sql.Types.LONGVARCHAR :
                         autonulls[i] = false;
                         // This means to preserve white space and to insert
                         // "" for "".  Otherwise we trim white space and
                         // insert null for \s*.
                         break;
                     case java.sql.Types.DATE:
                     case java.sql.Types.TIME:
                     case java.sql.Types.TIMESTAMP:
                     case org.hsqldb.types.Types.SQL_TIMESTAMP_WITH_TIME_ZONE:
                     case org.hsqldb.types.Types.SQL_TIME_WITH_TIME_ZONE:
                         parseDate[i] = true;
                 }
             }
         } catch (SQLException se) {
             throw new SqlToolError(SqltoolRB.query_metadatafail.getString(
                     typeQuerySb.toString()), se);
         }
 
         for (int i = 0; i < autonulls.length; i++) {
             if (i > 0) {
                 sb.append(", ");
             }
 
             sb.append('?');
         }
 
         // Initialize REJECT file(s)
         int rejectCount = 0;
         File rejectFile = null;
         File rejectReportFile = null;
         PrintWriter rejectWriter = null;
         PrintWriter rejectReportWriter = null;
         try {
         if (dsvRejectFile != null) try {
             rejectFile = new File(dereferenceAt(dsvRejectFile));
             rejectWriter = new PrintWriter(
                     new OutputStreamWriter(new FileOutputStream(rejectFile),
                     (shared.encoding == null)
                     ? DEFAULT_FILE_ENCODING : shared.encoding));
             rejectWriter.print(headerLine + dsvRowDelim);
         } catch (BadSpecial bs) {
             throw new SqlToolError(SqltoolRB.dsv_rejectfile_setupfail.getString(
                     dsvRejectFile), bs);
         } catch (IOException ioe) {
             throw new SqlToolError(SqltoolRB.dsv_rejectfile_setupfail.getString(
                     dsvRejectFile), ioe);
         }
         if (dsvRejectReport != null) try {
             rejectReportFile = new File(dereferenceAt(dsvRejectReport));
             rejectReportWriter = new PrintWriter(new OutputStreamWriter(
                     new FileOutputStream(rejectReportFile),
                     (shared.encoding == null)
                     ? DEFAULT_FILE_ENCODING : shared.encoding));
             rejectReportWriter.println(SqltoolRB.rejectreport_top.getString(
                     (new java.util.Date()).toString(),
                     dsvFile.getPath(),
                     ((rejectFile == null) ? SqltoolRB.none.getString()
                                     : rejectFile.getPath()),
                     ((rejectFile == null) ? null : rejectFile.getPath())));
         } catch (BadSpecial bs) {
             throw new SqlToolError(
                     SqltoolRB.dsv_rejectreport_setupfail.getString(
                     dsvRejectReport), bs);
         } catch (IOException ioe) {
             throw new SqlToolError(
                     SqltoolRB.dsv_rejectreport_setupfail.getString(
                     dsvRejectReport), ioe);
         }
 
         int recCount = 0;
         int skipCount = 0;
         PreparedStatement ps = null;
         boolean importAborted = false;
         boolean doResetAutocommit = false;
         try {
             doResetAutocommit = dsvRecordsPerCommit > 0
                 && shared.jdbcConn.getAutoCommit();
             if (doResetAutocommit) shared.jdbcConn.setAutoCommit(false);
         } catch (SQLException se) {
             throw new SqlToolError(
                     SqltoolRB.rpc_autocommit_failure.getString(), se);
         }
         // We're now assured that if dsvRecordsPerCommit is > 0, then
         // autocommit is off.
 
         try {
             try {
                 ps = shared.jdbcConn.prepareStatement(sb.toString() + ')');
             } catch (SQLException se) {
                 throw new SqlToolError(SqltoolRB.insertion_preparefail.getString(
                         sb.toString()), se);
             }
             String[] dataVals = new String[autonulls.length];
             // Length is number of cols to insert INTO, not nec. # in DSV file.
             int      readColCount;
             int      storeColCount;
             Matcher  arMatcher;
             String   currentFieldName = null;
             String[] arVals;
 
             // Insert data rows 1-row-at-a-time
             while (lineCount < lines.length) try { try {
                 curLine = lines[lineCount++];
                 trimmedLine = curLine.trim();
                 if (trimmedLine.length() < 1) {
                     continue;  // Silently skip blank lines
                 }
                 if (skipPrefix != null
                         && trimmedLine.startsWith(skipPrefix)) {
                     skipCount++;
                     continue;
                 }
                 if (switching) {
                     if (trimmedLine.equals("}")) {
                         switching = false;
                         continue;
                     }
                     int colonAt = trimmedLine.indexOf(':');
                     if (colonAt < 1 || colonAt == trimmedLine.length() - 1) {
                         throw new SqlToolError(SqltoolRB.dsv_header_matchernonhead.getString(
                                         lineCount));
                     }
                     continue;
                 }
                 // Finished using "trimmed" line now.  Whitespace is
                 // meaningful hereafter.
 
                 // Finally we will attempt to add a record!
                 recCount++;
                 // Remember that recCount counts both inserts + rejects
 
                 readColCount = 0;
                 storeColCount = 0;
                 cols = curLine.split(dsvColSplitter, -1);
 
                 for (String col : cols) {
                     if (readColCount == inputColHeadCount) {
                         throw new RowError(SqltoolRB.dsv_colcount_mismatch.getString(
                                 inputColHeadCount, 1 + readColCount));
                     }
 
                     if (headers[readColCount++] != null) {
                         dataVals[storeColCount++] = dsvTrimAll ? col.trim() : col;
                     }
                 }
                 if (readColCount < inputColHeadCount) {
                     throw new RowError(SqltoolRB.dsv_colcount_mismatch.getString(
                             inputColHeadCount, readColCount));
                 }
                 /* Already checked for readColCount too high in prev. block */
 
                 if (constColMap != null) {
                     for (String val : constColMap.values()) {
                         dataVals[storeColCount++] = val;
                     }
                 }
                 if (storeColCount != dataVals.length) {
                     throw new RowError(SqltoolRB.dsv_insertcol_mismatch.getString(
                             dataVals.length, storeColCount));
                 }
 
                 for (int i = 0; i < dataVals.length; i++) {
                     currentFieldName = insertFieldName[i];
                     if (autonulls[i]) dataVals[i] = dataVals[i].trim();
                     // N.b. WE SPECIFICALLY DO NOT HANDLE TIMES WITHOUT
                     // DATES, LIKE "3:14:00", BECAUSE, WHILE THIS MAY BE
                     // USEFUL AND EFFICIENT, IT IS NOT PORTABLE.
                     //System.err.println("ps.setString(" + i + ", "
                     //      + dataVals[i] + ')');
 
                     if (parseDate[i]) {
                         if ((dataVals[i].length() < 1 && autonulls[i])
                               || dataVals[i].equals(nullRepToken)) {
                             ps.setTimestamp(i + 1, null);
                         } else {
                             dateString = (dataVals[i].indexOf(':') > 0)
                                        ? dataVals[i]
                                        : (dataVals[i] + " 0:00:00");
                             // BEWARE:  This may not work for some foreign
                             // date/time formats.
                             try {
                                 ps.setTimestamp(i + 1,
                                         java.sql.Timestamp.valueOf(dateString));
                             } catch (IllegalArgumentException iae) {
                                 throw new RowError(SqltoolRB.time_bad.getString(
                                         dateString), iae);
                             }
                         }
                     } else if (parseBool[i]) {
                         if ((dataVals[i].length() < 1 && autonulls[i])
                               || dataVals[i].equals(nullRepToken)) {
                             ps.setNull(i + 1, java.sql.Types.BOOLEAN);
                         } else {
                             try {
                                 ps.setBoolean(i + 1,
                                         Boolean.parseBoolean(dataVals[i]));
                                 // Boolean... is equivalent to Java 4's
                                 // Boolean.parseBoolean().
                             } catch (IllegalArgumentException iae) {
                                 throw new RowError(SqltoolRB.boolean_bad.getString(
                                         dataVals[i]), iae);
                             }
                         }
                     } else {
                         switch (readFormat[i]) {
                             case 'b':
                                 ps.setBytes(
                                     i + 1,
                                     (dataVals[i].length() < 1) ? null
                                     : SqlFile.bitCharsToBytes(
                                         dataVals[i]));
                                 break;
                             case 'x':
                                 ps.setBytes(
                                     i + 1,
                                     (dataVals[i].length() < 1) ? null
                                     : SqlFile.hexCharOctetsToBytes(
                                         dataVals[i]));
                                 break;
                             case 'a' :
                                 if (SqlFile.createArrayOfMethod == null) {
                                     throw new SqlToolError(
                                             //SqltoolRB.boolean_bad.getString(
                                         "SqlTool requires += Java 1.6 at "
                                         + "runtime in order to import Array "
                                         + "values");
                                 }
                                 if (dataVals[i].length() < 1) {
                                     ps.setArray(i + 1, null);
                                     break;
                                 }
                                 arMatcher = arrayPattern.matcher(dataVals[i]);
                                 if (!arMatcher.matches()) {
                                     throw new RowError(
                                             //SqltoolRB.boolean_bad.getString(
                                         "Malformatted ARRAY value: ("
                                         + dataVals[i] + ')');
                                 }
                                 arVals = (arMatcher.group(1) == null)
                                        ? (new String[0])
                                        : arMatcher.group(1).split("\\s*,\\s*");
                                 // N.b. THIS DOES NOT HANDLE commas WITHIN
                                 // Array ELEMENT VALUES.
                                 try {
                                     ps.setArray(i + 1, (java.sql.Array)
                                             SqlFile.createArrayOfMethod.invoke(
                                             shared.jdbcConn,
                                             "VARCHAR", arVals));
                                 } catch (IllegalAccessException iae) {
                                     throw new RuntimeException(iae);
                                 } catch (InvocationTargetException ite) {
                                     if (ite.getCause() != null
                                             &&  ite.getCause()
                                             instanceof AbstractMethodError) {
                                         throw new SqlToolError(
                                             //SqltoolRB.boolean_bad.getString(
                                             "SqlTool binary is not "
                                             + "Array-compatible with your "
                                             + "runtime JRE.  Array imports "
                                             + "not possible.");
                                     }
                                     throw new RuntimeException(ite);
                                 }
                                 // createArrayOf method is Java-6-specific!
                                 break;
                             default:
                                 ps.setString(
                                     i + 1,
                                     (((dataVals[i].length() < 1 && autonulls[i])
                                       || dataVals[i].equals(nullRepToken))
                                      ? null
                                      : dataVals[i]));
                         }
                     }
                     currentFieldName = null;
                 }
 
                 retval = ps.executeUpdate();
 
                 if (retval != 1) {
                     throw new RowError(
                             SqltoolRB.inputrec_modified.getString(retval));
                 }
 
                 if (dsvRecordsPerCommit > 0
                     && (recCount - rejectCount) % dsvRecordsPerCommit == 0) {
                     shared.jdbcConn.commit();
                     shared.possiblyUncommitteds = false;
                 } else {
                     shared.possiblyUncommitteds = true;
                 }
             } catch (NumberFormatException nfe) {
                 throw new RowError(null, nfe);
             } catch (SQLException se) {
                 throw new RowError(null, se);
             } } catch (RowError re) {
                 rejectCount++;
                 if (rejectWriter != null || rejectReportWriter != null) {
                     if (rejectWriter != null) {
                         rejectWriter.print(curLine + dsvRowDelim);
                     }
                     if (rejectReportWriter != null) {
                         genRejectReportRecord(rejectReportWriter,
                                 rejectCount, lineCount,
                                 currentFieldName, re.getMessage(),
                                 re.getCause());
                     }
                 } else {
                     importAborted = true;
                     throw new SqlToolError(
                             SqltoolRB.dsv_recin_fail.getString(
                                     lineCount, currentFieldName)
                             + ((re.getMessage() == null)
                                     ? "" : ("  " + re.getMessage())),
                             re.getCause());
                 }
             }
         } finally {
             if (ps != null) try {
                 ps.close();
             } catch (SQLException se) {
                 // We already got what we want from it, or have/are
                 // processing a more specific error.
             } finally {
                 ps = null;  // Encourage GC of buffers
             }
             try {
                 if (dsvRecordsPerCommit > 0
                     && (recCount - rejectCount) % dsvRecordsPerCommit != 0) {
                     // To be consistent, if *DSV_RECORDS_PER_COMMIT is set, we
                     // always commit all inserted records.
                     // This little block commits any straggler commits since the
                     // last commit.
                     shared.jdbcConn.commit();
                     shared.possiblyUncommitteds = false;
                 }
                 if (doResetAutocommit) shared.jdbcConn.setAutoCommit(true);
             } catch (SQLException se) {
                 throw new SqlToolError(
                         SqltoolRB.rpc_commit_failure.getString(), se);
             }
             String summaryString = null;
             if (recCount > 0) {
                 summaryString = SqltoolRB.dsv_import_summary.getString(
                         ((skipPrefix == null)
                                   ? "" : ("'" + skipPrefix + "'-")),
                         Integer.toString(skipCount),
                         Integer.toString(rejectCount),
                         Integer.toString(recCount - rejectCount),
                         (importAborted ? "importAborted" : null));
                 stdprintln(summaryString);
             }
             try {
                 if (recCount > rejectCount && dsvRecordsPerCommit < 1
                         && !shared.jdbcConn.getAutoCommit()) {
                     stdprintln(SqltoolRB.insertions_notcommitted.getString());
                 }
             } catch (SQLException se) {
                 stdprintln(SqltoolRB.autocommit_fetchfail.getString());
                 stdprintln(SqltoolRB.insertions_notcommitted.getString());
                 // No reason to throw here.  If user attempts to use the
                 // connection for anything significant, we will throw then.
             }
             if (rejectWriter != null) {
                 rejectWriter.flush();
             }
             if (rejectReportWriter != null && rejectCount > 0) {
                 rejectReportWriter.println(SqltoolRB.rejectreport_bottom.getString(
                         summaryString, revnum));
                 rejectReportWriter.flush();
             }
             if (rejectCount == 0) {
                 if (rejectFile != null && rejectFile.exists()
                         && !rejectFile.delete())
                     errprintln(SqltoolRB.dsv_rejectfile_purgefail.getString(
                             rejectFile.toString()));
                 if (rejectReportFile != null && !rejectReportFile.delete())
                     errprintln(SqltoolRB.dsv_rejectreport_purgefail.getString(
                             (rejectFile == null)
                                     ? null : rejectFile.toString()));
                 // These are trivial errors.
             }
         }
         } finally {
             if (rejectWriter != null) {
                 try {
                     rejectWriter.close();
                 } finally {
                     rejectWriter = null;  // Encourage GC of buffers
                 }
             }
             if (rejectReportWriter != null) {
                 try {
                     rejectReportWriter.close();
                 } finally {
                     rejectReportWriter = null;  // Encourage GC of buffers
                 }
             }
         }
    }

    private void genRejectReportRecord(PrintWriter pw, int rCount,
             int lCount, String field, String eMsg, Throwable cause) {
         pw.println(SqltoolRB.rejectreport_row.getString(
                 ((rCount % 2 == 0) ? "even" : "odd") + "row",
                 Integer.toString(rCount),
                 Integer.toString(lCount),
                 ((field == null) ? "&nbsp;" : field),
                 (((eMsg == null) ? "" : eMsg)
                         + ((eMsg == null || cause == null) ? "" : "<HR/>")
                         + ((cause == null) ? "" : (
                                 (cause instanceof SQLException
                                         && cause.getMessage() != null)
                                     ? cause.getMessage()
                                     : cause.toString()
                                 )
                         )
                 )));
    }
}
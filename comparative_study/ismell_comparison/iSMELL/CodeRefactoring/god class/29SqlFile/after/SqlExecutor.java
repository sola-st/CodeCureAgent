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

class SqlExecutor {
    private Connection connection;
    private boolean autoCommit;
    private boolean reportTimes;

    public void setContinueOnError(boolean continueOnError) {
        this.continueOnError = continueOnError;
    }

    synchronized public void execute() throws SqlToolError, SQLException {
        if (reader == null)
            throw new IllegalStateException("Can't call execute() "
                    + "more than once for a single SqlFile instance");

        try {
            scanner = new SqlFileScanner(reader);
            scanner.setStdPrintStream(shared.psStd);
            scanner.setRawLeadinPrompt(SqltoolRB.raw_leadin.getString());
            if (interactive) {
                stdprintln(SqltoolRB.SqlFile_banner.getString(revnum));
                scanner.setRawPrompt(rawPrompt);
                scanner.setSqlPrompt(contPrompt);
                scanner.setSqltoolPrompt(primaryPrompt);
                scanner.setInteractive(true);
                if (shared.jdbcConn == null)
                    stdprintln("To connect to a data source, use '\\j "
                        + "urlid' or '\\j account password jdbc:url...'");
                stdprint(primaryPrompt);
            }
            scanpass(scanner);
        } finally {
            try {
                closeQueryOutputStream();
                if (autoClose) closeReader();
            } finally {
                reader = null; // Encourage GC of buffers
            }
        }
    }

    synchronized protected void scanpass(TokenSource ts)
                                      throws SqlToolError, SQLException {
         boolean rollbackUncoms = true;
         String nestingCommand;
         Token token = null;
 
         if (shared.userVars.size() > 0) {
             plMode = true;
         }
 
         try {
             while (true) try {
                 if (preempt) {
                     token = buffer;
                     preempt = false;
                 } else {
                     token = ts.yylex();
                     logger.finest("SqlFile got new token:  " + token);
                 }
                 if (token == null) break;
 
                 nestingCommand = nestingCommand(token);
                 if (nestingCommand != null) {
                     if (token.nestedBlock == null) {
                         token.nestedBlock = seekTokenSource(nestingCommand);
                         /* This command (and the same recursive call inside
                          * of the seekTokenSource() method) ensure that all
                          * "blocks" are tokenized immediately as block
                          * commands are encountered, and the blocks are
                          * tokenized in their entirety all the way to the
                          * leaves.
                          */
                     }
                     processBlock(token);
                         /* processBlock recurses through scanpass(),
                          * which processes the nested commands which have
                          * (in all cases) already beeen tokenized.
                          */
                     continue;
                 }
 
                 switch (token.type) {
                     case Token.SYNTAX_ERR_TYPE:
                         throw new SqlToolError(SqltoolRB.input_malformat.getString());
                         // Will get here if Scanner can't match input to any
                         // known command type.
                         // An easy way to get here is to start a command with
                         // quotes.
                     case Token.UNTERM_TYPE:
                         throw new SqlToolError(
                                 SqltoolRB.input_unterminated.getString(
                                 token.val));
                     case Token.RAW_TYPE:
                     case Token.RAWEXEC_TYPE:
                         /*
                          * A real problem in this block is that the Scanner
                          * has already displayed the next prompt at this
                          * point.  We handle this specially within this
                          * block, but if we throw, the handler will not
                          * know that the prompt has to be re-displayed.
                          * I.e., KNOWN ISSUE:  For some errors caught during
                          * raw command execution, interactive users will not
                          * get a prompt to tell them to proceed.
                          */
                         if (token.val == null) token.val = "";
                         /*
                          * Don't have time know to figure out whether it would
                          * ever be useful to send just (non-zero) whitespace
                          * to the DB.  Prohibiting for now.
                          */
                         if (token.val.trim().length() < 1) {
                             throw new SqlToolError(
                                     SqltoolRB.raw_empty.getString());
                         }
                         int receivedType = token.type;
                         token.type = Token.SQL_TYPE;
                         if (setBuf(token) && receivedType == Token.RAW_TYPE
                                 && interactive) {
                             stdprintln("");
                             stdprintln(SqltoolRB.raw_movedtobuffer.getString());
                             stdprint(primaryPrompt);
                             // All of these stdprint*'s are to work around a
                             // very complicated issue where the Scanner
                             // has already displayed the next prompt before
                             // we can display our status message.
                         }
                         if (receivedType == Token.RAWEXEC_TYPE) {
                             historize();
                             processSQL();
                         }
                         continue;
                     case Token.MACRO_TYPE:
                         processMacro(token);
                         continue;
                     case Token.PL_TYPE:
                         setBuf(token);
                         historize();
                         processPL(null);
                         continue;
                     case Token.SPECIAL_TYPE:
                         setBuf(token);
                         historize();
                         processSpecial(null);
                         continue;
                     case Token.EDIT_TYPE:
                         // Scanner only returns EDIT_TYPEs in interactive mode
                         processBuffHist(token);
                         continue;
                     case Token.BUFFER_TYPE:
                         token.type = Token.SQL_TYPE;
                         if (setBuf(token)) {
                             stdprintln(
                                     SqltoolRB.input_movedtobuffer.getString());
                         }
                         continue;
                     case Token.SQL_TYPE:
                         if (token.val == null) token.val = "";
                         setBuf(token);
                         historize();
                         processSQL();
                         continue;
                     default:
                         throw new RuntimeException(
                                 "Internal assertion failed.  "
                                 + "Unexpected token type: "
                                 + token.getTypeString());
                 }
             } catch (BadSpecial bs) {
                 // BadSpecials ALWAYS have non-null getMessage().
                 if (token == null) {
                     errprintln(SqltoolRB.errorat.getString(
                             inputStreamLabel, "?", "?", bs.getMessage()));
                 } else {
                     errprintln(SqltoolRB.errorat.getString(
                             inputStreamLabel,
                             Integer.toString(token.line),
                             token.reconstitute(),
                             bs.getMessage(), bs.getMessage()));
                 }
                 Throwable cause = bs.getCause();
                 if (cause != null) {
                     errprintln(SqltoolRB.causereport.getString(
                             cause.toString()));
 
                 }
 
                 if (!continueOnError) {
                     throw new SqlToolError(bs);
                 }
             } catch (SQLException se) {
                 //se.printStackTrace();
                 errprintln("SQL " + SqltoolRB.errorat.getString(
                         inputStreamLabel,
                         ((token == null) ? "?"
                                          : Integer.toString(token.line)),
                         lastSqlStatement,
                         se.getMessage()));
                 // It's possible that we could have
                 // SQLException.getMessage() == null, but if so, I think
                 // it reasonable to show "null".  That's a DB inadequacy.
 
                 if (!continueOnError) {
                     throw se;
                 }
             } catch (BreakException be) {
                 String msg = be.getMessage();
 
                 if (recursed) {
                     rollbackUncoms = false;
                     // Recursion level will exit by rethrowing the BE.
                     // We set rollbackUncoms to false because only the
                     // top level should detect break errors and
                     // possibly roll back.
                 } else if (msg == null || msg.equals("file")) {
                     break;
                 } else {
                     errprintln(SqltoolRB.break_unsatisfied.getString(msg));
                 }
 
                 if (recursed ||!continueOnError) {
                     throw be;
                 }
             } catch (ContinueException ce) {
                 String msg = ce.getMessage();
 
                 if (recursed) {
                     rollbackUncoms = false;
                 } else {
                     errprintln(SqltoolRB.continue_unsatisfied.getString(msg));
                 }
 
                 if (recursed ||!continueOnError) {
                     throw ce;
                 }
             } catch (QuitNow qn) {
                 throw qn;
             } catch (SqlToolError ste) {
                 StringBuffer sb = new StringBuffer(SqltoolRB.errorat.getString(
                     /* WARNING:  I have removed an extra LS appended to
                      * non-null ste.getMessages() below because I believe that
                      * it is unnecessary (and causes inconsistent blank lines
                      * to be written).
                      * If I am wrong and this is needed for Scanner display or
                      * something, restore it.
                      */
                     ((token == null)
                             ? (new String[] {
                                 inputStreamLabel, "?", "?",
                                 ((ste.getMessage() == null)
                                         ? "" : ste.getMessage())
                               })
                             : (new String[] {
                                 inputStreamLabel, Integer.toString(token.line),
                                 ((token.val == null) ? "" : token.reconstitute()),
                                 ((ste.getMessage() == null)
                                         ? "" : ste.getMessage())
                               }))
                 ));
                 Throwable cause = ste.getCause();
                 errprintln((cause == null) ? sb.toString()
                         : SqltoolRB.causereport.getString(cause.toString()));
                 if (!continueOnError) {
                     throw ste;
                 }
             }
 
             rollbackUncoms = false;
             // Exiting gracefully, so don't roll back.
         } catch (IOException ioe) {
             throw new SqlToolError(
                     SqltoolRB.primaryinput_accessfail.getString(), ioe);
         } catch (QuitNow qn) {
             if (recursed) {
                 throw qn;
                 // Will rollback if conditions otherwise require.
                 // Otherwise top level will decide based upon qn.getMessage().
             }
             rollbackUncoms = (qn.getMessage() != null);
 
             if (rollbackUncoms) {
                 errprintln(SqltoolRB.aborting.getString(qn.getMessage()));
                 throw new SqlToolError(qn.getMessage());
             }
 
             return;
         } finally {
             if (fetchingVar != null) {
                 errprintln(SqltoolRB.plvar_set_incomplete.getString(
                         fetchingVar));
                 rollbackUncoms = true;
             }
             if (shared.jdbcConn != null) {
                 if (shared.jdbcConn.getAutoCommit())
                     shared.possiblyUncommitteds = false;
                 if (rollbackUncoms && shared.possiblyUncommitteds) {
                     errprintln(SqltoolRB.rollingback.getString());
                     shared.jdbcConn.rollback();
                     shared.possiblyUncommitteds = false;
                 }
             }
         }
    }

    private void enforce1charSpecial(String tokenString, char command)
             throws BadSpecial {
         if (tokenString.length() != 1) {
             throw new BadSpecial(SqltoolRB.special_extrachars.getString(
                      Character.toString(command), tokenString.substring(1))); 
        }
    }
    
    private void enforce1charBH(String tokenString, char command)
             throws BadSpecial {
         if (tokenString != null) {
             throw new BadSpecial(SqltoolRB.buffer_extrachars.getString(
                     Character.toString(command), tokenString));
         }
    }

    private void processSpecial(String inString)
     throws BadSpecial, QuitNow, SQLException, SqlToolError {
         String string = (inString == null) ? buffer.val : inString;
         if (string.length() < 1) {
             throw new BadSpecial(SqltoolRB.special_unspecified.getString());
         }
         Matcher m = specialPattern.matcher(
                 plMode ? dereference(string, false) : string);
         if (!m.matches()) {
             throw new BadSpecial(SqltoolRB.special_malformat.getString());
             // I think it's impossible to get here, since the pattern is
             // so liberal.
         }
         if (m.groupCount() < 1 || m.groupCount() > 2) {
             // Failed assertion
             throw new RuntimeException(
                     "Internal assertion failed.  Pattern matched, yet captured "
                     + m.groupCount() + " groups");
         }
 
         String arg1 = m.group(1);
         String other = ((m.groupCount() > 1) ? m.group(2) : null);
 
         switch (arg1.charAt(0)) {
             case 'q' :
                 enforce1charSpecial(arg1, 'q');
                 if (other != null) {
                     throw new QuitNow(other);
                 }
 
                 throw new QuitNow();
             case 'H' :
                 enforce1charSpecial(arg1, 'H');
                 htmlMode = !htmlMode;
 
                 stdprintln(SqltoolRB.html_mode.getString(
                         Boolean.toString(htmlMode)));
 
                 return;
 
             case 'm' :
                 if (arg1.equals("m?") ||
                         (arg1.equals("m") && other != null
                                  && other.equals("?"))) {
                     stdprintln(DSV_OPTIONS_TEXT + LS + DSV_M_SYNTAX_MSG);
                     return;
                 }
                 if (arg1.length() != 1 || other == null) {
                     throw new BadSpecial(DSV_M_SYNTAX_MSG);
                 }
                 boolean noComments = other.charAt(other.length() - 1) == '*';
                 String skipPrefix = null;
 
                 if (noComments) {
                     other = other.substring(0, other.length()-1).trim();
                     if (other.length() < 1) {
                         throw new BadSpecial(DSV_M_SYNTAX_MSG);
                     }
                 } else {
                     skipPrefix = dsvSkipPrefix;
                 }
                 int colonIndex = other.indexOf(" :");
                 if (colonIndex > -1 && colonIndex < other.length() - 2) {
                     skipPrefix = other.substring(colonIndex + 2);
                     other = other.substring(0, colonIndex).trim();
                 }
 
                 importDsv(dereferenceAt(other), skipPrefix);
 
                 return;
 
             case 'x' :
                 requireConnection();
                 if (arg1.equals("x?") ||
                         (arg1.equals("x") && other != null
                                  && other.equals("?"))) {
                     stdprintln(DSV_OPTIONS_TEXT + LS + DSV_X_SYNTAX_MSG);
                     return;
                 }
                 try {
                     if (arg1.length() != 1 || other == null) {
                         throw new BadSpecial(DSV_X_SYNTAX_MSG);
                     }
 
                     String tableName = ((other.indexOf(' ') > 0) ? null
                                                                  : other);
 
                     if (dsvTargetFile == null && tableName == null) {
                         throw new BadSpecial(
                                 SqltoolRB.dsv_targetfile_demand.getString());
                     }
                     File dsvFile = new File((dsvTargetFile == null)
                                             ? (tableName + ".dsv")
                                             : dereferenceAt(dsvTargetFile));
 
                     pwDsv = new PrintWriter(new OutputStreamWriter(
                             new FileOutputStream(dsvFile),
                             (shared.encoding == null)
                             ? DEFAULT_FILE_ENCODING : shared.encoding));
 
                     ResultSet rs = shared.jdbcConn.createStatement()
                             .executeQuery((tableName == null) ? other
                                                 : ("SELECT * FROM "
                                                    + tableName));
                     try {
                         List<Integer> colList = new ArrayList<Integer>();
                         int[] incCols = null;
                         if (dsvSkipCols != null) {
                             Set<String> skipCols = new HashSet<String>();
                             for (String s : dsvSkipCols.split(dsvColDelim, -1)) {
                             // Don't know if better to use dsvColDelim or
                             // dsvColSplitter.  Going with former, since the
                             // latter should not need to be set for eXporting
                             // (only importing).
                                 skipCols.add(s.trim().toLowerCase());
                             }
                             ResultSetMetaData rsmd = rs.getMetaData();
                             for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                                 if (!skipCols.remove(rsmd.getColumnName(i)
                                         .toLowerCase())) {
                                     colList.add(Integer.valueOf(i));
                                 }
                             }
                             if (colList.size() < 1) {
                                 throw new BadSpecial(
                                         SqltoolRB.dsv_nocolsleft.getString(
                                         dsvSkipCols));
                             }
                             if (skipCols.size() > 0) {
                                 throw new BadSpecial(
                                         SqltoolRB.dsv_skipcols_missing.getString(
                                         skipCols.toString()));
                             }
                             incCols = new int[colList.size()];
                             for (int i = 0; i < incCols.length; i++) {
                                 incCols[i] = colList.get(i).intValue();
                             }
                         }
                         displayResultSet(null, rs, incCols, null);
                     } finally {
                         rs.close();
                     }
                     pwDsv.flush();
                     stdprintln(SqltoolRB.file_wrotechars.getString(
                             Long.toString(dsvFile.length()),
                             dsvFile.toString()));
                 } catch (FileNotFoundException e) {
                     throw new BadSpecial(SqltoolRB.file_writefail.getString(
                             other), e);
                 } catch (UnsupportedEncodingException e) {
                     throw new BadSpecial(SqltoolRB.file_writefail.getString(
                             other), e);
                 } finally {
                     // Reset all state changes
                     if (pwDsv != null) {
                         try {
                             pwDsv.close();
                         } finally {
                             pwDsv = null; // Encourage GC of buffers
                         }
                     }
                 }
 
                 return;
 
             case 'd' :
                 requireConnection();
                 if (arg1.equals("d?") ||
                         (arg1.equals("d") && other != null
                                  && other.equals("?"))) {
                     stdprintln(D_OPTIONS_TEXT);
                     return;
                 }
                 if (arg1.length() == 2) {
                     listTables(arg1.charAt(1), other);
 
                     return;
                 }
 
                 if (arg1.length() == 1 && other != null) try {
                     int space = other.indexOf(' ');
 
                     if (space < 0) {
                         describe(other, null);
                     } else {
                         describe(other.substring(0, space),
                                  other.substring(space + 1).trim());
                     }
 
                     return;
                 } catch (SQLException se) {
                     throw new BadSpecial(
                             SqltoolRB.metadata_fetch_fail.getString(), se);
                 }
 
                 throw new BadSpecial(SqltoolRB.special_d_like.getString());
             case 'o' :
                 enforce1charSpecial(arg1, 'o');
                 if (other == null) {
                     if (pwQuery == null) {
                         throw new BadSpecial(
                                 SqltoolRB.outputfile_nonetoclose.getString());
                     }
 
                     closeQueryOutputStream();
 
                     return;
                 }
 
                 if (pwQuery != null) {
                     stdprintln(SqltoolRB.outputfile_reopening.getString());
                     closeQueryOutputStream();
                 }
 
                 try {
                     pwQuery = new PrintWriter(new OutputStreamWriter(
                             new FileOutputStream(dereferenceAt(other), true),
                             (shared.encoding == null)
                             ? DEFAULT_FILE_ENCODING : shared.encoding));
 
                     /* Opening in append mode, so it's possible that we will
                      * be adding superfluous <HTML> and <BODY> tags.
                      * I think that browsers can handle that */
                     pwQuery.println((htmlMode
                             ? ("<HTML>" + LS + "<!--")
                             : "#") + " " + (new java.util.Date()) + ".  "
                                     + SqltoolRB.outputfile_header.getString(
                                     getClass().getName())
                                     + (htmlMode ? (" -->" + LS + LS + "<BODY>")
                                                 : LS));
                     pwQuery.flush();
                 } catch (Exception e) {
                     throw new BadSpecial(SqltoolRB.file_writefail.getString(
                             other), e);
                 }
 
                 return;
 
             case 'i' :
                 enforce1charSpecial(arg1, 'i');
                 if (other == null) {
                     throw new BadSpecial(
                             SqltoolRB.sqlfile_name_demand.getString());
                 }
 
                 try {
                     new SqlFile(this, new File(dereferenceAt(other))).execute();
                 } catch (ContinueException ce) {
                     throw ce;
                 } catch (BreakException be) {
                     String beMessage = be.getMessage();
 
                     // Handle "file" and plain breaks (by doing nothing)
                     if (beMessage != null &&!beMessage.equals("file")) {
                         throw be;
                     }
                 } catch (QuitNow qn) {
                     throw qn;
                 } catch (Exception e) {
                     throw new BadSpecial(
                             SqltoolRB.sqlfile_execute_fail.getString(other), e);
                 }
 
                 return;
 
             case 'p' :
                 enforce1charSpecial(arg1, 'p');
                 if (other == null) {
                     stdprintln(true);
                 } else {
                     stdprintln(other, true);
                 }
 
                 return;
 
             case 'l' :
                 if ((arg1.equals("l?") && other == null)
                         || (arg1.equals("l") && other != null
                                 && other.equals("?"))) {
                     stdprintln(SqltoolRB.log_syntax.getString());
                 } else {
                     enforce1charSpecial(arg1, 'l');
                     Matcher logMatcher = ((other == null) ? null
                             : logPattern.matcher(other.trim()));
                     if (logMatcher == null || (!logMatcher.matches()))
                         throw new BadSpecial(
                                 SqltoolRB.log_syntax_error.getString());
                     String levelString = logMatcher.group(1);
                     Level level = null;
                     if (levelString.equalsIgnoreCase("FINER"))
                         level = Level.FINER;
                     else if (levelString.equalsIgnoreCase("WARNING"))
                         level = Level.WARNING;
                     else if (levelString.equalsIgnoreCase("SEVERE"))
                         level = Level.SEVERE;
                     else if (levelString.equalsIgnoreCase("INFO"))
                         level = Level.INFO;
                     else if (levelString.equalsIgnoreCase("FINEST"))
                         level = Level.FINEST;
                     if (level == null)
                         throw new RuntimeException(
                                 "Internal assertion failed.  "
                                 + " Unexpected Level string: " + levelString);
                     logger.enduserlog(level, logMatcher.group(2));
                 }
 
                 return;
 
             case 'a' :
                 requireConnection();
                 enforce1charSpecial(arg1, 'a');
                 if (other != null) {
                     shared.jdbcConn.setAutoCommit(
                         Boolean.parseBoolean(other));
                     shared.possiblyUncommitteds = false;
                 }
 
                 stdprintln(SqltoolRB.a_setting.getString(
                         Boolean.toString(shared.jdbcConn.getAutoCommit())));
 
                 return; case 'j' : try {
                 enforce1charSpecial(arg1, 'j');
                 String urlid = null;
                 String acct = null;
                 String pwd = null;
                 String url = null;
                 boolean goalAutoCommit = false;
                 String[] tokens = (other == null)
                         ? (new String[0]) : other.split("\\s+", 3);
                 switch (tokens.length) {
                     case 0:
                         break;
                     case 1:
                         urlid = tokens[0];
                         break;
                     case 2:
                         acct = tokens[0];
                         pwd = "";  // default password to ""
                         url = tokens[1];
                         break;
                     case 3:
                         acct = tokens[0];
                         pwd = tokens[1];
                         url = tokens[2];
                         break;
                 }
                 if (tokens.length > 0) {
                     // Close current connection
                     if (shared.jdbcConn != null) try {
                         goalAutoCommit = shared.jdbcConn.getAutoCommit();
                         shared.jdbcConn.close();
                         shared.possiblyUncommitteds = false;
                         shared.jdbcConn = null;
                         stdprintln(SqltoolRB.disconnect_success.getString());
                     } catch (SQLException se) {
                         throw new BadSpecial(
                                 SqltoolRB.disconnect_failure.getString(), se);
                     }
                 }
                 if (urlid != null || acct != null) try {
                     if (urlid != null) {
                         shared.jdbcConn = new RCData(new File(
                             SqlTool.DEFAULT_RCFILE), urlid).getConnection();
                     } else if (acct != null) {
                         shared.jdbcConn =
                                 DriverManager.getConnection(url, acct, pwd);
                     }
                     shared.possiblyUncommitteds = false;
                     shared.jdbcConn.setAutoCommit(goalAutoCommit);
                 } catch (Exception e) {
                     throw new BadSpecial("Failed to connect", e);
                 }
                 displayConnBanner();
             } catch (Throwable t) {
                 t.printStackTrace();
                 return;
             }
                 return;
             case 'v' :
                 requireConnection();
                 enforce1charSpecial(arg1, 'v');
                 if (other != null) {
                     if (integerPattern.matcher(other).matches()) {
                         shared.jdbcConn.setTransactionIsolation(
                                 Integer.parseInt(other));
                     } else {
                         RCData.setTI(shared.jdbcConn, other);
                     }
                 }
 
                 stdprintln(SqltoolRB.transiso_report.getString(
                         (shared.jdbcConn.isReadOnly() ? "R/O " : "R/W "),
                         RCData.tiToString(
                                 shared.jdbcConn.getTransactionIsolation())));
 
                 return;
             case '=' :
                 requireConnection();
                 enforce1charSpecial(arg1, '=');
                 shared.jdbcConn.commit();
                 shared.possiblyUncommitteds = false;
                 stdprintln(SqltoolRB.committed.getString());
 
                 return;
 
             case 'b' :
                 if (arg1.length() == 1) {
                     if (other != null) {
                         throw new BadSpecial(
                                 SqltoolRB.special_b_malformat.getString());
                     }
                     fetchBinary = true;
 
                     return;
                 }
 
                 if (arg1.charAt(1) == 'p') {
                     if (other != null) {
                         throw new BadSpecial(
                                 SqltoolRB.special_b_malformat.getString());
                     }
                     doPrepare = true;
 
                     return;
                 }
 
                 if ((arg1.charAt(1) != 'd' && arg1.charAt(1) != 'l')
                         || other == null) {
                     throw new BadSpecial(
                             SqltoolRB.special_b_malformat.getString());
                 }
 
                 File otherFile = new File(dereferenceAt(other));
 
                 try {
                     if (arg1.charAt(1) == 'd') {
                         dump(otherFile);
                     } else {
                         binBuffer = SqlFile.loadBinary(otherFile);
                         stdprintln(SqltoolRB.binary_loadedbytesinto.getString(
                                 binBuffer.length));
                     }
                 } catch (BadSpecial bs) {
                     throw bs;
                 } catch (IOException ioe) {
                     throw new BadSpecial(SqltoolRB.binary_filefail.getString(
                             other), ioe);
                 }
 
                 return;
 
             case 't' :
                 enforce1charSpecial(arg1, '=');
                 if (other != null) {
                     // But remember that we have to abort on some I/O errors.
                     reportTimes = Boolean.parseBoolean(other);
                 }
 
                 stdprintln(SqltoolRB.exectime_reporting.getString(
                         Boolean.toString(reportTimes)));
                 return;
 
             case '*' :
             case 'c' :
                 enforce1charSpecial(arg1, '=');
                 if (other != null) {
                     // But remember that we have to abort on some I/O errors.
                     continueOnError = Boolean.parseBoolean(other);
                 }
 
                 stdprintln(SqltoolRB.c_setting.getString(
                         Boolean.toString(continueOnError)));
 
                 return;
 
             case '?' :
                 stdprintln(SqltoolRB.special_help.getString());
 
                 return;
 
             case '!' :
                 /* N.b. This DOES NOT HANDLE UNIX shell wildcards, since there
                  * is no UNIX shell involved.
                  * Doesn't make sense to incur overhead of a shell without
                  * stdin capability.
                  * Could pipe System.in to the forked process, but that's
                  * probably not worth the effort due to Java's terrible
                  * and inescapable System.in buffering.  I.e., the forked
                  * program or shell wouldn't get stdin until user hits Enter.
                  *
                  * I'd like to execute the user's default shell if they
                  * ran "\!" with no argument, but (a) there is no portable
                  * way to determine the user's default or login shell; and
                  * (b) shell is useless without stdin ability.
                  */
 
                 InputStream stream;
                 byte[]      ba         = new byte[1024];
                 String      extCommand = ((arg1.length() == 1)
                         ? "" : arg1.substring(1))
                     + ((arg1.length() > 1 && other != null)
                        ? " " : "") + ((other == null) ? "" : other);
                 if (extCommand.trim().length() < 1)
                     throw new BadSpecial(SqltoolRB.bang_incomplete.getString());
 
                 Process proc = null;
                 try {
                     Runtime runtime = Runtime.getRuntime();
                     proc = ((wincmdPattern == null)
                             ? runtime.exec(extCommand)
                             : runtime.exec(genWinArgs(extCommand))
                     );
 
                     proc.getOutputStream().close();
 
                     int i;
 
                     stream = proc.getInputStream();
 
                     while ((i = stream.read(ba)) > 0) {
                         stdprint(new String(ba, 0, i));
                     }
 
                     stream.close();
 
                     stream = proc.getErrorStream();
 
                     String s;
                     while ((i = stream.read(ba)) > 0) {
                         s = new String(ba, 0, i);
                         if (s.endsWith(LS)) {
                             // This block just prevents logging of
                             // double-line-breaks.
                             if (s.length() == LS.length()) continue;
                             s = s.substring(0, s.length() - LS.length());
                         }
                         logger.severe(s);
                     }
 
                     stream.close();
                     stream = null;  // Encourage buffer GC
 
                     if (proc.waitFor() != 0) {
                         throw new BadSpecial(
                                 SqltoolRB.bang_command_fail.getString(
                                 extCommand));
                     }
                 } catch (BadSpecial bs) {
                     throw bs;
                 } catch (Exception e) {
                     throw new BadSpecial(SqltoolRB.bang_command_fail.getString(
                             extCommand), e);
                 } finally {
                     if (proc != null) {
                         proc.destroy();
                     }
                 }
 
                 return;
         }
 
         throw new BadSpecial(SqltoolRB.special_unknown.getString(
                 Character.toString(arg1.charAt(0))));
    }

    private void processBlock(Token token) throws BadSpecial, SqlToolError {
        Matcher m = plPattern.matcher(dereference(token.val, false));
        if (!m.matches()) {
            throw new BadSpecial(SqltoolRB.pl_malformat.getString());
            // I think it's impossible to get here, since the pattern is
            // so liberal.
        }
        if (m.groupCount() < 1 || m.group(1) == null) {
            plMode = true;
            stdprintln(SqltoolRB.pl_expansionmode.getString("on"));
            return;
        }

        String[] tokens = m.group(1).split("\\s+", -1);

        // If user runs any PL command, we turn PL mode on.
        plMode = true;

        if (tokens[0].equals("foreach")) {
            Matcher foreachM = foreachPattern.matcher(
                    dereference(token.val, false));
            if (!foreachM.matches()) {
                throw new BadSpecial(SqltoolRB.foreach_malformat.getString());
            }
            if (foreachM.groupCount() != 2) {
                throw new RuntimeException(
                        "Internal assertion failed.  "
                        + "foreach pattern matched, but captured "
                        + foreachM.groupCount() + " groups");
            }

            String varName   = foreachM.group(1);
            if (varName.indexOf(':') > -1) {
                throw new BadSpecial(SqltoolRB.plvar_nocolon.getString());
            }
            String[] values = foreachM.group(2).split("\\s+", -1);

            String origval = shared.userVars.get(varName);


            try {
                for (String val : values) {
                    try {
                        shared.userVars.put(varName, val);
                        updateUserSettings();

                        boolean origRecursed = recursed;
                        recursed = true;
                        try {
                            scanpass(token.nestedBlock.dup());
                        } finally {
                            recursed = origRecursed;
                        }
                    } catch (ContinueException ce) {
                        String ceMessage = ce.getMessage();

                        if (ceMessage != null
                                &&!ceMessage.equals("foreach")) {
                            throw ce;
                        }
                    }
                }
            } catch (BreakException be) {
                String beMessage = be.getMessage();

                // Handle "foreach" and plain breaks (by doing nothing)
                if (beMessage != null &&!beMessage.equals("foreach")) {
                    throw be;
                }
            } catch (QuitNow qn) {
                throw qn;
            } catch (RuntimeException re) {
                throw re;  // Unrecoverable
            } catch (Exception e) {
                throw new BadSpecial(SqltoolRB.pl_block_fail.getString(), e);
            }

            if (origval == null) {
                shared.userVars.remove(varName);
                updateUserSettings();
            } else {
                shared.userVars.put(varName, origval);
            }

            return;
        }

        if (tokens[0].equals("if") || tokens[0].equals("while")) {
            Matcher ifwhileM= ifwhilePattern.matcher(
                    dereference(token.val, false));
            if (!ifwhileM.matches()) {
                throw new BadSpecial(SqltoolRB.ifwhile_malformat.getString());
            }
            if (ifwhileM.groupCount() != 1) {
                throw new RuntimeException(
                        "Internal assertion failed.  "
                        + "if/while pattern matched, but captured "
                        + ifwhileM.groupCount() + " groups");
            }

            String[] values =
                    ifwhileM.group(1).replaceAll("!([a-zA-Z0-9*])", "! $1").
                        replaceAll("([a-zA-Z0-9*])!", "$1 !").split("\\s+", -1);

            if (tokens[0].equals("if")) {
                try {
                    if (eval(values)) {
                        boolean origRecursed = recursed;
                        recursed = true;
                        try {
                            scanpass(token.nestedBlock.dup());
                        } finally {
                            recursed = origRecursed;
                        }
                    }
                } catch (BreakException be) {
                    String beMessage = be.getMessage();

                    // Handle "if" and plain breaks (by doing nothing)
                    if (beMessage == null ||!beMessage.equals("if")) {
                        throw be;
                    }
                } catch (ContinueException ce) {
                    throw ce;
                } catch (QuitNow qn) {
                    throw qn;
                } catch (BadSpecial bs) {
                    bs.appendMessage(SqltoolRB.if_malformat.getString());
                    throw bs;
                } catch (RuntimeException re) {
                    throw re;  // Unrecoverable
                } catch (Exception e) {
                    throw new BadSpecial(
                        SqltoolRB.pl_block_fail.getString(), e);
                }
            } else if (tokens[0].equals("while")) {
                try {

                    while (eval(values)) {
                        try {
                            boolean origRecursed = recursed;
                            recursed = true;
                            try {
                                scanpass(token.nestedBlock.dup());
                            } finally {
                                recursed = origRecursed;
                            }
                        } catch (ContinueException ce) {
                            String ceMessage = ce.getMessage();

                            if (ceMessage != null &&!ceMessage.equals("while")) {
                                throw ce;
                            }
                        }
                    }
                } catch (BreakException be) {
                    String beMessage = be.getMessage();

                    // Handle "while" and plain breaks (by doing nothing)
                    if (beMessage != null &&!beMessage.equals("while")) {
                        throw be;
                    }
                } catch (QuitNow qn) {
                    throw qn;
                } catch (BadSpecial bs) {
                    bs.appendMessage(SqltoolRB.while_malformat.getString());
                    throw bs;
                } catch (RuntimeException re) {
                    throw re;  // Unrecoverable
                } catch (Exception e) {
                    throw new BadSpecial(
                            SqltoolRB.pl_block_fail.getString(), e);
                }
            } else {
                // Assertion
                throw new RuntimeException(
                        SqltoolRB.pl_unknown.getString(tokens[0]));
            }

            return;
        }

        throw new BadSpecial(SqltoolRB.pl_unknown.getString(tokens[0]));
    }

    private void listTables(char c, String inFilter) throws BadSpecial,
             SqlToolError {
         requireConnection();
         String   schema  = null;
         int[]    listSet = null;
         String[] types   = null;
 
         /** For workaround for \T for Oracle */
         String[] additionalSchemas = null;
 
         /** This is for specific non-getTable() queries */
         Statement statement = null;
         ResultSet rs        = null;
         String    narrower  = "";
         /*
          * Doing case-sensitive filters now, for greater portability.
         String                    filter = ((inFilter == null)
                                           ? null : inFilter.toUpperCase());
          */
         String filter = inFilter;
 
         try {
             DatabaseMetaData md            = shared.jdbcConn.getMetaData();
             String           dbProductName = md.getDatabaseProductName();
             int              majorVersion  = 0;
             int              minorVersion  = 0;
 
             // We only use majorVersion and minorVersion for HyperSQL so far
             // The calls avoided here avoid problems with non-confirmant drivers
             if (dbProductName.indexOf("HSQL") > -1) try {
                 majorVersion  = md.getDatabaseMajorVersion();
                 minorVersion  = md.getDatabaseMinorVersion();
             } catch (UnsupportedOperationException uoe) {
                 // It seems that Sun's JDBC/ODBC bridge throws here
                 majorVersion = 2;
                 minorVersion = 0;
             }
 
             //System.err.println("DB NAME = (" + dbProductName + ')');
             // Database-specific table filtering.
 
             /* 3 Types of actions:
              *    1) Special handling.  Return from the "case" block directly.
              *    2) Execute a specific query.  Set statement in the "case".
              *    3) Otherwise, set filter info for dbmd.getTable() in the
              *       "case".
              */
             types = new String[1];
 
             switch (c) {
                 case '*' :
                     types = null;
                     break;
 
                 case 'S' :
                     if (dbProductName.indexOf("Oracle") > -1) {
                         errprintln(SqltoolRB.vendor_oracle_dS.getString());
 
                         types[0]          = "TABLE";
                         schema            = "SYS";
                         additionalSchemas = oracleSysSchemas;
                     } else {
                         types[0] = "SYSTEM TABLE";
                     }
                     break;
 
                 case 's' :
                     if (dbProductName.indexOf("HSQL") > -1) {
                         //  HSQLDB does not consider Sequences as "tables",
                         //  hence we do not list them in
                         //  DatabaseMetaData.getTables().
                         if (filter != null) {
                             Matcher matcher = dotPattern.matcher(filter);
                             if (matcher.matches()) {
                                 filter = (matcher.group(2).length() > 0)
                                         ? matcher.group(2) : null;
                                 narrower = "\nWHERE sequence_schema = '"
                                         + ((matcher.group(1).length() > 0)
                                                 ? matcher.group(1)
                                                 : getCurrentSchema()) + "'";
                             }
                         }
 
                         statement = shared.jdbcConn.createStatement();
 
                         statement.execute(
                             "SELECT sequence_schema, sequence_name FROM "
                             + "information_schema."
                             + ((minorVersion> 8 || majorVersion > 1)
                             ? "sequences" : "system_sequences") + narrower);
                     } else {
                         types[0] = "SEQUENCE";
                     }
                     break;
 
                 case 'r' :
                     if (dbProductName.indexOf("HSQL") > -1) {
                         statement = shared.jdbcConn.createStatement();
 
                         statement.execute(
                             "SELECT authorization_name FROM information_schema."
                             + ((minorVersion> 8 || majorVersion > 1)
                             ? "authorizations" : "system_authorizations")
                             + "\nWHERE authorization_type = 'ROLE'\n"
                             + "ORDER BY authorization_name");
                     } else if (dbProductName.indexOf(
                             "Adaptive Server Enterprise") > -1) {
                         // This is the basic Sybase server.  Sybase also has
                         // their "Anywhere", ASA (for embedded), and replication
                         // databases, but I don't know the Metadata strings for
                         // those.
                         statement = shared.jdbcConn.createStatement();
 
                         statement.execute(
                             "SELECT name FROM syssrvroles ORDER BY name");
                     } else if (dbProductName.indexOf(
                             "Apache Derby") > -1) {
                         throw new BadSpecial(
                             SqltoolRB.vendor_derby_dr.getString());
                     } else {
                         throw new BadSpecial(
                             SqltoolRB.vendor_nosup_d.getString("r"));
                     }
                     break;
 
                 case 'u' :
                     if (dbProductName.indexOf("HSQL") > -1) {
                         statement = shared.jdbcConn.createStatement();
 
                         statement.execute("SELECT "
                             + ((minorVersion> 8 || majorVersion > 1)
                             ? "user_name" : "user") + ", admin FROM "
                             + "information_schema.system_users\n"
                             + "ORDER BY user_name");
                     } else if (dbProductName.indexOf("Oracle") > -1) {
                         statement = shared.jdbcConn.createStatement();
 
                         statement.execute(
                             "SELECT username, created FROM all_users "
                             + "ORDER BY username");
                     } else if (dbProductName.indexOf("PostgreSQL") > -1) {
                         statement = shared.jdbcConn.createStatement();
 
                         statement.execute(
                             "SELECT usename, usesuper FROM pg_catalog.pg_user "
                             + "ORDER BY usename");
                     } else if (dbProductName.indexOf(
                             "Adaptive Server Enterprise") > -1) {
                         // This is the basic Sybase server.  Sybase also has
                         // their "Anywhere", ASA (for embedded), and replication
                         // databases, but I don't know the Metadata strings for
                         // those.
                         statement = shared.jdbcConn.createStatement();
 
                         statement.execute(
                             "SELECT name, accdate, fullname FROM syslogins "
                             + "ORDER BY name");
                     } else if (dbProductName.indexOf(
                             "Apache Derby") > -1) {
                         throw new BadSpecial(
                             SqltoolRB.vendor_derby_du.getString());
                     } else {
                         throw new BadSpecial(
                             SqltoolRB.vendor_nosup_d.getString("u"));
                     }
                     break;
 
                 case 'a' :
                     if (dbProductName.indexOf("HSQL") > -1
                         && (minorVersion < 9 && majorVersion < 2)) {
                         // HSQLDB after 1.8 doesn't support any type of aliases
                         //  Earlier HSQLDB Aliases are not the same things as
                         //  the aliases listed in DatabaseMetaData.getTables().
                         if (filter != null) {
                             Matcher matcher = dotPattern.matcher(filter);
                             if (matcher.matches()) {
                                 filter = (matcher.group(2).length() > 0)
                                         ? matcher.group(2) : null;
                                 narrower = "\nWHERE alias_schema = '"
                                         + ((matcher.group(1).length() > 0)
                                                 ? matcher.group(1)
                                                 : getCurrentSchema()) + "'";
                             }
                         }
 
                         statement = shared.jdbcConn.createStatement();
 
                         statement.execute(
                             "SELECT alias_schem, alias FROM "
                             + "information_schema.system_aliases" + narrower);
                     } else {
                         types[0] = "ALIAS";
                     }
                     break;
 
                 case 't' :
                     excludeSysSchemas = (dbProductName.indexOf("Oracle")
                                          > -1);
                     types[0] = "TABLE";
                     break;
 
                 case 'v' :
                     types[0] = "VIEW";
                     break;
 
                 case 'n' :
                     rs = md.getSchemas();
 
                     if (rs == null) {
                         throw new BadSpecial(
                             "Failed to get metadata from database");
                     }
 
                     displayResultSet(null, rs, listMDSchemaCols, filter);
 
                     return;
 
                 case 'i' :
 
                     // Some databases require to specify table, some don't.
                     /*
                     if (filter == null) {
                         throw new BadSpecial("You must specify the index's "
                                 + "table as argument to \\di");
                     }
                      */
                     String table = null;
 
                     if (filter != null) {
                         Matcher matcher = dotPattern.matcher(filter);
                         if (matcher.matches()) {
                             table = (matcher.group(2).length() > 0)
                                     ? matcher.group(2) : null;
                             schema = (matcher.group(1).length() > 0)
                                     ? matcher.group(1) : getCurrentSchema();
                         } else {
                             table = filter;
                         }
                         filter = null;
                     }
 
                     // N.b. Oracle incorrectly reports the INDEX SCHEMA as
                     // the TABLE SCHEMA.  The Metadata structure seems to
                     // be designed with the assumption that the INDEX schema
                     // will be the same as the TABLE schema.
                     rs = md.getIndexInfo(null, schema, table, false, true);
 
                     if (rs == null) {
                         throw new BadSpecial(
                             "Failed to get metadata from database");
                     }
 
                     displayResultSet(null, rs, listMDIndexCols, null);
 
                     return;
 
                 default :
                     throw new BadSpecial(SqltoolRB.special_d_unknown.getString(
                             Character.toString(c)) + LS + D_OPTIONS_TEXT);
             }
 
             if (statement == null) {
                 if (dbProductName.indexOf("HSQL") > -1) {
                     listSet = listMDTableCols[HSQLDB_ELEMENT];
                 } else if (dbProductName.indexOf("Oracle") > -1) {
                     listSet = listMDTableCols[ORACLE_ELEMENT];
                 } else {
                     listSet = listMDTableCols[DEFAULT_ELEMENT];
                 }
 
 
                 if (schema == null && filter != null) {
                     Matcher matcher = dotPattern.matcher(filter);
                     if (matcher.matches()) {
                         filter = (matcher.group(2).length() > 0)
                                 ? matcher.group(2) : null;
                         schema = (matcher.group(1).length() > 0)
                                 ? matcher.group(1)
                                 : getCurrentSchema();
                     }
                 }
             }
 
             rs = ((statement == null)
                   ? md.getTables(null, schema, null, types)
                   : statement.getResultSet());
 
             if (rs == null) {
                 throw new BadSpecial(SqltoolRB.metadata_fetch_fail.getString());
             }
 
             displayResultSet(null, rs, listSet, filter);
 
             if (additionalSchemas != null) {
                 for (String additionalSchema : additionalSchemas) {
                     /*
                      * Inefficient, but we have to do each successful query
                      * twice in order to prevent calling displayResultSet
                      * for empty/non-existent schemas
                      */
                     rs = md.getTables(null, additionalSchema, null,
                                       types);
 
                     if (rs == null) {
                         throw new BadSpecial(
                                 SqltoolRB.metadata_fetch_failfor.getString(
                                 additionalSchema));
                     }
 
                     if (!rs.next()) {
                         continue;
                     }
 
                     displayResultSet(
                         null,
                         md.getTables(
                             null, additionalSchema, null, types), listSet, filter);
                 }
             }
         } catch (SQLException se) {
             throw new BadSpecial(SqltoolRB.metadata_fetch_fail.getString(), se);
         } catch (NullPointerException npe) {
             throw new BadSpecial(SqltoolRB.metadata_fetch_fail.getString(),
                     npe);
         } finally {
             excludeSysSchemas = false;
 
             if (rs != null) {
                 try {
                     rs.close();
                 } catch (SQLException se) {
                     // We already got what we want from it, or have/are
                     // processing a more specific error.
                 }
             }
 
             if (statement != null) {
                 try {
                     statement.close();
                 } catch (SQLException se) {
                     // Purposefully doing nothing
                 }
             }
         }
    }

    private void processSQL() throws SQLException, SqlToolError {
        requireConnection();
        if (buffer == null)
            throw new RuntimeException(
                    "Internal assertion failed.  No buffer in processSQL().");
        if (buffer.type != Token.SQL_TYPE)
            throw new RuntimeException(
                    "Internal assertion failed.  "
                    + "Token type " + buffer.getTypeString()
                    + " in processSQL().");
        // No reason to check autoCommit constantly.  If we need to roll
        // back, we will check the autocommit state at that time.
        lastSqlStatement    = (plMode ? dereference(buffer.val, true)
                                      : buffer.val);
        // N.b. "lastSqlStatement" is a misnomer only inside this method.
        // Outside of this method, this var references the "last" SQL
        // statement which we attempted to execute.
        if ((!permitEmptySqlStatements) && buffer.val == null
                || buffer.val.trim().length() < 1) {
            throw new SqlToolError(SqltoolRB.sqlstatement_empty.getString());
            // There is nothing inherently wrong with issuing
            // an empty command, like to test DB server health.
            // But, this check effectively catches many syntax
            // errors early.
        }
        Statement statement = null;

        long startTime = 0;
        if (reportTimes) startTime = (new java.util.Date()).getTime();
        try { // VERY outer block just to ensure we close "statement"
        try { if (doPrepare) {
            if (lastSqlStatement.indexOf('?') < 1) {
                lastSqlStatement = null;
                throw new SqlToolError(SqltoolRB.prepare_demandqm.getString());
            }

            doPrepare = false;

            PreparedStatement ps =
                    shared.jdbcConn.prepareStatement(lastSqlStatement);
            statement = ps;

            if (prepareVar == null) {
                if (binBuffer == null) {
                    lastSqlStatement = null;
                    throw new SqlToolError(
                            SqltoolRB.binbuffer_empty.getString());
                }

                ps.setBytes(1, binBuffer);
            } else {
                String val = shared.userVars.get(prepareVar);

                if (val == null) {
                    lastSqlStatement = null;
                    throw new SqlToolError(
                            SqltoolRB.plvar_undefined.getString(prepareVar));
                }

                prepareVar = null;

                ps.setString(1, val);
            }

            ps.executeUpdate();
        } else {
            statement = shared.jdbcConn.createStatement();

            statement.execute(lastSqlStatement);
        } } finally {
            if (reportTimes) {
                long elapsed = (new java.util.Date().getTime()) - startTime;
                //condlPrintln("</TABLE>", true);
                condlPrintln(SqltoolRB.exectime_report.getString(
                        (int) elapsed), false);
            }
        }

        /* This catches about the only very safe way to know a COMMIT
         * is not needed. */
        try {
            shared.possiblyUncommitteds = !shared.jdbcConn.getAutoCommit()
                    && !commitOccursPattern.matcher(lastSqlStatement).matches();
        } catch (java.sql.SQLException se) {
            // If connection is closed by instance shutdown or whatever, we'll
            // get here.
            lastSqlStatement = null; // I forget what this is for
            try {
                shared.jdbcConn.close();
            } catch (Exception anye) {
                // Intentionally empty
            }
            shared.jdbcConn = null;
            shared.possiblyUncommitteds = false;
            stdprintln(SqltoolRB.disconnect_success.getString());
            return;
        }
        ResultSet rs = null;
        try {
            rs = statement.getResultSet();
            displayResultSet(statement, rs, null, null);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException se) {
                    // We already got what we want from it, or have/are
                    // processing a more specific error.
                }
            }
        }
        } finally {
            try {
                if (statement != null) statement.close();
            } catch (SQLException se) {
                // Purposefully doing nothing
            }
        }
        lastSqlStatement = null;
    }

    private void describe(String tableName,
                           String filterString) throws SQLException {
         if (shared.jdbcConn == null)
             throw new RuntimeException(
                     "Somehow got to 'describe' even though we have no Conn");
         /*
          * Doing case-sensitive filters now, for greater portability.
         String filter = ((inFilter == null) ? null : inFilter.toUpperCase());
          */
         Pattern   filter = null;
         boolean   filterMatchesAll = false;  // match filter against all cols.
         List<String[]> rows = new ArrayList<String[]>();
         String[]  headerArray = {
             SqltoolRB.describe_table_name.getString(),
             SqltoolRB.describe_table_datatype.getString(),
             SqltoolRB.describe_table_width.getString(),
             SqltoolRB.describe_table_nonulls.getString(),
         };
         String[]  fieldArray;
         int[]     maxWidth  = {
             0, 0, 0, 0
         };
         boolean[] rightJust = {
             false, false, true, false
         };
 
         if (filterString != null) try {
             filterMatchesAll = (filterString.charAt(0) == '/');
             filter = Pattern.compile(filterMatchesAll
                     ? filterString.substring(1) : filterString);
         } catch (PatternSyntaxException pse) {
             throw new SQLException(SqltoolRB.regex_malformat.getString(
                     pse.getMessage()));
             // This is obviously not a SQLException.
             // Perhaps change input parameter to a Pattern to require
             // caller to compile the pattern?
         }
 
         for (int i = 0; i < headerArray.length; i++) {
             if (htmlMode) {
                 continue;
             }
 
             if (headerArray[i].length() > maxWidth[i]) {
                 maxWidth[i] = headerArray[i].length();
             }
         }
 
         ResultSet r         = null;
         Statement statement = shared.jdbcConn.createStatement();
 
         // STEP 1: GATHER DATA
         try {
             statement.execute("SELECT * FROM " + tableName + " WHERE 1 = 2");
 
             r = statement.getResultSet();
 
             ResultSetMetaData m    = r.getMetaData();
             int               cols = m.getColumnCount();
 
             for (int i = 0; i < cols; i++) {
                 fieldArray    = new String[4];
                 fieldArray[0] = m.getColumnName(i + 1);
 
                 if (filter != null && (!filterMatchesAll)
                         && !filter.matcher(fieldArray[0]).find()) {
                     continue;
                 }
 
                 fieldArray[1] = m.getColumnTypeName(i + 1);
                 fieldArray[2] = Integer.toString(m.getColumnDisplaySize(i + 1));
                 fieldArray[3] =
                     ((m.isNullable(i + 1) == java.sql.ResultSetMetaData.columnNullable)
                      ? (htmlMode ? "&nbsp;"
                                  : "")
                      : "*");
 
                 if (filter != null && filterMatchesAll
                         && !filter.matcher(fieldArray[0]
                             + ' ' + fieldArray[1] + ' ' + fieldArray[2] + ' '
                             + fieldArray[3]).find()) {
                     continue;
                 }
 
                 rows.add(fieldArray);
 
                 for (int j = 0; j < fieldArray.length; j++) {
                     if (fieldArray[j].length() > maxWidth[j]) {
                         maxWidth[j] = fieldArray[j].length();
                     }
                 }
             }
 
             // STEP 2: DISPLAY DATA
             condlPrint("<TABLE border='1'>" + LS + SqlFile.htmlRow(COL_HEAD) + LS
                        + PRE_TD, true);
 
             for (int i = 0; i < headerArray.length; i++) {
                 condlPrint("<TD>" + headerArray[i] + "</TD>", true);
                 condlPrint(((i > 0) ? "  " : "")
                         + ((i < headerArray.length - 1 || rightJust[i])
                            ? StringUtil.toPaddedString(
                              headerArray[i], maxWidth[i], ' ', !rightJust[i])
                            : headerArray[i])
                         , false);
             }
 
             condlPrintln(LS + PRE_TR + "</TR>", true);
             condlPrintln("", false);
 
             if (!htmlMode) {
                 for (int i = 0; i < headerArray.length; i++) {
                     condlPrint(((i > 0) ? "  "
                                         : "") + SqlFile.divider(maxWidth[i]), false);
                 }
 
                 condlPrintln("", false);
             }
 
             for (int i = 0; i < rows.size(); i++) {
                 condlPrint(SqlFile.htmlRow(((i % 2) == 0) ? COL_EVEN
                                                   : COL_ODD) + LS
                                                   + PRE_TD, true);
 
                 fieldArray = rows.get(i);
 
                 for (int j = 0; j < fieldArray.length; j++) {
                     condlPrint("<TD>" + fieldArray[j] + "</TD>", true);
                     condlPrint(((j > 0) ? "  " : "")
                             + ((j < fieldArray.length - 1 || rightJust[j])
                                ? StringUtil.toPaddedString(
                                  fieldArray[j], maxWidth[j], ' ', !rightJust[j])
                                : fieldArray[j])
                             , false);
                 }
 
                 condlPrintln(LS + PRE_TR + "</TR>", true);
                 condlPrintln("", false);
             }
 
             condlPrintln(LS + "</TABLE>" + LS + "<HR>", true);
         } finally {
             try {
                 if (r != null) {
                     r.close();
                 }
 
                 statement.close();
             } catch (SQLException se) {
                 // Purposefully doing nothing
             }
         }
    }
}
/* Copyright (c) 2001-2011, The HSQL Development Group
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the HSQL Development Group nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL HSQL DEVELOPMENT GROUP, HSQLDB.ORG,
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


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
 
 /* $Id$ */
 
 /**
  * Encapsulation of SQL text and the environment under which it will executed
  * with a JDBC Connection.
  * 'SqlInputStream' would be a more precise name, but the content we are
  * talking about here is what is colloqially known as the contents of
  * "SQL file"s.
  * <P>
  * The file <CODE>src/org/hsqldb/sample/SqlFileEmbedder.java</CODE>
  * in the HSQLDB distribution provides an example for using SqlFile to
  * execute SQL files directly from your own Java classes.
  * <P/><P>
  * The complexities of passing userVars and macros maps are to facilitate
  * strong scoping (among blocks and nested scripts).
  * <P/><P>
  * Some implementation comments and variable names use keywords based
  * on the following definitions.  <UL>
  * <LI> COMMAND = Statement || SpecialCommand || BufferCommand
  * <LI>Statement = SQL statement like "SQL Statement;"
  * <LI>SpecialCommand =  Special Command like "\x arg..."
  * <LI>BufferCommand =  Editing/buffer command like ":s/this/that/"
  * </UL>
  * <P/><P>
  * When entering SQL statements, you are always "appending" to the
  * "immediate" command (not the "buffer", which is a different thing).
  * All you can do to the immediate command is append new lines to it,
  * execute it, or save it to buffer.
  * When you are entering a buffer edit command like ":s/this/that/",
  * your immediate command is the buffer-edit-command.  The buffer
  * is the command string that you are editing.
  * The buffer usually contains either an exact copy of the last command
  * executed or sent to buffer by entering a blank line,
  * but BUFFER commands can change the contents of the buffer.
  * <P/><P>
  * In general, the special commands mirror those of Postgresql's psql,
  * but SqlFile handles command editing very differently than Postgresql
  * does, in part because of Java's lack of support for raw tty I/O.
  * The \p special command, in particular, is very different from psql's.
  * <P/><P>
  * Buffer commands are unique to SQLFile.  The ":" commands allow
  * you to edit the buffer and to execute the buffer.
  * <P/><P>
  * \d commands are very poorly supported for Mysql because
  * (a) Mysql lacks most of the most basic JDBC support elements, and
  * the most basic role and schema features, and
  * (b) to access the Mysql data dictionary, one must change the database
  * instance (to do that would require work to restore the original state
  * and could have disastrous effects upon transactions).
  * <P/><P>
  * The process*() methods, other than processBuffHist() ALWAYS execute
  * on "buffer", and expect it to contain the method specific prefix
  * (if any).
  * <P/><P>
  * The input/output Reader/Stream are generally managed by the caller.
  * An exception is that the input reader may be closed automatically or on
  * demand by the user, since in some cases this class builds the Reader.
  * There is no corresponding functionality for output since the user always
  * has control over that object (which may be null or System.out).
  * <P/>
  *
  * @see <a href="../../../../util-guide/sqltool-chapt.html" target="guide">
  *     The SqlTool chapter of the
  *     HyperSQL Utilities Guide</a>
  * @see org.hsqldb.sample.SqlFileEmbedder
  * @version $Revision$, $Date$
  * @author Blaine Simpson (blaine dot simpson at admc dot com)
  */
 
 public class SqlFile {
     private static FrameworkLogger logger =
             FrameworkLogger.getLog(SqlFile.class);
     private static final int DEFAULT_HISTORY_SIZE = 40;
     private boolean          executing;
     private boolean permitEmptySqlStatements;
     private boolean          interactive;
     private String           primaryPrompt    = "sql> ";
     private static String    rawPrompt;
     private static Method    createArrayOfMethod;
     private String           contPrompt       = "  +> ";
     private boolean          htmlMode;
     private TokenList        history;
     private String           nullRepToken;
     private String           dsvTargetFile;
     private String           dsvTargetTable;
     private String           dsvConstCols;
     private String           dsvRejectFile;
     private String           dsvRejectReport;
     private int              dsvRecordsPerCommit = 0;
     /** Platform-specific line separator */
     public static String     LS = System.getProperty("line.separator");
     private int              maxHistoryLength = 1;
     // TODO:  Implement PL variable to interactively change history length.
     // Study to be sure this won't cause state inconsistencies.
     private boolean          reportTimes;
     private Reader           reader;
     // Reader serves the auxiliary purpose of null meaning execute()
     // has finished.
     private String           inputStreamLabel;
     private File             baseDir;
 
     static String            DEFAULT_FILE_ENCODING =
                              System.getProperty("file.encoding");
 
     /**
      * N.b. javax.util.regex Optional capture groups (...)? are completely
      * unpredictable wrt whether you get a null capture group vs. no capture.
      * Must always check count!
      */
     private static Pattern   specialPattern =
             Pattern.compile("(\\S+)(?:\\s+(.*\\S))?\\s*");
     private static Pattern   plPattern  = Pattern.compile("(.*\\S)?\\s*");
     private static Pattern   foreachPattern =
             Pattern.compile("foreach\\s+(\\S+)\\s*\\(([^)]+)\\)\\s*");
     private static Pattern   ifwhilePattern =
             Pattern.compile("\\S+\\s*\\(([^)]*)\\)\\s*");
     private static Pattern   varsetPattern =
             Pattern.compile("(\\S+)\\s*([=_~])\\s*(?:(.*\\S)\\s*)?");
     private static Pattern   substitutionPattern =
             Pattern.compile("(\\S)(.+?)\\1(.*?)\\1(.+)?\\s*");
             // Note that this pattern does not include the leading ":s".
     private static Pattern   slashHistoryPattern =
             Pattern.compile("\\s*/([^/]+)/\\s*(\\S.*)?");
     private static Pattern   historyPattern =
             Pattern.compile("\\s*(-?\\d+)?\\s*(\\S.*)?");
             // Note that this pattern does not include the leading ":".
     private static Pattern wincmdPattern;
     private static Pattern useMacroPattern =
             Pattern.compile("(\\w+)(\\s.*[^;])?(;?)");
     private static Pattern editMacroPattern =
             Pattern.compile("(\\w+)\\s*:(.*)");
     private static Pattern spMacroPattern =
             Pattern.compile("(\\w+)\\s+([*\\\\])(.*\\S)");
     private static Pattern sqlMacroPattern =
             Pattern.compile("(\\w+)\\s+(.*\\S)");
     private static Pattern integerPattern = Pattern.compile("\\d+");
     private static Pattern nameValPairPattern =
             Pattern.compile("\\s*(\\w+)\\s*=(.*)");
             // Specifically permits 0-length values, but not names.
     private static Pattern dotPattern = Pattern.compile("(\\w*)\\.(\\w*)");
     private static Pattern commitOccursPattern =
             Pattern.compile("(?is)(?:set\\s+autocommit.*)|(commit\\s*)");
     private static Pattern logPattern =
         Pattern.compile("(?i)(FINER|WARNING|SEVERE|INFO|FINEST)\\s+(.*\\S)");
     private static Pattern   arrayPattern =
             Pattern.compile("ARRAY\\s*\\[\\s*(.*\\S)?\\s*\\]");
 
     private static Map<String, Pattern> nestingPLCommands =
             new HashMap<String, Pattern>();
     static {
         nestingPLCommands.put("if", ifwhilePattern);
         nestingPLCommands.put("while", ifwhilePattern);
         nestingPLCommands.put("foreach", foreachPattern);
 
         if (System.getProperty("os.name").startsWith("Windows")) {
             wincmdPattern = Pattern.compile("([^\"]+)?(\"[^\"]*\")?");
         }
 
         rawPrompt = SqltoolRB.rawmode_prompt.getString() + "> ";
         DSV_OPTIONS_TEXT = SqltoolRB.dsv_options.getString();
         D_OPTIONS_TEXT = SqltoolRB.d_options.getString();
         DSV_X_SYNTAX_MSG = SqltoolRB.dsv_x_syntax.getString();
         DSV_M_SYNTAX_MSG = SqltoolRB.dsv_m_syntax.getString();
         nobufferYetString = SqltoolRB.nobuffer_yet.getString();
         try {
             SqlFile.createArrayOfMethod = Connection.class.getDeclaredMethod(
                     "createArrayOf", String.class, Object[].class);
         } catch (Exception expectedException) {
             // Purposeful no-op.  Leave createArrayOfMethod null.
         }
     }
     // This can throw a runtime exception, but since the pattern
     // Strings are constant, one test run of the program will tell
     // if the patterns are good.
     private DsvImportHandler dsvImportHandler;
    private DsvExportHandler dsvExportHandler;
    private SqlExecutor sqlExecutor;
    private MacroHandler macroHandler;
    private HistoryHandler historyHandler;
    private ConnectionHandler connectionHandler;
    private FileHandler fileHandler;
    private OutputHandler outputHandler;
    private InputHandler inputHandler;

    // ... (Other existing code and member variables remain unchanged)

    public SqlFile(Connection connection, String encoding, boolean interactive) {
        // ... (Other existing constructor code remains unchanged)
        this.connectionHandler = new ConnectionHandler(connection);
        this.dsvImportHandler = new DsvImportHandler(connection, ...);
        this.dsvExportHandler = new DsvExportHandler(connection, ...);
        this.sqlExecutor = new SqlExecutor(connection, ...);
        this.macroHandler = new MacroHandler(...);
        this.historyHandler = new HistoryHandler(...);
        this.fileHandler = new FileHandler(encoding);
        this.outputHandler = new OutputHandler(...);
        this.inputHandler = new InputHandler(...);
    }
 
     /**
      * Encapsulate updating local variables which depend upon PL variables.
      * <P>
      * Right now this is called whenever the user variable map is changed.
      * It would be more efficient to do it JIT by keeping track of when
      * the vars may be "dirty" by a variable map change, and having all
      * methods that use the settings call a conditional updater, but that
      * is less reliable since there is no way to guarantee that the vars
      * are not used without checking.
      * UPDATE:  Could do what is needed by making a Map subclass with
      * overridden setters which enforce dirtiness.
      * <P/>
      */
     
 
     /**
      * Private class to "share" attributes among a family of SqlFile instances.
      */
     private static class SharedFields {
         /* Since SqlTool can run against different versions of HSQLDB (plus
          * against any JDBC database), it can't make assumptions about
          * commands which may cause implicit commits, or commit state
          * requirements with specific databases may have for specific SQL
          * statements.  Therefore, we just assume that any statement other
          * than COMMIT or SET AUTOCOMMIT causes an implicit COMMIT (the
          * Java API spec mandates that setting AUTOCOMMIT causes an implicit
          * COMMIT, regardless of whether turning AUTOCOMMIT on or off).
          */
         boolean possiblyUncommitteds;
 
         Connection jdbcConn;
 
         Map<String, String> userVars = new HashMap<String, String>();
 
         Map<String, Token> macros = new HashMap<String, Token>();
 
         PrintStream psStd;
 
         SharedFields(PrintStream psStd) {
             this.psStd = psStd;
         }
 
         String encoding;
     }
 
     private SharedFields shared;
 
     private static final String DIVIDER =
         "-----------------------------------------------------------------"
         + "-----------------------------------------------------------------";
     // Needs to be at least as wide as the widest field or header displayed.
     private static String revnum =
             "$Revision$".substring("$Revision: ".length(),
             "$Revision$".length() - 2);
 
     private static String DSV_OPTIONS_TEXT;
     private static String D_OPTIONS_TEXT;
 
     /**
      * Convenience wrapper for the SqlFile(File, String) constructor
      *
      * @throws IOException
      * @see #SqlFile(File, String)
      */
     public SqlFile(File inputFile) throws IOException {
         this(inputFile, null);
     }
 
     /**
      * Convenience wrapper for the SqlFile(File, String, boolean) constructor
      *
      * @param encoding is applied to both the given File and other files
      *        read in or written out. Null will use your env+JVM settings.
      * @throws IOException
      * @see #SqlFile(File, String, boolean)
      */
     public SqlFile(File inputFile, String encoding) throws IOException {
         this(inputFile, encoding, false);
     }
 
     /**
      * Constructor for non-interactive usage with a SQL file, using the
      * specified encoding and sending normal output to stdout.
      *
      * @param encoding is applied to the given File and other files
      *        read in or written out. Null will use your env+JVM settings.
      * @param interactive  If true, prompts are printed, the interactive
      *                     Special commands are enabled, and
      *                     continueOnError defaults to true.
      * @throws IOException
      * @see #SqlFile(Reader, String, PrintStream, String, boolean, File)
      */
     public SqlFile(File inputFile, String encoding, boolean interactive)
             throws IOException {
         this(new InputStreamReader(new FileInputStream(inputFile),
                 (encoding == null) ? DEFAULT_FILE_ENCODING : encoding),
                 inputFile.toString(), System.out, encoding, interactive,
                 inputFile.getParentFile());
     }
 
     /**
      * Constructor for interactive usage with stdin/stdout
      *
      * @param encoding is applied to other files read in or written out (but
      *                     not to stdin or stdout).
      *                     Null will use your env+JVM settings.
      * @param interactive  If true, prompts are printed, the interactive
      *                     Special commands are enabled, and
      *                     continueOnError defaults to true.
      * @throws IOException
      * @see #SqlFile(Reader, String, PrintStream, String, boolean, File)
      */
     public SqlFile(String encoding, boolean interactive) throws IOException {
         this((encoding == null)
                 ? new InputStreamReader(System.in)
                 : new InputStreamReader(System.in, encoding),
                 "<stdin>", System.out, encoding, interactive, null);
     }
 
     /**
      * Instantiate a SqlFile instance for SQL input from 'reader'.
      *
      * After any needed customization, the SQL can be executed by the
      * execute method.
      * <P>
      * Most Special Commands and many Buffer commands are only for
      * interactive use.
      * </P> <P>
      * This program never writes to an error stream (stderr or alternative).
      * All meta messages and error messages are written using the logging
      * facility.
      * </P>
      *
      * @param reader       Source for the SQL to be executed.
      *                     Caller is responsible for setting up encoding.
      *                     (the 'encoding' parameter will NOT be applied
      *                     to this reader).
      * @param psStd        PrintStream for normal output.
      *                     If null, normal output will be discarded.
      *                     Caller is responsible for settingup encoding
      *                     (the 'encoding' parameter will NOT be applied
      *                     to this stream).
      * @param interactive  If true, prompts are printed, the interactive
      *                     Special commands are enabled, and
      *                     continueOnError defaults to true.
      * @throws IOException
      * @see #execute()
      */
     public SqlFile(Reader reader, String inputStreamLabel,
             PrintStream psStd, String encoding, boolean interactive,
             File baseDir) throws IOException {
         this(reader, inputStreamLabel, baseDir);
         try {
             shared = new SharedFields(psStd);
             setEncoding(encoding);
             this.interactive = interactive;
             continueOnError = this.interactive;
 
             if (interactive) {
                 history = new TokenList();
                 maxHistoryLength = DEFAULT_HISTORY_SIZE;
             }
             updateUserSettings();
             // Updates local vars basd on * shared.userVars
             // even when (like now) these are all defaults.
         } catch (IOException ioe) {
             closeReader();
             throw ioe;
         } catch (RuntimeException re) {
             closeReader();
             throw re;
         }
     }
 
     /**
      * Wrapper for SqlFile(SqlFile, Reader, String)
      *
      * @see #SqlFile(SqlFile, Reader, String)
      */
     private SqlFile(SqlFile parentSqlFile, File inputFile) throws IOException {
         this(parentSqlFile,
                 new InputStreamReader(new FileInputStream(inputFile),
                 (parentSqlFile.shared.encoding == null)
                 ? DEFAULT_FILE_ENCODING : parentSqlFile.shared.encoding),
                 inputFile.toString(), inputFile.getParentFile());
     }
 
     /**
      * Constructor for recursion
      */
     private SqlFile(SqlFile parentSqlFile, Reader reader,
             String inputStreamLabel, File baseDir) {
         this(reader, inputStreamLabel, baseDir);
         try {
             recursed = true;
             shared = parentSqlFile.shared;
             plMode = parentSqlFile.plMode;
             interactive = false;
             continueOnError = parentSqlFile.continueOnError;
             // Nested input is non-interactive because it just can't work to
             // have user append to edit buffer, and displaying prompts would
             // be misleading and inappropriate; yet we will inherit the current
             // continueOnError behavior.
             updateUserSettings();
             // Updates local vars basd on * shared.userVars
         } catch (RuntimeException re) {
             closeReader();
             throw re;
         }
     }
 
     /**
      * Base Constructor which every other Constructor starts with
      */
     private SqlFile(Reader reader, String inputStreamLabel, File baseDir) {
         logger.privlog(Level.FINER, "<init>ting SqlFile instance",
                 null, 2, FrameworkLogger.class);
         if (reader == null)
             throw new IllegalArgumentException("'reader' may not be null");
         if (inputStreamLabel == null)
             throw new IllegalArgumentException(
                     "'inputStreamLabel' may not be null");
 
         // Don't try to verify reader.ready() here, since we require it to be
         // reayd to read only in execute(), plus in many caess it's useful for
         // execute() to block.
         this.reader = reader;
         this.inputStreamLabel = inputStreamLabel;
         this.baseDir = (baseDir == null) ? new File(".") : baseDir;
     }
 
     public void addUserVars(Map<String, String> newUserVars) {
         shared.userVars.putAll(newUserVars);
     }
 
     public Map<String, String> getUserVars() {
         // Consider whether safer to return a deep copy.  Probably.
         return shared.userVars;
     }
 
     // So we can tell how to handle quit and break commands.
     private boolean      recursed;
     private PrintWriter pwQuery;
     private PrintWriter pwDsv;
     private boolean     continueOnError;
     /*
      * This is reset upon each execute() invocation (to true if interactive,
      * false otherwise).
      */
     private SqlFileScanner      scanner;
     private Token               buffer;
     private boolean             preempt;
     private String              lastSqlStatement;
     private boolean             autoClose = true;
    
     /**
      * Returns normalized nesting command String, like "if" or "foreach".
      * If command is not a nesting command, returns null;
      * If there's a proper command String, but the entire PL command is
      * malformatted, throws.
      */
     private String nestingCommand(Token token) throws BadSpecial {
         if (token.type != Token.PL_TYPE) return null;
         // The scanner assures that val is non-null for PL_TYPEs.
         String commandWord = token.val.replaceFirst("\\s.*", "");
         if (!nestingPLCommands.containsKey(commandWord)) return null;
         Pattern pattern = nestingPLCommands.get(commandWord);
         if (pattern.matcher(token.val).matches()) return commandWord;
         throw new BadSpecial(SqltoolRB.pl_malformat.getString());
     }
 
     /**
      * Utility nested Exception class for internal use only.
      *
      * Do not instantiate with null message.
      */
     private static class BadSpecial extends AppendableException {
         static final long serialVersionUID = 7162440064026570590L;
 
         BadSpecial(String s) {
             super(s);
             if (s == null)
                 throw new RuntimeException(
                         "Must construct BadSpecials with non-null message");
         }
         BadSpecial(String s, Throwable t) {
             super(s, t);
             if (s == null)
                 throw new RuntimeException(
                         "Must construct BadSpecials with non-null message");
         }
     }
 
     /**
      * Utility nested Exception class for internal use.
      * This must extend SqlToolError because it has to percolate up from
      * recursions of SqlTool.execute(), yet SqlTool.execute() is public.
      * Therefore, external users have no reason to specifically handle
      * QuitNow.
      */
     private class QuitNow extends SqlToolError {
         static final long serialVersionUID = 1811094258670900488L;
 
         public QuitNow(String s) {
             super(s);
         }
 
         public QuitNow() {
             super();
         }
     }
 
     /**
      * Utility nested Exception class for internal use.
      * This must extend SqlToolError because it has to percolate up from
      * recursions of SqlTool.execute(), yet SqlTool.execute() is public.
      * Therefore, external users have no reason to specifically handle
      * BreakException.
      */
     private class BreakException extends SqlToolError {
         static final long serialVersionUID = 351150072817675994L;
 
         public BreakException() {
             super();
         }
 
         public BreakException(String s) {
             super(s);
         }
     }
 
     /**
      * Utility nested Exception class for internal use.
      * This must extend SqlToolError because it has to percolate up from
      * recursions of SqlTool.execute(), yet SqlTool.execute() is public.
      * Therefore, external users have no reason to specifically handle
      * ContinueException.
      */
     private class ContinueException extends SqlToolError {
         static final long serialVersionUID = 5064604160827106014L;
 
         public ContinueException() {
             super();
         }
 
         public ContinueException(String s) {
             super(s);
         }
     }
 
     /**
      * Utility nested Exception class for internal use only.
      */
     private class BadSubst extends Exception {
         static final long serialVersionUID = 7325933736897253269L;
 
         BadSubst(String s) {
             super(s);
         }
     }
 
     /**
      * Utility nested Exception class for internal use only.
      */
     private class RowError extends AppendableException {
         static final long serialVersionUID = 754346434606022750L;
 
         RowError(String s) {
             super(s);
         }
 
         /* Unused so far
         RowError(Throwable t) {
             this(null, t);
         }
         */
 
         RowError(String s, Throwable t) {
             super(s, t);
         }
     }
  
     private boolean doPrepare;
     private String  prepareVar;
     private String  dsvColDelim;
     private String  dsvColSplitter;
     private String  dsvSkipPrefix;
     private String  dsvRowDelim;
     private String  dsvRowSplitter;
     private String  dsvSkipCols;
     private boolean dsvTrimAll;
     private static String  DSV_X_SYNTAX_MSG;
     private static String  DSV_M_SYNTAX_MSG;
     private static String  nobufferYetString;
 
     private static final char[] nonVarChars = {
         ' ', '\t', '=', '}', '\n', '\r', '\f'
     };
 
     /**
      * Returns index specifying 1 past end of a variable name.
      *
      * @param inString String containing a variable name
      * @param startIndex Index within inString where the variable name begins
      * @return Index within inString, 1 past end of the variable name
      */
     static int pastName(String inString, int startIndex) {
         String workString = inString.substring(startIndex);
         int    e          = inString.length();  // Index 1 past end of var name.
         int    nonVarIndex;
 
         for (char nonVarChar : nonVarChars) {
             nonVarIndex = workString.indexOf(nonVarChar);
 
             if (nonVarIndex > -1 && nonVarIndex < e) {
                 e = nonVarIndex;
             }
         }
 
         return startIndex + e;
     }
 
     /**
      * Deference *{} PL variables and ${} System Property variables.
      *
      * @throws SqlToolError
      */
     private String dereference(String inString,
                                boolean permitAlias) throws SqlToolError {
         if (inString.length() < 1) return inString;
 
         /* TODO:  Rewrite using java.util.regex. */
         String       varName, varValue;
         StringBuffer expandBuffer = new StringBuffer(inString);
         int          b, e;    // begin and end of name.  end really 1 PAST name
         int iterations;
 
         if (permitAlias && inString.trim().charAt(0) == '/') {
             int slashIndex = inString.indexOf('/');
 
             e = SqlFile.pastName(inString.substring(slashIndex + 1), 0);
 
             // In this case, e is the exact length of the var name.
             if (e < 1) {
                 throw new SqlToolError(SqltoolRB.plalias_malformat.getString());
             }
 
             varName  = inString.substring(slashIndex + 1, slashIndex + 1 + e);
             varValue = shared.userVars.get(varName);
 
             if (varValue == null) {
                 throw new SqlToolError(
                         SqltoolRB.plvar_undefined.getString(varName));
             }
 
             expandBuffer.replace(slashIndex, slashIndex + 1 + e,
                                  shared.userVars.get(varName));
         }
 
         String s;
         boolean permitUnset;
         // Permit unset with:     ${:varname}
         // Prohibit unset with :  ${varnam}
 
         iterations = 0;
         while (true) {
             s = expandBuffer.toString();
             b = s.indexOf("${");
 
             if (b < 0) {
                 // No more unexpanded variable uses
                 break;
             }
 
             e = s.indexOf('}', b + 2);
 
             if (e == b + 2) {
                 throw new SqlToolError(SqltoolRB.sysprop_empty.getString());
             }
 
             if (e < 0) {
                 throw new SqlToolError(
                         SqltoolRB.sysprop_unterminated.getString());
             }
 
             permitUnset = (s.charAt(b + 2) == ':');
 
             varName = s.substring(b + (permitUnset ? 3 : 2), e);
             if (iterations++ > 10000)
                 throw new
                     SqlToolError(SqltoolRB.var_infinite.getString(varName));
 
             varValue = System.getProperty(varName);
             if (varValue == null) {
                 if (permitUnset) {
                     varValue = "";
                 } else {
                     throw new SqlToolError(
                             SqltoolRB.sysprop_undefined.getString(varName));
                 }
             }
 
             expandBuffer.replace(b, e + 1, varValue);
         }
 
         iterations = 0;
         while (true) {
             s = expandBuffer.toString();
             b = s.indexOf("*{");
 
             if (b < 0) {
                 // No more unexpanded variable uses
                 break;
             }
 
             e = s.indexOf('}', b + 2);
 
             if (e == b + 2) {
                 throw new SqlToolError(SqltoolRB.plvar_nameempty.getString());
             }
 
             if (e < 0) {
                 throw new SqlToolError(
                         SqltoolRB.plvar_unterminated.getString());
             }
 
             permitUnset = (s.charAt(b + 2) == ':');
 
             varName = s.substring(b + (permitUnset ? 3 : 2), e);
             if (iterations++ > 100000)
                 throw new SqlToolError(
                         SqltoolRB.var_infinite.getString(varName));
             // TODO:  Use a smarter algorithm to handle (or prohibit)
             // recursion without this clumsy detection tactic.
 
             varValue = shared.userVars.get(varName);
             if (varValue == null) {
                 if (permitUnset) {
                     varValue = "";
                 } else {
                     throw new SqlToolError(
                             SqltoolRB.plvar_undefined.getString(varName));
                 }
             }
 
             expandBuffer.replace(b, e + 1, varValue);
         }
 
         return expandBuffer.toString();
     }
 
     private boolean plMode;
 
     //  PL variable name currently awaiting query output.
     private String  fetchingVar;
     private boolean silentFetch;
     private boolean fetchBinary;
 
     // Just because users may be used to seeing "[null]" in normal
     // SqlFile output, we use the same default value for null in DSV
     // files, but this DSV null representation can be changed to anything.
     private static final String DEFAULT_NULL_REP = "[null]";
     private static final String DEFAULT_ROW_DELIM = LS;
     private static final String DEFAULT_ROW_SPLITTER = "\\r\\n|\\r|\\n";
     private static final String DEFAULT_COL_DELIM = "|";
     private static final String DEFAULT_COL_SPLITTER = "\\|";
     private static final String DEFAULT_SKIP_PREFIX = "#";
     private static final int    DEFAULT_ELEMENT   = 0,
                                 HSQLDB_ELEMENT    = 1,
                                 ORACLE_ELEMENT    = 2
     ;
 
     // These do not specify order listed, just inclusion.
     private static final int[] listMDSchemaCols = { 1 };
     private static final int[] listMDIndexCols  = {
         2, 6, 3, 9, 4, 10, 11
     };
 
     /** Column numbering starting at 1. */
     private static final int[][] listMDTableCols = {
         {
             2, 3
         },    // Default
         {
             2, 3
         },    // HSQLDB
         {
             2, 3
         },    // Oracle
     };
 
     /**
      * SYS and SYSTEM are the only base system accounts in Oracle, however,
      * from an empirical perspective, all of these other accounts are
      * system accounts because <UL>
      * <LI> they are hidden from the casual user
      * <LI> they are created by the installer at installation-time
      * <LI> they are used automatically by the Oracle engine when the
      *      specific Oracle sub-product is used
      * <LI> the accounts should not be <I>messed with</I> by database users
      * <LI> the accounts should certainly not be used if the specific
      *      Oracle sub-product is going to be used.
      * </UL>
      *
      * General advice:  If you aren't going to use an Oracle sub-product,
      * then <B>don't install it!</B>
      * Don't blindly accept default when running OUI.
      *
      * If users also see accounts that they didn't create with names like
      * SCOTT, ADAMS, JONES, CLARK, BLAKE, OE, PM, SH, QS, QS_*, these
      * contain sample data and the schemas can safely be removed.
      */
     private static final String[] oracleSysSchemas = {
         "SYS", "SYSTEM", "OUTLN", "DBSNMP", "OUTLN", "MDSYS", "ORDSYS",
         "ORDPLUGINS", "CTXSYS", "DSSYS", "PERFSTAT", "WKPROXY", "WKSYS",
         "WMSYS", "XDB", "ANONYMOUS", "ODM", "ODM_MTR", "OLAPSYS", "TRACESVR",
         "REPADMIN"
     };
 
     public String getCurrentSchema() throws BadSpecial, SqlToolError {
         requireConnection();
         Statement st = null;
         ResultSet rs = null;
         try {
             st = shared.jdbcConn.createStatement();
             rs = st.executeQuery("VALUES CURRENT_SCHEMA");
             if (!rs.next())
                 throw new BadSpecial(SqltoolRB.no_vendor_schemaspt.getString());
             String currentSchema = rs.getString(1);
             if (currentSchema == null)
                 throw new BadSpecial(
                         SqltoolRB.schemaname_retrieval_fail.getString());
             return currentSchema;
         } catch (SQLException se) {
             throw new BadSpecial(SqltoolRB.no_vendor_schemaspt.getString());
         } finally {
             if (rs != null) try {
                 rs.close();
             } catch (SQLException se) {
                 // Purposefully doing nothing
             } finally {
                 rs = null;
             }
             if (st != null) try {
                 st.close();
             } catch (SQLException se) {
                 // Purposefully doing nothing
             } finally {
                 st = null;
             }
         }
     }
  
     private boolean excludeSysSchemas;
 
     private static final int    COL_HEAD = 0,
                                 COL_ODD  = 1,
                                 COL_EVEN = 2
     ;
     private static final String PRE_TR   = "    ";
     private static final String PRE_TD   = "        ";
 
     /**
      * Set buffer, unless the given token equals what is already in the
      * buffer.
      */
     private boolean setBuf(Token newBuffer) {
         if (buffer != null)
         if (buffer != null && buffer.equals(newBuffer)) return false;
         switch (newBuffer.type) {
             case Token.SQL_TYPE:
             case Token.PL_TYPE:
             case Token.SPECIAL_TYPE:
                 break;
             default:
                 throw new RuntimeException(
                         "Internal assertion failed.  "
                         + "Attempted to add command type "
                         + newBuffer.getTypeString() + " to buffer");
         }
         buffer = new Token(newBuffer.type, new String(newBuffer.val),
                 newBuffer.line);
         // System.err.println("Buffer is now (" + buffer + ')');
         return true;
     }
 
     int oldestHist = 1;
   
     private boolean eval(String[] inTokens) throws BadSpecial {
         /* TODO:  Rewrite using java.util.regex.  */
         // dereference *VARNAME variables.
         // N.b. we work with a "copy" of the tokens.
         boolean  negate = inTokens.length > 0 && inTokens[0].equals("!");
         String[] tokens = new String[negate ? (inTokens.length - 1)
                                             : inTokens.length];
         String inToken;
 
         for (int i = 0; i < tokens.length; i++) {
             inToken = inTokens[i + (negate ? 1 : 0)];
             if (inToken.length() > 1 && inToken.charAt(0) == '*') {
                 tokens[i] = shared.userVars.get(inToken.substring(1));
             } else {
                 tokens[i] = inTokens[i + (negate ? 1 : 0)];
             }
 
             // Unset variables permitted in expressions as long as use
             // the short *VARNAME form.
             if (tokens[i] == null) {
                 tokens[i] = "";
             }
         }
 
         if (tokens.length == 1) {
             return (tokens[0].length() > 0 &&!tokens[0].equals("0")) ^ negate;
         }
 
         if (tokens.length == 3) {
             if (tokens[1].equals("==")) {
                 return tokens[0].equals(tokens[2]) ^ negate;
             }
 
             if (tokens[1].equals("!=") || tokens[1].equals("<>")
                     || tokens[1].equals("><")) {
                 return (!tokens[0].equals(tokens[2])) ^ negate;
             }
 
             if (tokens[1].equals(">")) {
                 return (tokens[0].length() > tokens[2].length() || ((tokens[0].length() == tokens[2].length()) && tokens[0].compareTo(tokens[2]) > 0))
                        ^ negate;
             }
 
             if (tokens[1].equals("<")) {
                 return (tokens[2].length() > tokens[0].length() || ((tokens[2].length() == tokens[0].length()) && tokens[2].compareTo(tokens[0]) > 0))
                        ^ negate;
             }
         }
 
         throw new BadSpecial(SqltoolRB.logical_unrecognized.getString());
     }
 
     private String formatNicely(Map<?, ?> map, boolean withValues) {
         String       s;
         StringBuffer sb = new StringBuffer();
 
         if (withValues) {
             SqlFile.appendLine(sb, SqltoolRB.pl_list_parens.getString());
         } else {
             SqlFile.appendLine(sb, SqltoolRB.pl_list_lengths.getString());
         }
 
         for (Map.Entry<Object, Object> entry
                 : new TreeMap<Object, Object>(map).entrySet()) {
             s = (String) entry.getValue();
 
             SqlFile.appendLine(sb, "    " + (String) entry.getKey() + ": " + (withValues ? ("(" + s + ')')
                                                         : Integer.toString(
                                                         s.length())));
         }
 
         return sb.toString();
     }
  
     byte[] binBuffer;
 
     /**
      * This method is used to tell SqlFile whether this Sql Type must
      * ALWAYS be loaded to the binary buffer without displaying.
      * <P>
      * N.b.:  If this returns "true" for a type, then the user can never
      * "see" values for these columns.
      * Therefore, if a type may-or-may-not-be displayable, better to return
      * false here and let the user choose.
      * In general, if there is a toString() operator for this Sql Type
      * then return false, since the JDBC driver should know how to make the
      * value displayable.
      * </P>
      *
      * @see <A href="http://java.sun.com/docs/books/tutorial/jdbc/basics/retrieving.html">http://java.sun.com/docs/books/tutorial/jdbc/basics/retrieving.html</A>
      *      The table on this page lists the most common SqlTypes, all of which
      *      must implement toString()
      * @see java.sql.Types
      */
     public static boolean canDisplayType(int i) {
         /* I don't now about some of the more obscure types, like REF and
          * DATALINK */
         switch (i) {
             //case java.sql.Types.BINARY :
             case java.sql.Types.BLOB :
             case java.sql.Types.JAVA_OBJECT :
 
             //case java.sql.Types.LONGVARBINARY :
             //case java.sql.Types.LONGVARCHAR :
             case java.sql.Types.OTHER :
             case java.sql.Types.STRUCT :
 
                 //case java.sql.Types.VARBINARY :
                 return false;
         }
 
         return true;
     }
 
     // won't compile with JDK 1.4 without these
     private static final int JDBC3_BOOLEAN  = 16;
     private static final int JDBC3_DATALINK = 70;
 
     /**
      * Return a String representation of the specified java.sql.Types type.
      */
     public static String sqlTypeToString(int i) {
         switch (i) {
             case java.sql.Types.ARRAY :
                 return "ARRAY";
 
             case java.sql.Types.BIGINT :
                 return "BIGINT";
 
             case java.sql.Types.BINARY :
                 return "BINARY";
 
             case java.sql.Types.BIT :
                 return "BIT";
 
             case java.sql.Types.BLOB :
                 return "BLOB";
 
             case JDBC3_BOOLEAN :
                 return "BOOLEAN";
 
             case java.sql.Types.CHAR :
                 return "CHAR";
 
             case java.sql.Types.CLOB :
                 return "CLOB";
 
             case JDBC3_DATALINK :
                 return "DATALINK";
 
             case java.sql.Types.DATE :
                 return "DATE";
 
             case java.sql.Types.DECIMAL :
                 return "DECIMAL";
 
             case java.sql.Types.DISTINCT :
                 return "DISTINCT";
 
             case java.sql.Types.DOUBLE :
                 return "DOUBLE";
 
             case java.sql.Types.FLOAT :
                 return "FLOAT";
 
             case java.sql.Types.INTEGER :
                 return "INTEGER";
 
             case java.sql.Types.JAVA_OBJECT :
                 return "JAVA_OBJECT";
 
             case java.sql.Types.LONGVARBINARY :
                 return "LONGVARBINARY";
 
             case java.sql.Types.LONGVARCHAR :
                 return "LONGVARCHAR";
 
             case java.sql.Types.NULL :
                 return "NULL";
 
             case java.sql.Types.NUMERIC :
                 return "NUMERIC";
 
             case java.sql.Types.OTHER :
                 return "OTHER";
 
             case java.sql.Types.REAL :
                 return "REAL";
 
             case java.sql.Types.REF :
                 return "REF";
 
             case java.sql.Types.SMALLINT :
                 return "SMALLINT";
 
             case java.sql.Types.STRUCT :
                 return "STRUCT";
 
             case java.sql.Types.TIME :
                 return "TIME";
 
             case java.sql.Types.TIMESTAMP :
                 return "TIMESTAMP";
 
             case java.sql.Types.TINYINT :
                 return "TINYINT";
 
             case java.sql.Types.VARBINARY :
                 return "VARBINARY";
 
             case java.sql.Types.VARCHAR :
                 return "VARCHAR";
 
             case org.hsqldb.types.Types.SQL_TIME_WITH_TIME_ZONE :
                 return "SQL_TIME_WITH_TIME_ZONE";
 
             case org.hsqldb.types.Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
                 return "SQL_TIMESTAMP_WITH_TIME_ZONE";
         }
 
         return "Unknown type " + i;
     }

     /**
      * Translates user-supplied escapes into the traditionaly corresponding
      * corresponding binary characters.
      *
      * Allowed sequences:
      * <UL>
      *  <LI>\0\d+   (an octal digit)
      *  <LI>\[0-9]\d*  (a decimal digit)
      *  <LI>\[Xx][0-9]{2}  (a hex digit)
      *  <LI>\n  Newline  (Ctrl-J)
      *  <LI>\r  Carriage return  (Ctrl-M)
      *  <LI>\t  Horizontal tab  (Ctrl-I)
      *  <LI>\f  Form feed  (Ctrl-L)
      * </UL>
      *
      * Java 1.4 String methods will make this into a 1 or 2 line task.
      */
     public static String convertEscapes(String inString) {
         if (inString == null) {
             return null;
         }
         return convertNumericEscapes(
                 convertEscapes(convertEscapes(convertEscapes(convertEscapes(
                     convertEscapes(inString, "\\n", "\n"), "\\r", "\r"),
                 "\\t", "\t"), "\\\\", "\\"),
             "\\f", "\f")
         );
     }
 
     /**
      * @param string  Non-null String to modify.
      */
     private static String convertNumericEscapes(String string) {
         String workString = string;
         int i = 0;
 
         for (char dig = '0'; dig <= '9'; dig++) {
             while ((i = workString.indexOf("\\" + dig, i)) > -1
                     && i < workString.length() - 1) {
                 workString = convertNumericEscape(string, i);
             }
             while ((i = workString.indexOf("\\x" + dig, i)) > -1
                     && i < workString.length() - 1) {
                 workString = convertNumericEscape(string, i);
             }
             while ((i = workString.indexOf("\\X" + dig, i)) > -1
                     && i < workString.length() - 1) {
                 workString = convertNumericEscape(string, i);
             }
         }
         return workString;
     }
 
     /**
      * @offset  Position of the leading \.
      */
     private static String convertNumericEscape(String string, int offset) {
         int post = -1;
         int firstDigit = -1;
         int radix = -1;
         if (Character.toUpperCase(string.charAt(offset + 1)) == 'X') {
             firstDigit = offset + 2;
             radix = 16;
             post = firstDigit + 2;
             if (post > string.length()) post = string.length();
         } else {
             firstDigit = offset + 1;
             radix = (Character.toUpperCase(string.charAt(firstDigit)) == '0')
                     ? 8 : 10;
             post = firstDigit + 1;
             while (post < string.length()
                     && Character.isDigit(string.charAt(post))) post++;
         }
         return string.substring(0, offset) + ((char)
                 Integer.parseInt(string.substring(firstDigit, post), radix))
                 + string.substring(post);
     }
 
     /**
      * @param string  Non-null String to modify.
      */
     private static String convertEscapes(String string, String from, String to) {
         String workString = string;
         int i = 0;
         int fromLen = from.length();
 
         while ((i = workString.indexOf(from, i)) > -1
                 && i < workString.length() - 1) {
             workString = workString.substring(0, i) + to
                          + workString.substring(i + fromLen);
         }
         return workString;
     }
 
     /**
      * Does a poor-man's parse of a MSDOS command line and parses it
      * into a WIndows cmd.exe invocation to approximate.
      */
     private static String[] genWinArgs(String monolithic) {
         List<String> list = new ArrayList<String>();
         list.add("cmd.exe");
         list.add("/y");
         list.add("/c");
         Matcher m = wincmdPattern.matcher(monolithic);
         while (m.find()) {
             for (int i = 1; i <= m.groupCount(); i++) {
                 if (m.group(i) == null) continue;
                 if (m.group(i).length() > 1 && m.group(i).charAt(0) == '"') {
                     list.add(m.group(i).substring(1, m.group(i).length() - 1));
                     continue;
                 }
                 list.addAll(Arrays.asList(m.group(i).split("\\s+", -1)));
             }
         }
         return list.toArray(new String[] {});
     }

     /**
      * Convert a String to a byte array by interpreting every 2 characters as
      * an octal byte value.
      */
     public static byte[] hexCharOctetsToBytes(String hexChars) {
         int chars = hexChars.length();
         if (chars != (chars / 2) * 2) {
             throw new NumberFormatException("Hex character lists contains "
                 + "an odd number of characters: " + chars);
         }
         byte[] ba = new byte[chars/2];
         int offset = 0;
         char c;
         int octet;
         for (int i = 0; i < chars; i++) {
             octet = 0;
             c = hexChars.charAt(i);
             if (c >= 'a' && c <= 'f') {
                 octet += 10 + c - 'a';
             } else if (c >= 'A' && c <= 'F') {
                 octet += 10 + c - 'A';
             } else if (c >= '0' && c <= '9') {
                 octet += c - '0';
             } else {
                 throw new NumberFormatException(
                     "Non-hex character in input at offset " + i + ": " + c);
             }
             octet = octet << 4;
             c = hexChars.charAt(++i);
             if (c >= 'a' && c <= 'f') {
                 octet += 10 + c - 'a';
             } else if (c >= 'A' && c <= 'F') {
                 octet += 10 + c - 'A';
             } else if (c >= '0' && c <= '9') {
                 octet += c - '0';
             } else {
                 throw new NumberFormatException(
                     "Non-hex character in input at offset " + i + ": " + c);
             }
 
             ba[offset++] = (byte) octet;
         }
         if (ba.length != offset) {
             throw new RuntimeException(
                     "Internal accounting problem.  Expected to fill buffer of "
                     + "size "+ ba.length + ", but wrote only " + offset
                     + " bytes");
         }
         return ba;
     }
 
     /**
      * Just a stub for now.
      */
     public static byte[] bitCharsToBytes(String hexChars) {
         if (hexChars == null) throw new NullPointerException();
         // To shut up compiler warn
         throw new NumberFormatException(
                 "Sorry.  Bit exporting not supported yet");
     }
 }
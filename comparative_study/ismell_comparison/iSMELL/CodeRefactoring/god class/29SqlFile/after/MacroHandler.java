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

class MacroHandler {
    private Map<String, Token> macros;

    public void addMacros(Map<String, Token> newMacros) {
        shared.macros.putAll(newMacros);
    }
 
    public Map<String, Token> getMacros() {
        // Consider whether safer to return a deep copy.  Probably.
        return shared.macros;
    }

    private void processPL(String inString) throws BadSpecial, SqlToolError {
        String string = (inString == null) ? buffer.val : inString;
        Matcher m = plPattern.matcher(dereference(string, false));
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

        if (tokens[0].charAt(0) == '?') {
            stdprintln(SqltoolRB.pl_help.getString());

            return;
        }

        // If user runs any PL command, we turn PL mode on.
        plMode = true;

        if (tokens[0].equals("end")) {
            throw new BadSpecial(SqltoolRB.end_noblock.getString());
        }

        if (tokens[0].equals("continue")) {
            if (tokens.length > 1) {
                if (tokens.length == 2 &&
                        (tokens[1].equals("foreach") ||
                         tokens[1].equals("while"))) {
                    throw new ContinueException(tokens[1]);
                }
                throw new BadSpecial(SqltoolRB.continue_syntax.getString());
            }

            throw new ContinueException();
        }

        if (tokens[0].equals("break")) {
            if (tokens.length > 1) {
                if (tokens.length == 2 &&
                        (tokens[1].equals("foreach") ||
                         tokens[1].equals("if") ||
                         tokens[1].equals("while") ||
                         tokens[1].equals("file"))) {
                    throw new BreakException(tokens[1]);
                }
                throw new BadSpecial(SqltoolRB.break_syntax.getString());
            }

            throw new BreakException();
        }

        if (tokens[0].equals("list") || tokens[0].equals("listvalues")
                || tokens[0].equals("listsysprops")) {
            boolean sysProps =tokens[0].equals("listsysprops");
            String  s;
            boolean doValues = (tokens[0].equals("listvalues") || sysProps);
            // Always list System Property values.
            // They are unlikely to be very long, like PL variables may be.

            if (tokens.length == 1) {
                stdprint(formatNicely(
                        (sysProps ? System.getProperties() : shared.userVars),
                        doValues));
            } else {
                if (doValues) {
                    stdprintln(SqltoolRB.pl_list_parens.getString());
                } else {
                    stdprintln(SqltoolRB.pl_list_lengths.getString());
                }

                for (String token : tokens) {
                    s = (String) (sysProps ? System.getProperties()
                                           : shared.userVars).get(token);
                    if (s == null) continue;
                    stdprintln("    " + token + ": "
                               + (doValues ? ("(" + s + ')')
                                           : Integer.toString(s.length())));
                }
            }

            return;
        }

        if (tokens[0].equals("dump") || tokens[0].equals("load")) {
            if (tokens.length != 3) {
                throw new BadSpecial(SqltoolRB.dumpload_malformat.getString());
            }

            String varName = tokens[1];

            if (varName.indexOf(':') > -1) {
                throw new BadSpecial(SqltoolRB.plvar_nocolon.getString());
            }
            File   dlFile    = new File(dereferenceAt(tokens[2]));

            try {
                if (tokens[0].equals("dump")) {
                    dump(varName, dlFile);
                } else {
                    load(varName, dlFile, shared.encoding);
                }
            } catch (IOException ioe) {
                throw new BadSpecial(SqltoolRB.dumpload_fail.getString(
                        varName, dlFile.toString()), ioe);
            }

            return;
        }

        if (tokens[0].equals("prepare")) {
            if (tokens.length != 2) {
                throw new BadSpecial(SqltoolRB.prepare_malformat.getString());
            }

            if (shared.userVars.get(tokens[1]) == null) {
                throw new BadSpecial(
                        SqltoolRB.plvar_undefined.getString(tokens[1]));
            }

            prepareVar = tokens[1];
            doPrepare  = true;

            return;
        }

        m = varsetPattern.matcher(dereference(string, false));
        if (!m.matches()) {
            throw new BadSpecial(SqltoolRB.pl_unknown.getString(tokens[0]));
        }
        if (m.groupCount() < 2 || m.groupCount() > 3) {
            // Assertion
            throw new RuntimeException("varset pattern matched but captured "
                    + m.groupCount() + " groups");
        }

        String varName  = m.group(1);

        if (varName.indexOf(':') > -1) {
            throw new BadSpecial(SqltoolRB.plvar_nocolon.getString());
        }

        switch (m.group(2).charAt(0)) {
            case '_' :
                silentFetch = true;
            case '~' :
                if (m.groupCount() > 2 && m.group(3) != null) {
                    throw new BadSpecial(SqltoolRB.plvar_tildedash_nomoreargs.getString(
                            m.group(3)));
                }

                shared.userVars.remove(varName);
                updateUserSettings();

                fetchingVar = varName;

                return;

            case '=' :
                if (fetchingVar != null && fetchingVar.equals(varName)) {
                    fetchingVar = null;
                }

                if (varName.equals("*ENCODING")) try {
                    // Special case so we can proactively prohibit encodings
                    // which will not work, so we'll always be confident
                    // that 'encoding' value is always good.
                    setEncoding(m.group(3));
                    return;
                } catch (UnsupportedEncodingException use) {
                    throw new BadSpecial(
                            SqltoolRB.encode_fail.getString(m.group(3)));
                }
                if (m.groupCount() > 2 && m.group(3) != null) {
                    shared.userVars.put(varName, m.group(3));
                } else {
                    shared.userVars.remove(varName);
                }
                updateUserSettings();

                return;
        }

        throw new BadSpecial(SqltoolRB.pl_unknown.getString(tokens[0]));
        // I think this would already be caught in the setvar block above.
    }

    private void processMacro(Token defToken) throws BadSpecial {
        Matcher matcher;
        Token macroToken;

        if (defToken.val.length() < 1) {
            throw new BadSpecial(SqltoolRB.macro_tip.getString());
        }
        switch (defToken.val.charAt(0)) {
            case '?':
                stdprintln(SqltoolRB.macro_help.getString());
                break;
            case '=':
                String defString = defToken.val;
                defString = defString.substring(1).trim();
                if (defString.length() < 1) {
                    for (Map.Entry<String, Token> entry
                            : shared.macros.entrySet()) {
                        stdprintln(entry.getKey() + " = "
                                + entry.getValue().reconstitute());
                    }
                    break;
                }

                int newType = -1;
                StringBuffer newVal = new StringBuffer();
                matcher = editMacroPattern.matcher(defString);
                if (matcher.matches()) {
                    if (buffer == null) {
                        stdprintln(nobufferYetString);
                        return;
                    }
                    newVal.append(buffer.val);
                    if (matcher.groupCount() > 1 && matcher.group(2) != null
                            && matcher.group(2).length() > 0)
                        newVal.append(matcher.group(2));
                    newType = buffer.type;
                } else {
                    matcher = spMacroPattern.matcher(defString);
                    if (matcher.matches()) {
                        newVal.append(matcher.group(3));
                        newType = (matcher.group(2).equals("*")
                                ?  Token.PL_TYPE : Token.SPECIAL_TYPE);
                    } else {
                        matcher = sqlMacroPattern.matcher(defString);
                        if (!matcher.matches())
                            throw new BadSpecial(
                                    SqltoolRB.macro_malformat.getString());
                        newVal.append(matcher.group(2));
                        newType = Token.SQL_TYPE;
                    }
                }
                if (newVal.length() < 1)
                    throw new BadSpecial(SqltoolRB.macrodef_empty.getString());
                if (newVal.charAt(newVal.length() - 1) == ';')
                    throw new BadSpecial(SqltoolRB.macrodef_semi.getString());
                shared.macros.put(matcher.group(1),
                        new Token(newType, newVal, defToken.line));
                break;
            default:
                matcher = useMacroPattern.matcher(defToken.val);
                if (!matcher.matches())
                    throw new BadSpecial(SqltoolRB.macro_malformat.getString());
                macroToken = shared.macros.get(matcher.group(1));
                if (macroToken == null)
                    throw new BadSpecial(SqltoolRB.macro_undefined.getString(
                            matcher.group(1)));
                setBuf(macroToken);
                buffer.line = defToken.line;
                if (matcher.groupCount() > 1 && matcher.group(2) != null
                        && matcher.group(2).length() > 0)
                    buffer.val += matcher.group(2);
                preempt = matcher.group(matcher.groupCount()).equals(";");
        }
    }
}
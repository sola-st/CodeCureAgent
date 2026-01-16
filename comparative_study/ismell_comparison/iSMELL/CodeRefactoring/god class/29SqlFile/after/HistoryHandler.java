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

class HistoryHandler {
    private List<Token> history;
    private int maxHistoryLength;
    
    public void setMaxHistoryLength(int maxHistoryLength) {
        if (executing)
            throw new IllegalStateException(
                "Can't set maxHistoryLength after execute() has been called");
        if (reader == null)
            throw new IllegalStateException(
                "Can't set maxHistoryLength execute() has run");
        this.maxHistoryLength = maxHistoryLength;
    }

    private void processBuffHist(Token token)
     throws BadSpecial, SQLException, SqlToolError {
         if (token.val.length() < 1) {
             throw new BadSpecial(SqltoolRB.bufhist_unspecified.getString());
         }
 
         // First handle the simple cases where user may not specify a
         // command number.
         char commandChar = token.val.charAt(0);
         String other       = token.val.substring(1);
         if (other.trim().length() == 0) {
             other = null;
         }
         switch (commandChar) {
             case 'l' :
             case 'b' :
                 enforce1charBH(other, 'l');
                 if (buffer == null) {
                     stdprintln(nobufferYetString);
                 } else {
                     stdprintln(SqltoolRB.editbuffer_contents.getString(
                             buffer.reconstitute()));
                 }
 
                 return;
 
             case 'h' :
                 enforce1charBH(other, 'h');
                 showHistory();
 
                 return;
 
             case '?' :
                 stdprintln(SqltoolRB.buffer_help.getString());
 
                 return;
         }
 
         Integer histNum = null;
         Matcher hm = slashHistoryPattern.matcher(token.val);
         if (hm.matches()) {
             histNum = historySearch(hm.group(1));
             if (histNum == null) {
                 stdprintln(SqltoolRB.substitution_nomatch.getString());
                 return;
             }
         } else {
             hm = historyPattern.matcher(token.val);
             if (!hm.matches()) {
                 throw new BadSpecial(SqltoolRB.edit_malformat.getString());
                 // Empirically, I find that this pattern always captures two
                 // groups.  Unfortunately, there's no way to guarantee that :( .
             }
             histNum = ((hm.group(1) == null || hm.group(1).length() < 1)
                     ? null : Integer.valueOf(hm.group(1)));
         }
         if (hm.groupCount() != 2) {
             throw new BadSpecial(SqltoolRB.edit_malformat.getString());
             // Empirically, I find that this pattern always captures two
             // groups.  Unfortunately, there's no way to guarantee that :( .
         }
         commandChar = ((hm.group(2) == null || hm.group(2).length() < 1)
                 ? '\0' : hm.group(2).charAt(0));
         other = ((commandChar == '\0') ? null : hm.group(2).substring(1));
         if (other != null && other.length() < 1) other = null;
         Token targetCommand = ((histNum == null)
                 ? null : commandFromHistory(histNum.intValue()));
         // Every command below depends upon buffer content.
 
         switch (commandChar) {
             case '\0' :  // Special token set above.  Just history recall.
                 setBuf(targetCommand);
                 stdprintln(SqltoolRB.buffer_restored.getString(
                         buffer.reconstitute()));
                 return;
 
             case ';' :
                 enforce1charBH(other, ';');
 
                 if (targetCommand != null) setBuf(targetCommand);
                 if (buffer == null) throw new BadSpecial(
                         SqltoolRB.nobuffer_yet.getString());
                 stdprintln(SqltoolRB.buffer_executing.getString(
                         buffer.reconstitute()));
                 preempt = true;
                 return;
 
             case 'a' :
                 if (targetCommand == null) targetCommand = buffer;
                 if (targetCommand == null) throw new BadSpecial(
                         SqltoolRB.nobuffer_yet.getString());
                 boolean doExec = false;
 
                 if (other != null) {
                     if (other.trim().charAt(other.trim().length() - 1) == ';') {
                         other = other.substring(0, other.lastIndexOf(';'));
                         if (other.trim().length() < 1)
                             throw new BadSpecial(
                                     SqltoolRB.append_empty.getString());
                         doExec = true;
                     }
                 }
                 Token newToken = new Token(targetCommand.type,
                         targetCommand.val, targetCommand.line);
                 if (other != null) newToken.val += other;
                 setBuf(newToken);
                 if (doExec) {
                     stdprintln(SqltoolRB.buffer_executing.getString(
                             buffer.reconstitute()));
                     preempt = true;
                     return;
                 }
 
                 if (interactive) scanner.setMagicPrefix(
                         newToken.reconstitute());
 
                 switch (newToken.type) {
                     case Token.SQL_TYPE:
                         scanner.setRequestedState(SqlFileScanner.SQL);
                         break;
                     case Token.SPECIAL_TYPE:
                         scanner.setRequestedState(SqlFileScanner.SPECIAL);
                         break;
                     case Token.PL_TYPE:
                         scanner.setRequestedState(SqlFileScanner.PL);
                         break;
                     default:
                         throw new RuntimeException(
                             "Internal assertion failed.  "
                             + "Appending to unexpected type: "
                             + newToken.getTypeString());
                 }
                 scanner.setCommandBuffer(newToken.val);
 
                 return;
 
             case 'w' :
                 if (targetCommand == null) targetCommand = buffer;
                 if (targetCommand == null) throw new BadSpecial(
                         SqltoolRB.nobuffer_yet.getString());
                 if (other == null) {
                     throw new BadSpecial(SqltoolRB.destfile_demand.getString());
                 }
                 String targetFile =
                         dereferenceAt(dereference(other.trim(), false));
                 // Dereference and trim the target file name
                 // This is the only case where we dereference a : command.
 
                 PrintWriter pw = null;
                 try {
                     pw = new PrintWriter(
                             new OutputStreamWriter(
                             new FileOutputStream(targetFile, true),
                             (shared.encoding == null)
                             ? DEFAULT_FILE_ENCODING : shared.encoding)
                             // Appendmode so can append to an SQL script.
                     );
 
                     pw.println(targetCommand.reconstitute(true));
                     pw.flush();
                 } catch (Exception e) {
                     throw new BadSpecial(SqltoolRB.file_appendfail.getString(
                             targetFile), e);
                 } finally {
                     if (pw != null) {
                         try {
                             pw.close();
                         } finally {
                             pw = null; // Encourage GC of buffers
                         }
                     }
                 }
 
                 return;
 
             case 's' :
                 boolean modeExecute = false;
                 boolean modeGlobal = false;
                 if (targetCommand == null) targetCommand = buffer;
                 if (targetCommand == null) throw new BadSpecial(
                         SqltoolRB.nobuffer_yet.getString());
 
                 try {
                     if (other == null || other.length() < 3) {
                         throw new BadSubst(
                                 SqltoolRB.substitution_malformat.getString());
                     }
                     Matcher m = substitutionPattern.matcher(other);
                     if (!m.matches()) {
                         throw new BadSubst(
                                 SqltoolRB.substitution_malformat.getString());
                     }
 
                     // Note that this pattern does not include the leading :.
                     if (m.groupCount() < 3 || m.groupCount() > 4) {
                         throw new RuntimeException(
                                 "Internal assertion failed.  "
                                 + "Matched substitution "
                                 + "pattern, but captured "
                                 + m.groupCount() + " groups");
                     }
                     String optionGroup = (
                             (m.groupCount() > 3 && m.group(4) != null)
                             ? (new String(m.group(4))) : null);
 
                     if (optionGroup != null) {
                         if (optionGroup.indexOf(';') > -1) {
                             modeExecute = true;
                             optionGroup = optionGroup.replaceFirst(";", "");
                         }
                         if (optionGroup.indexOf('g') > -1) {
                             modeGlobal = true;
                             optionGroup = optionGroup.replaceFirst("g", "");
                         }
                     }
 
                     Matcher bufferMatcher = Pattern.compile("(?s"
                             + ((optionGroup == null) ? "" : optionGroup)
                             + ')' + m.group(2)).matcher(targetCommand.val);
                     Token newBuffer = new Token(targetCommand.type,
                             (modeGlobal
                                 ? bufferMatcher.replaceAll(m.group(3))
                                 : bufferMatcher.replaceFirst(m.group(3))),
                                 targetCommand.line);
                     if (newBuffer.val.equals(targetCommand.val)) {
                         stdprintln(SqltoolRB.substitution_nomatch.getString());
                         return;
                     }
 
                     setBuf(newBuffer);
                     stdprintln(modeExecute
                             ? SqltoolRB.buffer_executing.getString(
                                     buffer.reconstitute())
                             : SqltoolRB.editbuffer_contents.getString(
                                     buffer.reconstitute())
                     );
                 } catch (PatternSyntaxException pse) {
                     throw new BadSpecial(
                             SqltoolRB.substitution_syntax.getString(), pse);
                 } catch (BadSubst badswitch) {
                     throw new BadSpecial(
                             SqltoolRB.substitution_syntax.getString());
                 }
                 if (modeExecute) preempt = true;
 
                 return;
         }
 
         throw new BadSpecial(SqltoolRB.buffer_unknown.getString(
                 Character.toString(commandChar)));
    }

    private void showHistory() throws BadSpecial {
        if (history == null) {
            throw new BadSpecial(SqltoolRB.history_unavailable.getString());
        }
        if (history.size() < 1) {
            throw new BadSpecial(SqltoolRB.history_none.getString());
        }
        if (shared.psStd == null) return;
          // Input can be dual-purpose, i.e. the script can be intended for
          // both interactive and non-interactive usage.
        Token token;
        for (int i = 0; i < history.size(); i++) {
            token = history.get(i);
            shared.psStd.println("#" + (i + oldestHist) + " or "
                    + (i - history.size()) + ':');
            shared.psStd.println(token.reconstitute());
        }
        if (buffer != null) {
            shared.psStd.println(SqltoolRB.editbuffer_contents.getString(
                    buffer.reconstitute()));
        }

        shared.psStd.println();
        shared.psStd.println(SqltoolRB.buffer_instructions.getString());
    }

    private Token commandFromHistory(int inIndex) throws BadSpecial {
        int index = inIndex;  // Just to quiet compiler warnings.

        if (history == null) {
            throw new BadSpecial(SqltoolRB.history_unavailable.getString());
        }
        if (index == 0) {
            throw new BadSpecial(SqltoolRB.history_number_req.getString());
        }
        if (index > 0) {
            // Positive command# given
            index -= oldestHist;
            if (index < 0) {
                throw new BadSpecial(
                        SqltoolRB.history_backto.getString(oldestHist));
            }
            if (index >= history.size()) {
                throw new BadSpecial(SqltoolRB.history_upto.getString(
                       history.size() + oldestHist - 1));
            }
        } else {
            // Negative command# given
            index += history.size();
            if (index < 0) {
                throw new BadSpecial(
                        SqltoolRB.history_back.getString(history.size()));
            }
        }
        return history.get(index);
    }

    private Integer historySearch(String findRegex) throws BadSpecial {
        if (history == null) {
            throw new BadSpecial(SqltoolRB.history_unavailable.getString());
        }
        Pattern pattern = null;
        try {
            pattern = Pattern.compile("(?ims)" + findRegex);
        } catch (PatternSyntaxException pse) {
            throw new BadSpecial(
                    SqltoolRB.regex_malformat.getString(pse.getMessage()));
        }
        // Make matching more liberal.  Users can customize search behavior
        // by using "(?-OPTIONS)" or (?OPTIONS) in their regexes.
        for (int index = history.size() - 1; index >= 0; index--)
            if (pattern.matcher((history.get(index)).val).find())
                return Integer.valueOf(index + oldestHist);
        return null;
    }

    private boolean historize() {
        if (history == null || buffer == null) {
            return false;
        }
        if (history.size() > 0 &&
                history.get(history.size() - 1).equals(buffer)) {
            // Don't store two consecutive commands that are exactly the same.
            return false;
        }
        history.add(buffer);
        if (history.size() <= maxHistoryLength) {
            return true;
        }
        history.remove(0);
        oldestHist++;
        return true;
    }
}
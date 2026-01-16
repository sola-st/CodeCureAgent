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
import java.util.Scanner;
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

class InputHandler {
    private Scanner scanner;
    private String inputFilePath;
    private boolean interactive;

    public void setAutoClose(boolean autoClose) {
         this.autoClose = autoClose;
    }

    public void closeReader() {
        if (reader == null) {
            return;
        }
        try {
            if (scanner != null) try {
                scanner.yyclose();
            } catch (IOException ioe) {
                errprintln("Failed to close pipes");
            }
            try {
                reader.close();
            } catch (IOException ioe) {
                // Purposefully empty.
                // The reader will usually already be closed at this point.
            }
        } finally {
            reader = null; // Encourage GC of buffers
        }
    }

    private TokenList seekTokenSource(String nestingCommand)
             throws BadSpecial, IOException {
         Token token;
         TokenList newTS = new TokenList();
         Pattern endPattern = Pattern.compile("end\\s+" + nestingCommand);
         String subNestingCommand;
 
         while ((token = scanner.yylex()) != null) {
             if (token.type == Token.PL_TYPE
                     && endPattern.matcher(token.val).matches()) {
                 return newTS;
             }
             subNestingCommand = nestingCommand(token);
             if (subNestingCommand != null) {
                 token.nestedBlock = seekTokenSource(subNestingCommand);
             }
             newTS.add(token);
         }
         throw new BadSpecial(
                 SqltoolRB.pl_block_unterminated.getString(nestingCommand));
    }
}
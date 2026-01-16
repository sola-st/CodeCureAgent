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

class ConnectionHandler {
    private Connection connection;
    private boolean possiblyUncommitteds;

    public void setConnection(Connection jdbcConn) {
        if (jdbcConn == null)
            throw new IllegalArgumentException(
                    "We don't yet support unsetting the JDBC Connection");
        shared.jdbcConn = jdbcConn;
    }
 
    public Connection getConnection() {
        return shared.jdbcConn;
    }

    private void requireConnection() throws SqlToolError {
        if (shared.jdbcConn == null)
            throw new SqlToolError(SqltoolRB.no_required_conn.getString());
    }

    public static String getBanner(Connection c) {
        try {
            DatabaseMetaData md = c.getMetaData();
            return (md == null)
                    ? null
                    : SqltoolRB.jdbc_established.getString(
                            md.getDatabaseProductName(),
                            md.getDatabaseProductVersion(),
                            md.getUserName(),
                                    (c.isReadOnly() ? "R/O " : "R/W ")
                                    + RCData.tiToString(
                                    c.getTransactionIsolation()));
        } catch (SQLException se) {
            return null;
        }
    }

    private void displayConnBanner() {
        String msg = (shared.jdbcConn == null)
                   ? SqltoolRB.disconnected_msg.getString()
                   : SqlFile.getBanner(shared.jdbcConn);
        stdprintln((msg == null)
                  ? SqltoolRB.connected_fallbackmsg.getString()
                  : msg);
    }
}
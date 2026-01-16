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

class FileHandler {
    private String encoding;
    
    private void setEncoding(String newEncoding)
             throws UnsupportedEncodingException {
         if (newEncoding == null) {
             shared.encoding = null;
             shared.userVars.remove("ENCODING");
             return;
         }
         if (!Charset.isSupported(newEncoding))
             throw new UnsupportedEncodingException(newEncoding);
         shared.userVars.put("*ENCODING", newEncoding);
         shared.encoding = newEncoding;
    }

    private void dump(String varName,
                       File dumpFile) throws IOException, BadSpecial {
         String val = shared.userVars.get(varName);
 
         if (val == null) {
             throw new BadSpecial(SqltoolRB.plvar_undefined.getString(varName));
         }
 
         OutputStreamWriter osw = new OutputStreamWriter(
                 new FileOutputStream(dumpFile), (shared.encoding == null)
                 ? DEFAULT_FILE_ENCODING : shared.encoding);
 
         try {
             osw.write(val);
 
             if (val.length() > 0) {
                 char lastChar = val.charAt(val.length() - 1);
 
                 if (lastChar != '\n' && lastChar != '\r') {
                     osw.write(LS);
                 }
             }
 
             osw.flush();
         } finally {
             try {
                 osw.close();
             } catch (IOException ioe) {
                 // Intentionally empty
             } finally {
                 osw = null;  // Encourage GC of buffers
             }
         }
 
         // Since opened in overwrite mode, since we didn't exception out,
         // we can be confident that we wrote all the bytest in the file.
         stdprintln(SqltoolRB.file_wrotechars.getString(
                 Long.toString(dumpFile.length()), dumpFile.toString()));
    }

    private void dump(File dumpFile) throws IOException, BadSpecial {
        if (binBuffer == null) {
            throw new BadSpecial(SqltoolRB.binbuffer_empty.getString());
        }

        int len = 0;
        FileOutputStream fos = new FileOutputStream(dumpFile);

        try {
            fos.write(binBuffer);

            len = binBuffer.length;

            binBuffer = null;

            fos.flush();
        } finally {
            try {
                fos.close();
            } catch (IOException ioe) {
                // Intentionally empty
            } finally {
                fos = null; // Encourage GC of buffers
            }
        }
        stdprintln(SqltoolRB.file_wrotechars.getString(
                len, dumpFile.toString()));
    }

    public String streamToString(InputStream isIn, String cs)
             throws IOException {
         InputStream is = isIn;  // Compiler warning, when we can null the ref
         byte[] ba = null;
         int bytesread = 0;
         int retval;
         try {
             try {
                 ba = new byte[is.available()];
             } catch (RuntimeException re) {
                 throw new IOException(SqltoolRB.read_toobig.getString());
             }
             while (bytesread < ba.length &&
                     (retval = is.read(
                             ba, bytesread, ba.length - bytesread)) > 0) {
                 bytesread += retval;
             }
             if (bytesread != ba.length) {
                 throw new IOException(
                         SqltoolRB.read_partial.getString(bytesread, ba.length));
             }
             try {
                 return (cs == null) ? (new String(ba))
                                          : (new String(ba, cs));
             } catch (UnsupportedEncodingException uee) {
                 throw new IOException(
                         SqltoolRB.encode_fail.getString(uee.getMessage()));
             } catch (RuntimeException re) {
                 throw new IOException(SqltoolRB.read_convertfail.getString());
             }
         } finally {
             try {
                 is.close();
             } catch (IOException ioe) {
                 // intentionally empty
             } finally {
                 is = null;  // Encourage GC of buffers
             }
         }
    }

    private void load(String varName, File asciiFile, String cs)
             throws IOException {
         String string = streamToString(new FileInputStream(asciiFile), cs);
         // The streamToString() method ensures that the Stream gets closed
         shared.userVars.put(varName, string);
         updateUserSettings();
    }

    public static byte[] streamToBytes(InputStream is) throws IOException {
        byte[]                xferBuffer = new byte[10240];
        byte[]                outBytes = null;
        int                   i;
        ByteArrayOutputStream baos       = new ByteArrayOutputStream();

        try {
            while ((i = is.read(xferBuffer)) > 0) {
                baos.write(xferBuffer, 0, i);
            }
            outBytes = baos.toByteArray();
        } finally {
            baos = null;  // Encourage buffer GC
        }
        return outBytes;
    }

    public static byte[] loadBinary(File binFile) throws IOException {
        byte[]                xferBuffer = new byte[10240];
        byte[]                outBytes = null;
        ByteArrayOutputStream baos;
        int                   i;
        FileInputStream       fis        = new FileInputStream(binFile);

        try {
            baos = new ByteArrayOutputStream();
            while ((i = fis.read(xferBuffer)) > 0) {
                baos.write(xferBuffer, 0, i);
            }
            outBytes = baos.toByteArray();
        } finally {
            try {
                fis.close();
            } catch (IOException ioe) {
                // intentionally empty
            } finally {
                fis = null; // Encourage GC of buffers
                baos = null; // Encourage GC of buffers
            }
        }

        return outBytes;
    }

    private String dereferenceAt(String s) throws BadSpecial {
        if (s.indexOf('@') != 0) return s;
        if (baseDir == null)
            throw new BadSpecial(
                    "Leading @ in file paths has special meaning, and may "
                    + " only be used if input is a file.");
        return baseDir.getPath() + s.substring(1);
    }
}
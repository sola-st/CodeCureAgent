package org.apache.tools.ant.taskdefs.optional.net;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.apache.tools.ant.util.FileUtils;

class FTPDirectoryOperations {
    private FTPClient ftpClient;
    private boolean verbose;

    protected void listFile(FTPClient ftp, BufferedWriter bw, String filename)
         throws IOException, BuildException {
        if (verbose) {
            log("listing " + filename);
        }

        FTPFile ftpfile = ftp.listFiles(resolveFile(filename))[0];

        bw.write(ftpfile.toString());
        bw.newLine();

        transferred++;
    }

    protected void makeRemoteDir(FTPClient ftp, String dir)
         throws IOException, BuildException {
        String workingDirectory = ftp.printWorkingDirectory();
        if (verbose) {
            log("Creating directory: " + dir);
        }
        if (dir.indexOf("/") == 0) {
            ftp.changeWorkingDirectory("/");
        }
        String subdir = new String();
        StringTokenizer st = new StringTokenizer(dir, "/");
        while (st.hasMoreTokens()) {
            subdir = st.nextToken();
            log("Checking " + subdir, Project.MSG_DEBUG);
            if (!ftp.changeWorkingDirectory(subdir)) {
                if (!ftp.makeDirectory(subdir)) {
                    // codes 521, 550 and 553 can be produced by FTP Servers
                    //  to indicate that an attempt to create a directory has
                    //  failed because the directory already exists.
                    int rc = ftp.getReplyCode();
                    if (!(ignoreNoncriticalErrors
                        && (rc == FTPReply.CODE_550 || rc == FTPReply.CODE_553
                        || rc == CODE_521))) {
                        throw new BuildException("could not create directory: "
                            + ftp.getReplyString());
                    }
                    if (verbose) {
                        log("Directory already exists");
                    }
                } else {
                    if (verbose) {
                        log("Directory created OK");
                    }
                    ftp.changeWorkingDirectory(subdir);
                }
            }
        }
        if (workingDirectory != null) {
            ftp.changeWorkingDirectory(workingDirectory);
        }
    }

    protected void rmDir(FTPClient ftp, String dirname)
         throws IOException, BuildException {
        if (verbose) {
            log("removing " + dirname);
        }

        if (!ftp.removeDirectory(resolveFile(dirname))) {
            String s = "could not remove directory: " + ftp.getReplyString();

            if (skipFailedTransfers) {
                log(s, Project.MSG_WARN);
                skipped++;
            } else {
                throw new BuildException(s);
            }
        } else {
            log("Directory " + dirname + " removed from " + server,
                Project.MSG_VERBOSE);
            transferred++;
        }
    }

    protected void createParents(FTPClient ftp, String filename)
         throws IOException, BuildException {

        File dir = new File(filename);
        if (dirCache.contains(dir)) {
            return;
        }


        Vector parents = new Vector();
        String dirname;

        while ((dirname = dir.getParent()) != null) {
            File checkDir = new File(dirname);
            if (dirCache.contains(checkDir)) {
                break;
            }
            dir = checkDir;
            parents.addElement(dir);
        }

        // find first non cached dir
        int i = parents.size() - 1;

        if (i >= 0) {
            String cwd = ftp.printWorkingDirectory();
            String parent = dir.getParent();
            if (parent != null) {
                if (!ftp.changeWorkingDirectory(resolveFile(parent))) {
                    throw new BuildException("could not change to "
                        + "directory: " + ftp.getReplyString());
                }
            }

            while (i >= 0) {
                dir = (File) parents.elementAt(i--);
                // check if dir exists by trying to change into it.
                if (!ftp.changeWorkingDirectory(dir.getName())) {
                    // could not change to it - try to create it
                    log("creating remote directory "
                        + resolveFile(dir.getPath()), Project.MSG_VERBOSE);
                    if (!ftp.makeDirectory(dir.getName())) {
                        handleMkDirFailure(ftp);
                    }
                    if (!ftp.changeWorkingDirectory(dir.getName())) {
                        throw new BuildException("could not change to "
                            + "directory: " + ftp.getReplyString());
                    }
                }
                dirCache.addElement(dir);
            }
            ftp.changeWorkingDirectory(cwd);
        }
    }
}
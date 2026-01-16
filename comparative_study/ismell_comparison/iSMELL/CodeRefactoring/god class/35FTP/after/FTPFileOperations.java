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

class FTPFileOperations {
    private FTPClient ftpClient;
    private boolean verbose;
    private boolean newerOnly;

    protected void sendFile(FTPClient ftp, String dir, String filename)
         throws IOException, BuildException {
        InputStream instream = null;

        try {
            // XXX - why not simply new File(dir, filename)?
            File file = getProject().resolveFile(new File(dir, filename).getPath());

            if (newerOnly && isUpToDate(ftp, file, resolveFile(filename))) {
                return;
            }

            if (verbose) {
                log("transferring " + file.getAbsolutePath());
            }

            instream = new BufferedInputStream(new FileInputStream(file));

            createParents(ftp, filename);

            ftp.storeFile(resolveFile(filename), instream);

            boolean success = FTPReply.isPositiveCompletion(ftp.getReplyCode());

            if (!success) {
                String s = "could not put file: " + ftp.getReplyString();

                if (skipFailedTransfers) {
                    log(s, Project.MSG_WARN);
                    skipped++;
                } else {
                    throw new BuildException(s);
                }

            } else {
                // see if we should issue a chmod command
                if (chmod != null) {
                    doSiteCommand(ftp, "chmod " + chmod + " " + resolveFile(filename));
                }
                log("File " + file.getAbsolutePath() + " copied to " + server,
                    Project.MSG_VERBOSE);
                transferred++;
            }
        } finally {
            if (instream != null) {
                try {
                    instream.close();
                } catch (IOException ex) {
                    // ignore it
                }
            }
        }
    }

    protected void getFile(FTPClient ftp, String dir, String filename)
         throws IOException, BuildException {
        OutputStream outstream = null;

        try {
            File file = getProject().resolveFile(new File(dir, filename).getPath());

            if (newerOnly && isUpToDate(ftp, file, resolveFile(filename))) {
                return;
            }

            if (verbose) {
                log("transferring " + filename + " to "
                     + file.getAbsolutePath());
            }

            File pdir = fileUtils.getParentFile(file);

            if (!pdir.exists()) {
                pdir.mkdirs();
            }
            outstream = new BufferedOutputStream(new FileOutputStream(file));
            ftp.retrieveFile(resolveFile(filename), outstream);

            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                String s = "could not get file: " + ftp.getReplyString();

                if (skipFailedTransfers) {
                    log(s, Project.MSG_WARN);
                    skipped++;
                } else {
                    throw new BuildException(s);
                }

            } else {
                log("File " + file.getAbsolutePath() + " copied from "
                     + server, Project.MSG_VERBOSE);
                transferred++;
                if (preserveLastModified) {
                    outstream.close();
                    outstream = null;
                    FTPFile[] remote = ftp.listFiles(resolveFile(filename));
                    if (remote.length > 0) {
                        fileUtils.setFileLastModified(file,
                                                      remote[0].getTimestamp()
                                                      .getTime().getTime());
                    }
                }
            }
        } finally {
            if (outstream != null) {
                try {
                    outstream.close();
                } catch (IOException ex) {
                    // ignore it
                }
            }
        }
    }

    protected void delFile(FTPClient ftp, String filename)
         throws IOException, BuildException {
        if (verbose) {
            log("deleting " + filename);
        }

        if (!ftp.deleteFile(resolveFile(filename))) {
            String s = "could not delete file: " + ftp.getReplyString();

            if (skipFailedTransfers) {
                log(s, Project.MSG_WARN);
                skipped++;
            } else {
                throw new BuildException(s);
            }
        } else {
            log("File " + filename + " deleted from " + server,
                Project.MSG_VERBOSE);
            transferred++;
        }
    }

    protected boolean isUpToDate(FTPClient ftp, File localFile,
                                 String remoteFile)
         throws IOException, BuildException {
        log("checking date for " + remoteFile, Project.MSG_VERBOSE);

        FTPFile[] files = ftp.listFiles(remoteFile);

        // For Microsoft's Ftp-Service an Array with length 0 is
        // returned if configured to return listings in "MS-DOS"-Format
        if (files == null || files.length == 0) {
            // If we are sending files, then assume out of date.
            // If we are getting files, then throw an error

            if (action == SEND_FILES) {
                log("Could not date test remote file: " + remoteFile
                     + "assuming out of date.", Project.MSG_VERBOSE);
                return false;
            } else {
                throw new BuildException("could not date test remote file: "
                    + ftp.getReplyString());
            }
        }

        long remoteTimestamp = files[0].getTimestamp().getTime().getTime();
        long localTimestamp = localFile.lastModified();

        if (this.action == SEND_FILES) {
            return remoteTimestamp + timeDiffMillis > localTimestamp;
        } else {
            return localTimestamp > remoteTimestamp + timeDiffMillis;
        }
    }

    protected String resolveFile(String file) {
        return file.replace(System.getProperty("file.separator").charAt(0),
            remoteFileSep.charAt(0));
    }
}
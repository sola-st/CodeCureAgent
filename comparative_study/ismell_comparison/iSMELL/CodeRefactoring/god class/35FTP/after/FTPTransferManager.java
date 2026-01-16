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

class FTPTransferManager {
    private FTPConnectionDetails connectionDetails;
    private FTPFileOperations fileOperations;
    private FTPDirectoryOperations directoryOperations;

    protected void transferFiles(FTPClient ftp)
         throws IOException, BuildException {
        transferred = 0;
        skipped = 0;

        if (filesets.size() == 0) {
            throw new BuildException("at least one fileset must be specified.");
        } else {
            // get files from filesets
            for (int i = 0; i < filesets.size(); i++) {
                FileSet fs = (FileSet) filesets.elementAt(i);

                if (fs != null) {
                    transferFiles(ftp, fs);
                }
            }
        }

        log(transferred + " " + ACTION_TARGET_STRS[action] + " "
            + COMPLETED_ACTION_STRS[action]);
        if (skipped != 0) {
            log(skipped + " " + ACTION_TARGET_STRS[action]
                + " were not successfully " + COMPLETED_ACTION_STRS[action]);
        }
    }

    protected int transferFiles(FTPClient ftp, FileSet fs)
         throws IOException, BuildException {
        DirectoryScanner ds;

        if (action == SEND_FILES) {
            ds = fs.getDirectoryScanner(getProject());
        } else {
            // warn that selectors are not supported
            if (fs.getSelectors(getProject()).length != 0) {
                getProject().log("selectors are not supported in remote filesets",
                    Project.MSG_WARN);
            }
            ds = new FTPDirectoryScanner(ftp);
            fs.setupDirectoryScanner(ds, getProject());
            ds.setFollowSymlinks(fs.isFollowSymlinks());
            ds.scan();
        }

        String[] dsfiles = null;
        if (action == RM_DIR) {
            dsfiles = ds.getIncludedDirectories();
        } else {
            dsfiles = ds.getIncludedFiles();
        }
        String dir = null;

        if ((ds.getBasedir() == null)
             && ((action == SEND_FILES) || (action == GET_FILES))) {
            throw new BuildException("the dir attribute must be set for send "
                 + "and get actions");
        } else {
            if ((action == SEND_FILES) || (action == GET_FILES)) {
                dir = ds.getBasedir().getAbsolutePath();
            }
        }

        // If we are doing a listing, we need the output stream created now.
        BufferedWriter bw = null;

        try {
            if (action == LIST_FILES) {
                File pd = fileUtils.getParentFile(listing);

                if (!pd.exists()) {
                    pd.mkdirs();
                }
                bw = new BufferedWriter(new FileWriter(listing));
            }
            if (action == RM_DIR) {
                // to remove directories, start by the end of the list
                // the trunk does not let itself be removed before the leaves
                for (int i = dsfiles.length - 1; i >= 0; i--) {
                    rmDir(ftp, dsfiles[i]);
                }
            }   else {
                for (int i = 0; i < dsfiles.length; i++) {
                    switch (action) {
                        case SEND_FILES:
                            sendFile(ftp, dir, dsfiles[i]);
                            break;
                        case GET_FILES:
                            getFile(ftp, dir, dsfiles[i]);
                            break;
                        case DEL_FILES:
                            delFile(ftp, dsfiles[i]);
                            break;
                        case LIST_FILES:
                            listFile(ftp, bw, dsfiles[i]);
                            break;
                        case CHMOD:
                            doSiteCommand(ftp, "chmod " + chmod + " " + resolveFile(dsfiles[i]));
                            transferred++;
                            break;
                        default:
                            throw new BuildException("unknown ftp action " + action);
                    }
                }
            }
        } finally {
            if (bw != null) {
                bw.close();
            }
        }

        return dsfiles.length;
    }
}
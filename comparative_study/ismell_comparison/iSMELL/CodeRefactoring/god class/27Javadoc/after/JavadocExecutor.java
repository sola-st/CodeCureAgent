package org.apache.tools.ant.taskdefs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.tools.ant.taskdefs.Javadoc.PackageAndSourceFiles.*;
import org.apache.tools.ant.taskdefs.Javadoc.DocletParam;
import org.apache.tools.ant.taskdefs.Javadoc.ExtensionInfo;
import org.apache.tools.ant.taskdefs.Javadoc.GroupArgument;
import org.apache.tools.ant.taskdefs.Javadoc.LinkArgument;
import org.apache.tools.ant.taskdefs.Javadoc.SourceFile;
import org.apache.tools.ant.taskdefs.Javadoc.TagArgument;

class JavadocExecutor {
    private Project project;
    private Commandline toExecute;

    public JavadocExecutor(Project project) {
        this.project = project;
        this.toExecute = new Commandline();
    }

    public void execute() throws BuildException {
        checkTaskName();

        Vector packagesToDoc = new Vector();
        Path sourceDirs = new Path(getProject());

        checkPackageAndSourcePath();

        if (sourcePath != null) {
            sourceDirs.addExisting(sourcePath);
        }

        parsePackages(packagesToDoc, sourceDirs);
        checkPackages(packagesToDoc, sourceDirs);

        Vector sourceFilesToDoc = (Vector) sourceFiles.clone();
        addSourceFiles(sourceFilesToDoc);

        checkPackagesToDoc(packagesToDoc, sourceFilesToDoc);

        log("Generating Javadoc", Project.MSG_INFO);

        Commandline toExecute = (Commandline) cmd.clone();
        if (executable != null) {
            toExecute.setExecutable(executable);
        } else {
            toExecute.setExecutable(JavaEnvUtils.getJdkExecutable("javadoc"));
        }

        //  Javadoc arguments
        generalJavadocArguments(toExecute);  // general Javadoc arguments
        doSourcePath(toExecute, sourceDirs); // sourcepath
        doDoclet(toExecute);   // arguments for default doclet
        doBootPath(toExecute); // bootpath
        doLinks(toExecute);    // links arguments
        doGroup(toExecute);    // group attribute
        doGroups(toExecute);  // groups attribute
        doDocFilesSubDirs(toExecute); // docfilessubdir attribute

        doJava14(toExecute);
        if (breakiterator && (doclet == null || JAVADOC_5)) {
            toExecute.createArgument().setValue("-breakiterator");
        }
        // If using an external file, write the command line options to it
        if (useExternalFile) {
            writeExternalArgs(toExecute);
        }

        File tmpList = null;
        BufferedWriter srcListWriter = null;

        try {
            /**
             * Write sourcefiles and package names to a temporary file
             * if requested.
             */
            if (useExternalFile) {
                tmpList = FILE_UTILS.createTempFile("javadoc", "", null, true, true);
                toExecute.createArgument()
                    .setValue("@" + tmpList.getAbsolutePath());
                srcListWriter = new BufferedWriter(
                    new FileWriter(tmpList.getAbsolutePath(),
                                   true));
            }

            doSourceAndPackageNames(
                toExecute, packagesToDoc, sourceFilesToDoc,
                useExternalFile, tmpList, srcListWriter);
        } catch (IOException e) {
            tmpList.delete();
            throw new BuildException("Error creating temporary file",
                                     e, getLocation());
        } finally {
            FileUtils.close(srcListWriter);
        }

        if (packageList != null) {
            toExecute.createArgument().setValue("@" + packageList);
        }
        log(toExecute.describeCommand(), Project.MSG_VERBOSE);

        log("Javadoc execution", Project.MSG_INFO);

        JavadocOutputStream out = new JavadocOutputStream(Project.MSG_INFO);
        JavadocOutputStream err = new JavadocOutputStream(Project.MSG_WARN);
        Execute exe = new Execute(new PumpStreamHandler(out, err));
        exe.setAntRun(getProject());

        /*
         * No reason to change the working directory as all filenames and
         * path components have been resolved already.
         *
         * Avoid problems with command line length in some environments.
         */
        exe.setWorkingDirectory(null);
        try {
            exe.setCommandline(toExecute.getCommandline());
            int ret = exe.execute();
            if (ret != 0 && failOnError) {
                throw new BuildException("Javadoc returned " + ret,
                                         getLocation());
            }
        } catch (IOException e) {
            throw new BuildException("Javadoc failed: " + e, e, getLocation());
        } finally {
            if (tmpList != null) {
                tmpList.delete();
                tmpList = null;
            }

            out.logFlush();
            err.logFlush();
            try {
                out.close();
                err.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private void doSourcePath(Commandline toExecute, Path sourceDirs) {
        if (sourceDirs.size() > 0) {
            toExecute.createArgument().setValue("-sourcepath");
            toExecute.createArgument().setPath(sourceDirs);
        }
    }

    private void doDoclet(Commandline toExecute) {
        if (doclet != null) {
            if (doclet.getName() == null) {
                throw new BuildException("The doclet name must be "
                                         + "specified.", getLocation());
            } else {
                toExecute.createArgument().setValue("-doclet");
                toExecute.createArgument().setValue(doclet.getName());
                if (doclet.getPath() != null) {
                    Path docletPath
                        = doclet.getPath().concatSystemClasspath("ignore");
                    if (docletPath.size() != 0) {
                        toExecute.createArgument().setValue("-docletpath");
                        toExecute.createArgument().setPath(docletPath);
                    }
                }
                for (Enumeration e = doclet.getParams();
                     e.hasMoreElements();) {
                    DocletParam param = (DocletParam) e.nextElement();
                    if (param.getName() == null) {
                        throw new BuildException("Doclet parameters must "
                                                 + "have a name");
                    }

                    toExecute.createArgument().setValue(param.getName());
                    if (param.getValue() != null) {
                        toExecute.createArgument()
                            .setValue(param.getValue());
                    }
                }
            }
        }
    }

    private void doBootPath(Commandline toExecute) {
        Path bcp = new Path(getProject());
        if (bootclasspath != null) {
            bcp.append(bootclasspath);
        }
        bcp = bcp.concatSystemBootClasspath("ignore");
        if (bcp.size() > 0) {
            toExecute.createArgument().setValue("-bootclasspath");
            toExecute.createArgument().setPath(bcp);
        }
    }

    private void doLinks(Commandline toExecute) {
        if (links.size() != 0) {
            for (Enumeration e = links.elements(); e.hasMoreElements();) {
                LinkArgument la = (LinkArgument) e.nextElement();

                if (la.getHref() == null || la.getHref().length() == 0) {
                    log("No href was given for the link - skipping",
                        Project.MSG_VERBOSE);
                    continue;
                }
                String link = null;
                if (la.shouldResolveLink()) {
                    File hrefAsFile =
                        getProject().resolveFile(la.getHref());
                    if (hrefAsFile.exists()) {
                        try {
                            link = FILE_UTILS.getFileURL(hrefAsFile)
                                .toExternalForm();
                        } catch (MalformedURLException ex) {
                            // should be impossible
                            log("Warning: link location was invalid "
                                + hrefAsFile, Project.MSG_WARN);
                        }
                    }
                }
                if (link == null) {
                    // is the href a valid URL
                    try {
                        URL base = new URL("file://.");
                        new URL(base, la.getHref());
                        link = la.getHref();
                    } catch (MalformedURLException mue) {
                        // ok - just skip
                        log("Link href \"" + la.getHref()
                            + "\" is not a valid url - skipping link",
                            Project.MSG_WARN);
                        continue;
                    }
                }

                if (la.isLinkOffline()) {
                    File packageListLocation = la.getPackagelistLoc();
                    URL packageListURL = la.getPackagelistURL();
                    if (packageListLocation == null
                        && packageListURL == null) {
                        throw new BuildException("The package list"
                                                 + " location for link "
                                                 + la.getHref()
                                                 + " must be provided "
                                                 + "because the link is "
                                                 + "offline");
                    }
                    if (packageListLocation != null) {
                        File packageListFile =
                            new File(packageListLocation, "package-list");
                        if (packageListFile.exists()) {
                            try {
                                packageListURL =
                                    FILE_UTILS.getFileURL(packageListLocation);
                            } catch (MalformedURLException ex) {
                                log("Warning: Package list location was "
                                    + "invalid " + packageListLocation,
                                    Project.MSG_WARN);
                            }
                        } else {
                            log("Warning: No package list was found at "
                                + packageListLocation, Project.MSG_VERBOSE);
                        }
                    }
                    if (packageListURL != null) {
                        toExecute.createArgument().setValue("-linkoffline");
                        toExecute.createArgument().setValue(link);
                        toExecute.createArgument()
                            .setValue(packageListURL.toExternalForm());
                    }
                } else {
                    toExecute.createArgument().setValue("-link");
                    toExecute.createArgument().setValue(link);
                }
            }
        }
    }

    private void doGroup(Commandline toExecute) {
        // add the single group arguments
        // Javadoc 1.2 rules:
        //   Multiple -group args allowed.
        //   Each arg includes 3 strings: -group [name] [packagelist].
        //   Elements in [packagelist] are colon-delimited.
        //   An element in [packagelist] may end with the * wildcard.

        // Ant javadoc task rules for group attribute:
        //   Args are comma-delimited.
        //   Each arg is 2 space-delimited strings.
        //   E.g., group="XSLT_Packages org.apache.xalan.xslt*,
        //                XPath_Packages org.apache.xalan.xpath*"
        if (group != null) {
            StringTokenizer tok = new StringTokenizer(group, ",", false);
            while (tok.hasMoreTokens()) {
                String grp = tok.nextToken().trim();
                int space = grp.indexOf(" ");
                if (space > 0) {
                    String name = grp.substring(0, space);
                    String pkgList = grp.substring(space + 1);
                    toExecute.createArgument().setValue("-group");
                    toExecute.createArgument().setValue(name);
                    toExecute.createArgument().setValue(pkgList);
                }
            }
        }
    }

    private void doGroups(Commandline toExecute) {
        if (groups.size() != 0) {
            for (Enumeration e = groups.elements(); e.hasMoreElements();) {
                GroupArgument ga = (GroupArgument) e.nextElement();
                String title = ga.getTitle();
                String packages = ga.getPackages();
                if (title == null || packages == null) {
                    throw new BuildException("The title and packages must "
                                             + "be specified for group "
                                             + "elements.");
                }
                toExecute.createArgument().setValue("-group");
                toExecute.createArgument().setValue(expand(title));
                toExecute.createArgument().setValue(packages);
            }
        }
    }

    private void doJava14(Commandline toExecute) {
        for (Enumeration e = tags.elements(); e.hasMoreElements();) {
            Object element = e.nextElement();
            if (element instanceof TagArgument) {
                TagArgument ta = (TagArgument) element;
                File tagDir = ta.getDir(getProject());
                if (tagDir == null) {
                    // The tag element is not used as a fileset,
                    // but specifies the tag directly.
                    toExecute.createArgument().setValue ("-tag");
                    toExecute.createArgument()
                        .setValue (ta.getParameter());
                } else {
                    // The tag element is used as a
                    // fileset. Parse all the files and create
                    // -tag arguments.
                    DirectoryScanner tagDefScanner =
                        ta.getDirectoryScanner(getProject());
                    String[] files = tagDefScanner.getIncludedFiles();
                    for (int i = 0; i < files.length; i++) {
                        File tagDefFile = new File(tagDir, files[i]);
                        try {
                            BufferedReader in
                                = new BufferedReader(
                                    new FileReader(tagDefFile)
                                                     );
                            String line = null;
                            while ((line = in.readLine()) != null) {
                                toExecute.createArgument()
                                    .setValue("-tag");
                                toExecute.createArgument()
                                    .setValue(line);
                            }
                            in.close();
                        } catch (IOException ioe) {
                            throw new BuildException(
                                "Couldn't read "
                                + " tag file from "
                                + tagDefFile.getAbsolutePath(), ioe);
                        }
                    }
                }
            } else {
                ExtensionInfo tagletInfo = (ExtensionInfo) element;
                toExecute.createArgument().setValue("-taglet");
                toExecute.createArgument().setValue(tagletInfo
                                                    .getName());
                if (tagletInfo.getPath() != null) {
                    Path tagletPath = tagletInfo.getPath()
                        .concatSystemClasspath("ignore");
                    if (tagletPath.size() != 0) {
                        toExecute.createArgument()
                            .setValue("-tagletpath");
                        toExecute.createArgument().setPath(tagletPath);
                    }
                }
            }
        }

        String sourceArg = source != null ? source
            : getProject().getProperty(MagicNames.BUILD_JAVAC_SOURCE);
        if (sourceArg != null) {
            toExecute.createArgument().setValue("-source");
            toExecute.createArgument().setValue(sourceArg);
        }

        if (linksource && doclet == null) {
            toExecute.createArgument().setValue("-linksource");
        }
        if (noqualifier != null && doclet == null) {
            toExecute.createArgument().setValue("-noqualifier");
            toExecute.createArgument().setValue(noqualifier);
        }
    }

    private void doDocFilesSubDirs(Commandline toExecute) {
        if (docFilesSubDirs) {
            toExecute.createArgument().setValue("-docfilessubdirs");
            if (excludeDocFilesSubDir != null
                && excludeDocFilesSubDir.trim().length() > 0) {
                toExecute.createArgument().setValue("-excludedocfilessubdir");
                toExecute.createArgument().setValue(excludeDocFilesSubDir);
            }
        }
    }

    private void doSourceAndPackageNames(
        Commandline toExecute,
        Vector packagesToDoc,
        Vector sourceFilesToDoc,
        boolean useExternalFile,
        File    tmpList,
        BufferedWriter srcListWriter)
        throws IOException {
        Enumeration e = packagesToDoc.elements();
        while (e.hasMoreElements()) {
            String packageName = (String) e.nextElement();
            if (useExternalFile) {
                srcListWriter.write(packageName);
                srcListWriter.newLine();
            } else {
                toExecute.createArgument().setValue(packageName);
            }
        }

        e = sourceFilesToDoc.elements();
        while (e.hasMoreElements()) {
            SourceFile sf = (SourceFile) e.nextElement();
            String sourceFileName = sf.getFile().getAbsolutePath();
            if (useExternalFile) {
                // XXX what is the following doing?
                //     should it run if !javadoc4 && executable != null?
                if (sourceFileName.indexOf(" ") > -1) {
                    String name = sourceFileName;
                    if (File.separatorChar == '\\') {
                        name = sourceFileName.replace(File.separatorChar, '/');
                    }
                    srcListWriter.write("\"" + name + "\"");
                } else {
                    srcListWriter.write(sourceFileName);
                }
                srcListWriter.newLine();
            } else {
                toExecute.createArgument().setValue(sourceFileName);
            }
        }
    }
}
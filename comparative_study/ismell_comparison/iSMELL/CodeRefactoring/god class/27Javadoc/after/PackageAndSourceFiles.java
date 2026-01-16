package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.tools.ant.taskdefs.Javadoc.SourceFile;
import org.apache.tools.ant.taskdefs.Javadoc.PackageName;
import org.apache.tools.ant.taskdefs.Javadoc.ResourceCollectionContainer;

class PackageAndSourceFiles {
    private Vector packagesToDoc;
    private Vector sourceFilesToDoc;

    public PackageAndSourceFiles() {
        packagesToDoc = new Vector();
        sourceFilesToDoc = new Vector();
    }

    public void setSourcefiles(String src) {
        StringTokenizer tok = new StringTokenizer(src, ",");
        while (tok.hasMoreTokens()) {
            String f = tok.nextToken();
            SourceFile sf = new SourceFile();
            sf.setFile(getProject().resolveFile(f.trim()));
            addSource(sf);
        }
    }

    public void addSource(SourceFile sf) {
        sourceFiles.addElement(sf);
    }

    public void setPackagenames(String packages) {
        StringTokenizer tok = new StringTokenizer(packages, ",");
        while (tok.hasMoreTokens()) {
            String p = tok.nextToken();
            PackageName pn = new PackageName();
            pn.setName(p);
            addPackage(pn);
        }
    }

    public void addPackage(PackageName pn) {
        packageNames.addElement(pn);
    }

    public void setExcludePackageNames(String packages) {
        StringTokenizer tok = new StringTokenizer(packages, ",");
        while (tok.hasMoreTokens()) {
            String p = tok.nextToken();
            PackageName pn = new PackageName();
            pn.setName(p);
            addExcludePackage(pn);
        }
    }

    public void addExcludePackage(PackageName pn) {
        excludePackageNames.addElement(pn);
    }

    public void addPackageset(DirSet packageSet) {
        packageSets.addElement(packageSet);
    }

    public void addFileset(FileSet fs) {
        createSourceFiles().add(fs);
    }

    public ResourceCollectionContainer createSourceFiles() {
        return nestedSourceFiles;
    }

    private void parsePackages(Vector pn, Path sp) {
        HashSet addedPackages = new HashSet();
        Vector dirSets = (Vector) packageSets.clone();

        // for each sourcePath entry, add a directoryset with includes
        // taken from packagenames attribute and nested package
        // elements and excludes taken from excludepackages attribute
        // and nested excludepackage elements
        if (sourcePath != null) {
            PatternSet ps = new PatternSet();
            ps.setProject(getProject());
            if (packageNames.size() > 0) {
                Enumeration e = packageNames.elements();
                while (e.hasMoreElements()) {
                    PackageName p = (PackageName) e.nextElement();
                    String pkg = p.getName().replace('.', '/');
                    if (pkg.endsWith("*")) {
                        pkg += "*";
                    }
                    ps.createInclude().setName(pkg);
                }
            } else {
                ps.createInclude().setName("**");
            }

            Enumeration e = excludePackageNames.elements();
            while (e.hasMoreElements()) {
                PackageName p = (PackageName) e.nextElement();
                String pkg = p.getName().replace('.', '/');
                if (pkg.endsWith("*")) {
                    pkg += "*";
                }
                ps.createExclude().setName(pkg);
            }


            String[] pathElements = sourcePath.list();
            for (int i = 0; i < pathElements.length; i++) {
                File dir = new File(pathElements[i]);
                if (dir.isDirectory()) {
                    DirSet ds = new DirSet();
                    ds.setProject(getProject());
                    ds.setDefaultexcludes(useDefaultExcludes);
                    ds.setDir(dir);
                    ds.createPatternSet().addConfiguredPatternset(ps);
                    dirSets.addElement(ds);
                } else {
                    log("Skipping " + pathElements[i]
                        + " since it is no directory.", Project.MSG_WARN);
                }
            }
        }

        Enumeration e = dirSets.elements();
        while (e.hasMoreElements()) {
            DirSet ds = (DirSet) e.nextElement();
            File baseDir = ds.getDir(getProject());
            log("scanning " + baseDir + " for packages.", Project.MSG_DEBUG);
            DirectoryScanner dsc = ds.getDirectoryScanner(getProject());
            String[] dirs = dsc.getIncludedDirectories();
            boolean containsPackages = false;
            for (int i = 0; i < dirs.length; i++) {
                // are there any java files in this directory?
                File pd = new File(baseDir, dirs[i]);
                String[] files = pd.list(new FilenameFilter () {
                        public boolean accept(File dir1, String name) {
                            return name.endsWith(".java")
                                || (includeNoSourcePackages
                                    && name.equals("package.html"));
                        }
                    });

                if (files.length > 0) {
                    if ("".equals(dirs[i])) {
                        log(baseDir
                            + " contains source files in the default package,"
                            + " you must specify them as source files"
                            + " not packages.",
                            Project.MSG_WARN);
                    } else {
                        containsPackages = true;
                        String packageName =
                            dirs[i].replace(File.separatorChar, '.');
                        if (!addedPackages.contains(packageName)) {
                            addedPackages.add(packageName);
                            pn.addElement(packageName);
                        }
                    }
                }
            }
            if (containsPackages) {
                // We don't need to care for duplicates here,
                // Path.list does it for us.
                sp.createPathElement().setLocation(baseDir);
            } else {
                log(baseDir + " doesn\'t contain any packages, dropping it.",
                    Project.MSG_VERBOSE);
            }
        }
    }

    protected void checkPackages(Vector packagesToDoc, Path sourceDirs) {
        if (packagesToDoc.size() != 0 && sourceDirs.size() == 0) {
            String msg = "sourcePath attribute must be set when "
                + "specifying package names.";
            throw new BuildException(msg);
        }
    }

    protected void addSourceFiles(Vector sf) {
        Iterator e = nestedSourceFiles.iterator();
        while (e.hasNext()) {
            ResourceCollection rc = (ResourceCollection) e.next();
            if (!rc.isFilesystemOnly()) {
                throw new BuildException("only file system based resources are"
                                         + " supported by javadoc");
            }
            if (rc instanceof FileSet) {
                FileSet fs = (FileSet) rc;
                if (!fs.hasPatterns() && !fs.hasSelectors()) {
                    FileSet fs2 = (FileSet) fs.clone();
                    fs2.createInclude().setName("**/*.java");
                    if (includeNoSourcePackages) {
                        fs2.createInclude().setName("**/package.html");
                    }
                    rc = fs2;
                }
            }
            Iterator iter = rc.iterator();
            while (iter.hasNext()) {
                Resource r = (Resource) iter.next();
                sf.addElement(new SourceFile(((FileProvider) r.as(FileProvider.class)).getFile()));
            }
        }
    }
}
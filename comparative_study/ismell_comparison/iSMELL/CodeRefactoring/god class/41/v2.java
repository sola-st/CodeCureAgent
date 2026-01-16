class PatternMatcher{
    /**
     * Test whether or not a string matches against a pattern.
     * The pattern may contain two special characters:<br>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     *
     * @param pattern The pattern to match against.
     *                Must not be <code>null</code>.
     * @param str     The string which must be matched against the pattern.
     *                Must not be <code>null</code>.
     *
     * @return <code>true</code> if the string matches against the pattern,
     *         or <code>false</code> otherwise.
     */
    public static boolean match(String pattern, String str) {
        return SelectorUtils.match(pattern, str);
    }

    /**
     * Test whether or not a string matches against a pattern.
     * The pattern may contain two special characters:<br>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     *
     * @param pattern The pattern to match against.
     *                Must not be <code>null</code>.
     * @param str     The string which must be matched against the pattern.
     *                Must not be <code>null</code>.
     * @param isCaseSensitive Whether or not matching should be performed
     *                        case sensitively.
     *
     *
     * @return <code>true</code> if the string matches against the pattern,
     *         or <code>false</code> otherwise.
     */
    protected static boolean match(String pattern, String str,
                                   boolean isCaseSensitive) {
        return SelectorUtils.match(pattern, str, isCaseSensitive);
    }
    /**
     * Test whether or not a given path matches a given pattern.
     *
     * @param pattern The pattern to match against. Must not be
     *                <code>null</code>.
     * @param str     The path to match, as a String. Must not be
     *                <code>null</code>.
     *
     * @return <code>true</code> if the pattern matches against the string,
     *         or <code>false</code> otherwise.
     */
    protected static boolean matchPath(String pattern, String str) {
        return SelectorUtils.matchPath(pattern, str);
    }

    /**
     * Test whether or not a given path matches a given pattern.
     *
     * @param pattern The pattern to match against. Must not be
     *                <code>null</code>.
     * @param str     The path to match, as a String. Must not be
     *                <code>null</code>.
     * @param isCaseSensitive Whether or not matching should be performed
     *                        case sensitively.
     *
     * @return <code>true</code> if the pattern matches against the string,
     *         or <code>false</code> otherwise.
     */
    protected static boolean matchPath(String pattern, String str,
                                       boolean isCaseSensitive) {
        return SelectorUtils.matchPath(pattern, str, isCaseSensitive);
    }
    /**
     * Test whether or not a given path matches the start of a given
     * pattern up to the first "**".
     * <p>
     * This is not a general purpose test and should only be used if you
     * can live with false positives. For example, <code>pattern=**\a</code>
     * and <code>str=b</code> will yield <code>true</code>.
     *
     * @param pattern The pattern to match against. Must not be
     *                <code>null</code>.
     * @param str     The path to match, as a String. Must not be
     *                <code>null</code>.
     *
     * @return whether or not a given path matches the start of a given
     * pattern up to the first "**".
     */
    protected static boolean matchPatternStart(String pattern, String str) {
        return SelectorUtils.matchPatternStart(pattern, str);
    }

    /**
     * Test whether or not a given path matches the start of a given
     * pattern up to the first "**".
     * <p>
     * This is not a general purpose test and should only be used if you
     * can live with false positives. For example, <code>pattern=**\a</code>
     * and <code>str=b</code> will yield <code>true</code>.
     *
     * @param pattern The pattern to match against. Must not be
     *                <code>null</code>.
     * @param str     The path to match, as a String. Must not be
     *                <code>null</code>.
     * @param isCaseSensitive Whether or not matching should be performed
     *                        case sensitively.
     *
     * @return whether or not a given path matches the start of a given
     * pattern up to the first "**".
     */
    protected static boolean matchPatternStart(String pattern, String str,
                                               boolean isCaseSensitive) {
        return SelectorUtils.matchPatternStart(pattern, str, isCaseSensitive);
    }

}

// Class for handling include patterns
class IncludePatternMatcher {
    private PatternMatcher patternMatcher;

    public IncludePatternMatcher(PatternMatcher patternMatcher) {
        this.patternMatcher = patternMatcher;
    }

    public boolean matches(String path) {
        // Implementation
        return false;
    }
    /**
     * Test whether or not a name matches against at least one include
     * pattern.
     *
     * @param name The name to match. Must not be <code>null</code>.
     * @return <code>true</code> when the name matches against at least one
     *         include pattern, or <code>false</code> otherwise.
     */
    protected boolean isIncluded(String name) {
        ensureNonPatternSetsReady();

        if (isCaseSensitive()
                ? includeNonPatterns.contains(name)
                : includeNonPatterns.contains(name.toUpperCase())) {
            return true;
        }
        for (int i = 0; i < includePatterns.length; i++) {
            if (matchPath(includePatterns[i], name, isCaseSensitive())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test whether or not a name matches the start of at least one include
     * pattern.
     *
     * @param name The name to match. Must not be <code>null</code>.
     * @return <code>true</code> when the name matches against the start of at
     *         least one include pattern, or <code>false</code> otherwise.
     */
    protected boolean couldHoldIncluded(String name) {
        for (int i = 0; i < includes.length; i++) {
            if (matchPatternStart(includes[i], name, isCaseSensitive())
                    && isMorePowerfulThanExcludes(name, includes[i])
                    && isDeeper(includes[i], name)) {
                return true;
            }
        }
        return false;
    }

    /**
     *  Find out whether one particular include pattern is more powerful
     *  than all the excludes.
     *  Note:  the power comparison is based on the length of the include pattern
     *  and of the exclude patterns without the wildcards.
     *  Ideally the comparison should be done based on the depth
     *  of the match; that is to say how many file separators have been matched
     *  before the first ** or the end of the pattern.
     *
     *  IMPORTANT : this function should return false "with care".
     *
     *  @param name the relative path to test.
     *  @param includepattern one include pattern.
     *  @return true if there is no exclude pattern more powerful than this include pattern.
     *  @since Ant 1.6
     */
    private boolean isMorePowerfulThanExcludes(String name, String includepattern) {
        String soughtexclude = name + File.separator + "**";
        for (int counter = 0; counter < excludes.length; counter++) {
            if (excludes[counter].equals(soughtexclude))  {
                return false;
            }
        }
        return true;
    }
}

// Class for handling exclude patterns
class ExcludePatternMatcher {
    private PatternMatcher patternMatcher;

    public ExcludePatternMatcher(PatternMatcher patternMatcher) {
        this.patternMatcher = patternMatcher;
    }

    public boolean matches(String path) {
        // Implementation
        return false;
    }
    /**
     * Test whether or not a name matches against at least one exclude
     * pattern.
     *
     * @param name The name to match. Must not be <code>null</code>.
     * @return <code>true</code> when the name matches against at least one
     *         exclude pattern, or <code>false</code> otherwise.
     */
    protected boolean isExcluded(String name) {
        ensureNonPatternSetsReady();

        if (isCaseSensitive()
                ? excludeNonPatterns.contains(name)
                : excludeNonPatterns.contains(name.toUpperCase())) {
            return true;
        }
        for (int i = 0; i < excludePatterns.length; i++) {
            if (matchPath(excludePatterns[i], name, isCaseSensitive())) {
                return true;
            }
        }
        return false;
    }
}

// Utility class for symlink detection
class SymlinkDetector {
    /**
     * Do we have to traverse a symlink when trying to reach path from
     * basedir?
     * @param base base File (dir).
     * @param path file path.
     * @since Ant 1.6
     */
    private boolean isSymlink(File base, String path) {
        return isSymlink(base, SelectorUtils.tokenizePath(path));
    }

    /**
     * Do we have to traverse a symlink when trying to reach path from
     * basedir?
     * @param base base File (dir).
     * @param pathElements Vector of path elements (dirs...file).
     * @since Ant 1.6
     */
    private boolean isSymlink(File base, Vector pathElements) {
        if (pathElements.size() > 0) {
            String current = (String) pathElements.remove(0);
            try {
                return FILE_UTILS.isSymbolicLink(base, current)
                        || isSymlink(new File(base, current), pathElements);
            } catch (IOException ioe) {
                String msg = "IOException caught while checking "
                        + "for links, couldn't get canonical path!";
                // will be caught and redirected to Ant's logging system
                System.err.println(msg);
            }
        }
        return false;
    }
}

// Utility class for path normalization
class PathNormalizer {
    /**
     * All '/' and '\' characters are replaced by
     * <code>File.separatorChar</code>, so the separator used need not
     * match <code>File.separatorChar</code>.
     *
     * <p> When a pattern ends with a '/' or '\', "**" is appended.
     *
     * @since Ant 1.6.3
     */
    public static String normalizePattern(String p) {
        String pattern = p.replace('/', File.separatorChar)
                .replace('\\', File.separatorChar);
        if (pattern.endsWith(File.separator)) {
            pattern += "**";
        }
        return pattern;
    }
}

// Class to manage scan results
class ScanResult {
    private List<String> includedFiles = new ArrayList<>();
    private List<String> excludedFiles = new ArrayList<>();
    // Add directories, deselected files, etc., as needed

    public void addIncludedFile(String path) {
        includedFiles.add(path);
    }

    public void addExcludedFile(String path) {
        excludedFiles.add(path);
    }

    // Getter methods and other result management functionalities
    /**
     * Return the names of the files which matched at least one of the
     * include patterns and none of the exclude patterns.
     * The names are relative to the base directory.
     *
     * @return the names of the files which matched at least one of the
     *         include patterns and none of the exclude patterns.
     */
    public synchronized String[] getIncludedFiles() {
        if (filesIncluded == null) {
            throw new IllegalStateException();
        }
        String[] files = new String[filesIncluded.size()];
        filesIncluded.copyInto(files);
        Arrays.sort(files);
        return files;
    }

    /**
     * Return the count of included files.
     * @return <code>int</code>.
     * @since Ant 1.6.3
     */
    public synchronized int getIncludedFilesCount() {
        if (filesIncluded == null) {
            throw new IllegalStateException();
        }
        return filesIncluded.size();
    }

    /**
     * Return the names of the files which matched none of the include
     * patterns. The names are relative to the base directory. This involves
     * performing a slow scan if one has not already been completed.
     *
     * @return the names of the files which matched none of the include
     *         patterns.
     *
     * @see #slowScan
     */
    public synchronized String[] getNotIncludedFiles() {
        slowScan();
        String[] files = new String[filesNotIncluded.size()];
        filesNotIncluded.copyInto(files);
        return files;
    }

    /**
     * Return the names of the files which matched at least one of the
     * include patterns and at least one of the exclude patterns.
     * The names are relative to the base directory. This involves
     * performing a slow scan if one has not already been completed.
     *
     * @return the names of the files which matched at least one of the
     *         include patterns and at least one of the exclude patterns.
     *
     * @see #slowScan
     */
    public synchronized String[] getExcludedFiles() {
        slowScan();
        String[] files = new String[filesExcluded.size()];
        filesExcluded.copyInto(files);
        return files;
    }

    /**
     * <p>Return the names of the files which were selected out and
     * therefore not ultimately included.</p>
     *
     * <p>The names are relative to the base directory. This involves
     * performing a slow scan if one has not already been completed.</p>
     *
     * @return the names of the files which were deselected.
     *
     * @see #slowScan
     */
    public synchronized String[] getDeselectedFiles() {
        slowScan();
        String[] files = new String[filesDeselected.size()];
        filesDeselected.copyInto(files);
        return files;
    }

    /**
     * Return the names of the directories which matched at least one of the
     * include patterns and none of the exclude patterns.
     * The names are relative to the base directory.
     *
     * @return the names of the directories which matched at least one of the
     * include patterns and none of the exclude patterns.
     */
    public synchronized String[] getIncludedDirectories() {
        if (dirsIncluded == null) {
            throw new IllegalStateException();
        }
        String[] directories = new String[dirsIncluded.size()];
        dirsIncluded.copyInto(directories);
        Arrays.sort(directories);
        return directories;
    }

    /**
     * Return the count of included directories.
     * @return <code>int</code>.
     * @since Ant 1.6.3
     */
    public synchronized int getIncludedDirsCount() {
        if (dirsIncluded == null) {
            throw new IllegalStateException();
        }
        return dirsIncluded.size();
    }

    /**
     * Return the names of the directories which matched none of the include
     * patterns. The names are relative to the base directory. This involves
     * performing a slow scan if one has not already been completed.
     *
     * @return the names of the directories which matched none of the include
     * patterns.
     *
     * @see #slowScan
     */
    public synchronized String[] getNotIncludedDirectories() {
        slowScan();
        String[] directories = new String[dirsNotIncluded.size()];
        dirsNotIncluded.copyInto(directories);
        return directories;
    }

    /**
     * Return the names of the directories which matched at least one of the
     * include patterns and at least one of the exclude patterns.
     * The names are relative to the base directory. This involves
     * performing a slow scan if one has not already been completed.
     *
     * @return the names of the directories which matched at least one of the
     * include patterns and at least one of the exclude patterns.
     *
     * @see #slowScan
     */
    public synchronized String[] getExcludedDirectories() {
        slowScan();
        String[] directories = new String[dirsExcluded.size()];
        dirsExcluded.copyInto(directories);
        return directories;
    }

    /**
     * <p>Return the names of the directories which were selected out and
     * therefore not ultimately included.</p>
     *
     * <p>The names are relative to the base directory. This involves
     * performing a slow scan if one has not already been completed.</p>
     *
     * @return the names of the directories which were deselected.
     *
     * @see #slowScan
     */
    public synchronized String[] getDeselectedDirectories() {
        slowScan();
        String[] directories = new String[dirsDeselected.size()];
        dirsDeselected.copyInto(directories);
        return directories;
    }

    /**
     * Clear the result caches for a scan.
     */
    protected synchronized void clearResults() {
        filesIncluded    = new Vector();
        filesNotIncluded = new Vector();
        filesExcluded    = new Vector();
        filesDeselected  = new Vector();
        dirsIncluded     = new Vector();
        dirsNotIncluded  = new Vector();
        dirsExcluded     = new Vector();
        dirsDeselected   = new Vector();
        everythingIncluded = (basedir != null);
        scannedDirs.clear();
    }
}

// Class for selecting files based on patterns and selectors
class FileSelectionStrategy {
    private IncludePatternMatcher includeMatcher;
    private ExcludePatternMatcher excludeMatcher;

    public FileSelectionStrategy(IncludePatternMatcher includeMatcher, ExcludePatternMatcher excludeMatcher) {
        this.includeMatcher = includeMatcher;
        this.excludeMatcher = excludeMatcher;
    }

    /**
     * Test whether a file should be selected.
     *
     * @param name the filename to check for selecting.
     * @param file the java.io.File object for this filename.
     * @return <code>false</code> when the selectors says that the file
     *         should not be selected, <code>true</code> otherwise.
     */
    protected boolean isSelected(String name, File file) {
        if (selectors != null) {
            for (int i = 0; i < selectors.length; i++) {
                if (!selectors[i].isSelected(basedir, name, file)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Set the selectors that will select the filelist.
     *
     * @param selectors specifies the selectors to be invoked on a scan.
     */
    public synchronized void setSelectors(FileSelector[] selectors) {
        this.selectors = selectors;
    }
}

// Class for actual directory traversal and application of selection strategy
class DirectoryWalker {
    private FileSelectionStrategy selectionStrategy;
    private File baseDir;
    private ScanResult scanResult;

    public DirectoryWalker(File baseDir, FileSelectionStrategy selectionStrategy, ScanResult scanResult) {
        this.baseDir = baseDir;
        this.selectionStrategy = selectionStrategy;
        this.scanResult = scanResult;
    }

    /**
     * Scan the given directory for files and directories. Found files and
     * directories are placed in their respective collections, based on the
     * matching of includes, excludes, and the selectors.  When a directory
     * is found, it is scanned recursively.
     *
     * @param dir   The directory to scan. Must not be <code>null</code>.
     * @param vpath The path relative to the base directory (needed to
     *              prevent problems with an absolute path when using
     *              dir). Must not be <code>null</code>.
     * @param fast  Whether or not this call is part of a fast scan.
     *
     * @see #filesIncluded
     * @see #filesNotIncluded
     * @see #filesExcluded
     * @see #dirsIncluded
     * @see #dirsNotIncluded
     * @see #dirsExcluded
     * @see #slowScan
     */
    public void scandir(File dir, String vpath, boolean fast) {
        if (dir == null) {
            throw new BuildException("dir must not be null.");
        } else if (!dir.exists()) {
            throw new BuildException(dir + " doesn't exists.");
        } else if (!dir.isDirectory()) {
            throw new BuildException(dir + " is not a directory.");
        }
        // avoid double scanning of directories, can only happen in fast mode
        if (fast && hasBeenScanned(vpath)) {
            return;
        }
        String[] newfiles = dir.list();

        if (newfiles == null) {
            throw new BuildException("IO error scanning directory "
                    + dir.getAbsolutePath());
        }
        if (!followSymlinks) {
            Vector noLinks = new Vector();
            for (int i = 0; i < newfiles.length; i++) {
                try {
                    if (FILE_UTILS.isSymbolicLink(dir, newfiles[i])) {
                        String name = vpath + newfiles[i];
                        File file = new File(dir, newfiles[i]);
                        (file.isDirectory()
                                ? dirsExcluded : filesExcluded).addElement(name);
                    } else {
                        noLinks.addElement(newfiles[i]);
                    }
                } catch (IOException ioe) {
                    String msg = "IOException caught while checking "
                            + "for links, couldn't get canonical path!";
                    // will be caught and redirected to Ant's logging system
                    System.err.println(msg);
                    noLinks.addElement(newfiles[i]);
                }
            }
            newfiles = new String[noLinks.size()];
            noLinks.copyInto(newfiles);
        }
        for (int i = 0; i < newfiles.length; i++) {
            String name = vpath + newfiles[i];
            File file = new File(dir, newfiles[i]);
            if (file.isDirectory()) {
                if (isIncluded(name)) {
                    accountForIncludedDir(name, file, fast);
                } else {
                    everythingIncluded = false;
                    dirsNotIncluded.addElement(name);
                    if (fast && couldHoldIncluded(name)) {
                        scandir(file, name + File.separator, fast);
                    }
                }
                if (!fast) {
                    scandir(file, name + File.separator, fast);
                }
            } else if (file.isFile()) {
                if (isIncluded(name)) {
                    accountForIncludedFile(name, file);
                } else {
                    everythingIncluded = false;
                    filesNotIncluded.addElement(name);
                }
            }
        }
    }
    /**
     * Process included file.
     * @param name  path of the file relative to the directory of the FileSet.
     * @param file  included File.
     */
    private void accountForIncludedFile(String name, File file) {
        if (filesIncluded.contains(name)
                || filesExcluded.contains(name)
                || filesDeselected.contains(name)) {
            return;
        }
        boolean included = false;
        if (isExcluded(name)) {
            filesExcluded.addElement(name);
        } else if (isSelected(name, file)) {
            included = true;
            filesIncluded.addElement(name);
        } else {
            filesDeselected.addElement(name);
        }
        everythingIncluded &= included;
    }

    /**
     * Process included directory.
     * @param name path of the directory relative to the directory of
     *             the FileSet.
     * @param file directory as File.
     * @param fast whether to perform fast scans.
     */
    private void accountForIncludedDir(String name, File file, boolean fast) {
        if (dirsIncluded.contains(name)
                || dirsExcluded.contains(name)
                || dirsDeselected.contains(name)) {
            return;
        }
        boolean included = false;
        if (isExcluded(name)) {
            dirsExcluded.addElement(name);
        } else if (isSelected(name, file)) {
            included = true;
            dirsIncluded.addElement(name);
        } else {
            dirsDeselected.addElement(name);
        }
        everythingIncluded &= included;
        if (fast && couldHoldIncluded(name) && !contentsExcluded(name)) {
            scandir(file, name + File.separator, fast);
        }
    }

    /**
     * This routine is actually checking all the include patterns in
     * order to avoid scanning everything under base dir.
     * @since Ant 1.6
     */
    public void checkIncludePatterns() {
        Hashtable newroots = new Hashtable();
        // put in the newroots vector the include patterns without
        // wildcard tokens
        for (int icounter = 0; icounter < includes.length; icounter++) {
            newroots.put(SelectorUtils.rtrimWildcardTokens(
                    includes[icounter]), includes[icounter]);
        }
        if (newroots.containsKey("")) {
            // we are going to scan everything anyway
            scandir(basedir, "", true);
        } else {
            // only scan directories that can include matched files or
            // directories
            Enumeration enum2 = newroots.keys();

            File canonBase = null;
            try {
                canonBase = basedir.getCanonicalFile();
            } catch (IOException ex) {
                throw new BuildException(ex);
            }
            while (enum2.hasMoreElements()) {
                String currentelement = (String) enum2.nextElement();
                String originalpattern = (String) newroots.get(currentelement);
                File myfile = new File(basedir, currentelement);

                if (myfile.exists()) {
                    // may be on a case insensitive file system.  We want
                    // the results to show what's really on the disk, so
                    // we need to double check.
                    try {
                        File canonFile = myfile.getCanonicalFile();
                        String path = FILE_UTILS.removeLeadingPath(canonBase,
                                canonFile);
                        if (!path.equals(currentelement) || ON_VMS) {
                            myfile = findFile(basedir, currentelement, true);
                            if (myfile != null) {
                                currentelement =
                                        FILE_UTILS.removeLeadingPath(basedir,
                                                myfile);
                            }
                        }
                    } catch (IOException ex) {
                        throw new BuildException(ex);
                    }
                }
                if ((myfile == null || !myfile.exists()) && !isCaseSensitive()) {
                    File f = findFile(basedir, currentelement, false);
                    if (f.exists()) {
                        // adapt currentelement to the case we've
                        // actually found
                        currentelement = FILE_UTILS.removeLeadingPath(basedir,
                                f);
                        myfile = f;
                    }
                }
                if (myfile != null && myfile.exists()) {
                    if (!followSymlinks
                            && isSymlink(basedir, currentelement)) {
                        continue;
                    }
                    if (myfile.isDirectory()) {
                        if (isIncluded(currentelement)
                                && currentelement.length() > 0) {
                            accountForIncludedDir(currentelement, myfile, true);
                        }  else {
                            if (currentelement.length() > 0) {
                                if (currentelement.charAt(currentelement
                                        .length() - 1)
                                        != File.separatorChar) {
                                    currentelement =
                                            currentelement + File.separatorChar;
                                }
                            }
                            scandir(myfile, currentelement, true);
                        }
                    } else {
                        boolean included = isCaseSensitive()
                                ? originalpattern.equals(currentelement)
                                : originalpattern.equalsIgnoreCase(currentelement);
                        if (included) {
                            accountForIncludedFile(currentelement, myfile);
                        }
                    }
                }
            }
        }
    }


    /**
     * Top level invocation for a slow scan. A slow scan builds up a full
     * list of excluded/included files/directories, whereas a fast scan
     * will only have full results for included files, as it ignores
     * directories which can't possibly hold any included files/directories.
     * <p>
     * Returns immediately if a slow scan has already been completed.
     */
    public void slowScan() {
        synchronized (slowScanLock) {
            if (haveSlowResults) {
                return;
            }
            if (slowScanning) {
                while (slowScanning) {
                    try {
                        slowScanLock.wait();
                    } catch (InterruptedException e) {
                    }
                }
                return;
            }
            slowScanning = true;
        }
        try {
            synchronized (this) {

                String[] excl = new String[dirsExcluded.size()];
                dirsExcluded.copyInto(excl);

                String[] notIncl = new String[dirsNotIncluded.size()];
                dirsNotIncluded.copyInto(notIncl);

                for (int i = 0; i < excl.length; i++) {
                    if (!couldHoldIncluded(excl[i])) {
                        scandir(new File(basedir, excl[i]),
                                excl[i] + File.separator, false);
                    }
                }
                for (int i = 0; i < notIncl.length; i++) {
                    if (!couldHoldIncluded(notIncl[i])) {
                        scandir(new File(basedir, notIncl[i]),
                                notIncl[i] + File.separator, false);
                    }
                }
            }
        } finally {
            synchronized (slowScanLock) {
                haveSlowResults = true;
                slowScanning = false;
                slowScanLock.notifyAll();
            }
        }
    }

    /**
     * Has the directory with the given path relative to the base
     * directory already been scanned?
     *
     * <p>Registers the given directory as scanned as a side effect.</p>
     *
     * @since Ant 1.6
     */
    private boolean hasBeenScanned(String vpath) {
        return !scannedDirs.add(vpath);
    }

}


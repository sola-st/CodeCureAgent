```java
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package at.redeye.FrameWork.utilities;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple case insesitive file extension filter
 * @author moberza
 */
public class FileExtFilter implements FilenameFilter, FileFilter
{
    // private static final Logger logger = Logger.getLogger("FileExtFilter");

    final private Pattern pattern;

    /**
     * eg: FileExtFilter("*.txt *.html");
     * @param extensions all required file extensions here. Space, comma, tab, and semicolon are allowed
     */
    public FileExtFilter(String extensions)
    {
        final String[] ext = extensions.split("[ \t;,]");

        final StringBuilder regexString = new StringBuilder("^.*\\.");
        boolean firstRegex = true;

        regexString.append("(");

        for (String s : ext)
        {
            // remove *.
            s = s.replaceAll("\\*\\.", "");

            String sLower = s;

            if (firstRegex)
                firstRegex = false;
            else
                regexString.append("|");

            regexString.append("(");

            for (int i = 0; i < sLower.length(); i++)
            {
                char cLower = sLower.charAt(i);

                if (cLower == '*') {
                    regexString.append(".*");
                } else if (cLower == '.') {
                    regexString.append("\\.");
                } else {
                    regexString.append(cLower);
                }
            }

            regexString.append(")");
        }

        regexString.append(")");

        regexString.append("$");

        // logger.info("regex: " + regexString);

        pattern = Pattern.compile(regexString.toString(), Pattern.CASE_INSENSITIVE);
    }

    @Override
    public boolean accept(File dir, String name)
    {
        //logger.info(name);

        Matcher matcher = pattern.matcher(name);
        return matcher.matches();
    }

    @Override
    public boolean accept(File pathname) {
        return accept(null, pathname.getName());
    }

}

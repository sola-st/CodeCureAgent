        // just to check if temp.stat.it.iteration exists
        boolean statsCurrItExists = false;

        if (fileExists(tmpDirPrefix + "temp.stats.it" + iteration)) {
          inStream_statsCurrIt = new FileInputStream(tmpDirPrefix + "temp.stats.it" + iteration);
          inFile_statsCurrIt = new BufferedReader(new InputStreamReader(inStream_statsCurrIt,
              "utf8"));
          statsCurrItExists = true;
          copyFile(tmpDirPrefix + "temp.stats.it" + iteration, tmpDirPrefix + "temp.stats.it"
              + iteration + ".copy");
        } else if (fileExists(tmpDirPrefix + "temp.stats.it" + iteration + ".gz")) {
          inStream_statsCurrIt = new GZIPInputStream(new FileInputStream(tmpDirPrefix
              + "temp.stats.it" + iteration + ".gz"));
          inFile_statsCurrIt = new BufferedReader(new InputStreamReader(inStream_statsCurrIt,
              "utf8"));
          statsCurrItExists = true;
          copyFile(tmpDirPrefix + "temp.stats.it" + iteration + ".gz", tmpDirPrefix
              + "temp.stats.it" + iteration + ".copy.gz");
        } else {
          outFile_statsCurrIt = new PrintWriter(tmpDirPrefix + "temp.stats.it" + iteration);
        }
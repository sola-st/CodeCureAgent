```java
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.joshua.metrics;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.TreeMap;

/***
 * @author Omar Zaidan
 */
public abstract class EvaluationMetric {
  /* static data members */
  private static TreeMap<String, Integer> metricOptionCount; // maps metric names -> number of
                                                             // options for that metric
  protected static int numSentences; // number of sentences in the MERT set
  protected static int numDocuments; // number of documents in the MERT set
  protected static int refsPerSen;
  protected static String[][] refSentences;
  protected final static DecimalFormat f0 = new DecimalFormat("###0");
  protected final static DecimalFormat f4 = new DecimalFormat("###0.0000");
  protected static String tmpDirPrefix;

  /* non-static data members */
  protected int suffStatsCount; // number of sufficient statistics
  protected String metricName; // number of metric
  protected boolean toBeMinimized;

  // is this a metric that should be minimized?
  // e.g. toBeMinimized = true for 01LOSS, WER, TER
  // toBeMinimized = false for BLEU

  /* static (=> also non-abstract) methods */
  public static void set_knownMetrics() {
    metricOptionCount = new TreeMap<>();

    metricOptionCount.put("BLEU", 2);
    // the "BLEU" metric expects an options array of length 2
    metricOptionCount.put("BLEU_SBP", 2);
    // the "BLEU_SBP" metric expects an options array of length 2
    metricOptionCount.put("01LOSS", 0);
    // the "01LOSS" metric expects an options array of length 0
    metricOptionCount.put("TER", 6);
    // the "TER" metric expects an options array of length 5
    // metricOptionCount.put("METEOR",4);
    // the "METEOR" metric expects an options array of length 4
    // metricOptionCount.put("RYPT",5);
    // the "RYPT" metric expects an options array of length 5
    metricOptionCount.put("TER-BLEU", 8);
    // the "TER-BLEU" metric expects an options array of length 7
    // metricOptionCount.put("WER",0);
    // the "WER" metric expects an options array of length 0
    metricOptionCount.put("MC_BLEU", 4);
    metricOptionCount.put("PRECIS", 6);
    metricOptionCount.put("SRC_BLEU", 4);
    metricOptionCount.put("PRECIS-SRC_BLEU", 6);
    metricOptionCount.put("GL_BLEU", 3);
    metricOptionCount.put("SARI", 2); // options: max-ngram source-path
    metricOptionCount.put("CHRF", 2); // options: beta (how much to weight recall vs precision) and max-ngram
  }

  public static EvaluationMetric getMetric(String metricName, String[] metricOptions) {
    EvaluationMetric retMetric = null;

    switch (metricName) {
    case "BLEU":
      retMetric = new BLEU(metricOptions); // the "BLEU" metric corresponds to the BLEU class

      break;
    case "BLEU_SBP":
      retMetric = new BLEU_SBP(metricOptions); // the "BLEU_SBP" metric corresponds to the BLEU_SBP

      // class
      break;
    case "01LOSS":
      retMetric = new ZeroOneLoss(metricOptions); // the "01LOSS" metric corresponds to the

      // ZeroOneLoss class
      break;
    case "TER":
      retMetric = new TER(metricOptions); // the "TER" metric corresponds to the TER class

      // } else if (metricName.equals("METEOR")) {
      // retMetric = new METEOR(metricOptions); // the "METEOR" metric corresponds to the METEOR
      // class
      // } else if (metricName.equals("RYPT")) {
      // retMetric = new RYPT(metricOptions); // the "RYPT" metric corresponds to the RYPT class
      break;
    case "TER-BLEU":
      retMetric = new TERMinusBLEU(metricOptions); // the "TER-BLEU" metric corresponds to the

      // TERMinusBLEU class
      // } else if (metricName.equals("WER")) {
      // retMetric = new WordErrorRate(metricOptions); // the "WER" metric corresponds to the
      // WordErrorRate class
      break;
    case "MC_BLEU":
      retMetric = new MinimumChangeBLEU(metricOptions); // the "MC_BLEU" metric corresponds to the

      // ParaphraseBLEU class
      break;
    case "PRECIS":
      retMetric = new Precis(metricOptions);
      break;
    case "SRC_BLEU":
      retMetric = new SourceBLEU(metricOptions);
      break;
    case "PRECIS-SRC_BLEU":
      retMetric = new PrecisMinusSourceBLEU(metricOptions);
      break;
    case "GL_BLEU":
      retMetric = new GradeLevelBLEU(metricOptions); // the "GL_BLEU" metric corresponds to the

      // GradeLevelBLEU class
      break;
    case "SARI":
      retMetric = new SARI(metricOptions);

      break;
    case "CHRF":
      retMetric = new CHRF(metricOptions);
      break;
    }
    
    return retMetric;
  }

  public static void set_numSentences(int x) {
    numSentences = x;
  }

  public static void set_numDocuments(int x) {
    numDocuments = x;
  }

  public static void set_refsPerSen(int x) {
    refsPerSen = x;
  }

  public static void set_tmpDirPrefix(String s) {
    tmpDirPrefix = s;
  }

  public static void set_refSentences(String[][] refs) {
    refSentences = new String[numSentences][refsPerSen];
    for (int i = 0; i < numSentences; ++i) {
      System.arraycopy(refs[i], 0, refSentences[i], 0, refsPerSen);
    }
  }

  public static boolean knownMetricName(String name) {
    return metricOptionCount.containsKey(name);
  }

  public static int metricOptionCount(String name) {
    return metricOptionCount.get(name);
  }

  /* non-abstract, non-static methods */
  public int get_suffStatsCount() {
    return suffStatsCount;
  }

  public String get_metricName() {
    return metricName;
  }

  public boolean getToBeMinimized() {
    return toBeMinimized;
  }

  public boolean isBetter(double x, double y) {
    // return true if x is better than y
    if (toBeMinimized) {
      return (x < y);
    } else {
      return (x > y);
    }
  }

  public double score(String candStr, int i) {
    String[] sa = new String[1];
    sa[0] = candStr;
    int[] ia = new int[1];
    ia[0] = i;

    int[][] ss = suffStats(sa, ia);

    int[] stats = new int[suffStatsCount];
    System.arraycopy(ss[0], 0, stats, 0, suffStatsCount);

    return score(stats);
  }

  public double score(String[] topCandStr) {
    int[] stats = suffStats(topCandStr);
    return score(stats);
  }

  public int[] suffStats(String[] topCandStr) {
    int[] ia = new int[numSentences];
    for (int i = 0; i < numSentences; ++i) {
      ia[i] = i;
    }

    int[][] ss = suffStats(topCandStr, ia);

    int[] totStats = new int[suffStatsCount];
    for (int s = 0; s < suffStatsCount; ++s) {
      totStats[s] = 0;
      for (int i = 0; i < numSentences; ++i) {
        totStats[s] += ss[i][s];
      }
    }

    return totStats;
  }

  /**
   * Calculates sufficient statistics on each sentence in the corpus, returning them as arrays.
   * 
   * @param candStrings todo
   * @param candIndices todo
   * @return todo
   */
  public int[][] suffStats(String[] candStrings, int[] candIndices) {

    int candCount = candStrings.length;
    if (candIndices.length != candCount) {
      System.out.println("Array lengths mismatch in suffStats(String[],int[]); returning null.");
      return null;
    }

    int[][] stats = new int[candCount][suffStatsCount];

    for (int d = 0; d < candCount; ++d) {
      int[] currStats = suffStats(candStrings[d], candIndices[d]);

      System.arraycopy(currStats, 0, stats[d], 0, suffStatsCount);
    } // for (d)

    return stats;
  }

  public void createSuffStatsFile(String candStringsFileName, String candIndicesFileName,
      String outputFileName, int maxBatchSize) {
    // similar to the above suffStats(String[], int[])

    try {
      FileInputStream inStreamCands = new FileInputStream(candStringsFileName);
      BufferedReader inFileCands =
          new BufferedReader(new InputStreamReader(inStreamCands, "utf8"));

      FileInputStream inStreamIndices = new FileInputStream(candIndicesFileName);
      BufferedReader inFileIndices =
          new BufferedReader(new InputStreamReader(inStreamIndices, "utf8"));

      PrintWriter outFile = new PrintWriter(outputFileName);

      String[] candStrings = new String[maxBatchSize];
      int[] candIndices = new int[maxBatchSize];

      String lineCand = inFileCands.readLine();
      String lineIndex = inFileIndices.readLine();

      while (lineCand != null) {
        int size = 0;
        while (lineCand != null) {
          candStrings[size] = lineCand;
          candIndices[size] = Integer.parseInt(lineIndex);
          ++size; // now size is how many were read for this currnet batch
          if (size == maxBatchSize) break;

          lineCand = inFileCands.readLine();
          lineIndex = inFileIndices.readLine();
        }

        if (size < maxBatchSize) { // last batch, and smaller than maxBatchSize
          String[] candStringsTemp = new String[size];
          int[] candIndicesTemp = new int[size];
          for (int d = 0; d < size; ++d) {
            candStringsTemp[d] = candStrings[d];
            candIndicesTemp[d] = candIndices[d];
          }
          candStrings = candStringsTemp;
          candIndices = candIndicesTemp;
        }

        int[][] ss = suffStats(candStrings, candIndices);
        for (int d = 0; d < size; ++d) {
          StringBuilder statsStr = new StringBuilder();

          for (int s = 0; s < suffStatsCount - 1; ++s) {
            statsStr.append(ss[d][s]).append(" ");
          }
          statsStr.append(ss[d][suffStatsCount - 1]);

          outFile.println(statsStr);
        }

        lineCand = inFileCands.readLine();
        lineIndex = inFileIndices.readLine();
      }

      inFileCands.close();
      inFileIndices.close();
      outFile.close();

    } catch (IOException e) {
      throw new RuntimeException("IOException in EvaluationMetric.createSuffStatsFile(...): "
          + e.getMessage(), e);
    }

  }

  public void printDetailedScore(String[] topCandStr, boolean oneLiner) {
    int[] stats = suffStats(topCandStr);
    printDetailedScore_fromStats(stats, oneLiner);
  }

  public double score(int[][] stats) {
    // returns an average of document scores (aka the document-level score, as opposed to
    // corpus-level score)
    // stats[][] is indexed [doc][s]

    double retVal = 0.0;
    for (int doc = 0; doc < numDocuments; ++doc) {
      retVal += score(stats[doc]);
    }
    return retVal / numDocuments;
  }

  public double score(int[][] stats, int firstRank, int lastRank) {
    // returns an average of document scores, restricted to the documents
    // ranked firstRank-lastRank, inclusive (ranks are 1-indexed, even though the docs are
    // 0-indexed)

    double[] scores = docScores(stats);

    Arrays.sort(scores);
    // sorts into ascending order

    double retVal = 0.0;

    if (toBeMinimized) {
      // scores[0] is rank 1, scores[numDocuments-1] is rank numDocuments
      // => scores[j] is rank j+1
      // => rank r is scores[r-1]
      for (int j = firstRank - 1; j < lastRank; ++j) {
        retVal += scores[j];
      }
    } else {
      // scores[numDocuments-1] is rank 1, scores[0] is rank numDocuments
      // => scores[j] is rank numDocuments-j
      // => rank r is scores[numDocuments-r]
      for (int j = numDocuments - firstRank; j >= numDocuments - lastRank; --j) {
        retVal += scores[j];
      }
    }

    return retVal / (lastRank - firstRank + 1);

  }

  public double[] docScores(int[][] stats) {
    // returns an array of document scores
    // stats[][] is indexed [doc][s]

    double[] scores = new double[numDocuments];
    for (int doc = 0; doc < numDocuments; ++doc) {
      scores[doc] = score(stats[doc]);
    }
    return scores;
  }

  public void printDetailedScore_fromStats(int[][] stats, String[] docNames) {
    // prints individual document scores
    // stats[][] is indexed [doc][s]

    for (int doc = 0; doc < numDocuments; ++doc) {
      if (docNames == null) {
        System.out.print("Document #" + doc + ": ");
      } else {
        System.out.print(docNames[doc] + ": ");
      }
      printDetailedScore_fromStats(stats[doc], true);
    }
  }

  /* abstract (=> also non-static) methods */
  protected abstract void initialize();

  public abstract double bestPossibleScore();

  public abstract double worstPossibleScore();

  public abstract int[] suffStats(String candStr, int i);

  public abstract double score(int[] stats);

  public abstract void printDetailedScore_fromStats(int[] stats, boolean oneLiner);
}


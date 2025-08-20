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
package org.apache.joshua.adagrad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.metrics.EvaluationMetric;

// this class implements the AdaGrad algorithm
public class Optimizer {
    public Optimizer(Vector<String>_output, boolean[] _isOptimizable, double[] _initialLambda,
      HashMap<String, String>[] _feat_hash, HashMap<String, String>[] _stats_hash) {
    output = _output; // (not used for now)
    isOptimizable = _isOptimizable;
    initialLambda = _initialLambda; // initial weights array
    paramDim = initialLambda.length - 1;
    initialLambda = _initialLambda;
    feat_hash = _feat_hash; // feature hash table
    stats_hash = _stats_hash; // suff. stats hash table
    finalLambda = new double[initialLambda.length];
	    System.arraycopy(initialLambda, 0, finalLambda, 0, finalLambda.length);
  }

  //run AdaGrad for one epoch
  public double[] runOptimizer() {
      List<Integer> sents = new ArrayList<>();
      for( int i = 0; i < sentNum; ++i )
	  sents.add(i);
      double[] avgLambda = new double[initialLambda.length]; //only needed if averaging is required
      for( int i = 0; i < initialLambda.length; ++i )
	  avgLambda[i] = 0;
      for ( int iter = 0; iter < adagradIter; ++iter ) {
	  System.arraycopy(finalLambda, 1, initialLambda, 1, paramDim);
    	  if(needShuffle)
	      Collections.shuffle(sents);

	  double oraMetric, oraScore, predMetric, predScore;
	  double[] oraPredScore = new double[4];
	  double loss = 0;
	  double diff = 0;
	  double sumMetricScore = 0;
	  double sumModelScore = 0;
	  String oraFeat = "";
	  String predFeat = "";
	  String[] oraPredFeat = new String[2];
	  String[] vecOraFeat;
	  String[] vecPredFeat;
	  String[] featInfo;
	  int numBatch = 0;
	  int numUpdate = 0;
	  Iterator<Integer> it;
	  Integer diffFeatId;

	  //update weights
	  Integer s;
	  int sentCount = 0;
	  double prevLambda = 0;
	  double diffFeatVal = 0;
	  double oldVal = 0;
	  double gdStep = 0;
	  double Hii = 0;
	  double gradiiSquare = 0;
	  int lastUpdateTime = 0;
	  HashMap<Integer, Integer> lastUpdate = new HashMap<>();
	  HashMap<Integer, Double> lastVal = new HashMap<>();
	  HashMap<Integer, Double> H = new HashMap<>();
	  while( sentCount < sentNum ) {
	      loss = 0;
	      ++numBatch;
	      HashMap<Integer, Double> featDiff = new HashMap<>();
	      for(int b = 0; b < batchSize; ++b ) {
		  //find out oracle and prediction
		  s = sents.get(sentCount);
		  findOraPred(s, oraPredScore, oraPredFeat, finalLambda, featScale);

		  //the model scores here are already scaled in findOraPred
		  oraMetric = oraPredScore[0];
		  oraScore = oraPredScore[1];
		  predMetric = oraPredScore[2];
		  predScore = oraPredScore[3];
		  oraFeat = oraPredFeat[0];
		  predFeat = oraPredFeat[1];

		  //update the scale
		  if(needScale) { //otherwise featscale remains 1.0
		      sumMetricScore += Math.abs(oraMetric + predMetric);
		      //restore the original model score
		      sumModelScore += Math.abs(oraScore + predScore) / featScale;

		      if(sumModelScore/sumMetricScore > scoreRatio)
			  featScale = sumMetricScore/sumModelScore;
		  }
		  // processedSent++;

		  vecOraFeat = oraFeat.split("\\s+");
		  vecPredFeat = predFeat.split("\\s+");

		  //accumulate difference feature vector
		  if ( b == 0 ) {
			  for (String aVecOraFeat : vecOraFeat) {
				  featInfo = aVecOraFeat.split("=");
				  diffFeatId = Integer.parseInt(featInfo[0]);
				  featDiff.put(diffFeatId, Double.parseDouble(featInfo[1]));
			  }
			  for (String aVecPredFeat : vecPredFeat) {
				  featInfo = aVecPredFeat.split("=");
				  diffFeatId = Integer.parseInt(featInfo[0]);
				  if (featDiff.containsKey(diffFeatId)) { //overlapping features
					  diff = featDiff.get(diffFeatId) - Double.parseDouble(featInfo[1]);
					  if (Math.abs(diff) > 1e-20)
						  featDiff.put(diffFeatId, diff);
					  else
						  featDiff.remove(diffFeatId);
				  } else //features only firing in the 2nd feature vector
					  featDiff.put(diffFeatId, -1.0 * Double.parseDouble(featInfo[1]));
			  }
		  } else {
			  for (String aVecOraFeat : vecOraFeat) {
				  featInfo = aVecOraFeat.split("=");
				  diffFeatId = Integer.parseInt(featInfo[0]);
				  if (featDiff.containsKey(diffFeatId)) { //overlapping features
					  diff = featDiff.get(diffFeatId) + Double.parseDouble(featInfo[1]);
					  if (Math.abs(diff) > 1e-20)
						  featDiff.put(diffFeatId, diff);
					  else
						  featDiff.remove(diffFeatId);
				  } else //features only firing in the new oracle feature vector
					  featDiff.put(diffFeatId, Double.parseDouble(featInfo[1]));
			  }
			  for (String aVecPredFeat : vecPredFeat) {
				  featInfo = aVecPredFeat.split("=");
				  diffFeatId = Integer.parseInt(featInfo[0]);
				  if (featDiff.containsKey(diffFeatId)) { //overlapping features
					  diff = featDiff.get(diffFeatId) - Double.parseDouble(featInfo[1]);
					  if (Math.abs(diff) > 1e-20)
						  featDiff.put(diffFeatId, diff);
					  else
						  featDiff.remove(diffFeatId);
				  } else //features only firing in the new prediction feature vector
					  featDiff.put(diffFeatId, -1.0 * Double.parseDouble(featInfo[1]));
			  }
		  }

		  //remember the model scores here are already scaled
		  double singleLoss = evalMetric.getToBeMinimized() ?
		      (predMetric-oraMetric) - (oraScore-predScore)/featScale:
		      (oraMetric-predMetric) - (oraScore-predScore)/featScale;
		  if(singleLoss > 0)
		      loss += singleLoss;
		  ++sentCount;
		  if( sentCount >= sentNum ) {
		      break;
		  }
	      } //for(int b : batchSize)

	      //System.out.println("\n\n"+sentCount+":");

	      if( loss > 0 ) {
	      //if(true) {
		  ++numUpdate;
		  //update weights (see Duchi'11, Eq.23. For l1-reg, use lazy update)
		  Set<Integer> diffFeatSet = featDiff.keySet();
		  it = diffFeatSet.iterator();
		  while(it.hasNext()) { //note these are all non-zero gradients!
		      diffFeatId = it.next();
		      diffFeatVal = -1.0 * featDiff.get(diffFeatId); //gradient
		      if( regularization > 0 ) {
			  lastUpdateTime =
			      lastUpdate.get(diffFeatId) == null ? 0 : lastUpdate.get(diffFeatId);
			  if( lastUpdateTime < numUpdate - 1 ) {
			      //haven't been updated (gradient=0) for at least 2 steps
			      //lazy compute prevLambda now
			      oldVal =
				  lastVal.get(diffFeatId) == null ? initialLambda[diffFeatId] : lastVal.get(diffFeatId);
			      Hii =
				  H.get(diffFeatId) == null ? 0 : H.get(diffFeatId);
```java
		    candStr = aCandSet;

    } else { //in all other situations, use normal stats
      for (int j = 0; j < evalMetric.get_suffStatsCount(); j++)
        statVal[j] = Integer.parseInt(statVal_str[j]);
    }

    return evalMetric.score(statVal);
  }

  // from ZMERT
  private void normalizeLambda(double[] origLambda) {
    // private String[] normalizationOptions;
    // How should a lambda[] vector be normalized (before decoding)?
    // nO[0] = 0: no normalization
    // nO[0] = 1: scale so that parameter nO[2] has absolute value nO[1]
    // nO[0] = 2: scale so that the maximum absolute value is nO[1]
    // nO[0] = 3: scale so that the minimum absolute value is nO[1]
    // nO[0] = 4: scale so that the L-nO[1] norm equals nO[2]

    int normalizationMethod = (int) normalizationOptions[0];
    double scalingFactor = 1.0;
    if (normalizationMethod == 0) {
      scalingFactor = 1.0;
    } else if (normalizationMethod == 1) {
      int c = (int) normalizationOptions[2];
      scalingFactor = normalizationOptions[1] / Math.abs(origLambda[c]);
    } else if (normalizationMethod == 2) {
      double maxAbsVal = -1;
      int maxAbsVal_c = 0;
      for (int c = 1; c <= paramDim; ++c) {
        if (Math.abs(origLambda[c]) > maxAbsVal) {
          maxAbsVal = Math.abs(origLambda[c]);
          maxAbsVal_c = c;
        }
      }
      scalingFactor = normalizationOptions[1] / Math.abs(origLambda[maxAbsVal_c]);

    } else if (normalizationMethod == 3) {
      double minAbsVal = PosInf;
      int minAbsVal_c = 0;

      for (int c = 1; c <= paramDim; ++c) {
        if (Math.abs(origLambda[c]) < minAbsVal) {
          minAbsVal = Math.abs(origLambda[c]);
          minAbsVal_c = c;
        }
      }
      scalingFactor = normalizationOptions[1] / Math.abs(origLambda[minAbsVal_c]);

    } else if (normalizationMethod == 4) {
      double pow = normalizationOptions[1];
      double norm = L_norm(origLambda, pow);
      scalingFactor = normalizationOptions[2] / norm;
    }

    for (int c = 1; c <= paramDim; ++c) {
      origLambda[c] *= scalingFactor;
    }
  }

  // from ZMERT
  private double L_norm(double[] A, double pow) {
    // calculates the L-pow norm of A[]
    // NOTE: this calculation ignores A[0]
    double sum = 0.0;
    for (int i = 1; i < A.length; ++i)
      sum += Math.pow(Math.abs(A[i]), pow);

    return Math.pow(sum, 1 / pow);
  }

  public static double getScale()
  {
    return featScale;
  }

  public static void initBleuHistory(int sentNum, int statCount)
  {
    bleuHistory = new double[sentNum][statCount];
    for(int i=0; i<sentNum; i++) {
      for(int j=0; j<statCount; j++) {
        bleuHistory[i][j] = 0.0;
      }
    }
  }

  public double getMetricScore()
  {
      return finalMetricScore;
  }

  private final Vector<String> output;
  private double[] initialLambda;
  private final double[] finalLambda;
  private double finalMetricScore;
  private final HashMap<String, String>[] feat_hash;
  private final HashMap<String, String>[] stats_hash;
  private final int paramDim;
  private final boolean[] isOptimizable;
  public static int sentNum;
  public static int adagradIter; //AdaGrad internal iterations
  public static int oraSelectMode;
  public static int predSelectMode;
  public static int batchSize;
  public static int regularization;
  public static boolean needShuffle;
  public static boolean needScale;
  public static double scoreRatio;
  public static boolean needAvg;
  public static boolean usePseudoBleu;
  public static double featScale = 1.0; //scale the features in order to make the model score comparable with metric score
                                            //updates in each epoch if necessary
  public static double eta;
  public static double lam;
  public static double R; //corpus decay(used only when pseudo corpus is used to compute BLEU)
  public static EvaluationMetric evalMetric;
  public static double[] normalizationOptions;
  public static double[][] bleuHistory;

  private final static double NegInf = (-1.0 / 0.0);
  private final static double PosInf = (+1.0 / 0.0);
}
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
package org.apache.joshua.pro;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.metrics.EvaluationMetric;

// this class implements the PRO tuning method
public class Optimizer {
    public Optimizer(long _seed, boolean[] _isOptimizable, Vector<String> _output, double[] _initialLambda,
      HashMap<String, String>[] _feat_hash, HashMap<String, String>[] _stats_hash,
      EvaluationMetric _evalMetric, int _Tau, int _Xi, double _metricDiff,
      double[] _normalizationOptions, String _classifierAlg, String[] _classifierParam) {
    sentNum = _feat_hash.length; // total number of training sentences
    output = _output; // (not used for now)
    initialLambda = _initialLambda;
    isOptimizable = _isOptimizable;
    paramDim = initialLambda.length - 1;
    feat_hash = _feat_hash; // feature hash table
    stats_hash = _stats_hash; // suff. stats hash table
    evalMetric = _evalMetric; // evaluation metric
    Tau = _Tau; // param Tau in PRO
    Xi = _Xi; // param Xi in PRO
    metricDiff = _metricDiff; // threshold for sampling acceptance
    normalizationOptions = _normalizationOptions; // weight normalization option
    randgen = new Random(_seed); // random number generator
    classifierAlg = _classifierAlg; // classification algorithm
    classifierParam = _classifierParam; // params for the specified classifier
  }

  public double[] run_Optimizer() {
    // sampling from all candidates
    Vector<String> allSamples = process_Params();

    try {
      // create classifier object from the given class name string
      ClassifierInterface myClassifier =
          (ClassifierInterface) Class.forName(classifierAlg).newInstance();
      System.out.println("Total training samples(class +1 & class -1): " + allSamples.size());

      // set classifier parameters
      myClassifier.setClassifierParam(classifierParam);
      //run classifier
      double[] finalLambda = myClassifier.runClassifier(allSamples, initialLambda, paramDim);
      normalizeLambda(finalLambda);
      //parameters that are not optimizable are assigned with initial values
      for ( int i = 1; i < isOptimizable.length; ++i ) {
	  if ( !isOptimizable[i] )
	      finalLambda[i] = initialLambda[i];
      }

      double initMetricScore = computeCorpusMetricScore(initialLambda); // compute the initial
                                                                        // corpus-level metric score
      finalMetricScore = computeCorpusMetricScore(finalLambda); // compute the final
                                                                       // corpus-level metric score

      // for( int i=0; i<finalLambda.length; i++ ) System.out.print(finalLambda[i]+" ");
      // System.out.println(); System.exit(0);

      // prepare the printing info
      // int numParamToPrint = 0;
      // String result = "";
      // numParamToPrint = paramDim > 10 ? 10 : paramDim; // how many parameters to print
      // result = paramDim > 10 ? "Final lambda (first 10): {" : "Final lambda: {";
      
      // for (int i = 1; i <= numParamToPrint; i++)
      //     result += String.format("%.4f", finalLambda[i]) + " ";
```java
            StringBuilder featDiffBuilder = new StringBuilder();
            StringBuilder negFeatDiffBuilder = new StringBuilder();
	for (Integer id : feat_diff.keySet()) {
            featDiffBuilder.append(id).append(":").append(feat_diff.get(id)).append(" ");
            negFeatDiffBuilder.append(id).append(":").append(-1.0 * Double.parseDouble(feat_diff.get(id))).append(" ");
	}
        featDiff = featDiffBuilder.toString();
        neg_featDiff = negFeatDiffBuilder.toString();

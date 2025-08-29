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
package org.apache.joshua.decoder.hypergraph;

import java.util.HashMap;


/**
 * to use the functions here, one need to extend the class to provide a way to calculate the
 * transitionLogP based on feature set
 * 
 * @author Zhifei Li, zhifei.work@gmail.com
 * @version $LastChangedDate$
 */

// TODO: currently assume log semiring, need to generalize to other semiring
// already implement both max-product and sum-product algortithms for log-semiring
// Note: this class requires the correctness of transitionLogP of each hyperedge, which itself may
// require the correctness of bestDerivationLogP at each item

public abstract class DefaultInsideOutside {
  /**
   * Two operations: add and multi add: different hyperedges lead to a specific item multi: prob of
   * a derivation is a multi of all constituents
   */
  int ADD_MODE = 0; // 0: sum; 1: viterbi-min, 2: viterbi-max
  final int LOG_SEMIRING = 1;
  int SEMIRING = LOG_SEMIRING; // default is in log; or real, or logic
  double zeroInSemiring = Double.NEGATIVE_INFINITY;// log-domain
  double oneInSemiring = 0;// log-domain
  double scalingFactor; // try to scale the original distribution: smooth or winner-take-all

  private final HashMap<HGNode, Double> tblInsideProb = new HashMap<>();// remember inside
                                                                        // prob of each
                                                                        // item:
  private final HashMap<HGNode, Double> tblOutsideProb = new HashMap<>();// remember
                                                                          // outside prob
                                                                          // of each item
  double normalizationConstant = oneInSemiring;

  /**
   * for each item, remember how many deductions pointering to me, this is needed for outside
   * estimation during outside estimation, an item will recursive call its deductions to do
   * outside-estimation only after it itself is done with outside estimation, this is necessary
   * because the outside estimation of the items under its deductions require the item's outside
   * value
   */
  private final HashMap<HGNode, Integer> tblNumParentDeductions = new HashMap<>();

  private HashMap<HGNode, Integer> tblForSanityCheck = null;

  // get feature-set specific **log probability** for each hyperedge
  protected abstract double getHyperedgeLogProb(HyperEdge dt, HGNode parent_it);

  protected double getHyperedgeLogProb(HyperEdge dt, HGNode parent_it, double scaling_factor) {
    return getHyperedgeLogProb(dt, parent_it) * scaling_factor;
  }

  // the results are stored in tbl_inside_prob and tbl_outside_prob
  public void runInsideOutside(HyperGraph hg, int add_mode, int semiring, double scaling_factor_) {// add_mode|||
                                                                                                   // 0:
                                                                                                   // sum;
                                                                                                   // 1:
                                                                                                   // viterbi-min,
                                                                                                   // 2:
                                                                                                   // viterbi-max

    setupSemiring(semiring, add_mode);
    scalingFactor = scaling_factor_;

    // System.out.println("outside estimation");
    insideEstimationHg(hg);
    // System.out.println("inside estimation");
    outsideEstimationHg(hg);
    normalizationConstant = tblInsideProb.get(hg.goalNode);
    System.out.println("normalization constant is " + normalizationConstant);
    tblNumParentDeductions.clear();
    sanityCheckHG(hg);
  }

  // to save memory, external class should call this method
  public void clearState() {
    tblNumParentDeductions.clear();
    tblInsideProb.clear();
    tblOutsideProb.clear();
  }

  // ######### use of inside-outside probs ##########################
  // this is the logZ where Z is the sum[ exp( log prob ) ]
  public double getLogNormalizationConstant() {
    return normalizationConstant;
  }

  // this is the log of expected/posterior prob (i.e., LogP, where P is the posterior probability),
  // without normalization
  public double getEdgeUnormalizedPosteriorLogProb(HyperEdge dt, HGNode parent) {
    // ### outside of parent
    double outside = tblOutsideProb.get(parent);

    // ### get inside prob of all my ant-items
    double inside = oneInSemiring;
    if (dt.getTailNodes() != null) {
      for (HGNode ant_it : dt.getTailNodes())
        inside = multiInSemiring(inside, tblInsideProb.get(ant_it));
    }

    // ### add deduction/rule specific prob
    double merit = multiInSemiring(inside, outside);
    merit = multiInSemiring(merit, getHyperedgeLogProb(dt, parent, this.scalingFactor));

    return merit;
  }

  // normalized probabily in [0,1]
  public double getEdgePosteriorProb(HyperEdge dt, HGNode parent) {
    if (SEMIRING == LOG_SEMIRING) {
      double res =
          Math.exp((getEdgeUnormalizedPosteriorLogProb(dt, parent) - getLogNormalizationConstant()));
      if (res < 0.0 - 1e-2 || res > 1.0 + 1e-2) {
        throw new RuntimeException("res is not within [0,1], must be wrong value: " + res);
      }
      return res;
    } else {
      throw new RuntimeException("not implemented");
    }
  }

  // this is the log of expected/posterior prob (i.e., LogP, where P is the posterior probability),
  // without normalization
  public double getNodeUnnormalizedPosteriorLogProb(HGNode node) {
    // ### outside of parent
    double inside = tblInsideProb.get(node);
    double outside = tblOutsideProb.get(node);
    return multiInSemiring(inside, outside);
  }


  // normalized probabily in [0,1]
  public double getNodePosteriorProb(HGNode node) {
    if (SEMIRING == LOG_SEMIRING) {
      double res =
          Math.exp((getNodeUnnormalizedPosteriorLogProb(node) - getLogNormalizationConstant()));
      if (res < 0.0 - 1e-2 || res > 1.0 + 1e-2) {
        throw new RuntimeException("res is not within [0,1], must be wrong value: " + res);
      }
      return res;
    } else {
      throw new RuntimeException("not implemented");
    }
  }

  /*
   * Originally, to see if the sum of the posterior probabilities of all the hyperedges sum to one
   * However, this won't work! The sum should be greater than 1.
   */
  public void sanityCheckHG(HyperGraph hg) {
    tblForSanityCheck = new HashMap<>();
    // System.out.println("num_dts: " + hg.goal_item.l_deductions.size());
    sanityCheckItem(hg.goalNode);
    System.out.println("survied sanity check!!!!");
  }

  private void sanityCheckItem(HGNode it) {
    if (tblForSanityCheck.containsKey(it)) return;
    tblForSanityCheck.put(it, 1);
    double prob_sum = 0;
    // ### recursive call on each deduction
    for (HyperEdge dt : it.hyperedges) {
      prob_sum += getEdgePosteriorProb(dt, it);
      sanityCheckDeduction(dt);// deduction-specifc operation
    }
    double supposed_sum = getNodePosteriorProb(it);
    if (Math.abs(prob_sum - supposed_sum) > 1e-3) {
      throw new RuntimeException("prob_sum=" + prob_sum + "; supposed_sum=" + supposed_sum
          + "; sanity check fail!!!!");
    }
    // ### item-specific operation
  }

  private void sanityCheckDeduction(HyperEdge dt) {
    // ### recursive call on each ant item
    if (null != dt.getTailNodes()) {
      dt.getTailNodes().forEach(this::sanityCheckItem);
    }

    // ### deduction-specific operation

  }

  // ################## end use of inside-outside probs



  // ############ bottomn-up insdide estimation ##########################
  private void insideEstimationHg(HyperGraph hg) {
    tblInsideProb.clear();
    tblNumParentDeductions.clear();
    insideEstimationItem(hg.goalNode);
  }

  private double insideEstimationItem(HGNode it) {
    // ### get number of deductions that point to me
    Integer numCalled = tblNumParentDeductions.get(it);
    if (null == numCalled) {
      tblNumParentDeductions.put(it, 1);
    } else {
      tblNumParentDeductions.put(it, numCalled + 1);
    }

    if (tblInsideProb.containsKey(it)) {
      return tblInsideProb.get(it);
    }
    double insideProb = zeroInSemiring;

    // ### recursive call on each deduction
    for (HyperEdge dt : it.hyperedges) {
      double vDt = insideEstimationDeduction(dt, it);// deduction-specifc operation
      insideProb = addInSemiring(insideProb, vDt);
    }
    // ### item-specific operation, but all the prob should be factored into each deduction

    tblInsideProb.put(it, insideProb);
    return insideProb;
  }

  private double insideEstimationDeduction(HyperEdge dt, HGNode parent_item) {
    double insideProb = oneInSemiring;
    // ### recursive call on each ant item
    if (dt.getTailNodes() != null) for (HGNode ant_it : dt.getTailNodes()) {
      double vItem = insideEstimationItem(ant_it);
      insideProb = multiInSemiring(insideProb, vItem);
    }

    // ### deduction operation
    double deductProb = getHyperedgeLogProb(dt, parent_item, this.scalingFactor);// feature-set
                                                                                   // specific
    insideProb = multiInSemiring(insideProb, deductProb);
    return insideProb;
  }

  // ########### end inside estimation

  // ############ top-downn outside estimation ##########################

  private void outsideEstimationHg(HyperGraph hg) {
    tblOutsideProb.clear();
    tblOutsideProb.put(hg.goalNode, oneInSemiring);// initialize
    for (HyperEdge dt : hg.goalNode.hyperedges)
      outsideEstimationDeduction(dt, hg.goalNode);
  }

  private void outsideEstimationItem(HGNode curIt, HGNode upperItem, HyperEdge parentDt,
      double parentDeductProb) {
    Integer numCalled = tblNumParentDeductions.get(curIt);
    if (null == numCalled || 0 == numCalled) {
      throw new RuntimeException("un-expected call, must be wrong");
    }
    tblNumParentDeductions.put(curIt, numCalled - 1);

    double oldOutsideProb = zeroInSemiring;
    if (tblOutsideProb.containsKey(curIt)) {
      oldOutsideProb = tblOutsideProb.get(curIt);
    }

    double additionalOutsideProb = oneInSemiring;

    // ### add parent deduction prob
    additionalOutsideProb = multiInSemiring(additionalOutsideProb, parentDeductProb);

    // ### sibing specifc
    if (parentDt.getTailNodes() != null && parentDt.getTailNodes().size() > 1)
      for (HGNode antIt : parentDt.getTailNodes()) {
        if (antIt != curIt) {
          double insideProbItem = tblInsideProb.get(antIt);// inside prob
          additionalOutsideProb = multiInSemiring(additionalOutsideProb, insideProbItem);
        }
      }

    // ### upper item
    double outsideProbItem = tblOutsideProb.get(upperItem);// outside prob
    additionalOutsideProb = multiInSemiring(additionalOutsideProb, outsideProbItem);

    // #### add to old prob
    additionalOutsideProb = addInSemiring(additionalOutsideProb, oldOutsideProb);

    tblOutsideProb.put(curIt, additionalOutsideProb);

    // ### recursive call on each deduction
    if (numCalled - 1 <= 0) {// i am done
      for (HyperEdge dt : curIt.hyperedges) {
        // TODO: potentially, we can collect the feature expection in each hyperedge here, to avoid
        // another pass of the hypergraph to get the counts
        outsideEstimationDeduction(dt, curIt);
      }
    }
  }


  private void outsideEstimationDeduction(HyperEdge dt, HGNode parent_item) {
    // we do not need to outside prob if no ant items
    if (dt.getTailNodes() != null) {
      // ### deduction specific prob
      double deductionProb = getHyperedgeLogProb(dt, parent_item, this.scalingFactor);// feature-set
                                                                                        // specific

      // ### recursive call on each ant item
      for (HGNode antIt : dt.getTailNodes()) {
        outsideEstimationItem(antIt, parent_item, dt, deductionProb);
      }
    }
  }

  // ########### end outside estimation



  // ############ common ##########################
  // BUG: replace integer pseudo-enum with a real Java enum
  // BUG: use a Semiring class instead of all this?
  private void setupSemiring(int semiring, int add_mode) {
    ADD_MODE = add_mode;
    SEMIRING = semiring;
    if (SEMIRING == LOG_SEMIRING) {
      if (ADD_MODE == 0) { // sum
        zeroInSemiring = Double.NEGATIVE_INFINITY;
        oneInSemiring = 0;
      } else if (ADD_MODE == 1) { // viter-min
        zeroInSemiring = Double.POSITIVE_INFINITY;
        oneInSemiring = 0;
      } else if (ADD_MODE == 2) { // viter-max
        zeroInSemiring = Double.NEGATIVE_INFINITY;
        oneInSemiring = 0;
      } else {
        throw new RuntimeException("invalid add mode");
      }
    } else {
      throw new RuntimeException("un-supported semiring");
    }
  }

  private double multiInSemiring(double x, double y) {
    if (SEMIRING == LOG_SEMIRING) {
      return multiInLogSemiring(x, y);
    } else {
      throw new RuntimeException("un-supported semiring");
    }
  }

  private double addInSemiring(double x, double y) {
    if (SEMIRING == LOG_SEMIRING) {
      return addInLogSemiring(x, y);
    } else {
      throw new RuntimeException("un-supported semiring");
    }
  }

  // AND
  private double multiInLogSemiring(double x, double y) { // value is Log prob
    return x + y;
  }


  // OR: return Math.log(Math.exp(x) + Math.exp(y));
  // BUG: Replace ADD_MODE pseudo-enum with a real Java enum
  private double addInLogSemiring(double x, double y) { // prevent under-flow
    if (ADD_MODE == 0) { // sum
      if (x == Double.NEGATIVE_INFINITY) { // if y is also n-infinity, then return n-infinity
        return y;
      }
      if (y == Double.NEGATIVE_INFINITY) {
        return x;
      }

      if (y <= x) {
        return x + Math.log(1 + Math.exp(y - x));
      } else {
        return y + Math.log(1 + Math.exp(x - y));
      }
    } else if (ADD_MODE == 1) { // viter-min
      return (x <= y ? x : y);
    } else if (ADD_MODE == 2) { // viter-max
      return (x >= y ? x : y);
    } else {
      throw new RuntimeException("invalid add mode");
    }
  }
  // ############ end common #####################

}


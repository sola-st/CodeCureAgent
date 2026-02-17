package org.uma.jmetal.problem.multiobjective.cre;

import org.uma.jmetal.problem.doubleproblem.impl.AbstractDoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

import java.util.List;

/**
 * Class representing problem CRE32. Source: Ryoji Tanabe and Hisao Ishibuchi, An easy-to-use
 * real-world multi-objective optimization problem suite, Applied Soft Computing, Vol. 89, pp.
 * 106078 (2020). DOI: https://doi.org/10.1016/j.asoc.2020.106078
 *
 * @author Antonio J. Nebro
 */
@SuppressWarnings("serial")
public class CRE32 extends AbstractDoubleProblem {

  /** Constructor */
  public CRE32() {
    setNumberOfVariables(6);
    setNumberOfObjectives(3);
    setNumberOfConstraints(9);
    setName("CRE32");

    List<Double> lowerLimit = List.of(150.0, 20.0, 13.0, 10.0, 14.0, 0.63);
    List<Double> upperLimit = List.of(274.32, 32.31, 25.0, 11.71, 18.0, 0.75);

    setVariableBounds(lowerLimit, upperLimit);
  }

  /** Evaluate() method */
  @Override
  public DoubleSolution evaluate(DoubleSolution solution) {
    double xL = solution.getVariable(0);
    double xB = solution.getVariable(1);
    double xD = solution.getVariable(2);
    double xT = solution.getVariable(3);
    double xVk = solution.getVariable(4);
    double xCb = solution.getVariable(5);

    double displacement = 1.025 * xL * xB * xT * xCb;
    double v = 0.5144 * xVk;
    double g = 9.8065;
    double fn = v / Math.pow(g * xL, 0.5);
    double a = (4977.06 * xCb * xCb) - (8105.61 * xCb) + 4456.51;
    double b = (-10847.2 * xCb * xCb) + (12817.0 * xCb) - 6960.32;

    double power = (Math.pow(displacement, 2.0/3.0) * Math.pow(xVk, 3.0)) / (a + (b * fn));
    double outfitWeight = 1.0 * Math.pow(xL , 0.8) * Math.pow(xB , 0.6) * Math.pow(xD, 0.3) * Math.pow(xCb, 0.1);
    double steelWeight = 0.034 * Math.pow(xL ,1.7) * Math.pow(xB ,0.7) * Math.pow(xD ,0.4) * Math.pow(xCb ,0.5);
    double machineryWeight = 0.17 * Math.pow(power, 0.9);
    double lightShipWeight = steelWeight + outfitWeight + machineryWeight;

    double shipCost = 1.3 * ((2000.0 * Math.pow(steelWeight, 0.85))  + (3500.0 * outfitWeight) + (2400.0 * Math.pow(power, 0.8)));
    double capitalCosts = 0.2 * shipCost;

    double dwt = displacement - lightShipWeight;

    double runningCosts = 40000.0 * Math.pow(dwt, 0.3);

    double roundTripMiles = 5000.0;
    double seaDays = (roundTripMiles / 24.0) * xVk;
    double handlingRate = 8000.0;

    double dailyConsumption = ((0.19 * power * 24.0) / 1000.0) + 0.2;
    double fuelPrice = 100.0;
    double fuelCost = 1.05 * dailyConsumption * seaDays * fuelPrice;
    double portCost = 6.3 * Math.pow(dwt, 0.8);

    double fuelCarried = dailyConsumption * (seaDays + 5.0);
    double miscellaneousDwt = 2.0 * Math.pow(dwt, 0.5);

    double cargoDwt = dwt - fuelCarried - miscellaneousDwt;
    double portDays = 2.0 * ((cargoDwt / handlingRate) + 0.5);
    double rtpa = 350.0 / (seaDays + portDays);

    double voyageCosts = (fuelCost + portCost) * rtpa;
    double annualCosts = capitalCosts + runningCosts + voyageCosts;
    double annualCargo = cargoDwt * rtpa;

    solution.setObjective(0, annualCosts / annualCargo);
    solution.setObjective(1, lightShipWeight);
    solution.setObjective(2, -annualCargo);

    evaluateConstraints(solution, dwt, fn);

    return solution;
  }

  /** EvaluateConstraints() method */
  public void evaluateConstraints(DoubleSolution solution, double dwt, double fn) {
    double[] constraint = new double[this.getNumberOfConstraints()];

    double xL = solution.getVariable(0);
    double xB = solution.getVariable(1);
    double xD = solution.getVariable(2);
    double xT = solution.getVariable(3);
    double xVk = solution.getVariable(4);
    double xCb = solution.getVariable(5);

    constraint[0] = (xL / xB) - 6.0;
    constraint[1] = -(xL / xD) + 15.0;
    constraint[2] =  -(xL / xT) + 19.0;
    constraint[3] = 0.45 * Math.pow(dwt, 0.31) - xT;
    constraint[4] = 0.7 * xD + 0.7 - xT;
    constraint[5] = 50000.0 - dwt;
    constraint[6] = dwt - 3000.0;
    constraint[7] = 0.32 - fn;

    double kb = 0.53 * xT;
    double bmt = ((0.085 * xCb - 0.002) * xB * xB) / (xT * xCb);
    double kg = 1.0 + 0.52 * xD;
    constraint[8] = (kb + bmt - kg) - (0.07 * xB);

    for (int i = 0; i < getNumberOfConstraints(); i++) {
      if (constraint[i] < 0.0) {
        constraint[i] = -constraint[i];
      } else {
        constraint[i] = 0;
      }
    }

    for (int i = 0; i < getNumberOfConstraints(); i++) {
      solution.setConstraint(i, constraint[i]);
    }
  }
}
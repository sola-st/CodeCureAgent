```java
//
// Special Session on Real-Parameter Optimization at CEC-05
// Edinburgh, UK, 2-5 Sept. 2005
//
// Organizers:
//	Prof. Kalyanmoy Deb
//		deb@iitk.ac.in
//		http://www.iitk.ac.in/kangal/deb.htm
//	A/Prof. P. N. Suganthan
//		epnsugan@ntu.edu.sg
//		http://www.ntu.edu.sg/home/EPNSugan
//
// Java version of the org.uma.test functions
//
// Matlab reference code
//	http://www.ntu.edu.sg/home/EPNSugan
//
// Java version developer:
//	Assistant Prof. Ying-ping Chen
//		Department of Computer Science
//		National Chiao Tung University
//		HsinChu City, Taiwan
//		ypchen@csie.nctu.edu.tw
//		http://www.csie.nctu.edu.tw/~ypchen/
//
// Typical use of the org.uma.test functions in the Benchmark:
//
//		// Create a Benchmark object
// 		Benchmark theBenchmark = new Benchmark();
//		// Use the factory function call to create a org.uma.test function object
//		//		org.uma.test function 3 with 50 dimension
//		//		the object class is "TestFunc"
//		TestFunc aTestFunc = theBenchmark.testFunctionFactory(3, 50);
//		// Invoke the function with x
//		double experimentoutput = aTestFunc.f(x);
//
// Version 0.90
//		Currently, this version cannot handle any numbers of dimensions.
//		It cannot generate the shifted global optima and rotation matrices
//		that are not provided with the Matlab reference code.
//		It can handle all cases whose data files are provided with
//		the Matlab reference code.
// Version 0.91
//		Revised according to the Matlab reference code and the PDF document
//		dated March 8, 2005.
//
package org.uma.jmetal.problem.singleobjective.cec2005competitioncode;

import org.uma.jmetal.util.errorchecking.JMetalException;

public class F24RotatedHybridComposition4 extends TestFunc {

  // Fixed (class) parameters
  static final public String FUNCTION_NAME = "Rotated Hybrid Composition Function 4";
  static final public String DEFAULT_FILE_DATA = Benchmark.CEC2005SUPPORTDATADIRECTORY + "/hybrid_func4_data.txt";
  static final public String DEFAULT_FILE_MX_PREFIX = Benchmark.CEC2005SUPPORTDATADIRECTORY + "/hybrid_func4_M_D";
  static final public String DEFAULT_FILE_MX_SUFFIX = ".txt";

  // Number of functions
  static final public int NUM_FUNC = 10;

  private final MyHCJob theJob = new MyHCJob();

  // Shifted global optimum
  private final double[][] mO;
  private final double[][][] mM;
  private final double[] mSigma = {
    2.0, 2.0, 2.0, 2.0, 2.0,
    2.0, 2.0, 2.0, 2.0, 2.0
  };
  private final double[] mLambda = {
    10.0, 5.0 / 20.0, 1.0, 5.0 / 32.0, 1.0,
    5.0 / 100.0, 5.0 / 50.0, 1.0, 5.0 / 100.0, 5.0 / 100.0
  };
  private final double[] mFuncBiases = {
    0.0, 100.0, 200.0, 300.0, 400.0,
    500.0, 600.0, 700.0, 800.0, 900.0
  };
  private final double[] mTestPoint;
  private final double[] mTestPointM;
  private final double[] mFmax;

  // In order to avoid excessive memory allocation,
  // a fixed memory buffer is allocated for each function object.
  private double[] mW;
  private double[][] mZ;
  private double[][] mZM;

  // Constructors
  public F24RotatedHybridComposition4(int dimension, double bias) throws JMetalException {
    this(dimension, bias, DEFAULT_FILE_DATA,
      DEFAULT_FILE_MX_PREFIX + dimension + DEFAULT_FILE_MX_SUFFIX);
  }

  public F24RotatedHybridComposition4(int dimension, double bias, String file_data, String file_m) throws
      JMetalException {
    super(dimension, bias, FUNCTION_NAME);

    // Note: dimension starts from 0
    mO = new double[NUM_FUNC][mDimension];
    mM = new double[NUM_FUNC][mDimension][mDimension];

    mTestPoint = new double[mDimension];
    mTestPointM = new double[mDimension];
    mFmax = new double[NUM_FUNC];

    mW = new double[NUM_FUNC];
    mZ = new double[NUM_FUNC][mDimension];
    mZM = new double[NUM_FUNC][mDimension];

    // Load the shifted global optimum
    Benchmark.loadMatrixFromFile(file_data, NUM_FUNC, mDimension, mO);
    // Load the matrix
    Benchmark.loadNMatrixFromFile(file_m, NUM_FUNC, mDimension, mDimension, mM);

    // Initialize the hybrid composition job object
    theJob.numberOfBasicFunctions = NUM_FUNC;
    theJob.numberOfDimensions = mDimension;
    theJob.C = 2000.0;
    theJob.sigma = mSigma;
    theJob.biases = mFuncBiases;
    theJob.lambda = mLambda;
    theJob.shiftGlobalOptimum = mO;
    theJob.linearTransformationMatrix = mM;
    theJob.w = mW;
    theJob.z = mZ;
    theJob.zM = mZM;
    // Calculate/estimate the fmax for all the functions involved
    for (int i = 0; i < NUM_FUNC; i++) {
      for (int j = 0; j < mDimension; j++) {
        mTestPoint[j] = (5.0 / mLambda[i]);
      }
      Benchmark.rotate(mTestPointM, mTestPoint, mM[i]);
      mFmax[i] = Math.abs(theJob.basicFunc(i, mTestPointM));
    }
    theJob.fmax = mFmax;
  }

  // Function body
  public double f(double[] x) throws JMetalException {

    double result = 0.0;

    result = Benchmark.hybrid_composition(x, theJob);

    result += mBias;

    return (result);
  }


  private class MyHCJob extends HCJob {
    public double basicFunc(int func_no, double[] x) throws JMetalException {
      double result = 0.0;
      // This part is according to Matlab reference code
      switch (func_no) {
        case 0:
          result = Benchmark.weierstrass(x);
          break;
        case 1:
          result = Benchmark.EScafferF6(x);
          break;
        case 2:
          result = Benchmark.F8F2(x);
          break;
        case 3:
          result = Benchmark.ackley(x);
          break;
        case 4:
          result = Benchmark.rastrigin(x);
          break;
        case 5:
          result = Benchmark.griewank(x);
          break;
        case 6:
          result = Benchmark.EScafferF6NonCont(x);
          break;
        case 7:
          result = Benchmark.rastriginNonCont(x);
          break;
        case 8:
          result = Benchmark.elliptic(x);
          break;
        case 9:
          result = Benchmark.sphere_noise(x);
          break;
        default:
          throw new JMetalException("func_no is out of range.");
      }
      return (result);
    }
  }
}

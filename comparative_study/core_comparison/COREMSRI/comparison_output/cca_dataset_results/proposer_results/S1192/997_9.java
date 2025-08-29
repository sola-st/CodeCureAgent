```java
package org.locationtech.jtstest.cmd;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;

import junit.framework.TestCase;

public class JTSOpCmdTest extends TestCase {
  private boolean isVerbose = true;

  private static final String ERR_FILE_NOT_FOUND = JTSOpRunner.ERR_FILE_NOT_FOUND;
  private static final String ERR_FUNCTION_NOT_FOUND = JTSOpRunner.ERR_FUNCTION_NOT_FOUND;
  private static final String ERR_WRONG_ARG_COUNT = JTSOpRunner.ERR_WRONG_ARG_COUNT;
  private static final String ERR_INVALID_ARG_PARAM = JTSOpCmd.ERR_INVALID_ARG_PARAM;
  private static final String ERR_REQUIRED_A = JTSOpRunner.ERR_REQUIRED_A;
  private static final String ERR_REQUIRED_B = JTSOpRunner.ERR_REQUIRED_B;
  private static final String ERR_PARSE_GEOM = JTSOpRunner.ERR_PARSE_GEOM;

  private static final String POINT_0_0 = "POINT(0 0)";
  private static final String LINESTRING_1_1_2_2 = "LINESTRING ( 1 1, 2 2)";
  private static final String LINESTRING_1_0_2_0 = "LINESTRING ( 1 0, 2 0 )";
  private static final String LINESTRING_0_0_10_10 = "LINESTRING(0 0, 10 10)";
  private static final String LINESTRING_0_10_10_0 = "LINESTRING(0 10, 10 0)";
  private static final String MULTILINESTRING_0_0_10_10_100_100_110_110 = "MULTILINESTRING((0 0, 10 10), (100 100, 110 110))";
  private static final String MULTIPOINT_0_0_0_1 = "MULTIPOINT((0 0), (0 1))";
  private static final String MULTIPOINT_9_9_8_8 = "MULTIPOINT((9 9), (8 8))";
  private static final String MULTILINESTRING_0_0_5_5_10_0_15_5 = "MULTILINESTRING((0 0, 5 5), (10 0, 15 5))";
  private static final String MULTIPOINT_1_1_11_1 = "MULTIPOINT((1 1), (11 1))";
  private static final String POINT_EMPTY = "POINT EMPTY";
  private static final String LINESTRING_EMPTY = "LINESTRING EMPTY";

  private static final String TEST_BUFFER = "Buffer.buffer";
  private static final String TEST_OVERLAY_UNION = "Overlay.union";
  private static final String TEST_OVERLAY_INTERSECTION = "Overlay.intersection";
  private static final String TEST_ENVELOPE = "envelope";
  private static final String TEST_LENGTH = "length";
  private static final String TEST_CREATE_RANDOM_POINTS = "CreateRandomShape.randomPoints";
  private static final String TEST_DISTANCE_NEAREST_POINTS = "Distance.nearestPoints";
  private static final String TEST_POLYGONIZE = "Polygonize.polygonize";
  private static final String TEST_CONSTRUCTION_BOUNDARY = "Construction.boundary";

  private static final String FORMAT_WKT = "wkt";
  private static final String FORMAT_TXT = "txt";
  private static final String FORMAT_GEOJSON = "geojson";
  private static final String FORMAT_SVG = "svg";
  private static final String FORMAT_GML = "gml";
  private static final String FORMAT_WKB = "wkb";

  private static final String STDIN = "stdin";

  public JTSOpCmdTest(String Name_) {
    super(Name_);
  }

  public static void main(String[] args) {
    String[] testCaseName = {JTSOpCmdTest.class.getName()};
    junit.textui.TestRunner.main(testCaseName);
  }
  
  public void testErrorFileNotFoundA() {
    runCmdError( args("-a", "missing.wkt"), 
        ERR_FILE_NOT_FOUND );
  }
  
  public void testErrorFileNotFoundB() {
    runCmdError( args("-b", "missing.wkt"), 
        ERR_FILE_NOT_FOUND );
  }
  
  public void testErrorFunctioNotFound() {
    runCmdError( args("-a", "POINT ( 1 1 )", "buffer" ),
        ERR_FUNCTION_NOT_FOUND );
  }
  
  public void testErrorMissingArgBuffer() {
    runCmdError( args("-a", "POINT ( 1 1 )", TEST_BUFFER ),
        ERR_WRONG_ARG_COUNT );
  }
  
  public void testErrorbadMultiArgsNoRParen() {
    runCmdError( args("-a", "POINT ( 1 1 )", TEST_BUFFER, "(1,2,3" ),
        ERR_INVALID_ARG_PARAM );
  }
  
  public void testErrorbadMultiArgsNoLParen() {
    runCmdError( args("-a", "POINT ( 1 1 )", TEST_BUFFER, "1,2,3)" ),
        ERR_INVALID_ARG_PARAM );
  }
  
  public void testErrorMissingGeomABuffer() {
    runCmdError( args(TEST_BUFFER, "10" ),
        ERR_REQUIRED_A );
  }
  
  /*
   // Missing B check is disabled for now
  public void testErrorMissingGeomBUnion() {
    runCmdError( args("-a", "POINT ( 1 1 )", TEST_OVERLAY_UNION ),
        ERR_REQUIRED_B );
  }
  */
  
  public void testErrorMissingGeomAUnion() {
    runCmdError( args("-b", "POINT ( 1 1 )", TEST_OVERLAY_UNION ),
        ERR_REQUIRED_A );
  }
  //===========================================
  
  public void testOpEnvelope() {
    runCmd( args("-a", LINESTRING_1_1_2_2, "-f", FORMAT_WKT, TEST_ENVELOPE), 
        "POLYGON" );
  }
  
  public void testOpLength() {
    runCmd( args("-a", LINESTRING_1_0_2_0, "-f", FORMAT_TXT, TEST_LENGTH), 
        "1" );
  }
  
  public void testOpUnionLines() {
    runCmd( args("-a", LINESTRING_1_0_2_0, "-b", "LINESTRING ( 2 0, 3 0 )", "-f", FORMAT_WKT, TEST_OVERLAY_UNION ), 
        "MULTILINESTRING ((1 0, 2 0), (2 0, 3 0))" );
  }
  
  public void testOpNoArg() {
    runCmd( args("-f", FORMAT_WKT, TEST_CREATE_RANDOM_POINTS, "10"), 
        "MULTIPOINT" );
  }
  //===========================================

  public void testOpEachA() {
    runCmd( args("-a", MULTILINESTRING_0_0_10_10_100_100_110_110, 
        "-each", "a",
        "-f", FORMAT_WKT, TEST_ENVELOPE), 
        "POLYGON ((0 0, 0 10, 10 10, 10 0, 0 0))\nPOLYGON ((100 100, 100 110, 110 110, 110 100, 100 100))" );
  }

  public void testOpEachAB() {
    runCmd( args(
        "-a", MULTIPOINT_0_0_0_1, 
        "-b", MULTIPOINT_9_9_8_8, 
        "-each", "ab",
        "-f", FORMAT_WKT, TEST_DISTANCE_NEAREST_POINTS), 
        "LINESTRING (0 0, 9 9)\nLINESTRING (0 0, 8 8)\nLINESTRING (0 1, 9 9)\nLINESTRING (0 1, 8 8)" );
  }

  public void testOpEachB() {
    runCmd( args(
        "-a", MULTIPOINT_0_0_0_1, 
        "-b", MULTIPOINT_9_9_8_8, 
        "-each", "b",
        "-f", FORMAT_WKT, TEST_DISTANCE_NEAREST_POINTS ), 
        "LINESTRING (0 1, 9 9)\nLINESTRING (0 1, 8 8)" );
  }

  public void testOpEachAA() {
    runCmd( args(
        "-a", MULTIPOINT_0_0_0_1, 
        "-each", "aa",
        "-f", FORMAT_WKT, TEST_DISTANCE_NEAREST_POINTS), 
        "LINESTRING (0 0, 0 0)\nLINESTRING (0 0, 0 1)\nLINESTRING (0 1, 0 0)\nLINESTRING (0 1, 0 1)" );
  }

  public void testOpEachABIndexed() {
    runCmd( args(
        "-a", MULTILINESTRING_0_0_5_5_10_0_15_5, 
        "-b", MULTIPOINT_1_1_11_1, 
        "-each", "ab",
        "-index",
        "-f", FORMAT_WKT, TEST_DISTANCE_NEAREST_POINTS), 
        "LINESTRING (1 1, 1 1)\nLINESTRING (11 1, 11 1)" );
  }

  public void testOpBufferVals() {
    JTSOpCmd cmd = runCmd( args(
        "-a", POINT_0_0, 
        "-f", FORMAT_WKT, 
        TEST_BUFFER, "val(1,2,3,4)" ), 
        null, null );
    List<Geometry> results = cmd.getResultGeometry();
    assertTrue("Not enough results for arg values",  results.size() == 4 );
    assertEquals("Incorrect summary value for arg values",  computeArea(results), 93.6, 1);
  }

  public void testOpBufferMultiArgParen() {
    JTSOpCmd cmd = runCmd( args(
        "-a", POINT_0_0, 
        "-f", FORMAT_WKT, 
        TEST_BUFFER, "(1,2,3,4)" ), 
        null, null );
    List<Geometry> results = cmd.getResultGeometry();
    assertTrue("Not enough results for arg values",  results.size() == 4 );
    assertEquals("Incorrect summary value for arg values",  computeArea(results), 93.6, 1);
  }

  public void testOpBufferMultiArgNoParen() {
    JTSOpCmd cmd = runCmd( args(
        "-a", POINT_0_0, 
        "-f", FORMAT_WKT, 
        TEST_BUFFER, "1,2,3,4" ), 
        null, null );
    List<Geometry> results = cmd.getResultGeometry();
    assertTrue("Not enough results for arg values",  results.size() == 4 );
    assertEquals("Incorrect summary value for arg values",  computeArea(results), 93.6, 1);
  }

  //----------------------------------------------------------------
  
  public void testSRIDBuffer() throws ParseException {
    JTSOpCmd cmd = runCmd( args(
        "-a", POINT_0_0, 
        "-srid", "4326",
        "-f", FORMAT_WKB, 
        TEST_BUFFER, "1" ), 
        null, null );
    List<Geometry> results = cmd.getResultGeometry();
    assertEquals("Incorrect SRID", 4326, results.get(0).getSRID());
    
    Geometry outGeom = readWKB(cmd.getOutput());
    assertEquals("Incorrect SRID in WKB", 4326, outGeom.getSRID());
  }

  public void testSRIDStdIn() throws ParseException {
    JTSOpCmd cmd = runCmd( args(
        "-a", STDIN, 
        "-srid", "4326",
        "-f", FORMAT_WKB, 
        TEST_BUFFER, "1" ), 
        stdin(POINT_0_0), null );
    List<Geometry> results = cmd.getResultGeometry();
    assertEquals("Incorrect SRID", 4326, results.get(0).getSRID());
    
    Geometry outGeom = readWKB(cmd.getOutput());
    assertEquals("Incorrect SRID in WKB", 4326, outGeom.getSRID());
  }

  public void testSRIDPolygonize() throws ParseException {
    JTSOpCmd cmd = runCmd( args(
        "-a", "MULTILINESTRING ((1 1, 9 9), (9 9, 9 1), (9 1, 1 1), (9 1, 16 9), (9 9, 16 9))", 
        "-srid", "4326",
        "-explode",
        "-f", FORMAT_WKB, 
        "Polygonize.polygonize" ), 
        null, null );
    List<Geometry> results = cmd.getResultGeometry();
    assertEquals("Incorrect SRID", 4326,  results.get(0).getSRID());
    assertEquals("Incorrect SRID", 4326,  results.get(1).getSRID());
    
    String[] output = cmd.getOutputLines();
    for (String out : output) {
      Geometry outGeom = readWKB(out);
      assertEquals("Incorrect SRID in WKB",  outGeom.getSRID(), 4326);
    }
  }


  //----------------------------------------------------------------

  private Geometry readWKB(String wkbHex) throws ParseException {
    byte[] wkb = WKBReader.hexToBytes(wkbHex);
    WKBReader rdr = new WKBReader();
    return rdr.read(wkb);
  }

  public void testExplode() {
    JTSOpCmd cmd = runCmd( args(
        "-a", LINESTRING_0_0_10_10, 
        "-b", LINESTRING_0_10_10_0, 
        "-explode", 
        "-f", FORMAT_WKT, 
        TEST_OVERLAY_UNION ), 
        null, null );
    List<Geometry> results = cmd.getResultGeometry();
    assertEquals("Not enough results for explode",  results.size(), 4 );
  }

  public void testLiteralEmptyLinestring() {
    JTSOpCmd cmd = runCmd( args(
        "-a", LINESTRING_EMPTY, 
        "-f", FORMAT_WKT, 
        TEST_CONSTRUCTION_BOUNDARY ), 
        null, null );
    List<Geometry> results = cmd.getResultGeometry();
    assertEquals("Too many results for operation",  results.size(), 1 );
    assertTrue("Expected empty result",  results.get(0).isEmpty() );
  }

  public void testLiteralEmptyPoint() {
    JTSOpCmd cmd = runCmd( args(
        "-a", POINT_EMPTY, 
        "-f", FORMAT_WKT, 
        TEST_CONSTRUCTION_BOUNDARY ), 
        null, null );
    List<Geometry> results = cmd.getResultGeometry();
    assertEquals("Too many results for operation",  results.size(), 1 );
    assertTrue("Expected empty result",  results.get(0).isEmpty() );
  }

  //===========================================
  
  public void testFormatWKB() {
    runCmd( args("-a", LINESTRING_1_1_2_2, "-f", FORMAT_WKB), 
        "0000000002000000023FF00000000000003FF000000000000040000000000000004000000000000000" );
  }
  
  public void testFormatGeoJSON() {
    runCmd( args("-a", LINESTRING_1_1_2_2, "-f", FORMAT_GEOJSON), 
        "{\"type\":\"LineString\",\"coordinates\":[[1,1],[2,2]]}" );
  }
  
  public void testFormatSVG() {
    runCmd( args("-a", LINESTRING_1_1_2_2, "-f", FORMAT_SVG), 
        "<polyline" );
  }
  
  public void testFormatGML() {
    runCmd( args("-a", LINESTRING_1_1_2_2, "-f", FORMAT_GML), 
        "<gml:LineString>" );
  }
  
  //===========================================

  public void testStdInWKT() {
    runCmd( args("-a", STDIN, "-f", FORMAT_WKT, TEST_ENVELOPE), 
        stdin(LINESTRING_1_1_2_2),
        "POLYGON" );
  }
  
  public void testStdInWKB() {
    runCmd( args("-a", STDIN, "-f", FORMAT_WKT, TEST_ENVELOPE), 
        stdin("000000000200000005405900000000000040590000000000004072C000000000004062C00000000000405900000000000040690000000000004072C00000000000406F40000000000040590000000000004072C00000000000"),
        "POLYGON" );
  }
  
  public void testGeomABStdIn() {
    runCmd( args("-ab", STDIN, "-f", FORMAT_WKT, TEST_OVERLAY_INTERSECTION), 
        stdin("MULTILINESTRING (( 1 1, 3 3), (1 3, 3 1))"),
        "POINT (2 2)" );
  }
  

  public void testErrorStdInBadFormat() {
    runCmdError( args("-a", STDIN, "-f", FORMAT_WKT, TEST_ENVELOPE), 
        stdin("<gml fdlfld >"),
        ERR_PARSE_GEOM );
  }
  
  private String[] args(String ... args) {
    return args;
  }

  private static InputStream stdin(String data) {
    InputStream instr = new ByteArrayInputStream(data.getBytes(Charset.forName("UTF-8")));
    return instr;
  }
  
  private static InputStream stdinFile(String filename) {
    try {
      return new FileInputStream(filename);
    }
    catch (FileNotFoundException ex) {
      throw new RuntimeException("File not found: " + filename);
    }
  }
  
  public void runCmd(String[] args, String expected)
  {    
    run
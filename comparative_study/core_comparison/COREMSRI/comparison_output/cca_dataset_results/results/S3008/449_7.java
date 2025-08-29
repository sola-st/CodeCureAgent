package test.jts;

public class GeometryTestData {

  public static String wktPoint = "POINT (10 10)";
  public static String wktPointEmpty = "POINT EMPTY";
  
  public static String wktLinestring = "LINESTRING (10 10, 20 20, 30 40)";
  public static String wktLinestringEmpty = "LINESTRING EMPTY";
  
  public static String wktLinearring = "LINEARRING (10 10, 20 20, 30 40, 10 10)";
  public static String wktLinearringEmpty = "LINEARRING EMPTY";
  
  public static String wktPoly = "POLYGON ((50 50, 50 150, 150 150, 150 50, 50 50))";
  public static String wktPolyHole = "POLYGON ((50 50, 50 150, 150 150, 150 50, 50 50), (70 130, 130 130, 130 70, 70 70, 70 130))";
  public static String wktPolyHole2 = "POLYGON ((50 50, 50 150, 150 150, 150 50, 50 50), (60 140, 90 140, 90 110, 60 110, 60 140), (110 90, 140 90, 140 60, 110 60, 110 90))";
  public static String wktPolyEmpty = "POLYGON EMPTY";
  
  public static String wktMultipoint = "MULTIPOINT ((10 10), (20 20))";
  public static String wktMultipointSingle = "MULTIPOINT ((10 10))";
  public static String wktMultipointEmpty = "MULTIPOINT EMPTY";
  
  public static String wktMultilinestring = "MULTILINESTRING ((10 10, 20 20), (15 15, 30 15))";
  public static String wktMultilinestringSingle = "MULTILINESTRING ((10 10, 20 20))";
  public static String wktMultilinestringEmpty = "MULTILINESTRING EMPTY";
  
  public static String wktMultipolygon = "MULTIPOLYGON (((10 10, 10 20, 20 20, 20 15, 10 10)), ((60 60, 70 70, 80 60, 60 60)))";
  public static String wktMultipolygonSingle = "MULTIPOLYGON (((10 10, 10 20, 20 20, 20 15, 10 10)))";
  public static String wktMultipolygonEmpty = "MULTIPOLYGON EMPTY";
  
  public static String wktGc = "GEOMETRYCOLLECTION (POLYGON ((100 200, 200 200, 200 100, 100 100, 100 200)), LINESTRING (150 250, 250 250))";
  public static String wktGcAlp = "GEOMETRYCOLLECTION (POLYGON ((100 200, 200 200, 200 100, 100 100, 100 200)), LINESTRING (150 250, 250 250), POINT (1 1))";
  public static String wktGcNested = "GEOMETRYCOLLECTION (LINESTRING (1 1, 2 2), GEOMETRYCOLLECTION (POLYGON ((100 200, 200 200, 200 100, 100 100, 100 200)), LINESTRING (150 250, 250 250), POINT (1 1)))";
  public static String wktGcEmpty = "GEOMETRYCOLLECTION EMPTY";

  public static String[] wktAll = {
      wktPoint, wktPointEmpty,
      wktLinestring, wktLinestringEmpty,
      wktLinearring, wktLinearringEmpty,
      wktPoly, wktPolyHole, wktPolyHole2, wktPolyEmpty,
      wktMultipoint, wktMultipointSingle, wktMultipointEmpty,
      wktMultilinestring, wktMultilinestringSingle, wktMultilinestringEmpty,
      wktMultipolygon, wktMultipolygonSingle, wktMultipolygonEmpty,
      wktGc, wktGcAlp, wktGcNested, wktGcEmpty
  };

}

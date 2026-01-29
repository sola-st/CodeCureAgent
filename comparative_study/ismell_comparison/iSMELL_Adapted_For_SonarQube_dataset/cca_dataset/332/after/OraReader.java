...
    private MultiPoint readMultiPoint(OraGeom oraGeom, int elemIndex) 
    {
      CoordinateSequence seq;
      /**
       * Special handling when GTYPE is MULTIPOINT.
       * In this case all ordinates are read as a single MultiPoint, regardless of elemInfo contents.
       * This is because MultiPoints can be encoded as either a single MULTI elemInfo,
       * or as multiple POINT elemInfos
       */
      if (oraGeom.geomType() == OraGeom.GEOM_TYPE.MULTIPOINT) {
        seq = extractCoords(oraGeom, oraGeom.ordinates);
      }
      else {
        int etype = oraGeom.eType(elemIndex);
        int interpretation = oraGeom.interpretation(elemIndex);
  
        checkOrdinates(oraGeom, elemIndex, "MultiPoint");
        checkETYPE(etype, OraGeom.ETYPE.POINT, "MultiPoint");
        // MultiPoints have a unique interpretation code
        if (! (interpretation >= 1)){
          errorInterpretation(interpretation, "MultiPoint");
        }
        seq = extractCoords(oraGeom, elemIndex);
      }
      return geometryFactory.createMultiPoint(seq);
    }
...
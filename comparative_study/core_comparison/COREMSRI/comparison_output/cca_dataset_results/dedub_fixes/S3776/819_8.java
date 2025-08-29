/*
 Copyright 1995-2018 Esri

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 For additional information, contact:
 Environmental Systems Research Institute, Inc.
 Attn: Contracts Dept
 380 New York Street
 Redlands, California, USA 92373

 email: contracts@esri.com
 */

package com.esri.core.geometry.ogc;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Envelope1D;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryCursor;
import com.esri.core.geometry.GeometryCursorAppend;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.JsonParserReader;
import com.esri.core.geometry.MapGeometry;
import com.esri.core.geometry.MapOGCStructure;
import com.esri.core.geometry.MultiPoint;
import com.esri.core.geometry.NumberUtils;
import com.esri.core.geometry.OGCStructure;
import com.esri.core.geometry.Operator;
import com.esri.core.geometry.OperatorBuffer;
import com.esri.core.geometry.OperatorCentroid2D;
import com.esri.core.geometry.OperatorConvexHull;
import com.esri.core.geometry.OperatorExportToGeoJson;
import com.esri.core.geometry.OperatorExportToWkb;
import com.esri.core.geometry.OperatorFactoryLocal;
import com.esri.core.geometry.OperatorImportFromESRIShape;
import com.esri.core.geometry.OperatorImportFromGeoJson;
import com.esri.core.geometry.OperatorImportFromWkb;
import com.esri.core.geometry.OperatorImportFromWkt;
import com.esri.core.geometry.OperatorIntersection;
import com.esri.core.geometry.OperatorSimplify;
import com.esri.core.geometry.OperatorSimplifyOGC;
import com.esri.core.geometry.OperatorUnion;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Point2D;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SimpleGeometryCursor;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.VertexDescription;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * OGC Simple Feature Access specification v.1.2.1
 * 
 */
public abstract class OGCGeometry {
	public int dimension() {
		return getEsriGeometry().getDimension();
	}

	public int coordinateDimension() {
		int d = 2;
		if (getEsriGeometry().getDescription().hasAttribute(
				VertexDescription.Semantics.M))
			d++;
		if (getEsriGeometry().getDescription().hasAttribute(
				VertexDescription.Semantics.Z))
			d++;

		return d;
	}

	abstract public String geometryType();

	/**
	 * Returns an estimate of this object size in bytes.
	 * <p>
	 * This estimate doesn't include the size of the {@link SpatialReference} object
	 * because instances of {@link SpatialReference} are expected to be shared among
	 * geometry objects.
	 *
	 * @return Returns an estimate of this object size in bytes.
	 */
	public abstract long estimateMemorySize();

	public int SRID() {
		if (esriSR == null)
			return 0;

		return esriSR.getID();
	}

	public OGCGeometry envelope() {
		com.esri.core.geometry.Envelope env = new com.esri.core.geometry.Envelope();
		getEsriGeometry().queryEnvelope(env);
		com.esri.core.geometry.Polygon polygon = new com.esri.core.geometry.Polygon();
		polygon.addEnvelope(env, false);
		return new OGCPolygon(polygon, esriSR);
	}

	public String asText() {
		return GeometryEngine.geometryToWkt(getEsriGeometry(), 0);
	}

	public ByteBuffer asBinary() {
		OperatorExportToWkb op = (OperatorExportToWkb) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.ExportToWkb);
		return op.execute(0, getEsriGeometry(), null);
	}

	public String asGeoJson() {
		OperatorExportToGeoJson op = (OperatorExportToGeoJson) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.ExportToGeoJson);
		return op.execute(esriSR, getEsriGeometry());
	}

	String asGeoJsonImpl(int export_flags) {
		OperatorExportToGeoJson op = (OperatorExportToGeoJson) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ExportToGeoJson);
		return op.execute(export_flags, esriSR, getEsriGeometry());
	}
	
	/**
	 * 
	 * @return Convert to REST JSON.
	 */
	public String asJson() {
		return GeometryEngine.geometryToJson(esriSR, getEsriGeometry());
	}

	
	public boolean isEmpty() {
		return getEsriGeometry().isEmpty();
	}

	public double MinZ() {
		Envelope1D e = getEsriGeometry().queryInterval(
				VertexDescription.Semantics.Z, 0);
		return e.vmin;
	}

	public double MaxZ() {
		Envelope1D e = getEsriGeometry().queryInterval(
				VertexDescription.Semantics.Z, 0);
		return e.vmax;
	}

	public double MinMeasure() {
		Envelope1D e = getEsriGeometry().queryInterval(
				VertexDescription.Semantics.M, 0);
		return e.vmin;
	}

	public double MaxMeasure() {
		Envelope1D e = getEsriGeometry().queryInterval(
				VertexDescription.Semantics.M, 0);
		return e.vmax;
	}

	/**
	 * Returns true if this geometric object has no anomalous geometric points,
	 * such as self intersection or self tangency. See the
	 * "Simple feature access - Part 1" document (OGC 06-103r4) for meaning of
	 * "simple" for each geometry type.
	 * 
	 * The method has O(n log n) complexity when the input geometry is simple.
	 * For non-simple geometries, it terminates immediately when the first issue
	 * is encountered.
	 * 
	 * @return True if geometry is simple and false otherwise.
	 * 
	 * Note: If isSimple is true, then isSimpleRelaxed is true too. 
	 */
	public boolean isSimple() {
		return OperatorSimplifyOGC.local().isSimpleOGC(getEsriGeometry(),
				esriSR, true, null, null);
	}

	/**
	 * Extension method - checks if geometry is simple for Geodatabase.
	 * 
	 * @return Returns true if geometry is simple, false otherwise.
	 * 
	 * Note: If isSimpleRelaxed is true, then isSimple is either true or false. Geodatabase has more relaxed requirements for simple geometries than OGC.
	 */
	public boolean isSimpleRelaxed() {
		OperatorSimplify op = (OperatorSimplify) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Simplify);
		return op.isSimpleAsFeature(getEsriGeometry(), esriSR, true, null, null);
	}

	@Deprecated
	/**
	 * Use makeSimpleRelaxed instead.
	 */
	public OGCGeometry MakeSimpleRelaxed(boolean forceProcessing) {
		return makeSimpleRelaxed(forceProcessing);
	}
	/**
	 * Makes a simple geometry for Geodatabase.
	 * 
	 * @return Returns simplified geometry.
	 * 
	 * Note: isSimpleRelaxed should return true after this operation.
	 */
	public OGCGeometry makeSimpleRelaxed(boolean forceProcessing) {
		OperatorSimplify op = (OperatorSimplify) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Simplify);
		return OGCGeometry.createFromEsriGeometry(
				op.execute(getEsriGeometry(), esriSR, forceProcessing, null),
				esriSR);
	}
	
	/**
	 * Resolves topological issues in this geometry and makes it Simple according to OGC specification.
	 * 
	 * @return Returns simplified geometry.
	 * 
	 * Note: isSimple and isSimpleRelaxed should return true after this operation. 
	 */
	public OGCGeometry makeSimple() {
		return simplifyBunch_(getEsriGeometryCursor());
	}

	public boolean is3D() {
		return getEsriGeometry().getDescription().hasAttribute(
				VertexDescription.Semantics.Z);
	}

	public boolean isMeasured() {
		return getEsriGeometry().getDescription().hasAttribute(
				VertexDescription.Semantics.M);
	}

	abstract public OGCGeometry boundary();

	/**
	 * OGC equals. Performs topological comparison with tolerance.
	 * This is different from equals(Object), that uses exact comparison.
	 */
	public boolean Equals(OGCGeometry another) {
		if (this == another)
			return !isEmpty();
		
		if (another == null)
			return false;
		
		if (another.geometryType() == OGCConcreteGeometryCollection.TYPE) {
			return another.Equals(this);
		}
		
		com.esri.core.geometry.Geometry geom1 = getEsriGeometry();
		com.esri.core.geometry.Geometry geom2 = another.getEsriGeometry();
		return com.esri.core.geometry.GeometryEngine.equals(geom1, geom2,
				getEsriSpatialReference());
	}

	@Deprecated
	public boolean equals(OGCGeometry another) {
		return Equals(another);
	}
	
	public boolean disjoint(OGCGeometry another) {
		if (another.geometryType() == OGCConcreteGeometryCollection.TYPE) {
			return another.disjoint(this);
		}
		
		com.esri.core.geometry.Geometry geom1 = getEsriGeometry();
		com.esri.core.geometry.Geometry geom2 = another.getEsriGeometry();
		return com.esri.core.geometry.GeometryEngine.disjoint(geom1, geom2,
				getEsriSpatialReference());
	}

	public boolean intersects(OGCGeometry another) {
		return !disjoint(another);
	}

	public boolean touches(OGCGeometry another) {
		if (another.geometryType() == OGCConcreteGeometryCollection.TYPE) {
			//TODO
			throw new UnsupportedOperationException();
		}
		
		com.esri.core.geometry.Geometry geom1 = getEsriGeometry();
		com.esri.core.geometry.Geometry geom2 = another.getEsriGeometry();
		return com.esri.core.geometry.GeometryEngine.touches(geom1, geom2,
				getEsriSpatialReference());
	}

	public boolean crosses(OGCGeometry another) {
		if (another.geometryType() == OGCConcreteGeometryCollection.TYPE) {
			//TODO
			throw new UnsupportedOperationException();
		}
		
		com.esri.core.geometry.Geometry geom1 = getEsriGeometry();
		com.esri.core.geometry.Geometry geom2 = another.getEsriGeometry();
		return com.esri.core.geometry.GeometryEngine.crosses(geom1, geom2,
				getEsriSpatialReference());
	}

	public boolean within(OGCGeometry another) {
		return another.contains(this);
	}

	public boolean contains(OGCGeometry another) {
		if (another.geometryType() == OGCConcreteGeometryCollection.TYPE) {
			return new OGCConcreteGeometryCollection(this, esriSR).contains(another);
		}
		
		com.esri.core.geometry.Geometry geom1 = getEsriGeometry();
		com.esri.core.geometry.Geometry geom2 = another.getEsriGeometry();
		return com.esri.core.geometry.GeometryEngine.contains(geom1, geom2,
				getEsriSpatialReference());
	}

	public boolean overlaps(OGCGeometry another) {
		if (another.geometryType() == OGCConcreteGeometryCollection.TYPE) {
			// TODO
			throw new UnsupportedOperationException();
		}
		
		com.esri.core.geometry.Geometry geom1 = getEsriGeometry();
		com.esri.core.geometry.Geometry geom2 = another.getEsriGeometry();
		return com.esri.core.geometry.GeometryEngine.overlaps(geom1, geom2,
				getEsriSpatialReference());
	}

	public boolean relate(OGCGeometry another, String matrix) {
		if (another.geometryType() == OGCConcreteGeometryCollection.TYPE) {
			//TODO
			throw new UnsupportedOperationException();
		}
		
		com.esri.core.geometry.Geometry geom1 = getEsriGeometry();
		com.esri.core.geometry.Geometry geom2 = another.getEsriGeometry();
		return com.esri.core.geometry.GeometryEngine.relate(geom1, geom2,
				getEsriSpatialReference(), matrix);
	}

	abstract public OGCGeometry locateAlong(double mValue);

	abstract public OGCGeometry locateBetween(double mStart, double mEnd);

	// analysis
	public double distance(OGCGeometry another) {
		if (this == another) {
			return isEmpty() ? Double.NaN : 0;
		}
		
		if (another.geometryType() == OGCConcreteGeometryCollection.TYPE) {
			return another.distance(this);
		}

		com.esri.core.geometry.Geometry geom1 = getEsriGeometry();
		com.esri.core.geometry.Geometry geom2 = another.getEsriGeometry();
		return com.esri.core.geometry.GeometryEngine.distance(geom1, geom2,
				getEsriSpatialReference());
	}

	// This method firstly groups geometries by dimension (points, lines,
	// areas),
	// then simplifies each group such that each group is reduced to a single
	// geometry.
	// As a result there are at most three geometries, each geometry is Simple.
	// Afterwards
	// it produces a single OGCGeometry.
	private OGCGeometry simplifyBunch_(GeometryCursor gc) {
		// Combines geometries into multipoint, polyline, and polygon types,
		// simplifying them and unioning them,
		// then produces OGCGeometry from the result.
		// Can produce OGCConcreteGoemetryCollection
		MultiPoint dstMultiPoint = null;
		ArrayList<Geometry> dstPolylines = new ArrayList<Geometry>();
		ArrayList<Geometry> dstPolygons = new ArrayList<Geometry>();
		for (com.esri.core.geometry.Geometry g = gc.next(); g != null; g = gc
				.next()) {
			switch (g.getType()) {
			case Point:
				if (dstMultiPoint == null)
					dstMultiPoint = new MultiPoint();
				dstMultiPoint.add((Point) g);
				break;
			case MultiPoint:
				if (dstMultiPoint == null)
					dstMultiPoint = new MultiPoint();
				dstMultiPoint.add((MultiPoint) g, 0, -1);
				break;
			case Polyline:
				dstPolylines.add((Polyline) g.copy());
				break;
			case Polygon:
				dstPolygons.add((Polygon) g.copy());
				break;
			default:
				throw new UnsupportedOperationException();
			}
		}

		ArrayList<Geometry> result = new ArrayList<Geometry>(3);
		if (dstMultiPoint != null) {
			Geometry resMP = OperatorSimplifyOGC.local().execute(dstMultiPoint,
					esriSR, true, null);
			result.add(resMP);
		}

		if (dstPolylines.size() > 0) {
			if (dstPolylines.size() == 1) {
				Geometry resMP = OperatorSimplifyOGC.local().execute(
						dstPolylines.get(0), esriSR, true, null);
				result.add(resMP);
			} else {
				GeometryCursor res = OperatorUnion.local().execute(
						new SimpleGeometryCursor(dstPolylines), esriSR, null);
	public static OGCGeometry createFromEsriGeometry(Geometry geom,
			SpatialReference sr, boolean multiType) {
		if (geom == null)
			return null;
		Geometry.Type t = geom.getType();
		switch (t) {
			case Polygon:
				if (!multiType && ((Polygon) geom).getExteriorRingCount() == 1)
					return new OGCPolygon((Polygon) geom, sr);
				else
					return new OGCMultiPolygon((Polygon) geom, sr);
			case Polyline:
				if (!multiType && ((Polyline) geom).getPathCount() == 1)
					return new OGCLineString((Polyline) geom, 0, sr);
				else
					return new OGCMultiLineString((Polyline) geom, sr);
			case MultiPoint:
				if (!multiType && ((MultiPoint) geom).getPointCount() <= 1) {
					if (geom.isEmpty())
						return new OGCPoint(new Point(), sr);
					else
						return new OGCPoint(((MultiPoint) geom).getPoint(0), sr);
				} else
					return new OGCMultiPoint((MultiPoint) geom, sr);
			case Point:
				if (!multiType) {
					return new OGCPoint((Point) geom, sr);
				} else {
					return new OGCMultiPoint((Point) geom, sr);
				}
			case Envelope:
				Polygon p = new Polygon();
				p.addEnvelope((Envelope) geom, false);
				return createFromEsriGeometry(p, sr, multiType);
			default:
				throw new UnsupportedOperationException();
		}
	}

/*
 Copyright 1995-2015 Esri

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
package com.esri.core.geometry;

import java.util.ArrayList;

class RelationalOperations {
	interface Relation {
		static final int contains = 1;
		static final int within = 2;
		static final int equals = 3;
		static final int disjoint = 4;
		static final int touches = 8;
		static final int crosses = 16;
		static final int overlaps = 32;

		static final int unknown = 0;
		static final int intersects = 0x40000000;
	}

	static boolean relate(Geometry geometry_a, Geometry geometry_b,
			SpatialReference sr, int relation, ProgressTracker progress_tracker) {
		int type_a = geometry_a.getType().value();
		int type_b = geometry_b.getType().value();

		// Give preference to the Point vs Envelope, Envelope vs Envelope and
		// Point vs Point realtions:
		if (type_a == Geometry.GeometryType.Envelope) {
			if (type_b == Geometry.GeometryType.Envelope) {
				return relate((Envelope) geometry_a, (Envelope) geometry_b, sr,
						relation, progress_tracker);
			} else if (type_b == Geometry.GeometryType.Point) {
				if (relation == Relation.within)
					relation = Relation.contains;
				else if (relation == Relation.contains)
					relation = Relation.within;

				return relate((Point) geometry_b, (Envelope) geometry_a, sr,
						relation, progress_tracker);
			} else {
				// proceed below
			}
		} else if (type_a == Geometry.GeometryType.Point) {
			if (type_b == Geometry.GeometryType.Envelope) {
				return relate((Point) geometry_a, (Envelope) geometry_b, sr,
						relation, progress_tracker);
			} else if (type_b == Geometry.GeometryType.Point) {
				return relate((Point) geometry_a, (Point) geometry_b, sr,
						relation, progress_tracker);
			} else {
				// proceed below
			}
		} else {
			// proceed below
		}

		if (geometry_a.isEmpty() || geometry_b.isEmpty()) {
			if (relation == Relation.disjoint)
				return true; // Always true

			return false; // Always false
		}

		Envelope2D env1 = new Envelope2D();
		geometry_a.queryEnvelope2D(env1);
		Envelope2D env2 = new Envelope2D();
		geometry_b.queryEnvelope2D(env2);

		Envelope2D envMerged = new Envelope2D();
		envMerged.setCoords(env1);
		envMerged.merge(env2);
		double tolerance = InternalUtils.calculateToleranceFromGeometry(sr,
				envMerged, false);

		if (envelopeDisjointEnvelope_(env1, env2, tolerance, progress_tracker)) {
			if (relation == Relation.disjoint)
				return true;

			return false;
		}

		boolean bRelation = false;

		Geometry _geometry_a;
		Geometry _geometry_b;
		Polyline polyline_a, polyline_b;

		if (MultiPath.isSegment(type_a)) {
			polyline_a = new Polyline(geometry_a.getDescription());
			polyline_a.addSegment((Segment) geometry_a, true);
			_geometry_a = polyline_a;
			type_a = Geometry.GeometryType.Polyline;
		} else {
			_geometry_a = geometry_a;
		}

		if (MultiPath.isSegment(type_b)) {
			polyline_b = new Polyline(geometry_b.getDescription());
			polyline_b.addSegment((Segment) geometry_b, true);
			_geometry_b = polyline_b;
			type_b = Geometry.GeometryType.Polyline;
		} else {
			_geometry_b = geometry_b;
		}

		if (type_a != Geometry.GeometryType.Envelope
				&& type_b != Geometry.GeometryType.Envelope) {
			if (_geometry_a.getDimension() < _geometry_b.getDimension()
					|| (type_a == Geometry.GeometryType.Point && type_b == Geometry.GeometryType.MultiPoint)) {// we
																												// will
																												// switch
																												// the
																												// order
																												// of
																												// the
																												// geometries
																												// below.
				if (relation == Relation.within)
					relation = Relation.contains;
				else if (relation == Relation.contains)
					relation = Relation.within;
			}
		} else {
			if (type_a != Geometry.GeometryType.Polygon
					&& type_b != Geometry.GeometryType.Envelope) { // we will
																	// switch
																	// the order
																	// of the
																	// geometries
																	// below.
				if (relation == Relation.within)
					relation = Relation.contains;
				else if (relation == Relation.contains)
					relation = Relation.within;
			}
		}

		switch (type_a) {
		case Geometry.GeometryType.Polygon:
			switch (type_b) {
			case Geometry.GeometryType.Polygon:
				bRelation = polygonRelatePolygon_((Polygon) (_geometry_a),
						(Polygon) (_geometry_b), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.Polyline:
				bRelation = polygonRelatePolyline_((Polygon) (_geometry_a),
						(Polyline) (_geometry_b), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.Point:
				bRelation = polygonRelatePoint_((Polygon) (_geometry_a),
						(Point) (_geometry_b), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.MultiPoint:
				bRelation = polygonRelateMultiPoint_((Polygon) (_geometry_a),
						(MultiPoint) (_geometry_b), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.Envelope:
				bRelation = polygonRelateEnvelope_((Polygon) (_geometry_a),
						(Envelope) (_geometry_b), tolerance, relation,
						progress_tracker);
				break;

			default:
				break; // warning fix
			}
			break;

		case Geometry.GeometryType.Polyline:
			switch (type_b) {
			case Geometry.GeometryType.Polygon:
				bRelation = polygonRelatePolyline_((Polygon) (_geometry_b),
						(Polyline) (_geometry_a), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.Polyline:
				bRelation = polylineRelatePolyline_((Polyline) (_geometry_a),
						(Polyline) (_geometry_b), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.Point:
				bRelation = polylineRelatePoint_((Polyline) (_geometry_a),
						(Point) (_geometry_b), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.MultiPoint:
				bRelation = polylineRelateMultiPoint_((Polyline) (_geometry_a),
						(MultiPoint) (_geometry_b), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.Envelope:
				bRelation = polylineRelateEnvelope_((Polyline) (_geometry_a),
						(Envelope) (_geometry_b), tolerance, relation,
						progress_tracker);
				break;

			default:
				break; // warning fix
			}
			break;

		case Geometry.GeometryType.Point:
			switch (type_b) {
			case Geometry.GeometryType.Polygon:
				bRelation = polygonRelatePoint_((Polygon) (_geometry_b),
						(Point) (_geometry_a), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.Polyline:
				bRelation = polylineRelatePoint_((Polyline) (_geometry_b),
						(Point) (_geometry_a), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.MultiPoint:
				bRelation = multiPointRelatePoint_((MultiPoint) (_geometry_b),
						(Point) (_geometry_a), tolerance, relation,
						progress_tracker);
				break;

			default:
				break; // warning fix
			}
			break;

		case Geometry.GeometryType.MultiPoint:
			switch (type_b) {
			case Geometry.GeometryType.Polygon:
				bRelation = polygonRelateMultiPoint_((Polygon) (_geometry_b),
						(MultiPoint) (_geometry_a), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.Polyline:
				bRelation = polylineRelateMultiPoint_((Polyline) (_geometry_b),
						(MultiPoint) (_geometry_a), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.MultiPoint:
				bRelation = multiPointRelateMultiPoint_(
						(MultiPoint) (_geometry_a), (MultiPoint) (_geometry_b),
						tolerance, relation, progress_tracker);
				break;

			case Geometry.GeometryType.Point:
				bRelation = multiPointRelatePoint_((MultiPoint) (_geometry_a),
						(Point) (_geometry_b), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.Envelope:
				bRelation = multiPointRelateEnvelope_(
						(MultiPoint) (_geometry_a), (Envelope) (_geometry_b),
						tolerance, relation, progress_tracker);
				break;

			default:
				break; // warning fix
			}
			break;

		case Geometry.GeometryType.Envelope:
			switch (type_b) {
			case Geometry.GeometryType.Polygon:
				bRelation = polygonRelateEnvelope_((Polygon) (_geometry_b),
						(Envelope) (_geometry_a), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.Polyline:
				bRelation = polylineRelateEnvelope_((Polyline) (_geometry_b),
						(Envelope) (_geometry_a), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.MultiPoint:
				bRelation = multiPointRelateEnvelope_(
						(MultiPoint) (_geometry_b), (Envelope) (_geometry_a),
						tolerance, relation, progress_tracker);
				break;

			default:
				break; // warning fix
			}
			break;

		default:
			break; // warning fix
		}

		return bRelation;
	}

	// Computes the necessary 9 intersection relationships of boundary,
	// interior, and exterior of envelope_a vs envelope_b for the given
	// relation.
	private static boolean relate(Envelope envelope_a, Envelope envelope_b,
			SpatialReference sr, int relation, ProgressTracker progress_tracker) {
		if (envelope_a.isEmpty() || envelope_b.isEmpty()) {
			if (relation == Relation.disjoint)
				return true; // Always true

			return false; // Always false
		}

		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D(), env_merged = new Envelope2D();
		envelope_a.queryEnvelope2D(env_a);
		envelope_b.queryEnvelope2D(env_b);
		env_merged.setCoords(env_a);
		env_merged.merge(env_b);

		double tolerance = InternalUtils.calculateToleranceFromGeometry(sr,
				env_merged, false);

		switch (relation) {
		case Relation.disjoint:
			return envelopeDisjointEnvelope_(env_a, env_b, tolerance,
					progress_tracker);

		case Relation.within:
			return envelopeContainsEnvelope_(env_b, env_a, tolerance,
					progress_tracker);

		case Relation.contains:
			return envelopeContainsEnvelope_(env_a, env_b, tolerance,
					progress_tracker);

		case Relation.equals:
			return envelopeEqualsEnvelope_(env_a, env_b, tolerance,
					progress_tracker);

		case Relation.touches:
			return envelopeTouchesEnvelope_(env_a, env_b, tolerance,
					progress_tracker);

		case Relation.overlaps:
			return envelopeOverlapsEnvelope_(env_a, env_b, tolerance,
					progress_tracker);

		case Relation.crosses:
			return envelopeCrossesEnvelope_(env_a, env_b, tolerance,
					progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Computes the necessary 9 intersection relationships of boundary,
	// interior, and exterior of point_a vs envelope_b for the given relation.
	private static boolean relate(Point point_a, Envelope envelope_b,
			SpatialReference sr, int relation, ProgressTracker progress_tracker) {
		if (point_a.isEmpty() || envelope_b.isEmpty()) {
			if (relation == Relation.disjoint)
				return true; // Always true

			return false; // Always false
		}

		Point2D pt_a = point_a.getXY();
		Envelope2D env_b = new Envelope2D(), env_merged = new Envelope2D();
		envelope_b.queryEnvelope2D(env_b);
		env_merged.setCoords(pt_a);
		env_merged.merge(env_b);

		double tolerance = InternalUtils.calculateToleranceFromGeometry(sr,
				env_merged, false);

		switch (relation) {
		case Relation.disjoint:
			return pointDisjointEnvelope_(pt_a, env_b, tolerance,
					progress_tracker);

		case Relation.within:
			return pointWithinEnvelope_(pt_a, env_b, tolerance,
					progress_tracker);

		case Relation.contains:
			return pointContainsEnvelope_(pt_a, env_b, tolerance,
					progress_tracker);

		case Relation.equals:
			return pointEqualsEnvelope_(pt_a, env_b, tolerance,
					progress_tracker);

		case Relation.touches:
			return pointTouchesEnvelope_(pt_a, env_b, tolerance,
					progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Computes the necessary 9 intersection relationships of boundary,
	// interior, and exterior of point_a vs point_b for the given relation.
	private static boolean relate(Point point_a, Point point_b,
			SpatialReference sr, int relation, ProgressTracker progress_tracker) {
		if (point_a.isEmpty() || point_b.isEmpty()) {
			if (relation == Relation.disjoint)
				return true; // Always true

			return false; // Always false
		}

		Point2D pt_a = point_a.getXY();
		Point2D pt_b = point_b.getXY();
		Envelope2D env_merged = new Envelope2D();
		env_merged.setCoords(pt_a);
		env_merged.merge(pt_b);

		double tolerance = InternalUtils.calculateToleranceFromGeometry(sr,
				env_merged, false);

		switch (relation) {
		case Relation.disjoint:
			return pointDisjointPoint_(pt_a, pt_b, tolerance, progress_tracker);

		case Relation.within:
			return pointContainsPoint_(pt_b, pt_a, tolerance, progress_tracker);

		case Relation.contains:
			return pointContainsPoint_(pt_a, pt_b, tolerance, progress_tracker);

		case Relation.equals:
			return pointEqualsPoint_(pt_a, pt_b, tolerance, progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Returns true if the relation holds.
	private static boolean polygonRelatePolygon_(Polygon polygon_a,
			Polygon polygon_b, double tolerance, int relation,
			ProgressTracker progress_tracker) {
		switch (relation) {
		case Relation.disjoint:
			return polygonDisjointPolygon_(polygon_a, polygon_b, tolerance,
					progress_tracker);

		case Relation.within:
			return polygonContainsPolygon_(polygon_b, polygon_a, tolerance,
					progress_tracker);

		case Relation.contains:
			return polygonContainsPolygon_(polygon_a, polygon_b, tolerance,
					progress_tracker);

		case Relation.equals:
			return polygonEqualsPolygon_(polygon_a, polygon_b, tolerance,
					progress_tracker);

		case Relation.touches:
			return polygonTouchesPolygon_(polygon_a, polygon_b, tolerance,
					progress_tracker);

		case Relation.overlaps:
			return polygonOverlapsPolygon_(polygon_a, polygon_b, tolerance,
					progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Returns true if the relation holds.
	private static boolean polygonRelatePolyline_(Polygon polygon_a,
			Polyline polyline_b, double tolerance, int relation,
			ProgressTracker progress_tracker) {
		switch (relation) {
		case Relation.disjoint:
			return polygonDisjointPolyline_(polygon_a, polyline_b, tolerance,
					progress_tracker);

		case Relation.contains:
			return polygonContainsPolyline_(polygon_a, polyline_b, tolerance,
					progress_tracker);

		case Relation.touches:
			return polygonTouchesPolyline_(polygon_a, polyline_b, tolerance,
					progress_tracker);

		case Relation.crosses:
			return polygonCrossesPolyline_(polygon_a, polyline_b, tolerance,
					progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Returns true if the relation holds.
	private static boolean polygonRelatePoint_(Polygon polygon_a,
			Point point_b, double tolerance, int relation,
			ProgressTracker progress_tracker) {
		switch (relation) {
		case Relation.disjoint:
			return polygonDisjointPoint_(polygon_a, point_b, tolerance,
					progress_tracker);

		case Relation.contains:
			return polygonContainsPoint_(polygon_a, point_b, tolerance,
					progress_tracker);

		case Relation.touches:
			return polygonTouchesPoint_(polygon_a, point_b, tolerance,
					progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Returns true if the relation holds
	private static boolean polygonRelateMultiPoint_(Polygon polygon_a,
			MultiPoint multipoint_b, double tolerance, int relation,
			ProgressTracker progress_tracker) {
		switch (relation) {
		case Relation.disjoint:
			return polygonDisjointMultiPoint_(polygon_a, multipoint_b,
					tolerance, true, progress_tracker);

		case Relation.contains:
			return polygonContainsMultiPoint_(polygon_a, multipoint_b,
					tolerance, progress_tracker);

		case Relation.touches:
			return polygonTouchesMultiPoint_(polygon_a, multipoint_b,
					tolerance, progress_tracker);

		case Relation.crosses:
			return polygonCrossesMultiPoint_(polygon_a, multipoint_b,
					tolerance, progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Returns true if the relation holds
	private static boolean polygonRelateEnvelope_(Polygon polygon_a,
			Envelope envelope_b, double tolerance, int relation,
			ProgressTracker progress_tracker) {
		if (polygonDisjointEnvelope_(polygon_a, envelope_b, tolerance,
				progress_tracker)) {
			if (relation == Relation.disjoint)
				return true;

			return false;
		} else if (relation == Relation.disjoint) {
			return false;
		}

		switch (relation) {
		case Relation.within:
			return polygonWithinEnvelope_(polygon_a, envelope_b, tolerance,
					progress_tracker);

		case Relation.contains:
			return polygonContainsEnvelope_(polygon_a, envelope_b, tolerance,
					progress_tracker);

		case Relation.equals:
			return polygonEqualsEnvelope_(polygon_a, envelope_b, tolerance,
					progress_tracker);

		case Relation.touches:
			return polygonTouchesEnvelope_(polygon_a, envelope_b, tolerance,
					progress_tracker);

		case Relation.overlaps:
			return polygonOverlapsEnvelope_(polygon_a, envelope_b, tolerance,
					progress_tracker);

		case Relation.crosses:
			return polygonCrossesEnvelope_(polygon_a, envelope_b, tolerance,
					progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Returns true if the relation holds.
	private static boolean polylineRelatePolyline_(Polyline polyline_a,
			Polyline polyline_b, double tolerance, int relation,
			ProgressTracker progress_tracker) {
		switch (relation) {
		case Relation.disjoint:
			return polylineDisjointPolyline_(polyline_a, polyline_b, tolerance,
					progress_tracker);

		case Relation.within:
			return polylineContainsPolyline_(polyline_b, polyline_a, tolerance,
					progress_tracker);

		case Relation.contains:
			return polylineContainsPolyline_(polyline_a, polyline_b, tolerance,
					progress_tracker);

		case Relation.equals:
			return polylineEqualsPolyline_(polyline_a, polyline_b, tolerance,
					progress_tracker);

		case Relation.touches:
			return polylineTouchesPolyline_(polyline_a, polyline_b, tolerance,
					progress_tracker);

		case Relation.overlaps:
			return polylineOverlapsPolyline_(polyline_a, polyline_b, tolerance,
					progress_tracker);

		case Relation.crosses:
			return polylineCrossesPolyline_(polyline_a, polyline_b, tolerance,
					progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Returns true if the relation holds.
	private static boolean polylineRelatePoint_(Polyline polyline_a,
			Point point_b, double tolerance, int relation,
			ProgressTracker progress_tracker) {
		switch (relation) {
		case Relation.disjoint:
			return polylineDisjointPoint_(polyline_a, point_b, tolerance,
					progress_tracker);

		case Relation.contains:
			return polylineContainsPoint_(polyline_a, point_b, tolerance,
					progress_tracker);

		case Relation.touches:
			return polylineTouchesPoint_(polyline_a, point_b, tolerance,
					progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Returns true if the relation holds.
	private static boolean polylineRelateMultiPoint_(Polyline polyline_a,
			MultiPoint multipoint_b, double tolerance, int relation,
			ProgressTracker progress_tracker) {
		switch (relation) {
		case Relation.disjoint:
			return polylineDisjointMultiPoint_(polyline_a, multipoint_b,
					tolerance, progress_tracker);

		case Relation.contains:
			return polylineContainsMultiPoint_(polyline_a, multipoint_b,
					tolerance, progress_tracker);

		case Relation.touches:
			return polylineTouchesMultiPoint_(polyline_a, multipoint_b,
					tolerance, progress_tracker);

		case Relation.crosses:
			return polylineCrossesMultiPoint_(polyline_a, multipoint_b,
					tolerance, progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Returns true if the relation holds.
	private static boolean polylineRelateEnvelope_(Polyline polyline_a,
			Envelope envelope_b, double tolerance, int relation,
			ProgressTracker progress_tracker) {
		if (polylineDisjointEnvelope_(polyline_a, envelope_b, tolerance,
				progress_tracker)) {
			if (relation == Relation.disjoint)
				return true;

			return false;
		} else if (relation == Relation.disjoint) {
			return false;
		}

		switch (relation) {
		case Relation.within:
			return polylineWithinEnvelope_(polyline_a, envelope_b, tolerance,
					progress_tracker);

		case Relation.contains:
			return polylineContainsEnvelope_(polyline_a, envelope_b, tolerance,
					progress_tracker);

		case Relation.equals:
			return polylineEqualsEnvelope_(polyline_a, envelope_b, tolerance,
					progress_tracker);

		case Relation.touches:
			return polylineTouchesEnvelope_(polyline_a, envelope_b, tolerance,
					progress_tracker);

		case Relation.overlaps:
			return polylineOverlapsEnvelope_(polyline_a, envelope_b, tolerance,
					progress_tracker);

		case Relation.crosses:
			return polylineCrossesEnvelope_(polyline_a, envelope_b, tolerance,
					progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Returns true if the relation holds.
	private static boolean multiPointRelateMultiPoint_(MultiPoint multipoint_a,
			MultiPoint multipoint_b, double tolerance, int relation,
			ProgressTracker progress_tracker) {
		switch (relation) {
		case Relation.disjoint:
			return multiPointDisjointMultiPoint_(multipoint_a, multipoint_b,
					tolerance, progress_tracker);

		case Relation.within:
			return multiPointContainsMultiPoint_(multipoint_b, multipoint_a,
					tolerance, progress_tracker);

		case Relation.contains:
			return multiPointContainsMultiPoint_(multipoint_a, multipoint_b,
					tolerance, progress_tracker);

		case Relation.equals:
			return multiPointEqualsMultiPoint_(multipoint_a, multipoint_b,
					tolerance, progress_tracker);

		case Relation.overlaps:
			return multiPointOverlapsMultiPoint_(multipoint_a, multipoint_b,
					tolerance, progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Returns true if the relation holds.
	private static boolean multiPointRelatePoint_(MultiPoint multipoint_a,
			Point point_b, double tolerance, int relation,
			ProgressTracker progress_tracker) {
		switch (relation) {
		case Relation.disjoint:
			return multiPointDisjointPoint_(multipoint_a, point_b, tolerance,
					progress_tracker);

		case Relation.within:
			return multiPointWithinPoint_(multipoint_a, point_b, tolerance,
					progress_tracker);

		case Relation.contains:
			return multiPointContainsPoint_(multipoint_a, point_b, tolerance,
					progress_tracker);

		case Relation.equals:
			return multiPointEqualsPoint_(multipoint_a, point_b, tolerance,
					progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Returns true if the relation holds.
	private static boolean multiPointRelateEnvelope_(MultiPoint multipoint_a,
			Envelope envelope_b, double tolerance, int relation,
			ProgressTracker progress_tracker) {
		switch (relation) {
		case Relation.disjoint:
			return multiPointDisjointEnvelope_(multipoint_a, envelope_b,
					tolerance, progress_tracker);

		case Relation.within:
			return multiPointWithinEnvelope_(multipoint_a, envelope_b,
					tolerance, progress_tracker);

		case Relation.contains:
			return multiPointContainsEnvelope_(multipoint_a, envelope_b,
					tolerance, progress_tracker);

		case Relation.equals:
			return multiPointEqualsEnvelope_(multipoint_a, envelope_b,
					tolerance, progress_tracker);

		case Relation.touches:
			return multiPointTouchesEnvelope_(multipoint_a, envelope_b,
					tolerance, progress_tracker);

		case Relation.crosses:
			return multiPointCrossesEnvelope_(multipoint_a, envelope_b,
					tolerance, progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// The fix for SonarQube S117 warning:
	// Renamed method parameters scalar_a_0, scalar_a_1, ivertex_b to scalarA0, scalarA1, iVertexB respectively.
	private static boolean linearPathWithinLinearPath_(MultiPath multipathA,
			MultiPath multipathB, double tolerance, boolean bEnforceOrientation) {
		boolean bWithin = true;
		double[] scalarsA = new double[2];
		double[] scalarsB = new double[2];

		int ievent = 0;
		AttributeStreamOfInt32 eventIndices = new AttributeStreamOfInt32(0);
		RelationalOperations relOps = new RelationalOperations();
		OverlapComparer overlapComparer = new OverlapComparer(relOps);
		OverlapEvent overlapEvent;

		Envelope2D env_a = new Envelope2D();
		Envelope2D env_b = new Envelope2D();
		Envelope2D envInter = new Envelope2D();
		multipathA.queryEnvelope2D(env_a);
		multipathB.queryEnvelope2D(env_b);
		env_a.inflate(tolerance, tolerance);
		env_b.inflate(tolerance, tolerance);
		envInter.setCoords(env_a);
		envInter.intersect(env_b);

		SegmentIteratorImpl segIterA = ((MultiPathImpl) multipathA._getImpl())
				.querySegmentIterator();
		SegmentIteratorImpl segIterB = ((MultiPathImpl) multipathB._getImpl())
				.querySegmentIterator();

		QuadTreeImpl qtB = null;
		QuadTreeImpl quadTreeB = null;
        QuadTreeImpl quadTreePathsB = null;

		GeometryAccelerators accel = ((MultiPathImpl) multipathB._getImpl())
				._getAccelerators();

		if (accel != null) {
			quadTreeB = accel.getQuadTree();
            quadTreePathsB = accel.getQuadTreeForPaths();
			if (quadTreeB == null) {
				qtB = InternalUtils.buildQuadTree(
						(MultiPathImpl) multipathB._getImpl(), envInter);
				quadTreeB = qtB;
			}
		} else {
			qtB = InternalUtils.buildQuadTree(
					(MultiPathImpl) multipathB._getImpl(), envInter);
			quadTreeB = qtB;
		}

		QuadTreeImpl.QuadTreeIteratorImpl qtIterB = quadTreeB.getIterator();

        QuadTreeImpl.QuadTreeIteratorImpl qtIterPathsB = null;
        if (quadTreePathsB != null)
            qtIterPathsB = quadTreePathsB.getIterator();

		while (segIterA.nextPath()) {
			while (segIterA.hasNextSegment()) {
				boolean bStringOfSegmentAsCovered = false;

				Segment segmentA = segIterA.nextSegment();
				segmentA.queryEnvelope2D(env_a);

				if (!env_a.isIntersecting(envInter)) {
					return false; // bWithin = false
				}

                if (qtIterPathsB != null) {
                    qtIterPathsB.resetIterator(env_a, tolerance);

                    if (qtIterPathsB.next() == -1) {
                        bWithin = false;
                        return false;
                    }
                }

				double lengthA = segmentA.calculateLength2D();

				qtIterB.resetIterator(segmentA, tolerance);

				for (int elementHandleB = qtIterB.next(); elementHandleB != -1; elementHandleB = qtIterB
						.next()) {
					int vertex_b = quadTreeB.getElement(elementHandleB);
					segIterB.resetToVertex(vertex_b);
					Segment segmentB = segIterB.nextSegment();

					int result = segmentA.intersect(segmentB, null, scalarsA,
							scalarsB, tolerance);

					if (result == 2 && (!bEnforceOrientation || scalarsB[0] <= scalarsB[1])) {
						double scalarA0 = scalarsA[0];
						double scalarA1 = scalarsA[1];
						double scalar_b_0 = scalarsB[0];
						double scalar_b_1 = scalarsB[1];

						// Performance enhancement for nice cases where
						// localization occurs. Increment segIterA as far as we
						// can while the current segmentA is covered.
						if (scalarA0 * lengthA <= tolerance
								&& (1.0 - scalarA1) * lengthA <= tolerance) {
							bStringOfSegmentAsCovered = true;

							ievent = 0;
							eventIndices.resize(0);
							relOps.m_overlap_events.clear();

							int ivertex_a = segIterA.getStartPointIndex();
							boolean bSegmentACovered = true;

							while (bSegmentACovered) {// keep going while the
								// current segmentA is
								// covered.
								if (segIterA.hasNextSegment()) {
									segmentA = segIterA.nextSegment();
									lengthA = segmentA.calculateLength2D();

									result = segmentA.intersect(segmentB, null,
											scalarsA, scalarsB, tolerance);

									if (result == 2 && (!bEnforceOrientation || scalarsB[0] <= scalarsB[1])) {
										scalarA0 = scalarsA[0];
										scalarA1 = scalarsA[1];

										if (scalarA0 * lengthA <= tolerance
												&& (1.0 - scalarA1) * lengthA <= tolerance) {
											ivertex_a = segIterA
													.getStartPointIndex();
											continue;
										}
									}

									if (segIterB.hasNextSegment()) {
										segmentB = segIterB.nextSegment();
										result = segmentA.intersect(segmentB,
												null, scalarsA, scalarsB,
												tolerance);

										if (result == 2 && (!bEnforceOrientation || scalarsB[0] <= scalarsB[1])) {
											scalarA0 = scalarsA[0];
											scalarA1 = scalarsA[1];

											if (scalarA0 * lengthA <= tolerance
													&& (1.0 - scalarA1)
															* lengthA <= tolerance) {
												ivertex_a = segIterA
														.getStartPointIndex();
												continue;
											}
										}
									}
								}

								bSegmentACovered = false;
							}

							if (ivertex_a != segIterA.getStartPointIndex()) {
								segIterA.resetToVertex(ivertex_a);
								segIterA.nextSegment();
							}

							break;
						} else {
							int ivertex_a = segIterA.getStartPointIndex();
							int ipath_a = segIterA.getPathIndex();
							int iVertexB = segIterB.getStartPointIndex();
							int ipath_b = segIterB.getPathIndex();

							overlapEvent = OverlapEvent.construct(ivertex_a,
									ipath_a, scalarA0, scalarA1, iVertexB,
									ipath_b, scalar_b_0, scalar_b_1);
							relOps.m_overlap_events.add(overlapEvent);
							eventIndices.add(eventIndices.size());
						}
					}
				}

				if (bStringOfSegmentAsCovered) {
					continue; // no need to check that segmentA is covered
				}
				if (ievent == relOps.m_overlap_events.size()) {
					return false; // bWithin = false
				}

				if (eventIndices.size() - ievent > 1) {
					eventIndices.Sort(ievent, eventIndices.size(),
							overlapComparer);
				}

				double lastScalar = 0.0;

				for (int i = ievent; i < relOps.m_overlap_events.size(); i++) {
					overlapEvent = relOps.m_overlap_events.get(eventIndices
							.get(i));

					if (overlapEvent.m_scalar_a_0 < lastScalar
							&& overlapEvent.m_scalar_a_1 < lastScalar) {
						continue;
					}

					if (lengthA * (overlapEvent.m_scalar_a_0 - lastScalar) > tolerance) {
						return false; // bWithin = false
					} else {
						lastScalar = overlapEvent.m_scalar_a_1;

						if (lengthA * (1.0 - lastScalar) <= tolerance
								|| lastScalar == 1.0) {
							break;
						}
					}
				}

				if (lengthA * (1.0 - lastScalar) > tolerance) {
					return false; // bWithin = false
				}

				ievent = 0;
				eventIndices.resize(0);
				relOps.m_overlap_events.clear();
			}
		}

		return bWithin;
	}

	// ... rest of code unchanged ...
}
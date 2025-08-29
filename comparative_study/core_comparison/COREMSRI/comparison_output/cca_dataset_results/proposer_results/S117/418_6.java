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
/*
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

import com.esri.core.geometry.VertexDescription.Semantics;

class OperatorExportToGeoJsonCursor extends JsonCursor {
	GeometryCursor m_inputGeometryCursor;
	SpatialReference m_spatialReference;
	int m_index;
	int m_export_flags;

	public OperatorExportToGeoJsonCursor(int export_flags, SpatialReference spatialReference,
			GeometryCursor geometryCursor) {
		m_index = -1;
		if (geometryCursor == null)
			throw new IllegalArgumentException();

		m_export_flags = export_flags;
		m_spatialReference = spatialReference;
		m_inputGeometryCursor = geometryCursor;
	}

	@Override
	public int getID() {
		return m_index;
	}

	@Override
	public String next() {
		Geometry geometry;
		if ((geometry = m_inputGeometryCursor.next()) != null) {
			m_index = m_inputGeometryCursor.getGeometryID();
```java
			return exportToGeoJson(mExportFlags, geometry, mSpatialReference);
		}
		return null;
	}

	// Mirrors wkt
	static String exportToGeoJson(int exportFlags, Geometry geometry, SpatialReference spatialReference) {

		if (geometry == null)
			throw new IllegalArgumentException("");

		JsonWriter jsonWriter = new JsonStringWriter();

		jsonWriter.startObject();

		exportGeometryToGeoJson_(exportFlags, geometry, jsonWriter);

		if ((exportFlags & GeoJsonExportFlags.geoJsonExportSkipCRS) == 0) {
			jsonWriter.addFieldName("crs");
			exportSpatialReference(exportFlags, spatialReference, jsonWriter);
		}

		jsonWriter.endObject();

		return (String) jsonWriter.getJson();
	}

	static String exportSpatialReference(int exportFlags, SpatialReference spatialReference) {
		if (spatialReference == null || (exportFlags & GeoJsonExportFlags.geoJsonExportSkipCRS) != 0)
			throw new IllegalArgumentException("");

		JsonWriter jsonWriter = new JsonStringWriter();
		exportSpatialReference(exportFlags, spatialReference, jsonWriter);

		return (String) jsonWriter.getJson();
	}

	private static void exportGeometryToGeoJson_(int exportFlags, Geometry geometry, JsonWriter jsonWriter) {
		int type = geometry.getType().value();
		switch (type) {
		case Geometry.GeometryType.Polygon:
			exportPolygonToGeoJson_(exportFlags, (Polygon) geometry, jsonWriter);
			return;

		case Geometry.GeometryType.Polyline:
			exportPolylineToGeoJson_(exportFlags, (Polyline) geometry, jsonWriter);
			return;

		case Geometry.GeometryType.MultiPoint:
			exportMultiPointToGeoJson_(exportFlags, (MultiPoint) geometry, jsonWriter);
			return;

		case Geometry.GeometryType.Point:
			exportPointToGeoJson_(exportFlags, (Point) geometry, jsonWriter);
			return;

		case Geometry.GeometryType.Envelope:
			exportEnvelopeToGeoJson_(exportFlags, (Envelope) geometry,
					jsonWriter);
			return;

		default:
			throw new RuntimeException("not implemented for this geometry type");
		}
	}

	private static void exportSpatialReference(int exportFlags, SpatialReference spatialReference,
			JsonWriter jsonWriter) {
		if (spatialReference != null) {
			int wkid = spatialReference.getLatestID();

			if (wkid <= 0)
				throw new GeometryException("invalid call");

			jsonWriter.startObject();

			jsonWriter.addFieldName("type");

			jsonWriter.addValueString("name");

			jsonWriter.addFieldName("properties");
			jsonWriter.startObject();

			jsonWriter.addFieldName("name");

			String authority = ((SpatialReferenceImpl) spatialReference).getAuthority();
			authority = authority.toUpperCase();
			StringBuilder crsIdentifier = new StringBuilder(authority);
			crsIdentifier.append(':');
			crsIdentifier.append(wkid);
			jsonWriter.addValueString(crsIdentifier.toString());

			jsonWriter.endObject();

			jsonWriter.endObject();
		} else {
			jsonWriter.addValueNull();
		}
	}

	// Mirrors wkt
	private static void exportPolygonToGeoJson_(int exportFlags, Polygon polygon, JsonWriter jsonWriter) {
		MultiPathImpl polygonImpl = (MultiPathImpl) (polygon._getImpl());

		if ((exportFlags & GeoJsonExportFlags.geoJsonExportFailIfNotSimple) != 0) {
			int simple = polygonImpl.getIsSimple(0.0);

			if (simple != MultiPathImpl.GeometryXSimple.Strong)
				throw new GeometryException("corrupted geometry");
		}

		int pointCount = polygon.getPointCount();
		int polygonCount = polygonImpl.getOGCPolygonCount();

		if (pointCount > 0 && polygonCount == 0)
			throw new GeometryException("corrupted geometry");

		int precision = 17 - (31 & (exportFlags >> 13));
		boolean bFixedPoint = (GeoJsonExportFlags.geoJsonExportPrecisionFixedPoint & exportFlags) != 0;
		boolean bExportZs = polygonImpl.hasAttribute(VertexDescription.Semantics.Z)
				&& (exportFlags & GeoJsonExportFlags.geoJsonExportStripZs) == 0;
		boolean bExportMs = polygonImpl.hasAttribute(VertexDescription.Semantics.M)
				&& (exportFlags & GeoJsonExportFlags.geoJsonExportStripMs) == 0;

		if (!bExportZs && bExportMs)
			throw new IllegalArgumentException("invalid argument");

		int pathCount = 0;
		AttributeStreamOfDbl position = null;
		AttributeStreamOfDbl zs = null;
		AttributeStreamOfDbl ms = null;
		AttributeStreamOfInt8 pathFlags = null;
		AttributeStreamOfInt32 paths = null;

		if (pointCount > 0) {
			position = (AttributeStreamOfDbl) polygonImpl.getAttributeStreamRef(Semantics.POSITION);
			pathFlags = polygonImpl.getPathFlagsStreamRef();
			paths = polygonImpl.getPathStreamRef();
			pathCount = polygonImpl.getPathCount();

			if (bExportZs) {
				if (polygonImpl._attributeStreamIsAllocated(Semantics.Z))
					zs = (AttributeStreamOfDbl) polygonImpl.getAttributeStreamRef(Semantics.Z);
			}

			if (bExportMs) {
				if (polygonImpl._attributeStreamIsAllocated(Semantics.M))
					ms = (AttributeStreamOfDbl) polygonImpl.getAttributeStreamRef(Semantics.M);
			}
		}

		if ((exportFlags & GeoJsonExportFlags.geoJsonExportPreferMultiGeometry) == 0 && polygonCount <= 1)
			polygonTaggedText_(precision, bFixedPoint, bExportZs, bExportMs, zs, ms, position, paths, pathCount,
					jsonWriter);
		else
			multiPolygonTaggedText_(precision, bFixedPoint, bExportZs, bExportMs, zs, ms, position, pathFlags,
					paths, polygonCount, pathCount, jsonWriter);
	}

	// Mirrors wkt
	private static void exportPolylineToGeoJson_(int exportFlags, Polyline polyline, JsonWriter jsonWriter) {
		MultiPathImpl polylineImpl = (MultiPathImpl) polyline._getImpl();

		int pointCount = polylineImpl.getPointCount();
		int pathCount = polylineImpl.getPathCount();

		if (pointCount > 0 && pathCount == 0)
			throw new GeometryException("corrupted geometry");

		int precision = 17 - (31 & (exportFlags >> 13));
		boolean bFixedPoint = (GeoJsonExportFlags.geoJsonExportPrecisionFixedPoint & exportFlags) != 0;
		boolean bExportZs = polylineImpl.hasAttribute(VertexDescription.Semantics.Z)
				&& (exportFlags & GeoJsonExportFlags.geoJsonExportStripZs) == 0;
		boolean bExportMs = polylineImpl.hasAttribute(VertexDescription.Semantics.M)
				&& (exportFlags & GeoJsonExportFlags.geoJsonExportStripMs) == 0;

		if (!bExportZs && bExportMs)
			throw new IllegalArgumentException("invalid argument");

		AttributeStreamOfDbl position = null;
		AttributeStreamOfDbl zs = null;
		AttributeStreamOfDbl ms = null;
		AttributeStreamOfInt8 pathFlags = null;
		AttributeStreamOfInt32 paths = null;

		if (pointCount > 0) {
			position = (AttributeStreamOfDbl) polylineImpl.getAttributeStreamRef(Semantics.POSITION);
			pathFlags = polylineImpl.getPathFlagsStreamRef();
			paths = polylineImpl.getPathStreamRef();

			if (bExportZs) {
				if (polylineImpl._attributeStreamIsAllocated(Semantics.Z))
					zs = (AttributeStreamOfDbl) polylineImpl.getAttributeStreamRef(Semantics.Z);
			}

			if (bExportMs) {
				if (polylineImpl._attributeStreamIsAllocated(Semantics.M))
					ms = (AttributeStreamOfDbl) polylineImpl.getAttributeStreamRef(Semantics.M);
			}
		}

		if ((exportFlags & GeoJsonExportFlags.geoJsonExportPreferMultiGeometry) == 0 && pathCount <= 1)
			lineStringTaggedText_(precision, bFixedPoint, bExportZs, bExportMs, zs, ms, position, pathFlags, paths,
					jsonWriter);
		else
			multiLineStringTaggedText_(precision, bFixedPoint, bExportZs, bExportMs, zs, ms, position, pathFlags,
					paths, pathCount, jsonWriter);
	}

	// Mirrors wkt
	private static void exportMultiPointToGeoJson_(int exportFlags, MultiPoint multiPoint, JsonWriter jsonWriter) {
		MultiPointImpl multipointImpl = (MultiPointImpl) multiPoint._getImpl();

		int pointCount = multipointImpl.getPointCount();

		int precision = 17 - (31 & (exportFlags >> 13));
		boolean bFixedPoint = (GeoJsonExportFlags.geoJsonExportPrecisionFixedPoint & exportFlags) != 0;
		boolean bExportZs = multipointImpl.hasAttribute(VertexDescription.Semantics.Z)
				&& (exportFlags & GeoJsonExportFlags.geoJsonExportStripZs) == 0;
		boolean bExportMs = multipointImpl.hasAttribute(VertexDescription.Semantics.M)
				&& (exportFlags & GeoJsonExportFlags.geoJsonExportStripMs) == 0;

		if (!bExportZs && bExportMs)
			throw new IllegalArgumentException("invalid argument");

		AttributeStreamOfDbl position = null;
		AttributeStreamOfDbl zs = null;
		AttributeStreamOfDbl ms = null;

		if (pointCount > 0) {
			position = (AttributeStreamOfDbl) multipointImpl.getAttributeStreamRef(Semantics.POSITION);

			if (bExportZs) {
				if (multipointImpl._attributeStreamIsAllocated(Semantics.Z))
					zs = (AttributeStreamOfDbl) multipointImpl.getAttributeStreamRef(Semantics.Z);
			}

			if (bExportMs) {
				if (multipointImpl._attributeStreamIsAllocated(Semantics.M))
					ms = (AttributeStreamOfDbl) multipointImpl.getAttributeStreamRef(Semantics.M);
			}
		}

		multiPointTaggedText_(precision, bFixedPoint, bExportZs, bExportMs, zs, ms, position, pointCount,
				jsonWriter);
	}

	// Mirrors wkt
	private static void exportPointToGeoJson_(int exportFlags, Point point, JsonWriter jsonWriter) {
		int precision = 17 - (31 & (exportFlags >> 13));
		boolean bFixedPoint = (GeoJsonExportFlags.geoJsonExportPrecisionFixedPoint & exportFlags) != 0;
		boolean bExportZs = point.hasAttribute(VertexDescription.Semantics.Z)
				&& (exportFlags & GeoJsonExportFlags.geoJsonExportStripZs) == 0;
		boolean bExportMs = point.hasAttribute(VertexDescription.Semantics.M)
				&& (exportFlags & GeoJsonExportFlags.geoJsonExportStripMs) == 0;

		if (!bExportZs && bExportMs)
			throw new IllegalArgumentException("invalid argument");

		double x = NumberUtils.NaN();
		double y = NumberUtils.NaN();
		double z = NumberUtils.NaN();
		double m = NumberUtils.NaN();

		if (!point.isEmpty()) {
			x = point.getX();
			y = point.getY();

			if (bExportZs)
				z = point.getZ();

			if (bExportMs)
				m = point.getM();
		}

		if ((exportFlags & GeoJsonExportFlags.geoJsonExportPreferMultiGeometry) == 0)
			pointTaggedText_(precision, bFixedPoint, bExportZs, bExportMs, x, y, z, m, jsonWriter);
		else
			multiPointTaggedTextFromPoint_(precision, bFixedPoint, bExportZs, bExportMs, x, y, z, m, jsonWriter);
	}

	// Mirrors wkt
	private static void exportEnvelopeToGeoJson_(int exportFlags, Envelope envelope, JsonWriter jsonWriter) {
		int precision = 17 - (31 & (exportFlags >> 13));
		boolean bFixedPoint = (GeoJsonExportFlags.geoJsonExportPrecisionFixedPoint & exportFlags) != 0;
		boolean bExportZs = envelope.hasAttribute(VertexDescription.Semantics.Z)
				&& (exportFlags & GeoJsonExportFlags.geoJsonExportStripZs) == 0;
		boolean bExportMs = envelope.hasAttribute(VertexDescription.Semantics.M)
				&& (exportFlags & GeoJsonExportFlags.geoJsonExportStripMs) == 0;

		if (!bExportZs && bExportMs)
			throw new IllegalArgumentException("invalid argument");

		double xmin = NumberUtils.NaN();
		double ymin = NumberUtils.NaN();
		double xmax = NumberUtils.NaN();
		double ymax = NumberUtils.NaN();
		double zmin = NumberUtils.NaN();
		double zmax = NumberUtils.NaN();
		double mmin = NumberUtils.NaN();
		double mmax = NumberUtils.NaN();

		if (!envelope.isEmpty()) {
			xmin = envelope.getXMin();
			ymin = envelope.getYMin();
			xmax = envelope.getXMax();
			ymax = envelope.getYMax();

			Envelope1D interval;

			if (bExportZs) {
				interval = envelope.queryInterval(Semantics.Z, 0);
				zmin = interval.vmin;
				zmax = interval.vmax;
			}

			if (bExportMs) {
				interval = envelope.queryInterval(Semantics.M, 0);
				mmin = interval.vmin;
				mmax = interval.vmax;
			}
		}

		if ((exportFlags & GeoJsonExportFlags.geoJsonExportPreferMultiGeometry) == 0)
			polygonTaggedTextFromEnvelope_(precision, bFixedPoint, bExportZs, bExportMs, xmin, ymin, xmax, ymax,
					zmin, zmax, mmin, mmax, jsonWriter);
		else
			multiPolygonTaggedTextFromEnvelope_(precision, bFixedPoint, bExportZs, bExportMs, xmin, ymin, xmax,
					ymax, zmin, zmax, mmin, mmax, jsonWriter);
	}

	// Mirrors wkt
	private static void multiPolygonTaggedText_(int precision, boolean bFixedPoint, boolean bExportZs,
			boolean bExportMs, AttributeStreamOfDbl zs, AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt8 pathFlags, AttributeStreamOfInt32 paths, int polygonCount, int pathCount,
			JsonWriter jsonWriter) {
		jsonWriter.addFieldName("type");
		jsonWriter.addValueString("MultiPolygon");

		jsonWriter.addFieldName("coordinates");

		if (position == null) {
			jsonWriter.startArray();
			jsonWriter.endArray();
			return;
		}

		jsonWriter.startArray();

		multiPolygonText_(precision, bFixedPoint, bExportZs, bExportMs, zs, ms, position, pathFlags, paths,
				polygonCount, pathCount, jsonWriter);

		jsonWriter.endArray();
	}

	// Mirrors wkt
	private static void multiPolygonTaggedTextFromEnvelope_(int precision, boolean bFixedPoint, boolean bExportZs,
			boolean bExportMs, double xmin, double ymin, double xmax, double ymax, double zmin, double zmax,
			double mmin, double mmax, JsonWriter jsonWriter) {
		jsonWriter.addFieldName("type");
		jsonWriter.addValueString("MultiPolygon");

		jsonWriter.addFieldName("coordinates");

		if (NumberUtils.isNaN(xmin)) {
			jsonWriter.startArray();
			jsonWriter.endArray();
			return;
		}

		jsonWriter.startArray();

		writeEnvelopeAsGeoJsonPolygon_(precision, bFixedPoint, bExportZs, bExportMs, xmin, ymin, xmax, ymax, zmin,
				zmax, mmin, mmax, jsonWriter);

		jsonWriter.endArray();
	}

	// Mirrors wkt
	private static void multiLineStringTaggedText_(int precision, boolean bFixedPoint, boolean bExportZs,
			boolean bExportMs, AttributeStreamOfDbl zs, AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt8 pathFlags, AttributeStreamOfInt32 paths, int pathCount, JsonWriter jsonWriter) {
		jsonWriter.addFieldName("type");
		jsonWriter.addValueString("MultiLineString");

		jsonWriter.addFieldName("coordinates");

		if (position == null) {
			jsonWriter.startArray();
			jsonWriter.endArray();
			return;
		}

		jsonWriter.startArray();

		multiLineStringText_(precision, bFixedPoint, bExportZs, bExportMs, zs, ms, position, pathFlags, paths,
				pathCount, jsonWriter);

		jsonWriter.endArray();
	}

	// Mirrors wkt
	private static void multiPointTaggedText_(int precision, boolean bFixedPoint, boolean bExportZs,
			boolean bExportMs, AttributeStreamOfDbl zs, AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			int pointCount, JsonWriter jsonWriter) {
		jsonWriter.addFieldName("type");
		jsonWriter.addValueString("MultiPoint");

		jsonWriter.addFieldName("coordinates");

		if (position == null) {
			jsonWriter.startArray();
			jsonWriter.endArray();

			return;
		}

		lineStringText_(false, false, precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, 0,
				point_count, json_writer);
	}

	// Mirrors wkt
	private static void multiPointTaggedTextFromPoint_(int precision, boolean bFixedPoint, boolean b_export_zs,
			boolean b_export_ms, double x, double y, double z, double m, JsonWriter json_writer) {
		json_writer.addFieldName("type");
		json_writer.addValueString("MultiPoint");

		json_writer.addFieldName("coordinates");

		if (NumberUtils.isNaN(x)) {
			json_writer.startArray();
			json_writer.endArray();
			return;
		}

		json_writer.startArray();

		pointText_(precision, bFixedPoint, b_export_zs, b_export_ms, x, y, z, m, json_writer);

		json_writer.endArray();
	}

	// Mirrors wkt
	private static void polygonTaggedText_(int precision, boolean bFixedPoint, boolean b_export_zs, boolean b_export_ms,
			AttributeStreamOfDbl zs, AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt32 paths, int path_count, JsonWriter json_writer) {
		json_writer.addFieldName("type");
		json_writer.addValueString("Polygon");

		json_writer.addFieldName("coordinates");

		if (position == null) {
			json_writer.startArray();
			json_writer.endArray();
			return;
		}

		polygonText_(precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, paths, 0, path_count,
				json_writer);
	}

	// Mirrors wkt
	private static void polygonTaggedTextFromEnvelope_(int precision, boolean bFixedPoint, boolean b_export_zs,
			boolean b_export_ms, double xmin, double ymin, double xmax, double ymax, double zmin, double zmax,
			double mmin, double mmax, JsonWriter json_writer) {
		json_writer.addFieldName("type");
		json_writer.addValueString("Polygon");

		json_writer.addFieldName("coordinates");

		if (NumberUtils.isNaN(xmin)) {
			json_writer.startArray();
			json_writer.endArray();
			return;
		}

		writeEnvelopeAsGeoJsonPolygon_(precision, bFixedPoint, b_export_zs, b_export_ms, xmin, ymin, xmax, ymax, zmin,
				zmax, mmin, mmax, json_writer);
	}

	// Mirrors wkt
	private static void lineStringTaggedText_(int precision, boolean bFixedPoint, boolean b_export_zs,
			boolean b_export_ms, AttributeStreamOfDbl zs, AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt8 path_flags, AttributeStreamOfInt32 paths, JsonWriter json_writer) {
		json_writer.addFieldName("type");
		json_writer.addValueString("LineString");

		json_writer.addFieldName("coordinates");

		if (position == null) {
			json_writer.startArray();
			json_writer.endArray();
			return;
		}

		boolean b_closed = ((path_flags.read(0) & PathFlags.enumClosed) != 0);

		lineStringText_(false, b_closed, precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, 0,
				paths.read(1), json_writer);
	}

	// Mirrors wkt
	private static void pointTaggedText_(int precision, boolean bFixedPoint, boolean b_export_zs, boolean b_export_ms,
			double x, double y, double z, double m, JsonWriter json_writer) {
		json_writer.addFieldName("type");
		json_writer.addValueString("Point");

		json_writer.addFieldName("coordinates");

		if (NumberUtils.isNaN(x)) {
			json_writer.startArray();
			json_writer.endArray();

			return;
		}

		pointText_(precision, bFixedPoint, b_export_zs, b_export_ms, x, y, z, m, json_writer);
	}

	// Mirrors wkt
	private static void multiPolygonText_(int precision, boolean bFixedPoint, boolean b_export_zs, boolean b_export_ms,
			AttributeStreamOfDbl zs, AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt8 path_flags, AttributeStreamOfInt32 paths, int polygon_count, int path_count,
			JsonWriter json_writer) {
		int polygon_start = 0;
		int polygon_end = 1;

		while (polygon_end < path_count && ((int) path_flags.read(polygon_end) & PathFlags.enumOGCStartPolygon) == 0)
			polygon_end++;

		polygonText_(precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, paths, polygon_start,
				polygon_end, json_writer);

		for (int ipolygon = 1; ipolygon < polygon_count; ipolygon++) {
			polygon_start = polygon_end;
			polygon_end++;

			while (polygon_end < path_count
					&& ((int) path_flags.read(polygon_end) & PathFlags.enumOGCStartPolygon) == 0)
				polygon_end++;

			polygonText_(precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, paths, polygon_start,
					polygon_end, json_writer);
		}
	}

	// Mirrors wkt
	private static void multiLineStringText_(int precision, boolean bFixedPoint, boolean b_export_zs,
			boolean b_export_ms, AttributeStreamOfDbl zs, AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt8 path_flags, AttributeStreamOfInt32 paths, int path_count, JsonWriter json_writer) {
		boolean b_closed = ((path_flags.read(0) & PathFlags.enumClosed) != 0);

		lineStringText_(false, b_closed, precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, 0,
				paths.read(1), json_writer);

		for (int path = 1; path < path_count; path++) {
			b_closed = ((path_flags.read(path) & PathFlags.enumClosed) != 0);

			int istart = paths.read(path);
			int iend = paths.read(path + 1);
			lineStringText_(false, b_closed, precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, istart,
					iend, json_writer);
		}
	}

	// Mirrors wkt
	private static void polygonText_(int precision, boolean bFixedPoint, boolean b_export_zs, boolean b_export_ms,
			AttributeStreamOfDbl zs, AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt32 paths, int polygon_start, int polygon_end, JsonWriter json_writer) {
		json_writer.startArray();

		int istart = paths.read(polygon_start);
		int iend = paths.read(polygon_start + 1);
		lineStringText_(true, true, precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, istart, iend,
				json_writer);

		for (int path = polygon_start + 1; path < polygon_end; path++) {
			istart = paths.read(path);
			iend = paths.read(path + 1);
			lineStringText_(true, true, precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, istart,
					iend, json_writer);
		}

		json_writer.endArray();
	}

	// Mirrors wkt
	private static void lineStringText_(boolean bRing, boolean b_closed, int precision, boolean bFixedPoint,
			boolean b_export_zs, boolean b_export_ms, AttributeStreamOfDbl zs, AttributeStreamOfDbl ms,
			AttributeStreamOfDbl position, int istart, int iend, JsonWriter json_writer) {
		if (istart == iend) {
			json_writer.startArray();
			json_writer.endArray();
			return;
		}

		json_writer.startArray();

		if (bRing) {
			pointText_(precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, istart, json_writer);

			for (int point = iend - 1; point >= istart + 1; point--)
				pointText_(precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, point, json_writer);

			pointText_(precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, istart, json_writer);
		} else {
			for (int point = istart; point < iend - 1; point++)
				pointText_(precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, point, json_writer);

			pointText_(precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, iend - 1, json_writer);

			if (b_closed)
				pointText_(precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, istart, json_writer);
		}

		json_writer.endArray();
	}

	// Mirrors wkt
	private static int pointText_(int precision, boolean bFixedPoint, boolean b_export_zs, boolean b_export_ms,
			double x, double y, double z, double m, JsonWriter json_writer) {

		json_writer.startArray();

		json_writer.addValueDouble(x, precision, bFixedPoint);
		json_writer.addValueDouble(y, precision, bFixedPoint);

		if (b_export_zs)
			json_writer.addValueDouble(z, precision, bFixedPoint);

		if (b_export_ms)
			json_writer.addValueDouble(m, precision, bFixedPoint);

		json_writer.endArray();

		return 1;
	}

	// Mirrors wkt
	private static void pointText_(int precision, boolean bFixedPoint, boolean b_export_zs, boolean b_export_ms,
			AttributeStreamOfDbl zs, AttributeStreamOfDbl ms, AttributeStreamOfDbl position, int point,
			JsonWriter json_writer) {
		double x = position.readAsDbl(2 * point);
		double y = position.readAsDbl(2 * point + 1);
		double z = NumberUtils.NaN();
		double m = NumberUtils.NaN();

		if (b_export_zs)
			z = (zs != null ? zs.readAsDbl(point) : VertexDescription.getDefaultValue(Semantics.Z));

		if (b_export_ms)
			m = (ms != null ? ms.readAsDbl(point) : VertexDescription.getDefaultValue(Semantics.M));

		pointText_(precision, bFixedPoint, b_export_zs, b_export_ms, x, y, z, m, json_writer);
	}

	// Mirrors wkt
	private static void writeEnvelopeAsGeoJsonPolygon_(int precision, boolean bFixedPoint, boolean b_export_zs,
			boolean b_export_ms, double xmin, double ymin, double xmax, double ymax, double zmin, double zmax,
			double mmin, double mmax, JsonWriter json_writer) {
		json_writer.startArray();
		json_writer.startArray();

		json_writer.startArray();
		json_writer.addValueDouble(xmin, precision, bFixedPoint);
		json_writer.addValueDouble(ymin, precision, bFixedPoint);

		if (b_export_zs)
			json_writer.addValueDouble(zmin, precision, bFixedPoint);

		if (b_export_ms)
			json_writer.addValueDouble(mmin, precision, bFixedPoint);

		json_writer.endArray();

		json_writer.startArray();
		json_writer.addValueDouble(xmax, precision, bFixedPoint);
		json_writer.addValueDouble(ymin, precision, bFixedPoint);

		if (b_export_zs)
			json_writer.addValueDouble(zmax, precision, bFixedPoint);

		if (b_export_ms)
			json_writer.addValueDouble(mmax, precision, bFixedPoint);

		json_writer.endArray();

		json_writer.startArray();
		json_writer.addValueDouble(xmax, precision, bFixedPoint);
		json_writer.addValueDouble(ymax, precision, bFixedPoint);

		if (b_export_zs)
			json_writer.addValueDouble(zmin, precision, bFixedPoint);

		if (b_export_ms)
			json_writer.addValueDouble(mmin, precision, bFixedPoint);

		json_writer.endArray();

		json_writer.startArray();
		json_writer.addValueDouble(xmin, precision, bFixedPoint);
		json_writer.addValueDouble(ymax, precision, bFixedPoint);

		if (b_export_zs)
			json_writer.addValueDouble(zmax, precision, bFixedPoint);

		if (b_export_ms)
			json_writer.addValueDouble(mmax, precision, bFixedPoint);

		json_writer.endArray();

		json_writer.startArray();
		json_writer.addValueDouble(xmin, precision, bFixedPoint);
		json_writer.addValueDouble(ymin, precision, bFixedPoint);

		if (b_export_zs)
			json_writer.addValueDouble(zmin, precision, bFixedPoint);

		if (b_export_ms)
			json_writer.addValueDouble(mmin, precision, bFixedPoint);

		json_writer.endArray();

		json_writer.endArray();
		json_writer.endArray();
	}
}
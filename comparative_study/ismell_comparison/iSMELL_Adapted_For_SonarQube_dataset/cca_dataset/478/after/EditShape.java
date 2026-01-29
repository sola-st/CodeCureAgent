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

import com.esri.core.geometry.Geometry.GeometryType;

/**
 * A helper geometry structure that can store MultiPoint, Polyline, Polygon
 * geometries in linked lists. It allows constant time manipulation of geometry
 * vertices.
 */
final class EditShape {
	interface PathFlags_ {
		static final int closedPath = 1;
		static final int exteriorPath = 2;
		static final int ringAreaValid = 4;
	}

	private int m_geometryCount;
	private int m_path_count;
	private int m_point_count;
	private int m_first_geometry;
	private int m_last_geometry;

	private StridedIndexTypeCollection m_vertex_index_list;

	// ****************Vertex Data******************
	private MultiPoint m_vertices_mp; // vertex coordinates are stored here
	// Attribute_stream_of_index_type::SPtr m_indexRemap;
	private MultiPointImpl m_vertices; // Internals of m_vertices_mp
	AttributeStreamOfDbl m_xy_stream; // The xy stream of the m_vertices.
	VertexDescription m_vertex_description;// a shortcut to the vertex
											// description.
	boolean m_b_has_attributes; // a short cut to know if we have something in
								// addition to x and y.

	ArrayList<Segment> m_segments;// may be NULL if all segments a Lines,
									// otherwise contains NULLs for Line
									// segments. Curves are not NULL.
	AttributeStreamOfDbl m_weights;// may be NULL if no weights are provided.
									// NULL weights assumes weight value of 1.
	ArrayList<AttributeStreamOfInt32> m_indices;// user indices are here
	// ****************End Vertex Data**************
	StridedIndexTypeCollection m_path_index_list; // doubly connected list. Path
													// index into the Path Data
													// arrays, Prev path, next
													// path.
	// ******************Path Data******************
	AttributeStreamOfDbl m_path_areas;
	AttributeStreamOfDbl m_path_lengths;
	// Block_array<Envelope::SPtr>::SPtr m_path_envelopes;
	ArrayList<AttributeStreamOfInt32> m_pathindices;// path user indices are
													// here
	// *****************End Path Data***************
	StridedIndexTypeCollection m_geometry_index_list;
	ArrayList<AttributeStreamOfInt32> m_geometry_indices;// geometry user
															// indices are here

	// *********** Helpers for Bucket sort**************
	static class EditShapeBucketSortHelper extends ClassicSort {
		EditShape m_shape;

		EditShapeBucketSortHelper(EditShape shape) {
			m_shape = shape;
		}

		@Override
		public void userSort(int begin, int end, AttributeStreamOfInt32 indices) {
			m_shape.sortVerticesSimpleByYHelper_(indices, begin, end);
		}

		@Override
		public double getValue(int index) {
			return m_shape.getY(index);
		}
	};

	BucketSort m_bucket_sort;

	// Envelope::SPtr m_envelope; //the BBOX for all attributes
	Point m_helper_point; // a helper point for intermediate operations

	Segment getSegmentFromIndex_(int vindex) {
		return m_segments != null ? m_segments.get(vindex) : null;
	}

	void setSegmentToIndex_(int vindex, Segment seg) {
		if (m_segments == null) {
			if (seg == null)
				return;
			m_segments = new ArrayList<Segment>();
			for (int i = 0, n = m_vertices.getPointCount(); i < n; i++)
				m_segments.add(null);
		}
		m_segments.set(vindex, seg);
	}

	void setPrevPath_(int path, int prev) {
		m_path_index_list.setField(path, 1, prev);
	}

	void setNextPath_(int path, int next) {
		m_path_index_list.setField(path, 2, next);
	}

	void setPathFlags_(int path, int flags) {
		m_path_index_list.setField(path, 6, flags);
	}

	int getPathFlags_(int path) {
		return m_path_index_list.getField(path, 6);
	}

	void setPathGeometry_(int path, int geom) {
		m_path_index_list.setField(path, 7, geom);
	}

	int getPathIndex_(int path) {
		return m_path_index_list.getField(path, 0);
	}

	void setNextGeometry_(int geom, int next) {
		m_geometry_index_list.setField(geom, 1, next);
	}

	void setPrevGeometry_(int geom, int prev) {
		m_geometry_index_list.setField(geom, 0, prev);
	}

	int getGeometryIndex_(int geom) {
		return m_geometry_index_list.getField(geom, 7);
	}

	int getFirstPath_(int geom) {
		return m_geometry_index_list.getField(geom, 3);
	}

	void setFirstPath_(int geom, int firstPath) {
		m_geometry_index_list.setField(geom, 3, firstPath);
	}

	void setLastPath_(int geom, int path) {
		m_geometry_index_list.setField(geom, 4, path);
	}

	int newGeometry_(int gt) {
		// Index_type index = m_first_free_geometry;
		if (m_geometry_index_list == null)
			m_geometry_index_list = new StridedIndexTypeCollection(8);

		int index = m_geometry_index_list.newElement();
		// m_geometry_index_list.set(index + 0, -1);//prev
		// m_geometry_index_list.set(index + 1, -1);//next
		m_geometry_index_list.setField(index, 2, gt);// Geometry_type
		// m_geometry_index_list.set(index + 3, -1);//first path
		// m_geometry_index_list.set(index + 4, -1);//last path
		m_geometry_index_list.setField(index, 5, 0);// point count
		m_geometry_index_list.setField(index, 6, 0);// path count
		m_geometry_index_list.setField(index, 7,
				m_geometry_index_list.elementToIndex(index));// geometry index

		return index;
	}

	void freeGeometry_(int geom) {
		m_geometry_index_list.deleteElement(geom);
	}

	int newPath_(int geom) {
		if (m_path_index_list == null) {
			m_path_index_list = new StridedIndexTypeCollection(8);
			m_vertex_index_list = new StridedIndexTypeCollection(5);
			m_path_areas = new AttributeStreamOfDbl(0);
			m_path_lengths = new AttributeStreamOfDbl(0);
		}

		int index = m_path_index_list.newElement();
		int pindex = m_path_index_list.elementToIndex(index);
		m_path_index_list.setField(index, 0, pindex);// size
		// m_path_index_list.set(index + 1, -1);//prev
		// m_path_index_list.set(index + 2, -1);//next
		m_path_index_list.setField(index, 3, 0);// size
		// m_path_index_list.set(index + 4, -1);//first vertex handle
		// m_path_index_list.set(index + 5, -1);//last vertex handle
		m_path_index_list.setField(index, 6, 0);// path flags
		setPathGeometry_(index, geom);
		if (pindex >= m_path_areas.size()) {
			int sz = pindex < 16 ? 16 : (pindex * 3) / 2;
			m_path_areas.resize(sz);
			m_path_lengths.resize(sz);
			// if (m_path_envelopes)
			// m_path_envelopes.resize(sz);
		}
		m_path_areas.set(pindex, 0);
		m_path_lengths.set(pindex, 0);
		// if (m_path_envelopes)
		// m_path_envelopes.set(pindex, nullptr);

		m_path_count++;
		return index;
	}

	void freePath_(int path) {
		m_path_index_list.deleteElement(path);
		m_path_count--;
	}

	void freeVertex_(int vertex) {
		m_vertex_index_list.deleteElement(vertex);
		m_point_count--;
	}

	int newVertex_(int vindex) {
		assert (vindex >= 0 || vindex == -1);// vindex is not a handle

		if (m_path_index_list == null) {
			m_path_index_list = new StridedIndexTypeCollection(8);
			m_vertex_index_list = new StridedIndexTypeCollection(5);
			m_path_areas = new AttributeStreamOfDbl(0);
			m_path_lengths = new AttributeStreamOfDbl(0);
		}

		int index = m_vertex_index_list.newElement();
		int vi = vindex >= 0 ? vindex : m_vertex_index_list
				.elementToIndex(index);
		m_vertex_index_list.setField(index, 0, vi);
		if (vindex < 0) {
			if (vi >= m_vertices.getPointCount()) {
				int sz = vi < 16 ? 16 : (vi * 3) / 2;
				// m_vertices.reserveRounded(sz);
				m_vertices.resize(sz);
				if (m_segments != null) {
					for (int i = 0; i < sz; i++)
						m_segments.add(null);
				}

				if (m_weights != null)
					m_weights.resize(sz);

				m_xy_stream = (AttributeStreamOfDbl) m_vertices
						.getAttributeStreamRef(VertexDescription.Semantics.POSITION);
			}

			m_vertices.setXY(vi, -1e38, -1e38);

			if (m_segments != null)
				m_segments.set(vi, null);

			if (m_weights != null)
				m_weights.write(vi, 1.0);
		} else {
			// We do not set vertices or segments here, because we assume those
			// are set correctly already.
			// We only here to create linked list of indices on existing vertex
			// value.
			// m_segments->set(m_point_count, nullptr);
		}

		m_vertex_index_list.setField(index, 4, vi * 2);
		m_point_count++;
		return index;
	}

	void free_vertex_(int vertex) {
		m_vertex_index_list.deleteElement(vertex);
		m_point_count--;
	}

	int insertVertex_(int path, int before, Point point) {
		int prev = before != -1 ? getPrevVertex(before) : getLastVertex(path);
		int next = prev != -1 ? getNextVertex(prev) : -1;

		int vertex = newVertex_(point == null ? m_point_count : -1);
		int vindex = getVertexIndex(vertex);
		if (point != null)
			m_vertices.setPointByVal(vindex, point);

		setPathToVertex_(vertex, path);
		setNextVertex_(vertex, next);
		setPrevVertex_(vertex, prev);

		if (next != -1)
			setPrevVertex_(next, vertex);

		if (prev != -1)
			setNextVertex_(prev, vertex);

		boolean b_closed = isClosedPath(path);
		int first = getFirstVertex(path);
		if (before == -1)
			setLastVertex_(path, vertex);

		if (before == first)
			setFirstVertex_(path, vertex);

		if (b_closed && next == -1) {
			setNextVertex_(vertex, vertex);
			setPrevVertex_(vertex, vertex);
		}

		setPathSize_(path, getPathSize(path) + 1);
		int geometry = getGeometryFromPath(path);
		setGeometryVertexCount_(geometry, getPointCount(geometry) + 1);

		return vertex;
	}

	Point getHelperPoint_() {
		if (m_helper_point == null)
			m_helper_point = new Point(m_vertices.getDescription());
		return m_helper_point;
	}
	
	void setFillRule(int geom, int rule) {
	      int t = m_geometry_index_list.getField(geom, 2);
	      t &= ~(0x8000000);
	      t |= rule == Polygon.FillRule.enumFillRuleWinding ? 0x8000000 : 0;
	      m_geometry_index_list.setField(geom, 2, t);//fill rule combined with geometry type
	}

	int getFillRule(int geom) {
	  int t = m_geometry_index_list.getField(geom, 2);
	  return (t & 0x8000000) != 0 ? Polygon.FillRule.enumFillRuleWinding : Polygon.FillRule.enumFillRuleOddEven;
	}
	
	int addMultiPath_(MultiPath multi_path) {
		int newgeom = createGeometry(multi_path.getType(),
				multi_path.getDescription());
		if (multi_path.getType() == Geometry.Type.Polygon)
			setFillRule(newgeom, ((Polygon)multi_path).getFillRule());
		
		appendMultiPath_(newgeom, multi_path);
		return newgeom;
	}

	int addMultiPoint_(MultiPoint multi_point) {
		int newgeometry = createGeometry(multi_point.getType(),
				multi_point.getDescription());
		appendMultiPoint_(newgeometry, multi_point);
		return newgeometry;
	}

	void appendMultiPath_(int dstGeom, MultiPath multi_path) {
		MultiPathImpl mpImpl = (MultiPathImpl) multi_path._getImpl();
		// m_vertices->reserve_rounded(m_vertices->get_point_count() +
		// mp_impl->get_point_count());//ensure reallocation happens by blocks
		// so that already allocated vertices do not get reallocated.
		m_vertices_mp.add(multi_path, 0, mpImpl.getPointCount());
		m_xy_stream = (AttributeStreamOfDbl) m_vertices
				.getAttributeStreamRef(VertexDescription.Semantics.POSITION);
		boolean b_some_segments = m_segments != null
				&& mpImpl.getSegmentFlagsStreamRef() != null;

		for (int ipath = 0, npath = mpImpl.getPathCount(); ipath < npath; ipath++) {
			if (mpImpl.getPathSize(ipath) < 2) // CR249862 - Clipping geometry
												// which has empty part produces
												// a crash
				continue;

			int path = insertPath(dstGeom, -1);
			setClosedPath(path, mpImpl.isClosedPath(ipath));
			for (int ivertex = mpImpl.getPathStart(ipath), iend = mpImpl
					.getPathEnd(ipath); ivertex < iend; ivertex++) {
				int vertex = insertVertex_(path, -1, null);
				if (b_some_segments) {
					int vindex = getVertexIndex(vertex);
					if ((mpImpl.getSegmentFlags(ivertex) & (byte) SegmentFlags.enumLineSeg) != 0) {
						setSegmentToIndex_(vindex, null);
					} else {
						SegmentBuffer seg_buffer = new SegmentBuffer();
						mpImpl.getSegment(ivertex, seg_buffer, true);
						setSegmentToIndex_(vindex, seg_buffer.get());
					}
				}
			}
		}

		// {//debug
		// #ifdef DEBUG
		// for (Index_type geometry = get_first_geometry(); geometry != -1;
		// geometry = get_next_geometry(geometry))
		// {
		// for (Index_type path = get_first_path(geometry); path != -1; path =
		// get_next_path(path))
		// {
		// Index_type first = get_first_vertex(path);
		// Index_type v = first;
		// for (get_next_vertex(v); v != first; v = get_next_vertex(v))
		// {
		// assert(get_next_vertex(get_prev_vertex(v)) == v);
		// }
		// }
		// }
		// #endif
		// }
	}

	void appendMultiPoint_(int dstGeom, MultiPoint multi_point) {
		// m_vertices->reserve_rounded(m_vertices->get_point_count() +
		// multi_point.get_point_count());//ensure reallocation happens by
		// blocks so that already allocated vertices do not get reallocated.
		m_vertices_mp.add(multi_point, 0, multi_point.getPointCount());
		m_xy_stream = (AttributeStreamOfDbl) m_vertices
				.getAttributeStreamRef(VertexDescription.Semantics.POSITION);

		int path = insertPath(dstGeom, -1);
		for (int ivertex = 0, iend = multi_point.getPointCount(); ivertex < iend; ivertex++) {
			insertVertex_(path, -1, null);
		}
	}
	
    // ... rest of the class unchanged ...
}
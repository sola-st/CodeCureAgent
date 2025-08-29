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


/**
 * Implementation for the segment cracking.
 * 
 * Finds and splits all intersecting segments. Used by the TopoGraph and
 * Simplify.
 */
final class Cracker {
	private EditShape m_shape;
	private ProgressTracker m_progressTracker;
	private NonSimpleResult m_nonSimpleResult;
	private double m_tolerance;
	private Treap m_sweepStructure;
	private SweepComparator m_sweepComparator;
	private boolean m_bAllowCoincident;

	private Segment getSegment_(int vertex, Line lineHelper) {
		Segment seg = m_shape.getSegment(vertex);
		if (seg == null) {
			if (!m_shape.queryLineConnector(vertex, lineHelper))
				return null;
			
			seg = (Segment)lineHelper;
		}
		
		return seg;
	}
	
	private boolean crackBruteForce_() {
		EditShape.VertexIterator iter1 = m_shape.queryVertexIterator(false);
		boolean bCracked = false;
		Line line1 = new Line();
		Line line2 = new Line();
		Envelope2D seg1Env = new Envelope2D();
		seg1Env.setEmpty();
		Envelope2D seg2Env = new Envelope2D();
		seg2Env.setEmpty();
		boolean assumeIntersecting = false;
		Point helperPoint = new Point();
		SegmentIntersector segmentIntersector = new SegmentIntersector();

		for (int vertex1 = iter1.next(); vertex1 != -1; vertex1 = iter1.next()) {
			ProgressTracker.checkAndThrow(m_progressTracker);

			int gt1 = m_shape.getGeometryType(iter1.currentGeometry());

			Segment seg1 = null;
			boolean seg1Zero = false;
			if (!Geometry.isPoint(gt1)) {
				seg1 = getSegment_(vertex1, line1);
				if (seg1 == null)
					continue;
				
				seg1.queryEnvelope2D(seg1Env);
				seg1Env.inflate(m_tolerance, m_tolerance);

				if (seg1.isDegenerate(m_tolerance))// do not crack with
													// degenerate segments
				{
					if (seg1.isDegenerate(0)) {
						seg1Zero = true;
						seg1 = null;
					}
					else {
						continue;
					}
				}
			}

			EditShape.VertexIterator iter2 = m_shape.queryVertexIterator(iter1);
			int vertex2 = iter2.next();
			if (vertex2 != -1)
				vertex2 = iter2.next();

			for (; vertex2 != -1; vertex2 = iter2.next()) {
				int gt2 = m_shape.getGeometryType(iter2.currentGeometry());

				Segment seg2 = null;
				boolean seg2Zero = false;
				if (!Geometry.isPoint(gt2)) {
					seg2 = getSegment_(vertex2, line2);
					if (seg2 == null) {
						continue;
					}
					
					seg2.queryEnvelope2D(seg2Env);
					if (seg2.isDegenerate(m_tolerance))// do not crack with
														// degenerate segments
					{
						if (seg2.isDegenerate(0)) {
							seg2Zero = true;
							seg2 = null;
						}
						else {
							continue;
						}
					}
				}

				int splitCount1 = 0;
				int splitCount2 = 0;
				if (seg1 != null && seg2 != null) {
					if (seg1Env.isIntersectingNE(seg2Env)) {
						segmentIntersector.pushSegment(seg1);
						segmentIntersector.pushSegment(seg2);
						segmentIntersector.intersect(m_tolerance, assumeIntersecting);
						splitCount1 = segmentIntersector.getResultSegmentCount(0);
						splitCount2 = segmentIntersector.getResultSegmentCount(1);
						if (splitCount1 + splitCount2 > 0) {
							m_shape.splitSegment_(vertex1, segmentIntersector, 0, true);
							m_shape.splitSegment_(vertex2, segmentIntersector, 1, true);
						}
						segmentIntersector.clear();
					}
				} else {
					if (seg1 != null) {
						Point2D pt = new Point2D();
						m_shape.getXY(vertex2, pt);
						if (seg1Env.contains(pt)) {
							segmentIntersector.pushSegment(seg1);
							m_shape.queryPoint(vertex2, helperPoint);
							segmentIntersector.intersect(m_tolerance, helperPoint, 0, 1.0, assumeIntersecting);
							splitCount1 = segmentIntersector.getResultSegmentCount(0);
							if (splitCount1 > 0) {
								m_shape.splitSegment_(vertex1, segmentIntersector, 0, true);
								if (seg2Zero) {
				                    //seg_2 was zero length. Need to change all coincident points
				                    //segment at vertex_2 is dzero length, change all attached zero length segments
				                    int vTo = -1;
				                    for (int v = m_shape.getNextVertex(vertex2); v != -1 && v != vertex2; v = m_shape.getNextVertex(v)) {
				                      seg2 = getSegment_(v, line2);
				                      vTo = v;
				                      if (seg2 == null || !seg2.isDegenerate(0))
				                        break;
				                    }
				                    //change from vertex_2 to v_to (inclusive).
				                    for (int v = vertex2; v != -1; v = m_shape.getNextVertex(v)) {
				                      m_shape.setPoint(v, segmentIntersector.getResultPoint());
				                      if (v == vTo)
				                        break;
				                    }									
								}
								else {
									m_shape.setPoint(vertex2, segmentIntersector.getResultPoint());
								}
							}
							segmentIntersector.clear();
						}
					} else if (seg2 != null) {
						Point2D pt = new Point2D();
						m_shape.getXY(vertex1, pt);
						seg2Env.inflate(m_tolerance, m_tolerance);
						if (seg2Env.contains(pt)) {
							segmentIntersector.pushSegment(seg2);
							m_shape.queryPoint(vertex1, helperPoint);
							segmentIntersector.intersect(m_tolerance, helperPoint, 0, 1.0, assumeIntersecting);
							splitCount2 = segmentIntersector.getResultSegmentCount(0);
							if (splitCount2 > 0) {
								m_shape.splitSegment_(vertex2, segmentIntersector, 0, true);
								if (seg1Zero) {
				                    //seg_1 was zero length. Need to change all coincident points
				                    //segment at vertex_2 is dzero length, change all attached zero length segments
				                    int vTo = -1;
				                    for (int v = m_shape.getNextVertex(vertex1); v != -1 && v != vertex1; v = m_shape.getNextVertex(v)) {
				                      seg2 = getSegment_(v, line2);//using here seg_2 for seg_1
				                      vTo = v;
				                      if (seg2 == null || !seg2.isDegenerate(0))
				                        break;
				                    }
				                    //change from vertex_2 to v_to (inclusive).
				                    for (int v = vertex1; v != -1; v = m_shape.getNextVertex(v)) {
				                      m_shape.setPoint(v, segmentIntersector.getResultPoint());
				                      if (v == vTo)
				                        break;
				                    }
								}
								else {
									m_shape.setPoint(vertex1, segmentIntersector.getResultPoint());
								}
							}
							segmentIntersector.clear();
						}
					} else {
						continue;// points on points
					}
				}

				if (splitCount1 + splitCount2 != 0) {
					if (splitCount1 != 0) {
						seg1 = m_shape.getSegment(vertex1);// reload segment
															// after split
						if (seg1 == null) {
							if (!m_shape.queryLineConnector(vertex1, line1))
								continue;
							seg1 = line1;
							line1.queryEnvelope2D(seg1Env);
						} else
							seg1.queryEnvelope2D(seg1Env);

						if (seg1.isDegenerate(m_tolerance))// do not crack with
															// degenerate
															// segments
						{
							break;
						}
					}

					bCracked = true;
				}
			}
		}

		return bCracked;
	}

	boolean crackerPlaneSweep_() {
		boolean bCracked = planeSweep_();
		return bCracked;
	}

	boolean planeSweep_() {
		PlaneSweepCrackerHelper planeSweep = new PlaneSweepCrackerHelper();
		boolean bCracked = planeSweep.sweep(m_shape, m_tolerance);
		return bCracked;
	}

	boolean needsCrackingImpl_() {
		boolean bNeedsCracking = false;
		
		if (m_sweepStructure == null)

			m_sweep_structure = new Treap();

		AttributeStreamOfInt32 event_q = new AttributeStreamOfInt32(0);
		event_q.reserve(m_shape.getTotalPointCount() + 1);

		EditShape.VertexIterator iter = m_shape.queryVertexIterator();
		for (int vert = iter.next(); vert != -1; vert = iter.next()) {
			event_q.add(vert);
		}
		assert (m_shape.getTotalPointCount() == event_q.size());

		m_shape.sortVerticesSimpleByY_(event_q, 0, event_q.size());
		event_q.add(-1);// for termination;
		// create user indices to store edges that end at vertices.
		int edge_index_1 = m_shape.createUserIndex();
		int edge_index_2 = m_shape.createUserIndex();
		m_sweep_comparator = new SweepComparator(m_shape, m_tolerance, !m_bAllowCoincident);
		m_sweep_structure.setComparator(m_sweep_comparator);

		AttributeStreamOfInt32 swept_edges_to_delete = new AttributeStreamOfInt32(
				0);
		AttributeStreamOfInt32 edges_to_insert = new AttributeStreamOfInt32(0);

		// Go throught the sorted vertices
		int event_q_index = 0;
		Point2D cluster_pt = new Point2D();

		// sweep-line algorithm:
		for (int vertex = event_q.get(event_q_index++); vertex != -1;) {
			m_shape.getXY(vertex, cluster_pt);
			
			do {
				int next_vertex = m_shape.getNextVertex(vertex);
				int prev_vertex = m_shape.getPrevVertex(vertex);

				if (next_vertex != -1
						&& m_shape.compareVerticesSimpleY_(vertex, next_vertex) < 0) {
					edges_to_insert.add(vertex);
					edges_to_insert.add(next_vertex);
				}

				if (prev_vertex != -1
						&& m_shape.compareVerticesSimpleY_(vertex, prev_vertex) < 0) {
					edges_to_insert.add(prev_vertex);
					edges_to_insert.add(prev_vertex);
				}

				// Continue accumulating current cluster
				int attached_edge_1 = m_shape
						.getUserIndex(vertex, edge_index_1);
				if (attached_edge_1 != -1) {
					swept_edges_to_delete.add(attached_edge_1);
					m_shape.setUserIndex(vertex, edge_index_1, -1);
				}
				int attached_edge_2 = m_shape
						.getUserIndex(vertex, edge_index_2);
				if (attached_edge_2 != -1) {
					swept_edges_to_delete.add(attached_edge_2);
					m_shape.setUserIndex(vertex, edge_index_2, -1);
				}
				vertex = event_q.get(event_q_index++);
			} while (vertex != -1 && m_shape.isEqualXY(vertex, cluster_pt));

			boolean b_continuing_segment_chain_optimization = swept_edges_to_delete
					.size() == 1 && edges_to_insert.size() == 2;

			int new_left = -1;
			int new_right = -1;
			// Process the cluster
			for (int i = 0, n = swept_edges_to_delete.size(); i < n; i++) {
				// Find left and right neighbour of the edges that terminate at
				// the cluster (there will be atmost only one left and one
				// right).
				int edge = swept_edges_to_delete.get(i);
				int left = m_sweep_structure.getPrev(edge);
				if (left != -1 && !swept_edges_to_delete.hasElement(left))// Note:
																			// for
																			// some
																			// heavy
																			// cases,
																			// it
																			// could
																			// be
																			// better
																			// to
																			// use
																			// binary
																			// search.
				{
					assert (new_left == -1);
					new_left = left;
				}

				int right = m_sweep_structure.getNext(edge);
				if (right != -1 && !swept_edges_to_delete.hasElement(right)) {
					assert (new_right == -1);
					new_right = right;
				}
//#ifdef NDEBUG				
				if (new_left != -1 && new_right != -1)
					break;
//#endif
			}

			assert (new_left == -1 || new_left != new_right);

			m_sweep_comparator.setSweepY(cluster_pt.y, cluster_pt.x);

			// Delete the edges that terminate at the cluster.
			for (int i = 0, n = swept_edges_to_delete.size(); i < n; i++) {
				int edge = swept_edges_to_delete.get(i);
				m_sweep_structure.deleteNode(edge, -1);
			}
			swept_edges_to_delete.clear(false);

			if (!b_continuing_segment_chain_optimization && new_left != -1 && new_right != -1) {
				if (checkForIntersections_(new_left, new_right)) {
					b_needs_cracking = true;
					m_non_simple_result = m_sweep_comparator.getResult();
					break;
				}
			}

			for (int i = 0, n = edges_to_insert.size(); i < n; i += 2) {
				int v = edges_to_insert.get(i);
				int otherv = edges_to_insert.get(i + 1);

				int new_edge_1 = -1;
				if (b_continuing_segment_chain_optimization) {
					new_edge_1 = m_sweep_structure.addElementAtPosition(
							new_left, new_right, v, true, true, -1);
					b_continuing_segment_chain_optimization = false;
				} else {
					new_edge_1 = m_sweep_structure.addElement(v, -1); // the
																		// sweep
																		// structure
																		// consist
																		// of
																		// the
																		// origin
																		// vertices
																		// for
																		// edges.
																		// One
																		// can
																		// always
																		// get
																		// the
																		// other
																		// endpoint
																		// as
																		// the
																		// next
																		// vertex.
				}

				if (m_sweep_comparator.intersectionDetected()) {
					m_non_simple_result = m_sweep_comparator.getResult();
					b_needs_cracking = true;
					break;
				}

				int e_1 = m_shape.getUserIndex(otherv, edge_index_1);
				if (e_1 == -1)
					m_shape.setUserIndex(otherv, edge_index_1, new_edge_1);
				else {
					assert (m_shape.getUserIndex(otherv, edge_index_2) == -1);
					m_shape.setUserIndex(otherv, edge_index_2, new_edge_1);
				}
			}

			if (b_needs_cracking)
				break;

			// Start accumulating new cluster
			edges_to_insert.resizePreserveCapacity(0);
		}

		m_shape.removeUserIndex(edge_index_1);
		m_shape.removeUserIndex(edge_index_2);
		return b_needs_cracking;
	}

	boolean checkForIntersections_(int sweep_edge_1, int sweep_edge_2) {
		assert (sweep_edge_1 != sweep_edge_2);
		int left = m_sweep_structure.getElement(sweep_edge_1);
		assert (left != m_sweep_structure.getElement(sweep_edge_2));
		m_sweep_comparator.compare(m_sweep_structure, left, sweep_edge_2);// compare
																			// detects
																			// intersections
		boolean b_intersects = m_sweep_comparator.intersectionDetected();
		m_sweep_comparator.clearIntersectionDetectedFlag();
		return b_intersects;
	}

	// void dbg_print_sweep_edge_(int edge);
	// void dbg_print_sweep_structure_();
	// void dbg_check_sweep_structure_();
	Cracker(ProgressTracker progress_tracker) {
		m_progress_tracker = progress_tracker;
		m_bAllowCoincident = true;
	}

	static boolean canBeCracked(EditShape shape) {
		for (int geometry = shape.getFirstGeometry(); geometry != -1; geometry = shape
				.getNextGeometry(geometry)) {
			if (!Geometry.isMultiPath(shape.getGeometryType(geometry)))
				continue;
			return true;
		}
		return false;
	}

	static boolean execute(EditShape shape, Envelope2D extent,
			double tolerance, ProgressTracker progress_tracker) {
		if (!canBeCracked(shape)) // make sure it contains some segments,
									// otherwise no need to crack.
			return false;

		Cracker cracker = new Cracker(progress_tracker);
		cracker.m_shape = shape;
		cracker.m_tolerance = tolerance;
		// Use brute force for smaller shapes, and a planesweep for bigger
		// shapes.
		boolean b_cracked = false;
		if (shape.getTotalPointCount() < 15) // what is a good number?
		{
			b_cracked = cracker.crackBruteForce_();
		} else {
			boolean b_cracked_1 = cracker.crackerPlaneSweep_();
			return b_cracked_1;
		}
		return b_cracked;
	}

	static boolean execute(EditShape shape, double tolerance,
			ProgressTracker progress_tracker) {
		return Cracker.execute(shape, shape.getEnvelope2D(), tolerance,
				progress_tracker);
	}

	// Used for IsSimple.
	static boolean needsCracking(boolean allowCoincident, EditShape shape, double tolerance,
			NonSimpleResult result, ProgressTracker progress_tracker) {
		if (!canBeCracked(shape))
			return false;

		Cracker cracker = new Cracker(progress_tracker);
		cracker.m_shape = shape;
		cracker.m_tolerance = tolerance;
		cracker.m_bAllowCoincident = allowCoincident;
		if (cracker.needsCrackingImpl_()) {
			if (result != null)
				result.Assign(cracker.m_non_simple_result);
			return true;
		}

		// Now swap the coordinates to catch horizontal cases.
		Transformation2D transform = new Transformation2D();
		transform.setSwapCoordinates();
		shape.applyTransformation(transform);

		cracker = new Cracker(progress_tracker);
		cracker.m_shape = shape;
		cracker.m_tolerance = tolerance;
		cracker.m_bAllowCoincident = allowCoincident;
		boolean b_res = cracker.needsCrackingImpl_();

		transform.setSwapCoordinates();
		shape.applyTransformation(transform);// restore shape

		if (b_res) {
			if (result != null)
				result.Assign(cracker.m_non_simple_result);
			return true;
		}

		return false;
	}
}
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
 * Implementation for the vertex clustering.
 * 
 * Used by the TopoGraph and Simplify.
 */
final class Clusterer {
	// Clusters vertices of the shape. Returns True, if some vertices were moved
	// (clustered).
	// Uses reciprocal clustering (cluster vertices that are mutual nearest
	// neighbours)
	/*
	 * static boolean executeReciprocal(EditShape shape, double tolerance) {
	 * Clusterer clusterer = new Clusterer(); clusterer.m_shape = shape;
	 * clusterer.m_tolerance = tolerance; clusterer.m_sqr_tolerance = tolerance
	 * * tolerance; clusterer.m_cell_size = 2 * tolerance;
	 * clusterer.m_inv_cell_size = 1.0 / clusterer.m_cell_size; return
	 * clusterer.clusterReciprocal_(); }
	 */

	// Clusters vertices of the shape. Returns True, if some vertices were moved
	// (clustered).
	// Uses non-reciprocal clustering (cluster any vertices that are closer than
	// the tolerance in the first-found-first-clustered order)
	static boolean executeNonReciprocal(EditShape shape, double tolerance) {
		Clusterer clusterer = new Clusterer();
		clusterer.m_shape = shape;
		clusterer.m_tolerance = tolerance;
		clusterer.m_sqr_tolerance = tolerance * tolerance;
		clusterer.m_cell_size = 2 * tolerance;
		clusterer.m_inv_cell_size = 1.0 / clusterer.m_cell_size;
		return clusterer.clusterNonReciprocal_();
	}

	// Use b_conservative == True for simplify, and False for IsSimple. This
	// makes sure Simplified shape is more robust to transformations.
	static boolean isClusterCandidate_(double x_1, double y1, double x2,
			double y2, double sqr_tolerance) {
		double dx = x_1 - x2;
		double dy = y1 - y2;
		return dx * dx + dy * dy <= sqr_tolerance;
	}

	Point2D m_origin = new Point2D();
	double m_tolerance;
	double m_sqr_tolerance;
	double m_cell_size;
	double m_inv_cell_size;
	int[] m_bucket_array = new int[4];// temporary 4 element array
	int[] m_bucket_hash = new int[4];// temporary 4 element array
	int m_dbg_candidate_check_count = 0;
	int m_hash_values = -1;
	int m_new_clusters = -1;

	static int hashFunction_(int xi, int yi) {
		int h = NumberUtils.hash(xi);
		return NumberUtils.hash(h, yi);
	}

	final class ClusterHashFunction extends IndexHashTable.HashFunction {
		EditShape m_shape;
		double m_sqr_tolerance;
		double m_inv_cell_size;
		Point2D m_origin = new Point2D();
		Point2D m_pt = new Point2D();
		Point2D m_pt_2 = new Point2D();
		int m_hash_values;

		public ClusterHashFunction(EditShape shape, Point2D origin,
				double sqr_tolerance, double inv_cell_size, int hash_values) {
			m_shape = shape;
			m_sqr_tolerance = sqr_tolerance;
			m_inv_cell_size = inv_cell_size;
			m_origin = origin;
			m_hash_values = hash_values;
			m_pt.setNaN();
			m_pt_2.setNaN();
		}

		int calculate_hash(int element) {
			return calculate_hash_from_vertex(element);
		}

		int dbg_calculate_hash_from_xy(double x, double y) {
			double dx = x - m_origin.x;
			int xi = (int) (dx * m_inv_cell_size + 0.5);
			double dy = y - m_origin.y;
			int yi = (int) (dy * m_inv_cell_size + 0.5);
			return hashFunction_(xi, yi);
		}

		int calculate_hash_from_vertex(int vertex) {
			m_shape.getXY(vertex, m_pt);
			double dx = m_pt.x - m_origin.x;
			int xi = (int) (dx * m_inv_cell_size + 0.5);
			double dy = m_pt.y - m_origin.y;
			int yi = (int) (dy * m_inv_cell_size + 0.5);
			return hashFunction_(xi, yi);
		}

		@Override
		public int getHash(int element) {
			return m_shape.getUserIndex(element, m_hash_values);
		}

		@Override
		public boolean equal(int element_1, int element_2) {
			int xyindex_1 = element_1;
			int xyindex_2 = element_2;
			m_shape.getXY(xyindex_1, m_pt);
			m_shape.getXY(xyindex_2, m_pt_2);
			return isClusterCandidate_(m_pt.x, m_pt.y, m_pt_2.x, m_pt_2.y,
					m_sqr_tolerance);
		}

		@Override
		public int getHash(Object element_descriptor) {
			// UNUSED
			return 0;
		}

		@Override
		public boolean equal(Object element_descriptor, int element) {
			// UNUSED
			return false;
		}
	};

	EditShape m_shape;
	IndexMultiList m_clusters;
	ClusterHashFunction m_hash_function;
```java
	IndexHashTable m_hash_table;

	static class ClusterCandidate {
		public int vertex;
		double distance;
	};

	void getNearestNeighbourCandidate_(int xyIndex, Point2D pointOfInterest,
		 int bucketPtr, ClusterCandidate candidate) {
		candidate.vertex = -1;
		candidate.distance = NumberUtils.doubleMax();

		Point2D pt = new Point2D();
		for (int node = bucketPtr; node != -1; node = m_hash_table
				.getNextInBucket(node)) {
			int xyInd = m_hash_table.getElement(node);
			if (xyIndex == xyInd)
				continue;

			m_shape.getXY(xyInd, pt);
			if (isClusterCandidate_(pointOfInterest.x, pointOfInterest.y, pt.x,
					pt.y, m_sqr_tolerance)) {
				pt.sub(pointOfInterest);
				double l = pt.length();
				if (l < candidate.distance) {
					candidate.distance = l;
					candidate.vertex = xyInd;
				}
			}
		}
	}

	void findClusterCandidate_(int xyIndex, ClusterCandidate candidate) {
		Point2D pointOfInterest = new Point2D();
		m_shape.getXY(xyIndex, pointOfInterest);
		double x0 = pointOfInterest.x - m_origin.x;
		double x = x0 * m_inv_cell_size;
		double y0 = pointOfInterest.y - m_origin.y;
		double y = y0 * m_inv_cell_size;

		int xi = (int) x;
		int yi = (int) y;

		// find the nearest neighbour in the 4 neigbouring cells.

		candidate.vertex = -1;
		candidate.distance = NumberUtils.doubleMax();
		ClusterCandidate c = new ClusterCandidate();
		for (int dx = 0; dx <= 1; dx += 1) {
			for (int dy = 0; dy <= 1; dy += 1) {
				int bucketPtr = m_hash_table.getFirstInBucket(hashFunction_(xi
						+ dx, yi + dy));
				if (bucketPtr != IndexHashTable.nullNode()) {
					getNearestNeighbourCandidate_(xyIndex, pointOfInterest,
							bucketPtr, c);
					if (c.vertex != IndexHashTable.nullNode()
							&& c.distance < candidate.distance) {
						candidate = c;
					}
				}
			}
		}
	}

	void collectClusterCandidates_(int xyIndex,
			AttributeStreamOfInt32 candidates) {
		Point2D pointOfInterest = new Point2D();
		m_shape.getXY(xyIndex, pointOfInterest);
		double x0 = pointOfInterest.x - m_origin.x;
		double x = x0 * m_inv_cell_size;
		double y0 = pointOfInterest.y - m_origin.y;
		double y = y0 * m_inv_cell_size;

		int xi = (int) x;
		int yi = (int) y;

		int bucketCount = 0;
		// find all nearest neighbours in the 4 neigbouring cells.
		// Note, because we check four neighbours, there should be 4 times more
		// bins in the hash table to reduce collision probability in this loop.
		for (int dx = 0; dx <= 1; dx += 1) {
			for (int dy = 0; dy <= 1; dy += 1) {
				int hash = hashFunction_(xi + dx, yi + dy);
				int bucketPtr = m_hash_table.getFirstInBucket(hash);
				if (bucketPtr != -1) {
					// Check if we already have this bucket.
					// There could be a hash collision for neighbouring buckets.
					m_bucket_array[bucketCount] = bucketPtr;
					m_bucket_hash[bucketCount] = hash;

					bucketCount++;
				}
			}
		}

		// Clear duplicate buckets
		// There could be a hash collision for neighboring buckets.
		for (int j = bucketCount - 1; j >= 1; j--) {
			int bucketPtr = m_bucket_array[j];
			for (int i = j - 1; i >= 0; i--) {
				if (bucketPtr == m_bucket_array[i])// hash values for two
													// neighbouring cells have
													// collided.
				{
					m_bucket_hash[i] = -1; // forget collided hash
					bucketCount--;
					if (j != bucketCount) {
						m_bucket_hash[j] = m_bucket_hash[bucketCount];
						m_bucket_array[j] = m_bucket_array[bucketCount];
					}
					break;// duplicate
				}
			}
		}

		for (int i = 0; i < bucketCount; i++) {
			collectNearestNeighbourCandidates_(xyIndex, m_bucket_hash[i],
					pointOfInterest, m_bucket_array[i], candidates);
		}
	}

	void collectNearestNeighbourCandidates_(int xyIndex, int hash,
			Point2D pointOfInterest, int bucketPtr,
			AttributeStreamOfInt32 candidates) {
		Point2D pt = new Point2D();
		for (int node = bucketPtr; node != -1; node = m_hash_table
				.getNextInBucket(node)) {
			int xyInd = m_hash_table.getElement(node);
			if (xyIndex == xyInd || hash != -1
					&& m_shape.getUserIndex(xyInd, m_hash_values) != hash)
				continue;// processing same vertex, or the bucket hash modulo
							// bin count collides.

			m_shape.getXY(xyInd, pt);
			m_dbg_candidate_check_count++;
			if (isClusterCandidate_(pointOfInterest.x, pointOfInterest.y, pt.x,
					pt.y, m_sqr_tolerance)) {
				candidates.add(node);// note that we add the cluster node
										// instead of the cluster.
			}
		}
	}

	boolean mergeClusters_(int vertex1, int vertex2, boolean updateHash) {
		int cluster1 = m_shape.getUserIndex(vertex1, m_new_clusters);
		int cluster2 = m_shape.getUserIndex(vertex2, m_new_clusters);
		assert (cluster1 != StridedIndexTypeCollection.impossibleIndex2());
		assert (cluster2 != StridedIndexTypeCollection.impossibleIndex2());

		if (cluster1 == -1) {
			cluster1 = m_clusters.createList();
			m_clusters.addElement(cluster1, vertex1);
			m_shape.setUserIndex(vertex1, m_new_clusters, cluster1);
		}

		if (cluster2 == -1) {
			m_clusters.addElement(cluster1, vertex2);
		} else {
			m_clusters.concatenateLists(cluster1, cluster2);
		}

		// ensure only single vertex refers to the cluster.
		m_shape.setUserIndex(vertex2, m_new_clusters,
				StridedIndexTypeCollection.impossibleIndex2());

		// merge cordinates
		boolean res = mergeVertices_(vertex1, vertex2);

		if (updateHash) {
			int hash = m_hash_function.calculate_hash_from_vertex(vertex1);
			m_shape.setUserIndex(vertex1, m_hash_values, hash);
		} else {

		}

		return res;
	}

	// recalculate coordinates of the vertices by averaging them using weights.
	// return true if the coordinates has changed.
	static boolean mergeVertices(Point pt1, Point pt2, double w1,
			int rank1, double w2, int rank2, Point2D ptRes, double[] wRes,
			int[] rankRes) {
		assert (!pt1.isEmpty() && !pt2.isEmpty());
		boolean res = pt1.equals(pt2);

		if (rank1 > rank2) {
			ptRes = pt1;
			if (wRes != null) {
				rankRes[0] = rank1;
				wRes[0] = w1;
			}
			return res;
		} else if (rank2 > rank1) {
			ptRes = pt2;
			if (wRes != null) {
				rankRes[0] = rank2;
				wRes[0] = w2;
			}
			return res;
		}

		ptRes = pt1;
		Point2D pt2d = new Point2D();
		mergeVertices2D(pt1.getXY(), pt2.getXY(), w1, rank1, w2, rank2,
				pt2d, wRes, rankRes);
		ptRes.setXY(pt2d);
		return res;
	}

	static boolean mergeVertices2D(Point2D pt1, Point2D pt2, double w1,
			int rank1, double w2, int rank2, Point2D ptRes, double[] wRes,
			int[] rankRes) {
		double w = w1 + w2;
		boolean r = false;
		double x = pt1.x;
		if (pt1.x != pt2.x) {
			if (rank1 == rank2)
				x = (pt1.x * w1 + pt2.x * w2) / w;

			r = true;
		}
		double y = pt1.y;
		if (pt1.y != pt2.y) {
			if (rank1 == rank2)
				y = (pt1.y * w1 + pt2.y * w2) / w;

			r = true;
		}

		if (rank1 != rank2) {
			if (rank1 > rank2) {
				if (wRes != null) {
					rankRes[0] = rank1;
					wRes[0] = w1;
				}
				ptRes = pt1;
			} else {
				if (wRes != null) {
					rankRes[0] = rank2;
					wRes[0] = w2;
				}
				ptRes = pt2;
			}
		} else {
			ptRes.setCoords(x, y);
			if (wRes != null) {
				wRes[0] = w;
				rankRes[0] = rank1;
			}
		}

		return r;
	}

	boolean mergeVertices_(int vert1, int vert2) {
		Point2D pt1 = new Point2D();
		m_shape.getXY(vert1, pt1);
		Point2D pt2 = new Point2D();
		m_shape.getXY(vert2, pt2);

		double w1 = m_shape.getWeight(vert1);
		double w2 = m_shape.getWeight(vert2);
		double w = w1 + w2;
		int r = 0;
		double x = pt1.x;
		if (pt1.x != pt2.x) {
			x = (pt1.x * w1 + pt2.x * w2) / w;
			r++;
		}
		double y = pt1.y;
		if (pt1.y != pt2.y) {
			y = (pt1.y * w1 + pt2.y * w2) / w;
			r++;
		}

		if (r > 0)
			m_shape.setXY(vert1, x, y);

		m_shape.setWeight(vert1, w);
		return r != 0;
	}

	boolean clusterNonReciprocal_() {
		int pointCount = m_shape.getTotalPointCount();
		Envelope2D env = m_shape.getEnvelope2D();
		m_origin = env.getLowerLeft();
		double dim = Math.max(env.getHeight(), env.getWidth());
		double minCell = dim / (NumberUtils.intMax() - 1);
		if (m_cell_size < minCell) {
			m_cell_size = minCell;
			m_inv_cell_size = 1.0 / m_cell_size;
		}

		// This holds clusters.
		m_clusters = new IndexMultiList();
		m_clusters.reserveLists(m_shape.getTotalPointCount() / 3 + 1);
		m_clusters.reserveNodes(m_shape.getTotalPointCount() / 3 + 1);

		m_hash_values = m_shape.createUserIndex();
		m_new_clusters = m_shape.createUserIndex();

		// Make the hash table. It serves a purpose of fine grain grid.
		// Make it 25% larger than the 4 times point count to reduce the chance
		// of collision.
		// The 4 times comes from the fact that we check four neighbouring cells
		// in the grid for each point.
		m_hash_function = new ClusterHashFunction(m_shape, m_origin,
				m_sqr_tolerance, m_inv_cell_size, m_hash_values);
		m_hash_table = new IndexHashTable(4 * pointCount / 3, m_hash_function);
		m_hash_table.reserveElements(m_shape.getTotalPointCount());
		boolean bClustered = false;

		// Go through all vertices stored in the m_shape and put the handles of
		// the vertices into the clusters and the hash table.
		for (int geometry = m_shape.getFirstGeometry(); geometry != -1; geometry = m_shape
				.getNextGeometry(geometry)) {
			for (int path = m_shape.getFirstPath(geometry); path != -1; path = m_shape
					.getNextPath(path)) {
				int vertex = m_shape.getFirstVertex(path);
				for (int index = 0, nindex = m_shape.getPathSize(path); index < nindex; index++) {
					assert (vertex != -1);
					int hash = m_hash_function
							.calculate_hash_from_vertex(vertex);
					m_shape.setUserIndex(vertex, m_hash_values, hash);
					m_hash_table.addElement(vertex, hash); // add cluster to the
															// hash table
					assert (m_shape.getUserIndex(vertex, m_new_clusters) == -1);
					vertex = m_shape.getNextVertex(vertex);
				}
			}
		}

		// m_hash_table->dbg_print_bucket_histogram_();

		{// scope for candidates array
			AttributeStreamOfInt32 candidates = new AttributeStreamOfInt32(0);
			candidates.reserve(10);

			for (int geometry = m_shape.getFirstGeometry(); geometry != -1; geometry = m_shape
					.getNextGeometry(geometry)) {
				for (int path = m_shape.getFirstPath(geometry); path != -1; path = m_shape
						.getNextPath(path)) {
					int vertex = m_shape.getFirstVertex(path);
					for (int index = 0, nindex = m_shape.getPathSize(path); index < nindex; index++) {
						if (m_shape.getUserIndex(vertex, m_new_clusters) == StridedIndexTypeCollection
								.impossibleIndex2()) {
							vertex = m_shape.getNextVertex(vertex);
							continue;// this vertex was merged with another
										// cluster. It also was removed from the
										// hash table.
						}

						int hash = m_shape.getUserIndex(vertex, m_hash_values);
						m_hash_table.deleteElement(vertex, hash);

						while (true) {
							collectClusterCandidates_(vertex, candidates);
							if (candidates.size() == 0) {// no candidate for
															// clustering has
															// been found for
															// the cluster1.
								break;
							}

							boolean clustered = false;
							for (int candidateIndex = 0, ncandidates = candidates
									.size(); candidateIndex < ncandidates; candidateIndex++) {
								int clusterNode = candidates
										.get(candidateIndex);
								int otherVertex = m_hash_table
										.getElement(clusterNode);
								m_hash_table.deleteNode(clusterNode);
								clustered |= mergeClusters_(vertex,
										otherVertex,
										candidateIndex + 1 == ncandidates);
							}

							bClustered |= clustered;
							candidates.clear(false);
							// repeat search for the cluster candidates for
							// cluster1
							if (!clustered)
								break;// positions did not change
						}

						// m_shape->set_user_index(vertex, m_new_clusters,
						// Strided_index_type_collection::impossible_index_2());
						vertex = m_shape.getNextVertex(vertex);
					}
				}
			}
		}

		if (bClustered) {
			applyClusterPositions_();
		}

		m_hash_table = null;
		m_hash_function = null;
		m_shape.removeUserIndex(m_hash_values);
		m_shape.removeUserIndex(m_new_clusters);

		// output_debug_printf("total: %d\n",m_shape->get_total_point_count());
		// output_debug_printf("clustered: %d\n",m_dbg_candidate_check_count);
		return bClustered;
	}

	void applyClusterPositions_() {
		Point2D clusterPt = new Point2D();
		// move vertices to the clustered positions.
		for (int list = m_clusters.getFirstList(); list != -1; list = m_clusters
				.getNextList(list)) {
			int node = m_clusters.getFirst(list);
			assert (node != -1);
			int vertex = m_clusters.getElement(node);
			m_shape.getXY(vertex, clusterPt);
			for (node = m_clusters.getNext(node); node != -1; node = m_clusters
					.getNext(node)) {
				int vertex1 = m_clusters.getElement(node);
				m_shape.setXY(vertex1, clusterPt);
			}

		}
	}

	Clusterer() {
	}

}
	// Returns true if the relation intersects, crosses, or contains holds
	// between multipathA and multipoint_b. multipathA is put in the
	// Quad_tree_impl.
	private static boolean linearPathIntersectsMultiPoint_(
			MultiPath multipathA, MultiPoint multipoint_b, double tolerance,
			boolean b_intersects_all) {
		SegmentIteratorImpl segIterA = ((MultiPathImpl) multipathA._getImpl())
				.querySegmentIterator();

		Envelope2D env_a = new Envelope2D();
		Envelope2D env_b = new Envelope2D();
		Envelope2D envInter = new Envelope2D();
		multipathA.queryEnvelope2D(env_a);
		multipoint_b.queryEnvelope2D(env_b);
		env_a.inflate(tolerance, tolerance);

		env_b.inflate(tolerance, tolerance);
		envInter.setCoords(env_a);
		envInter.intersect(env_b);

		QuadTreeImpl qtA = null;
		QuadTreeImpl quadTreeA = null;
        QuadTreeImpl quadTreePathsA = null;

		GeometryAccelerators accel = ((MultiPathImpl) multipathA._getImpl())
				._getAccelerators();

		if (accel != null) {
			quadTreeA = accel.getQuadTree();
			quadTreePathsA = accel.getQuadTreeForPaths();
			if (quadTreeA == null) {
				qtA = InternalUtils.buildQuadTree(
						(MultiPathImpl) multipathA._getImpl(), envInter);
				quadTreeA = qtA;
			}
		} else {
			qtA = InternalUtils.buildQuadTree(
					(MultiPathImpl) multipathA._getImpl(), envInter);
			quadTreeA = qtA;
		}

		QuadTreeImpl.QuadTreeIteratorImpl qtIterA = quadTreeA.getIterator();

        QuadTreeImpl.QuadTreeIteratorImpl qtIterPathsA = null;
        if (quadTreePathsA != null)
            qtIterPathsA = quadTreePathsA.getIterator();

		Point2D ptB = new Point2D(), closest = new Point2D();
		double toleranceSq = tolerance * tolerance;

        // Helper method to check contains each point
        // Returns false if any point is not covered when b_intersects_all is true
        // or returns true if any point is covered when b_intersects_all is false
        class PointCoverageChecker {
            boolean checkPointCoverage(int index) {
                multipoint_b.getXY(index, ptB);

                if (!envInter.contains(ptB)) {
                    // if b_intersects_all and point is outside intersection envelope, skip or return false
                    if (b_intersects_all) {
                        return false;
                    } else {
                        return false; // continue search for any covered point, so this point can't contribute
                    }
                }

                env_b.setCoords(ptB.x, ptB.y, ptB.x, ptB.y);

                if (qtIterPathsA != null) {
                    qtIterPathsA.resetIterator(env_b, tolerance);
                    if (qtIterPathsA.next() == -1)
                        return false;
                }

                qtIterA.resetIterator(env_b, tolerance);

                for (int elementHandleA = qtIterA.next(); elementHandleA != -1; elementHandleA = qtIterA
                        .next()) {
                    int vertex_a = quadTreeA.getElement(elementHandleA);
                    segIterA.resetToVertex(vertex_a);
                    Segment segmentA = segIterA.nextSegment();

                    double t = segmentA.getClosestCoordinate(ptB, false);
                    segmentA.getCoord2D(t, closest);

                    if (Point2D.sqrDistance(closest, ptB) <= toleranceSq) {
                        return true;
                    }
                }
                return false;
            }
        }

        PointCoverageChecker checker = new PointCoverageChecker();

		if (b_intersects_all) {
            for (int i = 0; i < multipoint_b.getPointCount(); i++) {
                if (!checker.checkPointCoverage(i)) {
                    return false;
                }
            }
            return true;
        }
        else {
            for (int i = 0; i < multipoint_b.getPointCount(); i++) {
                if (checker.checkPointCoverage(i)) {
                    return true;
                }
            }
            return false;
        }
	}
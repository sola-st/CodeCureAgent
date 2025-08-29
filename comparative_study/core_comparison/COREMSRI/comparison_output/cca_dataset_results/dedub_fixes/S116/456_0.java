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
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.esri.core.geometry;

class SweepMonkierComparator extends Treap.MonikerComparator {
	protected EditShape shape;
	protected boolean bIntersectionDetected;
	protected Point2D pointOfInterest;
	protected Line line1;
	protected Envelope1D env;
	protected int vertex1;
	protected int currentNode;
	protected double minDist;
	protected double tolerance;

	SweepMonkierComparator(EditShape shape, double tol) {
		this.shape = shape;
		tolerance = tol;
		bIntersectionDetected = false;
		vertex1 = -1;
		env = new Envelope1D();
		pointOfInterest = new Point2D();
		pointOfInterest.setNaN();
		line1 = new Line();
		currentNode = -1;
		minDist = NumberUtils.doubleMax();
	}

	int getCurrentNode() {
		return currentNode;
	}

	// Makes the comparator to forget about the last detected intersection.
	// Need to be called after the intersection has been resolved.
	void clearIntersectionDetectedFlag() {
		bIntersectionDetected = false;
		minDist = NumberUtils.doubleMax();
	}

	// Returns True if there has been intersection detected during compare call.
	// Once intersection is detected subsequent calls to compare method do
	// nothing until clear_intersection_detected_flag is called.
	boolean intersectionDetected() {
		return bIntersectionDetected;
	}

	void setPoint(Point2D pt) {
		pointOfInterest.setCoords(pt);
	}

	// Compares the moniker, contained in the Moniker_comparator with the
	// element contained in the given node.
	@Override
	int compare(Treap treap, int node) {
		int vertex = treap.getElement(node);
		return compareVertex_(treap, node, vertex);
	}

	protected int compareVertex_(Treap treap, int node, int vertex) {
		boolean bCurve = shape.getSegment(vertex) != null;
		if (!bCurve) {
			shape.queryLineConnector(vertex, line1);
			env.setCoordsNoNaN_(line1.getStartX(), line1.getEndX());
		}

		if (bCurve) {
			throw new GeometryException("not implemented");
		}

		if (pointOfInterest.x + tolerance < env.vmin)
			return -1;

		if (pointOfInterest.x - tolerance > env.vmax)
			return 1;

		if (line1.getStartY() == line1.getEndY()) {
			currentNode = node;
			bIntersectionDetected = true;
			return 0;
		}

		line1.orientBottomUp_();
		Point2D start = line1.getStartXY();
		Point2D vector = new Point2D();
		vector.sub(line1.getEndXY(), start);
		vector.rightPerpendicular();
		Point2D v2 = new Point2D();
		v2.sub(pointOfInterest, start);
		double dot = vector.dotProduct(v2);
		dot /= vector.length();
		if (dot < -tolerance * 10)
			return -1;
		if (dot > tolerance * 10)
			return 1;

		if (line1.isIntersecting(pointOfInterest, tolerance)) {
			double absDot = Math.abs(dot);
			if (absDot < minDist) {
				currentNode = node;
				minDist = absDot;
			}
			bIntersectionDetected = true;
			if (absDot < 0.25 * tolerance)
				return 0;
		}

		return dot < 0 ? -1 : 1;
	}
}

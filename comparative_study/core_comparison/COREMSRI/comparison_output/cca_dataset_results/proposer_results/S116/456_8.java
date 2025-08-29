```java
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
	protected EditShape mShape;
	protected boolean mBIntersectionDetected;
	protected Point2D mPointOfInterest;
	protected Line mLine1;
	protected Envelope1D mEnv;
	protected int mVertex1;
	protected int mCurrentNode;
	protected double mMinDist;
	protected double mTolerance;

	SweepMonkierComparator(EditShape shape, double tol) {
		mShape = shape;
		mTolerance = tol;
		mBIntersectionDetected = false;
		mVertex1 = -1;
		mEnv = new Envelope1D();
		mPointOfInterest = new Point2D();
		mPointOfInterest.setNaN();
		mLine1 = new Line();
		mCurrentNode = -1;
		mMinDist = NumberUtils.doubleMax();
	}

	int getCurrentNode() {
		return mCurrentNode;
	}

	// Makes the comparator to forget about the last detected intersection.
	// Need to be called after the intersection has been resolved.
	void clearIntersectionDetectedFlag() {
		mBIntersectionDetected = false;
		mMinDist = NumberUtils.doubleMax();
	}

	// Returns True if there has been intersection detected during compare call.
	// Once intersection is detected subsequent calls to compare method do
	// nothing until clear_intersection_detected_flag is called.
	boolean intersectionDetected() {
		return mBIntersectionDetected;
	}

	void setPoint(Point2D pt) {
		mPointOfInterest.setCoords(pt);
	}

	// Compares the moniker, contained in the Moniker_comparator with the
	// element contained in the given node.
	@Override
	int compare(Treap treap, int node) {
		int vertex = treap.getElement(node);
		return compareVertex_(treap, node, vertex);
	}

	protected int compareVertex_(Treap treap, int node, int vertex) {
		boolean bCurve = mShape.getSegment(vertex) != null;
		if (!bCurve) {
			mShape.queryLineConnector(vertex, mLine1);
			mEnv.setCoordsNoNaN_(mLine1.getStartX(), mLine1.getEndX());
		}

		if (bCurve) {
			throw new GeometryException("not implemented");
		}

		if (mPointOfInterest.x + mTolerance < mEnv.vmin)
			return -1;

		if (mPointOfInterest.x - mTolerance > mEnv.vmax)
			return 1;

		if (mLine1.getStartY() == mLine1.getEndY()) {
			mCurrentNode = node;
			mBIntersectionDetected = true;
			return 0;
		}

		mLine1.orientBottomUp_();
		Point2D start = mLine1.getStartXY();
		Point2D vector = new Point2D();
		vector.sub(mLine1.getEndXY(), start);
		vector.rightPerpendicular();
		Point2D v2 = new Point2D();
		v2.sub(mPointOfInterest, start);
		double dot = vector.dotProduct(v2);
		dot /= vector.length();
		if (dot < -mTolerance * 10)
			return -1;
		if (dot > mTolerance * 10)
			return 1;

		if (mLine1.isIntersecting(mPointOfInterest, mTolerance)) {
			double absDot = Math.abs(dot);
			if (absDot < mMinDist) {
				mCurrentNode = node;
				mMinDist = absDot;
			}
			mBIntersectionDetected = true;
			if (absDot < 0.25 * mTolerance)
				return 0;
		}

		return dot < 0 ? -1 : 1;
	}
}


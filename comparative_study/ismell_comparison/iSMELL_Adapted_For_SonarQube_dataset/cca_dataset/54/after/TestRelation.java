/*
 Copyright 1995-2017 Esri

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

import java.io.IOException;

import junit.framework.TestCase;

import org.junit.Test;

import com.esri.core.geometry.Geometry.GeometryAccelerationDegree;

public class TestRelation extends TestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void testCreation() {
		{
			OperatorFactoryLocal projEnv = OperatorFactoryLocal.getInstance();
			SpatialReference inputSR = SpatialReference.create(3857);

			Polygon poly1 = new Polygon();
			Envelope2D env1 = new Envelope2D();
			env1.setCoords(855277, 3892059, 855277 + 100, 3892059 + 100);
			poly1.addEnvelope(env1, false);

			Polygon poly2 = new Polygon();
			Envelope2D env2 = new Envelope2D();
			env2.setCoords(855277, 3892059, 855277 + 300, 3892059 + 200);
			poly2.addEnvelope(env2, false);

			{
				OperatorEquals operatorEquals = (OperatorEquals) (projEnv
						.getOperator(Operator.Type.Equals));
				boolean result = operatorEquals.execute(poly1, poly2, inputSR,
						null);
				assertTrue(!result);
				Polygon poly11 = new Polygon();
				poly1.copyTo(poly11);
				result = operatorEquals.execute(poly1, poly11, inputSR, null);
				assertTrue(result);
			}
			{
				OperatorCrosses operatorCrosses = (OperatorCrosses) (projEnv
						.getOperator(Operator.Type.Crosses));
				boolean result = operatorCrosses.execute(poly1, poly2, inputSR,
						null);
				assertTrue(!result);
			}
			{
				OperatorWithin operatorWithin = (OperatorWithin) (projEnv
						.getOperator(Operator.Type.Within));
				boolean result = operatorWithin.execute(poly1, poly2, inputSR,
						null);
				assertTrue(result);
			}

			{
				OperatorDisjoint operatorDisjoint = (OperatorDisjoint) (projEnv
						.getOperator(Operator.Type.Disjoint));
				OperatorIntersects operatorIntersects = (OperatorIntersects) (projEnv
						.getOperator(Operator.Type.Intersects));
				boolean result = operatorDisjoint.execute(poly1, poly2,
						inputSR, null);
				assertTrue(!result);
				{
					result = operatorIntersects.execute(poly1, poly2, inputSR,
							null);
					assertTrue(result);
				}
			}

			{
				OperatorDisjoint operatorDisjoint = (OperatorDisjoint) (projEnv
						.getOperator(Operator.Type.Disjoint));
				OperatorIntersects operatorIntersects = (OperatorIntersects) (projEnv
						.getOperator(Operator.Type.Intersects));
				Envelope2D env2D = new Envelope2D();
				poly2.queryEnvelope2D(env2D);
				Envelope envelope = new Envelope(env2D);
				boolean result = operatorDisjoint.execute(envelope, poly2,
						inputSR, null);
				assertTrue(!result);
				{
					result = operatorIntersects.execute(envelope, poly2,
							inputSR, null);
					assertTrue(result);
				}
			}

			testPolygonMultiEnvelope(projEnv, inputSR);
			
			{
				OperatorTouches operatorTouches = (OperatorTouches) (projEnv
						.getOperator(Operator.Type.Touches));
				boolean result = operatorTouches.execute(poly1, poly2, inputSR,
						null);
				assertTrue(!result);
			}

		}
	}

	private void testPolygonMultiEnvelope(OperatorFactoryLocal projEnv, SpatialReference inputSR) {
		// Extracted method for the nested code block (remove nested code block per S1199)

		OperatorDisjoint operatorDisjoint = (OperatorDisjoint) (projEnv
				.getOperator(Operator.Type.Disjoint));
		OperatorIntersects operatorIntersects = (OperatorIntersects) (projEnv
				.getOperator(Operator.Type.Intersects));
		Polygon poly = new Polygon();

		Envelope2D env2D = new Envelope2D();
		env2D.setCoords(855277, 3892059, 855277 + 100, 3892059 + 100);
		poly.addEnvelope(env2D, false);
		env2D.setCoords(855277 + 10, 3892059 + 10, 855277 + 90,
				3892059 + 90);
		poly.addEnvelope(env2D, true);

		env2D.setCoords(855277 + 20, 3892059 + 20, 855277 + 200,
				3892059 + 80);
		Envelope envelope = new Envelope(env2D);
		boolean result = operatorDisjoint.execute(envelope, poly,
				inputSR, null);
		assertTrue(!result);
		{
			result = operatorIntersects.execute(envelope, poly,
					inputSR, null);
			assertTrue(result);
		}
	}

	@Test
	public void testOperatorDisjoint() {
		{
			OperatorFactoryLocal projEnv = OperatorFactoryLocal.getInstance();
			SpatialReference inputSR = SpatialReference.create(3857);

			Polygon poly1 = new Polygon();
			Envelope2D env1 = new Envelope2D();
			env1.setCoords(855277, 3892059, 855277 + 100, 3892059 + 100);
			poly1.addEnvelope(env1, false);

			Polygon poly2 = new Polygon();
			Envelope2D env2 = new Envelope2D();
			env2.setCoords(855277, 3892059, 855277 + 300, 3892059 + 200);
			poly2.addEnvelope(env2, false);

			Polygon poly3 = new Polygon();
			Envelope2D env3 = new Envelope2D();
			env3.setCoords(855277 + 100, 3892059 + 100, 855277 + 100 + 100,
					3892059 + 100 + 100);
			poly3.addEnvelope(env3, false);

			Polygon poly4 = new Polygon();
			Envelope2D env4 = new Envelope2D();
			env4.setCoords(855277 + 200, 3892059 + 200, 855277 + 200 + 100,
					3892059 + 200 + 100);
			poly4.addEnvelope(env4, false);

			Point point1 = new Point(855277, 3892059);
			Point point2 = new Point(855277 + 2, 3892059 + 3);
			Point point3 = new Point(855277 - 2, 3892059 - 3);

			{
				OperatorDisjoint operatorDisjoint = (OperatorDisjoint) (projEnv
						.getOperator(Operator.Type.Disjoint));
				boolean result = operatorDisjoint.execute(poly1, poly2,
						inputSR, null);
				assertTrue(!result);
				result = operatorDisjoint.execute(poly1, poly3, inputSR, null);
				assertTrue(!result);
				result = operatorDisjoint.execute(poly1, poly4, inputSR, null);
				assertTrue(result);

				result = operatorDisjoint.execute(poly1, point1, inputSR, null);
				assertTrue(!result);
				result = operatorDisjoint.execute(point1, poly1, inputSR, null);
				assertTrue(!result);
				result = operatorDisjoint.execute(poly1, point2, inputSR, null);
				assertTrue(!result);
				result = operatorDisjoint.execute(point2, poly1, inputSR, null);
				assertTrue(!result);
				result = operatorDisjoint.execute(poly1, point3, inputSR, null);
				assertTrue(result);
				result = operatorDisjoint.execute(point3, poly1, inputSR, null);
				assertTrue(result);
			}
		}
	}

	// All other unchanged tests continue here...
	// ...

	// The rest of the class remains unchanged after here.
}
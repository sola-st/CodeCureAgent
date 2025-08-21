/*
 * Copyright (c) 2016 Vivid Solutions.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.io.gml2;

import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.gml2.GMLHandler.Handler;
import org.locationtech.jts.util.StringUtil;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;


/**
 * Container for GML2 Geometry parsing strategies which can be represented in JTS.
 *
 * @author David Zwiers, Vivid Solutions.
 */
public class GeometryStrategies{

	/**
	 * This set of strategies is not expected to be used directly outside of this distribution.
	 * 
	 * The implementation of this class are intended to be used as static function points in C. These strategies should be associated with an element when the element begins. The strategy is utilized at the end of the element to create an object of value to the user. 
	 * 
	 * In this case all the objects are either java.lang.* or JTS Geometry objects
	 *
	 * @author David Zwiers, Vivid Solutions.
	 */
	static interface ParseStrategy{
		/**
		 * @param arg Value to interpret
		 * @param gf GeometryFactory
		 * @return The interpreted value
		 * @throws SAXException 
		 */
		Object parse(Handler arg, GeometryFactory gf) throws SAXException;
	}
	
	private static HashMap strategies = loadStrategies();
	private static HashMap loadStrategies(){
		HashMap strats = new HashMap();
		
		// point
		strats.put(GMLConstants.GML_POINT.toLowerCase(),new ParseStrategy(){

			public Object parse(Handler arg, GeometryFactory gf) throws SAXException {
				// one child, either a coord
				// or a coordinate sequence
				
				if(arg.children.size()!=1)
					throw new SAXException("Cannot create a point without exactly one coordinate");

				int srid = getSrid(arg.attrs,gf.getSRID());

				Object c = arg.children.get(0);
				Point p = null;
				if(c instanceof Coordinate){
					p = gf.createPoint((Coordinate)c);
				}else{
					p = gf.createPoint((CoordinateSequence)c);
				}
				if(p.getSRID()!=srid)
					p.setSRID(srid);
				
				return p;
			}
		});
		
		// linestring
		strats.put(GMLConstants.GML_LINESTRING.toLowerCase(),new ParseStrategy(){

			public Object parse(Handler arg, GeometryFactory gf) throws SAXException {
				// one child, either a coord
				// or a coordinate sequence
				
				if(arg.children.size()<1)
					throw new SAXException("Cannot create a linestring without atleast two coordinates or one coordinate sequence");

				int srid = getSrid(arg.attrs,gf.getSRID());
				
				LineString ls = null;
				if(arg.children.size() == 1){
					// coord set
					try{
						CoordinateSequence cs = (CoordinateSequence) arg.children.get(0);
						ls = gf.createLineString(cs);
					}catch(ClassCastException e){
						throw new SAXException("Cannot create a linestring without atleast two coordinates or one coordinate sequence",e);
					}
				}else{
					try{
						Coordinate[] coords = (Coordinate[]) arg.children.toArray(new Coordinate[arg.children.size()]);
						ls = gf.createLineString(coords);
					}catch(ClassCastException e){
						throw new SAXException("Cannot create a linestring without atleast two coordinates or one coordinate sequence",e);
					}
				}
				
				if(ls.getSRID()!=srid)
					ls.setSRID(srid);
				
				return ls;
			}
		});
		
		// linearring
		strats.put(GMLConstants.GML_LINEARRING.toLowerCase(),new ParseStrategy(){

			public Object parse(Handler arg, GeometryFactory gf) throws SAXException {
				// one child, either a coord
				// or a coordinate sequence
				
				if(arg.children.size()!=1 && arg.children.size()<4)
					throw new SAXException("Cannot create a linear ring without atleast four coordinates or one coordinate sequence");

				int srid = getSrid(arg.attrs,gf.getSRID());
				
				LinearRing ls = null;
				if(arg.children.size() == 1){
					// coord set
					try{
						CoordinateSequence cs = (CoordinateSequence) arg.children.get(0);
						ls = gf.createLinearRing(cs);
					}catch(ClassCastException e){
						throw new SAXException("Cannot create a linear ring without atleast four coordinates or one coordinate sequence",e);
					}
				}else{
					try{
						Coordinate[] coords = (Coordinate[]) arg.children.toArray(new Coordinate[arg.children.size()]);
						ls = gf.createLinearRing(coords);
					}catch(ClassCastException e){
						throw new SAXException("Cannot create a linear ring without atleast four coordinates or one coordinate sequence",e);
					}
				}
				
				if(ls.getSRID()!=srid)
					ls.setSRID(srid);
				
				return ls;
			}
		});
		
		// polygon
		strats.put(GMLConstants.GML_POLYGON.toLowerCase(),new ParseStrategy(){

			public Object parse(Handler arg, GeometryFactory gf) throws SAXException {
				// one child, either a coord
				// or a coordinate sequence
				
				if(arg.children.size()<1)
					throw new SAXException("Cannot create a polygon without atleast one linear ring");

				int srid = getSrid(arg.attrs,gf.getSRID());
				
				LinearRing outer = (LinearRing) arg.children.get(0); // will be the first
				List t = arg.children.size()>1?arg.children.subList(1,arg.children.size()):null;
				LinearRing[] inner = t==null?null:(LinearRing[]) t.toArray(new LinearRing[t.size()]);
				
				Polygon p = gf.createPolygon(outer,inner);
				
				if(p.getSRID()!=srid)
					p.setSRID(srid);
				
				return p;
			}
		});
		
		// box
		strats.put(GMLConstants.GML_BOX.toLowerCase(),new ParseStrategy(){

			public Object parse(Handler arg, GeometryFactory gf) throws SAXException {
				// one child, either a coord
				// or a coordinate sequence
				
				if(arg.children.size()<1 || arg.children.size()>2)
					throw new SAXException("Cannot create a box without either two coords or one coordinate sequence");

//				int srid = getSrid(arg.attrs,gf.getSRID());
				
				Envelope box = null;
				if(arg.children.size() == 1){
					CoordinateSequence cs = (CoordinateSequence) arg.children.get(0);
					box = cs.expandEnvelope(new Envelope());
```java
				c.x = axis[0];  // unboxed automatically
				if(axis.length>1)
					c.y = axis[1]; // unboxed automatically
				if(axis.length>2)
					c.setZ(axis[2]); // unboxed automatically

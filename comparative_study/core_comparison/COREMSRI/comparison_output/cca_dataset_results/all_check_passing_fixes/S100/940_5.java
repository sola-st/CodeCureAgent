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

final class WktParser {
	interface WktToken {
		static final int not_available = 0;
		static final int empty = 50;
		static final int left_paren = 51;
		static final int right_paren = 52;
		static final int x_literal = 0x80000000;
		static final int y_literal = 0x40000000;
		static final int z_literal = 0x20000000;
		static final int m_literal = 0x10000000;
		static final int point = 1;
		static final int linestring = 2;
		static final int polygon = 3;
		static final int multipoint = 4;
		static final int multilinestring = 5;
		static final int multipolygon = 6;
		static final int geometrycollection = 7;
		static final int attribute_z = 1000;
		static final int attribute_m = 2000;
		static final int attribute_zm = 3000;
	}

	WktParser() {
	}

	WktParser(String string) {
		resetParser(string);
	}

	void resetParser(String string) {
		if (m_function_stack == null)
			m_function_stack = new AttributeStreamOfInt32(0);

		reset_();
		m_wkt_string = string;
	}

	int nextToken() {
		switch (m_function_stack.getLast()) {
		case State.xLiteral:
			xLiteral_();
			break;
		case State.yLiteral:
			yLiteral_();
			break;
		case State.zLiteral:
			zLiteral_();
			break;
		case State.mLiteral:
			mLiteral_();
			break;
		case State.pointStart:
			pointStart_();
			break;
		case State.pointStartAlt:
			pointStartAlt_();
			break;
		case State.pointEnd:
			pointEnd_();
			break;
		case State.lineStringStart:
			lineStringStart_();
			break;
		case State.lineStringEnd:
			lineStringEnd_();
			break;
		case State.multiPointStart:
			multiPointStart_();
			break;
		case State.multiPointEnd:
			multiPointEnd_();
			break;
		case State.polygonStart:
			polygonStart_();
			break;
		case State.polygonEnd:
			polygonEnd_();
			break;
		case State.multiLineStringStart:
			multiLineStringStart_();
			break;
		case State.multiLineStringEnd:
			multiLineStringEnd_();
			break;
		case State.multiPolygonStart:
			multiPolygonStart_();
			break;
		case State.multiPolygonEnd:
			multiPolygonEnd_();
			break;
		case State.geometryCollectionStart:
			geometryCollectionStart_();
			break;
		case State.geometryCollectionEnd:
			geometryCollectionEnd_();
			break;
		case State.accept:
			accept_();
			break;
		case State.geometry:
			geometry_();
			break;
		case State.attributes:
			attributes_();
			break;
		}

		return m_current_token_type;
	}

	double currentNumericLiteral() {
		if (((int) m_current_token_type & (int) Number.signed_numeric_literal) == 0)
			throw new GeometryException("runtime error");

		if (m_b_nan)
			return NumberUtils.TheNaN;

		double value = Double.parseDouble(m_wkt_string.substring(m_start_token,
				m_end_token));
		return value;
	}

	int currentToken() {
		return m_current_token_type;
	}

	boolean hasZs() {
		return m_b_has_zs;
	}

	boolean hasMs() {
		return m_b_has_ms;
	}

	private String m_wkt_string;
	private int m_start_token;
	private int m_end_token;
	private int m_current_token_type;

	private boolean m_b_has_zs;
	private boolean m_b_has_ms;
	private boolean m_b_check_consistent_attributes;
	private boolean m_b_nan;

	private AttributeStreamOfInt32 m_function_stack;

	private interface State {
		static final int xLiteral = 0;
		static final int yLiteral = 1;
		static final int zLiteral = 2;
		static final int mLiteral = 3;
		static final int pointStart = 4;
		static final int pointStartAlt = 5;
		static final int pointEnd = 6;
		static final int lineStringStart = 7;
		static final int lineStringEnd = 8;
		static final int multiPointStart = 9;
		static final int multiPointEnd = 10;
		static final int polygonStart = 11;
		static final int polygonEnd = 12;
		static final int multiLineStringStart = 13;
		static final int multiLineStringEnd = 14;
		static final int multiPolygonStart = 15;
		static final int multiPolygonEnd = 16;
		static final int geometryCollectionStart = 17;
		static final int geometryCollectionEnd = 18;
		static final int accept = 19;
		static final int geometry = 20;
		static final int attributes = 21;
	}

	private interface Number {
		static final int signed_numeric_literal = WktToken.x_literal
				| WktToken.y_literal | WktToken.z_literal | WktToken.m_literal;
	}

	private void reset() {
		m_function_stack.add(State.accept);
		m_function_stack.add(State.geometry);
		m_start_token = -1;
		m_end_token = 0;
		m_current_token_type = WktToken.not_available;
		m_b_has_zs = false;
		m_b_has_ms = false;
		m_b_check_consistent_attributes = false;
		m_b_nan = false;
	}

	private void accept() {
		m_start_token = m_end_token;
		m_current_token_type = WktToken.not_available;
	}

	private void geometry() {
		skipWhiteSpace();
		m_start_token = m_end_token;
		m_function_stack.removeLast();

		if (m_start_token + 5 <= m_wkt_string.length()
				&& m_wkt_string.regionMatches(true, m_start_token, "point", 0,
						5)) {
			m_end_token = m_start_token + 5;
			m_current_token_type = WktToken.point;
			m_function_stack.add(State.pointStart);
		} else if (m_start_token + 10 <= m_wkt_string.length()
				&& m_wkt_string.regionMatches(true, m_start_token,
						"linestring", 0, 10)) {
			m_end_token = m_start_token + 10;
			m_current_token_type = WktToken.linestring;
			m_function_stack.add(State.lineStringStart);
		} else if (m_start_token + 10 <= m_wkt_string.length()
				&& m_wkt_string.regionMatches(true, m_start_token,
						"multipoint", 0, 10)) {
			m_end_token = m_start_token + 10;
			m_current_token_type = WktToken.multipoint;
			m_function_stack.add(State.multiPointStart);
		} else if (m_start_token + 7 <= m_wkt_string.length()
				&& m_wkt_string.regionMatches(true, m_start_token, "polygon",
						0, 7)) {
			m_end_token = m_start_token + 7;
			m_current_token_type = WktToken.polygon;
			m_function_stack.add(State.polygonStart);
		} else if (m_start_token + 15 <= m_wkt_string.length()
				&& m_wkt_string.regionMatches(true, m_start_token,
						"multilinestring", 0, 15)) {
			m_end_token = m_start_token + 15;
			m_current_token_type = WktToken.multilinestring;
			m_function_stack.add(State.multiLineStringStart);
		} else if (m_start_token + 12 <= m_wkt_string.length()
				&& m_wkt_string.regionMatches(true, m_start_token,
						"multipolygon", 0, 12)) {
			m_end_token = m_start_token + 12;
			m_current_token_type = WktToken.multipolygon;
			m_function_stack.add(State.multiPolygonStart);
		} else if (m_start_token + 18 <= m_wkt_string.length()
				&& m_wkt_string.regionMatches(true, m_start_token,
						"geometrycollection", 0, 18)) {
			m_end_token = m_start_token + 18;
			m_current_token_type = WktToken.geometrycollection;
			m_function_stack.add(State.geometryCollectionStart);
		} else {
			//String snippet = (m_wkt_string.length() > 200 ? m_wkt_string
			//		.substring(0, 200) + "..." : m_wkt_string);
			//throw new IllegalArgumentException(
			//		"Could not parse Well-Known Text: " + snippet);
			throw new IllegalArgumentException(
					"Could not parse Well-Known Text around position: " + m_end_token);
		}

		m_function_stack.add(State.attributes);
	}

	private void attributes() {
		skipWhiteSpace();
		m_start_token = m_end_token;
		m_function_stack.removeLast();

		// Z and M is not allowed to have a space between them
		boolean b_has_zs = false, b_has_ms = false;

		if (m_wkt_string.charAt(m_end_token) == 'z'
				|| m_wkt_string.charAt(m_end_token) == 'Z') {
			b_has_zs = true;

			if (++m_end_token >= m_wkt_string.length())
				throw new IllegalArgumentException();
		}

		if (m_wkt_string.charAt(m_end_token) == 'm'
				|| m_wkt_string.charAt(m_end_token) == 'M') {
			b_has_ms = true;

			if (++m_end_token >= m_wkt_string.length())
				throw new IllegalArgumentException();
		}

		if (m_b_check_consistent_attributes) {
			if (b_has_zs != m_b_has_zs || b_has_ms != m_b_has_ms)
				throw new IllegalArgumentException();
		}

		m_b_has_zs = b_has_zs;
		m_b_has_ms = b_has_ms;

		if (m_b_has_zs || m_b_has_ms) {
			if (m_b_has_zs && !m_b_has_ms)
				m_current_token_type = WktToken.attribute_z;
			else if (m_b_has_ms && !m_b_has_zs)
				m_current_token_type = WktToken.attribute_m;
			else
				m_current_token_type = WktToken.attribute_zm;
		} else {
			nextToken();
		}
	}

	private void geometryCollectionStart() {
		skipWhiteSpace();
		m_start_token = m_end_token;
		m_b_check_consistent_attributes = true;

		if (empty()) {
			m_function_stack.removeLast();
		} else if (leftParen()) {
			m_function_stack.removeLast();
			m_function_stack.add(State.geometryCollectionEnd);
			m_function_stack.add(State.geometry);
		} else {
			throw new IllegalArgumentException();
		}
	}

	private void geometryCollectionEnd() {
		skipWhiteSpace();
		m_start_token = m_end_token;

		if (comma()) {
			m_function_stack.add(State.geometry);
			geometry();
		} else if (rightParen()) {
			m_function_stack.removeLast();
		} else {
			throw new IllegalArgumentException();
		}
	}

	private void multiPolygonStart() {
		skipWhiteSpace();
		m_start_token = m_end_token;

		if (empty()) {
			m_function_stack.removeLast();
		} else if (leftParen()) {
			m_function_stack.removeLast();
			m_function_stack.add(State.multiPolygonEnd);
			m_function_stack.add(State.polygonStart);
		} else {
			throw new IllegalArgumentException();
		}
	}

	private void multiPolygonEnd() {
		skipWhiteSpace();
		m_start_token = m_end_token;

		if (comma()) {
			m_function_stack.add(State.polygonStart);
			polygonStart();
		} else if (rightParen()) {
			m_function_stack.removeLast();
		} else {
			throw new IllegalArgumentException();
		}
	}

	private void multiLineStringStart() {
		skipWhiteSpace();
		m_start_token = m_end_token;

		if (empty()) {
			m_function_stack.removeLast();
		} else if (leftParen()) {
			m_function_stack.removeLast();
			m_function_stack.add(State.multiLineStringEnd);
			m_function_stack.add(State.lineStringStart);
		} else {
			throw new IllegalArgumentException();
		}
	}

	private void multiLineStringEnd() {
		skipWhiteSpace();
		m_start_token = m_end_token;

		if (comma()) {
			m_function_stack.add(State.lineStringStart);
			lineStringStart();
		} else if (rightParen()) {
			m_function_stack.removeLast();
		} else {
			throw new IllegalArgumentException();
		}
	}

	private void multiPointStart() {
		skipWhiteSpace();
		m_start_token = m_end_token;

		if (empty()) {
			m_function_stack.removeLast();
		} else if (leftParen()) {
			m_function_stack.removeLast();
			m_function_stack.add(State.multiPointEnd);
			m_function_stack.add(State.pointStartAlt);
		} else {
			throw new IllegalArgumentException();
		}
	}

	private void multiPointEnd() {
		skipWhiteSpace();
		m_start_token = m_end_token;

		if (comma()) {
			m_function_stack.add(State.pointStart);
			pointStart();
		} else if (rightParen()) {
			m_function_stack.removeLast();
		} else {
			throw new IllegalArgumentException();
		}
	}

	private void polygonStart() {
		skipWhiteSpace();
		m_start_token = m_end_token;

		if (empty()) {
			m_function_stack.removeLast();
		} else if (leftParen()) {
			m_function_stack.removeLast();
			m_function_stack.add(State.polygonEnd);
			m_function_stack.add(State.lineStringStart);
		} else {
			throw new IllegalArgumentException();
		}
	}

	private void polygonEnd() {
		skipWhiteSpace();
		m_start_token = m_end_token;

		if (comma()) {
			m_function_stack.add(State.lineStringStart);
			lineStringStart();
		} else if (rightParen()) {
			m_function_stack.removeLast();
		} else {
			throw new IllegalArgumentException();
		}
	}

	private void lineStringStart() {
		skipWhiteSpace();
		m_start_token = m_end_token;

		if (empty()) {
			m_function_stack.removeLast();
		} else if (leftParen()) {
			m_function_stack.removeLast();
			m_function_stack.add(State.lineStringEnd);
			m_function_stack.add(State.xLiteral);
		} else {
			throw new IllegalArgumentException();
		}
	}

	private void lineStringEnd() {
		skipWhiteSpace();
		m_start_token = m_end_token;

		if (comma()) {
			m_function_stack.add(State.xLiteral);
			xLiteral();
		} else if (rightParen()) {
			m_function_stack.removeLast();
		} else {
			throw new IllegalArgumentException();
		}
	}

	private void pointStart() {
		skipWhiteSpace();
		m_start_token = m_end_token;

		if (empty()) {
			m_function_stack.removeLast();
		} else if (leftParen()) {
			m_function_stack.removeLast();
			m_function_stack.add(State.pointEnd);
			m_function_stack.add(State.xLiteral);
		} else {
			throw new IllegalArgumentException();
		}
	}

	private void pointStartAlt() {
		skipWhiteSpace();
		m_start_token = m_end_token;

		if (empty()) {// ogc standard
			m_function_stack.removeLast();
		} else if (leftParen()) {// ogc standard
			m_function_stack.removeLast();
			m_function_stack.add(State.pointEnd);
			m_function_stack.add(State.xLiteral);
		} else {// not ogc standard. treat as linestring
			m_function_stack.removeLast();
			m_function_stack.removeLast();
			m_function_stack.add(State.lineStringEnd);
			m_function_stack.add(State.xLiteral);
			nextToken();
		}
	}

	private void pointEnd() {
		skipWhiteSpace();
		m_start_token = m_end_token;

		if (rightParen()) {
			m_function_stack.removeLast();
		} else {
			throw new IllegalArgumentException();
		}
	}

	private void xLiteral() {
		signedNumericLiteral();
		m_current_token_type = WktToken.x_literal;
		m_function_stack.removeLast();
		m_function_stack.add(State.yLiteral);
	}

	private void yLiteral() {
		signedNumericLiteral();
		m_current_token_type = WktToken.y_literal;
		m_function_stack.removeLast();

		if (m_b_has_zs)
			m_function_stack.add(State.zLiteral);
		else if (m_b_has_ms)
			m_function_stack.add(State.mLiteral);
	}

	private void zLiteral() {
		signedNumericLiteral();
		m_current_token_type = WktToken.z_literal;
		m_function_stack.removeLast();

		if (m_b_has_ms)
			m_function_stack.add(State.mLiteral);
	}

	private void mLiteral() {
		signedNumericLiteral();
		m_current_token_type = WktToken.m_literal;
		m_function_stack.removeLast();
	}

	private boolean nan() {
		if (m_wkt_string.regionMatches(true, m_start_token, "nan", 0, 3)) {
			m_end_token += 3;
			m_b_nan = true;
			return true;
		}

		m_b_nan = false;
		return false;
	}

	private void sign() {
		// Optional - or + sign
		if (m_wkt_string.charAt(m_end_token) == '-'
				|| m_wkt_string.charAt(m_end_token) == '+') {
			if (++m_end_token >= m_wkt_string.length())
				throw new IllegalArgumentException();
		}
	}

	private void signedNumericLiteral() {
		skipWhiteSpace();
		m_start_token = m_end_token;

		if (nan())
			return;

		sign(); // Optional
		unsignedNumericLiteral();
	}

	private void unsignedNumericLiteral() {
		exactNumericLiteral();
		exp(); // Optional
	}

	private void exactNumericLiteral() {
		if (Character.isDigit(m_wkt_string.charAt(m_end_token))) {
			digits();

			// Optional
			if (m_wkt_string.charAt(m_end_token) == '.') {
				if (++m_end_token >= m_wkt_string.length())
					throw new IllegalArgumentException();

				// Optional
				if (Character.isDigit(m_wkt_string.charAt(m_end_token)))
					digits();
			}
		} else if (m_wkt_string.charAt(m_end_token) == '.') {
			if (++m_end_token >= m_wkt_string.length())
				throw new IllegalArgumentException();

			if (!Character.isDigit(m_wkt_string.charAt(m_end_token)))
				throw new IllegalArgumentException();

			digits();
		} else {
			throw new IllegalArgumentException();
		}
	}

	private void digits() {
		do {
			if (++m_end_token >= m_wkt_string.length())
				throw new IllegalArgumentException();

		} while (Character.isDigit(m_wkt_string.charAt(m_end_token)));
	}

	private void exp() {
		// This is an optional state
		if (m_wkt_string.charAt(m_end_token) == 'e'
				|| m_wkt_string.charAt(m_end_token) == 'E') {
			if (++m_end_token >= m_wkt_string.length())
				throw new IllegalArgumentException();

			sign(); // optional

			if (!Character.isDigit(m_wkt_string.charAt(m_end_token)))
				throw new IllegalArgumentException();

			digits();
		}
	}

	private void skipWhiteSpace() {
		if (m_end_token >= m_wkt_string.length())
			throw new IllegalArgumentException();

		while (Character.isWhitespace(m_wkt_string.charAt(m_end_token))) {
			if (++m_end_token >= m_wkt_string.length())
				throw new IllegalArgumentException();
		}
	}

	private boolean empty() {
		if (m_wkt_string.regionMatches(true, m_start_token, "empty", 0, 5)) {
			m_end_token += 5;
			m_current_token_type = WktToken.empty;
			return true;
		}

		return false;
	}

	private boolean comma() {
		if (m_wkt_string.charAt(m_end_token) == ',') {
			m_end_token++;
			return true;
		}

		return false;
	}

	private boolean leftParen() {

		if (m_wkt_string.charAt(m_end_token) == '(') {
			m_end_token++;
			m_current_token_type = WktToken.left_paren;
			return true;
		}

		return false;
	}

	private boolean rightParen_() {
		if (m_wkt_string.charAt(m_end_token) == ')') {
			m_end_token++;
			m_current_token_type = WktToken.right_paren;
			return true;
		}

		return false;
	}

}
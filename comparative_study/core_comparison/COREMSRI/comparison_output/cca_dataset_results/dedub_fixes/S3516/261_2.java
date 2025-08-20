@Override
	public boolean tock() {
		if (m_b_done)
			return true;

		if (!m_b_merge) {
			//Do not use tick/tock with the non-merging convex hull.
			//Call tick/next instead,
			//because tick pushes geometry into the cursor, and next performs a single convex hull on it. 
			throw new GeometryException("Invalid call for non merging convex hull.");
		}

		Geometry geometry = m_inputGeometryCursor.next();
		if (geometry != null) {
			m_hull.addGeometry(geometry);
			return true;
		}

		m_b_done = true;
		return false;
	}

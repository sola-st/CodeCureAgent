// [...]
		if (_env_a.getHeight() <= tolerance || _env_a.getWidth() <= tolerance) {// treat
																				// env_a
																				// as
																				// line

			if (_env_b.getHeight() <= tolerance
					|| _env_b.getWidth() <= tolerance) {// treat env_b as line

				Line lineA = new Line(), lineB = new Line();
				double[] scalars_a = new double[2];
				double[] scalars_b = new double[2];
				Point2D pt = new Point2D();
				_env_a.queryLowerLeft(pt);
				lineA.setStartXY(pt);
				_env_a.queryUpperRight(pt);
				lineA.setEndXY(pt);
				_env_b.queryLowerLeft(pt);
				lineB.setStartXY(pt);
				_env_b.queryUpperRight(pt);
				lineB.setEndXY(pt);

				lineA.intersect(lineB, null, scalars_a, scalars_b, tolerance);
				int count = lineA.intersect(lineB, null, null, null,
						tolerance);

				if (count != 1)
					return false;

				return scalars_a[0] == 0.0 || scalars_a[1] == 1.0
						|| scalars_b[0] == 0.0 || scalars_b[1] == 1.0;
			}

			// treat env_b as area

			Envelope2D env_b_deflated = new Envelope2D(), env_inter = new Envelope2D();
			env_b_deflated.setCoords(_env_b);
			env_b_deflated.inflate(-tolerance, -tolerance);
			env_inter.setCoords(env_b_deflated);
			env_inter.intersect(_env_a);

			if (!env_inter.isEmpty()
					&& (env_inter.getHeight() > tolerance || env_inter
							.getWidth() > tolerance))
				return false;

			assert (!envelopeDisjointEnvelope_(_env_a, _env_b, tolerance,
					progress_tracker));
			return true; // we already know they intersect within a tolerance
		}
// [...]
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

import com.esri.core.geometry.VertexDescription.Persistence;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.esri.core.geometry.SizeOf.SIZE_OF_ATTRIBUTE_STREAM_OF_DBL;
import static com.esri.core.geometry.SizeOf.SIZE_OF_ATTRIBUTE_STREAM_OF_INT32;
import static com.esri.core.geometry.SizeOf.sizeOfDoubleArray;

final class AttributeStreamOfDbl extends AttributeStreamBase {

	private double[] m_buffer = null;
	private int m_size;

	public int size() {
		return m_size;
	}

	public void reserve(int reserve)
	{
		if (reserve <= 0)
			return;
		if (m_buffer == null)
			m_buffer = new double[reserve];
		else {
			if (reserve <= m_buffer.length)
				return;
			double[] buf = new double[reserve];
			System.arraycopy(m_buffer, 0, buf, 0, m_size);
			m_buffer = buf;
		}

	}

	public int capacity() {
		return m_buffer != null ? m_buffer.length : 0;
	}
	
	public AttributeStreamOfDbl(int size) {
		if (end > size || (end > sizeOther && (size != sizeOther)))
			return false;

		if (validSize - (index + count) > 0) {
			System.arraycopy(m_buffer, index + count, m_buffer, index,
					validSize - (index + count));
		}
		m_size -= count;
	}

	@Override
	public void readRange(int srcStart, int count, ByteBuffer dst,
			int dstOffset, boolean bForward) {
		if (srcStart < 0 || count < 0 || dstOffset < 0
				|| size() < count + srcStart)
			throw new IllegalArgumentException();

		final int elmSize = NumberUtils.sizeOf((double) 0);

		if (dst.capacity() < (int) (dstOffset + elmSize * count))
			throw new IllegalArgumentException();

		if (count == 0)
			return;

		int j = srcStart;
		if (!bForward)
			j += count - 1;

		final int dj = bForward ? 1 : -1;

		int offset = dstOffset;
		for (int i = 0; i < count; i++, offset += elmSize) {
			dst.putDouble(offset, m_buffer[j]);
			j += dj;
		}
	}

	@Override
	public void reverseRange(int index, int count, int stride) {
		if (m_bReadonly)
			throw new GeometryException("invalid_call");

		if (stride < 1 || count % stride != 0)
			throw new GeometryException("invalid_call");

		int cIterations = count >> 1;
		int n = count;

		for (int i = 0; i < cIterations; i += stride) {
			n -= stride;

			for (int s = 0; s < stride; s++) {
				double temp = m_buffer[index + i + s];
				m_buffer[index + i + s] = m_buffer[index + n + s];
				m_buffer[index + n + s] = temp;
			}
		}
	}

	@Override
	public void setRange(double value, int start, int count) {
		if (start < 0 || count < 0 || start < 0 || count + start > size())
			throw new IllegalArgumentException();

		double v = value;
		Arrays.fill(m_buffer, start, start + count, v);
		// for (int i = start, n = start + count; i < n; i++)
		// write(i, v);
	}

	@Override
	public void writeRange(int startElement, int count,
			AttributeStreamBase _src, int srcStart, boolean bForward, int stride) {
		if (startElement < 0 || count < 0 || srcStart < 0)
			throw new IllegalArgumentException();

		if (!bForward && (stride <= 0 || (count % stride != 0)))
			throw new IllegalArgumentException();

		AttributeStreamOfDbl src = (AttributeStreamOfDbl) _src; // the input
		// type must
		// match

		if (src.size() < (int) (srcStart + count))
			throw new IllegalArgumentException();

		if (count == 0)
			return;

		if (size() < count + startElement)
			resize(count + startElement);

		if (_src == (AttributeStreamBase) this) {
			_selfWriteRangeImpl(startElement, count, srcStart, bForward, stride);
			return;
		}

		if (bForward) {
			int j = startElement;
			int offset = srcStart;
			for (int i = 0; i < count; i++) {
				m_buffer[j] = src.m_buffer[offset];
				j++;
				offset++;
			}
		} else {
			int j = startElement;
			int offset = srcStart + count - stride;
			if (stride == 1) {
				for (int i = 0; i < count; i++) {
					m_buffer[j] = src.m_buffer[offset];
					j++;
					offset--;
				}
			} else {
				for (int i = 0, n = count / stride; i < n; i++) {
					for (int k = 0; k < stride; k++)
						m_buffer[j + k] = src.m_buffer[offset + k];

					j += stride;
					offset -= stride;
				}
			}
		}
	}

	private void _selfWriteRangeImpl(int toElement, int count, int fromElement,
			boolean bForward, int stride) {

		// writing from to this stream.
		if (bForward) {
			if (toElement == fromElement)
				return;
		}

		System.arraycopy(m_buffer, fromElement, m_buffer, toElement, count);

		if (bForward)
			return;
		// reverse what we written
		int j = toElement;
		int offset = toElement + count - stride;
		int dj = stride;
		for (int i = 0, n = count / 2; i < n; i++) {
			for (int k = 0; k < stride; k++) {
				double v = m_buffer[j + k];
				m_buffer[j + k] = m_buffer[offset + k];
				m_buffer[offset + k] = v;
			}
			j += stride;
			offset -= stride;
		}

	}

	@Override
	public void writeRange(int startElement, int count, ByteBuffer src,
			int offsetBytes, boolean bForward) {
		if (startElement < 0 || count < 0 || offsetBytes < 0)
			throw new IllegalArgumentException();

		final int elmSize = NumberUtils.sizeOf((double) 0);
		if (src.capacity() < (int) (offsetBytes + elmSize * count))
			throw new IllegalArgumentException();

		if (count == 0)
			return;

		if (size() < count + startElement)
			resize(count + startElement);

		int j = startElement;
		if (!bForward)
			j += count - 1;

		final int dj = bForward ? 1 : -1;

		int offset = offsetBytes;
		for (int i = 0; i < count; i++, offset += elmSize) {
			m_buffer[j] = src.getDouble(offset);
			j += dj;
		}

	}

	public void writeRange(int streamOffset, int pointCount, Point2D[] src,
			int arrayOffset, boolean bForward) {
		if (streamOffset < 0 || pointCount < 0 || arrayOffset < 0)
			throw new IllegalArgumentException();

		// if (src->Length < (int)(arrayOffset + pointCount)) jt: we have lost
		// the length check, not sure about this
		// GEOMTHROW(invalid_argument);

		if (pointCount == 0)
			return;

		if (size() < (pointCount << 1) + streamOffset)
			resize((pointCount << 1) + streamOffset);

		int j = streamOffset;
		if (!bForward)
			j += (pointCount - 1) << 1;

		final int dj = bForward ? 2 : -2;

		// TODO: refactor to take advantage of the known block array structure

		final int i0 = arrayOffset;
		pointCount += i0;
		for (int i = i0; i < pointCount; i++) {
			m_buffer[j] = src[i].x;
			m_buffer[j + 1] = src[i].y;
			j += dj;
		}
	}

	// Less efficient as boolean bForward set to false, as it is looping through
	// half
	// of the elements of the array
	public void readRange(int srcStart, int count, double[] dst, int dstOffset,
			boolean bForward) {
		if (srcStart < 0 || count < 0 || dstOffset < 0
				|| size() < count + srcStart)
			throw new IllegalArgumentException();

		if (bForward)
			System.arraycopy(m_buffer, srcStart, dst, dstOffset, count);
		else {
			int j = dstOffset + count - 1;
			for (int i = srcStart; i < count; i++) {
				dst[j] = m_buffer[i];
				j--;
			}
		}
	}

	public void sort(int start, int end) {
		Arrays.sort(m_buffer, start, end);
	}
}
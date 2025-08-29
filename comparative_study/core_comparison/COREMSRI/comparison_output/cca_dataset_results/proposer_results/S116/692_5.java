```java
/*
 Copyright 1995-2018 Esri

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

import java.io.Serializable;

import static com.esri.core.geometry.SizeOf.SIZE_OF_STRIDED_INDEX_TYPE_COLLECTION;
import static com.esri.core.geometry.SizeOf.sizeOfIntArray;
import static com.esri.core.geometry.SizeOf.sizeOfObjectArray;

/**
 * A collection of strides of Index_type elements. To be used when one needs a
 * collection of homogeneous elements that contain only integer fields (i.e.
 * structs with Index_type members) Recycles the strides. Allows for constant
 * time creation and deletion of an element.
 */
final class StridedIndexTypeCollection implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private int[][] buffer = null;
	private int firstFree = -1;
	private int last = 0;
	private int size = 0;
	private int capacity = 0;
	private int bufferSize = 0;
	private int stride;
	private int realStride;
	private int blockSize;

	/*
	 private final static int realBlockSize = 2048;//if you change this, change blockSize, blockPower, blockMask, and st_sizes
	 private final static int blockMask = 0x7FF;
	 private final static int blockPower = 11;
	 private final static int[] st_sizes = {16, 32, 64, 128, 256, 512, 1024, 2048};
	 */

	private final static int realBlockSize = 16384;// if you change this,
														// change blockSize,
														// blockPower,
														// blockMask, and
														// st_sizes
	private final static int blockMask = 0x3FFF;
	private final static int blockPower = 14;
	private final static int[] st_sizes = { 16, 32, 64, 128, 256, 512, 1024,
			2048, 4096, 8192, 16384 };

	StridedIndexTypeCollection(int stride) {
		this.stride = stride;
		this.realStride = stride;
		this.blockSize = realBlockSize / realStride;
	}

	private boolean dbgdelete_(int element) {
		buffer[element >> blockPower][(element & blockMask) + 1] = -0x7eadbeed;
		return true;
	}
	
	void deleteElement(int element) {
		assert(dbgdelete_(element));
		int totalStrides = (element >> blockPower) * blockSize
				* realStride + (element & blockMask);
		if (totalStrides < last * realStride) {
			buffer[element >> blockPower][element & blockMask] = firstFree;
			firstFree = element;
		} else {
			assert (totalStrides == last * realStride);
			last--;
		}
		size--;
	}

	// Returns the given field of the element.
	int getField(int element, int field) {
		assert(buffer[element >> blockPower][(element & blockMask) + 1] != -0x7eadbeed);
		return buffer[element >> blockPower][(element & blockMask)
				+ field];
	}

	// Sets the given field of the element.
	void setField(int element, int field, int value) {
		assert(buffer[element >> blockPower][(element & blockMask) + 1] != -0x7eadbeed);
		buffer[element >> blockPower][(element & blockMask) + field] = value;
	}

	// Returns the stride size
	int getStride() {
		return stride;
	}

	// Creates the new element. This is a constant time operation.
	// All fields are initialized to -1.
	int newElement() {
		int element = firstFree;
		if (element == -1) {
			if (last == capacity) {
				long newcap = capacity != 0 ? (((long) capacity + 1) * 3 / 2)
						: (long) 1;
				if (newcap > Integer.MAX_VALUE)
					newcap = Integer.MAX_VALUE;// cannot grow past 2gb elements
												// presently

				if (newcap == capacity)
					throw new IndexOutOfBoundsException();

				grow_(newcap);
			}

			element = ((last / blockSize) << blockPower)
					+ (last % blockSize) * realStride;
			last++;
		} else {
			firstFree = buffer[element >> blockPower][element
					& blockMask];
		}

		size++;
		int ar[] = buffer[element >> blockPower];
		int ind = element & blockMask;
		for (int i = 0; i < stride; i++) {
			ar[ind + i] = -1;
		}
		return element;
	}

	int elementToIndex(int element) {
		return (element >> blockPower) * blockSize
				+ (element & blockMask) / realStride;
	}

	// Deletes all elements and frees all the memory if b_free_memory is True.
	void deleteAll(boolean b_free_memory) {
		firstFree = -1;
		last = 0;
		size = 0;
		if (b_free_memory) {
			buffer = null;
			capacity = 0;
		}
	}

	// Returns the count of existing elements
	int size() {
		return size;
	}

	// Sets the capcity of the collection. Only applied if current capacity is
	// smaller.
	void setCapacity(int capacity) {
		if (capacity > this.capacity)
			grow_(capacity);
	}

	// Returns the capacity of the collection
	int capacity() {
		return capacity;
	}

	// Swaps content of two elements (each field of the stride)
	void swap(int element1, int element2) {
		int ar1[] = buffer[element1 >> blockPower];
		int ar2[] = buffer[element2 >> blockPower];
		int ind1 = element1 & blockMask;
		int ind2 = element2 & blockMask;
		for (int i = 0; i < stride; i++) {
			int tmp = ar1[ind1 + i];
			ar1[ind1 + i] = ar2[ind2 + i];
			ar2[ind2 + i] = tmp;
		}
	}

	// Swaps content of two fields
	void swapField(int element1, int element2, int field) {
		int ar1[] = buffer[element1 >> blockPower];
		int ar2[] = buffer[element2 >> blockPower];
		int ind1 = (element1 & blockMask) + field;
		int ind2 = (element2 & blockMask) + field;
		int tmp = ar1[ind1];
		ar1[ind1] = ar2[ind2];
		ar2[ind2] = tmp;
	}

	// Returns a value of the index, that never will be returned by new_element
	// and is neither -1 nor impossible_index_3.
	static int impossibleIndex2() {
		return -2;
	}

	// Returns a value of the index, that never will be returned by new_element
	// and is neither -1 nor impossible_index_2.
	static int impossibleIndex3() {
		return -3;
	}

	static boolean isValidElement(int element) {
		return element >= 0;
	}

	private void ensureBufferBlocksCapacity(int blocks) {
		if (buffer.length < blocks) {
			int[][] newBuffer = new int[blocks][];
			for (int i = 0; i < buffer.length; i++) {
				newBuffer[i] = buffer[i];
			}

			buffer = newBuffer;
		}
	}

	private void grow_(long newsize) {
		if (buffer == null) {
			bufferSize = 0;
			buffer = new int[8][];
		}

		assert (newsize > capacity);

		long nblocks = (newsize + blockSize - 1) / blockSize;
		if (nblocks > Integer.MAX_VALUE)
			throw new IndexOutOfBoundsException();

		ensureBufferBlocksCapacity((int) nblocks);
		if (nblocks == 1) {
			// When less than one block is needed we allocate smaller arrays
			// than realBlockSize to avoid initialization cost.
			int oldsz = capacity > 0 ? capacity : 0;
			assert (oldsz < newsize);
			int i = 0;
			int realnewsize = (int) newsize * realStride;
			while (realnewsize > st_sizes[i])
				// get the size to allocate. Using fixed sizes to reduce
				// fragmentation.
				i++;
			int[] b = new int[st_sizes[i]];
			if (bufferSize == 1) {
				System.arraycopy(buffer[0], 0, b, 0, buffer[0].length);
				buffer[0] = b;
			} else {
				buffer[bufferSize] = b;
				bufferSize++;
			}
			capacity = b.length / realStride;
		} else {
			if (nblocks * blockSize > Integer.MAX_VALUE)
				throw new IndexOutOfBoundsException();

			if (bufferSize == 1) {
				if (buffer[0].length < realBlockSize) {
					// resize the first buffer to ensure it is equal the
					// realBlockSize.
					int[] b = new int[realBlockSize];
					System.arraycopy(buffer[0], 0, b, 0, buffer[0].length);
					buffer[0] = b;
					capacity = blockSize;
				}
			}

			while (bufferSize < nblocks) {
				buffer[bufferSize++] = new int[realBlockSize];
				capacity += blockSize;
			}
		}
	}

	public long estimateMemorySize()
	{
		long size = SIZE_OF_STRIDED_INDEX_TYPE_COLLECTION;
		if (buffer != null) {
			size += sizeOfObjectArray(buffer.length);
			for (int i = 0; i< buffer.length; i++) {
				if (buffer[i] != null) {
					size += sizeOfIntArray(buffer[i].length);
				}
			}
		}
		return size;
	}
}


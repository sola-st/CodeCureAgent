//========================================================================
//Copyright 2007-2010 David Yu dyuproject@gmail.com
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package io.protostuff;

import static io.protostuff.WireFormat.TAG_TYPE_BITS;
import static io.protostuff.WireFormat.TAG_TYPE_MASK;
import static io.protostuff.WireFormat.WIRETYPE_END_GROUP;
import static io.protostuff.WireFormat.WIRETYPE_FIXED32;
import static io.protostuff.WireFormat.WIRETYPE_FIXED64;
import static io.protostuff.WireFormat.WIRETYPE_LENGTH_DELIMITED;
import static io.protostuff.WireFormat.WIRETYPE_START_GROUP;
import static io.protostuff.WireFormat.WIRETYPE_TAIL_DELIMITER;
import static io.protostuff.WireFormat.WIRETYPE_VARINT;
import static io.protostuff.WireFormat.getTagFieldNumber;
import static io.protostuff.WireFormat.getTagWireType;
import static io.protostuff.WireFormat.makeTag;

import java.io.IOException;
import java.nio.ByteBuffer;

import io.protostuff.StringSerializer.STRING;

/**
 * Reads and decodes protocol buffer message fields from an internal byte array buffer. This object is re-usable via
 * doing a reset on the byte array position and length. This is used internally by {@link IOUtil} where it catches
 * {@link ArrayIndexOutOfBoundsException} when a message is truncated.
 * 
 * @author David Yu
 * @created Jun 22, 2010
 */
public final class ByteArrayInput implements Input
{

    private final byte[] buffer;
    private int offset, limit, lastTag = 0;
    private int packedLimit = 0;

    /**
     * If true, the nested messages are group-encoded
     */
    public final boolean decodeNestedMessageAsGroup;

    public ByteArrayInput(byte[] buffer, boolean decodeNestedMessageAsGroup)
    {
        this(buffer, 0, buffer.length, decodeNestedMessageAsGroup);
    }

    public ByteArrayInput(byte[] buffer, int offset, int len, boolean decodeNestedMessageAsGroup)
```java
    /**
     * Read an {@code sfixed32} field value from the internal buffer.
     */
    @Override
    public int readSFixed32() throws IOException
    {
        return readRawLittleEndian32();
    }

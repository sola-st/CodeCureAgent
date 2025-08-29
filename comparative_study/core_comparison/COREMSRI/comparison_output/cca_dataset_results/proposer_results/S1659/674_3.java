package io.protostuff;

import static io.protostuff.StringSerializer.FIVE_BYTE_LOWER_LIMIT;
import static io.protostuff.StringSerializer.FOUR_BYTE_EXCLUSIVE;
import static io.protostuff.StringSerializer.FOUR_BYTE_LOWER_LIMIT;
import static io.protostuff.StringSerializer.INT_MIN_VALUE;
import static io.protostuff.StringSerializer.LONG_MIN_VALUE;
import static io.protostuff.StringSerializer.ONE_BYTE_EXCLUSIVE;
import static io.protostuff.StringSerializer.THREE_BYTE_EXCLUSIVE;
import static io.protostuff.StringSerializer.THREE_BYTE_LOWER_LIMIT;
import static io.protostuff.StringSerializer.TWO_BYTE_EXCLUSIVE;
import static io.protostuff.StringSerializer.TWO_BYTE_LOWER_LIMIT;
import static io.protostuff.StringSerializer.putBytesFromInt;
import static io.protostuff.StringSerializer.putBytesFromLong;
import static io.protostuff.StringSerializer.writeFixed2ByteInt;

import java.io.IOException;

/**
 * UTF-8 String serialization
 * 
 * @author David Yu
 * @created Feb 4, 2010
 */
public final class StreamedStringSerializer
{

    private StreamedStringSerializer()
    {
    }

    /**
     * Writes the stringified int into the {@link LinkedBuffer}.
     */
    public static LinkedBuffer writeInt(final int value, final WriteSession session,
            LinkedBuffer lb) throws IOException
    {
        if (value == Integer.MIN_VALUE)
        {
            final int valueLen = INT_MIN_VALUE.length;
            session.size += valueLen;

            if (lb.offset + valueLen > lb.buffer.length)
            {
                // not enough size
                lb.offset = session.flush(lb.buffer, lb.start, lb.offset - lb.start);
                // lb = new LinkedBuffer(session.nextBufferSize, lb);
            }

            System.arraycopy(INT_MIN_VALUE, 0, lb.buffer, lb.offset, valueLen);

            lb.offset += valueLen;

            return lb;
        }

        final int size = (value < 0) ? StringSerializer.stringSize(-value) + 1 : StringSerializer.stringSize(value);
        session.size += size;

        if (lb.offset + size > lb.buffer.length)
        {
            // not enough size
            lb.offset = session.flush(lb.buffer, lb.start, lb.offset - lb.start);
            // lb = new LinkedBuffer(session.nextBufferSize, lb);
        }

        putBytesFromInt(value, lb.offset, size, lb.buffer);

        lb.offset += size;

        return lb;
    }

    /**
     * Writes the stringified long into the {@link LinkedBuffer}.
     */
    public static LinkedBuffer writeLong(final long value, final WriteSession session,
            LinkedBuffer lb) throws IOException
    {
        if (value == Long.MIN_VALUE)
        {
            final int valueLen = LONG_MIN_VALUE.length;
            session.size += valueLen;

            if (lb.offset + valueLen > lb.buffer.length)
            {
                // TODO space efficiency (slower path)
                // not enough size
                lb.offset = session.flush(lb.buffer, lb.start, lb.offset - lb.start);
                // lb = new LinkedBuffer(session.nextBufferSize, lb);
            }

            System.arraycopy(LONG_MIN_VALUE, 0, lb.buffer, lb.offset, valueLen);

            lb.offset += valueLen;

            return lb;
        }

        final int size = (value < 0) ? StringSerializer.stringSize(-value) + 1 : StringSerializer.stringSize(value);
        session.size += size;

        if (lb.offset + size > lb.buffer.length)
        {
            // TODO space efficiency (slower path)
            // not enough size
            lb.offset = session.flush(lb.buffer, lb.start, lb.offset - lb.start);
            // lb = new LinkedBuffer(session.nextBufferSize, lb);
        }

        putBytesFromLong(value, lb.offset, size, lb.buffer);
```java
        int lastSize = session.size;
        int withIntOffset = lb.offset + 1;

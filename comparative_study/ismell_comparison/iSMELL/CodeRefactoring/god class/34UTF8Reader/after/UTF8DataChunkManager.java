package org.apache.xerces.readers;

import org.apache.xerces.framework.XMLErrorReporter;
import org.apache.xerces.utils.QName;
import org.apache.xerces.utils.StringPool;
import org.apache.xerces.utils.SymbolCache;
import org.apache.xerces.utils.UTF8DataChunk;
import org.apache.xerces.utils.XMLCharacterProperties;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.LocatorImpl;
import java.io.InputStream;
import java.util.Vector;

public class UTF8DataChunkManager {
    private static final char[] cdata_string = { 'C','D','A','T','A','['};
    private StringPool.CharArrayRange fCharArrayRange = null;
    private InputStream fInputStream = null;
    private StringPool fStringPool = null;
    private UTF8DataChunk fCurrentChunk = null;
    private int fCurrentIndex = 0;
    private byte[] fMostRecentData = null;
    private int fMostRecentByte = 0;
    private int fLength = 0;
    private boolean fCalledCharPropInit = false;
    private boolean fCallClearPreviousChunk = true;

    private int fillCurrentChunk() throws Exception {
        byte[] buf = fCurrentChunk.toByteArray();
        if (fInputStream == null) {
            if (buf == null)
                buf = new byte[1];
            buf[0] = 0;
            fMostRecentData = buf;
            fCurrentIndex = 0;
            fCurrentChunk.setByteArray(fMostRecentData);
            return(fMostRecentByte = fMostRecentData[0] & 0xFF);
        }
        if (buf == null)
            buf = new byte[UTF8DataChunk.CHUNK_SIZE];
        int offset = 0;
        int capacity = UTF8DataChunk.CHUNK_SIZE;
        int result = 0;
        do {
            try {
                result = fInputStream.read(buf, offset, capacity);
            } catch (java.io.IOException ex) {
                result = -1;
            }
            if (result == -1) {
                //
                // We have reached the end of the stream.
                //
                fInputStream.close();
                fInputStream = null;
                try {
                    buf[offset] = 0;
                } catch (ArrayIndexOutOfBoundsException ex) {
                }
                break;
            }
            if (result > 0) {
                offset += result;
                capacity -= result;
            }
        } while (capacity > 0);
        fMostRecentData = buf;
        fLength += offset;
        fCurrentIndex = 0;
        fCurrentChunk.setByteArray(fMostRecentData);
        return(fMostRecentByte = fMostRecentData[0] & 0xFF);
    }

    private int slowLoadNextByte()  throws Exception {
        fCallClearPreviousChunk = true;
        if (fCurrentChunk.nextChunk() != null) {
            fCurrentChunk = fCurrentChunk.nextChunk();
            fCurrentIndex = 0;
            fMostRecentData = fCurrentChunk.toByteArray();
            return(fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF);
        } else {
            fCurrentChunk = UTF8DataChunk.createChunk(fStringPool, fCurrentChunk);
            return fillCurrentChunk();
        }
    }

    private int loadNextByte() throws Exception {
        fCurrentOffset++;
        if (USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE) {
            fCurrentIndex++;
            try {
                fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF;
                return fMostRecentByte;
            } catch (ArrayIndexOutOfBoundsException ex) {
                return slowLoadNextByte();
            }
        } else {
            if (++fCurrentIndex == UTF8DataChunk.CHUNK_SIZE)
                return slowLoadNextByte();
            else
                return(fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF);
        }
    }

    private boolean atEOF(int offset) {
        return(offset > fLength);
    }
}

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

public class CharDataHandler {
    private char[] fCharacters = new char[UTF8DataChunk.CHUNK_SIZE];
    private int fCharDataLength = 0;
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

    public void append(XMLEntityHandler.CharBuffer charBuffer, int offset, int length) {
        fCurrentChunk.append(charBuffer, offset, length);
    }

    private void appendCharData(int ch) throws Exception {
        try {
            fCharacters[fCharDataLength] = (char)ch;
            fCharDataLength++;
        } catch (ArrayIndexOutOfBoundsException ex) {
            slowAppendCharData(ch);
        }
    }

    private void slowAppendCharData(int ch) throws Exception {
        // flush the buffer...
        characters(0, fCharDataLength); /* DEFECT !! whitespace this long is unlikely, but possible */
        fCharDataLength = 0;
        fCharacters[fCharDataLength++] = (char)ch;
    }

    private void characters(int offset, int endOffset) throws Exception {
        //
        // REVISIT - need more up front bounds checking code of params...
        //
        if (!fSendCharDataAsCharArray) {
            int stringIndex = addString(offset, endOffset - offset);
            fCharDataHandler.processCharacters(stringIndex);
            return;
        }
        fCharDataHandler.processCharacters(fCharacters, 0, fCharDataLength);
    }

    private void whitespace(int offset, int endOffset) throws Exception {
        //
        // REVISIT - need more up front bounds checking code of params...
        //
        if (!fSendCharDataAsCharArray) {
            int stringIndex = addString(offset, endOffset - offset);
            fCharDataHandler.processWhitespace(stringIndex);
            return;
        }
        fCharDataHandler.processWhitespace(fCharacters, 0, fCharDataLength);
    }

    private boolean copyMultiByteCharData(int b0) throws Exception {
        UTF8DataChunk saveChunk = fCurrentChunk;
        int saveOffset = fCurrentOffset;
        int saveIndex = fCurrentIndex;
        int b1 = loadNextByte();
        if ((0xe0 & b0) == 0xc0) { // 110yyyyy 10xxxxxx (0x80 to 0x7ff)
            int ch = ((0x1f & b0)<<6) + (0x3f & b1);
            appendCharData(ch); // yyy yyxx xxxx (0x80 to 0x7ff)
            loadNextByte();
            return true;
        }
        int b2 = loadNextByte();
        if ((0xf0 & b0) == 0xe0) { // 1110zzzz 10yyyyyy 10xxxxxx
            // ch = ((0x0f & b0)<<12) + ((0x3f & b1)<<6) + (0x3f & b2); // zzzz yyyy yyxx xxxx (0x800 to 0xffff)
            // if ((ch >= 0xD800 && ch <= 0xDFFF) || ch >= 0xFFFE)
            if ((b0 == 0xED && b1 >= 0xA0) || (b0 == 0xEF && b1 == 0xBF && b2 >= 0xBE)) {
                fCurrentChunk = saveChunk;
                fCurrentIndex = saveIndex;
                fCurrentOffset = saveOffset;
                fMostRecentData = saveChunk.toByteArray();
                fMostRecentByte = b0;
                return false;
            }
            int ch = ((0x0f & b0)<<12) + ((0x3f & b1)<<6) + (0x3f & b2);
            appendCharData(ch); // zzzz yyyy yyxx xxxx (0x800 to 0xffff)
            loadNextByte();
            return true;
        }

        int b3 = loadNextByte();  // 11110uuu 10uuzzzz 10yyyyyy 10xxxxxx
        // ch = ((0x0f & b0)<<18) + ((0x3f & b1)<<12) + ((0x3f & b2)<<6) + (0x3f & b3); // u uuuu zzzz yyyy yyxx xxxx (0x10000 to 0x1ffff)
        // if (ch >= 0x110000)
        if (( 0xf8 & b0 ) == 0xf0 ) {
            if (b0 > 0xF4 || (b0 == 0xF4 && b1 >= 0x90)) {
                fCurrentChunk = saveChunk;
                fCurrentIndex = saveIndex;
                fCurrentOffset = saveOffset;
                fMostRecentData = saveChunk.toByteArray();
                fMostRecentByte = b0;
                return false;
            }
            int ch = ((0x0f & b0)<<18) + ((0x3f & b1)<<12) + ((0x3f & b2)<<6) + (0x3f & b3);
            if (ch < 0x10000) {
                appendCharData(ch);
            } else {
                appendCharData(((ch-0x00010000)>>10)+0xd800);
                appendCharData(((ch-0x00010000)&0x3ff)+0xdc00);
            }
            loadNextByte();
            return true;
        } else {
            fCurrentChunk = saveChunk;
            fCurrentIndex = saveIndex;
            fCurrentOffset = saveOffset;
            fMostRecentData = saveChunk.toByteArray();
            fMostRecentByte = b0;
            return false;
        }
    }

    private boolean skipMultiByteCharData(int b0) throws Exception {
        UTF8DataChunk saveChunk = fCurrentChunk;
        int saveOffset = fCurrentOffset;
        int saveIndex = fCurrentIndex;
        int b1 = loadNextByte();
        if ((0xe0 & b0) == 0xc0) { // 110yyyyy 10xxxxxx (0x80 to 0x7ff)
            loadNextByte();
            return true;
        }
        int b2 = loadNextByte();
        if ((0xf0 & b0) == 0xe0) { // 1110zzzz 10yyyyyy 10xxxxxx
            // ch = ((0x0f & b0)<<12) + ((0x3f & b1)<<6) + (0x3f & b2); // zzzz yyyy yyxx xxxx (0x800 to 0xffff)
            // if ((ch >= 0xD800 && ch <= 0xDFFF) || ch >= 0xFFFE)
            if ((b0 == 0xED && b1 >= 0xA0) || (b0 == 0xEF && b1 == 0xBF && b2 >= 0xBE)) {
                fCurrentChunk = saveChunk;
                fCurrentIndex = saveIndex;
                fCurrentOffset = saveOffset;
                fMostRecentData = saveChunk.toByteArray();
                fMostRecentByte = b0;
                return false;
            }
            loadNextByte();
            return true;
        }
        int b3 = loadNextByte();  // 11110uuu 10uuzzzz 10yyyyyy 10xxxxxx
        // ch = ((0x0f & b0)<<18) + ((0x3f & b1)<<12) + ((0x3f & b2)<<6) + (0x3f & b3); // u uuuu zzzz yyyy yyxx xxxx (0x10000 to 0x1ffff)
        // if (ch >= 0x110000)
        if (b0 > 0xF4 || (b0 == 0xF4 && b1 >= 0x90)) {
            fCurrentChunk = saveChunk;
            fCurrentIndex = saveIndex;
            fCurrentOffset = saveOffset;
            fMostRecentData = saveChunk.toByteArray();
            fMostRecentByte = b0;
            return false;
        }
        loadNextByte();
        return true;
    }

    private int copyAsciiCharData() throws Exception {
        int srcIndex = fCurrentIndex;
        int offset = fCurrentOffset - srcIndex;
        byte[] data = fMostRecentData;
        int dstIndex = fCharDataLength;
        boolean skiplf = false;
        while (true) {
            int ch;
            try {
                ch = data[srcIndex] & 0xFF;
            } catch (ArrayIndexOutOfBoundsException ex) {
                offset += srcIndex;
                slowLoadNextByte();
                srcIndex = 0;
                data = fMostRecentData;
                ch = data[srcIndex] & 0xFF;
            }
            if (ch >= 0x80) {
                fCurrentOffset = offset + srcIndex;
                fCurrentIndex = srcIndex;
                fMostRecentByte = ch;
                return ch;
            }
            if (XMLCharacterProperties.fgAsciiCharData[ch] == 0) {
                fCharacterCounter++;
                skiplf = false;
            } else if (ch == 0x0A) {
                fLinefeedCounter++;
                if (skiplf) {
                    skiplf = false;
                    srcIndex++;
                    continue;
                }
                fCharacterCounter = 1;
            } else if (ch == 0x0D) {
                fCarriageReturnCounter++;
                fCharacterCounter = 1;
                skiplf = true;
                ch = 0x0A;
            } else {
                fCurrentOffset = offset + srcIndex;
                fCurrentIndex = srcIndex;
                fMostRecentByte = ch;
                return ch;
            }
            srcIndex++;
            try {
                fCharacters[fCharDataLength] = (char)ch;
                fCharDataLength++;
            } catch (ArrayIndexOutOfBoundsException ex) {
                slowAppendCharData(ch);
            }
        }
    }
    private int skipAsciiCharData() throws Exception {
        int srcIndex = fCurrentIndex;
        int offset = fCurrentOffset - srcIndex;
        byte[] data = fMostRecentData;
        while (true) {
            int ch;
            try {
                ch = data[srcIndex] & 0xFF;
            } catch (ArrayIndexOutOfBoundsException ex) {
                offset += srcIndex;
                slowLoadNextByte();
                srcIndex = 0;
                data = fMostRecentData;
                ch = data[srcIndex] & 0xFF;
            }
            if (ch >= 0x80) {
                fCurrentOffset = offset + srcIndex;
                fCurrentIndex = srcIndex;
                fMostRecentByte = ch;
                return ch;
            }
            if (XMLCharacterProperties.fgAsciiCharData[ch] == 0) {
                fCharacterCounter++;
            } else if (ch == 0x0A) {
                fLinefeedCounter++;
                fCharacterCounter = 1;
            } else if (ch == 0x0D) {
                fCarriageReturnCounter++;
                fCharacterCounter = 1;
            } else {
                fCurrentOffset = offset + srcIndex;
                fCurrentIndex = srcIndex;
                fMostRecentByte = ch;
                return ch;
            }
            srcIndex++;
        }
    }
}

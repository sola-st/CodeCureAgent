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

public class CharacterProperties {
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
    
    public boolean lookingAtChar(char ch, boolean skipPastChar) throws Exception {
        int b0 = fMostRecentByte;
        if (b0 != ch) {
            if (b0 == 0) {
                if (atEOF(fCurrentOffset + 1)) {
                    return changeReaders().lookingAtChar(ch, skipPastChar);
                }
            }
            if (ch == 0x0A && b0 == 0x0D) {
                if (skipPastChar) {
                    fCarriageReturnCounter++;
                    fCharacterCounter = 1;
                    if (USE_OUT_OF_LINE_LOAD_NEXT_BYTE) {
                        b0 = loadNextByte();
                    } else {
                        fCurrentOffset++;
                        if (USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE) {
                            fCurrentIndex++;
                            try {
                                fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF;
                                b0 = fMostRecentByte;
                            } catch (ArrayIndexOutOfBoundsException ex) {
                                b0 = slowLoadNextByte();
                            }
                        } else {
                            if (++fCurrentIndex == UTF8DataChunk.CHUNK_SIZE)
                                b0 = slowLoadNextByte();
                            else
                                b0 = (fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF);
                        }
                    }
                    if (b0 == 0x0A) {
                        fLinefeedCounter++;
                        if (USE_OUT_OF_LINE_LOAD_NEXT_BYTE) {
                            loadNextByte();
                        } else {
                            fCurrentOffset++;
                            if (USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE) {
                                fCurrentIndex++;
                                try {
                                    fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF;
                                } catch (ArrayIndexOutOfBoundsException ex) {
                                    slowLoadNextByte();
                                }
                            } else {
                                if (++fCurrentIndex == UTF8DataChunk.CHUNK_SIZE)
                                    slowLoadNextByte();
                                else
                                    fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF;
                            }
                        }
                    }
                }
                return true;
            }
            return false;
        }
        if (ch == 0x0D)
            return false;
        if (skipPastChar) {
            fCharacterCounter++;
            if (USE_OUT_OF_LINE_LOAD_NEXT_BYTE) {
                loadNextByte();
            } else {
                fCurrentOffset++;
                if (USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE) {
                    fCurrentIndex++;
                    try {
                        fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF;
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        slowLoadNextByte();
                    }
                } else {
                    if (++fCurrentIndex == UTF8DataChunk.CHUNK_SIZE)
                        slowLoadNextByte();
                    else
                        fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF;
                }
            }
        }
        return true;
    }

    public boolean lookingAtValidChar(boolean skipPastChar) throws Exception {
        int b0 = fMostRecentByte;
        if (b0 < 0x80) {  // 0xxxxxxx
            if (b0 >= 0x20 || b0 == 0x09) {
                if (skipPastChar) {
                    fCharacterCounter++;
                    if (USE_OUT_OF_LINE_LOAD_NEXT_BYTE) {
                        loadNextByte();
                    } else {
                        fCurrentOffset++;
                        if (USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE) {
                            fCurrentIndex++;
                            try {
                                fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF;
                            } catch (ArrayIndexOutOfBoundsException ex) {
                                slowLoadNextByte();
                            }
                        } else {
                            if (++fCurrentIndex == UTF8DataChunk.CHUNK_SIZE)
                                slowLoadNextByte();
                            else
                                fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF;
                        }
                    }
                }
                return true;
            }
            if (b0 == 0x0A) {
                if (skipPastChar) {
                    fLinefeedCounter++;
                    fCharacterCounter = 1;
                    if (USE_OUT_OF_LINE_LOAD_NEXT_BYTE) {
                        loadNextByte();
                    } else {
                        fCurrentOffset++;
                        if (USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE) {
                            fCurrentIndex++;
                            try {
                                fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF;
                            } catch (ArrayIndexOutOfBoundsException ex) {
                                slowLoadNextByte();
                            }
                        } else {
                            if (++fCurrentIndex == UTF8DataChunk.CHUNK_SIZE)
                                slowLoadNextByte();
                            else
                                fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF;
                        }
                    }
                }
                return true;
            }
            if (b0 == 0x0D) {
                if (skipPastChar) {
                    fCarriageReturnCounter++;
                    fCharacterCounter = 1;
                    if (USE_OUT_OF_LINE_LOAD_NEXT_BYTE) {
                        b0 = loadNextByte();
                    } else {
                        fCurrentOffset++;
                        if (USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE) {
                            fCurrentIndex++;
                            try {
                                fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF;
                                b0 = fMostRecentByte;
                            } catch (ArrayIndexOutOfBoundsException ex) {
                                b0 = slowLoadNextByte();
                            }
                        } else {
                            if (++fCurrentIndex == UTF8DataChunk.CHUNK_SIZE)
                                b0 = slowLoadNextByte();
                            else
                                b0 = (fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF);
                        }
                    }
                    if (b0 == 0x0A) {
                        fLinefeedCounter++;
                        if (USE_OUT_OF_LINE_LOAD_NEXT_BYTE) {
                            loadNextByte();
                        } else {
                            fCurrentOffset++;
                            if (USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE) {
                                fCurrentIndex++;
                                try {
                                    fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF;
                                } catch (ArrayIndexOutOfBoundsException ex) {
                                    slowLoadNextByte();
                                }
                            } else {
                                if (++fCurrentIndex == UTF8DataChunk.CHUNK_SIZE)
                                    slowLoadNextByte();
                                else
                                    fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF;
                            }
                        }
                    }
                }
                return true;
            }
            if (b0 == 0) {
                if (atEOF(fCurrentOffset + 1)) {
                    return changeReaders().lookingAtValidChar(skipPastChar);
                }
            }
            return false;
        }
        //
        // REVISIT - optimize this with in-buffer lookahead.
        //
        UTF8DataChunk saveChunk = fCurrentChunk;
        int saveIndex = fCurrentIndex;
        int saveOffset = fCurrentOffset;
        int b1 = loadNextByte();
        if ((0xe0 & b0) == 0xc0) { // 110yyyyy 10xxxxxx (0x80 to 0x7ff)
            if (skipPastChar) {
                fCharacterCounter++;
                loadNextByte();
            } else {
                fCurrentChunk = saveChunk;
                fCurrentIndex = saveIndex;
                fCurrentOffset = saveOffset;
                fMostRecentData = saveChunk.toByteArray();
                fMostRecentByte = b0;
            }
            return true; // [#x20-#xD7FF]
        }
        int b2 = loadNextByte();
        if ((0xf0 & b0) == 0xe0) { // 1110zzzz 10yyyyyy 10xxxxxx
            // ch = ((0x0f & b0)<<12) + ((0x3f & b1)<<6) + (0x3f & b2); // zzzz yyyy yyxx xxxx (0x800 to 0xffff)
            // if (!((ch >= 0xD800 && ch <= 0xDFFF) || ch >= 0xFFFE))
            // if ((ch <= 0xD7FF) || (ch >= 0xE000 && ch <= 0xFFFD))
            boolean result = false;
            if (!((b0 == 0xED && b1 >= 0xA0) || (b0 == 0xEF && b1 == 0xBF && b2 >= 0xBE))) { // [#x20-#xD7FF] | [#xE000-#xFFFD]
                if (skipPastChar) {
                    fCharacterCounter++;
                    loadNextByte();
                    return true;
                }
                result = true;
            }
            fCurrentChunk = saveChunk;
            fCurrentIndex = saveIndex;
            fCurrentOffset = saveOffset;
            fMostRecentData = saveChunk.toByteArray();
            fMostRecentByte = b0;
            return result;
        }
        int b3 = loadNextByte();  // 11110uuu 10uuzzzz 10yyyyyy 10xxxxxx
        // ch = ((0x0f & b0)<<18) + ((0x3f & b1)<<12) + ((0x3f & b2)<<6) + (0x3f & b3); // u uuuu zzzz yyyy yyxx xxxx (0x10000 to 0x1ffff)
        // if (ch >= 0x110000)
        boolean result = false;

        //if (( 0xf8 & b0 ) == 0xf0 ) {
        //if (!(b0 > 0xF4 || (b0 == 0xF4 && b1 >= 0x90))) { // [#x10000-#x10FFFF]
        if ( ((b0&0xf8) == 0xf0) && ((b1&0xc0)==0x80) &&
             ((b2&0xc0) == 0x80) && ((b3&0xc0)==0x80)){
            if (!(b0 > 0xF4 || (b0 == 0xF4 && b1 >= 0x90))) { // [#x10000-#x10FFFF]

                if (skipPastChar) {
                    fCharacterCounter++;
                    loadNextByte();
                    return true;
                }
                result = true;
            }
            fCurrentChunk = saveChunk;
            fCurrentIndex = saveIndex;
            fCurrentOffset = saveOffset;
            fMostRecentData = saveChunk.toByteArray();
            fMostRecentByte = b0;
            return result;
        } else{
            fCurrentChunk = saveChunk;
            fCurrentIndex = saveIndex;
            fCurrentOffset = saveOffset;
            fMostRecentData = saveChunk.toByteArray();
            fMostRecentByte = b0;
            return result;
        }
    }

    public boolean lookingAtSpace(boolean skipPastChar) throws Exception {
        int ch = fMostRecentByte;
        if (ch > 0x20)
            return false;
        if (ch == 0x20 || ch == 0x09) {
            if (!skipPastChar)
                return true;
            fCharacterCounter++;
        } else if (ch == 0x0A) {
            if (!skipPastChar)
                return true;
            fLinefeedCounter++;
            fCharacterCounter = 1;
        } else if (ch == 0x0D) {
            if (!skipPastChar)
                return true;
            fCarriageReturnCounter++;
            fCharacterCounter = 1;
            if (USE_OUT_OF_LINE_LOAD_NEXT_BYTE) {
                ch = loadNextByte();
            } else {
                fCurrentOffset++;
                if (USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE) {
                    fCurrentIndex++;
                    try {
                        fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF;
                        ch = fMostRecentByte;
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        ch = slowLoadNextByte();
                    }
                } else {
                    if (++fCurrentIndex == UTF8DataChunk.CHUNK_SIZE)
                        ch = slowLoadNextByte();
                    else
                        ch = (fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF);
                }
            }
            if (ch != 0x0A)
                return true;
            fLinefeedCounter++;
        } else {
            if (ch == 0) { // REVISIT - should we be checking this here ?
                if (atEOF(fCurrentOffset + 1)) {
                    return changeReaders().lookingAtSpace(skipPastChar);
                }
            }
            return false;
        }
        if (USE_OUT_OF_LINE_LOAD_NEXT_BYTE) {
            loadNextByte();
        } else {
            fCurrentOffset++;
            if (USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE) {
                fCurrentIndex++;
                try {
                    fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF;
                } catch (ArrayIndexOutOfBoundsException ex) {
                    slowLoadNextByte();
                }
            } else {
                if (++fCurrentIndex == UTF8DataChunk.CHUNK_SIZE)
                    slowLoadNextByte();
                else
                    fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF;
            }
        }
        return true;
    }

    protected boolean skippedMultiByteCharWithFlag(int b0, int flag) throws Exception {
        UTF8DataChunk saveChunk = fCurrentChunk;
        int saveOffset = fCurrentOffset;
        int saveIndex = fCurrentIndex;
        if (!fCalledCharPropInit) {
            XMLCharacterProperties.initCharFlags();
            fCalledCharPropInit = true;
        }
        int b1 = loadNextByte();
        if ((0xe0 & b0) == 0xc0) { // 110yyyyy 10xxxxxx
            if ((XMLCharacterProperties.fgCharFlags[((0x1f & b0)<<6) + (0x3f & b1)] & flag) == 0) { // yyy yyxx xxxx (0x80 to 0x7ff)
                fCurrentChunk = saveChunk;
                fCurrentIndex = saveIndex;
                fCurrentOffset = saveOffset;
                fMostRecentData = saveChunk.toByteArray();
                fMostRecentByte = b0;
                return false;
            }
            return true;
        }
        int b2 = loadNextByte();
        if ((0xf0 & b0) == 0xe0) { // 1110zzzz 10yyyyyy 10xxxxxx
            // if ((ch >= 0xD800 && ch <= 0xDFFF) || ch >= 0xFFFE)
            if ((b0 == 0xED && b1 >= 0xA0) || (b0 == 0xEF && b1 == 0xBF && b2 >= 0xBE)) {
                fCurrentChunk = saveChunk;
                fCurrentIndex = saveIndex;
                fCurrentOffset = saveOffset;
                fMostRecentData = saveChunk.toByteArray();
                fMostRecentByte = b0;
                return false;
            }
            if ((XMLCharacterProperties.fgCharFlags[((0x0f & b0)<<12) + ((0x3f & b1)<<6) + (0x3f & b2)] & flag) == 0) { // zzzz yyyy yyxx xxxx (0x800 to 0xffff)
                fCurrentChunk = saveChunk;
                fCurrentIndex = saveIndex;
                fCurrentOffset = saveOffset;
                fMostRecentData = saveChunk.toByteArray();
                fMostRecentByte = b0;
                return false;
            }
            return true;
        } else { // 11110uuu 10uuzzzz 10yyyyyy 10xxxxxx
            fCurrentChunk = saveChunk;
            fCurrentIndex = saveIndex;
            fCurrentOffset = saveOffset;
            fMostRecentData = saveChunk.toByteArray();
            fMostRecentByte = b0;
            return false;
        }
    }
}

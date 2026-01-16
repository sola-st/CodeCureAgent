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

public class ContentScanner {
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
    
    public int scanContent(QName element) throws Exception {
        if (fCallClearPreviousChunk && fCurrentChunk.clearPreviousChunk())
            fCallClearPreviousChunk = false;
        fCharDataLength = 0;
        int charDataOffset = fCurrentOffset;
        int ch = fMostRecentByte;
        if (ch < 0x80) {
            switch (XMLCharacterProperties.fgAsciiWSCharData[ch]) {
            case 0:
                if (fSendCharDataAsCharArray) {
                    try {
                        fCharacters[fCharDataLength] = (char)ch;
                        fCharDataLength++;
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        slowAppendCharData(ch);
                    }
                }
                fCharacterCounter++;
                if (USE_OUT_OF_LINE_LOAD_NEXT_BYTE) {
                    ch = loadNextByte();
                } else {
                    fCurrentOffset++;
                    if (USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE) {
                        fCurrentIndex++;
                        try {
                            ch = (fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF);
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
                break;
            case 1: // '<'
                fCharacterCounter++;
                if (USE_OUT_OF_LINE_LOAD_NEXT_BYTE) {
                    ch = loadNextByte();
                } else {
                    fCurrentOffset++;
                    if (USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE) {
                        fCurrentIndex++;
                        try {
                            ch = (fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF);
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
                if (!fInCDSect) {
                    return recognizeMarkup(ch, element);
                }
                if (fSendCharDataAsCharArray)
                    appendCharData('<');
                break;
            case 2: // '&'
                fCharacterCounter++;
                ch = loadNextByte();
                if (!fInCDSect) {
                    return recognizeReference(ch);
                }
                if (fSendCharDataAsCharArray)
                    appendCharData('&');
                break;
            case 3: // ']'
                fCharacterCounter++;
                ch = loadNextByte();
                if (ch != ']') {
                    if (fSendCharDataAsCharArray)
                        appendCharData(']');
                    break;
                }
                if (fCurrentIndex + 1 == UTF8DataChunk.CHUNK_SIZE) {
                    UTF8DataChunk saveChunk = fCurrentChunk;
                    int saveIndex = fCurrentIndex;
                    int saveOffset = fCurrentOffset;
                    if (loadNextByte() != '>') {
                        fCurrentChunk = saveChunk;
                        fCurrentIndex = saveIndex;
                        fCurrentOffset = saveOffset;
                        fMostRecentData = fCurrentChunk.toByteArray();
                        fMostRecentByte = ']';
                        if (fSendCharDataAsCharArray)
                            appendCharData(']');
                        break;
                    }
                } else {
                    if (fMostRecentData[fCurrentIndex + 1] != '>') {
                        if (fSendCharDataAsCharArray)
                            appendCharData(']');
                        break;
                    }
                    fCurrentIndex++;
                    fCurrentOffset++;
                }
                loadNextByte();
                fCharacterCounter += 2;
                return XMLEntityHandler.CONTENT_RESULT_END_OF_CDSECT;
            case 4: // invalid char
                if (ch == 0 && atEOF(fCurrentOffset + 1)) {
                    changeReaders();
                    return XMLEntityHandler.CONTENT_RESULT_INVALID_CHAR; // REVISIT - not quite...
                }
                return XMLEntityHandler.CONTENT_RESULT_INVALID_CHAR;
            case 5:
                do {
                    if (ch == 0x0A) {
                        fLinefeedCounter++;
                        fCharacterCounter = 1;
                    } else if (ch == 0x0D) {
                        fCarriageReturnCounter++;
                        fCharacterCounter = 1;
                        if (USE_OUT_OF_LINE_LOAD_NEXT_BYTE) {
                            ch = loadNextByte();
                        } else {
                            fCurrentOffset++;
                            if (USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE) {
                                fCurrentIndex++;
                                try {
                                    ch = (fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF);
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
                        if (ch != 0x0A) {
                            if (fSendCharDataAsCharArray)
                                appendCharData(0x0A);
                            if (ch == 0x20 || ch == 0x09 || ch == 0x0D)
                                continue;
                            break;
                        }
                        fLinefeedCounter++;
                    } else {
                        fCharacterCounter++;
                    }
                    if (fSendCharDataAsCharArray) {
                        try {
                            fCharacters[fCharDataLength] = (char)ch;
                            fCharDataLength++;
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            slowAppendCharData(ch);
                        }
                    }
                    if (USE_OUT_OF_LINE_LOAD_NEXT_BYTE) {
                        ch = loadNextByte();
                    } else {
                        fCurrentOffset++;
                        if (USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE) {
                            fCurrentIndex++;
                            try {
                                ch = (fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF);
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
                } while (ch == 0x20 || ch == 0x09 || ch == 0x0A || ch == 0x0D);
                if (ch < 0x80) {
                    switch (XMLCharacterProperties.fgAsciiCharData[ch]) {
                    case 0:
                        if (fSendCharDataAsCharArray)
                            appendCharData(ch);
                        fCharacterCounter++;
                        ch = loadNextByte();
                        break;
                    case 1: // '<'
                        if (!fInCDSect) {
                            if (fSendCharDataAsCharArray) {
                                fCharDataHandler.processWhitespace(fCharacters, 0, fCharDataLength);
                            } else {
                                int stringIndex = addString(charDataOffset, fCurrentOffset - charDataOffset);
                                fCharDataHandler.processWhitespace(stringIndex);
                            }
                            fCharacterCounter++;
                            if (USE_OUT_OF_LINE_LOAD_NEXT_BYTE) {
                                ch = loadNextByte();
                            } else {
                                fCurrentOffset++;
                                if (USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE) {
                                    fCurrentIndex++;
                                    try {
                                        ch = (fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF);
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
                            return recognizeMarkup(ch, element);
                        }
                        if (fSendCharDataAsCharArray)
                            appendCharData('<');
                        fCharacterCounter++;
                        ch = loadNextByte();
                        break;
                    case 2: // '&'
                        if (!fInCDSect) {
                            whitespace(charDataOffset, fCurrentOffset);
                            fCharacterCounter++;
                            ch = loadNextByte();
                            return recognizeReference(ch);
                        }
                        if (fSendCharDataAsCharArray)
                            appendCharData('&');
                        fCharacterCounter++;
                        ch = loadNextByte();
                        break;
                    case 3: // ']'
                        int endOffset = fCurrentOffset;
                        ch = loadNextByte();
                        if (ch != ']') {
                            fCharacterCounter++;
                            if (fSendCharDataAsCharArray)
                                appendCharData(']');
                            break;
                        }
                        if (fCurrentIndex + 1 == UTF8DataChunk.CHUNK_SIZE) {
                            UTF8DataChunk saveChunk = fCurrentChunk;
                            int saveIndex = fCurrentIndex;
                            int saveOffset = fCurrentOffset;
                            if (loadNextByte() != '>') {
                                fCurrentChunk = saveChunk;
                                fCurrentIndex = saveIndex;
                                fCurrentOffset = saveOffset;
                                fMostRecentData = fCurrentChunk.toByteArray();
                                fMostRecentByte = ']';
                                fCharacterCounter++;
                                if (fSendCharDataAsCharArray)
                                    appendCharData(']');
                                break;
                            }
                        } else {
                            if (fMostRecentData[fCurrentIndex + 1] != '>') {
                                fCharacterCounter++;
                                if (fSendCharDataAsCharArray)
                                    appendCharData(']');
                                break;
                            }
                            fCurrentIndex++;
                            fCurrentOffset++;
                        }
                        loadNextByte();
                        whitespace(charDataOffset, endOffset);
                        fCharacterCounter += 3;
                        return XMLEntityHandler.CONTENT_RESULT_END_OF_CDSECT;
                    case 4: // invalid char
                        whitespace(charDataOffset, fCurrentOffset);
                        if (ch == 0 && atEOF(fCurrentOffset + 1)) {
                            changeReaders();
                            return XMLEntityHandler.CONTENT_RESULT_INVALID_CHAR; // REVISIT - not quite...
                        }
                        return XMLEntityHandler.CONTENT_RESULT_INVALID_CHAR;
                    }
                } else {
                    if (fSendCharDataAsCharArray) {
                        if (!copyMultiByteCharData(ch)) {
                            whitespace(charDataOffset, fCurrentOffset);
                            return XMLEntityHandler.CONTENT_RESULT_INVALID_CHAR;
                        }
                    } else if (!skipMultiByteCharData(ch)) {
                        whitespace(charDataOffset, fCurrentOffset);
                        return XMLEntityHandler.CONTENT_RESULT_INVALID_CHAR;
                    }
                }
                break;
            }
        } else {
            if (fSendCharDataAsCharArray) {
                if (!copyMultiByteCharData(ch)) {
                    return XMLEntityHandler.CONTENT_RESULT_INVALID_CHAR;
                }
            } else {
                if (!skipMultiByteCharData(ch)) {
                    return XMLEntityHandler.CONTENT_RESULT_INVALID_CHAR;
                }
            }
        }
        if (fSendCharDataAsCharArray)
            ch = copyAsciiCharData();
        else
            ch = skipAsciiCharData();
        while (true) {
            if (ch < 0x80) {
                switch (XMLCharacterProperties.fgAsciiCharData[ch]) {
                case 0:
                    if (fSendCharDataAsCharArray)
                        appendCharData(ch);
                    fCharacterCounter++;
                    ch = loadNextByte();
                    break;
                case 1: // '<'
                    if (!fInCDSect) {
                        if (fSendCharDataAsCharArray) {
                            fCharDataHandler.processCharacters(fCharacters, 0, fCharDataLength);
                        } else {
                            int stringIndex = addString(charDataOffset, fCurrentOffset - charDataOffset);
                            fCharDataHandler.processCharacters(stringIndex);
                        }
                        fCharacterCounter++;
                        if (USE_OUT_OF_LINE_LOAD_NEXT_BYTE) {
                            ch = loadNextByte();
                        } else {
                            fCurrentOffset++;
                            if (USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE) {
                                fCurrentIndex++;
                                try {
                                    ch = (fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF);
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
                        return recognizeMarkup(ch, element);
                    }
                    if (fSendCharDataAsCharArray)
                        appendCharData('<');
                    fCharacterCounter++;
                    ch = loadNextByte();
                    break;
                case 2: // '&'
                    if (!fInCDSect) {
                        characters(charDataOffset, fCurrentOffset);
                        fCharacterCounter++;
                        ch = loadNextByte();
                        return recognizeReference(ch);
                    }
                    if (fSendCharDataAsCharArray)
                        appendCharData('&');
                    fCharacterCounter++;
                    ch = loadNextByte();
                    break;
                case 3: // ']'
                    int endOffset = fCurrentOffset;
                    ch = loadNextByte();
                    if (ch != ']') {
                        fCharacterCounter++;
                        if (fSendCharDataAsCharArray)
                            appendCharData(']');
                        break;
                    }
                    if (fCurrentIndex + 1 == UTF8DataChunk.CHUNK_SIZE) {
                        UTF8DataChunk saveChunk = fCurrentChunk;
                        int saveIndex = fCurrentIndex;
                        int saveOffset = fCurrentOffset;
                        if (loadNextByte() != '>') {
                            fCurrentChunk = saveChunk;
                            fCurrentIndex = saveIndex;
                            fCurrentOffset = saveOffset;
                            fMostRecentData = fCurrentChunk.toByteArray();
                            fMostRecentByte = ']';
                            fCharacterCounter++;
                            if (fSendCharDataAsCharArray)
                                appendCharData(']');
                            break;
                        }
                    } else {
                        if (fMostRecentData[fCurrentIndex + 1] != '>') {
                            fCharacterCounter++;
                            if (fSendCharDataAsCharArray)
                                appendCharData(']');
                            break;
                        }
                        fCurrentIndex++;
                        fCurrentOffset++;
                    }
                    loadNextByte();
                    characters(charDataOffset, endOffset);
                    fCharacterCounter += 3;
                    return XMLEntityHandler.CONTENT_RESULT_END_OF_CDSECT;
                case 4: // invalid char
                    if (ch == 0x0A) {
                        if (fSendCharDataAsCharArray)
                            appendCharData(ch);
                        fLinefeedCounter++;
                        fCharacterCounter = 1;
                        ch = loadNextByte();
                        break;
                    }
                    if (ch == 0x0D) {
                        if (fSendCharDataAsCharArray)
                            appendCharData(0x0A);
                        fCarriageReturnCounter++;
                        fCharacterCounter = 1;
                        ch = loadNextByte();
                        if (ch == 0x0A) {
                            fLinefeedCounter++;
                            ch = loadNextByte();
                        }
                        break;
                    }
                    characters(charDataOffset, fCurrentOffset);
                    if (ch == 0 && atEOF(fCurrentOffset + 1)) {
                        changeReaders();
                        return XMLEntityHandler.CONTENT_RESULT_INVALID_CHAR; // REVISIT - not quite...
                    }
                    return XMLEntityHandler.CONTENT_RESULT_INVALID_CHAR;
                }
            } else {
                if (fSendCharDataAsCharArray) {
                    if (!copyMultiByteCharData(ch)) {
                        characters(charDataOffset, fCurrentOffset);
                        return XMLEntityHandler.CONTENT_RESULT_INVALID_CHAR;
                    }
                } else if (!skipMultiByteCharData(ch)) {
                    characters(charDataOffset, fCurrentOffset);
                    return XMLEntityHandler.CONTENT_RESULT_INVALID_CHAR;
                }
                ch = fMostRecentByte;
            }
        }
    }

    private int recognizeMarkup(int b0, QName element) throws Exception {
        switch (b0) {
        case 0:
            return XMLEntityHandler.CONTENT_RESULT_MARKUP_END_OF_INPUT;
        case '?':
            fCharacterCounter++;
            loadNextByte();
            return XMLEntityHandler.CONTENT_RESULT_START_OF_PI;
        case '!':
            fCharacterCounter++;
            b0 = loadNextByte();
            if (b0 == 0) {
                fCharacterCounter--;
                fCurrentOffset--;
                return XMLEntityHandler.CONTENT_RESULT_MARKUP_END_OF_INPUT;
            }
            if (b0 == '-') {
                fCharacterCounter++;
                b0 = loadNextByte();
                if (b0 == 0) {
                    fCharacterCounter -= 2;
                    fCurrentOffset -= 2;
                    return XMLEntityHandler.CONTENT_RESULT_MARKUP_END_OF_INPUT;
                }
                if (b0 == '-') {
                    fCharacterCounter++;
                    b0 = loadNextByte();
                    return XMLEntityHandler.CONTENT_RESULT_START_OF_COMMENT;
                }
                break;
            }
            if (b0 == '[') {
                for (int i = 0; i < 6; i++) {
                    fCharacterCounter++;
                    b0 = loadNextByte();
                    if (b0 == 0) {
                        fCharacterCounter -= (2 + i);
                        fCurrentOffset -= (2 + i);
                        return XMLEntityHandler.CONTENT_RESULT_MARKUP_END_OF_INPUT;
                    }
                    if (b0 != cdata_string[i]) {
                        return XMLEntityHandler.CONTENT_RESULT_MARKUP_NOT_RECOGNIZED;
                    }
                }
                fCharacterCounter++;
                loadNextByte();
                return XMLEntityHandler.CONTENT_RESULT_START_OF_CDSECT;
            }
            break;
        case '/':
            fCharacterCounter++;
            if (USE_OUT_OF_LINE_LOAD_NEXT_BYTE) {
                b0 = loadNextByte();
            } else {
                fCurrentOffset++;
                if (USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE) {
                    fCurrentIndex++;
                    try {
                        b0 = (fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF);
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
            int expectedName = element.rawname;
            fStringPool.getCharArrayRange(expectedName, fCharArrayRange);
            char[] expected = fCharArrayRange.chars;
            int offset = fCharArrayRange.offset;
            int len = fCharArrayRange.length;
            //
            // DEFECT !! - needs UTF8 multibyte support...
            //
            if (b0 == expected[offset++]) {
                UTF8DataChunk savedChunk = fCurrentChunk;
                int savedIndex = fCurrentIndex;
                int savedOffset = fCurrentOffset;
                for (int i = 1; i < len; i++) {
                    if (USE_OUT_OF_LINE_LOAD_NEXT_BYTE) {
                        b0 = loadNextByte();
                    } else {
                        fCurrentOffset++;
                        if (USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE) {
                            fCurrentIndex++;
                            try {
                                b0 = (fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF);
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
                    //
                    // DEFECT !! - needs UTF8 multibyte support...
                    //
                    if (b0 != expected[offset++]) {
                        fCurrentChunk = savedChunk;
                        fCurrentIndex = savedIndex;
                        fCurrentOffset = savedOffset;
                        fMostRecentData = fCurrentChunk.toByteArray();
                        fMostRecentByte = fMostRecentData[savedIndex] & 0xFF;
                        return XMLEntityHandler.CONTENT_RESULT_START_OF_ETAG;
                    }
                }
                fCharacterCounter += len; // REVISIT - double check this...
                fCharacterCounter++;
                if (USE_OUT_OF_LINE_LOAD_NEXT_BYTE) {
                    b0 = loadNextByte();
                } else {
                    fCurrentOffset++;
                    if (USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE) {
                        fCurrentIndex++;
                        try {
                            b0 = (fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF);
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
                if (b0 == '>') {
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
                    return XMLEntityHandler.CONTENT_RESULT_MATCHING_ETAG;
                }
                while (b0 == 0x20 || b0 == 0x09 || b0 == 0x0A || b0 == 0x0D) {
                    if (b0 == 0x0A) {
                        fLinefeedCounter++;
                        fCharacterCounter = 1;
                        b0 = loadNextByte();
                    } else if (b0 == 0x0D) {
                        fCarriageReturnCounter++;
                        fCharacterCounter = 1;
                        b0 = loadNextByte();
                        if (b0 == 0x0A) {
                            fLinefeedCounter++;
                            b0 = loadNextByte();
                        }
                    } else {
                        fCharacterCounter++;
                        b0 = loadNextByte();
                    }
                    if (b0 == '>') {
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
                        return XMLEntityHandler.CONTENT_RESULT_MATCHING_ETAG;
                    }
                }
                fCurrentChunk = savedChunk;
                fCurrentIndex = savedIndex;
                fCurrentOffset = savedOffset;
                fMostRecentData = fCurrentChunk.toByteArray();
                fMostRecentByte = fMostRecentData[savedIndex] & 0xFF;
            }
            return XMLEntityHandler.CONTENT_RESULT_START_OF_ETAG;
        default:
            return XMLEntityHandler.CONTENT_RESULT_START_OF_ELEMENT;
        }
        return XMLEntityHandler.CONTENT_RESULT_MARKUP_NOT_RECOGNIZED;
    }
}

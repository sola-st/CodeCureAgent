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

public class NameTokenScanner {
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

    public int addString(int offset, int length) {
        if (length == 0)
            return 0;
        return fCurrentChunk.addString(offset, length);
    }

    public int addSymbol(int offset, int length) {
        if (length == 0)
            return 0;
        return fCurrentChunk.addSymbol(offset, length, 0);
    }

    private int addSymbol(int offset, int length, int hashcode) {
        if (length == 0)
            return 0;
        return fCurrentChunk.addSymbol(offset, length, hashcode);
    }
    public void skipToChar(char ch) throws Exception {
        //
        // REVISIT - this will skip invalid characters without reporting them.
        //
        int b0 = fMostRecentByte;
        while (true) {
            if (b0 == ch) // ch will always be an ascii character
                return;
            if (b0 == 0) {
                if (atEOF(fCurrentOffset + 1)) {
                    changeReaders().skipToChar(ch);
                    return;
                }
                fCharacterCounter++;
            } else if (b0 == 0x0A) {
                fLinefeedCounter++;
                fCharacterCounter = 1;
            } else if (b0 == 0x0D) {
                fCarriageReturnCounter++;
                fCharacterCounter = 1;
                b0 = loadNextByte();
                if (b0 != 0x0A)
                    continue;
                fLinefeedCounter++;
            } else if (b0 < 0x80) { // 0xxxxxxx
                fCharacterCounter++;
            } else {
                fCharacterCounter++;
                if ((0xe0 & b0) == 0xc0) { // 110yyyyy 10xxxxxx
                    loadNextByte();
                } else if ((0xf0 & b0) == 0xe0) { // 1110zzzz 10yyyyyy 10xxxxxx
                    loadNextByte();
                    loadNextByte();
                } else { // 11110uuu 10uuzzzz 10yyyyyy 10xxxxxx
                    loadNextByte();
                    loadNextByte();
                    loadNextByte();
                }
            }
            b0 = loadNextByte();
        }
    }
    //
    //
    //
    public void skipPastSpaces() throws Exception {
        int ch = fMostRecentByte;
        while (true) {
            if (ch == 0x20 || ch == 0x09) {
                fCharacterCounter++;
            } else if (ch == 0x0A) {
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
                    continue;
                fLinefeedCounter++;
            } else {
                if (ch == 0 && atEOF(fCurrentOffset + 1))
                    changeReaders().skipPastSpaces();
                return;
            }
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
        }
    }

    public void skipPastName(char fastcheck) throws Exception {
        int b0 = fMostRecentByte;
        if (b0 < 0x80) {
            if (XMLCharacterProperties.fgAsciiInitialNameChar[b0] == 0)
                return;
        } else {
            if (!fCalledCharPropInit) {
                XMLCharacterProperties.initCharFlags();
                fCalledCharPropInit = true;
            }
            if (!skippedMultiByteCharWithFlag(b0, XMLCharacterProperties.E_InitialNameCharFlag))
                return;
        }
        while (true) {
            fCharacterCounter++;
            b0 = loadNextByte();
            if (fastcheck == b0)
                return;
            if (b0 < 0x80) {
                if (XMLCharacterProperties.fgAsciiNameChar[b0] == 0)
                    return;
            } else {
                if (!fCalledCharPropInit) {
                    XMLCharacterProperties.initCharFlags();
                    fCalledCharPropInit = true;
                }
                if (!skippedMultiByteCharWithFlag(b0, XMLCharacterProperties.E_NameCharFlag))
                    return;
            }
        }
    }

    public void skipPastNmtoken(char fastcheck) throws Exception {
        int b0 = fMostRecentByte;
        while (true) {
            if (fastcheck == b0)
                return;
            if (b0 < 0x80) {
                if (XMLCharacterProperties.fgAsciiNameChar[b0] == 0)
                    return;
            } else {
                if (!skippedMultiByteCharWithFlag(b0, XMLCharacterProperties.E_NameCharFlag))
                    return;
            }
            fCharacterCounter++;
            b0 = loadNextByte();
        }
    }

    public boolean skippedString(char[] s) throws Exception {
        int length = s.length;
        byte[] data = fMostRecentData;
        int index = fCurrentIndex + length;
        int sindex = length;
        try {
            while (sindex-- > 0) {
                if (data[--index] != s[sindex])
                    return false;
            }
            fCurrentIndex += length;
        } catch (ArrayIndexOutOfBoundsException ex) {
            int i = 0;
            index = fCurrentIndex;
            while (index < UTF8DataChunk.CHUNK_SIZE) {
                if (data[index++] != s[i++])
                    return false;
            }
            UTF8DataChunk dataChunk = fCurrentChunk;
            int savedOffset = fCurrentOffset;
            int savedIndex = fCurrentIndex;
            slowLoadNextByte();
            data = fMostRecentData;
            index = 0;
            while (i < length) {
                if (data[index++] != s[i++]) {
                    fCurrentChunk = dataChunk;
                    fCurrentIndex = savedIndex;
                    fCurrentOffset = savedOffset;
                    fMostRecentData = fCurrentChunk.toByteArray();
                    fMostRecentByte = fMostRecentData[savedIndex] & 0xFF;
                    return false;
                }
            }
            fCurrentIndex = index;
        }
        fCharacterCounter += length;
        fCurrentOffset += length;
        try {
            fMostRecentByte = data[fCurrentIndex] & 0xFF;
        } catch (ArrayIndexOutOfBoundsException ex) {
            slowLoadNextByte();
        }
        return true;
    }

    public int scanInvalidChar() throws Exception {
        int b0 = fMostRecentByte;
        int ch = b0;
        if (ch == 0x0A) {
            fLinefeedCounter++;
            fCharacterCounter = 1;
        } else if (ch == 0x0D) {
            fCarriageReturnCounter++;
            fCharacterCounter = 1;
            ch = loadNextByte();
            if (ch != 0x0A)
                return 0x0A;
            fLinefeedCounter++;
        } else if (ch == 0) {
            if (atEOF(fCurrentOffset + 1)) {
                return changeReaders().scanInvalidChar();
            }
            fCharacterCounter++;
        } else if (b0 >= 0x80) {
            fCharacterCounter++;
            int b1 = loadNextByte();
            int b2 = 0;
            if ((0xe0 & b0) == 0xc0) { // 110yyyyy 10xxxxxx
                ch = ((0x1f & b0)<<6) + (0x3f & b1);
            } else if ( (0xf0 & b0) == 0xe0 ) { 
                b2 = loadNextByte();
                ch = ((0x0f & b0)<<12) + ((0x3f & b1)<<6) + (0x3f & b2);
            } else if (( 0xf8 & b0 ) == 0xf0 ){
                b2 = loadNextByte();
                int b3 = loadNextByte(); // 11110uuu 10uuzzzz 10yyyyyy 10xxxxxx
                ch = ((0x0f & b0)<<18) + ((0x3f & b1)<<12)
                     + ((0x3f & b2)<<6) + (0x3f & b3);
            }
        }
        loadNextByte();
        return ch;
    }

    public int scanStringLiteral() throws Exception {
        boolean single;
        if (!(single = lookingAtChar('\'', true)) && !lookingAtChar('\"', true)) {
            return XMLEntityHandler.STRINGLIT_RESULT_QUOTE_REQUIRED;
        }
        int offset = fCurrentOffset;
        char qchar = single ? '\'' : '\"';
        while (!lookingAtChar(qchar, false)) {
            if (!lookingAtValidChar(true)) {
                return XMLEntityHandler.STRINGLIT_RESULT_INVALID_CHAR;
            }
        }
        int stringIndex = fCurrentChunk.addString(offset, fCurrentOffset - offset);
        lookingAtChar(qchar, true); // move past qchar
        return stringIndex;
    }
    public boolean scanExpectedName(char fastcheck, StringPool.CharArrayRange expectedName) throws Exception {
        char[] expected = expectedName.chars;
        int offset = expectedName.offset;
        int len = expectedName.length;
        int b0 = fMostRecentByte;
        int ch = 0;
        int i = 0;
        while (true) {
            if (b0 < 0x80) {
                ch = b0;
                if (i == len)
                    break;
                if (ch != expected[offset]) {
                    skipPastNmtoken(fastcheck);
                    return false;
                }
            } else {
                //
                // REVISIT - optimize this with in-buffer lookahead.
                //
                UTF8DataChunk saveChunk = fCurrentChunk;
                int saveIndex = fCurrentIndex;
                int saveOffset = fCurrentOffset;
                int b1;
                if (USE_OUT_OF_LINE_LOAD_NEXT_BYTE) {
                    b1 = loadNextByte();
                } else {
                    fCurrentOffset++;
                    if (USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE) {
                        fCurrentIndex++;
                        try {
                            b1 = fMostRecentData[fCurrentIndex] & 0xFF;
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            b1 = slowLoadNextByte();
                        }
                    } else {
                        if (++fCurrentIndex == UTF8DataChunk.CHUNK_SIZE)
                            b1 = slowLoadNextByte();
                        else
                            b1 = (fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF);
                    }
                }
                if ((0xe0 & b0) == 0xc0) { // 110yyyyy 10xxxxxx
                    ch = ((0x1f & b0)<<6) + (0x3f & b1);
                    if (i == len)
                        break;
                    if (ch != expected[offset]) {
                        fCurrentChunk = saveChunk;
                        fCurrentIndex = saveIndex;
                        fCurrentOffset = saveOffset;
                        fMostRecentData = saveChunk.toByteArray();
                        fMostRecentByte = b0;
                        skipPastNmtoken(fastcheck);
                        return false;
                    }
                } else {
                    int b2;
                    if (USE_OUT_OF_LINE_LOAD_NEXT_BYTE) {
                        b2 = loadNextByte();
                    } else {
                        fCurrentOffset++;
                        if (USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE) {
                            fCurrentIndex++;
                            try {
                                b2 = fMostRecentData[fCurrentIndex] & 0xFF;
                            } catch (ArrayIndexOutOfBoundsException ex) {
                                b2 = slowLoadNextByte();
                            }
                        } else {
                            if (++fCurrentIndex == UTF8DataChunk.CHUNK_SIZE)
                                b2 = slowLoadNextByte();
                            else
                                b2 = (fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF);
                        }
                    }
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
                        ch = ((0x0f & b0)<<12) + ((0x3f & b1)<<6) + (0x3f & b2);
                        if (i == len)
                            break;
                        if (ch != expected[offset]) {
                            fCurrentChunk = saveChunk;
                            fCurrentIndex = saveIndex;
                            fCurrentOffset = saveOffset;
                            fMostRecentData = saveChunk.toByteArray();
                            fMostRecentByte = b0;
                            skipPastNmtoken(fastcheck);
                            return false;
                        }
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
            i++;
            offset++;
            fCharacterCounter++;
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
        if (ch == fastcheck)
            return true;
        if (ch < 0x80) {
            if (XMLCharacterProperties.fgAsciiNameChar[ch] == 0)
                return true;
        } else {
            if (!fCalledCharPropInit) {
                XMLCharacterProperties.initCharFlags();
                fCalledCharPropInit = true;
            }
            if ((XMLCharacterProperties.fgCharFlags[ch] & XMLCharacterProperties.E_NameCharFlag) == 0)
                return true;
        }
        skipPastNmtoken(fastcheck);
        return false;
    }

    public void scanQName(char fastcheck, QName qname) throws Exception {
        int offset = fCurrentOffset;
        int ch = fMostRecentByte;
        if (ch < 0x80) {
            if (XMLCharacterProperties.fgAsciiInitialNameChar[ch] == 0) {
                qname.clear();
                return;
            }
            if (ch == ':') {
                qname.clear();
                return;
            }
        } else {
            if (!fCalledCharPropInit) {
                XMLCharacterProperties.initCharFlags();
                fCalledCharPropInit = true;
            }
            ch = getMultiByteSymbolChar(ch);
            fCurrentIndex--;
            fCurrentOffset--;
            if ((XMLCharacterProperties.fgCharFlags[ch] & XMLCharacterProperties.E_InitialNameCharFlag) == 0) {
                qname.clear();
                return;
            }
        }
        int index = fCurrentIndex;
        byte[] data = fMostRecentData;
        int prefixend = -1;
        while (true) {
            fCharacterCounter++;
            fCurrentOffset++;
            index++;
            try {
                ch = data[index] & 0xFF;
            } catch (ArrayIndexOutOfBoundsException ex) {
                ch = slowLoadNextByte();
                index = 0;
                data = fMostRecentData;
            }
            if (fastcheck == ch)
                break;
            if (ch < 0x80) {
                if (XMLCharacterProperties.fgAsciiNameChar[ch] == 0)
                    break;
                if (ch == ':') {
                    if (prefixend != -1)
                        break;
                    prefixend = fCurrentOffset;
                    //
                    // We need to peek ahead one character.  If the next character is not a
                    // valid initial name character, or is another colon, then we cannot meet
                    // both the Prefix and LocalPart productions for the QName production,
                    // which means that there is no Prefix and we need to terminate the QName
                    // at the first colon.
                    //
                    try {
                        ch = data[index + 1] & 0xFF;
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        UTF8DataChunk savedChunk = fCurrentChunk;
                        int savedOffset = fCurrentOffset;
                        ch = slowLoadNextByte();
                        fCurrentChunk = savedChunk;
                        fCurrentOffset = savedOffset;
                        fMostRecentData = fCurrentChunk.toByteArray();
                    }
                    boolean lpok = true;
                    if (ch < 0x80) {
                        if (XMLCharacterProperties.fgAsciiInitialNameChar[ch] == 0 || ch == ':')
                            lpok = false;
                    } else {
                        if (!fCalledCharPropInit) {
                            XMLCharacterProperties.initCharFlags();
                            fCalledCharPropInit = true;
                        }
                        if ((XMLCharacterProperties.fgCharFlags[ch] & XMLCharacterProperties.E_InitialNameCharFlag) == 0)
                            lpok = false;
                    }
                    ch = ':';
                    if (!lpok) {
                        prefixend = -1;
                        break;
                    }
                }
            } else {
                if (!fCalledCharPropInit) {
                    XMLCharacterProperties.initCharFlags();
                    fCalledCharPropInit = true;
                }
                fCurrentIndex = index;
                fMostRecentByte = ch;
                ch = getMultiByteSymbolChar(ch);
                fCurrentIndex--;
                fCurrentOffset--;
                index = fCurrentIndex;
                if ((XMLCharacterProperties.fgCharFlags[ch] & XMLCharacterProperties.E_NameCharFlag) == 0)
                    break;
            }
        }
        fCurrentIndex = index;
        fMostRecentByte = ch;
        int length = fCurrentOffset - offset;
        qname.rawname = addSymbol(offset, length);
        qname.prefix = prefixend == -1 ? -1 : addSymbol(offset, prefixend - offset);
        qname.localpart = prefixend == -1 ? qname.rawname : addSymbol(prefixend + 1, fCurrentOffset - (prefixend + 1));
        qname.uri = -1;

    } // scanQName(char,QName)

    private int getMultiByteSymbolChar(int b0) throws Exception {
        //
        // REVISIT - optimize this with in-buffer lookahead.
        //
        UTF8DataChunk saveChunk = fCurrentChunk;
        int saveIndex = fCurrentIndex;
        int saveOffset = fCurrentOffset;
        if (!fCalledCharPropInit) {
            XMLCharacterProperties.initCharFlags();
            fCalledCharPropInit = true;
        }
        int b1;
        if (USE_OUT_OF_LINE_LOAD_NEXT_BYTE) {
            b1 = loadNextByte();
        } else {
            fCurrentOffset++;
            if (USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE) {
                fCurrentIndex++;
                try {
                    b1 = fMostRecentData[fCurrentIndex] & 0xFF;
                } catch (ArrayIndexOutOfBoundsException ex) {
                    b1 = slowLoadNextByte();
                }
            } else {
                if (++fCurrentIndex == UTF8DataChunk.CHUNK_SIZE)
                    b1 = slowLoadNextByte();
                else
                    b1 = (fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF);
            }
        }
        if ((0xe0 & b0) == 0xc0) { // 110yyyyy 10xxxxxx
            int ch = ((0x1f & b0)<<6) + (0x3f & b1);
            if ((XMLCharacterProperties.fgCharFlags[ch] & XMLCharacterProperties.E_NameCharFlag) == 0) { // yyy yyxx xxxx (0x80 to 0x7ff)
                fCurrentChunk = saveChunk;
                fCurrentIndex = saveIndex;
                fCurrentOffset = saveOffset;
                fMostRecentData = saveChunk.toByteArray();
                fMostRecentByte = b0;
                return -1;
            }
            loadNextByte();
            return ch;
        }
        int b2;
        if (USE_OUT_OF_LINE_LOAD_NEXT_BYTE) {
            b2 = loadNextByte();
        } else {
            fCurrentOffset++;
            if (USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE) {
                fCurrentIndex++;
                try {
                    b2 = fMostRecentData[fCurrentIndex] & 0xFF;
                } catch (ArrayIndexOutOfBoundsException ex) {
                    b2 = slowLoadNextByte();
                }
            } else {
                if (++fCurrentIndex == UTF8DataChunk.CHUNK_SIZE)
                    b2 = slowLoadNextByte();
                else
                    b2 = (fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF);
            }
        }
        if ((0xf0 & b0) == 0xe0) { // 1110zzzz 10yyyyyy 10xxxxxx
            // if ((ch >= 0xD800 && ch <= 0xDFFF) || ch >= 0xFFFE)
            if ((b0 == 0xED && b1 >= 0xA0) || (b0 == 0xEF && b1 == 0xBF && b2 >= 0xBE)) {
                fCurrentChunk = saveChunk;
                fCurrentIndex = saveIndex;
                fCurrentOffset = saveOffset;
                fMostRecentData = saveChunk.toByteArray();
                fMostRecentByte = b0;
                return -1;
            }
            int ch = ((0x0f & b0)<<12) + ((0x3f & b1)<<6) + (0x3f & b2);
            if ((XMLCharacterProperties.fgCharFlags[ch] & XMLCharacterProperties.E_NameCharFlag) == 0) { // zzzz yyyy yyxx xxxx (0x800 to 0xffff)
                fCurrentChunk = saveChunk;
                fCurrentIndex = saveIndex;
                fCurrentOffset = saveOffset;
                fMostRecentData = saveChunk.toByteArray();
                fMostRecentByte = b0;
                return -1;
            }
            loadNextByte();
            return ch;
        }
        // 11110uuu 10uuzzzz 10yyyyyy 10xxxxxx
        fCurrentChunk = saveChunk;
        fCurrentIndex = saveIndex;
        fCurrentOffset = saveOffset;
        fMostRecentData = saveChunk.toByteArray();
        fMostRecentByte = b0;
        return -1;
    }
    public int scanName(char fastcheck) throws Exception {
        int b0 = fMostRecentByte;
        int ch;
        if (b0 < 0x80) {
            if (XMLCharacterProperties.fgAsciiInitialNameChar[b0] == 0) {
                if (b0 == 0 && atEOF(fCurrentOffset + 1)) {
                    return changeReaders().scanName(fastcheck);
                }
                return -1;
            }
            ch = b0;
        } else {
            //
            // REVISIT - optimize this with in-buffer lookahead.
            //
            UTF8DataChunk saveChunk = fCurrentChunk;
            int saveIndex = fCurrentIndex;
            int saveOffset = fCurrentOffset;
            if (!fCalledCharPropInit) {
                XMLCharacterProperties.initCharFlags();
                fCalledCharPropInit = true;
            }
            int b1;
            if (USE_OUT_OF_LINE_LOAD_NEXT_BYTE) {
                b1 = loadNextByte();
            } else {
                fCurrentOffset++;
                if (USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE) {
                    fCurrentIndex++;
                    try {
                        b1 = fMostRecentData[fCurrentIndex] & 0xFF;
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        b1 = slowLoadNextByte();
                    }
                } else {
                    if (++fCurrentIndex == UTF8DataChunk.CHUNK_SIZE)
                        b1 = slowLoadNextByte();
                    else
                        b1 = (fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF);
                }
            }
            if ((0xe0 & b0) == 0xc0) { // 110yyyyy 10xxxxxx
                ch = ((0x1f & b0)<<6) + (0x3f & b1);
                if ((XMLCharacterProperties.fgCharFlags[ch] & XMLCharacterProperties.E_InitialNameCharFlag) == 0) { // yyy yyxx xxxx (0x80 to 0x7ff)
                    fCurrentChunk = saveChunk;
                    fCurrentIndex = saveIndex;
                    fCurrentOffset = saveOffset;
                    fMostRecentData = saveChunk.toByteArray();
                    fMostRecentByte = b0;
                    return -1;
                }
            } else {
                int b2;
                if (USE_OUT_OF_LINE_LOAD_NEXT_BYTE) {
                    b2 = loadNextByte();
                } else {
                    fCurrentOffset++;
                    if (USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE) {
                        fCurrentIndex++;
                        try {
                            b2 = fMostRecentData[fCurrentIndex] & 0xFF;
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            b2 = slowLoadNextByte();
                        }
                    } else {
                        if (++fCurrentIndex == UTF8DataChunk.CHUNK_SIZE)
                            b2 = slowLoadNextByte();
                        else
                            b2 = (fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF);
                    }
                }
                if ((0xf0 & b0) == 0xe0) { // 1110zzzz 10yyyyyy 10xxxxxx
                    // if ((ch >= 0xD800 && ch <= 0xDFFF) || ch >= 0xFFFE)
                    if ((b0 == 0xED && b1 >= 0xA0) || (b0 == 0xEF && b1 == 0xBF && b2 >= 0xBE)) {
                        fCurrentChunk = saveChunk;
                        fCurrentIndex = saveIndex;
                        fCurrentOffset = saveOffset;
                        fMostRecentData = saveChunk.toByteArray();
                        fMostRecentByte = b0;
                        return -1;
                    }
                    ch = ((0x0f & b0)<<12) + ((0x3f & b1)<<6) + (0x3f & b2);
                    if ((XMLCharacterProperties.fgCharFlags[ch] & XMLCharacterProperties.E_InitialNameCharFlag) == 0) { // zzzz yyyy yyxx xxxx (0x800 to 0xffff)
                        fCurrentChunk = saveChunk;
                        fCurrentIndex = saveIndex;
                        fCurrentOffset = saveOffset;
                        fMostRecentData = saveChunk.toByteArray();
                        fMostRecentByte = b0;
                        return -1;
                    }
                } else { // 11110uuu 10uuzzzz 10yyyyyy 10xxxxxx
                    fCurrentChunk = saveChunk;
                    fCurrentIndex = saveIndex;
                    fCurrentOffset = saveOffset;
                    fMostRecentData = saveChunk.toByteArray();
                    fMostRecentByte = b0;
                    return -1;
                }
            }
        }
        fCharacterCounter++;
        if (USE_OUT_OF_LINE_LOAD_NEXT_BYTE) {
            b0 = loadNextByte();
        } else {
            fCurrentOffset++;
            if (USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE) {
                fCurrentIndex++;
                try {
                    b0 = fMostRecentByte = fMostRecentData[fCurrentIndex] & 0xFF;
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
        return scanMatchingName(ch, b0, fastcheck);
    }
    private int scanMatchingName(int ch, int b0, int fastcheck) throws Exception {
        SymbolCache cache = fStringPool.getSymbolCache();
        int[][] cacheLines = cache.fCacheLines;
        char[] symbolChars = cache.fSymbolChars;
        boolean lengthOfOne = fastcheck == fMostRecentByte;
        int startOffset = cache.fSymbolCharsOffset;
        int entry = 0;
        int[] entries = cacheLines[entry];
        int offset = 1 + ((entries[0] - 1) * SymbolCache.CACHE_RECORD_SIZE);
        int totalMisses = 0;
        if (lengthOfOne) {
            while (offset > 0) {
                if (entries[offset + SymbolCache.CHAR_OFFSET] == ch) {
                    if (entries[offset + SymbolCache.INDEX_OFFSET] != -1) {
                        int symbolIndex = entries[offset + SymbolCache.INDEX_OFFSET];
                        if (totalMisses > 3)
                            fStringPool.updateCacheLine(symbolIndex, totalMisses, 1);
                        return symbolIndex;
                    }
                    break;
                }
                offset -= SymbolCache.CACHE_RECORD_SIZE;
                totalMisses++;
            }
            try {
                symbolChars[cache.fSymbolCharsOffset] = (char)ch;
            } catch (ArrayIndexOutOfBoundsException ex) {
                symbolChars = new char[cache.fSymbolCharsOffset * 2];
                System.arraycopy(cache.fSymbolChars, 0, symbolChars, 0, cache.fSymbolCharsOffset);
                cache.fSymbolChars = symbolChars;
                symbolChars[cache.fSymbolCharsOffset] = (char)ch;
            }
            cache.fSymbolCharsOffset++;
            if (offset < 0) {
                offset = 1 + (entries[0] * SymbolCache.CACHE_RECORD_SIZE);
                entries[0]++;
                try {
                    entries[offset + SymbolCache.CHAR_OFFSET] = ch;
                } catch (ArrayIndexOutOfBoundsException ex) {
                    int newSize = 1 + ((offset - 1) * 2);
                    entries = new int[newSize];
                    System.arraycopy(cacheLines[entry], 0, entries, 0, offset);
                    cacheLines[entry] = entries;
                    entries[offset + SymbolCache.CHAR_OFFSET] = ch;
                }
                entries[offset + SymbolCache.NEXT_OFFSET] = -1;
            }
            int result = fStringPool.createNonMatchingSymbol(startOffset, entry, entries, offset);
            return result;
        }
        try {
            symbolChars[cache.fSymbolCharsOffset] = (char)ch;
        } catch (ArrayIndexOutOfBoundsException ex) {
            symbolChars = new char[cache.fSymbolCharsOffset * 2];
            System.arraycopy(cache.fSymbolChars, 0, symbolChars, 0, cache.fSymbolCharsOffset);
            cache.fSymbolChars = symbolChars;
            symbolChars[cache.fSymbolCharsOffset] = (char)ch;
        }
        cache.fSymbolCharsOffset++;
        int depth = 1;
        while (true) {
            if (offset < 0)
                break;
            if (entries[offset + SymbolCache.CHAR_OFFSET] != ch) {
                offset -= SymbolCache.CACHE_RECORD_SIZE;
                totalMisses++;
                continue;
            }
            if (b0 >= 0x80) {
                ch = getMultiByteSymbolChar(b0);
                b0 = fMostRecentByte;
            } else if (b0 == fastcheck || XMLCharacterProperties.fgAsciiNameChar[b0] == 0) {
                ch = -1;
            } else {
                ch = b0;
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
            }
            if (ch == -1) {
                if (entries[offset + SymbolCache.INDEX_OFFSET] == -1) {
                    return fStringPool.createNonMatchingSymbol(startOffset, entry, entries, offset);
                }
                cache.fSymbolCharsOffset = startOffset;
                int symbolIndex = entries[offset + SymbolCache.INDEX_OFFSET];
                if (totalMisses > (depth * 3))
                    fStringPool.updateCacheLine(symbolIndex, totalMisses, depth);
                return symbolIndex;
            }
            try {
                symbolChars[cache.fSymbolCharsOffset] = (char)ch;
            } catch (ArrayIndexOutOfBoundsException ex) {
                symbolChars = new char[cache.fSymbolCharsOffset * 2];
                System.arraycopy(cache.fSymbolChars, 0, symbolChars, 0, cache.fSymbolCharsOffset);
                cache.fSymbolChars = symbolChars;
                symbolChars[cache.fSymbolCharsOffset] = (char)ch;
            }
            cache.fSymbolCharsOffset++;
            entry = entries[offset + SymbolCache.NEXT_OFFSET];
            try {
                entries = cacheLines[entry];
            } catch (ArrayIndexOutOfBoundsException ex) {
                if (entry == -1) {
                    entry = cache.fCacheLineCount++;
                    entries[offset + SymbolCache.NEXT_OFFSET] = entry;
                    entries = new int[1+(SymbolCache.INITIAL_CACHE_RECORD_COUNT*SymbolCache.CACHE_RECORD_SIZE)];
                    try {
                        cacheLines[entry] = entries;
                    } catch (ArrayIndexOutOfBoundsException ex2) {
                        cacheLines = new int[entry * 2][];
                        System.arraycopy(cache.fCacheLines, 0, cacheLines, 0, entry);
                        cache.fCacheLines = cacheLines;
                        cacheLines[entry] = entries;
                    }
                } else {
                    entries = cacheLines[entry];
                    throw new RuntimeException("RDR001 untested"); // REVISIT
                }
            }
            offset = 1 + ((entries[0] - 1) * SymbolCache.CACHE_RECORD_SIZE);
            depth++;
        }
        if (offset < 0)
            offset = 1 + (entries[0] * SymbolCache.CACHE_RECORD_SIZE);
        while (true) {
            entries[0]++;
            try {
                entries[offset + SymbolCache.CHAR_OFFSET] = ch;
            } catch (ArrayIndexOutOfBoundsException ex) {
                int newSize = 1 + ((offset - 1) * 2);
                entries = new int[newSize];
                System.arraycopy(cacheLines[entry], 0, entries, 0, offset);
                cacheLines[entry] = entries;
                entries[offset + SymbolCache.CHAR_OFFSET] = ch;
            }
            if (b0 >= 0x80) {
                ch = getMultiByteSymbolChar(b0);
                b0 = fMostRecentByte;
            } else if (b0 == fastcheck || XMLCharacterProperties.fgAsciiNameChar[b0] == 0) {
                ch = -1;
            } else {
                ch = b0;
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
            }
            if (ch == -1) {
                entries[offset + SymbolCache.NEXT_OFFSET] = -1;
                break;
            }
            entry = cache.fCacheLineCount++;
            entries[offset + SymbolCache.INDEX_OFFSET] = -1;
            entries[offset + SymbolCache.NEXT_OFFSET] = entry;
            entries = new int[1+(SymbolCache.INITIAL_CACHE_RECORD_COUNT*SymbolCache.CACHE_RECORD_SIZE)];
            try {
                cacheLines[entry] = entries;
            } catch (ArrayIndexOutOfBoundsException ex) {
                cacheLines = new int[entry * 2][];
                System.arraycopy(cache.fCacheLines, 0, cacheLines, 0, entry);
                cache.fCacheLines = cacheLines;
                cacheLines[entry] = entries;
            }
            offset = 1;
            try {
                symbolChars[cache.fSymbolCharsOffset] = (char)ch;
            } catch (ArrayIndexOutOfBoundsException ex) {
                symbolChars = new char[cache.fSymbolCharsOffset * 2];
                System.arraycopy(cache.fSymbolChars, 0, symbolChars, 0, cache.fSymbolCharsOffset);
                cache.fSymbolChars = symbolChars;
                symbolChars[cache.fSymbolCharsOffset] = (char)ch;
            }
            cache.fSymbolCharsOffset++;
        }

        int result = fStringPool.createNonMatchingSymbol(startOffset, entry, entries, offset);
        return result;
    }
}

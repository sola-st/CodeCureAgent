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

public class EntityReferenceHandler {
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
    
    private int recognizeReference(int ch) throws Exception {
        if (ch == 0) {
            return XMLEntityHandler.CONTENT_RESULT_REFERENCE_END_OF_INPUT;
        }
        //
        // [67] Reference ::= EntityRef | CharRef
        // [68] EntityRef ::= '&' Name ';'
        // [66] CharRef ::= '&#' [0-9]+ ';' | '&#x' [0-9a-fA-F]+ ';'
        //
        if (ch == '#') {
            fCharacterCounter++;
            loadNextByte();
            return XMLEntityHandler.CONTENT_RESULT_START_OF_CHARREF;
        } else {
            return XMLEntityHandler.CONTENT_RESULT_START_OF_ENTITYREF;
        }
    }

    public int scanEntityValue(int qchar, boolean createString) throws Exception
     {
         int offset = fCurrentOffset;
         int b0 = fMostRecentByte;
         while (true) {
             if (b0 < 0x80) {
                 switch (fgAsciiEntityValueChar[b0]) {
                 case 1: // quote char
                     if (b0 == qchar) {
                         if (!createString)
                             return XMLEntityHandler.ENTITYVALUE_RESULT_FINISHED;
                         int length = fCurrentOffset - offset;
                         int result = length == 0 ? StringPool.EMPTY_STRING : fCurrentChunk.addString(offset, length);
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
                         return result;
                     }
                     // the other quote character is not special
                     // fall through
                 case 0: // non-special char
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
                     continue;
                 case 5: // linefeed
                     fLinefeedCounter++;
                     fCharacterCounter = 1;
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
                     continue;
                 case 6: // carriage-return
                     fCarriageReturnCounter++;
                     fCharacterCounter = 1;
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
                     if (b0 != 0x0A) {
                         continue;
                     }
                     fLinefeedCounter++;
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
                     continue;
                 case 2: // reference
                     return XMLEntityHandler.ENTITYVALUE_RESULT_REFERENCE;
                 case 3: // peref
                     return XMLEntityHandler.ENTITYVALUE_RESULT_PEREF;
                 case 7:
                     if (atEOF(fCurrentOffset + 1)) {
                         changeReaders(); // do not call next reader, our caller may need to change the parameters
                         return XMLEntityHandler.ENTITYVALUE_RESULT_END_OF_INPUT;
                     }
                     // fall into...
                 case 4: // invalid
                     return XMLEntityHandler.ENTITYVALUE_RESULT_INVALID_CHAR;
                 }
             } else {
                 if (!skipMultiByteCharData(b0))
                     return XMLEntityHandler.ENTITYVALUE_RESULT_INVALID_CHAR;
                 b0 = fMostRecentByte;
             }
         }
    }

    public int scanCharRef(boolean hex) throws Exception {
        int ch = fMostRecentByte;
        if (ch == 0) {
            if (atEOF(fCurrentOffset + 1)) {
                return changeReaders().scanCharRef(hex);
            }
            return XMLEntityHandler.CHARREF_RESULT_INVALID_CHAR;
        }
        int num = 0;
        if (hex) {
            if (ch > 'f' || XMLCharacterProperties.fgAsciiXDigitChar[ch] == 0)
                return XMLEntityHandler.CHARREF_RESULT_INVALID_CHAR;
            num = ch - (ch < 'A' ? '0' : (ch < 'a' ? 'A' : 'a') - 10);
        } else {
            if (ch < '0' || ch > '9')
                return XMLEntityHandler.CHARREF_RESULT_INVALID_CHAR;
            num = ch - '0';
        }
        fCharacterCounter++;
        loadNextByte();
        boolean toobig = false;
        while (true) {
            ch = fMostRecentByte;
            if (ch == 0)
                break;
            if (hex) {
                if (ch > 'f' || XMLCharacterProperties.fgAsciiXDigitChar[ch] == 0)
                    break;
            } else {
                if (ch < '0' || ch > '9')
                    break;
            }
            fCharacterCounter++;
            loadNextByte();
            if (hex) {
                int dig = ch - (ch < 'A' ? '0' : (ch < 'a' ? 'A' : 'a') - 10);
                num = (num << 4) + dig;
            } else {
                int dig = ch - '0';
                num = (num * 10) + dig;
            }
            if (num > 0x10FFFF) {
                toobig = true;
                num = 0;
            }
        }
        if (ch != ';')
            return XMLEntityHandler.CHARREF_RESULT_SEMICOLON_REQUIRED;
        fCharacterCounter++;
        loadNextByte();
        if (toobig)
            return XMLEntityHandler.CHARREF_RESULT_OUT_OF_RANGE;
        return num;
    }
}

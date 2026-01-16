/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 1999,2000 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:  
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Xerces" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 1999, International
 * Business Machines, Inc., http://www.apache.org.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

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
 
 /**
  * This is the primary reader used for UTF-8 encoded byte streams.
  * <p>
  * This reader processes requests from the scanners against the
  * underlying UTF-8 byte stream, avoiding when possible any up-front
  * transcoding.  When the StringPool handle interfaces are used,
  * the information in the data stream will be added to the string
  * pool and lazy-evaluated until asked for.
  * <p>
  * We use the SymbolCache to match expected names (element types in
  * end tags) and walk the data structures of that class directly.
  * <p>
  * There is a significant amount of hand-inlining and some blatant
  * voilation of good object oriented programming rules, ignoring
  * boundaries of modularity, etc., in the name of good performance.
  * <p>
  * There are also some places where the code here frequently crashes
  * the SUN java runtime compiler (JIT) and the code here has been
  * carefully "crafted" to avoid those problems.
  * 
  * @version $Id$
  */
 final class UTF8Reader extends XMLEntityReader {
     //
     //
     //
     private final static boolean USE_OUT_OF_LINE_LOAD_NEXT_BYTE = false;
     private final static boolean USE_TRY_CATCH_FOR_LOAD_NEXT_BYTE = true;

    private UTF8DataChunkManager chunkManager;
    private EntityReferenceHandler entityReferenceHandler;
    private CharDataHandler charDataHandler;
    private ContentScanner contentScanner;
    private NameTokenScanner nameTokenScanner;
    private InputStreamProcessor inputStreamProcessor;
     //
     //
     //
     public UTF8Reader(XMLEntityHandler entityHandler, XMLErrorReporter errorReporter, boolean sendCharDataAsCharArray, InputStream dataStream, StringPool stringPool) throws Exception {
        this.chunkManager = new UTF8DataChunkManager();
        this.entityReferenceHandler = new EntityReferenceHandler();
        this.charDataHandler = new CharDataHandler();
        this.contentScanner = new ContentScanner();
        this.nameTokenScanner = new NameTokenScanner();
        this.inputStreamProcessor = new InputStreamProcessor(); 
        super(entityHandler, errorReporter, sendCharDataAsCharArray);
         fInputStream = dataStream;
         fStringPool = stringPool;
         fCharArrayRange = fStringPool.createCharArrayRange();
         fCurrentChunk = UTF8DataChunk.createChunk(fStringPool, null);
         chunkManager.fillCurrentChunk();
        
     }
     //
     // [10] AttValue ::= '"' ([^<&"] | Reference)* '"'
     //                   | "'" ([^<&'] | Reference)* "'"
     //
     // The values in the following table are defined as:
     //
     //      0 - not special
     //      1 - quote character
     //      2 - complex
     //      3 - less than
     //      4 - invalid
     //
     public static final byte fgAsciiAttValueChar[] = {
         4, 4, 4, 4, 4, 4, 4, 4, 4, 2, 2, 4, 4, 2, 4, 4, // tab is 0x09,  LF is 0x0A,  CR is 0x0D
         4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
         0, 0, 1, 0, 0, 0, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, // '\"' is 0x22, '&' is 0x26, '\'' is 0x27
         0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, // '<' is 0x3C
         0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
         0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
         0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
         0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
     };
     public int scanAttValue(char qchar, boolean asSymbol) throws Exception
     {
         int offset = fCurrentOffset;
         int b0 = fMostRecentByte;
         while (true) {
             if (b0 < 0x80) {
                 switch (fgAsciiAttValueChar[b0]) {
                 case 1: // quote char
                     if (b0 == qchar) {
                         int length = fCurrentOffset - offset;
                         int result = length == 0 ? StringPool.EMPTY_STRING : (asSymbol ? fCurrentChunk.addSymbol(offset, length, 0) : fCurrentChunk.addString(offset, length));
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
                 case 2: // complex
                     return XMLEntityHandler.ATTVALUE_RESULT_COMPLEX;
                 case 3: // less than
                     return XMLEntityHandler.ATTVALUE_RESULT_LESSTHAN;
                 case 4: // invalid
                     return XMLEntityHandler.ATTVALUE_RESULT_INVALID_CHAR;
                 }
             } else {
                 if (!skipMultiByteCharData(b0))
                     return XMLEntityHandler.ATTVALUE_RESULT_INVALID_CHAR;
                 b0 = fMostRecentByte;
             }
         }
     }
     //
     // [9] EntityValue ::= '"' ([^%&"] | PEReference | Reference)* '"'
     //                     | "'" ([^%&'] | PEReference | Reference)* "'"
     //
     // The values in the following table are defined as:
     //
     //      0 - not special
     //      1 - quote character
     //      2 - reference
     //      3 - peref
     //      4 - invalid
     //      5 - linefeed
     //      6 - carriage-return
     //      7 - end of input
     //
     public static final byte fgAsciiEntityValueChar[] = {
         7, 4, 4, 4, 4, 4, 4, 4, 4, 0, 5, 4, 4, 6, 4, 4, // tab is 0x09,  LF is 0x0A,  CR is 0x0D
         4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
         0, 0, 1, 0, 0, 3, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, // '\"', '%', '&', '\''
         0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
         0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
         0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
         0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
         0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
     };
     
     //
     //
     //
     
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

 }
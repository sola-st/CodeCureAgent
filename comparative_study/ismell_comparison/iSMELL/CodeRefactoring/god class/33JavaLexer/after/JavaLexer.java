// $ANTLR 2.7.1: "java.g" -> "JavaLexer.java"$

package org.argouml.language.java.generator;

import java.util.Vector;

import java.io.InputStream;
import antlr.TokenStreamException;
import antlr.TokenStreamIOException;
import antlr.TokenStreamRecognitionException;
import antlr.CharStreamException;
import antlr.CharStreamIOException;
import antlr.ANTLRException;
import java.io.Reader;
import java.util.Hashtable;
import antlr.CharScanner;
import antlr.InputBuffer;
import antlr.ByteBuffer;
import antlr.CharBuffer;
import antlr.Token;
import antlr.CommonToken;
import antlr.RecognitionException;
import antlr.NoViableAltForCharException;
import antlr.MismatchedCharException;
import antlr.TokenStream;
import antlr.ANTLRHashString;
import antlr.LexerSharedInputState;
import antlr.collections.impl.BitSet;
import antlr.SemanticException;

public class JavaLexer extends BaseLexer implements TokenStream
{
public JavaLexer(InputStream in) {
this(new ByteBuffer(in));
}
public JavaLexer(Reader in) {
this(new CharBuffer(in));
}
public JavaLexer(InputBuffer ib) {
this(new LexerSharedInputState(ib));
}
public JavaLexer(LexerSharedInputState state) {
super(state);
literals = new Hashtable();
literals.put(new ANTLRHashString("byte", this), new Integer(49));
literals.put(new ANTLRHashString("public", this), new Integer(60));
literals.put(new ANTLRHashString("case", this), new Integer(90));
literals.put(new ANTLRHashString("short", this), new Integer(51));
literals.put(new ANTLRHashString("break", this), new Integer(85));
literals.put(new ANTLRHashString("while", this), new Integer(83));
literals.put(new ANTLRHashString("new", this), new Integer(135));
literals.put(new ANTLRHashString("instanceof", this), new Integer(118));
literals.put(new ANTLRHashString("implements", this), new Integer(74));
literals.put(new ANTLRHashString("synchronized", this), new Integer(65));
literals.put(new ANTLRHashString("float", this), new Integer(53));
literals.put(new ANTLRHashString("package", this), new Integer(41));
literals.put(new ANTLRHashString("return", this), new Integer(87));
literals.put(new ANTLRHashString("throw", this), new Integer(89));
literals.put(new ANTLRHashString("null", this), new Integer(134));
literals.put(new ANTLRHashString("protected", this), new Integer(61));
literals.put(new ANTLRHashString("class", this), new Integer(68));
literals.put(new ANTLRHashString("throws", this), new Integer(78));
literals.put(new ANTLRHashString("do", this), new Integer(84));
literals.put(new ANTLRHashString("strictfp", this), new Integer(67));
literals.put(new ANTLRHashString("super", this), new Integer(131));
literals.put(new ANTLRHashString("transient", this), new Integer(63));
literals.put(new ANTLRHashString("native", this), new Integer(64));
literals.put(new ANTLRHashString("interface", this), new Integer(70));
literals.put(new ANTLRHashString("final", this), new Integer(39));
literals.put(new ANTLRHashString("if", this), new Integer(80));
literals.put(new ANTLRHashString("double", this), new Integer(55));
literals.put(new ANTLRHashString("volatile", this), new Integer(66));
literals.put(new ANTLRHashString("catch", this), new Integer(94));
literals.put(new ANTLRHashString("try", this), new Integer(92));
literals.put(new ANTLRHashString("int", this), new Integer(52));
literals.put(new ANTLRHashString("for", this), new Integer(82));
literals.put(new ANTLRHashString("extends", this), new Integer(69));
literals.put(new ANTLRHashString("boolean", this), new Integer(48));
literals.put(new ANTLRHashString("char", this), new Integer(50));
literals.put(new ANTLRHashString("private", this), new Integer(59));
literals.put(new ANTLRHashString("default", this), new Integer(91));
literals.put(new ANTLRHashString("false", this), new Integer(133));
literals.put(new ANTLRHashString("this", this), new Integer(130));
literals.put(new ANTLRHashString("static", this), new Integer(62));
literals.put(new ANTLRHashString("abstract", this), new Integer(40));
literals.put(new ANTLRHashString("continue", this), new Integer(86));
literals.put(new ANTLRHashString("finally", this), new Integer(93));
literals.put(new ANTLRHashString("else", this), new Integer(81));
literals.put(new ANTLRHashString("import", this), new Integer(43));
literals.put(new ANTLRHashString("void", this), new Integer(47));
literals.put(new ANTLRHashString("switch", this), new Integer(88));
literals.put(new ANTLRHashString("true", this), new Integer(132));
literals.put(new ANTLRHashString("long", this), new Integer(54));
caseSensitiveLiterals = true;
setCaseSensitive(true);

literalHandler = new LiteralHandler(state);
        numberHandler = new NumberHandler(state);
        identifierHandler = new IdentifierHandler(state, new Hashtable());
        whitespaceAndCommentHandler = new WhitespaceAndCommentHandler(state);
        operatorHandler = new OperatorHandler(state);
}

public Token nextToken() throws TokenStreamException {
Token theRetToken=null;
tryAgain:
for (;;) {
    Token _token = null;
    int _ttype = Token.INVALID_TYPE;
    resetText();
    try {   // for char stream error handling
        try {   // for lexical error handling
            switch ( LA(1)) {
            case '?':
            {
                mQUESTION(true);
                theRetToken=_returnToken;
                break;
            }
            case '(':
            {
                mLPAREN(true);
                theRetToken=_returnToken;
                break;
            }
            case ')':
            {
                mRPAREN(true);
                theRetToken=_returnToken;
                break;
            }
            case '[':
            {
                mLBRACK(true);
                theRetToken=_returnToken;
                break;
            }
            case ']':
            {
                mRBRACK(true);
                theRetToken=_returnToken;
                break;
            }
            case '{':
            {
                mLCURLY(true);
                theRetToken=_returnToken;
                break;
            }
            case '}':
            {
                mRCURLY(true);
                theRetToken=_returnToken;
                break;
            }
            case ':':
            {
                mCOLON(true);
                theRetToken=_returnToken;
                break;
            }
            case ',':
            {
                mCOMMA(true);
                theRetToken=_returnToken;
                break;
            }
            case '~':
            {
                mBNOT(true);
                theRetToken=_returnToken;
                break;
            }
            case ';':
            {
                mSEMI(true);
                theRetToken=_returnToken;
                break;
            }
            case '\t':  case '\n':  case '\u000c':  case '\r':
            case ' ':
            {
                mWS(true);
                theRetToken=_returnToken;
                break;
            }
            case '\'':
            {
                mCHAR_LITERAL(true);
                theRetToken=_returnToken;
                break;
            }
            case '"':
            {
                mSTRING_LITERAL(true);
                theRetToken=_returnToken;
                break;
            }
            case '$':  case 'A':  case 'B':  case 'C':
            case 'D':  case 'E':  case 'F':  case 'G':
            case 'H':  case 'I':  case 'J':  case 'K':
            case 'L':  case 'M':  case 'N':  case 'O':
            case 'P':  case 'Q':  case 'R':  case 'S':
            case 'T':  case 'U':  case 'V':  case 'W':
            case 'X':  case 'Y':  case 'Z':  case '_':
            case 'a':  case 'b':  case 'c':  case 'd':
            case 'e':  case 'f':  case 'g':  case 'h':
            case 'i':  case 'j':  case 'k':  case 'l':
            case 'm':  case 'n':  case 'o':  case 'p':
            case 'q':  case 'r':  case 's':  case 't':
            case 'u':  case 'v':  case 'w':  case 'x':
            case 'y':  case 'z':
            {
                mIDENT(true);
                theRetToken=_returnToken;
                break;
            }
            case '.':  case '0':  case '1':  case '2':
            case '3':  case '4':  case '5':  case '6':
            case '7':  case '8':  case '9':
            {
                mNUM_INT(true);
                theRetToken=_returnToken;
                break;
            }
            default:
                if ((LA(1)=='>') && (LA(2)=='>') && (LA(3)=='>') && (LA(4)=='=')) {
                    mBSR_ASSIGN(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='>') && (LA(2)=='>') && (LA(3)=='=')) {
                    mSR_ASSIGN(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='>') && (LA(2)=='>') && (LA(3)=='>') && (true)) {
                    mBSR(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='<') && (LA(2)=='<') && (LA(3)=='=')) {
                    mSL_ASSIGN(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='/') && (LA(2)=='*') && (LA(3)=='*')) {
                    mJAVADOC(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='/') && (LA(2)=='*') && (_tokenSet_0.member(LA(3)))) {
                    mML_COMMENT(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='=') && (LA(2)=='=')) {
                    mEQUAL(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='!') && (LA(2)=='=')) {
                    mNOT_EQUAL(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='/') && (LA(2)=='=')) {
                    mDIV_ASSIGN(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='+') && (LA(2)=='=')) {
                    mPLUS_ASSIGN(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='+') && (LA(2)=='+')) {
                    mINC(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='-') && (LA(2)=='=')) {
                    mMINUS_ASSIGN(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='-') && (LA(2)=='-')) {
                    mDEC(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='*') && (LA(2)=='=')) {
                    mSTAR_ASSIGN(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='%') && (LA(2)=='=')) {
                    mMOD_ASSIGN(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='>') && (LA(2)=='>') && (true)) {
                    mSR(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='>') && (LA(2)=='=')) {
                    mGE(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='<') && (LA(2)=='<') && (true)) {
                    mSL(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='<') && (LA(2)=='=')) {
                    mLE(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='^') && (LA(2)=='=')) {
                    mBXOR_ASSIGN(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='|') && (LA(2)=='=')) {
                    mBOR_ASSIGN(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='|') && (LA(2)=='|')) {
                    mLOR(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='&') && (LA(2)=='=')) {
                    mBAND_ASSIGN(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='&') && (LA(2)=='&')) {
                    mLAND(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='/') && (LA(2)=='/')) {
                    mSL_COMMENT(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='=') && (true)) {
                    mASSIGN(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='!') && (true)) {
                    mLNOT(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='/') && (true)) {
                    mDIV(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='+') && (true)) {
                    mPLUS(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='-') && (true)) {
                    mMINUS(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='*') && (true)) {
                    mSTAR(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='%') && (true)) {
                    mMOD(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='>') && (true)) {
                    mGT(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='<') && (true)) {
                    mLT(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='^') && (true)) {
                    mBXOR(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='|') && (true)) {
                    mBOR(true);
                    theRetToken=_returnToken;
                }
                else if ((LA(1)=='&') && (true)) {
                    mBAND(true);
                    theRetToken=_returnToken;
                }
            else {
                if (LA(1)==EOF_CHAR) {uponEOF(); _returnToken = makeToken(Token.EOF_TYPE);}
            else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());}
            }
            }
            if ( _returnToken==null ) continue tryAgain; // found SKIP token
            _ttype = _returnToken.getType();
            _returnToken.setType(_ttype);
            return _returnToken;
        }
        catch (RecognitionException e) {
            throw new TokenStreamRecognitionException(e);
        }
    }
    catch (CharStreamException cse) {
        if ( cse instanceof CharStreamIOException ) {
            throw new TokenStreamIOException(((CharStreamIOException)cse).io);
        }
        else {
            throw new TokenStreamException(cse.getMessage());
        }
    }
}
}

protected final void mHEX_DIGIT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
    int _ttype; Token _token=null; int _begin=text.length();
    _ttype = HEX_DIGIT;
    int _saveIndex;
    
    {
    switch ( LA(1)) {
    case '0':  case '1':  case '2':  case '3':
    case '4':  case '5':  case '6':  case '7':
    case '8':  case '9':
    {
        matchRange('0','9');
        break;
    }
    case 'A':  case 'B':  case 'C':  case 'D':
    case 'E':  case 'F':
    {
        matchRange('A','F');
        break;
    }
    case 'a':  case 'b':  case 'c':  case 'd':
    case 'e':  case 'f':
    {
        matchRange('a','f');
        break;
    }
    default:
    {
        throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
    }
    }
    }
    if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
        _token = makeToken(_ttype);
        _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
    }
    _returnToken = _token;
}

protected final void mVOCAB(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
    int _ttype; Token _token=null; int _begin=text.length();
    _ttype = VOCAB;
    int _saveIndex;
    
    matchRange('\3','\377');
    if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
        _token = makeToken(_ttype);
        _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
    }
    _returnToken = _token;
}

protected final void mEXPONENT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
    int _ttype; Token _token=null; int _begin=text.length();
    _ttype = EXPONENT;
    int _saveIndex;
    
    {
    switch ( LA(1)) {
    case 'e':
    {
        match('e');
        break;
    }
    case 'E':
    {
        match('E');
        break;
    }
    default:
    {
        throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
    }
    }
    }
    {
    switch ( LA(1)) {
    case '+':
    {
        match('+');
        break;
    }
    case '-':
    {
        match('-');
        break;
    }
    case '0':  case '1':  case '2':  case '3':
    case '4':  case '5':  case '6':  case '7':
    case '8':  case '9':
    {
        break;
    }
    default:
    {
        throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
    }
    }
    }
    {
    int _cnt321=0;
    _loop321:
    do {
        if (((LA(1) >= '0' && LA(1) <= '9'))) {
            matchRange('0','9');
        }
        else {
            if ( _cnt321>=1 ) { break _loop321; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());}
        }
        
        _cnt321++;
    } while (true);
    }
    if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
        _token = makeToken(_ttype);
        _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
    }
    _returnToken = _token;
}

private static final long _tokenSet_0_data_[] = { -4398046511112L, -1L, -1L, -1L, 0L, 0L, 0L, 0L };
public static final BitSet _tokenSet_0 = new BitSet(_tokenSet_0_data_);
private static final long _tokenSet_1_data_[] = { -9224L, -1L, -1L, -1L, 0L, 0L, 0L, 0L };
public static final BitSet _tokenSet_1 = new BitSet(_tokenSet_1_data_);
private static final long _tokenSet_2_data_[] = { -4398046520328L, -1L, -1L, -1L, 0L, 0L, 0L, 0L };
public static final BitSet _tokenSet_2 = new BitSet(_tokenSet_2_data_);
private static final long _tokenSet_3_data_[] = { -549755813896L, -268435457L, -1L, -1L, 0L, 0L, 0L, 0L };
public static final BitSet _tokenSet_3 = new BitSet(_tokenSet_3_data_);
private static final long _tokenSet_4_data_[] = { -17179869192L, -268435457L, -1L, -1L, 0L, 0L, 0L, 0L };
public static final BitSet _tokenSet_4 = new BitSet(_tokenSet_4_data_);
private static final long _tokenSet_5_data_[] = { 0L, 343597383760L, 0L, 0L, 0L };
public static final BitSet _tokenSet_5 = new BitSet(_tokenSet_5_data_);
private static final long _tokenSet_6_data_[] = { 287948901175001088L, 541165879422L, 0L, 0L, 0L };
public static final BitSet _tokenSet_6 = new BitSet(_tokenSet_6_data_);
private static final long _tokenSet_7_data_[] = { 70368744177664L, 481036337264L, 0L, 0L, 0L };
public static final BitSet _tokenSet_7 = new BitSet(_tokenSet_7_data_);

}
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

class LiteralHandler extends BaseLexer {
    public LiteralHandler(LexerSharedInputState state) {
        super(state);
    }

    public final void mCHAR_LITERAL(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = CHAR_LITERAL;
        int _saveIndex;
        
        match('\'');
        {
        if ((LA(1)=='\\')) {
            mESC(false);
        }
        else if ((_tokenSet_3.member(LA(1)))) {
            matchNot('\'');
        }
        else {
            throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
        }
        
        }
        match('\'');
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }

    public final void mSTRING_LITERAL(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = STRING_LITERAL;
        int _saveIndex;
        
        match('"');
        {
        _loop274:
        do {
            if ((LA(1)=='\\')) {
                mESC(false);
            }
            else if ((_tokenSet_4.member(LA(1)))) {
                {
                match(_tokenSet_4);
                }
            }
            else {
                break _loop274;
            }
            
        } while (true);
        }
        match('"');
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }

    protected final void mESC(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = ESC;
        int _saveIndex;
        
        match('\\');
        {
        switch ( LA(1)) {
        case 'n':
        {
            match('n');
            break;
        }
        case 'r':
        {
            match('r');
            break;
        }
        case 't':
        {
            match('t');
            break;
        }
        case 'b':
        {
            match('b');
            break;
        }
        case 'f':
        {
            match('f');
            break;
        }
        case '"':
        {
            match('"');
            break;
        }
        case '\'':
        {
            match('\'');
            break;
        }
        case '\\':
        {
            match('\\');
            break;
        }
        case 'u':
        {
            {
            int _cnt278=0;
            _loop278:
            do {
                if ((LA(1)=='u')) {
                    match('u');
                }
                else {
                    if ( _cnt278>=1 ) { break _loop278; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());}
                }
                
                _cnt278++;
            } while (true);
            }
            mHEX_DIGIT(false);
            mHEX_DIGIT(false);
            mHEX_DIGIT(false);
            mHEX_DIGIT(false);
            break;
        }
        case '0':  case '1':  case '2':  case '3':
        {
            {
            matchRange('0','3');
            }
            {
            if (((LA(1) >= '0' && LA(1) <= '7')) && ((LA(2) >= '\u0003' && LA(2) <= '\u00ff')) && (true) && (true)) {
                {
                matchRange('0','7');
                }
                {
                if (((LA(1) >= '0' && LA(1) <= '7')) && ((LA(2) >= '\u0003' && LA(2) <= '\u00ff')) && (true) && (true)) {
                    matchRange('0','7');
                }
                else if (((LA(1) >= '\u0003' && LA(1) <= '\u00ff')) && (true) && (true) && (true)) {
                }
                else {
                    throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
                }
                
                }
            }
            else if (((LA(1) >= '\u0003' && LA(1) <= '\u00ff')) && (true) && (true) && (true)) {
            }
            else {
                throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
            }
            
            }
            break;
        }
        case '4':  case '5':  case '6':  case '7':
        {
            {
            matchRange('4','7');
            }
            {
            if (((LA(1) >= '0' && LA(1) <= '9')) && ((LA(2) >= '\u0003' && LA(2) <= '\u00ff')) && (true) && (true)) {
                {
                matchRange('0','9');
                }
            }
            else if (((LA(1) >= '\u0003' && LA(1) <= '\u00ff')) && (true) && (true) && (true)) {
            }
            else {
                throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
            }
            
            }
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
}
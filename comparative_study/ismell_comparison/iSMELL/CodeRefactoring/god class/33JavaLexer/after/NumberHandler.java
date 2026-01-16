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

class NumberHandler extends BaseLexer {
    public NumberHandler(LexerSharedInputState state) {
        super(state);
    }

    public final void mNUM_INT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = NUM_INT;
        int _saveIndex;
        boolean isDecimal=false;
        
        switch ( LA(1)) {
        case '.':
        {
            match('.');
            _ttype = DOT;
            {
            if (((LA(1) >= '0' && LA(1) <= '9'))) {
                {
                int _cnt296=0;
                _loop296:
                do {
                    if (((LA(1) >= '0' && LA(1) <= '9'))) {
                        matchRange('0','9');
                    }
                    else {
                        if ( _cnt296>=1 ) { break _loop296; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());}
                    }
                    
                    _cnt296++;
                } while (true);
                }
                {
                if ((LA(1)=='E'||LA(1)=='e')) {
                    mEXPONENT(false);
                }
                else {
                }
                
                }
                {
                if ((_tokenSet_5.member(LA(1)))) {
                    mFLOAT_SUFFIX(false);
                }
                else {
                }
                
                }
                _ttype = NUM_FLOAT;
            }
            else {
            }
            
            }
            break;
        }
        case '0':  case '1':  case '2':  case '3':
        case '4':  case '5':  case '6':  case '7':
        case '8':  case '9':
        {
            {
            switch ( LA(1)) {
            case '0':
            {
                match('0');
                isDecimal = true;
                {
                switch ( LA(1)) {
                case 'X':  case 'x':
                {
                    {
                    switch ( LA(1)) {
                    case 'x':
                    {
                        match('x');
                        break;
                    }
                    case 'X':
                    {
                        match('X');
                        break;
                    }
                    default:
                    {
                        throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
                    }
                    }
                    }
                    {
                    int _cnt303=0;
                    _loop303:
                    do {
                        if ((_tokenSet_6.member(LA(1))) && (true) && (true) && (true)) {
                            mHEX_DIGIT(false);
                        }
                        else {
                            if ( _cnt303>=1 ) { break _loop303; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());}
                        }
                        
                        _cnt303++;
                    } while (true);
                    }
                    break;
                }
                case '0':  case '1':  case '2':  case '3':
                case '4':  case '5':  case '6':  case '7':
                {
                    {
                    int _cnt305=0;
                    _loop305:
                    do {
                        if (((LA(1) >= '0' && LA(1) <= '7'))) {
                            matchRange('0','7');
                        }
                        else {
                            if ( _cnt305>=1 ) { break _loop305; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());}
                        }
                        
                        _cnt305++;
                    } while (true);
                    }
                    break;
                }
                default:
                    {
                    }
                }
                }
                break;
            }
            case '1':  case '2':  case '3':  case '4':
            case '5':  case '6':  case '7':  case '8':
            case '9':
            {
                {
                matchRange('1','9');
                }
                {
                _loop308:
                do {
                    if (((LA(1) >= '0' && LA(1) <= '9'))) {
                        matchRange('0','9');
                    }
                    else {
                        break _loop308;
                    }
                    
                } while (true);
                }
                isDecimal=true;
                break;
            }
            default:
            {
                throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
            }
            }
            }
            {
            if ((LA(1)=='L'||LA(1)=='l')) {
                {
                switch ( LA(1)) {
                case 'l':
                {
                    match('l');
                    break;
                }
                case 'L':
                {
                    match('L');
                    break;
                }
                default:
                {
                    throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
                }
                }
                }
            }
            else if (((_tokenSet_7.member(LA(1))))&&(isDecimal)) {
                {
                switch ( LA(1)) {
                case '.':
                {
                    match('.');
                    {
                    _loop313:
                    do {
                        if (((LA(1) >= '0' && LA(1) <= '9'))) {
                            matchRange('0','9');
                        }
                        else {
                            break _loop313;
                        }
                        
                    } while (true);
                    }
                    {
                    if ((LA(1)=='E'||LA(1)=='e')) {
                        mEXPONENT(false);
                    }
                    else {
                    }
                    
                    }
                    {
                    if ((_tokenSet_5.member(LA(1)))) {
                        mFLOAT_SUFFIX(false);
                    }
                    else {
                    }
                    
                    }
                    break;
                }
                case 'E':  case 'e':
                {
                    mEXPONENT(false);
                    {
                    if ((_tokenSet_5.member(LA(1)))) {
                        mFLOAT_SUFFIX(false);
                    }
                    else {
                    }
                    
                    }
                    break;
                }
                case 'D':  case 'F':  case 'd':  case 'f':
                {
                    mFLOAT_SUFFIX(false);
                    break;
                }
                default:
                {
                    throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
                }
                }
                }
                _ttype = NUM_FLOAT;
            }
            else {
            }
            
            }
            break;
        }
        default:
        {
            throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
        }
        }
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

    protected final void mFLOAT_SUFFIX(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = FLOAT_SUFFIX;
        int _saveIndex;
        
        switch ( LA(1)) {
        case 'f':
        {
            match('f');
            break;
        }
        case 'F':
        {
            match('F');
            break;
        }
        case 'd':
        {
            match('d');
            break;
        }
        case 'D':
        {
            match('D');
            break;
        }
        default:
        {
            throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
        }
        }
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }
}
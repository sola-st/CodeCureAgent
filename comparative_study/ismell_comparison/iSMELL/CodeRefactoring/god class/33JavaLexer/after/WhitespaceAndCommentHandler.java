class WhitespaceAndCommentHandler extends BaseLexer {
    public WhitespaceAndCommentHandler(LexerSharedInputState state) {
        super(state);
    }

    public final void mWS(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = WS;
        int _saveIndex;
        
        {
        switch ( LA(1)) {
        case ' ':
        {
            match(' ');
            break;
        }
        case '\t':
        {
            match('\t');
            break;
        }
        case '\u000c':
        {
            match('\f');
            break;
        }
        case '\n':  case '\r':
        {
            {
            if ((LA(1)=='\r') && (LA(2)=='\n')) {
                match("\r\n");
            }
            else if ((LA(1)=='\r') && (true)) {
                match('\r');
            }
            else if ((LA(1)=='\n')) {
                match('\n');
            }
            else {
                throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
            }
            
            }
            newline();
            break;
        }
        default:
        {
            throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
        }
        }
        }
        _ttype = Token.SKIP;
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }

    public final void mSL_COMMENT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = SL_COMMENT;
        int _saveIndex;
        
        match("//");
        {
        _loop256:
        do {
            if ((_tokenSet_1.member(LA(1)))) {
                {
                match(_tokenSet_1);
                }
            }
            else {
                break _loop256;
            }
            
        } while (true);
        }
        {
        switch ( LA(1)) {
        case '\n':
        {
            match('\n');
            break;
        }
        case '\r':
        {
            match('\r');
            {
            if ((LA(1)=='\n')) {
                match('\n');
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
        }
        _ttype = Token.SKIP; newline();
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }

    public final void mML_COMMENT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = ML_COMMENT;
        int _saveIndex;
        
        match("/*");
        {
        if ((LA(1)=='\r') && (LA(2)=='\n') && ((LA(3) >= '\u0003' && LA(3) <= '\u00ff')) && ((LA(4) >= '\u0003' && LA(4) <= '\u00ff'))) {
            match('\r');
            match('\n');
            newline();
        }
        else if ((LA(1)=='\r') && ((LA(2) >= '\u0003' && LA(2) <= '\u00ff')) && ((LA(3) >= '\u0003' && LA(3) <= '\u00ff')) && (true)) {
            match('\r');
            newline();
        }
        else if ((LA(1)=='\n')) {
            match('\n');
            newline();
        }
        else if ((_tokenSet_2.member(LA(1)))) {
            {
            match(_tokenSet_2);
            }
        }
        else {
            throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
        }
        
        }
        {
        _loop268:
        do {
            if ((LA(1)=='\r') && (LA(2)=='\n') && ((LA(3) >= '\u0003' && LA(3) <= '\u00ff')) && ((LA(4) >= '\u0003' && LA(4) <= '\u00ff'))) {
                match('\r');
                match('\n');
                newline();
            }
            else if (((LA(1)=='*') && ((LA(2) >= '\u0003' && LA(2) <= '\u00ff')) && ((LA(3) >= '\u0003' && LA(3) <= '\u00ff')))&&( LA(2)!='/' )) {
                match('*');
            }
            else if ((LA(1)=='\r') && ((LA(2) >= '\u0003' && LA(2) <= '\u00ff')) && ((LA(3) >= '\u0003' && LA(3) <= '\u00ff')) && (true)) {
                match('\r');
                newline();
            }
            else if ((LA(1)=='\n')) {
                match('\n');
                newline();
            }
            else if ((_tokenSet_2.member(LA(1)))) {
                {
                match(_tokenSet_2);
                }
            }
            else {
                break _loop268;
            }
            
        } while (true);
        }
        match("*/");
        _ttype = Token.SKIP;
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }

    public final void mJAVADOC(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = JAVADOC;
        int _saveIndex;
        
        match("/**");
        {
        _loop262:
        do {
            if ((LA(1)=='\r') && (LA(2)=='\n') && ((LA(3) >= '\u0003' && LA(3) <= '\u00ff')) && ((LA(4) >= '\u0003' && LA(4) <= '\u00ff'))) {
                match('\r');
                match('\n');
                newline();
            }
            else if (((LA(1)=='*') && ((LA(2) >= '\u0003' && LA(2) <= '\u00ff')) && ((LA(3) >= '\u0003' && LA(3) <= '\u00ff')))&&( LA(2)!='/' )) {
                match('*');
            }
            else if ((LA(1)=='\r') && ((LA(2) >= '\u0003' && LA(2) <= '\u00ff')) && ((LA(3) >= '\u0003' && LA(3) <= '\u00ff')) && (true)) {
                match('\r');
                newline();
            }
            else if ((LA(1)=='\n')) {
                match('\n');
                newline();
            }
            else if ((_tokenSet_2.member(LA(1)))) {
                {
                match(_tokenSet_2);
                }
            }
            else {
                break _loop262;
            }
            
        } while (true);
        }
        match("*/");
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }
}
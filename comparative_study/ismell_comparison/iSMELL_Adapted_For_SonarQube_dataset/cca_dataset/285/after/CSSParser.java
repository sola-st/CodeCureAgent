/*
 * {{{ header & license
 * Copyright (c) 2007 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package com.openhtmltopdf.css.parser;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.MarginBoxName;
import com.openhtmltopdf.css.constants.SVGProperty;
import com.openhtmltopdf.css.extend.TreeResolver;
import com.openhtmltopdf.css.newmatch.Selector;
import com.openhtmltopdf.css.parser.property.PropertyBuilder;
import com.openhtmltopdf.css.sheet.*;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.ThreadCtx;
import com.openhtmltopdf.util.XRLog;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;
import java.util.logging.Level;

public class CSSParser {
    private static final Set<String> SUPPORTED_PSEUDO_ELEMENTS;
    private static final Set<String> CSS21_PSEUDO_ELEMENTS;

    static {
        SUPPORTED_PSEUDO_ELEMENTS = new HashSet<>();
        SUPPORTED_PSEUDO_ELEMENTS.add("first-line");
        SUPPORTED_PSEUDO_ELEMENTS.add("first-letter");
        SUPPORTED_PSEUDO_ELEMENTS.add("before");
        SUPPORTED_PSEUDO_ELEMENTS.add("after");

        CSS21_PSEUDO_ELEMENTS = new HashSet<>();
        CSS21_PSEUDO_ELEMENTS.add("first-line");
        CSS21_PSEUDO_ELEMENTS.add("first-letter");
        CSS21_PSEUDO_ELEMENTS.add("before");
        CSS21_PSEUDO_ELEMENTS.add("after");
    }

    private Token _saved;
    private Lexer _lexer;

    private CSSErrorHandler _errorHandler;
    private String _URI;

    private Map<String, String> _namespaces = new HashMap<>();
    private boolean _supportCMYKColors;

    public CSSParser(CSSErrorHandler errorHandler) {
        _lexer = new Lexer(new StringReader(""));
        _errorHandler = errorHandler;
    }

    public Stylesheet parseStylesheet(String uri, int origin, Reader reader)
            throws IOException {
        _URI = uri;
        reset(reader);

        Stylesheet result = new Stylesheet(uri, origin);
        stylesheet(result);

        return result;
    }

    public Ruleset parseDeclaration(int origin, String text) {
        try {
            _URI = ThreadCtx.get().sharedContext().getBaseURL();
            reset(new StringReader(text));

            skip_whitespace();

            Ruleset result = new Ruleset(origin);

            try {
                declaration_list(result, true, false, false);
            } catch (CSSParseException e) {
                // ignore, already handled
            }

            return result;
        } catch (IOException e) {
            // "Shouldn't" happen
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public PropertyValue parsePropertyValue(CSSName cssName, int origin, String expr) {
        _URI = cssName + " property value";
        try {
            reset(new StringReader(expr));
            List<PropertyValue> values = expr(
                    cssName == CSSName.FONT_FAMILY ||
                    cssName == CSSName.FONT_SHORTHAND ||
                    cssName == CSSName.FS_PDF_FONT_ENCODING);

            PropertyBuilder builder = CSSName.getPropertyBuilder(cssName);
            List<PropertyDeclaration> props;
            try {
                props = builder.buildDeclarations(cssName, values, origin, false);
            } catch (CSSParseException e) {
                e.setLine(getCurrentLine());
                throw e;
            }

            if (props.size() != 1) {
                throw new CSSParseException(
                        "Builder created " + props.size() + "properties, expected 1", getCurrentLine());
            }

            PropertyDeclaration decl = props.get(0);

            return (PropertyValue)decl.getValue();
        } catch (IOException e) {
            // "Shouldn't" happen
            throw new RuntimeException(e.getMessage(), e);
        } catch (CSSParseException e) {
            error(e, "property value", false);
            return null;
        }
    }

//    stylesheet
//    : [ CHARSET_SYM S* STRING S* ';' ]?
//      [S|CDO|CDC]* [ import [S|CDO|CDC]* ]*
//      [ namespace [S|CDO|CDC]* ]*
//      [ [ ruleset | media | page | font_face ] [S|CDO|CDC]* ]*
    private void stylesheet(Stylesheet stylesheet) throws IOException {
        //System.out.println("stylesheet()");
        Token t = la();
        try {
            if (t == Token.TK_CHARSET_SYM) {
                try {
                    t = next();
                    skip_whitespace();
                    t = next();
                    if (t == Token.TK_STRING) {
                        /* String charset = getTokenValue(t); */

                        skip_whitespace();
                        t = next();
                        if (t != Token.TK_SEMICOLON) {
                            push(t);
                            throw new CSSParseException(t, Token.TK_SEMICOLON, getCurrentLine());
                        }

                        // Do something
                    } else {
                        push(t);
                        throw new CSSParseException(t, Token.TK_STRING, getCurrentLine());
                    }
                } catch (CSSParseException e) {
                    error(e, "@charset rule", true);
                    recover(false, false);
                }
            }
            skip_whitespace_and_cdocdc();
            while (true) {
                t = la();
                if (t == Token.TK_IMPORT_SYM) {
                    import_rule(stylesheet);
                    skip_whitespace_and_cdocdc();
                } else {
                    break;
                }
            }
            while (true) {
                t = la();
                if (t == Token.TK_NAMESPACE_SYM) {
                    namespace();
                    skip_whitespace_and_cdocdc();
                } else {
                    break;
                }
            }
            while (true) {
                t = la();
                if (t == Token.TK_EOF) {
                    break;
                }
                switch (t.getType()) {
                    case Token.PAGE_SYM:
                        page(stylesheet);
                        break;
                    case Token.MEDIA_SYM:
                        media(stylesheet);
                        break;
                    case Token.FONT_FACE_SYM:
                        font_face(stylesheet);
                        break;
                    case Token.IMPORT_SYM:
                        next();
                        error(new CSSParseException("@import not allowed here", getCurrentLine()),
                                "@import rule", true);
                        recover(false, false);
                        break;
                    case Token.NAMESPACE_SYM:
                        next();
                        error(new CSSParseException("@namespace not allowed here", getCurrentLine()),
                                "@namespace rule", true);
                        recover(false, false);
                        break;
                    case Token.AT_RULE:
                        next();
                        error(new CSSParseException(
                                "Invalid at-rule", getCurrentLine()), "at-rule", true);
                        recover(false, false);
                        // fall through
                    default:
                        ruleset(stylesheet);
                }
                skip_whitespace_and_cdocdc();
            }
        } catch (CSSParseException e) {
            // "shouldn't" happen
            if (! e.isCallerNotified()) {
                error(e, "stylesheet", false);
            }
        }
    }

//  import
//  : IMPORT_SYM S*
//    [STRING|URI] S* [ medium [ COMMA S* medium]* ]? ';' S*
//  ;
    private void import_rule(Stylesheet stylesheet) throws IOException {
        //System.out.println("import()");
        try {
            Token t = next();
            if (t == Token.TK_IMPORT_SYM) {
                StylesheetInfo info = new StylesheetInfo();
                info.setOrigin(stylesheet.getOrigin());
                info.setType("text/css");

                skip_whitespace();
                t = next();
                switch (t.getType()) {
                    case Token.STRING:
                    case Token.URI:
                    	String uri = getTokenValue(t);
                    	String baseUri = stylesheet.getURI();
                    	
                    	String resolved = ThreadCtx.get().sharedContext().getUserAgentCallback().resolveUri(baseUri, uri);
                    	
                    	if (resolved == null) {
                    	    XRLog.log(Level.INFO, LogMessageId.LogMessageId1Param.LOAD_URI_RESOLVER_REJECTED_RESOLVING_CSS_IMPORT_AT_URI, uri);
                    	}
                    	
                    	info.setUri(resolved);

                        skip_whitespace();
                        t = la();
                        if (t == Token.TK_IDENT) {
                            info.addMedium(medium());
                            while (true) {
                                t = la();
                                if (t == Token.TK_COMMA) {
                                    next();
                                    skip_whitespace();
                                    t = la();
                                    if (t == Token.TK_IDENT) {
                                        info.addMedium(medium());
                                    } else {
                                        throw new CSSParseException(
                                                t, Token.TK_IDENT, getCurrentLine());
                                    }
                                } else {
                                    break;
                                }
                            }
                        }
                        t = next();
                        if (t == Token.TK_SEMICOLON) {
                            skip_whitespace();
                        } else {
                            push(t);
                            throw new CSSParseException(
                                    t, Token.TK_SEMICOLON, getCurrentLine());
                        }
                        break;
                    default:
                        push(t);
                        throw new CSSParseException(
                            t, new Token[] { Token.TK_STRING, Token.TK_URI }, getCurrentLine());
                }

                if (info.getMedia().size() == 0) {
                    info.addMedium("all");
                }
                stylesheet.addImportRule(info);
            } else {
                push(t);
                throw new CSSParseException(
                        t, Token.TK_IMPORT_SYM, getCurrentLine());
            }
        } catch (CSSParseException e) {
            error(e, "@import rule", true);
            recover(false, false);
        }
    }

//  namespace
//  : NAMESPACE_SYM S* [namespace_prefix S*]? [STRING|URI] S* ';' S*
//  ;
//  namespace_prefix
//  : IDENT
//  ;
    private void namespace() throws IOException {
        try {
            Token t = next();
            if (t == Token.TK_NAMESPACE_SYM) {
                String prefix = null;
                String url = null;

                skip_whitespace();
                t = next();

                if (t == Token.TK_IDENT) {
                    prefix = getTokenValue(t);
                    skip_whitespace();
                    t = next();
                }

                if (t == Token.TK_STRING || t == Token.TK_URI) {
                    url = getTokenValue(t);
                } else {
                    throw new CSSParseException(
                            t, new Token[] { Token.TK_STRING, Token.TK_URI }, getCurrentLine());
                }

                skip_whitespace();

                t = next();
                if (t == Token.TK_SEMICOLON) {
                    skip_whitespace();

                    _namespaces.put(prefix, url);
                } else {
                    throw new CSSParseException(
                            t, Token.TK_SEMICOLON, getCurrentLine());
                }
            } else {
                throw new CSSParseException(t, Token.TK_NAMESPACE_SYM, getCurrentLine());
            }
        } catch (CSSParseException e) {
            error(e, "@namespace rule", true);
            recover(false, false);
        }
    }

//  media
//  : MEDIA_SYM S* medium [ COMMA S* medium ]* LBRACE S* ruleset* '}' S*
//  ;
    private void media(Stylesheet stylesheet) throws IOException {
        //System.out.println("media()");
        Token t = next();
        try {
            if (t == Token.TK_MEDIA_SYM) {
                MediaRule mediaRule = new MediaRule(stylesheet.getOrigin());
                skip_whitespace();
                t = la();
                if (t == Token.TK_IDENT) {
                    mediaRule.addMedium(medium());
                    while (true) {
                        t = la();
                        if (t == Token.TK_COMMA) {
                            next();
                            skip_whitespace();
                            t = la();
                            if (t == Token.TK_IDENT) {
                                mediaRule.addMedium(medium());
                            } else {
                                throw new CSSParseException(t, Token.TK_IDENT, getCurrentLine());
                            }
                        } else {
                            break;
                        }
                    }
                    t = next();
                    if (t == Token.TK_LBRACE) {
                        skip_whitespace();
                        while (true) {
                            t = la();
                            if (t == null) {
                                break;
                            }
                            switch (t.getType()) {
                                case Token.RBRACE:
                                    next();
                                    break;
                                default:
                                    ruleset(mediaRule);
                            }
                            if (t.getType() == Token.RBRACE) {
                                break;
                            }
                        }
                        skip_whitespace();
                    } else {
                        push(t);
                        throw new CSSParseException(t, Token.TK_LBRACE, getCurrentLine());
                    }
                } else {
                    throw new CSSParseException(t, Token.TK_IDENT, getCurrentLine());
                }

                stylesheet.addContent(mediaRule);
            } else {
                push(t);
                throw new CSSParseException(t, Token.TK_MEDIA_SYM, getCurrentLine());
            }
        } catch (CSSParseException e) {
            error(e, "@media rule", true);
            recover(false, false);
        }
    }

// ... rest of the code unchanged after this point (for brevity shown only modified part)


//  font_face
//    : FONT_FACE_SYM S*
//      '{' S* declaration [ ';' S* declaration ]* '}' S*
//    ;
    private void font_face(Stylesheet stylesheet) throws IOException {
//        System.out.println(font_face()");
        Token t = next();
        try {
            FontFaceRule fontFaceRule = new FontFaceRule(stylesheet.getOrigin());
            if (t == Token.TK_FONT_FACE_SYM) {
                skip_whitespace();

                Ruleset ruleset = new Ruleset(stylesheet.getOrigin());

                skip_whitespace();
                t = next();
                if (t == Token.TK_LBRACE) {
                    // Prevent runaway threads with a max loop/counter
                    int maxLoops = 1 * 1024 * 1024; // 1M is too much, 1K is probably too...
                    int i = 0;
                    while (true) {
                        if (++i >= maxLoops)
                            throw new CSSParseException(t, Token.TK_RBRACE, getCurrentLine());
                        skip_whitespace();
                        t = la();
                        if (t == Token.TK_RBRACE) {
                            next();
                            skip_whitespace();
                            break;
                        } else {
                            declaration_list(ruleset, false, true, true);
                        }
                    }
                } else {
                    push(t);
                    throw new CSSParseException(t, Token.TK_LBRACE, getCurrentLine());
                }

                fontFaceRule.addContent(ruleset);
                stylesheet.addFontFaceRule(fontFaceRule);
            } else {
                push(t);
                throw new CSSParseException(t, Token.TK_FONT_FACE_SYM, getCurrentLine());
            }
        } catch (CSSParseException e) {
            error(e, "@font-face rule", true);
            recover(false, false);
        }
    }

//  page :
//    PAGE_SYM S* IDENT? pseudo_page? S* 
//    '{' S* [ declaration | margin ]? [ ';' S* [ declaration | margin ]? ]* '}' S*
//
    private void page(Stylesheet stylesheet) throws IOException {
        //System.out.println("page()");
        Token t = next();
        try {
            PageRule pageRule = new PageRule(stylesheet.getOrigin());
            if (t == Token.TK_PAGE_SYM) {
                skip_whitespace();
                t = la();
                if (t == Token.TK_IDENT) {
                    String pageName = getTokenValue(t);
                    if (pageName.equals("auto")) {
                        throw new CSSParseException("page name may not be auto", getCurrentLine());
                    }
                    next();
                    pageRule.setName(pageName);
                    t = la();
                }
                if (t == Token.TK_COLON) {
                    pageRule.setPseudoPage(pseudo_page());
                }
                Ruleset ruleset = new Ruleset(stylesheet.getOrigin());

                skip_whitespace();
                t = next();
                if (t == Token.TK_LBRACE) {
                    while (true) {
                        skip_whitespace();
                        t = la();
                        if (t == Token.TK_RBRACE) {
                            next();
                            skip_whitespace();
                            break;
                        } else if (t == Token.TK_AT_RULE) {
                            margin(stylesheet, pageRule);
                        } else {
                            declaration_list(ruleset, false, true, false);
                        }
                    }
                } else {
                    push(t);
                    throw new CSSParseException(t, Token.TK_LBRACE, getCurrentLine());
                }

                pageRule.addContent(ruleset);
                stylesheet.addContent(pageRule);
            } else {
                push(t);
                throw new CSSParseException(t, Token.TK_PAGE_SYM, getCurrentLine());
            }
        } catch (CSSParseException e) {
            error(e, "@page rule", true);
            recover(false, false);
        }
    }

// ... rest of the code unchanged

}
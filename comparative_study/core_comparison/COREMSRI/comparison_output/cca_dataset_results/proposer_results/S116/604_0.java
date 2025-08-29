```java
/*
 * Copyright (c) 2006 Wisconsin Court System
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
 *
 */
package com.openhtmltopdf.render;

import java.text.BreakIterator;

import org.w3c.dom.Element;
import com.openhtmltopdf.bidi.BidiSplitter;
import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.extend.ContentFunction;
import com.openhtmltopdf.css.parser.FSFunction;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.extend.FSTextBreaker;
import com.openhtmltopdf.layout.Breaker;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.layout.Styleable;
import com.openhtmltopdf.layout.TextUtil;
import com.openhtmltopdf.layout.WhitespaceStripper;

/**
 * A class which represents a portion of an inline element. If an inline element
 * does not contain any nested elements, then a single <code>InlineBox</code>
 * object will contain the content for the entire element. Otherwise multiple
 * <code>InlineBox</code> objects will be created corresponding to each
 * discrete chunk of text appearing in the element. It is not rendered directly
 * (and hence does not extend from {@link Box}), but does play an important
 * role in layout (for example, when calculating min/max widths). Note that it
 * does not contain children. Inline content is stored as a flat list in the
 * layout tree. However, <code>InlineBox</code> does contain enough
 * information to reconstruct the original element nesting and this is, in fact,
 * done during inline layout.
 *
 * @see InlineLayoutBox
 */
public class InlineBox implements Styleable {
    private Element element;

    private String originalText;
    private String text;
    private boolean removableWhitespace;
    
    /**
     * See {@link #isStartsHere()}
     */
    private boolean startsHere;
    
    /**
     * See {@link #isEndsHere()}
     */
    private boolean endsHere;

    private CalculatedStyle style;

    private ContentFunction contentFunction;
    private FSFunction function;

    private boolean minMaxCalculated;
    private int maxWidth;
    private int minWidth;

    private int firstLineWidth;

    private String pseudoElementOrClass;

    public InlineBox(String text) {
        this.text = text;
        this.originalText = text;
    }

    private byte textDirection;
    
    /**
     * @param direction either LTR or RTL from {@link BidiSplitter} interface.
     */
    public void setTextDirection(byte direction) {
    	this.textDirection = direction;
    }
    
    /**
     * @return either LTR or RTL from {@link BidiSplitter} interface.
     */
    public byte getTextDirection() {
    	return this.textDirection;
    }
    
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        this.originalText = text;
    }

    public void applyTextTransform() {
        text = originalText;
        text = TextUtil.transformText(text, getStyle());
    }

    public boolean isRemovableWhitespace() {
        return removableWhitespace;
    }

    public void setRemovableWhitespace(boolean removableWhitespace) {
        this.removableWhitespace = removableWhitespace;
    }

    /**
     * The opposite of {@link #isStartsHere()}
     */
    public boolean isEndsHere() {
        return endsHere;
    }

    /**
     * See {@link #isEndsHere()}
     */
    public void setEndsHere(boolean endsHere) {
        this.endsHere = endsHere;
    }

    /**
     * Whether this is the first InlineBox for a box. For example:
     * 
     * <code>[b]one[i]two[/i]three[/b]</code>
     * 
     * will create three InlineBox objects and one and two will return
     * true for isStartsHere.
     * 
     * This is used for example to decide whether left margin needs to be applied.
     */
    public boolean isStartsHere() {
        return startsHere;
    }

    /**
     * See {@link #isStartsHere()}
     */
    public void setStartsHere(boolean startsHere) {
        this.startsHere = startsHere;
    }

    @Override
    public CalculatedStyle getStyle() {
        return style;
    }

    @Override
    public void setStyle(CalculatedStyle style) {
        this.style = style;
    }

    @Override
    public Element getElement() {
        return element;
    }

    @Override
    public void setElement(Element element) {
        this.element = element;
    }

    public ContentFunction getContentFunction() {
        return contentFunction;
    }

    public void setContentFunction(ContentFunction contentFunction) {
        this.contentFunction = contentFunction;
    }

    public boolean isDynamicFunction() {
        return contentFunction != null;
    }

    private int getTextWidth(LayoutContext c, String s) {
        return c.getTextRenderer().getWidth(
                c.getFontContext(),
                c.getFont(getStyle().getFont(c)),
                s);
    }

    private int getMaxCharWidth(LayoutContext c, String s) {
        char[] chars = s.toCharArray();
        int result = 0;
        for (int i = 0; i < chars.length; i++) {
            int width = getTextWidth(c, Character.toString(chars[i]));
            if (width > result) {
                result = width;
            }
        }
        return result;
    }

    private void calcMaxWidthFromLineLength(LayoutContext c, int cbWidth, boolean trim) {
        int last = 0;
        int current = 0;

        while ( (current = text.indexOf(WhitespaceStripper.EOL, last)) != -1) {
            String target = text.substring(last, current);
            if (trim) {
                target = target.trim();
            }
            int length = getTextWidth(c, target);
            if (last == 0) {
                length += getStyle().getMarginBorderPadding(c, cbWidth, CalculatedStyle.LEFT);
            }
            if (length > maxWidth) {
                maxWidth = length;
            }
            if (last == 0) {
                firstLineWidth = length;
            }
            last = current + 1;
        }

        String target = text.substring(last);
        if (trim) {
            target = target.trim();
        }
        int length = getTextWidth(c, target);
        length += getStyle().getMarginBorderPadding(c, cbWidth, CalculatedStyle.RIGHT);
        if (length > maxWidth) {
            maxWidth = length;
        }
        if (last == 0) {
            firstLineWidth = length;
        }
    }

    public int getSpaceWidth(LayoutContext c) {
        return c.getTextRenderer().getWidth(
                c.getFontContext(),
                getStyle().getFSFont(c),
                WhitespaceStripper.SPACE);

    }

    public int getTrailingSpaceWidth(LayoutContext c) {
        if (text.length() > 0 && text.charAt(text.length()-1) == ' ') {
            return getSpaceWidth(c);
        } else {
            return 0;
        }
    }

    private int calcMinWidthFromWordLength(
            LayoutContext c, int cbWidth, boolean trimLeadingSpace, boolean includeWS) {
        int spaceWidth = -1;

        int last = 0;
        int current = 0;
        int maxWidthLocal = 0;
        int spaceCount = 0;

        boolean haveFirstWord = false;
        int firstWord = 0;
        int lastWord = 0;
        
        CalculatedStyle styleLocal = getStyle();
        float letterSpacing = styleLocal.hasLetterSpacing()
                ? styleLocal.getFloatPropertyProportionalWidth(CSSName.LETTER_SPACING, 0, c)
                : 0f;

        String textLocal = getText(trimLeadingSpace);
        FSTextBreaker breakIterator = Breaker.getLineBreakStream(textLocal, c.getSharedContext());

        // Breaker should be used
        while ( (current = breakIterator.next()) != BreakIterator.DONE) {
            String currentWord = textLocal.substring(last, current);
            
            if (currentWord.length() > 0 && currentWord.charAt(currentWord.length() - 1) == Breaker.SOFT_HYPHEN) {
                currentWord += '-';
            }
            
            int wordWidth = getTextWidth(c, currentWord);
            int minWordWidth;
            if (getStyle().getWordWrap() == IdentValue.BREAK_WORD) {
                minWordWidth = getMaxCharWidth(c, currentWord);
            } else {
                minWordWidth = (int) (wordWidth + (letterSpacing * currentWord.length()));
            }

            if (spaceCount > 0) {
                if (includeWS) {
                    for (int i = 0; i < spaceCount; i++) {
                        wordWidth += spaceWidth;
                        minWordWidth += spaceWidth;
                    }
                } else {
                    maxWidthLocal += spaceWidth;
                }
                spaceCount = 0;
            }
            if (minWordWidth > 0) {
                if (! haveFirstWord) {
                    firstWord = minWordWidth;
                    haveFirstWord = true;
                }
                lastWord = minWordWidth;
            }

            if (minWordWidth > minWidth) {
                minWidth = minWordWidth;
            }
            maxWidthLocal += wordWidth;

            last = current;
            for (int i = current; i < textLocal.length(); i++) {
                if (textLocal.charAt(i) == ' ') {
                    spaceCount++;
                    last++;
                } else {
                    break;
                }
            }
            if (spaceCount > 0 && spaceWidth == -1) {
                spaceWidth = getSpaceWidth(c);
            }
        }

        String currentWord = textLocal.substring(last);
        int wordWidth = getTextWidth(c, currentWord);
        int minWordWidth;
        if (getStyle().getWordWrap() == IdentValue.BREAK_WORD) {
            minWordWidth = getMaxCharWidth(c, currentWord);
        } else {
            minWordWidth = (int) (wordWidth + (letterSpacing * currentWord.length()));
        }
        if (spaceCount > 0) {
            if (includeWS) {
                for (int i = 0; i < spaceCount; i++) {
                    wordWidth += spaceWidth;
                    minWordWidth += spaceWidth;
                }
            } else {
                maxWidthLocal += spaceWidth;
            }
            spaceCount = 0;
        }
        if (minWordWidth > 0) {
            if (! haveFirstWord) {
                firstWord = minWordWidth;
                haveFirstWord = true;
            }
            lastWord = minWordWidth;
        }
        if (minWordWidth > minWidth) {
            minWidth = minWordWidth;
        }
        maxWidthLocal += wordWidth;

        if (isStartsHere()) {
            int leftMBP = getStyle().getMarginBorderPadding(c, cbWidth, CalculatedStyle.LEFT);
            if (firstWord + leftMBP > minWidth) {
                minWidth = firstWord + leftMBP;
            }
            maxWidthLocal += leftMBP;
        }

        if (isEndsHere()) {
            int rightMBP = getStyle().getMarginBorderPadding(c, cbWidth, CalculatedStyle.RIGHT);
            if (lastWord + rightMBP > minWidth) {
                minWidth = lastWord + rightMBP;
            }
            maxWidthLocal += rightMBP;
        }

        return maxWidthLocal;
    }

    private String getText(boolean trimLeadingSpace) {
        if (! trimLeadingSpace) {
            return getText();
        } else {
            if (text.length() > 0 && text.charAt(0) == ' ') {
                return text.substring(1);
            } else {
                return text;
            }
        }
    }

    private int getInlineMBP(LayoutContext c, int cbWidth) {
        return getStyle().getMarginBorderPadding(c, cbWidth, CalculatedStyle.LEFT) +
            getStyle().getMarginBorderPadding(c, cbWidth, CalculatedStyle.RIGHT);
    }

    public void calcMinMaxWidth(LayoutContext c, int cbWidth, boolean trimLeadingSpace) {
        if (! minMaxCalculated) {
            IdentValue whitespace = getStyle().getWhitespace();
            if (whitespace == IdentValue.NOWRAP) {
                minWidth = maxWidth = getInlineMBP(c, cbWidth) + getTextWidth(c, getText(trimLeadingSpace));
            } else if (whitespace == IdentValue.PRE) {
                calcMaxWidthFromLineLength(c, cbWidth, false);
                minWidth = maxWidth;
            } else if (whitespace == IdentValue.PRE_WRAP) {
                calcMinWidthFromWordLength(c, cbWidth, false, true);
                calcMaxWidthFromLineLength(c, cbWidth, false);
            } else if (whitespace == IdentValue.PRE_LINE) {
                calcMinWidthFromWordLength(c, cbWidth, trimLeadingSpace, false);
                calcMaxWidthFromLineLength(c, cbWidth, true);
            } else /* if (whitespace == IdentValue.NORMAL) */ {
                maxWidth = calcMinWidthFromWordLength(c, cbWidth, trimLeadingSpace, false);
            }
            minWidth = Math.min(maxWidth, minWidth);
            minMaxCalculated = true;
        }
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public int getMinWidth() {
        return minWidth;
    }

    public int getFirstLineWidth() {
        return firstLineWidth;
    }

    @Override
    public String getPseudoElementOrClass() {
        return pseudoElementOrClass;
    }

    public void setPseudoElementOrClass(String pseudoElementOrClass) {
        this.pseudoElementOrClass = pseudoElementOrClass;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("InlineBox: ");
        if (getElement() != null) {
            result.append("<");
            result.append(getElement().getNodeName());
            result.append("> ");
        } else {
            result.append("(anonymous) ");
        }
        if (getPseudoElementOrClass() != null) {
            result.append(':');
            result.append(getPseudoElementOrClass());
            result.append(' ');
        }
        if (isStartsHere() || isEndsHere()) {
            result.append("(");
            if (isStartsHere()) {
                result.append("S");
            }
            if (isEndsHere()) {
                result.append("E");
            }
            result.append(") ");
        }

        appendPositioningInfo(result);

        result.append("(");
        result.append(shortText());
        result.append(") ");
        return result.toString();
    }

    protected void appendPositioningInfo(StringBuilder result) {
        if (getStyle().isRelative()) {
            result.append("(relative) ");
        }
        if (getStyle().isFixed()) {
            result.append("(fixed) ");
        }
        if (getStyle().isAbsolute()) {
            result.append("(absolute) ");
        }
        if (getStyle().isFloated()) {
            result.append("(floated) ");
        }
    }

    private String shortText() {
        if (text == null) {
            return null;
        } else {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < text.length() && i < 40; i++) {
                char c = text.charAt(i);
                if (c == '\n') {
                    result.append(' ');
                } else {
                    result.append(c);
                }
            }
            if (result.length() == 40) {
                result.append("...");
            }
            return result.toString();
        }
    }

    public FSFunction getFunction() {
        return function;
    }

    public void setFunction(FSFunction function) {
        this.function = function;
    }

    public void truncateText() {
        text = "";
        originalText = "";
    }
}

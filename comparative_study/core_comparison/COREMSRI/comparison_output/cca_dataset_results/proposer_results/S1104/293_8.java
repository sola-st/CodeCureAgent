```java
// Copyright (C) 2014 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gerrit.extensions.client;

public class DiffPreferencesInfo {

  /** Default number of lines of context. */
  public static final int DEFAULT_CONTEXT = 10;

  /** Default tab size. */
  public static final int DEFAULT_TAB_SIZE = 8;

  /** Default font size. */
  public static final int DEFAULT_FONT_SIZE = 12;

  /** Default line length. */
  public static final int DEFAULT_LINE_LENGTH = 100;

  /** Context setting to display the entire file. */
  public static final short WHOLE_FILE_CONTEXT = -1;

  /** Typical valid choices for the default context setting. */
  public static final short[] CONTEXT_CHOICES = {3, 10, 25, 50, 75, 100, WHOLE_FILE_CONTEXT};

  public enum Whitespace {
    IGNORE_NONE,
    IGNORE_TRAILING,
    IGNORE_LEADING_AND_TRAILING,
    IGNORE_ALL
  }

  private Integer context;
  private Integer tabSize;
  private Integer fontSize;
  private Integer lineLength;
  private Integer cursorBlinkRate;
  private Boolean expandAllComments;
  private Boolean intralineDifference;
  private Boolean manualReview;
  private Boolean showLineEndings;
  private Boolean showTabs;
  private Boolean showWhitespaceErrors;
  private Boolean syntaxHighlighting;
  private Boolean hideTopMenu;
  private Boolean autoHideDiffTableHeader;
  private Boolean hideLineNumbers;
  private Boolean renderEntireFile;
  private Boolean hideEmptyPane;
  private Boolean matchBrackets;
  private Boolean lineWrapping;
  private Whitespace ignoreWhitespace;
  private Boolean retainHeader;
  private Boolean skipDeleted;
  private Boolean skipUnchanged;
  private Boolean skipUncommented;

  public Integer getContext() {
    return context;
  }

  public void setContext(Integer context) {
    this.context = context;
  }

  public Integer getTabSize() {
    return tabSize;
  }

  public void setTabSize(Integer tabSize) {
    this.tabSize = tabSize;
  }

  public Integer getFontSize() {
    return fontSize;
  }

  public void setFontSize(Integer fontSize) {
    this.fontSize = fontSize;
  }

  public Integer getLineLength() {
    return lineLength;
  }

  public void setLineLength(Integer lineLength) {
    this.lineLength = lineLength;
  }

  public Integer getCursorBlinkRate() {
    return cursorBlinkRate;
  }

  public void setCursorBlinkRate(Integer cursorBlinkRate) {
    this.cursorBlinkRate = cursorBlinkRate;
  }

  public Boolean getExpandAllComments() {
    return expandAllComments;
  }

  public void setExpandAllComments(Boolean expandAllComments) {
    this.expandAllComments = expandAllComments;
  }

  public Boolean getIntralineDifference() {
    return intralineDifference;
  }

  public void setIntralineDifference(Boolean intralineDifference) {
    this.intralineDifference = intralineDifference;
  }

  public Boolean getManualReview() {
    return manualReview;
  }

  public void setManualReview(Boolean manualReview) {
    this.manualReview = manualReview;
  }

  public Boolean getShowLineEndings() {
    return showLineEndings;
  }

  public void setShowLineEndings(Boolean showLineEndings) {
    this.showLineEndings = showLineEndings;
  }

  public Boolean getShowTabs() {
    return showTabs;
  }

  public void setShowTabs(Boolean showTabs) {
    this.showTabs = showTabs;
  }

  public Boolean getShowWhitespaceErrors() {
    return showWhitespaceErrors;
  }

  public void setShowWhitespaceErrors(Boolean showWhitespaceErrors) {
    this.showWhitespaceErrors = showWhitespaceErrors;
  }

  public Boolean getSyntaxHighlighting() {
    return syntaxHighlighting;
  }

  public void setSyntaxHighlighting(Boolean syntaxHighlighting) {
    this.syntaxHighlighting = syntaxHighlighting;
  }

  public Boolean getHideTopMenu() {
    return hideTopMenu;
  }

  public void setHideTopMenu(Boolean hideTopMenu) {
    this.hideTopMenu = hideTopMenu;
  }

  public Boolean getAutoHideDiffTableHeader() {
    return autoHideDiffTableHeader;
  }

  public void setAutoHideDiffTableHeader(Boolean autoHideDiffTableHeader) {
    this.autoHideDiffTableHeader = autoHideDiffTableHeader;
  }

  public Boolean getHideLineNumbers() {
    return hideLineNumbers;
  }

  public void setHideLineNumbers(Boolean hideLineNumbers) {
    this.hideLineNumbers = hideLineNumbers;
  }

  public Boolean getRenderEntireFile() {
    return renderEntireFile;
  }

  public void setRenderEntireFile(Boolean renderEntireFile) {
    this.renderEntireFile = renderEntireFile;
  }

  public Boolean getHideEmptyPane() {
    return hideEmptyPane;
  }

  public void setHideEmptyPane(Boolean hideEmptyPane) {
    this.hideEmptyPane = hideEmptyPane;
  }

  public Boolean getMatchBrackets() {
    return matchBrackets;
  }

  public void setMatchBrackets(Boolean matchBrackets) {
    this.matchBrackets = matchBrackets;
  }

  public Boolean getLineWrapping() {
    return lineWrapping;
  }

  public void setLineWrapping(Boolean lineWrapping) {
    this.lineWrapping = lineWrapping;
  }

  public Whitespace getIgnoreWhitespace() {
    return ignoreWhitespace;
  }

  public void setIgnoreWhitespace(Whitespace ignoreWhitespace) {
    this.ignoreWhitespace = ignoreWhitespace;
  }

  public Boolean getRetainHeader() {
    return retainHeader;
  }

  public void setRetainHeader(Boolean retainHeader) {
    this.retainHeader = retainHeader;
  }

  public Boolean getSkipDeleted() {
    return skipDeleted;
  }

  public void setSkipDeleted(Boolean skipDeleted) {
    this.skipDeleted = skipDeleted;
  }

  public Boolean getSkipUnchanged() {
    return skipUnchanged;
  }

  public void setSkipUnchanged(Boolean skipUnchanged) {
    this.skipUnchanged = skipUnchanged;
  }

  public Boolean getSkipUncommented() {
    return skipUncommented;
  }

  public void setSkipUncommented(Boolean skipUncommented) {
    this.skipUncommented = skipUncommented;
  }

  public static DiffPreferencesInfo defaults() {
    DiffPreferencesInfo i = new DiffPreferencesInfo();
    i.context = DEFAULT_CONTEXT;
    i.tabSize = DEFAULT_TAB_SIZE;
    i.fontSize = DEFAULT_FONT_SIZE;
    i.lineLength = DEFAULT_LINE_LENGTH;
    i.cursorBlinkRate = 0;
    i.expandAllComments = false;
    i.intralineDifference = true;
    i.manualReview = false;
    i.showLineEndings = true;
    i.showTabs = true;
    i.showWhitespaceErrors = true;
    i.syntaxHighlighting = true;
    i.hideTopMenu = false;
    i.autoHideDiffTableHeader = true;
    i.hideLineNumbers = false;
    i.renderEntireFile = false;
    i.hideEmptyPane = false;
    i.matchBrackets = false;
    i.lineWrapping = false;
    i.ignoreWhitespace = Whitespace.IGNORE_NONE;
    i.retainHeader = false;
    i.skipDeleted = false;
    i.skipUnchanged = false;
    i.skipUncommented = false;
    return i;
  }
}

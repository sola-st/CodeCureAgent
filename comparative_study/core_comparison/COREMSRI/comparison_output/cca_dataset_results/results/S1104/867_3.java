// Copyright (C) 2015 The Android Open Source Project
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

import java.util.List;

/** Preferences about a single user. */
public class GeneralPreferencesInfo {

  /** Default number of items to display per page. */
  public static final int DEFAULT_PAGESIZE = 25;

  /** Valid choices for the page size. */
  public static final int[] PAGESIZE_CHOICES = {10, 25, 50, 100};

  /** Preferred method to download a change. */
  public enum DownloadCommand {
    REPO_DOWNLOAD,
    PULL,
    CHECKOUT,
    CHERRY_PICK,
    FORMAT_PATCH
  }

  public enum DateFormat {
    /** US style dates: Apr 27, Feb 14, 2010 */
    STD("MMM d", "MMM d, yyyy"),

    /** US style dates: 04/27, 02/14/10 */
    US("MM/dd", "MM/dd/yy"),

    /** ISO style dates: 2010-02-14 */
    ISO("MM-dd", "yyyy-MM-dd"),

    /** European style dates: 27. Apr, 27.04.2010 */
    EURO("d. MMM", "dd.MM.yyyy"),

    /** UK style dates: 27/04, 27/04/2010 */
    UK("dd/MM", "dd/MM/yyyy");

    private final String shortFormat;
    private final String longFormat;

    DateFormat(String shortFormat, String longFormat) {
      this.shortFormat = shortFormat;
      this.longFormat = longFormat;
    }

    public String getShortFormat() {
      return shortFormat;
    }

    public String getLongFormat() {
      return longFormat;
    }
  }

  public enum DiffView {
    SIDE_BY_SIDE,
    UNIFIED_DIFF
  }

  public enum EmailStrategy {
    ENABLED,
    CC_ON_OWN_COMMENTS,
    DISABLED
  }

  public enum EmailFormat {
    PLAINTEXT,
    HTML_PLAINTEXT
  }

  public enum DefaultBase {
    AUTO_MERGE(null),
    FIRST_PARENT(-1);

    private final String base;

    DefaultBase(String base) {
      this.base = base;
    }

    DefaultBase(int base) {
      this(Integer.toString(base));
    }

    public String getBase() {
      return base;
    }
  }

  public enum Theme {
    DARK,
    LIGHT
  }

  public enum TimeFormat {
    /** 12-hour clock: 1:15 am, 2:13 pm */
    HHMM_12("h:mm a"),

    /** 24-hour clock: 01:15, 14:13 */
    HHMM_24("HH:mm");

    private final String format;

    TimeFormat(String format) {
      this.format = format;
    }

    public String getFormat() {
      return format;
    }
  }

  /** Number of changes to show in a screen. */
  private Integer changesPerPage;
  /** Type of download URL the user prefers to use. */
  private String downloadScheme;

  private Theme theme;
  private DateFormat dateFormat;
  private TimeFormat timeFormat;
  private Boolean expandInlineDiffs;
  private Boolean highlightAssigneeInChangeTable;
  private Boolean relativeDateInChangeTable;
  private DiffView diffView;
  private Boolean sizeBarInChangeTable;
  private Boolean legacycidInChangeTable;
  private Boolean muteCommonPathPrefixes;
  private Boolean signedOffBy;
  private EmailStrategy emailStrategy;
  private EmailFormat emailFormat;
  private DefaultBase defaultBaseForMerges;
  private Boolean publishCommentsOnPush;
  private Boolean workInProgressByDefault;
  private List<MenuItem> my;
  private List<String> changeTable;

  public Integer getChangesPerPage() {
    return changesPerPage;
  }

  public void setChangesPerPage(Integer changesPerPage) {
    this.changesPerPage = changesPerPage;
  }

  public String getDownloadScheme() {
    return downloadScheme;
  }

  public void setDownloadScheme(String downloadScheme) {
    this.downloadScheme = downloadScheme;
  }

  public Theme getTheme() {
    return theme;
  }

  public void setTheme(Theme theme) {
    this.theme = theme;
  }

  public DateFormat getDateFormat() {
    if (dateFormat == null) {
      return DateFormat.STD;
    }
    return dateFormat;
  }

  public void setDateFormat(DateFormat dateFormat) {
    this.dateFormat = dateFormat;
  }

  public TimeFormat getTimeFormat() {
    if (timeFormat == null) {
      return TimeFormat.HHMM_12;
    }
    return timeFormat;
  }

  public void setTimeFormat(TimeFormat timeFormat) {
    this.timeFormat = timeFormat;
  }

  public Boolean getExpandInlineDiffs() {
    return expandInlineDiffs;
  }

  public void setExpandInlineDiffs(Boolean expandInlineDiffs) {
    this.expandInlineDiffs = expandInlineDiffs;
  }

  public Boolean getHighlightAssigneeInChangeTable() {
    return highlightAssigneeInChangeTable;
  }

  public void setHighlightAssigneeInChangeTable(Boolean highlightAssigneeInChangeTable) {
    this.highlightAssigneeInChangeTable = highlightAssigneeInChangeTable;
  }

  public Boolean getRelativeDateInChangeTable() {
    return relativeDateInChangeTable;
  }

  public void setRelativeDateInChangeTable(Boolean relativeDateInChangeTable) {
    this.relativeDateInChangeTable = relativeDateInChangeTable;
  }

  public DiffView getDiffView() {
    if (diffView == null) {
      return DiffView.SIDE_BY_SIDE;
    }
    return diffView;
  }

  public void setDiffView(DiffView diffView) {
    this.diffView = diffView;
  }

  public Boolean getSizeBarInChangeTable() {
    return sizeBarInChangeTable;
  }

  public void setSizeBarInChangeTable(Boolean sizeBarInChangeTable) {
    this.sizeBarInChangeTable = sizeBarInChangeTable;
  }

  public Boolean getLegacycidInChangeTable() {
    return legacycidInChangeTable;
  }

  public void setLegacycidInChangeTable(Boolean legacycidInChangeTable) {
    this.legacycidInChangeTable = legacycidInChangeTable;
  }

  public Boolean getMuteCommonPathPrefixes() {
    return muteCommonPathPrefixes;
  }

  public void setMuteCommonPathPrefixes(Boolean muteCommonPathPrefixes) {
    this.muteCommonPathPrefixes = muteCommonPathPrefixes;
  }

  public Boolean getSignedOffBy() {
    return signedOffBy;
  }

  public void setSignedOffBy(Boolean signedOffBy) {
    this.signedOffBy = signedOffBy;
  }

  public EmailStrategy getEmailStrategy() {
    if (emailStrategy == null) {
      return EmailStrategy.ENABLED;
    }
    return emailStrategy;
  }

  public void setEmailStrategy(EmailStrategy emailStrategy) {
    this.emailStrategy = emailStrategy;
  }

  public EmailFormat getEmailFormat() {
    if (emailFormat == null) {
      return EmailFormat.HTML_PLAINTEXT;
    }
    return emailFormat;
  }

  public void setEmailFormat(EmailFormat emailFormat) {
    this.emailFormat = emailFormat;
  }

  public DefaultBase getDefaultBaseForMerges() {
    return defaultBaseForMerges;
  }

  public void setDefaultBaseForMerges(DefaultBase defaultBaseForMerges) {
    this.defaultBaseForMerges = defaultBaseForMerges;
  }

  public Boolean getPublishCommentsOnPush() {
    return publishCommentsOnPush;
  }

  public void setPublishCommentsOnPush(Boolean publishCommentsOnPush) {
    this.publishCommentsOnPush = publishCommentsOnPush;
  }

  public Boolean getWorkInProgressByDefault() {
    return workInProgressByDefault;
  }

  public void setWorkInProgressByDefault(Boolean workInProgressByDefault) {
    this.workInProgressByDefault = workInProgressByDefault;
  }

  public List<MenuItem> getMy() {
    return my;
  }

  public void setMy(List<MenuItem> my) {
    this.my = my;
  }

  public List<String> getChangeTable() {
    return changeTable;
  }

  public void setChangeTable(List<String> changeTable) {
    this.changeTable = changeTable;
  }

  public static GeneralPreferencesInfo defaults() {
    GeneralPreferencesInfo p = new GeneralPreferencesInfo();
    p.changesPerPage = DEFAULT_PAGESIZE;
    p.downloadScheme = null;
    p.theme = Theme.LIGHT;
    p.dateFormat = DateFormat.STD;
    p.timeFormat = TimeFormat.HHMM_12;
    p.expandInlineDiffs = false;
    p.highlightAssigneeInChangeTable = true;
    p.relativeDateInChangeTable = false;
    p.diffView = DiffView.SIDE_BY_SIDE;
    p.sizeBarInChangeTable = true;
    p.legacycidInChangeTable = false;
    p.muteCommonPathPrefixes = true;
    p.signedOffBy = false;
    p.emailStrategy = EmailStrategy.ENABLED;
    p.emailFormat = EmailFormat.HTML_PLAINTEXT;
    p.defaultBaseForMerges = DefaultBase.FIRST_PARENT;
    p.publishCommentsOnPush = false;
    p.workInProgressByDefault = false;
    return p;
  }
}


```java
// Copyright (C) 2016 The Android Open Source Project
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

package com.google.gerrit.extensions.common;

/** API response containing values from {@code gerrit.config} as nested objects. */
public class ServerInfo {
  private AccountsInfo accounts;
  private AuthInfo auth;
  private ChangeConfigInfo change;
  private DownloadInfo download;
  private GerritInfo gerrit;
  private Boolean noteDbEnabled;
  private PluginConfigInfo plugin;
  private SshdInfo sshd;
  private SuggestInfo suggest;
  private UserConfigInfo user;
  private ReceiveInfo receive;
  private String defaultTheme;

  public AccountsInfo getAccounts() {
    return accounts;
  }

  public void setAccounts(AccountsInfo accounts) {
    this.accounts = accounts;
  }

  public AuthInfo getAuth() {
    return auth;
  }

  public void setAuth(AuthInfo auth) {
    this.auth = auth;
  }

  public ChangeConfigInfo getChange() {
    return change;
  }

  public void setChange(ChangeConfigInfo change) {
    this.change = change;
  }

  public DownloadInfo getDownload() {
    return download;
  }

  public void setDownload(DownloadInfo download) {
    this.download = download;
  }

  public GerritInfo getGerrit() {
    return gerrit;
  }

  public void setGerrit(GerritInfo gerrit) {
    this.gerrit = gerrit;
  }

  public Boolean getNoteDbEnabled() {
    return noteDbEnabled;
  }

  public void setNoteDbEnabled(Boolean noteDbEnabled) {
    this.noteDbEnabled = noteDbEnabled;
  }

  public PluginConfigInfo getPlugin() {
    return plugin;
  }

  public void setPlugin(PluginConfigInfo plugin) {
    this.plugin = plugin;
  }

  public SshdInfo getSshd() {
    return sshd;
  }

  public void setSshd(SshdInfo sshd) {
    this.sshd = sshd;
  }

  public SuggestInfo getSuggest() {
    return suggest;
  }

  public void setSuggest(SuggestInfo suggest) {
    this.suggest = suggest;
  }

  public UserConfigInfo getUser() {
    return user;
  }

  public void setUser(UserConfigInfo user) {
    this.user = user;
  }

  public ReceiveInfo getReceive() {
    return receive;
  }

  public void setReceive(ReceiveInfo receive) {
    this.receive = receive;
  }

  public String getDefaultTheme() {
    return defaultTheme;
  }

  public void setDefaultTheme(String defaultTheme) {
    this.defaultTheme = defaultTheme;
  }
}

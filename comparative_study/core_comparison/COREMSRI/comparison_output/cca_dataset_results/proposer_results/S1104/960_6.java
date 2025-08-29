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

package com.google.gerrit.extensions.common;

import com.google.gerrit.extensions.client.ChangeKind;
import java.sql.Timestamp;
import java.util.Map;

public class RevisionInfo {
  // ActionJson#copy(List, RevisionInfo) must be adapted if new fields are added that are not
  // protected by any ListChangesOption.
  public transient boolean isCurrent;
  private ChangeKind kind;
  private int _number;
  private Timestamp created;
  private AccountInfo uploader;
  private String ref;
  private Map<String, FetchInfo> fetch;
  private CommitInfo commit;
  private Map<String, FileInfo> files;
  private Map<String, ActionInfo> actions;
  private String commitWithFooters;
  private PushCertificateInfo pushCertificate;
  private String description;

  public ChangeKind getKind() {
    return kind;
  }

  public void setKind(ChangeKind kind) {
    this.kind = kind;
  }

  public int get_number() {
    return _number;
  }

  public void set_number(int _number) {
    this._number = _number;
  }

  public Timestamp getCreated() {
    return created;
  }

  public void setCreated(Timestamp created) {
    this.created = created;
  }

  public AccountInfo getUploader() {
    return uploader;
  }

  public void setUploader(AccountInfo uploader) {
    this.uploader = uploader;
  }

  public String getRef() {
    return ref;
  }

  public void setRef(String ref) {
    this.ref = ref;
  }

  public Map<String, FetchInfo> getFetch() {
    return fetch;
  }

  public void setFetch(Map<String, FetchInfo> fetch) {
    this.fetch = fetch;
  }

  public CommitInfo getCommit() {
    return commit;
  }

  public void setCommit(CommitInfo commit) {
    this.commit = commit;
  }

  public Map<String, FileInfo> getFiles() {
    return files;
  }

  public void setFiles(Map<String, FileInfo> files) {
    this.files = files;
  }

  public Map<String, ActionInfo> getActions() {
    return actions;
  }

  public void setActions(Map<String, ActionInfo> actions) {
    this.actions = actions;
  }

  public String getCommitWithFooters() {
    return commitWithFooters;
  }

  public void setCommitWithFooters(String commitWithFooters) {
    this.commitWithFooters = commitWithFooters;
  }

  public PushCertificateInfo getPushCertificate() {
    return pushCertificate;
  }

  public void setPushCertificate(PushCertificateInfo pushCertificate) {
    this.pushCertificate = pushCertificate;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}

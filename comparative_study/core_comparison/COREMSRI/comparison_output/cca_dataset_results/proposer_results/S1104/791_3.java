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

package com.google.gerrit.extensions.common;

import java.sql.Timestamp;
import java.util.List;

public class GroupInfo extends GroupBaseInfo {
  private String url;
  private GroupOptionsInfo options;

  // These fields are only supplied for internal groups.
  private String description;
  private Integer groupId;
  private String owner;
  private String ownerId;
  private Timestamp createdOn;
  private Boolean _moreGroups;

  // These fields are only supplied for internal groups, and only if requested.
  private List<AccountInfo> members;
  private List<GroupInfo> includes;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public GroupOptionsInfo getOptions() {
    return options;
  }

  public void setOptions(GroupOptionsInfo options) {
    this.options = options;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Integer getGroupId() {
    return groupId;
  }

  public void setGroupId(Integer groupId) {
    this.groupId = groupId;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public Timestamp getCreatedOn() {
    return createdOn;
  }

  public void setCreatedOn(Timestamp createdOn) {
    this.createdOn = createdOn;
  }

  public Boolean get_moreGroups() {
    return _moreGroups;
  }

  public void set_moreGroups(Boolean _moreGroups) {
    this._moreGroups = _moreGroups;
  }

  public List<AccountInfo> getMembers() {
    return members;
  }

  public void setMembers(List<AccountInfo> members) {
    this.members = members;
  }

  public List<GroupInfo> getIncludes() {
    return includes;
  }

  public void setIncludes(List<GroupInfo> includes) {
    this.includes = includes;
  }
}
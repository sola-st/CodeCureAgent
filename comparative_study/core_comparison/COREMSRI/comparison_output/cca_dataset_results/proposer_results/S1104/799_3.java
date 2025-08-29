// Copyright (C) 2017 The Android Open Source Project
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

package com.google.gerrit.extensions.api.projects;

import java.util.ArrayList;
import java.util.List;

public class DashboardInfo {
  private String id;
  private String project;
  private String definingProject;
  private String ref;
  private String path;
  private String description;
  private String foreach;
  private String url;

  private Boolean isDefault;

  private String title;
  private List<DashboardSectionInfo> sections = new ArrayList<>();

  public DashboardInfo() {}

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getProject() {
    return project;
  }

  public void setProject(String project) {
    this.project = project;
  }

  public String getDefiningProject() {
    return definingProject;
  }

  public void setDefiningProject(String definingProject) {
    this.definingProject = definingProject;
  }

  public String getRef() {
    return ref;
  }

  public void setRef(String ref) {
    this.ref = ref;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getForeach() {
    return foreach;
  }

  public void setForeach(String foreach) {
    this.foreach = foreach;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Boolean getIsDefault() {
    return isDefault;
  }

  public void setIsDefault(Boolean isDefault) {
    this.isDefault = isDefault;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public List<DashboardSectionInfo> getSections() {
    return sections;
  }

  public void setSections(List<DashboardSectionInfo> sections) {
    this.sections = sections;
  }
}

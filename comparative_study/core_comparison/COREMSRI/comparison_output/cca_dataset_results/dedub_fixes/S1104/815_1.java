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

import com.google.gerrit.extensions.client.AccountFieldName;
import com.google.gerrit.extensions.client.AuthType;
import com.google.gerrit.extensions.client.GitBasicAuthPolicy;
import java.util.List;

/**
 * Representation of auth-related server configuration in the REST API.
 *
 * <p>This class determines the JSON format of auth-related server configuration in the REST API.
 *
 * <p>The contained values come from the {@code auth} section of {@code gerrit.config}.
 */
public class AuthInfo {
  /**
   * The authentication type that is configured on the server.
   *
   * <p>The value of the {@code auth.type} parameter in {@code gerrit.config}.
   */
  private AuthType authType;

  /**
   * Whether contributor agreements are required.
   *
   * <p>The value of the {@code auth.contributorAgreements} parameter in {@code gerrit.config}.
   */
  private Boolean useContributorAgreements;

  /** List of contributor agreements that have been configured on the server. */
  private List<AgreementInfo> contributorAgreements;

  /** List of account fields that are editable. */
  private List<AccountFieldName> editableAccountFields;

  /**
   * The login URL.
   *
   * <p>The value of the {@code auth.loginUrl} parameter in {@code gerrit.config}.
   *
   * <p>Only set if authentication type is {@code HTTP} or {@code HTTP_LDAP}.
   */
  private String loginUrl;

  /**
   * The login text.
   *
   * <p>The value of the {@code auth.loginText} parameter in {@code gerrit.config}.
   *
   * <p>Only set if authentication type is {@code HTTP} or {@code HTTP_LDAP}.
   */
  private String loginText;

  /**
   * The URL to switch accounts.
   *
   * <p>The value of the {@code auth.switchAccountUrl} parameter in {@code gerrit.config}.
   */
  private String switchAccountUrl;

  /**
   * The register URL.
   *
   * <p>The value of the {@code auth.registerUrl} parameter in {@code gerrit.config}.
   *
   * <p>Only set if authentication type is {@code LDAP}, {@code LDAP_BIND} or {@code
   * CUSTOM_EXTENSION}.
   */
  private String registerUrl;

  /**
   * The register text.
   *
   * <p>The value of the {@code auth.registerText} parameter in {@code gerrit.config}.
   *
   * <p>Only set if authentication type is {@code LDAP}, {@code LDAP_BIND} or {@code
   * CUSTOM_EXTENSION}.
   */
  private String registerText;

  /**
   * The URL to edit the full name.
   *
   * <p>The value of the {@code auth.editFullNameUrl} parameter in {@code gerrit.config}.
   *
   * <p>Only set if authentication type is {@code LDAP}, {@code LDAP_BIND} or {@code
   * CUSTOM_EXTENSION}.
   */
  private String editFullNameUrl;

  /**
   * The URL to obtain an HTTP password.
   *
   * <p>The value of the {@code auth.httpPasswordUrl} parameter in {@code gerrit.config}.
   *
   * <p>Only set if authentication type is {@code CUSTOM_EXTENSION}.
   */
  private String httpPasswordUrl;

  /**
   * The policy to authenticate Git over HTTP and REST API requests.
   *
   * <p>The value of the {@code auth.gitBasicAuthPolicy} parameter in {@code gerrit.config}.
   *
   * <p>Only set if authentication type is {@code LDAP}, {@code LDAP_BIND} or {@code OAUTH}.
   */
  private GitBasicAuthPolicy gitBasicAuthPolicy;

  public AuthType getAuthType() {
    return authType;
  }

  public void setAuthType(AuthType authType) {
    this.authType = authType;
  }

  public Boolean getUseContributorAgreements() {
    return useContributorAgreements;
  }

  public void setUseContributorAgreements(Boolean useContributorAgreements) {
    this.useContributorAgreements = useContributorAgreements;
  }

  public List<AgreementInfo> getContributorAgreements() {
    return contributorAgreements;
  }

  public void setContributorAgreements(List<AgreementInfo> contributorAgreements) {
    this.contributorAgreements = contributorAgreements;
  }

  public List<AccountFieldName> getEditableAccountFields() {
    return editableAccountFields;
  }

  public void setEditableAccountFields(List<AccountFieldName> editableAccountFields) {
    this.editableAccountFields = editableAccountFields;
  }

  public String getLoginUrl() {
    return loginUrl;
  }

  public void setLoginUrl(String loginUrl) {
    this.loginUrl = loginUrl;
  }

  public String getLoginText() {
    return loginText;
  }

  public void setLoginText(String loginText) {
    this.loginText = loginText;
  }

  public String getSwitchAccountUrl() {
    return switchAccountUrl;
  }

  public void setSwitchAccountUrl(String switchAccountUrl) {
    this.switchAccountUrl = switchAccountUrl;
  }

  public String getRegisterUrl() {
    return registerUrl;
  }

  public void setRegisterUrl(String registerUrl) {
    this.registerUrl = registerUrl;
  }

  public String getRegisterText() {
    return registerText;
  }

  public void setRegisterText(String registerText) {
    this.registerText = registerText;
  }

  public String getEditFullNameUrl() {
    return editFullNameUrl;
  }

  public void setEditFullNameUrl(String editFullNameUrl) {
    this.editFullNameUrl = editFullNameUrl;
  }

  public String getHttpPasswordUrl() {
    return httpPasswordUrl;
  }

  public void setHttpPasswordUrl(String httpPasswordUrl) {
    this.httpPasswordUrl = httpPasswordUrl;
  }

  public GitBasicAuthPolicy getGitBasicAuthPolicy() {
    return gitBasicAuthPolicy;
  }

  public void setGitBasicAuthPolicy(GitBasicAuthPolicy gitBasicAuthPolicy) {
    this.gitBasicAuthPolicy = gitBasicAuthPolicy;
  }
}
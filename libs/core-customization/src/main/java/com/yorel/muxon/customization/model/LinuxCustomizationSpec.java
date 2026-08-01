/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.customization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LinuxCustomizationSpec {
  private List<GuestUser> users;
  private List<String> packages;
  private List<Map<String, Object>> writeFiles;
  private List<String> bootcmd;
  private List<String> runcmd;

  /** Resolved script content to embed in bootcmd stage (pre-network). */
  private List<String> resolvedPreScripts;

  /** Resolved script content to embed in runcmd stage (post-all-config). */
  private List<String> resolvedPostScripts;

  public List<GuestUser> getUsers() {
    return users;
  }

  public void setUsers(List<GuestUser> users) {
    this.users = users;
  }

  public List<String> getPackages() {
    return packages;
  }

  public void setPackages(List<String> packages) {
    this.packages = packages;
  }

  public List<Map<String, Object>> getWriteFiles() {
    return writeFiles;
  }

  public void setWriteFiles(List<Map<String, Object>> writeFiles) {
    this.writeFiles = writeFiles;
  }

  public List<String> getBootcmd() {
    return bootcmd;
  }

  public void setBootcmd(List<String> bootcmd) {
    this.bootcmd = bootcmd;
  }

  public List<String> getRuncmd() {
    return runcmd;
  }

  public void setRuncmd(List<String> runcmd) {
    this.runcmd = runcmd;
  }

  public List<String> getResolvedPreScripts() {
    return resolvedPreScripts;
  }

  public void setResolvedPreScripts(List<String> resolvedPreScripts) {
    this.resolvedPreScripts = resolvedPreScripts;
  }

  public List<String> getResolvedPostScripts() {
    return resolvedPostScripts;
  }

  public void setResolvedPostScripts(List<String> resolvedPostScripts) {
    this.resolvedPostScripts = resolvedPostScripts;
  }
}

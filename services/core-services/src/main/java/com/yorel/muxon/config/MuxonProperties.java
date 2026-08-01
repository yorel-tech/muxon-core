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
package com.yorel.muxon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Binds {@code muxon.instanceName}, {@code muxon.instanceId}, and optional content-library paths.
 */
@ConfigurationProperties(prefix = "muxon", ignoreUnknownFields = true)
public class MuxonProperties {

  private String instanceName = "local";
  private int instanceId = 1;

  @NestedConfigurationProperty private ContentLibraries contentLibraries = new ContentLibraries();

  public String getInstanceName() {
    return instanceName;
  }

  public void setInstanceName(String instanceName) {
    this.instanceName = instanceName;
  }

  public int getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(int instanceId) {
    this.instanceId = instanceId;
  }

  public ContentLibraries getContentLibraries() {
    return contentLibraries;
  }

  public void setContentLibraries(ContentLibraries contentLibraries) {
    this.contentLibraries = contentLibraries;
  }

  public static class ContentLibraries {

    /**
     * Root directory where uploaded binaries are stored using {@code providerRelativePath} layout.
     * For hypervisor access, mount this path (or a parent) on the storage pool. When empty,
     * defaults to {@code /var/lib/muxon/content-libraries}.
     */
    private String uploadArtifactRoot = "/var/lib/muxon/content-libraries";

    public String getUploadArtifactRoot() {
      return uploadArtifactRoot;
    }

    public void setUploadArtifactRoot(String uploadArtifactRoot) {
      this.uploadArtifactRoot = uploadArtifactRoot;
    }
  }
}

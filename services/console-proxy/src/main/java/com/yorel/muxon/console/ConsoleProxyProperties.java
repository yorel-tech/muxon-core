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
package com.yorel.muxon.console;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "muxon.console")
public class ConsoleProxyProperties {

  private List<String> allowedOrigins =
      new ArrayList<>(
          List.of(
              "http://localhost:4000",
              "http://127.0.0.1:4000",
              "http://localhost:3000",
              "http://127.0.0.1:3000"));

  private long cleanupIntervalMs = 60_000L;

  /**
   * When true, trust all TLS certificates when connecting to hypervisor VNC (e.g. Proxmox). Use
   * only in dev/lab.
   */
  private boolean trustAllHypervisorTls = true;

  public List<String> getAllowedOrigins() {
    return allowedOrigins;
  }

  public void setAllowedOrigins(List<String> allowedOrigins) {
    this.allowedOrigins = allowedOrigins;
  }

  public long getCleanupIntervalMs() {
    return cleanupIntervalMs;
  }

  public void setCleanupIntervalMs(long cleanupIntervalMs) {
    this.cleanupIntervalMs = cleanupIntervalMs;
  }

  public boolean isTrustAllHypervisorTls() {
    return trustAllHypervisorTls;
  }

  public void setTrustAllHypervisorTls(boolean trustAllHypervisorTls) {
    this.trustAllHypervisorTls = trustAllHypervisorTls;
  }
}

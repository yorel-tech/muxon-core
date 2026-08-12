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

/**
 * Runtime customization spec attached to a VM deployment request. Deserialized from {@code
 * vms.customization} JSONB (secrets already decrypted by the worker).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VmCustomizationSpec {

  /** Guest OS family: linux or windows. Sourced from the template at merge time. */
  private String osFamily;

  private String hostname;
  private String domain;
  private String timezone;
  private List<String> dnsServers;
  private List<String> dnsSearch;
  private List<NicCustomizationSpec> nics;
  private LinuxCustomizationSpec linux;
  private WindowsCustomizationSpec windows;

  public String getOsFamily() {
    return osFamily;
  }

  public void setOsFamily(String osFamily) {
    this.osFamily = osFamily;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public String getTimezone() {
    return timezone;
  }

  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }

  public List<String> getDnsServers() {
    return dnsServers;
  }

  public void setDnsServers(List<String> dnsServers) {
    this.dnsServers = dnsServers;
  }

  public List<String> getDnsSearch() {
    return dnsSearch;
  }

  public void setDnsSearch(List<String> dnsSearch) {
    this.dnsSearch = dnsSearch;
  }

  public List<NicCustomizationSpec> getNics() {
    return nics;
  }

  public void setNics(List<NicCustomizationSpec> nics) {
    this.nics = nics;
  }

  public LinuxCustomizationSpec getLinux() {
    return linux;
  }

  public void setLinux(LinuxCustomizationSpec linux) {
    this.linux = linux;
  }

  public WindowsCustomizationSpec getWindows() {
    return windows;
  }

  public void setWindows(WindowsCustomizationSpec windows) {
    this.windows = windows;
  }

  public boolean isLinux() {
    return "linux".equalsIgnoreCase(osFamily);
  }

  public boolean isWindows() {
    return "windows".equalsIgnoreCase(osFamily);
  }
}

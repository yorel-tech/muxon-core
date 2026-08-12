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

/** Per-NIC network customization — deserialized from the VmCustomizationSpec JSON. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NicCustomizationSpec {
  /** 0-based NIC index or template network id. */
  private String nicId;

  /** dhcp or static. */
  private String ipAllocation = "dhcp";

  private String ipAddress;
  private Integer prefix;
  private String gateway;
  private List<String> dnsServers;

  public String getNicId() {
    return nicId;
  }

  public void setNicId(String nicId) {
    this.nicId = nicId;
  }

  public String getIpAllocation() {
    return ipAllocation;
  }

  public void setIpAllocation(String ipAllocation) {
    this.ipAllocation = ipAllocation;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public Integer getPrefix() {
    return prefix;
  }

  public void setPrefix(Integer prefix) {
    this.prefix = prefix;
  }

  public String getGateway() {
    return gateway;
  }

  public void setGateway(String gateway) {
    this.gateway = gateway;
  }

  public List<String> getDnsServers() {
    return dnsServers;
  }

  public void setDnsServers(List<String> dnsServers) {
    this.dnsServers = dnsServers;
  }

  public boolean isStatic() {
    return "static".equalsIgnoreCase(ipAllocation);
  }
}

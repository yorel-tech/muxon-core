package com.krito.muxon.customization.model;

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

    public String getNicId() { return nicId; }
    public void setNicId(String nicId) { this.nicId = nicId; }

    public String getIpAllocation() { return ipAllocation; }
    public void setIpAllocation(String ipAllocation) { this.ipAllocation = ipAllocation; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Integer getPrefix() { return prefix; }
    public void setPrefix(Integer prefix) { this.prefix = prefix; }

    public String getGateway() { return gateway; }
    public void setGateway(String gateway) { this.gateway = gateway; }

    public List<String> getDnsServers() { return dnsServers; }
    public void setDnsServers(List<String> dnsServers) { this.dnsServers = dnsServers; }

    public boolean isStatic() {
        return "static".equalsIgnoreCase(ipAllocation);
    }
}

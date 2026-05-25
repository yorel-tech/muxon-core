package com.sal.muxon.providers.libvirt;

import java.util.List;

/**
 * Represents hypervisor information from Libvirt.
 *
 * <p>Used to populate node entity with hypervisor details
 * when a Libvirt provider is created.</p>
 */
public class LibvirtHypervisorInfo {

    private String hypervisorType;
    private String libvirtVersion;
    private String qemuVersion;
    private String cpuModel;
    private int cpuCores;
    private int memoryMb;
    private List<String> ipAddresses;

    /**
     * Gets the hypervisor type (e.g., KVM, QEMU).
     *
     * @return Hypervisor type
     */
    public String getHypervisorType() {
        return hypervisorType;
    }

    /**
     * Sets the hypervisor type.
     *
     * @param hypervisorType The hypervisor type
     */
    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    /**
     * Gets the Libvirt version.
     *
     * @return Libvirt version
     */
    public String getLibvirtVersion() {
        return libvirtVersion;
    }

    /**
     * Sets the Libvirt version.
     *
     * @param libvirtVersion The Libvirt version
     */
    public void setLibvirtVersion(String libvirtVersion) {
        this.libvirtVersion = libvirtVersion;
    }

    /**
     * Gets the QEMU version.
     *
     * @return QEMU version
     */
    public String getQemuVersion() {
        return qemuVersion;
    }

    /**
     * Sets the QEMU version.
     *
     * @param qemuVersion The QEMU version
     */
    public void setQemuVersion(String qemuVersion) {
        this.qemuVersion = qemuVersion;
    }

    /**
     * Gets the CPU model.
     *
     * @return CPU model
     */
    public String getCpuModel() {
        return cpuModel;
    }

    /**
     * Sets the CPU model.
     *
     * @param cpuModel The CPU model
     */
    public void setCpuModel(String cpuModel) {
        this.cpuModel = cpuModel;
    }

    /**
     * Gets the total CPU cores.
     *
     * @return Total CPU cores
     */
    public int getCpuCores() {
        return cpuCores;
    }

    /**
     * Sets the total CPU cores.
     *
     * @param cpuCores The total CPU cores
     */
    public void setCpuCores(int cpuCores) {
        this.cpuCores = cpuCores;
    }

    /**
     * Gets the total memory in MB.
     *
     * @return Total memory in MB
     */
    public int getMemoryMb() {
        return memoryMb;
    }

    /**
     * Sets the total memory in MB.
     *
     * @param memoryMb The total memory in MB
     */
    public void setMemoryMb(int memoryMb) {
        this.memoryMb = memoryMb;
    }

    /**
     * Gets the hypervisor IP addresses.
     *
     * @return List of IP addresses
     */
    public List<String> getIpAddresses() {
        return ipAddresses;
    }

    /**
     * Sets the hypervisor IP addresses.
     *
     * @param ipAddresses The hypervisor IP addresses
     */
    public void setIpAddresses(List<String> ipAddresses) {
        this.ipAddresses = ipAddresses;
    }
}

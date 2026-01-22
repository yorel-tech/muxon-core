package com.onetattva.infron.core.providers.libvirt;

/**
 * Represents parsed Libvirt capabilities.
 *
 * <p>Used by LibvirtVmProvider to return hypervisor capabilities.</p>
 */
public class LibvirtCapabilities {

    private String arch;
    private String hypervisorType;
    private int maxMemoryMb;
    private int maxStorageGb;
    private int maxVms;
    private int maxCpuCores;
    private java.util.List<String> supportedCpuTypes = new java.util.ArrayList<>();
    private java.util.List<String> supportedDiskTypes = new java.util.ArrayList<>();
    private java.util.List<String> supportedNetworkModels = new java.util.ArrayList<>();
    private boolean supportsVirtio = false;
    private boolean supportsNestedVirtualization = false;
    private boolean supportsLiveMigration = false;

    /**
     * Gets maximum CPU cores.
     *
     * @return Maximum CPU cores
     */
    public int getMaxCpuCores() {
        return maxCpuCores;
    }

    /**
     * Sets maximum CPU cores.
     *
     * @param maxCpuCores Maximum CPU cores
     */
    public void setMaxCpuCores(int maxCpuCores) {
        this.maxCpuCores = maxCpuCores;
    }

    /**
     * Gets maximum memory in MB.
     *
     * @return Maximum memory in MB
     */
    public int getMaxMemoryMb() {
        return maxMemoryMb;
    }

    /**
     * Sets maximum memory in MB.
     *
     * @param maxMemoryMb Maximum memory in MB
     */
    public void setMaxMemoryMb(int maxMemoryMb) {
        this.maxMemoryMb = maxMemoryMb;
    }

    /**
     * Gets maximum storage in GB.
     *
     * @return Maximum storage in GB
     */
    public int getMaxStorageGb() {
        return maxStorageGb;
    }

    /**
     * Sets maximum storage in GB.
     *
     * @param maxStorageGb Maximum storage in GB
     */
    public void setMaxStorageGb(int maxStorageGb) {
        this.maxStorageGb = maxStorageGb;
    }

    /**
     * Gets maximum number of VMs.
     *
     * @return Maximum number of VMs
     */
    public int getMaxVms() {
        return maxVms;
    }

    /**
     * Sets maximum number of VMs.
     *
     * @param maxVms Maximum number of VMs
     */
    public void setMaxVms(int maxVms) {
        this.maxVms = maxVms;
    }

    /**
     * Gets supported CPU types.
     *
     * @return List of supported CPU types
     */
    public java.util.List<String> getSupportedCpuTypes() {
        return supportedCpuTypes;
    }

    /**
     * Sets supported CPU types.
     *
     * @param supportedCpuTypes List of supported CPU types
     */
    public void setSupportedCpuTypes(java.util.List<String> supportedCpuTypes) {
        this.supportedCpuTypes = supportedCpuTypes;
    }

    /**
     * Gets supported disk types.
     *
     * @return List of supported disk types
     */
    public java.util.List<String> getSupportedDiskTypes() {
        return supportedDiskTypes;
    }

    /**
     * Sets supported disk types.
     *
     * @param supportedDiskTypes List of supported disk types
     */
    public void setSupportedDiskTypes(java.util.List<String> supportedDiskTypes) {
        this.supportedDiskTypes = supportedDiskTypes;
    }

    /**
     * Gets supported network models.
     *
     * @return List of supported network models
     */
    public java.util.List<String> getSupportedNetworkModels() {
        return supportedNetworkModels;
    }

    /**
     * Sets supported network models.
     *
     * @param supportedNetworkModels List of supported network models
     */
    public void setSupportedNetworkModels(java.util.List<String> supportedNetworkModels) {
        this.supportedNetworkModels = supportedNetworkModels;
    }

    /**
     * Checks if virtio is supported.
     *
     * @return true if virtio is supported, false otherwise
     */
    public boolean supportsVirtio() {
        return supportsVirtio;
    }

    /**
     * Sets virtio support flag.
     *
     * @param supportsVirtio true if virtio is supported, false otherwise
     */
    public void setSupportsVirtio(boolean supportsVirtio) {
        this.supportsVirtio = supportsVirtio;
    }

    /**
     * Checks if nested virtualization is supported.
     *
     * @return true if nested virtualization is supported, false otherwise
     */
    public boolean supportsNestedVirtualization() {
        return supportsNestedVirtualization;
    }

    /**
     * Sets nested virtualization support flag.
     *
     * @param supportsNestedVirtualization true if nested virtualization is supported, false otherwise
     */
    public void setSupportsNestedVirtualization(boolean supportsNestedVirtualization) {
        this.supportsNestedVirtualization = supportsNestedVirtualization;
    }

    /**
     * Checks if live migration is supported.
     *
     * @return true if live migration is supported, false otherwise
     */
    public boolean supportsLiveMigration() {
        return supportsLiveMigration;
    }

    /**
     * Sets live migration support flag.
     *
     * @param supportsLiveMigration true if live migration is supported, false otherwise
     */
    public void setSupportsLiveMigration(boolean supportsLiveMigration) {
        this.supportsLiveMigration = supportsLiveMigration;
    }

    /**
     * Gets architecture.
     *
     * @return Architecture (e.g., x86_64)
     */
    public String getArch() {
        return arch;
    }

    /**
     * Sets architecture.
     *
     * @param arch Architecture (e.g., x86_64)
     */
    public void setArch(String arch) {
        this.arch = arch;
    }

    /**
     * Gets hypervisor type.
     *
     * @return Hypervisor type (e.g., KVM, QEMU)
     */
    public String getHypervisorType() {
        return hypervisorType;
    }

    /**
     * Sets hypervisor type.
     *
     * @param hypervisorType Hypervisor type (e.g., KVM, QEMU)
     */
    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }
}

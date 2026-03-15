package com.onetattva.infron.core.providers.libvirt;

import java.util.UUID;

/**
 * Builds Libvirt XML domain definitions from VmSpec.
 *
 * <p>Converts Infron VM specifications to Libvirt-compatible
 * XML format for domain creation.</p>
 */
public class LibvirtXmlBuilder {

    /** Default disk size in GiB when not specified in spec. */
    static final long DEFAULT_DISK_CAPACITY_GIB = 10L;

    /**
     * Builds complete Libvirt domain XML for VM creation.
     *
     * @param vmId The VM ID
     * @param spec The VM specification JSON string
     * @return Libvirt domain XML string
     */
    public static String buildDomainXml(UUID vmId, String spec) {
        return buildDomainXml(vmId, spec, "/var/lib/libvirt/images/vm-" + vmId + ".qcow2");
    }

    /**
     * Builds complete Libvirt domain XML for VM creation with an explicit disk path.
     * Use this when the disk image is created via a storage pool (e.g. StorageVol.getPath()).
     *
     * @param vmId The VM ID
     * @param spec The VM specification JSON string
     * @param diskPath Absolute path to the qcow2 disk file on the hypervisor
     * @return Libvirt domain XML string
     */
    public static String buildDomainXml(UUID vmId, String spec, String diskPath) {

        // For now, create a basic domain XML
        // In a real implementation, we would parse the spec JSON and extract the details
        StringBuilder xml = new StringBuilder();
        xml.append("<domain type='kvm'>\n");
        xml.append("  <name>vm-").append(vmId).append("</name>\n");
        xml.append("  <uuid>").append(vmId).append("</uuid>\n");
        xml.append("  <memory unit='MiB'>2048</memory>\n");
        xml.append("  <vcpu>2</vcpu>\n");
        xml.append("  <os>\n");
        xml.append("    <type arch='x86_64' machine='pc'>hvm</type>\n");
        xml.append("  </os>\n");
        xml.append("  <features>\n");
        xml.append("    <acpi/>\n");
        xml.append("    <apic/>\n");
        xml.append("  </features>\n");
        xml.append("  <cpu mode='host-passthrough'>\n");
        xml.append("  </cpu>\n");
        xml.append("  <clock offset='utc'>\n");
        xml.append("  </clock>\n");
        xml.append("  <on_poweroff>destroy</on_poweroff>\n");
        xml.append("  <on_reboot>restart</on_reboot>\n");
        xml.append("  <on_crash>restart</on_crash>\n");
        xml.append("  <devices>\n");

        // Add disk using the provided path (must exist; create via storage pool before calling)
        xml.append("    <disk type='file' device='disk'>\n");
        xml.append("      <driver name='qemu' type='qcow2' cache='writeback'/>\n");
        xml.append("      <source file='").append(escapeXmlAttr(diskPath)).append("'/>\n");
        xml.append("      <target dev='vda' bus='virtio'/>\n");
        xml.append("    </disk>\n");

        // Add a basic network interface
        xml.append("    <interface type='network'>\n");
        xml.append("      <source network='default'/>\n");
        xml.append("      <model type='virtio'/>\n");
        xml.append("    </interface>\n");

        // Add console
        xml.append("    <console type='pty'>\n");
        xml.append("      <target type='serial' port='0'/>\n");
        xml.append("    </console>\n");

        xml.append("  </devices>\n");
        xml.append("</domain>\n");

        return xml.toString();
    }

    /**
     * Builds storage volume XML for creating a qcow2 disk in a libvirt storage pool.
     *
     * @param volumeName Name of the volume (e.g. vm-{uuid}.qcow2)
     * @param capacityGib Capacity in GiB
     * @return Volume XML for storageVolCreateXML
     */
    public static String buildVolumeXml(String volumeName, long capacityGib) {
        return "<volume>\n"
                + "  <name>" + escapeXmlText(volumeName) + "</name>\n"
                + "  <capacity unit='GiB'>" + capacityGib + "</capacity>\n"
                + "  <target><format type='qcow2'/></target>\n"
                + "</volume>";
    }

    private static String escapeXmlAttr(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static String escapeXmlText(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

}

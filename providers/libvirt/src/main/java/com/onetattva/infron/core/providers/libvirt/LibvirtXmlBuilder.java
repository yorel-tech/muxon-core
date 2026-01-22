package com.onetattva.infron.core.providers.libvirt;

import java.util.UUID;

/**
 * Builds Libvirt XML domain definitions from VmSpec.
 *
 * <p>Converts Infron VM specifications to Libvirt-compatible
 * XML format for domain creation.</p>
 */
public class LibvirtXmlBuilder {

    /**
     * Builds complete Libvirt domain XML for VM creation.
     *
     * @param vmId The VM ID
     * @param spec The VM specification JSON string
     * @return Libvirt domain XML string
     */
    public static String buildDomainXml(UUID vmId, String spec) {

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

        // Add a basic disk
        xml.append("    <disk type='file' device='disk'>\n");
        xml.append("      <driver name='qemu' type='qcow2' cache='writeback'/>\n");
        xml.append("      <source file='/var/lib/libvirt/images/vm-").append(vmId).append(".qcow2'/>\n");
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

        // logger.debug("Generated Libvirt domain XML for VM {}: {}", vmId, xml);

        return xml.toString();
    }

}

package com.onetattva.infron.core.providers.libvirt;

import com.onetattva.infron.core.providers.VmCreationRequest;
import com.onetattva.infron.core.providers.VmSpec;
import com.onetattva.infron.core.providers.models.StorageSpec;
import com.onetattva.infron.core.providers.models.NetworkSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Builds Libvirt XML domain definitions from VmSpec.
 *
 * <p>Converts Infron VM specifications to Libvirt-compatible
 * XML format for domain creation.</p>
 */
public class LibvirtXmlBuilder {

    private static final Logger logger = LoggerFactory.getLogger(LibvirtXmlBuilder.class);

    /**
     * Builds complete Libvirt domain XML for VM creation.
     *
     * @param request The VM creation request
     * @return Libvirt domain XML string
     */
    public static String buildDomainXml(VmCreationRequest request) {
        VmSpec spec = request.getSpec();
        UUID vmId = request.getVmId();

        StringBuilder xml = new StringBuilder();
        xml.append("<domain type='kvm'>\n");
        xml.append("  <name>").append(vmId).append("</name>\n");
        xml.append("  <uuid>").append(vmId).append("</uuid>\n");
        xml.append("  <memory unit='MiB'>").append(spec.getMemory().getSizeMb()).append("</memory>\n");
        xml.append("  <vcpu>").append(spec.getCpu().getCores()).append("</vcpu>\n");
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

        // Add disks
        for (int i = 0; i < spec.getStorage().size(); i++) {
            xml.append(buildDiskXml(spec.getStorage().get(i), i));
        }

        // Add network interfaces
        for (int i = 0; i < spec.getNetwork().size(); i++) {
            xml.append(buildNetworkXml(spec.getNetwork().get(i), i));
        }

        // Add console
        xml.append("    <console type='pty'>\n");
        xml.append("      <target type='serial' port='0'/>\n");
        xml.append("    </console>\n");

        xml.append("  </devices>\n");
        xml.append("</domain>\n");

        logger.debug("Generated Libvirt domain XML for VM {}: {}", vmId, xml);

        return xml.toString();
    }

    /**
     * Builds disk device XML.
     *
     * @param storage The storage specification
     * @param index Disk index
     * @return Disk device XML string
     */
    private static String buildDiskXml(StorageSpec storage, int index) {
        StringBuilder xml = new StringBuilder();
        xml.append("    <disk type='file' device='disk'>\n");
        xml.append("      <driver name='qemu' type='qcow2' cache='writeback'/>\n");
        xml.append("      <source file='/var/lib/libvirt/images/")
                .append(storage.getStorageClass()).append("/")
                .append(storage.getName()).append(".qcow2'/>\n");
        xml.append("      <target dev='vd").append(index).append("' bus='virtio'/>\n");
        xml.append("    </disk>\n");
        return xml.toString();
    }

    /**
     * Builds network interface XML.
     *
     * @param network The network specification
     * @param index Network index
     * @return Network interface XML string
     */
    private static String buildNetworkXml(NetworkSpec network, int index) {
        StringBuilder xml = new StringBuilder();
        xml.append("    <interface type='network'>\n");
        xml.append("      <source network='").append(network.getNetwork()).append("'/>\n");
        xml.append("      <model type='virtio'/>\n");
        xml.append("    </interface>\n");
        return xml.toString();
    }
}

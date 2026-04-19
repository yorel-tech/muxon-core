package com.krito.muxon.providers.libvirt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses Libvirt capabilities XML.
 *
 * <p>Extracts supported CPU types, disk types, network models,
 * and resource limits from Libvirt capabilities output.</p>
 */
public class LibvirtCapabilitiesParser {

    private static final Logger logger = LoggerFactory.getLogger(LibvirtCapabilitiesParser.class);

    /**
     * Parses Libvirt capabilities XML string.
     *
     * @param capabilitiesXml The capabilities XML string
     * @return Parsed capabilities object
     */
    public static LibvirtCapabilities parse(String capabilitiesXml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            InputSource is = new InputSource();
            is.setCharacterStream(new java.io.StringReader(capabilitiesXml));

            Document doc = builder.parse(is);
            Element root = doc.getDocumentElement();

            LibvirtCapabilities caps = new LibvirtCapabilities();

            // Extract CPU information
            Element host = getSingleChild(root, "host");
            if (host != null) {
                Element cpu = getSingleChild(host, "cpu");
                if (cpu != null) {
                    caps.setArch(cpu.getAttribute("arch"));
                    caps.setHypervisorType(host.getAttribute("type"));

                    // Extract CPU models
                    NodeList cpuModels = cpu.getElementsByTagName("model");
                    List<String> supportedCpuTypes = new ArrayList<>();
                    for (int i = 0; i < cpuModels.getLength(); i++) {
                        Element model = (Element) cpuModels.item(i);
                        if (model != null) {
                            String name = model.getAttribute("name");
                            if (name != null && !name.isEmpty()) {
                                supportedCpuTypes.add(name);
                            }
                        }
                    }
                    caps.setSupportedCpuTypes(supportedCpuTypes);

                    // Check for nested virtualization support
                    Element features = getSingleChild(cpu, "features");
                    if (features != null) {
                        NodeList featureList = features.getElementsByTagName("feature");
                        for (int i = 0; i < featureList.getLength(); i++) {
                            Element feature = (Element) featureList.item(i);
                            if (feature != null) {
                                String name = feature.getAttribute("name");
                                if ("vmx".equals(name)) {
                                    caps.setSupportsNestedVirtualization(true);
                                }
                            }
                        }
                    }
                }
            }

            // Extract memory information
            Element memory = getSingleChild(host, "memory");
            if (memory != null) {
                // Extract maximum memory in KiB
                String maxMem = memory.getAttribute("size");
                if (maxMem != null && !maxMem.isEmpty()) {
                    try {
                        long maxMemKb = Long.parseLong(maxMem);
                        caps.setMaxMemoryMb((int) (maxMemKb / 1024));
                    } catch (NumberFormatException e) {
                        logger.warn("Could not parse memory size: {}", maxMem);
                        caps.setMaxMemoryMb(1048576); // Default 1TB
                    }
                }
            }

            // Extract storage information
            Element devices = getSingleChild(host, "devices");
            if (devices != null) {
                Element disk = getSingleChild(devices, "disk");
                if (disk != null) {
                    // Extract disk types
                    NodeList diskTypes = disk.getElementsByTagName("type");
                    List<String> supportedDiskTypes = new ArrayList<>();
                    for (int i = 0; i < diskTypes.getLength(); i++) {
                        Element type = (Element) diskTypes.item(i);
                        if (type != null) {
                            String name = type.getAttribute("name");
                            if (name != null && !name.isEmpty()) {
                                supportedDiskTypes.add(name);
                            }
                        }
                    }
                    caps.setSupportedDiskTypes(supportedDiskTypes);

                    // Extract maximum storage
                    String maxStorage = disk.getAttribute("capacity");
                    if (maxStorage != null && !maxStorage.isEmpty()) {
                        try {
                            long maxStorageBytes = Long.parseLong(maxStorage);
                            caps.setMaxStorageGb((int) (maxStorageBytes / (1024L * 1024 * 1024L))); // Convert to GB
                        } catch (NumberFormatException e) {
                            logger.warn("Could not parse storage capacity: {}", maxStorage);
                            caps.setMaxStorageGb(10240); // Default 10TB
                        }
                    }
                }
            }

            // Extract network information
            Element network = getSingleChild(devices, "network");
            if (network != null) {
                // Extract network models
                NodeList networkModels = network.getElementsByTagName("model");
                List<String> supportedNetworkModels = new ArrayList<>();
                for (int i = 0; i < networkModels.getLength(); i++) {
                    Element model = (Element) networkModels.item(i);
                    if (model != null) {
                        String name = model.getAttribute("name");
                        if (name != null && !name.isEmpty()) {
                            supportedNetworkModels.add(name);
                        }
                    }
                }
                caps.setSupportedNetworkModels(supportedNetworkModels);

                // Check for virtio support
                NodeList features = network.getElementsByTagName("feature");
                for (int i = 0; i < features.getLength(); i++) {
                    Element feature = (Element) features.item(i);
                    if (feature != null) {
                        String name = feature.getAttribute("name");
                        if ("virtio".equals(name)) {
                            caps.setSupportsVirtio(true);
                        }
                    }
                }
            }

            // Extract maximum number of VMs
            String maxVms = host.getAttribute("maxVms");
            if (maxVms != null && !maxVms.isEmpty()) {
                try {
                    caps.setMaxVms(Integer.parseInt(maxVms));
                } catch (NumberFormatException e) {
                    logger.warn("Could not parse max VMs: {}", maxVms);
                    caps.setMaxVms(1000); // Default
                }
            }

            // Extract maximum number of CPUs
            String maxCpus = host.getAttribute("maxCpus");
            if (maxCpus != null && !maxCpus.isEmpty()) {
                try {
                    caps.setMaxCpuCores(Integer.parseInt(maxCpus));
                } catch (NumberFormatException e) {
                    logger.warn("Could not parse max CPUs: {}", maxCpus);
                    caps.setMaxCpuCores(256); // Default
                }
            }

            logger.debug("Parsed Libvirt capabilities: arch={}, hypervisorType={}, maxMemoryMb={}, maxStorageGb={}, maxVms={}, maxCpuCores={}, supportedCpuTypes={}, supportedDiskTypes={}, supportedNetworkModels={}",
                    caps.getArch(), caps.getHypervisorType(), caps.getMaxMemoryMb(), caps.getMaxStorageGb(),
                    caps.getMaxVms(), caps.getMaxCpuCores(), caps.getSupportedCpuTypes(),
                    caps.getSupportedDiskTypes(), caps.getSupportedNetworkModels());

            return caps;

        } catch (SAXException e) {
            logger.error("Failed to parse Libvirt capabilities XML: {}", e.getMessage(), e);
            // Return empty capabilities on error
            return new LibvirtCapabilities();
        } catch (Exception e) {
            logger.error("Error parsing Libvirt capabilities XML: {}", e.getMessage(), e);
            return new LibvirtCapabilities();
        }
    }

    /**
     * Gets a single child element by tag name.
     */
    private static Element getSingleChild(Element parent, String tagName) {
        NodeList children = parent.getElementsByTagName(tagName);
        if (children != null && children.getLength() > 0) {
            return (Element) children.item(0);
        }
        return null;
    }
}

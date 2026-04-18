package com.krito.muxon.core.orch.config;

import com.krito.muxon.core.providers.libvirt.LibvirtContentItemImporter;
import com.krito.muxon.core.providers.proxmox.ProxmoxContentItemImporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProviderContentImportConfiguration {

    @Bean
    public ProxmoxContentItemImporter proxmoxContentItemImporter(
            @Value("${muxon.instanceName:local}") String instanceName,
            @Value("${muxon.instanceId:1}") int instanceId) {
        return new ProxmoxContentItemImporter(instanceName, instanceId);
    }

    @Bean
    public LibvirtContentItemImporter libvirtContentItemImporter(
            @Value("${muxon.instanceName:local}") String instanceName,
            @Value("${muxon.instanceId:1}") int instanceId) {
        return new LibvirtContentItemImporter(instanceName, instanceId);
    }
}

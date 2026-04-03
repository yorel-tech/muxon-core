package com.onetattva.infron.core.orch.config;

import com.onetattva.infron.core.providers.libvirt.LibvirtContentItemImporter;
import com.onetattva.infron.core.providers.proxmox.ProxmoxContentItemImporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProviderContentImportConfiguration {

    @Bean
    public ProxmoxContentItemImporter proxmoxContentItemImporter(
            @Value("${infron.instanceName:local}") String instanceName,
            @Value("${infron.instanceId:1}") int instanceId) {
        return new ProxmoxContentItemImporter(instanceName, instanceId);
    }

    @Bean
    public LibvirtContentItemImporter libvirtContentItemImporter(
            @Value("${infron.instanceName:local}") String instanceName,
            @Value("${infron.instanceId:1}") int instanceId) {
        return new LibvirtContentItemImporter(instanceName, instanceId);
    }
}

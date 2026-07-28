package com.scal.muxon.orch.config;

import com.scal.muxon.providers.libvirt.LibvirtContentItemImporter;
import com.scal.muxon.providers.proxmox.ProxmoxContentItemImporter;
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

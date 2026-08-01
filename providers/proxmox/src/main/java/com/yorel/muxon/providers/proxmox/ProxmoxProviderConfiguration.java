/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.providers.proxmox;

import com.yorel.muxon.providers.storage.StorageDiscoveryProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for Proxmox provider module.
 *
 * <p>Registers the Proxmox storage discovery provider with the global registry so that core
 * services can discover storage from Proxmox clusters.
 */
@Configuration
public class ProxmoxProviderConfiguration {

  private static final Logger log = LoggerFactory.getLogger(ProxmoxProviderConfiguration.class);

  @Bean
  public ProxmoxStorageDiscoveryProvider proxmoxStorageDiscoveryProvider(
      StorageDiscoveryProviderRegistry registry) {

    ProxmoxStorageDiscoveryProvider provider = new ProxmoxStorageDiscoveryProvider();
    registry.register(provider);

    log.info("Registered Proxmox storage discovery provider");

    return provider;
  }

  @Bean
  public ProxmoxStorageUploader proxmoxStorageUploader() {
    return new ProxmoxStorageUploader();
  }
}

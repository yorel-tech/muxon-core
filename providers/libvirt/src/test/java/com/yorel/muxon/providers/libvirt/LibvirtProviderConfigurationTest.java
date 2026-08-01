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
package com.yorel.muxon.providers.libvirt;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yorel.muxon.providers.storage.StorageDiscoveryProviderRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    classes = {TestStorageDiscoveryRegistryConfig.class, LibvirtProviderConfiguration.class})
@ActiveProfiles("test")
public class LibvirtProviderConfigurationTest {

  @Autowired private StorageDiscoveryProviderRegistry registry;

  @Autowired private LibvirtStorageDiscoveryProvider provider;

  @Test
  public void testRegistryBeanInjection() {
    assertNotNull(registry, "StorageDiscoveryProviderRegistry should be injected");
    assertNotNull(provider, "LibvirtStorageDiscoveryProvider should be injected");

    // Verify the provider is registered
    assertNotNull(registry.getProvider("libvirt"), "Libvirt provider should be registered");
  }
}

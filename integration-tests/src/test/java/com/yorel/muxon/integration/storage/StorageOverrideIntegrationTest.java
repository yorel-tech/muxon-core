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
package com.yorel.muxon.integration.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yorel.muxon.CoreServicesApplication;
import com.yorel.muxon.db.model.ProviderStorageEntity;
import com.yorel.muxon.db.model.StorageClassEntity;
import com.yorel.muxon.db.model.StorageOverrideEntity;
import com.yorel.muxon.db.repository.ProviderStorageRepository;
import com.yorel.muxon.db.repository.StorageClassRepository;
import com.yorel.muxon.db.repository.StorageOverrideRepository;
import com.yorel.muxon.services.storage.scheduler.StorageOverrideResolver;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.DockerClientFactory;

/** Integration tests for storage overrides functionality. */
@SpringBootTest(classes = CoreServicesApplication.class)
@ActiveProfiles("test")
@Transactional
public class StorageOverrideIntegrationTest {

  @BeforeAll
  static void requireDocker() {
    if (!DockerClientFactory.instance().isDockerAvailable()) {
      throw new TestAbortedException(
          "Docker is not available; skipping SpringBootTest storage integration tests");
    }
  }

  @Autowired private StorageOverrideResolver overrideResolver;

  @Autowired private StorageOverrideRepository overrideRepository;

  @Autowired private StorageClassRepository storageClassRepository;

  @Autowired private ProviderStorageRepository providerStorageRepository;

  private StorageClassEntity testStorageClass;
  private UUID testProviderId;

  @BeforeEach
  public void setup() {
    testProviderId = UUID.randomUUID();

    // Create test storage class
    testStorageClass = new StorageClassEntity();
    testStorageClass.setName("test-class");
    testStorageClass.setType("block");
    testStorageClass.setTier("performance");
    storageClassRepository.save(testStorageClass);

    // Create test provider storage
    createProviderStorage("pool1", "rbd");
    createProviderStorage("pool2", "lvm");
    createProviderStorage("pool3", "zfs");
  }

  @Test
  public void testCreateOverride() {
    // When: Create an override
    StorageOverrideEntity override = new StorageOverrideEntity();
    override.setStorageClassName("test-class");
    override.setProviderType("libvirt");
    override.setProviderStorageNames(List.of("pool1", "pool2"));
    override.setPriority(100);

    StorageOverrideEntity saved = overrideRepository.save(override);

    // Then: Should be saved successfully
    assertNotNull(saved.getId());
    assertEquals("test-class", saved.getStorageClassName());
    assertEquals("libvirt", saved.getProviderType());
    assertEquals(2, saved.getProviderStorageNames().size());
  }

  @Test
  public void testOverrideUniqueness() {
    // Given: Create first override
    StorageOverrideEntity override1 = new StorageOverrideEntity();
    override1.setStorageClassName("test-class");
    override1.setProviderType("libvirt");
    override1.setProviderStorageNames(List.of("pool1"));
    overrideRepository.save(override1);

    // When/Then: Try to create duplicate (should fail due to unique constraint)
    StorageOverrideEntity override2 = new StorageOverrideEntity();
    override2.setStorageClassName("test-class");
    override2.setProviderType("libvirt");
    override2.setProviderStorageNames(List.of("pool2"));

    assertThrows(
        Exception.class,
        () -> {
          overrideRepository.save(override2);
          overrideRepository.flush();
        });
  }

  @Test
  public void testResolveOverride() {
    // Given: Create override
    StorageOverrideEntity override = new StorageOverrideEntity();
    override.setStorageClassName("test-class");
    override.setProviderType("libvirt");
    override.setProviderStorageNames(List.of("pool1", "pool2"));
    overrideRepository.save(override);

    // When: Resolve override
    List<ProviderStorageEntity> resolved =
        overrideResolver.resolveOverride("test-class", "libvirt");

    // Then: Should return mapped storage
    assertEquals(2, resolved.size());
    assertTrue(resolved.stream().anyMatch(s -> s.getName().equals("pool1")));
    assertTrue(resolved.stream().anyMatch(s -> s.getName().equals("pool2")));
  }

  @Test
  public void testResolveOverride_NoOverride() {
    // When: Resolve non-existent override
    List<ProviderStorageEntity> resolved =
        overrideResolver.resolveOverride("test-class", "proxmox");

    // Then: Should return empty list
    assertTrue(resolved.isEmpty());
  }

  @Test
  public void testResolveOverride_DisabledStorageExcluded() {
    // Given: Create override and disable one storage
    StorageOverrideEntity override = new StorageOverrideEntity();
    override.setStorageClassName("test-class");
    override.setProviderType("libvirt");
    override.setProviderStorageNames(List.of("pool1", "pool2"));
    overrideRepository.save(override);

    ProviderStorageEntity pool1 =
        providerStorageRepository.findByProviderTypeAndName("libvirt", "pool1").get(0);
    pool1.setEnabled(false);
    providerStorageRepository.save(pool1);

    // When: Resolve override
    List<ProviderStorageEntity> resolved =
        overrideResolver.resolveOverride("test-class", "libvirt");

    // Then: Should only return enabled storage
    assertEquals(1, resolved.size());
    assertEquals("pool2", resolved.get(0).getName());
  }

  @Test
  public void testHasOverride() {
    // Given: Create override
    StorageOverrideEntity override = new StorageOverrideEntity();
    override.setStorageClassName("test-class");
    override.setProviderType("libvirt");
    override.setProviderStorageNames(List.of("pool1"));
    overrideRepository.save(override);

    // When/Then: Check if override exists
    assertTrue(overrideResolver.hasOverride("test-class", "libvirt"));
    assertFalse(overrideResolver.hasOverride("test-class", "proxmox"));
    assertFalse(overrideResolver.hasOverride("other-class", "libvirt"));
  }

  @Test
  public void testGetOverrides() {
    // Given: Create multiple overrides
    StorageOverrideEntity override1 = new StorageOverrideEntity();
    override1.setStorageClassName("test-class");
    override1.setProviderType("libvirt");
    override1.setProviderStorageNames(List.of("pool1"));
    overrideRepository.save(override1);

    StorageOverrideEntity override2 = new StorageOverrideEntity();
    override2.setStorageClassName("test-class");
    override2.setProviderType("proxmox");
    override2.setProviderStorageNames(List.of("pool2"));
    overrideRepository.save(override2);

    // When: Get all overrides for storage class
    List<StorageOverrideEntity> overrides = overrideResolver.getOverrides("test-class");

    // Then: Should return both
    assertEquals(2, overrides.size());
  }

  @Test
  public void testUpdateOverride() {
    // Given: Create override
    StorageOverrideEntity override = new StorageOverrideEntity();
    override.setStorageClassName("test-class");
    override.setProviderType("libvirt");
    override.setProviderStorageNames(List.of("pool1"));
    override.setPriority(100);
    StorageOverrideEntity saved = overrideRepository.save(override);

    // When: Update override
    saved.setProviderStorageNames(List.of("pool1", "pool2", "pool3"));
    saved.setPriority(200);
    StorageOverrideEntity updated = overrideRepository.save(saved);

    // Then: Should be updated
    assertEquals(3, updated.getProviderStorageNames().size());
    assertEquals(200, updated.getPriority());
  }

  @Test
  public void testDeleteOverride() {
    // Given: Create override
    StorageOverrideEntity override = new StorageOverrideEntity();
    override.setStorageClassName("test-class");
    override.setProviderType("libvirt");
    override.setProviderStorageNames(List.of("pool1"));
    StorageOverrideEntity saved = overrideRepository.save(override);

    // When: Delete override
    overrideRepository.delete(saved);

    // Then: Should be deleted
    assertFalse(overrideResolver.hasOverride("test-class", "libvirt"));
  }

  private void createProviderStorage(String name, String storageType) {
    ProviderStorageEntity storage = new ProviderStorageEntity();
    storage.setProviderId(testProviderId);
    storage.setProviderType("libvirt");
    storage.setExternalId(name);
    storage.setName(name);
    storage.setStorageType(storageType);
    storage.setCapabilities(Map.of());
    storage.setMetrics(Map.of("free_gb", 100L, "total_gb", 200L));
    storage.setEnabled(true);
    providerStorageRepository.save(storage);
  }
}

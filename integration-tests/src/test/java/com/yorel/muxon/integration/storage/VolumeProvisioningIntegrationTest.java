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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yorel.muxon.CoreServicesApplication;
import com.yorel.muxon.db.model.ProviderStorageEntity;
import com.yorel.muxon.db.model.StorageClassEntity;
import com.yorel.muxon.db.model.VolumeEntity;
import com.yorel.muxon.db.repository.ProviderStorageRepository;
import com.yorel.muxon.db.repository.StorageClassRepository;
import com.yorel.muxon.db.repository.VolumeRepository;
import com.yorel.muxon.services.storage.VolumeService;
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

/** Integration tests for volume provisioning with capability-based storage scheduler. */
@SpringBootTest(classes = CoreServicesApplication.class)
@ActiveProfiles("test")
@Transactional
public class VolumeProvisioningIntegrationTest {

  @BeforeAll
  static void requireDocker() {
    if (!DockerClientFactory.instance().isDockerAvailable()) {
      throw new TestAbortedException(
          "Docker is not available; skipping SpringBootTest storage integration tests");
    }
  }

  @Autowired private VolumeService volumeService;

  @Autowired private VolumeRepository volumeRepository;

  @Autowired private StorageClassRepository storageClassRepository;

  @Autowired private ProviderStorageRepository providerStorageRepository;

  private UUID testProviderId;
  private UUID testWorkspaceId;
  private StorageClassEntity testStorageClass;

  @BeforeEach
  public void setup() {
    testProviderId = UUID.randomUUID();
    testWorkspaceId = UUID.randomUUID();

    // Create test storage class
    testStorageClass = new StorageClassEntity();
    testStorageClass.setName("fast-ssd");
    testStorageClass.setType("block");
    testStorageClass.setTier("performance");
    testStorageClass.setCapabilities(
        Map.of(
            "performance", "high",
            "media", "ssd",
            "shared", true));
    testStorageClass.setConstraints(Map.of("min_iops", 10000));
    storageClassRepository.save(testStorageClass);

    // Create test provider storage
    createProviderStorage(
        "ssd_pool",
        "rbd",
        Map.of(
            "performance", "high",
            "media", "ssd",
            "shared", true),
        Map.of(
            "free_gb", 500L,
            "total_gb", 1000L,
            "estimated_iops", 50000));
  }

  @Test
  public void testVolumeCreation_WithScheduler() {
    // Given: Volume request
    VolumeEntity volume = new VolumeEntity();
    volume.setName("test-volume");
    volume.setWorkspaceId(testWorkspaceId);
    volume.setProviderId(testProviderId);
    volume.setStorageClass("fast-ssd");
    volume.setSizeBytes(100L * 1024 * 1024 * 1024); // 100GB
    volume.setProviderVolumeId("vol-12345");

    // When: Create volume
    VolumeEntity created = volumeService.createVolume(volume);

    // Then: Should be created with scheduler metadata
    assertNotNull(created.getId());
    assertEquals("creating", created.getStatus());
    assertNotNull(created.getSelectedStorageId());
    assertNotNull(created.getSchedulerMetadata());

    // Verify scheduler metadata
    Map<String, Object> metadata = created.getSchedulerMetadata();
    assertNotNull(metadata.get("score"));
    assertEquals("ssd_pool", metadata.get("storage_name"));
    assertEquals("rbd", metadata.get("storage_type"));
  }

  @Test
  public void testVolumeCreation_SchedulerSelectsBestStorage() {
    // Given: Two matching storage pools with different scores
    createProviderStorage(
        "ssd_pool_2",
        "rbd",
        Map.of(
            "performance", "high",
            "media", "ssd",
            "shared", true),
        Map.of(
            "free_gb", 300L, // Less free space
            "total_gb", 1000L,
            "estimated_iops", 60000 // Higher IOPS
            ));

    VolumeEntity volume = new VolumeEntity();
    volume.setName("test-volume");
    volume.setWorkspaceId(testWorkspaceId);
    volume.setProviderId(testProviderId);
    volume.setStorageClass("fast-ssd");
    volume.setSizeBytes(50L * 1024 * 1024 * 1024); // 50GB
    volume.setProviderVolumeId("vol-12345");

    // When: Create volume
    VolumeEntity created = volumeService.createVolume(volume);

    // Then: Should select based on scoring
    assertNotNull(created.getSelectedStorageId());
    assertNotNull(created.getSchedulerMetadata());

    // Verify a storage was selected
    ProviderStorageEntity selectedStorage =
        providerStorageRepository.findById(created.getSelectedStorageId()).orElse(null);
    assertNotNull(selectedStorage);
    assertTrue(selectedStorage.getName().startsWith("ssd_pool"));
  }

  @Test
  public void testVolumeCreation_InsufficientCapacity() {
    // Given: Request more than available
    VolumeEntity volume = new VolumeEntity();
    volume.setName("huge-volume");
    volume.setWorkspaceId(testWorkspaceId);
    volume.setProviderId(testProviderId);
    volume.setStorageClass("fast-ssd");
    volume.setSizeBytes(600L * 1024 * 1024 * 1024); // 600GB (more than free)
    volume.setProviderVolumeId("vol-12345");

    // When/Then: Should fail
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          volumeService.createVolume(volume);
        });
  }

  @Test
  public void testVolumeCreation_UnsupportedStorageClass() {
    // Given: Non-existent storage class
    VolumeEntity volume = new VolumeEntity();
    volume.setName("test-volume");
    volume.setWorkspaceId(testWorkspaceId);
    volume.setProviderId(testProviderId);
    volume.setStorageClass("non-existent");
    volume.setSizeBytes(10L * 1024 * 1024 * 1024);
    volume.setProviderVolumeId("vol-12345");

    // When/Then: Should fail
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          volumeService.createVolume(volume);
        });
  }

  @Test
  public void testVolumeCreation_NoMatchingStorage() {
    // Given: Storage class with impossible requirements
    StorageClassEntity impossible = new StorageClassEntity();
    impossible.setName("impossible");
    impossible.setType("block");
    impossible.setTier("performance");
    impossible.setCapabilities(Map.of("performance", "ultra"));
    storageClassRepository.save(impossible);

    VolumeEntity volume = new VolumeEntity();
    volume.setName("test-volume");
    volume.setWorkspaceId(testWorkspaceId);
    volume.setProviderId(testProviderId);
    volume.setStorageClass("impossible");
    volume.setSizeBytes(10L * 1024 * 1024 * 1024);
    volume.setProviderVolumeId("vol-12345");

    // When/Then: Should fail
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          volumeService.createVolume(volume);
        });
  }

  @Test
  public void testVolumeLifecycle() {
    // Given: Created volume
    VolumeEntity volume = new VolumeEntity();
    volume.setName("lifecycle-test");
    volume.setWorkspaceId(testWorkspaceId);
    volume.setProviderId(testProviderId);
    volume.setStorageClass("fast-ssd");
    volume.setSizeBytes(50L * 1024 * 1024 * 1024);
    volume.setProviderVolumeId("vol-12345");

    VolumeEntity created = volumeService.createVolume(volume);
    UUID volumeId = created.getId();

    // When: Update status to available
    volumeService.updateVolumeStatus(volumeId, "available");

    // Then: Status should be updated
    VolumeEntity updated = volumeService.getVolume(volumeId).orElseThrow();
    assertEquals("available", updated.getStatus());

    // When: Delete volume
    volumeService.deleteVolume(volumeId);

    // Then: Should be soft deleted
    VolumeEntity deleted = volumeRepository.findById(volumeId).orElseThrow();
    assertNotNull(deleted.getDeletedAt());
    assertEquals("deleted", deleted.getStatus());
  }

  @Test
  public void testVolumeResize() {
    // Given: Created volume
    VolumeEntity volume = new VolumeEntity();
    volume.setName("resize-test");
    volume.setWorkspaceId(testWorkspaceId);
    volume.setProviderId(testProviderId);
    volume.setStorageClass("fast-ssd");
    volume.setSizeBytes(50L * 1024 * 1024 * 1024); // 50GB
    volume.setProviderVolumeId("vol-12345");

    VolumeEntity created = volumeService.createVolume(volume);
    UUID volumeId = created.getId();

    // When: Resize to larger size
    long newSize = 100L * 1024 * 1024 * 1024; // 100GB
    volumeService.resizeVolume(volumeId, newSize);

    // Then: Size should be updated
    VolumeEntity resized = volumeService.getVolume(volumeId).orElseThrow();
    assertEquals(newSize, resized.getSizeBytes());
    assertEquals("resizing", resized.getStatus());
  }

  @Test
  public void testVolumeResize_SmallerSize() {
    // Given: Created volume
    VolumeEntity volume = new VolumeEntity();
    volume.setName("resize-fail-test");
    volume.setWorkspaceId(testWorkspaceId);
    volume.setProviderId(testProviderId);
    volume.setStorageClass("fast-ssd");
    volume.setSizeBytes(100L * 1024 * 1024 * 1024);
    volume.setProviderVolumeId("vol-12345");

    VolumeEntity created = volumeService.createVolume(volume);
    UUID volumeId = created.getId();

    // When/Then: Try to resize to smaller size (should fail)
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          volumeService.resizeVolume(volumeId, 50L * 1024 * 1024 * 1024);
        });
  }

  @Test
  public void testSchedulerMetadataPreserved() {
    // Given: Volume created with scheduler
    VolumeEntity volume = new VolumeEntity();
    volume.setName("metadata-test");
    volume.setWorkspaceId(testWorkspaceId);
    volume.setProviderId(testProviderId);
    volume.setStorageClass("fast-ssd");
    volume.setSizeBytes(50L * 1024 * 1024 * 1024);
    volume.setProviderVolumeId("vol-12345");

    VolumeEntity created = volumeService.createVolume(volume);
    UUID volumeId = created.getId();
    Map<String, Object> originalMetadata = created.getSchedulerMetadata();

    // When: Update volume status
    volumeService.updateVolumeStatus(volumeId, "available");

    // Then: Scheduler metadata should be preserved
    VolumeEntity updated = volumeService.getVolume(volumeId).orElseThrow();
    assertEquals(originalMetadata, updated.getSchedulerMetadata());
    assertNotNull(updated.getSelectedStorageId());
  }

  private void createProviderStorage(
      String name,
      String storageType,
      Map<String, Object> capabilities,
      Map<String, Object> metrics) {
    ProviderStorageEntity storage = new ProviderStorageEntity();
    storage.setProviderId(testProviderId);
    storage.setProviderType("libvirt");
    storage.setExternalId(name);
    storage.setName(name);
    storage.setStorageType(storageType);
    storage.setCapabilities(capabilities);
    storage.setMetrics(metrics);
    storage.setEnabled(true);
    providerStorageRepository.save(storage);
  }
}

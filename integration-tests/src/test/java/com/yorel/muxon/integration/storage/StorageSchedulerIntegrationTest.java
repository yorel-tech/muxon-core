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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yorel.muxon.CoreServicesApplication;
import com.yorel.muxon.db.model.ProviderStorageEntity;
import com.yorel.muxon.db.model.StorageClassEntity;
import com.yorel.muxon.db.model.StorageOverrideEntity;
import com.yorel.muxon.db.repository.ProviderStorageRepository;
import com.yorel.muxon.db.repository.StorageCapabilityMappingRepository;
import com.yorel.muxon.db.repository.StorageClassRepository;
import com.yorel.muxon.db.repository.StorageOverrideRepository;
import com.yorel.muxon.services.storage.scheduler.StorageSchedulerService;
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

/** Integration tests for the capability-based storage scheduler. */
@SpringBootTest(classes = CoreServicesApplication.class)
@ActiveProfiles("test")
@Transactional
public class StorageSchedulerIntegrationTest {

  @BeforeAll
  static void requireDocker() {
    if (!DockerClientFactory.instance().isDockerAvailable()) {
      throw new TestAbortedException(
          "Docker is not available; skipping SpringBootTest storage integration tests");
    }
  }

  @Autowired private StorageSchedulerService schedulerService;

  @Autowired private StorageClassRepository storageClassRepository;

  @Autowired private ProviderStorageRepository providerStorageRepository;

  @Autowired private StorageOverrideRepository storageOverrideRepository;

  @Autowired private StorageCapabilityMappingRepository capabilityMappingRepository;

  private UUID testProviderId;
  private StorageClassEntity testStorageClass;

  @BeforeEach
  public void setup() {
    testProviderId = UUID.randomUUID();

    // Create test storage class with capabilities
    testStorageClass = new StorageClassEntity();
    testStorageClass.setName("fast-ssd");
    testStorageClass.setType("block");
    testStorageClass.setTier("performance");
    testStorageClass.setCapabilities(
        Map.of(
            "performance", "high",
            "media", "ssd",
            "shared", true,
            "redundancy", "replicated"));
    testStorageClass.setConstraints(
        Map.of(
            "min_iops", 10000,
            "max_latency_ms", 5));
    storageClassRepository.save(testStorageClass);

    // Create test provider storage entries
    createProviderStorage(
        "ssd_pool",
        "rbd",
        Map.of(
            "performance", "high",
            "media", "ssd",
            "shared", true,
            "redundancy", "replicated"),
        Map.of(
            "free_gb", 500L,
            "total_gb", 1000L,
            "estimated_iops", 50000));

    createProviderStorage(
        "hdd_pool",
        "dir",
        Map.of(
            "performance", "low",
            "media", "hdd",
            "shared", false,
            "redundancy", "none"),
        Map.of(
            "free_gb", 2000L,
            "total_gb", 4000L,
            "estimated_iops", 1000));

    createProviderStorage(
        "balanced_pool",
        "lvm-thin",
        Map.of(
            "performance", "medium",
            "media", "ssd",
            "shared", false,
            "redundancy", "none"),
        Map.of(
            "free_gb", 800L,
            "total_gb", 1000L,
            "estimated_iops", 10000));
  }

  @Test
  public void testScheduleStorage_MatchesCapabilities() {
    // When: Schedule storage for fast-ssd class
    StorageSchedulerService.SchedulingResult result =
        schedulerService.scheduleStorage(
            "fast-ssd", testProviderId, 100L * 1024 * 1024 * 1024 // 100GB
            );

    // Then: Should succeed and select ssd_pool
    assertTrue(result.isSuccess(), "Scheduling should succeed");
    assertNotNull(result.getSelectedStorage());
    assertEquals("ssd_pool", result.getSelectedStorage().getName());
    assertEquals("rbd", result.getSelectedStorage().getStorageType());
  }

  @Test
  public void testScheduleStorage_FiltersIncompatibleStorage() {
    // Given: Create a storage class requiring high performance
    StorageClassEntity highPerf = new StorageClassEntity();
    highPerf.setName("ultra-fast");
    highPerf.setType("block");
    highPerf.setTier("performance");
    highPerf.setCapabilities(Map.of("performance", "high"));
    highPerf.setConstraints(Map.of("min_iops", 40000));
    storageClassRepository.save(highPerf);

    // When: Schedule storage
    StorageSchedulerService.SchedulingResult result =
        schedulerService.scheduleStorage("ultra-fast", testProviderId, 50L * 1024 * 1024 * 1024);

    // Then: Should select ssd_pool (only one meeting IOPS requirement)
    assertTrue(result.isSuccess());
    assertEquals("ssd_pool", result.getSelectedStorage().getName());

    // Verify filtering worked
    Map<String, Object> metadata = result.getMetadata();
    assertTrue((Integer) metadata.get("candidates_total") > 1);
    assertEquals(1, metadata.get("candidates_filtered"));
  }

  @Test
  public void testScheduleStorage_InsufficientCapacity() {
    // When: Request more storage than available
    StorageSchedulerService.SchedulingResult result =
        schedulerService.scheduleStorage(
            "fast-ssd",
            testProviderId,
            600L * 1024 * 1024 * 1024 // 600GB (more than ssd_pool has free)
            );

    // Then: Should fail
    assertFalse(result.isSuccess());
    assertNotNull(result.getErrorMessage());
    assertTrue(result.getErrorMessage().contains("No storage matches"));
  }

  @Test
  public void testScheduleStorage_UsesScoring() {
    // Given: Two pools that match capabilities
    createProviderStorage(
        "ssd_pool_2",
        "rbd",
        Map.of(
            "performance", "high",
            "media", "ssd",
            "shared", true,
            "redundancy", "replicated"),
        Map.of(
            "free_gb", 300L, // Less free space
            "total_gb", 1000L,
            "estimated_iops", 60000 // Higher IOPS
            ));

    // When: Schedule storage
    StorageSchedulerService.SchedulingResult result =
        schedulerService.scheduleStorage("fast-ssd", testProviderId, 50L * 1024 * 1024 * 1024);

    // Then: Should succeed and select based on scoring
    assertTrue(result.isSuccess());
    assertNotNull(result.getSelectedStorage());

    // Verify scoring metadata
    Map<String, Object> metadata = result.getMetadata();
    assertNotNull(metadata.get("score"));
    assertTrue((Double) metadata.get("score") > 0);
  }

  @Test
  public void testScheduleStorage_WithOverride() {
    // Given: Create an override for fast-ssd to use balanced_pool
    StorageOverrideEntity override = new StorageOverrideEntity();
    override.setStorageClassName("fast-ssd");
    override.setProviderType("libvirt");
    override.setProviderStorageNames(List.of("balanced_pool"));
    override.setPriority(100);
    storageOverrideRepository.save(override);

    // When: Schedule storage
    StorageSchedulerService.SchedulingResult result =
        schedulerService.scheduleStorage("fast-ssd", testProviderId, 50L * 1024 * 1024 * 1024);

    // Then: Should use override and select balanced_pool
    assertTrue(result.isSuccess());
    assertEquals("balanced_pool", result.getSelectedStorage().getName());
  }

  @Test
  public void testScheduleStorage_NoMatchingStorage() {
    // Given: Storage class with impossible requirements
    StorageClassEntity impossible = new StorageClassEntity();
    impossible.setName("impossible");
    impossible.setType("block");
    impossible.setTier("performance");
    impossible.setCapabilities(Map.of("performance", "ultra"));
    storageClassRepository.save(impossible);

    // When: Schedule storage
    StorageSchedulerService.SchedulingResult result =
        schedulerService.scheduleStorage("impossible", testProviderId, 10L * 1024 * 1024 * 1024);

    // Then: Should fail with appropriate message
    assertFalse(result.isSuccess());
    assertNotNull(result.getErrorMessage());
  }

  @Test
  public void testGetAvailableStorage() {
    // When: Get all available storage for a storage class
    List<ProviderStorageEntity> available = schedulerService.getAvailableStorage("fast-ssd");

    // Then: Should return only matching storage
    assertNotNull(available);
    assertEquals(1, available.size());
    assertEquals("ssd_pool", available.get(0).getName());
  }

  @Test
  public void testScheduleStorage_DisabledStorageExcluded() {
    // Given: Disable ssd_pool
    ProviderStorageEntity ssdPool =
        providerStorageRepository.findByProviderIdAndName(testProviderId, "ssd_pool").get(0);
    ssdPool.setEnabled(false);
    providerStorageRepository.save(ssdPool);

    // When: Schedule storage
    StorageSchedulerService.SchedulingResult result =
        schedulerService.scheduleStorage("fast-ssd", testProviderId, 50L * 1024 * 1024 * 1024);

    // Then: Should fail (no enabled storage matches)
    assertFalse(result.isSuccess());
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

package com.onetattva.infron.tests.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.tests.InfronEnvironment;
import io.restassured.response.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Utility class for VM-related test operations.
 */
public class VmTestUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static InfronEnvironment environment;
    private static String baseUrl;
    private static String accessToken;

    public static void setup(InfronEnvironment env, String token) {
        environment = env;
        baseUrl = env.getCoreServicesUrl();
        accessToken = token;
    }

    /**
     * Create a VM with basic configuration
     */
    public static Response createBasicVm(String name, String tenantDatacenterGrantId) {
        try {
            VmCreateRequest vmRequest = createBasicVmRequest(name, tenantDatacenterGrantId);
            
            return given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(objectMapper.writeValueAsString(vmRequest))
                .when()
                .post(baseUrl + "/vms");
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize VM request", e);
        }
    }

    /**
     * Create a VM with custom configuration
     */
    public static Response createVm(String name, String tenantDatacenterGrantId, VmSpec spec) {
        try {
            VmCreateRequest vmRequest = new VmCreateRequest();
            vmRequest.setName(name);
            vmRequest.setSpec(spec);
            vmRequest.setTenantDatacenterGrantId(UUID.fromString(tenantDatacenterGrantId));
            
            return given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(objectMapper.writeValueAsString(vmRequest))
                .when()
                .post(baseUrl + "/vms");
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize VM request", e);
        }
    }

    /**
     * List all VMs
     */
    public static Response listVms() {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get(baseUrl + "/vms");
    }

    /**
     * Get VM by ID
     */
    public static Response getVm(String vmId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get(baseUrl + "/vms/" + vmId);
    }

    /**
     * Update VM
     */
    public static Response updateVm(String vmId, VmUpdateRequest updateRequest) {
        try {
            return given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(objectMapper.writeValueAsString(updateRequest))
                .when()
                .put(baseUrl + "/vms/" + vmId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize VM update request", e);
        }
    }

    /**
     * Delete VM
     */
    public static Response deleteVm(String vmId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .delete(baseUrl + "/vms/" + vmId);
    }

    /**
     * Start VM
     */
    public static Response startVm(String vmId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .when()
            .post(baseUrl + "/vms/" + vmId + "/start");
    }

    /**
     * Stop VM
     */
    public static Response stopVm(String vmId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .when()
            .post(baseUrl + "/vms/" + vmId + "/stop");
    }

    /**
     * Restart VM
     */
    public static Response restartVm(String vmId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .when()
            .post(baseUrl + "/vms/" + vmId + "/restart");
    }

    /**
     * Suspend VM
     */
    public static Response suspendVm(String vmId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .when()
            .post(baseUrl + "/vms/" + vmId + "/suspend");
    }

    /**
     * Resume VM
     */
    public static Response resumeVm(String vmId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .when()
            .post(baseUrl + "/vms/" + vmId + "/resume");
    }

    /**
     * Get VM console access
     */
    public static Response getVmConsole(String vmId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get(baseUrl + "/vms/" + vmId + "/console");
    }

    /**
     * Get VM resource usage
     */
    public static Response getVmResourceUsage(String vmId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get(baseUrl + "/vms/" + vmId + "/usage");
    }

    /**
     * Create a basic VM request with default configuration
     */
    private static VmCreateRequest createBasicVmRequest(String name, String tenantDatacenterGrantId) {
        VmCreateRequest vmRequest = new VmCreateRequest();
        vmRequest.setName(name);
        vmRequest.setTenantDatacenterGrantId(UUID.fromString(tenantDatacenterGrantId));
        
        VmSpec spec = createBasicVmSpec();
        vmRequest.setSpec(spec);
        
        return vmRequest;
    }

    /**
     * Create a basic VM spec with default configuration
     */
    public static VmSpec createBasicVmSpec() {
        VmSpec spec = new VmSpec();
        
        // CPU configuration
        CpuSpec cpu = new CpuSpec();
        cpu.setCores(2);
        cpu.setSockets(1);
        cpu.setThreads(1);
        cpu.setType(CpuSpec.TypeEnum.EMULATED);
        spec.setCpu(cpu);
        
        // Memory configuration
        MemorySpec memory = new MemorySpec();
        memory.setSizeMb(2048);
        memory.setOvercommitRatio(1.0);
        spec.setMemory(memory);
        
        // Storage configuration
        List<StorageSpec> storage = new ArrayList<>();
        StorageSpec rootStorage = new StorageSpec();
        rootStorage.setType(StorageSpec.TypeEnum.ROOT);
        rootStorage.setSizeGb(20);
        rootStorage.setStorageClass("ssd");
        rootStorage.setBootable(true);
        storage.add(rootStorage);
        spec.setStorage(storage);
        
        // Network configuration
        List<NetworkSpec> network = new ArrayList<>();
        NetworkSpec primaryNetwork = new NetworkSpec();
        primaryNetwork.setType(NetworkSpec.TypeEnum.PRIMARY);
        primaryNetwork.setNetwork("default");
        primaryNetwork.setIpAllocation(NetworkSpec.IpAllocationEnum.DHCP);
        network.add(primaryNetwork);
        spec.setNetwork(network);
        
        // OS configuration
        OsSpec os = new OsSpec();
        os.setType(OsSpec.TypeEnum.LINUX);
        os.setDistribution("ubuntu");
        os.setVersion("22.04");
        spec.setOs(os);
        
        return spec;
    }

    /**
     * Create a VM spec with high-performance configuration
     */
    public static VmSpec createHighPerformanceVmSpec() {
        VmSpec spec = new VmSpec();
        
        // CPU configuration
        CpuSpec cpu = new CpuSpec();
        cpu.setCores(8);
        cpu.setSockets(2);
        cpu.setThreads(2);
        cpu.setType(CpuSpec.TypeEnum.HOST_PASSTHROUGH);
        spec.setCpu(cpu);
        
        // Memory configuration
        MemorySpec memory = new MemorySpec();
        memory.setSizeMb(16384);
        memory.setOvercommitRatio(1.0);
        spec.setMemory(memory);
        
        // Storage configuration
        List<StorageSpec> storage = new ArrayList<>();
        StorageSpec rootStorage = new StorageSpec();
        rootStorage.setType(StorageSpec.TypeEnum.ROOT);
        rootStorage.setSizeGb(100);
        rootStorage.setStorageClass("nvme");
        rootStorage.setBootable(true);
        storage.add(rootStorage);
        
        StorageSpec dataStorage = new StorageSpec();
        dataStorage.setType(StorageSpec.TypeEnum.DATA);
        dataStorage.setSizeGb(500);
        dataStorage.setStorageClass("nvme");
        dataStorage.setBootable(false);
        storage.add(dataStorage);
        spec.setStorage(storage);
        
        // Network configuration
        List<NetworkSpec> network = new ArrayList<>();
        NetworkSpec primaryNetwork = new NetworkSpec();
        primaryNetwork.setType(NetworkSpec.TypeEnum.PRIMARY);
        primaryNetwork.setNetwork("high-performance");
        primaryNetwork.setIpAllocation(NetworkSpec.IpAllocationEnum.STATIC);
        network.add(primaryNetwork);
        spec.setNetwork(network);
        
        // OS configuration
        OsSpec os = new OsSpec();
        os.setType(OsSpec.TypeEnum.LINUX);
        os.setDistribution("ubuntu");
        os.setVersion("22.04");
        spec.setOs(os);
        
        return spec;
    }

    /**
     * Create a VM spec with Windows configuration
     */
    public static VmSpec createWindowsVmSpec() {
        VmSpec spec = new VmSpec();
        
        // CPU configuration
        CpuSpec cpu = new CpuSpec();
        cpu.setCores(4);
        cpu.setSockets(1);
        cpu.setThreads(2);
        cpu.setType(CpuSpec.TypeEnum.EMULATED);
        spec.setCpu(cpu);
        
        // Memory configuration
        MemorySpec memory = new MemorySpec();
        memory.setSizeMb(8192);
        memory.setOvercommitRatio(1.0);
        spec.setMemory(memory);
        
        // Storage configuration
        List<StorageSpec> storage = new ArrayList<>();
        StorageSpec rootStorage = new StorageSpec();
        rootStorage.setType(StorageSpec.TypeEnum.ROOT);
        rootStorage.setSizeGb(60);
        rootStorage.setStorageClass("ssd");
        rootStorage.setBootable(true);
        storage.add(rootStorage);
        spec.setStorage(storage);
        
        // Network configuration
        List<NetworkSpec> network = new ArrayList<>();
        NetworkSpec primaryNetwork = new NetworkSpec();
        primaryNetwork.setType(NetworkSpec.TypeEnum.PRIMARY);
        primaryNetwork.setNetwork("default");
        primaryNetwork.setIpAllocation(NetworkSpec.IpAllocationEnum.DHCP);
        network.add(primaryNetwork);
        spec.setNetwork(network);
        
        // OS configuration
        OsSpec os = new OsSpec();
        os.setType(OsSpec.TypeEnum.WINDOWS);
        os.setDistribution("windows-server");
        os.setVersion("2022");
        spec.setOs(os);
        
        return spec;
    }

    /**
     * Generate unique VM name for testing
     */
    public static String generateUniqueVmName() {
        return "test-vm-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Wait for VM to reach a specific status (with timeout)
     */
    public static boolean waitForVmStatus(String vmId, VmStatus expectedStatus, int timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeoutSeconds * 1000L;
        
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            Response response = getVm(vmId);
            if (response.getStatusCode() == 200) {
                VmStatus currentStatus = VmStatus.fromValue(response.jsonPath().getString("status"));
                if (currentStatus == expectedStatus) {
                    return true;
                }
            }
            
            try {
                Thread.sleep(2000); // Wait 2 seconds before checking again
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        return false;
    }

    /**
     * Create a VM update request with new description and metadata
     */
    public static VmUpdateRequest createVmUpdateRequest(String description, Map<String, Object> metadata) {
        VmUpdateRequest updateRequest = new VmUpdateRequest();
        updateRequest.setDescription(description);
        
        if (metadata != null) {
            Metadata meta = new Metadata();
            meta.setAdditionalProperties(metadata);
            updateRequest.setMetadata(meta);
        }
        
        return updateRequest;
    }
}
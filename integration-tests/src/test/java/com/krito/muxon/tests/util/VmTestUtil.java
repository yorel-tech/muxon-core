package com.krito.muxon.tests.util;

import tools.jackson.databind.ObjectMapper;
import com.krito.muxon.api.model.*;
import com.krito.muxon.tests.MuxonEnvironment;
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
    private static MuxonEnvironment environment;
    private static String baseUrl;
    private static String accessToken;
    private static String tenantId;

    public static void setup(MuxonEnvironment env, String token, String tenantIdValue) {
        environment = env;
        baseUrl = env.getCoreServicesUrl();
        accessToken = token;
        tenantId = tenantIdValue;
    }

    private static String vmsCollectionPath() {
        return baseUrl + "/tenants/" + tenantId + "/vms";
    }

    private static String vmItemPath(String vmId) {
        return vmsCollectionPath() + "/" + vmId;
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
                .post(vmsCollectionPath());
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
                .post(vmsCollectionPath());
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
            .get(vmsCollectionPath());
    }

    /**
     * Get VM by ID
     */
    public static Response getVm(String vmId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get(vmItemPath(vmId));
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
                .patch(vmItemPath(vmId));
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
            .delete(vmItemPath(vmId));
    }

    /**
     * Start VM
     */
    public static Response startVm(String vmId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .when()
            .post(vmItemPath(vmId) + "/start");
    }

    /**
     * Stop VM
     */
    public static Response stopVm(String vmId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .when()
            .post(vmItemPath(vmId) + "/stop");
    }

    /**
     * Restart VM
     */
    public static Response restartVm(String vmId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .when()
            .post(vmItemPath(vmId) + "/restart");
    }

    /**
     * Suspend VM
     */
    public static Response suspendVm(String vmId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .when()
            .post(vmItemPath(vmId) + "/suspend");
    }

    /**
     * Resume VM
     */
    public static Response resumeVm(String vmId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .when()
            .post(vmItemPath(vmId) + "/resume");
    }

    /**
     * Get VM console access
     */
    public static Response getVmConsole(String vmId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get(vmItemPath(vmId) + "/console");
    }

    /**
     * Get VM resource usage
     */
    public static Response getVmResourceUsage(String vmId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get(vmItemPath(vmId) + "/usage");
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
     * aligned with the current VmSpec schema (compute/storage/network/os).
     */
    public static VmSpec createBasicVmSpec() {
        VmSpec spec = new VmSpec();

        // Compute configuration
        ComputeSpec compute = new ComputeSpec();
        compute.setCpus(2);
        compute.setMemorySizeMb(2048);
        spec.setCompute(compute);

        // Storage configuration
        DiskSpec rootDisk = new DiskSpec();
        rootDisk.setSizeMb(20 * 1024); // 20 GB
        rootDisk.setStorageClass("ssd");

        StorageSpec storage = new StorageSpec();
        List<DiskSpec> disks = new ArrayList<>();
        disks.add(rootDisk);
        storage.setDisks(disks);
        storage.setVmStorageClass("ssd");
        spec.setStorage(storage);

        // Network configuration
        NicSpec primaryNic = new NicSpec();
        primaryNic.setIsPrimary(true);
        primaryNic.setNetwork("default");
        primaryNic.setIpAllocation(NicSpec.IpAllocationEnum.DHCP);

        NetworkSpec network = new NetworkSpec();
        List<NicSpec> nics = new ArrayList<>();
        nics.add(primaryNic);
        network.setNics(nics);
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
     * aligned with the current VmSpec schema.
     */
    public static VmSpec createHighPerformanceVmSpec() {
        VmSpec spec = new VmSpec();

        // Compute configuration
        ComputeSpec compute = new ComputeSpec();
        compute.setCpus(8);
        compute.setMemorySizeMb(16384);
        spec.setCompute(compute);

        // Storage configuration
        DiskSpec rootDisk = new DiskSpec();
        rootDisk.setSizeMb(100 * 1024); // 100 GB
        rootDisk.setStorageClass("nvme");

        DiskSpec dataDisk = new DiskSpec();
        dataDisk.setSizeMb(500 * 1024); // 500 GB
        dataDisk.setStorageClass("nvme");

        StorageSpec storage = new StorageSpec();
        List<DiskSpec> disks = new ArrayList<>();
        disks.add(rootDisk);
        disks.add(dataDisk);
        storage.setDisks(disks);
        storage.setVmStorageClass("nvme");
        spec.setStorage(storage);

        // Network configuration
        NicSpec primaryNic = new NicSpec();
        primaryNic.setIsPrimary(true);
        primaryNic.setNetwork("high-performance");
        primaryNic.setIpAllocation(NicSpec.IpAllocationEnum.STATIC);

        NetworkSpec network = new NetworkSpec();
        List<NicSpec> nics = new ArrayList<>();
        nics.add(primaryNic);
        network.setNics(nics);
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
     * aligned with the current VmSpec schema.
     */
    public static VmSpec createWindowsVmSpec() {
        VmSpec spec = new VmSpec();

        // Compute configuration
        ComputeSpec compute = new ComputeSpec();
        compute.setCpus(4);
        compute.setMemorySizeMb(8192);
        spec.setCompute(compute);

        // Storage configuration
        DiskSpec rootDisk = new DiskSpec();
        rootDisk.setSizeMb(60 * 1024); // 60 GB
        rootDisk.setStorageClass("ssd");

        StorageSpec storage = new StorageSpec();
        List<DiskSpec> disks = new ArrayList<>();
        disks.add(rootDisk);
        storage.setDisks(disks);
        storage.setVmStorageClass("ssd");
        spec.setStorage(storage);

        // Network configuration
        NicSpec primaryNic = new NicSpec();
        primaryNic.setIsPrimary(true);
        primaryNic.setNetwork("default");
        primaryNic.setIpAllocation(NicSpec.IpAllocationEnum.DHCP);

        NetworkSpec network = new NetworkSpec();
        List<NicSpec> nics = new ArrayList<>();
        nics.add(primaryNic);
        network.setNics(nics);
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
    public static VmUpdateRequest createVmUpdateRequest(String description, Map<String, String> metadata) {
        VmUpdateRequest updateRequest = new VmUpdateRequest();
        updateRequest.setDescription(description);
        
        if (metadata != null) {
            updateRequest.setMetadata(metadata);
        }
        
        return updateRequest;
    }
}
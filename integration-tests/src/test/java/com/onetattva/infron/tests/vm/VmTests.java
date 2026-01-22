package com.onetattva.infron.tests.vm;

import com.onetattva.infron.api.model.*;
import com.onetattva.infron.tests.BaseIntegrationTest;
import com.onetattva.infron.tests.util.ProviderTestUtil;
import com.onetattva.infron.tests.util.VmTestUtil;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for VM functionality.
 * Tests VM lifecycle operations including creation, management, and deletion.
 */
public class VmTests extends BaseIntegrationTest {

    private static String tenantId;
    private static String datacenterId;
    private static String tenantDatacenterGrantId;

    @BeforeAll
    public static void setup() {
        // Get access token
        accessToken = getAccessToken();

        // Setup utility classes
        ProviderTestUtil.setup(environment, accessToken);
        VmTestUtil.setup(environment, accessToken);

        // Get default tenant ID
        Response tenantResponse = given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get("/tenants");

        assertStatusCode(tenantResponse, 200);
        tenantId = tenantResponse.jsonPath().getString("items[0].id");

        // Create a datacenter for VM tests
        String datacenterName = ProviderTestUtil.generateUniqueDatacenterName();
        Response datacenterResponse = ProviderTestUtil.createMockDatacenter(
            datacenterName,
            "Datacenter for VM tests"
        );
        assertStatusCode(datacenterResponse, 201);
        datacenterId = datacenterResponse.jsonPath().getString("id");

        // Create tenant datacenter grant
        Response grantResponse = ProviderTestUtil.createTenantDatacenterGrant(tenantId, datacenterId);
        assertStatusCode(grantResponse, 201);
        tenantDatacenterGrantId = grantResponse.jsonPath().getString("id");
    }

    @Test
    public void testCreateBasicVm() {
        String vmName = VmTestUtil.generateUniqueVmName();

        Response response = VmTestUtil.createBasicVm(vmName, tenantDatacenterGrantId);

        assertStatusCode(response, 201);
        response.then()
            .body("id", notNullValue())
            .body("name", equalTo(vmName))
            .body("status", equalTo("PENDING"))
            .body("tenantDatacenterGrantId", equalTo(tenantDatacenterGrantId));

        // Cleanup
        String vmId = response.jsonPath().getString("id");
        assertStatusCode(VmTestUtil.deleteVm(vmId), 204);
    }

    @Test
    public void testCreateHighPerformanceVm() {
        String vmName = VmTestUtil.generateUniqueVmName();
        VmSpec spec = VmTestUtil.createHighPerformanceVmSpec();

        Response response = VmTestUtil.createVm(vmName, tenantDatacenterGrantId, spec);

        assertStatusCode(response, 201);
        response.then()
            .body("id", notNullValue())
            .body("name", equalTo(vmName))
            .body("status", equalTo("PENDING"))
            .body("spec.cpu.cores", equalTo(8))
            .body("spec.cpu.sockets", equalTo(2))
            .body("spec.cpu.threads", equalTo(2))
            .body("spec.memory.sizeMb", equalTo(16384))
            .body("spec.storage", hasSize(2))
            .body("spec.storage[0].type", equalTo("root"))
            .body("spec.storage[0].sizeGb", equalTo(100))
            .body("spec.storage[1].type", equalTo("data"))
            .body("spec.storage[1].sizeGb", equalTo(500));

        // Cleanup
        String vmId = response.jsonPath().getString("id");
        assertStatusCode(VmTestUtil.deleteVm(vmId), 204);
    }

    @Test
    public void testCreateWindowsVm() {
        String vmName = VmTestUtil.generateUniqueVmName();
        VmSpec spec = VmTestUtil.createWindowsVmSpec();

        Response response = VmTestUtil.createVm(vmName, tenantDatacenterGrantId, spec);

        assertStatusCode(response, 201);
        response.then()
            .body("id", notNullValue())
            .body("name", equalTo(vmName))
            .body("status", equalTo("PENDING"))
            .body("spec.os.type", equalTo("windows"))
            .body("spec.os.distribution", equalTo("windows-server"))
            .body("spec.os.version", equalTo("2022"))
            .body("spec.memory.sizeMb", equalTo(8192));

        // Cleanup
        String vmId = response.jsonPath().getString("id");
        assertStatusCode(VmTestUtil.deleteVm(vmId), 204);
    }

    @Test
    public void testListVms() {
        // Create a VM first
        String vmName = VmTestUtil.generateUniqueVmName();
        Response createResponse = VmTestUtil.createBasicVm(vmName, tenantDatacenterGrantId);
        assertStatusCode(createResponse, 201);
        String vmId = createResponse.jsonPath().getString("id");

        // List VMs
        Response listResponse = VmTestUtil.listVms();
        assertStatusCode(listResponse, 200);
        listResponse.then()
            .body("total", greaterThanOrEqualTo(1))
            .body("items", notNullValue())
            .body("items.find { it.id == '%s' }.name".formatted(vmId), equalTo(vmName));

        // Cleanup
        assertStatusCode(VmTestUtil.deleteVm(vmId), 204);
    }

    @Test
    public void testGetVm() {
        // Create a VM first
        String vmName = VmTestUtil.generateUniqueVmName();
        Response createResponse = VmTestUtil.createBasicVm(vmName, tenantDatacenterGrantId);
        assertStatusCode(createResponse, 201);
        String vmId = createResponse.jsonPath().getString("id");

        // Get the VM
        Response getResponse = VmTestUtil.getVm(vmId);
        assertStatusCode(getResponse, 200);
        getResponse.then()
            .body("id", equalTo(vmId))
            .body("name", equalTo(vmName))
            .body("status", equalTo("PENDING"))
            .body("tenantDatacenterGrantId", equalTo(tenantDatacenterGrantId));

        // Cleanup
        assertStatusCode(VmTestUtil.deleteVm(vmId), 204);
    }

    @Test
    public void testUpdateVm() {
        // Create a VM first
        String vmName = VmTestUtil.generateUniqueVmName();
        Response createResponse = VmTestUtil.createBasicVm(vmName, tenantDatacenterGrantId);
        assertStatusCode(createResponse, 201);
        String vmId = createResponse.jsonPath().getString("id");

        // Update the VM
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("environment", "test");
        metadata.put("owner", "integration-test");
        
        VmUpdateRequest updateRequest = VmTestUtil.createVmUpdateRequest(
            "Updated description for test VM",
            metadata
        );

        Response updateResponse = VmTestUtil.updateVm(vmId, updateRequest);
        assertStatusCode(updateResponse, 200);
        updateResponse.then()
            .body("description", equalTo("Updated description for test VM"))
            .body("metadata.environment", equalTo("test"))
            .body("metadata.owner", equalTo("integration-test"));

        // Verify update
        Response verifyResponse = VmTestUtil.getVm(vmId);
        assertStatusCode(verifyResponse, 200);
        verifyResponse.then()
            .body("description", equalTo("Updated description for test VM"))
            .body("metadata.environment", equalTo("test"));

        // Cleanup
        assertStatusCode(VmTestUtil.deleteVm(vmId), 204);
    }

    @Test
    public void testVmPowerOperations() {
        // Create a VM first
        String vmName = VmTestUtil.generateUniqueVmName();
        Response createResponse = VmTestUtil.createBasicVm(vmName, tenantDatacenterGrantId);
        assertStatusCode(createResponse, 201);
        String vmId = createResponse.jsonPath().getString("id");

        // Start VM
        Response startResponse = VmTestUtil.startVm(vmId);
        assertStatusCode(startResponse, 200);
        startResponse.then()
            .body("message", containsString("started"))
            .body("operation_id", notNullValue());

        // Wait for VM to be active (with timeout)
        boolean isActive = VmTestUtil.waitForVmStatus(vmId, VmStatus.ACTIVE, 60);
        if (isActive) {
            // Stop VM
            Response stopResponse = VmTestUtil.stopVm(vmId);
            assertStatusCode(stopResponse, 200);
            stopResponse.then()
                .body("message", containsString("stopped"));
        }

        // Cleanup
        assertStatusCode(VmTestUtil.deleteVm(vmId), 204);
    }

    @Test
    public void testVmSuspendResumeOperations() {
        // Create a VM first
        String vmName = VmTestUtil.generateUniqueVmName();
        Response createResponse = VmTestUtil.createBasicVm(vmName, tenantDatacenterGrantId);
        assertStatusCode(createResponse, 201);
        String vmId = createResponse.jsonPath().getString("id");

        // Start VM first
        Response startResponse = VmTestUtil.startVm(vmId);
        assertStatusCode(startResponse, 200);

        // Wait for VM to be active
        boolean isActive = VmTestUtil.waitForVmStatus(vmId, VmStatus.ACTIVE, 60);
        if (isActive) {
            // Suspend VM
            Response suspendResponse = VmTestUtil.suspendVm(vmId);
            assertStatusCode(suspendResponse, 200);
            suspendResponse.then()
                .body("message", containsString("suspended"));

            // Resume VM
            Response resumeResponse = VmTestUtil.resumeVm(vmId);
            assertStatusCode(resumeResponse, 200);
            resumeResponse.then()
                .body("message", containsString("resumed"));
        }

        // Cleanup
        assertStatusCode(VmTestUtil.deleteVm(vmId), 204);
    }

    @Test
    public void testVmRestartOperation() {
        // Create a VM first
        String vmName = VmTestUtil.generateUniqueVmName();
        Response createResponse = VmTestUtil.createBasicVm(vmName, tenantDatacenterGrantId);
        assertStatusCode(createResponse, 201);
        String vmId = createResponse.jsonPath().getString("id");

        // Start VM first
        Response startResponse = VmTestUtil.startVm(vmId);
        assertStatusCode(startResponse, 200);

        // Wait for VM to be active
        boolean isActive = VmTestUtil.waitForVmStatus(vmId, VmStatus.ACTIVE, 60);
        if (isActive) {
            // Restart VM
            Response restartResponse = VmTestUtil.restartVm(vmId);
            assertStatusCode(restartResponse, 200);
            restartResponse.then()
                .body("message", containsString("restarted"))
                .body("operation_id", notNullValue());
        }

        // Cleanup
        assertStatusCode(VmTestUtil.deleteVm(vmId), 204);
    }

    @Test
    public void testVmConsoleAccess() {
        // Create a VM first
        String vmName = VmTestUtil.generateUniqueVmName();
        Response createResponse = VmTestUtil.createBasicVm(vmName, tenantDatacenterGrantId);
        assertStatusCode(createResponse, 201);
        String vmId = createResponse.jsonPath().getString("id");

        // Get VM console
        Response consoleResponse = VmTestUtil.getVmConsole(vmId);
        // Console access might not be available for all VM states
        // Accept 200 or 404 (not available)
        consoleResponse.then().statusCode(anyOf(equalTo(200), equalTo(404)));

        if (consoleResponse.getStatusCode() == 200) {
            consoleResponse.then()
                .body("url", notNullValue())
                .body("token", notNullValue())
                .body("expires_at", notNullValue());
        }

        // Cleanup
        assertStatusCode(VmTestUtil.deleteVm(vmId), 204);
    }

    @Test
    public void testVmResourceUsage() {
        // Create a VM first
        String vmName = VmTestUtil.generateUniqueVmName();
        Response createResponse = VmTestUtil.createBasicVm(vmName, tenantDatacenterGrantId);
        assertStatusCode(createResponse, 201);
        String vmId = createResponse.jsonPath().getString("id");

        // Get VM resource usage
        Response usageResponse = VmTestUtil.getVmResourceUsage(vmId);
        // Resource usage might not be available for all VM states
        // Accept 200 or 404 (not available)
        usageResponse.then().statusCode(anyOf(equalTo(200), equalTo(404)));

        if (usageResponse.getStatusCode() == 200) {
            usageResponse.then()
                .body("cpu", notNullValue())
                .body("memory", notNullValue());
        }

        // Cleanup
        assertStatusCode(VmTestUtil.deleteVm(vmId), 204);
    }

    // Negative test cases

    @Test
    public void testCreateVmWithInvalidName() {
        // Try to create a VM with invalid name (empty)
        Response response = VmTestUtil.createBasicVm("", tenantDatacenterGrantId);
        assertStatusCode(response, 400);
        response.then()
            .body("error", notNullValue())
            .body("message", containsString("name"));
    }

    @Test
    public void testCreateVmWithInvalidTenantDatacenterGrant() {
        String vmName = VmTestUtil.generateUniqueVmName();
        String invalidGrantId = UUID.randomUUID().toString();

        // Try to create a VM with invalid tenant datacenter grant
        Response response = VmTestUtil.createBasicVm(vmName, invalidGrantId);
        assertStatusCode(response, 404);
        response.then()
            .body("error", notNullValue())
            .body("message", containsString("tenant datacenter grant"));
    }

    @Test
    public void testCreateVmWithInvalidSpec() {
        String vmName = VmTestUtil.generateUniqueVmName();
        
        // Create a VM spec with invalid configuration (0 CPU cores)
        VmSpec invalidSpec = VmTestUtil.createBasicVmSpec();
        invalidSpec.getCpu().setCores(0);

        Response response = VmTestUtil.createVm(vmName, tenantDatacenterGrantId, invalidSpec);
        assertStatusCode(response, 400);
        response.then()
            .body("error", notNullValue())
            .body("message", containsString("cpu"));
    }

    @Test
    public void testGetNonExistentVm() {
        String nonExistentVmId = UUID.randomUUID().toString();

        Response response = VmTestUtil.getVm(nonExistentVmId);
        assertStatusCode(response, 404);
        response.then()
            .body("error", notNullValue())
            .body("message", containsString("VM"));
    }

    @Test
    public void testUpdateNonExistentVm() {
        String nonExistentVmId = UUID.randomUUID().toString();
        VmUpdateRequest updateRequest = VmTestUtil.createVmUpdateRequest("Test update", null);

        Response response = VmTestUtil.updateVm(nonExistentVmId, updateRequest);
        assertStatusCode(response, 404);
        response.then()
            .body("error", notNullValue())
            .body("message", containsString("VM"));
    }

    @Test
    public void testDeleteNonExistentVm() {
        String nonExistentVmId = UUID.randomUUID().toString();

        Response response = VmTestUtil.deleteVm(nonExistentVmId);
        assertStatusCode(response, 404);
        response.then()
            .body("error", notNullValue())
            .body("message", containsString("VM"));
    }

    @Test
    public void testStartNonExistentVm() {
        String nonExistentVmId = UUID.randomUUID().toString();

        Response response = VmTestUtil.startVm(nonExistentVmId);
        assertStatusCode(response, 404);
        response.then()
            .body("error", notNullValue())
            .body("message", containsString("VM"));
    }

    @Test
    public void testStopNonExistentVm() {
        String nonExistentVmId = UUID.randomUUID().toString();

        Response response = VmTestUtil.stopVm(nonExistentVmId);
        assertStatusCode(response, 404);
        response.then()
            .body("error", notNullValue())
            .body("message", containsString("VM"));
    }

    @Test
    public void testVmOperationsWithoutAuthentication() {
        String vmName = VmTestUtil.generateUniqueVmName();

        // Try to create VM without authentication
        Response response = given()
            .contentType("application/json")
            .body("{\"name\":\"" + vmName + "\"}")
            .when()
            .post("/vms");
        
        assertStatusCode(response, 401);
        response.then()
            .body("error", notNullValue());
    }

    @Test
    public void testCreateVmWithExcessiveResources() {
        String vmName = VmTestUtil.generateUniqueVmName();
        
        // Create a VM spec with excessive resources
        VmSpec excessiveSpec = new VmSpec();
        
        // CPU with too many cores
        CpuSpec cpu = new CpuSpec();
        cpu.setCores(1000); // Excessive number of cores
        cpu.setSockets(1);
        cpu.setThreads(1);
        excessiveSpec.setCpu(cpu);
        
        // Memory with too much size
        MemorySpec memory = new MemorySpec();
        memory.setSizeMb(10000000); // Excessive memory size
        excessiveSpec.setMemory(memory);
        
        // Add minimal storage to make it valid
        List<StorageSpec> storage = List.of(
            createMinimalStorageSpec()
        );
        excessiveSpec.setStorage(storage);
        
        // Add minimal network
        List<NetworkSpec> network = List.of(
            createMinimalNetworkSpec()
        );
        excessiveSpec.setNetwork(network);
        
        // Add minimal OS
        OsSpec os = new OsSpec();
        os.setType(OsSpec.TypeEnum.LINUX);
        excessiveSpec.setOs(os);

        Response response = VmTestUtil.createVm(vmName, tenantDatacenterGrantId, excessiveSpec);
        assertStatusCode(response, 400);
        response.then()
            .body("error", notNullValue())
            .body("message", anyOf(containsString("cpu"), containsString("memory")));
    }

    private static StorageSpec createMinimalStorageSpec() {
        StorageSpec storage = new StorageSpec();
        storage.setType(StorageSpec.TypeEnum.ROOT);
        storage.setSizeGb(10);
        storage.setBootable(true);
        return storage;
    }

    private static NetworkSpec createMinimalNetworkSpec() {
        NetworkSpec network = new NetworkSpec();
        network.setType(NetworkSpec.TypeEnum.PRIMARY);
        network.setNetwork("default");
        network.setIpAllocation(NetworkSpec.IpAllocationEnum.DHCP);
        return network;
    }
}
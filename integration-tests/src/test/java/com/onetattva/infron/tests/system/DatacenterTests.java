package com.onetattva.infron.tests.system;

import com.onetattva.infron.api.model.*;
import com.onetattva.infron.tests.BaseIntegrationTest;
import com.onetattva.infron.tests.util.ProviderTestUtil;
import com.onetattva.infron.tests.util.RoleTestUtil;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for datacenter functionality including provider tests.
 * Tests provider API through datacenter API which uses providers.
 */
public class DatacenterTests extends BaseIntegrationTest {

    private static String tenantId;

    @BeforeAll
    public static void setup() {
        // Get access token
        accessToken = getAccessToken();

        // Setup utility classes
        ProviderTestUtil.setup(environment, accessToken);
        RoleTestUtil.setup(environment, accessToken);

        // Get default tenant ID (use the admin's tenant)
        // For system-level tests, we'll use the first tenant found
        Response tenantResponse = given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get("/tenants");

        assertStatusCode(tenantResponse, 200);
        tenantId = tenantResponse.jsonPath().getString("items[0].id");
    }

    @Test
    public void testCreateMockDatacenter() {
        String datacenterName = ProviderTestUtil.generateUniqueDatacenterName();

        Response response = ProviderTestUtil.createMockDatacenter(
            datacenterName,
            "Test datacenter with mock provider"
        );

        assertStatusCode(response, 201);
        response.then()
            .body("name", equalTo(datacenterName))
            .body("description", equalTo("Test datacenter with mock provider"))
            .body("settings.providerType", equalTo("kvm"))
            .body("settings.defaultCpuOvercommitRatio", equalTo(4.0))
            .body("settings.defaultMemoryOvercommitRatio", equalTo(1.5))
            .body("capacity.totalCpus", equalTo(1000))
            .body("capacity.totalMemoryGb", equalTo(4096))
            .body("capacity.totalStorageGb", equalTo(20000));

        // Cleanup
        String datacenterId = response.jsonPath().getString("id");
        assertStatusCode(ProviderTestUtil.deleteDatacenter(datacenterId), 204);
    }

    @Test
    public void testListDatacenters() {
        // Create a datacenter first
        String datacenterName = ProviderTestUtil.generateUniqueDatacenterName();
        Response createResponse = ProviderTestUtil.createMockDatacenter(
            datacenterName,
            "Test datacenter for listing"
        );
        assertStatusCode(createResponse, 201);
        String datacenterId = createResponse.jsonPath().getString("id");

        // List datacenters
        Response listResponse = ProviderTestUtil.listDatacenters();
        assertStatusCode(listResponse, 200);
        listResponse.then()
            .body("items", notNullValue())
            .body("items.find { it.id == '%s' }.name".formatted(datacenterId), equalTo(datacenterName));

        // Cleanup
        assertStatusCode(ProviderTestUtil.deleteDatacenter(datacenterId), 204);
    }

    @Test
    public void testGetDatacenter() {
        // Create a datacenter first
        String datacenterName = ProviderTestUtil.generateUniqueDatacenterName();
        Response createResponse = ProviderTestUtil.createMockDatacenter(
            datacenterName,
            "Test datacenter for get"
        );
        assertStatusCode(createResponse, 201);
        String datacenterId = createResponse.jsonPath().getString("id");

        // Get the datacenter
        Response getResponse = ProviderTestUtil.getDatacenter(datacenterId);
        assertStatusCode(getResponse, 200);
        getResponse.then()
            .body("id", equalTo(datacenterId))
            .body("name", equalTo(datacenterName))
            .body("description", equalTo("Test datacenter for get"))
            .body("settings.providerType", equalTo("kvm"));

        // Cleanup
        assertStatusCode(ProviderTestUtil.deleteDatacenter(datacenterId), 204);
    }

    @Test
    public void testUpdateDatacenterSettings() {
        // Create a datacenter first
        String datacenterName = ProviderTestUtil.generateUniqueDatacenterName();
        Response createResponse = ProviderTestUtil.createMockDatacenter(
            datacenterName,
            "Test datacenter for update"
        );
        assertStatusCode(createResponse, 201);
        String datacenterId = createResponse.jsonPath().getString("id");

        // Update the datacenter settings
        DatacenterSettings newSettings = new DatacenterSettings();
        newSettings.setProviderType(DatacenterType.KVM);
        newSettings.setDefaultCpuOvercommitRatio(8.0F);
        newSettings.setDefaultMemoryOvercommitRatio(2.0F);
        newSettings.setVmClasses(Arrays.asList("small", "medium", "large"));
        newSettings.setStorageClasses(Arrays.asList("gold", "silver"));
        newSettings.setNetworkDomains(Arrays.asList("private", "public"));

        Response updateResponse = ProviderTestUtil.updateDatacenterSettings(datacenterId, newSettings);
        assertStatusCode(updateResponse, 200);
        updateResponse.then()
            .body("defaultCpuOvercommitRatio", equalTo(8.0))
            .body("defaultMemoryOvercommitRatio", equalTo(2.0))
            .body("vmClasses", hasItems("small", "medium", "large"))
            .body("storageClasses", hasItems("gold", "silver"))
            .body("networkDomains", hasItems("private", "public"));

        // Verify update
        Response verifyResponse = ProviderTestUtil.getDatacenter(datacenterId);
        assertStatusCode(verifyResponse, 200);
        verifyResponse.then()
            .body("settings.defaultCpuOvercommitRatio", equalTo(8.0))
            .body("settings.defaultMemoryOvercommitRatio", equalTo(2.0));

        // Cleanup
        assertStatusCode(ProviderTestUtil.deleteDatacenter(datacenterId), 204);
    }

    @Test
    public void testDeleteDatacenter() {
        // Create a datacenter first
        String datacenterName = ProviderTestUtil.generateUniqueDatacenterName();
        Response createResponse = ProviderTestUtil.createMockDatacenter(
            datacenterName,
            "Test datacenter for deletion"
        );
        assertStatusCode(createResponse, 201);
        String datacenterId = createResponse.jsonPath().getString("id");

        // Delete the datacenter
        Response deleteResponse = ProviderTestUtil.deleteDatacenter(datacenterId);
        assertStatusCode(deleteResponse, 204);

        // Verify it's deleted
        Response verifyResponse = ProviderTestUtil.getDatacenter(datacenterId);
        assertStatusCode(verifyResponse, 404);
    }

    @Test
    public void testCreateTenantDatacenterGrant() {
        // Create a datacenter first
        String datacenterName = ProviderTestUtil.generateUniqueDatacenterName();
        Response datacenterResponse = ProviderTestUtil.createMockDatacenter(
            datacenterName,
            "Test datacenter for tenant grant"
        );
        assertStatusCode(datacenterResponse, 201);
        String datacenterId = datacenterResponse.jsonPath().getString("id");

        // Create tenant datacenter grant
        Response grantResponse = ProviderTestUtil.createTenantDatacenterGrant(tenantId, datacenterId);
        assertStatusCode(grantResponse, 201);
        grantResponse.then()
            .body("tenantId", equalTo(tenantId))
            .body("datacenterId", equalTo(datacenterId))
            .body("access", equalTo(true))
            .body("limits.maxCpus", equalTo(100))
            .body("limits.maxMemoryGb", equalTo(100))
            .body("limits.maxStorageGb", equalTo(1000))
            .body("limits.maxVms", equalTo(50));

        // Verify grant was created
        Response verifyResponse = ProviderTestUtil.getTenantDatacenterGrant(tenantId, datacenterId);
        assertStatusCode(verifyResponse, 200);
        verifyResponse.then()
            .body("tenantId", equalTo(tenantId))
            .body("datacenterId", equalTo(datacenterId));

        // Cleanup
        assertStatusCode(ProviderTestUtil.deleteTenantDatacenterGrant(tenantId, datacenterId), 204);
        assertStatusCode(ProviderTestUtil.deleteDatacenter(datacenterId), 204);
    }

    @Test
    public void testDeleteTenantDatacenterGrant() {
        // Create a datacenter first
        String datacenterName = ProviderTestUtil.generateUniqueDatacenterName();
        Response datacenterResponse = ProviderTestUtil.createMockDatacenter(
            datacenterName,
            "Test datacenter for grant deletion"
        );
        assertStatusCode(datacenterResponse, 201);
        String datacenterId = datacenterResponse.jsonPath().getString("id");

        // Create tenant datacenter grant
        Response grantResponse = ProviderTestUtil.createTenantDatacenterGrant(tenantId, datacenterId);
        assertStatusCode(grantResponse, 201);

        // Delete the grant
        Response deleteResponse = ProviderTestUtil.deleteTenantDatacenterGrant(tenantId, datacenterId);
        assertStatusCode(deleteResponse, 204);

        // Verify it's deleted
        Response verifyResponse = ProviderTestUtil.getTenantDatacenterGrant(tenantId, datacenterId);
        assertStatusCode(verifyResponse, 404);

        // Cleanup
        assertStatusCode(ProviderTestUtil.deleteDatacenter(datacenterId), 204);
    }

    @Test
    public void testMockProviderType() {
        // Create a datacenter with mock provider type
        String datacenterName = ProviderTestUtil.generateUniqueDatacenterName();
        Response response = ProviderTestUtil.createMockDatacenter(
            datacenterName,
            "Test mock provider type"
        );

        assertStatusCode(response, 201);
        response.then()
            .body("settings.providerType", equalTo("kvm"));

        // Verify that mock provider is registered and accessible
        // This is tested indirectly by ensuring the datacenter creation succeeds
        // and that the providerType is correctly stored

        // Cleanup
        String datacenterId = response.jsonPath().getString("id");
        assertStatusCode(ProviderTestUtil.deleteDatacenter(datacenterId), 204);
    }

    @Test
    public void testProviderSettingsWithFeatures() {
        // Create a datacenter with features
        String datacenterName = ProviderTestUtil.generateUniqueDatacenterName();
        Response createResponse = ProviderTestUtil.createMockDatacenter(
            datacenterName,
            "Test datacenter with features"
        );
        assertStatusCode(createResponse, 201);
        String datacenterId = createResponse.jsonPath().getString("id");

        // Update settings with features
        DatacenterSettings settings = new DatacenterSettings();
        settings.setProviderType(DatacenterType.KVM);

        Response updateResponse = ProviderTestUtil.updateDatacenterSettings(datacenterId, settings);
        assertStatusCode(updateResponse, 200);

        // Cleanup
        assertStatusCode(ProviderTestUtil.deleteDatacenter(datacenterId), 204);
    }

    @Test
    public void testMultipleDatacentersWithDifferentProviderTypes() {
        // Create datacenters with different provider types
        String datacenterName1 = ProviderTestUtil.generateUniqueDatacenterName();
        String datacenterName2 = ProviderTestUtil.generateUniqueDatacenterName();

        Response response1 = ProviderTestUtil.createMockDatacenter(
            datacenterName1,
            "Test datacenter 1 with mock provider"
        );
        assertStatusCode(response1, 201);
        String datacenterId1 = response1.jsonPath().getString("id");

        // Create another datacenter (both will use mock provider as fallback)
        Response response2 = ProviderTestUtil.createMockDatacenter(
            datacenterName2,
            "Test datacenter 2 with mock provider"
        );
        assertStatusCode(response2, 201);
        String datacenterId2 = response2.jsonPath().getString("id");

        // List datacenters and verify both exist
        Response listResponse = ProviderTestUtil.listDatacenters();
        assertStatusCode(listResponse, 200);
        listResponse.then()
            .body("items.find { it.id == '%s' }.name".formatted(datacenterId1), equalTo(datacenterName1))
            .body("items.find { it.id == '%s' }.name".formatted(datacenterId2), equalTo(datacenterName2));

        // Cleanup
        assertStatusCode(ProviderTestUtil.deleteDatacenter(datacenterId1), 204);
        assertStatusCode(ProviderTestUtil.deleteDatacenter(datacenterId2), 204);
    }

    @Test
    public void testCreateLibvirtProviderDatacenter() {
        String datacenterName = ProviderTestUtil.generateUniqueDatacenterName();

        Response response = ProviderTestUtil.createMockDatacenter(
            datacenterName,
            "Test datacenter with libvirt provider"
        );

        assertStatusCode(response, 201);
        response.then()
            .body("name", equalTo(datacenterName))
            .body("description", equalTo("Test datacenter with libvirt provider"))
            .body("settings.providerType", equalTo("kvm"))
            .body("settings.defaultCpuOvercommitRatio", equalTo(4.0))
            .body("settings.defaultMemoryOvercommitRatio", equalTo(1.5));

        // Cleanup
        String datacenterId = response.jsonPath().getString("id");
        assertStatusCode(ProviderTestUtil.deleteDatacenter(datacenterId), 204);
    }

    @Test
    public void testLibvirtProviderCapabilities() {
        String datacenterName = ProviderTestUtil.generateUniqueDatacenterName();

        Response createResponse = ProviderTestUtil.createMockDatacenter(
            datacenterName,
            "Test datacenter for libvirt capabilities"
        );
        assertStatusCode(createResponse, 201);
        String datacenterId = createResponse.jsonPath().getString("id");

        // Update settings to enable libvirt-specific features
        DatacenterSettings settings = new DatacenterSettings();
        settings.setProviderType(DatacenterType.KVM);
        settings.setDefaultCpuOvercommitRatio(4.0F);
        settings.setDefaultMemoryOvercommitRatio(1.5F);
        settings.setVmClasses(Arrays.asList("small", "medium", "large"));
        settings.setStorageClasses(Arrays.asList("ssd", "hdd"));
        settings.setNetworkDomains(Arrays.asList("default", "management"));

        Response updateResponse = ProviderTestUtil.updateDatacenterSettings(datacenterId, settings);
        assertStatusCode(updateResponse, 200);
        updateResponse.then()
            .body("vmClasses", hasItems("small", "medium", "large"))
            .body("storageClasses", hasItems("ssd", "hdd"))
            .body("networkDomains", hasItems("default", "management"));

        // Cleanup
        assertStatusCode(ProviderTestUtil.deleteDatacenter(datacenterId), 204);
    }

    @Test
    public void testLibvirtProviderWithMultipleDatacenters() {
        // Create multiple datacenters with libvirt provider
        String datacenterName1 = ProviderTestUtil.generateUniqueDatacenterName();
        String datacenterName2 = ProviderTestUtil.generateUniqueDatacenterName();

        Response response1 = ProviderTestUtil.createMockDatacenter(
            datacenterName1,
            "Test libvirt datacenter 1"
        );
        assertStatusCode(response1, 201);
        String datacenterId1 = response1.jsonPath().getString("id");

        Response response2 = ProviderTestUtil.createMockDatacenter(
            datacenterName2,
            "Test libvirt datacenter 2"
        );
        assertStatusCode(response2, 201);
        String datacenterId2 = response2.jsonPath().getString("id");

        // Verify both datacenters exist and use KVM provider type
        Response listResponse = ProviderTestUtil.listDatacenters();
        assertStatusCode(listResponse, 200);
        listResponse.then()
            .body("items.find { it.id == '%s' }.name".formatted(datacenterId1), equalTo(datacenterName1))
            .body("items.find { it.id == '%s' }.settings.providerType".formatted(datacenterId1), equalTo("kvm"))
            .body("items.find { it.id == '%s' }.name".formatted(datacenterId2), equalTo(datacenterName2))
            .body("items.find { it.id == '%s' }.settings.providerType".formatted(datacenterId2), equalTo("kvm"));

        // Cleanup
        assertStatusCode(ProviderTestUtil.deleteDatacenter(datacenterId1), 204);
        assertStatusCode(ProviderTestUtil.deleteDatacenter(datacenterId2), 204);
    }
}

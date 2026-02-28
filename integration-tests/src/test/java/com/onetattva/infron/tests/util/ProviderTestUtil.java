package com.onetattva.infron.tests.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.tests.InfronEnvironment;
import io.restassured.response.Response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/**
 * Utility class for provider-related test operations.
 * Tests provider functionality through datacenter API.
 */
public class ProviderTestUtil {

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
     * Create a datacenter with mock provider type
     */
    public static Response createMockDatacenter(String name, String description) {
        try {
            DatacenterCreate datacenter = new DatacenterCreate();
            datacenter.setName(name);
            datacenter.setDescription(description);
            // Set provider type to KVM
            DatacenterSettings settings = new DatacenterSettings();
            settings.setProviderType(ProviderType.LIBVIRT);
            settings.setDefaultCpuOvercommitRatio(4.0F);
            settings.setDefaultMemoryOvercommitRatio(1.5F);
            datacenter.setSettings(settings);
            
            return given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(objectMapper.writeValueAsString(datacenter))
                .when()
                .post(baseUrl + "/datacenters");
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize datacenter", e);
        }
    }

    /**
     * List all datacenters
     */
    public static Response listDatacenters() {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get(baseUrl + "/datacenters");
    }

    /**
     * Get datacenter by ID
     */
    public static Response getDatacenter(String datacenterId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get(baseUrl + "/datacenters/" + datacenterId);
    }

    /**
     * Update datacenter settings
     */
    public static Response updateDatacenterSettings(String datacenterId, DatacenterSettings settings) {
        try {
            return given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(objectMapper.writeValueAsString(settings))
                .when()
                .put(baseUrl + "/datacenters/" + datacenterId + "/settings");
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize datacenter settings", e);
        }
    }

    /**
     * Delete datacenter
     */
    public static Response deleteDatacenter(String datacenterId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .delete(baseUrl + "/datacenters/" + datacenterId);
    }

    /**
     * Create tenant datacenter grant
     */
    public static Response createTenantDatacenterGrant(String tenantId, String datacenterId) {
        try {
            TenantDatacenterGrant grant = new TenantDatacenterGrant();
            grant.setTenantId(UUID.fromString(tenantId));
            grant.setDatacenterId(UUID.fromString(datacenterId));
            grant.setAccess(true);
            
            // Set resource limits
            ResourceLimits limits = new ResourceLimits();
            limits.setMaxCpus(100);
            limits.setMaxMemoryGb(100);
            limits.setMaxStorageGb(1000);
            limits.setMaxVms(50);
            grant.setLimits(limits);
            
            return given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(objectMapper.writeValueAsString(grant))
                .when()
                .post(baseUrl + "/tenants/" + tenantId + "/datacenters");
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize tenant datacenter grant", e);
        }
    }

    /**
     * Get tenant datacenter grant
     */
    public static Response getTenantDatacenterGrant(String tenantId, String datacenterId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get(baseUrl + "/tenants/" + tenantId + "/datacenters/" + datacenterId);
    }

    /**
     * Delete tenant datacenter grant
     */
    public static Response deleteTenantDatacenterGrant(String tenantId, String datacenterId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .delete(baseUrl + "/tenants/" + tenantId + "/datacenters/" + datacenterId);
    }

    /**
     * Generate unique datacenter name for testing
     */
    public static String generateUniqueDatacenterName() {
        return "test-datacenter-" + UUID.randomUUID().toString().substring(0, 8);
    }

    // --------------- Provider API (direct /providers endpoints) ---------------

    /**
     * Create a provider via POST /providers
     */
    public static Response createProvider(String name, ProviderType type, String endpoint, Map<String, String> credentials) {
        try {
            ProviderCreate create = new ProviderCreate();
            create.setName(name);
            create.setType(type);
            create.setEndpoint(endpoint);
            create.setCredentials(credentials);
            return given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(objectMapper.writeValueAsString(create))
                .when()
                .post(baseUrl + "/providers");
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize provider create", e);
        }
    }

    /**
     * List providers
     */
    public static Response listProviders(Integer page, Integer perPage, ProviderType type, ProviderStatus status) {
        RequestSpecification request = given()
            .header("Authorization", "Bearer " + accessToken)
            .queryParam("page", page != null ? page : 1)
            .queryParam("perPage", perPage != null ? perPage : 20);
        if (type != null) {
            request = request.queryParam("type", type.name());
        }
        if (status != null) {
            request = request.queryParam("status", status.name());
        }
        return request.when().get(baseUrl + "/providers");
    }

    /**
     * Get provider by ID
     */
    public static Response getProvider(String providerId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get(baseUrl + "/providers/" + providerId);
    }

    /**
     * Delete provider
     */
    public static Response deleteProvider(String providerId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .delete(baseUrl + "/providers/" + providerId);
    }

    /**
     * Test provider connection
     */
    public static Response testProviderConnection(String providerId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .post(baseUrl + "/providers/" + providerId + "/test");
    }

    /**
     * Get provider capabilities
     */
    public static Response getProviderCapabilities(String providerId, Boolean refresh) {
        RequestSpecification request = given().header("Authorization", "Bearer " + accessToken);
        if (refresh != null) {
            request = request.queryParam("refresh", refresh);
        }
        return request.when().get(baseUrl + "/providers/" + providerId + "/capabilities");
    }

    /**
     * Generate unique provider name for testing
     */
    public static String generateUniqueProviderName() {
        return "test-provider-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Minimal SSH private key (Ed25519) for Libvirt provider validation tests.
     * Used when only presence of sshPrivateKey is validated; connection may still fail.
     */
    public static final String MINIMAL_SSH_PRIVATE_KEY = "-----BEGIN OPENSSH PRIVATE KEY-----\n"
        + "b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtzc2gtZW\n"
        + "QyNTUxOQAAACBxY2F0cyBtaW5pbWFsIGtleSBmb3IgdGVzdHMgb25seQAAAECnp7Ro\n"
        + "-----END OPENSSH PRIVATE KEY-----";
}

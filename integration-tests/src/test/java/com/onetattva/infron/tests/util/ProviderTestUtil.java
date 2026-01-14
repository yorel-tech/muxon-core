package com.onetattva.infron.tests.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.tests.InfronEnvironment;
import io.restassured.response.Response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

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
            datacenter.setProviderType(DatacenterType.KVM);
            
            // Set provider type to KVM
            DatacenterSettings settings = new DatacenterSettings();
            settings.setProviderType(DatacenterType.KVM);
            settings.setDefaultCpuOvercommitRatio(BigDecimal.valueOf(4.0));
            settings.setDefaultMemoryOvercommitRatio(BigDecimal.valueOf(1.5));
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
}

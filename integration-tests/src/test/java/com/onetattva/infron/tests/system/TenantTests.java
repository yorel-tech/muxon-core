package com.onetattva.infron.tests.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.tests.BaseIntegrationTest;
import com.onetattva.infron.tests.RestConstants;
import com.onetattva.infron.tests.util.RoleBindingTestUtil;
import com.onetattva.infron.tests.util.RoleTestUtil;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class TenantTests extends BaseIntegrationTest {

    private static String limitedAdminToken;

    @BeforeAll
    public static void setup() {
        // Get admin access token first
        accessToken = getAccessToken(RestConstants.DEFAULT_USERNAME, RestConstants.DEFAULT_PASSWORD);

        // Setup utility classes
        RoleTestUtil.setup(environment, accessToken);
        RoleBindingTestUtil.setup(environment, accessToken);

        // Create a role for limited admin without tenant:manage permission
        String roleName = "limited-admin-role-" + UUID.randomUUID().toString().substring(0, 8);
        List<String> permissions = Arrays.asList("tenant:read", "tenant:edit");
        Response roleResponse = RoleTestUtil.createRole(roleName, "Limited Admin Role", "system", null, permissions);
        assertStatusCode(roleResponse, 201);
        String roleId = roleResponse.jsonPath().getString("id");

        // Get limited-admin user ID by decoding their token
        String tempLimitedAdminToken = getAccessToken(RestConstants.LIMITED_ADMIN_USERNAME, RestConstants.LIMITED_ADMIN_PASSWORD);
        String limitedAdminUserId = extractUserIdFromToken(tempLimitedAdminToken);

        // Assign the role to the limited-admin user
        List<String> userIds = Arrays.asList(limitedAdminUserId);
        Response bindingResponse = RoleBindingTestUtil.createRoleBindings(roleId, userIds, "system", null);
        assertStatusCode(bindingResponse, 201);

        // Now get the limited-admin token (which should include the role permissions)
        limitedAdminToken = getAccessToken(RestConstants.LIMITED_ADMIN_USERNAME, RestConstants.LIMITED_ADMIN_PASSWORD);
    }

    private static String extractUserIdFromToken(String token) {
        // JWT structure: header.payload.signature
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid JWT token");
        }

        // Decode the payload (second part)
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));

        // Parse JSON to extract 'sub' claim
        // Simple JSON parsing for 'sub' field
        int subIndex = payload.indexOf("\"sub\":\"");
        if (subIndex == -1) {
            throw new IllegalArgumentException("No 'sub' claim found in token");
        }

        int start = subIndex + 7; // Length of "\"sub\":\""
        int end = payload.indexOf("\"", start);
        if (end == -1) {
            throw new IllegalArgumentException("Invalid 'sub' claim format");
        }

        return payload.substring(start, end);
    }

    @Test
    public void testCreateTenant() {
        String tenantName = "test-tenant-" + UUID.randomUUID().toString().substring(0, 8);

        TenantCreate tenant = new TenantCreate();
        tenant.setName(tenantName);
        tenant.setDisplayName("Test Tenant");

        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body(tenant)
            .when()
            .post("/tenants");

        assertStatusCode(response, 201);
        response.then()
            .body("name", equalTo(tenantName))
            .body("displayName", equalTo("Test Tenant"))
            .body("status", equalTo("active"));
    }

    @Test
    public void testListTenants() {
        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get("/tenants");

        assertStatusCode(response, 200);
        response.then()
            .body("items", notNullValue());
    }

    @Test
    public void testGetTenant() {
        // First create a tenant
        String tenantName = "test-tenant-get-" + UUID.randomUUID().toString().substring(0, 8);

        TenantCreate tenant = new TenantCreate();
        tenant.setName(tenantName);
        tenant.setDisplayName("Test Tenant for Get");

        Response createResponse = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body(tenant)
            .when()
            .post("/tenants");

        assertStatusCode(createResponse, 201);
        String tenantId = createResponse.jsonPath().getString("id");

        // Now get the tenant
        Response getResponse = given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get("/tenants/" + tenantId);

        assertStatusCode(getResponse, 200);
        getResponse.then()
            .body("id", equalTo(tenantId))
            .body("name", equalTo(tenantName))
            .body("displayName", equalTo("Test Tenant for Get"));
    }

    @Test
    public void testUpdateTenant() {
        // First create a tenant
        String tenantName = "test-tenant-update-" + UUID.randomUUID().toString().substring(0, 8);

        TenantCreate createTenant = new TenantCreate();
        createTenant.setName(tenantName);
        createTenant.setDisplayName("Test Tenant for Update");

        Response createResponse = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body(createTenant)
            .when()
            .post("/tenants");

        assertStatusCode(createResponse, 201);
        String tenantId = createResponse.jsonPath().getString("id");

        // Now update the tenant
        TenantUpdate updateTenant = new TenantUpdate();
        updateTenant.setDisplayName("Updated Test Tenant");
        updateTenant.setStatus(TenantUpdate.StatusEnum.INACTIVE);

        Response updateResponse = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body(updateTenant)
            .when()
            .put("/tenants/" + tenantId);

        assertStatusCode(updateResponse, 200);
        updateResponse.then()
            .body("id", equalTo(tenantId))
            .body("name", equalTo(tenantName))
            .body("displayName", equalTo("Updated Test Tenant"))
            .body("status", equalTo("inactive"));
    }

    @Test
    public void testDeleteTenant() {
        // First create a tenant
        String tenantName = "test-tenant-delete-" + UUID.randomUUID().toString().substring(0, 8);

        TenantCreate tenant = new TenantCreate();
        tenant.setName(tenantName);
        tenant.setDisplayName("Test Tenant for Delete");

        Response createResponse = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body(tenant)
            .when()
            .post("/tenants");

        assertStatusCode(createResponse, 201);
        String tenantId = createResponse.jsonPath().getString("id");

        // Now delete the tenant
        Response deleteResponse = given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .delete("/tenants/" + tenantId);

        assertStatusCode(deleteResponse, 204);

        // Verify it's deleted by trying to get it
        Response getResponse = given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get("/tenants/" + tenantId);

        assertStatusCode(getResponse, 404);
    }

    @Test
    public void testCreateTenantWithoutPermission() {
        // Try to create tenant with limited-admin user (should fail due to missing tenant:manage permission)
        String tenantName = "test-tenant-limited-admin-" + UUID.randomUUID().toString().substring(0, 8);

        TenantCreate tenant = new TenantCreate();
        tenant.setName(tenantName);
        tenant.setDisplayName("Test Tenant by Limited Admin");

        Response response = given()
            .header("Authorization", "Bearer " + limitedAdminToken)
            .contentType("application/json")
            .body(tenant)
            .when()
            .post("/tenants");

        assertStatusCode(response, 403); // Forbidden due to insufficient permissions
    }

    @Test
    public void testDeleteTenantWithoutManagePermission() {
        // First create a tenant with admin user
        String tenantName = "test-tenant-delete-limited-admin-" + UUID.randomUUID().toString().substring(0, 8);

        TenantCreate tenant = new TenantCreate();
        tenant.setName(tenantName);
        tenant.setDisplayName("Test Tenant for Limited Admin Delete Test");

        Response createResponse = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body(tenant)
            .when()
            .post("/tenants");

        assertStatusCode(createResponse, 201);
        String tenantId = createResponse.jsonPath().getString("id");

        // Try to delete tenant with limited-admin user (should fail due to missing tenant:manage permission)
        Response deleteResponse = given()
            .header("Authorization", "Bearer " + limitedAdminToken)
            .when()
            .delete("/tenants/" + tenantId);

        assertStatusCode(deleteResponse, 403); // Forbidden due to insufficient permissions

        // Verify the tenant still exists (admin can still access it)
        Response getResponse = given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get("/tenants/" + tenantId);

        assertStatusCode(getResponse, 200);
        getResponse.then()
            .body("name", equalTo(tenantName));

        // Cleanup - delete tenant with admin permissions
        Response cleanupResponse = given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .delete("/tenants/" + tenantId);

        assertStatusCode(cleanupResponse, 204);
    }
}

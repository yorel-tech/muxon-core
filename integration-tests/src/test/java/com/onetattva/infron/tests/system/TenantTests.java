package com.onetattva.infron.tests.system;

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
        roleResponse.then().statusCode(201);
        String roleId = roleResponse.jsonPath().getString("id");

        // Get limited-admin user ID by decoding their token
        String tempLimitedAdminToken = getAccessToken(RestConstants.LIMITED_ADMIN_USERNAME, RestConstants.LIMITED_ADMIN_PASSWORD);
        String limitedAdminUserId = extractUserIdFromToken(tempLimitedAdminToken);

        // Assign the role to the limited-admin user
        List<String> userIds = Arrays.asList(limitedAdminUserId);
        RoleBindingTestUtil.createRoleBindings(roleId, userIds, "system", null)
            .then()
            .statusCode(201);

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

        given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body("""
                {
                    "name": "%s",
                    "displayName": "Test Tenant"
                }
                """.formatted(tenantName))
            .when()
            .post("/tenants")
            .then()
            .statusCode(201)
            .body("name", equalTo(tenantName))
            .body("displayName", equalTo("Test Tenant"))
            .body("status", equalTo("active"));
    }

    @Test
    public void testListTenants() {
        given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get("/tenants")
            .then()
            .statusCode(200)
            .body("items", notNullValue());
    }

    @Test
    public void testGetTenant() {
        // First create a tenant
        String tenantName = "test-tenant-get-" + UUID.randomUUID().toString().substring(0, 8);

        Response createResponse = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body("""
                {
                    "name": "%s",
                    "displayName": "Test Tenant for Get"
                }
                """.formatted(tenantName))
            .when()
            .post("/tenants");

        createResponse.then().statusCode(201);
        String tenantId = createResponse.jsonPath().getString("id");

        // Now get the tenant
        given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get("/tenants/" + tenantId)
            .then()
            .statusCode(200)
            .body("id", equalTo(tenantId))
            .body("name", equalTo(tenantName))
            .body("displayName", equalTo("Test Tenant for Get"));
    }

    @Test
    public void testUpdateTenant() {
        // First create a tenant
        String tenantName = "test-tenant-update-" + UUID.randomUUID().toString().substring(0, 8);

        Response createResponse = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body("""
                {
                    "name": "%s",
                    "displayName": "Test Tenant for Update"
                }
                """.formatted(tenantName))
            .when()
            .post("/tenants");

        createResponse.then().statusCode(201);
        String tenantId = createResponse.jsonPath().getString("id");

        // Now update the tenant
        given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body("""
                {
                    "displayName": "Updated Test Tenant",
                    "status": "inactive"
                }
                """)
            .when()
            .put("/tenants/" + tenantId)
            .then()
            .statusCode(200)
            .body("id", equalTo(tenantId))
            .body("name", equalTo(tenantName))
            .body("displayName", equalTo("Updated Test Tenant"))
            .body("status", equalTo("inactive"));
    }

    @Test
    public void testDeleteTenant() {
        // First create a tenant
        String tenantName = "test-tenant-delete-" + UUID.randomUUID().toString().substring(0, 8);

        Response createResponse = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body("""
                {
                    "name": "%s",
                    "displayName": "Test Tenant for Delete"
                }
                """.formatted(tenantName))
            .when()
            .post("/tenants");

        createResponse.then().statusCode(201);
        String tenantId = createResponse.jsonPath().getString("id");

        // Now delete the tenant
        given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .delete("/tenants/" + tenantId)
            .then()
            .statusCode(204);

        // Verify it's deleted by trying to get it
        given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get("/tenants/" + tenantId)
            .then()
            .statusCode(404);
    }

    @Test
    public void testCreateTenantWithoutPermission() {
        // Try to create tenant with limited-admin user (should fail due to missing tenant:manage permission)
        String tenantName = "test-tenant-limited-admin-" + UUID.randomUUID().toString().substring(0, 8);

        given()
            .header("Authorization", "Bearer " + limitedAdminToken)
            .contentType("application/json")
            .body("""
                {
                    "name": "%s",
                    "displayName": "Test Tenant by Limited Admin"
                }
                """.formatted(tenantName))
            .when()
            .post("/tenants")
            .then()
            .statusCode(403); // Forbidden due to insufficient permissions
    }

    @Test
    public void testDeleteTenantWithoutManagePermission() {
        // First create a tenant with admin user
        String tenantName = "test-tenant-delete-limited-admin-" + UUID.randomUUID().toString().substring(0, 8);

        Response createResponse = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body("""
                {
                    "name": "%s",
                    "displayName": "Test Tenant for Limited Admin Delete Test"
                }
                """.formatted(tenantName))
            .when()
            .post("/tenants");

        createResponse.then().statusCode(201);
        String tenantId = createResponse.jsonPath().getString("id");

        // Try to delete tenant with limited-admin user (should fail due to missing tenant:manage permission)
        given()
            .header("Authorization", "Bearer " + limitedAdminToken)
            .when()
            .delete("/tenants/" + tenantId)
            .then()
            .statusCode(403); // Forbidden due to insufficient permissions

        // Verify the tenant still exists (admin can still access it)
        given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get("/tenants/" + tenantId)
            .then()
            .statusCode(200)
            .body("name", equalTo(tenantName));

        // Cleanup - delete tenant with admin permissions
        given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .delete("/tenants/" + tenantId)
            .then()
            .statusCode(204);
    }
}

package com.scal.muxon.tests.system;

import com.scal.muxon.api.model.*;
import com.scal.muxon.tests.BaseIntegrationTest;
import com.scal.muxon.tests.RestConstants;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TenantTests extends BaseIntegrationTest {

    /** System tenant UUID from seed data (V1 migration). List endpoint must not return it. */
    private static final String SYSTEM_TENANT_ID = "215012d9-8b1e-5dc5-b54f-89022875fe1e";

    private static String limitedAdminToken;

    @BeforeAll
    public static void setup() {
        accessToken = getAccessToken(RestConstants.DEFAULT_USERNAME, RestConstants.DEFAULT_PASSWORD);
        // limited-admin exists in Keycloak but has no muxon role_bindings from bootstrap → no tenant:manage.
        limitedAdminToken = getAccessToken(RestConstants.LIMITED_ADMIN_USERNAME, RestConstants.LIMITED_ADMIN_PASSWORD);
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
    public void testListTenantsExcludesSystemTenant() {
        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get("/tenants");

        assertStatusCode(response, 200);
        List<String> tenantIds = response.jsonPath().getList("items.id", String.class);
        List<String> tenantNames = response.jsonPath().getList("items.name", String.class);
        if (tenantIds != null) {
            assertFalse(tenantIds.contains(SYSTEM_TENANT_ID),
                "List must not include system tenant (id=" + SYSTEM_TENANT_ID + ")");
        }
        if (tenantNames != null) {
            assertFalse(tenantNames.stream().anyMatch("system"::equalsIgnoreCase),
                "List must not include tenant with name 'system'");
        }
    }

    @Test
    public void testCreateTenantReservedNameRejected() {
        TenantCreate tenant = new TenantCreate();
        tenant.setName("system");
        tenant.setDisplayName("System");

        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body(tenant)
            .when()
            .post("/tenants");

        assertStatusCode(response, 400);
        response.then()
            .body("code", equalTo("BAD_REQUEST"))
            .body("message", containsString("reserved"));
    }

    @Test
    public void testCreateTenantDuplicateNameRejected() {
        String tenantName = "test-tenant-dup-" + UUID.randomUUID().toString().substring(0, 8);
        TenantCreate tenant = new TenantCreate();
        tenant.setName(tenantName);
        tenant.setDisplayName("First");

        Response createResponse = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body(tenant)
            .when()
            .post("/tenants");
        assertStatusCode(createResponse, 201);

        TenantCreate duplicate = new TenantCreate();
        duplicate.setName(tenantName);
        duplicate.setDisplayName("Duplicate");
        Response duplicateResponse = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body(duplicate)
            .when()
            .post("/tenants");
        assertStatusCode(duplicateResponse, 400);
        duplicateResponse.then()
            .body("code", equalTo("BAD_REQUEST"))
            .body("message", containsString("already exists"));

        // Cleanup
        String tenantId = createResponse.jsonPath().getString("id");
        given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .delete("/tenants/" + tenantId)
            .then()
            .statusCode(204);
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

package com.onetattva.infron.tests.tenant;

import com.onetattva.infron.tests.BaseIntegrationTest;
import com.onetattva.infron.tests.util.RoleBindingTestUtil;
import com.onetattva.infron.tests.util.RoleTestUtil;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class RoleTests extends BaseIntegrationTest {

    private static String tenantId;

    @BeforeAll
    public static void setup() {
        // Get access token
        accessToken = getAccessToken();

        // Setup utility classes
        RoleTestUtil.setup(environment, accessToken);
        RoleBindingTestUtil.setup(environment, accessToken);

        // Create a test tenant
        tenantId = createTestTenant();
    }

    private static String createTestTenant() {
        String tenantName = "test-tenant-" + UUID.randomUUID().toString().substring(0, 8);

        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body("""
                {
                    "name": "%s",
                    "displayName": "Test Tenant for Role Tests"
                }
                """.formatted(tenantName))
            .when()
            .post("/tenants");

        response.then().statusCode(201);
        return response.jsonPath().getString("id");
    }

    @Test
    public void testCreateTenantRole() {
        String roleName = RoleTestUtil.generateUniqueRoleName();

        Response response = RoleTestUtil.createTenantRole(tenantId, roleName, "Test tenant role", "tenant", tenantId);
        response.then()
            .statusCode(201)
            .body("name", equalTo(roleName))
            .body("description", equalTo("Test tenant role"))
            .body("scope_type", equalTo("tenant"))
            .body("scope_id", equalTo(tenantId))
            .body("immutable", equalTo(false));

        String roleId = response.jsonPath().getString("id");

        // Cleanup
        RoleTestUtil.deleteTenantRole(tenantId, roleId).then().statusCode(204);
    }

    @Test
    public void testListTenantRoles() {
        // Create a tenant role first
        String roleName = RoleTestUtil.generateUniqueRoleName();
        Response createResponse = RoleTestUtil.createTenantRole(tenantId, roleName, "Test tenant role for listing", "tenant", tenantId);
        createResponse.then().statusCode(201);
        String roleId = createResponse.jsonPath().getString("id");

        // List tenant roles
        RoleTestUtil.listTenantRoles(tenantId)
            .then()
            .statusCode(200)
            .body("items", notNullValue())
            .body("items.find { it.id == '%s' }.name".formatted(roleId), equalTo(roleName));

        // Cleanup
        RoleTestUtil.deleteTenantRole(tenantId, roleId).then().statusCode(204);
    }

    @Test
    public void testGetTenantRole() {
        // Create a tenant role first
        String roleName = RoleTestUtil.generateUniqueRoleName();
        Response createResponse = RoleTestUtil.createTenantRole(tenantId, roleName, "Test tenant role for get", "tenant", tenantId);
        createResponse.then().statusCode(201);
        String roleId = createResponse.jsonPath().getString("id");

        // Get the tenant role
        RoleTestUtil.getTenantRole(tenantId, roleId)
            .then()
            .statusCode(200)
            .body("id", equalTo(roleId))
            .body("name", equalTo(roleName))
            .body("description", equalTo("Test tenant role for get"))
            .body("scope_type", equalTo("tenant"))
            .body("scope_id", equalTo(tenantId));

        // Cleanup
        RoleTestUtil.deleteTenantRole(tenantId, roleId).then().statusCode(204);
    }

    @Test
    public void testUpdateTenantRole() {
        // Create a tenant role first
        String roleName = RoleTestUtil.generateUniqueRoleName();
        Response createResponse = RoleTestUtil.createTenantRole(tenantId, roleName, "Original tenant description", "tenant", tenantId);
        createResponse.then().statusCode(201);
        String roleId = createResponse.jsonPath().getString("id");

        // Update the tenant role
        RoleTestUtil.updateTenantRole(tenantId, roleId, roleName + "-updated", "Updated tenant description")
            .then()
            .statusCode(200)
            .body("id", equalTo(roleId))
            .body("name", equalTo(roleName + "-updated"))
            .body("description", equalTo("Updated tenant description"));

        // Verify update
        RoleTestUtil.getTenantRole(tenantId, roleId)
            .then()
            .statusCode(200)
            .body("name", equalTo(roleName + "-updated"))
            .body("description", equalTo("Updated tenant description"));

        // Cleanup
        RoleTestUtil.deleteTenantRole(tenantId, roleId).then().statusCode(204);
    }

    @Test
    public void testDeleteTenantRole() {
        // Create a tenant role first
        String roleName = RoleTestUtil.generateUniqueRoleName();
        Response createResponse = RoleTestUtil.createTenantRole(tenantId, roleName, "Test tenant role for deletion", "tenant", tenantId);
        createResponse.then().statusCode(201);
        String roleId = createResponse.jsonPath().getString("id");

        // Delete the tenant role
        RoleTestUtil.deleteTenantRole(tenantId, roleId)
            .then()
            .statusCode(204);

        // Verify it's deleted
        RoleTestUtil.getTenantRole(tenantId, roleId)
            .then()
            .statusCode(404);
    }

    @Test
    public void testAddPermissionsToTenantRole() {
        // Create a tenant role first
        String roleName = RoleTestUtil.generateUniqueRoleName();
        Response createResponse = RoleTestUtil.createTenantRole(tenantId, roleName, "Test tenant role for permissions", "tenant", tenantId);
        createResponse.then().statusCode(201);
        String roleId = createResponse.jsonPath().getString("id");

        // Use permission action strings directly
        List<String> permissionsToAdd = Arrays.asList("vm:read", "vm:edit");

        // Add permissions to tenant role
        RoleTestUtil.addTenantRolePermissions(tenantId, roleId, permissionsToAdd)
            .then()
            .statusCode(200);

        // Verify permissions were added
        RoleTestUtil.listTenantRolePermissions(tenantId, roleId)
            .then()
            .statusCode(200)
            .body("items.size()", greaterThan(0));

        // Cleanup
        RoleTestUtil.deleteTenantRole(tenantId, roleId).then().statusCode(204);
    }

    @Test
    public void testRemovePermissionsFromTenantRole() {
        // Create a tenant role with permissions
        String roleName = RoleTestUtil.generateUniqueRoleName();
        List<String> permissionsToAdd = Arrays.asList("vm:read", "vm:edit");

        Response createResponse = RoleTestUtil.createTenantRole(tenantId, roleName, "Test tenant role for permission removal", "tenant", tenantId, permissionsToAdd);
        createResponse.then().statusCode(201);
        String roleId = createResponse.jsonPath().getString("id");

        // Remove permissions from tenant role
        RoleTestUtil.removeTenantRolePermissions(tenantId, roleId, permissionsToAdd)
            .then()
            .statusCode(200);

        // Verify permissions were removed
        RoleTestUtil.listTenantRolePermissions(tenantId, roleId)
            .then()
            .statusCode(200)
            .body("items.size()", equalTo(0));

        // Cleanup
        RoleTestUtil.deleteTenantRole(tenantId, roleId).then().statusCode(204);
    }

    @Test
    public void testCreateTenantRoleBindings() {
        // Create a tenant role first
        String roleName = RoleTestUtil.generateUniqueRoleName();
        Response createResponse = RoleTestUtil.createTenantRole(tenantId, roleName, "Test tenant role for bindings", "tenant", tenantId);
        createResponse.then().statusCode(201);
        String roleId = createResponse.jsonPath().getString("id");

        // Create tenant role bindings
        String userId1 = RoleBindingTestUtil.generateUniqueUserId();
        String userId2 = RoleBindingTestUtil.generateUniqueUserId();
        List<String> userIds = Arrays.asList(userId1, userId2);

        RoleBindingTestUtil.createTenantRoleBindings(tenantId, roleId, userIds, "tenant", tenantId)
            .then()
            .statusCode(201)
            .body("items.size()", equalTo(2));

        // Verify bindings were created
        RoleBindingTestUtil.listTenantRoleBindings(tenantId, roleId)
            .then()
            .statusCode(200)
            .body("items.size()", equalTo(2));

        // Cleanup - delete role (should cascade delete bindings)
        RoleTestUtil.deleteTenantRole(tenantId, roleId).then().statusCode(204);
    }

    @Test
    public void testListTenantRoleBindings() {
        // Create a tenant role first
        String roleName = RoleTestUtil.generateUniqueRoleName();
        Response createResponse = RoleTestUtil.createTenantRole(tenantId, roleName, "Test tenant role for binding listing", "tenant", tenantId);
        createResponse.then().statusCode(201);
        String roleId = createResponse.jsonPath().getString("id");

        // Create tenant role binding
        String userId = RoleBindingTestUtil.generateUniqueUserId();
        List<String> userIds = Arrays.asList(userId);

        RoleBindingTestUtil.createTenantRoleBindings(tenantId, roleId, userIds, "tenant", tenantId)
            .then()
            .statusCode(201);

        // List tenant role bindings
        RoleBindingTestUtil.listTenantRoleBindings(tenantId, roleId)
            .then()
            .statusCode(200)
            .body("items.size()", equalTo(1))
            .body("items[0].role_id", equalTo(roleId))
            .body("items[0].subject_id", equalTo(userId))
            .body("items[0].scope_type", equalTo("tenant"))
            .body("items[0].scope_id", equalTo(tenantId));

        // Cleanup
        RoleTestUtil.deleteTenantRole(tenantId, roleId).then().statusCode(204);
    }
}

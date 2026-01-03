package com.onetattva.infron.tests.system;

import com.onetattva.infron.tests.BaseIntegrationTest;
import com.onetattva.infron.tests.util.RoleBindingTestUtil;
import com.onetattva.infron.tests.util.RoleTestUtil;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class RoleTests extends BaseIntegrationTest {

    @BeforeAll
    public static void setup() {
        // Get access token
        accessToken = getAccessToken();

        // Setup utility classes
        RoleTestUtil.setup(environment, accessToken);
        RoleBindingTestUtil.setup(environment, accessToken);
    }

    @Test
    public void testCreateSystemRole() {
        String roleName = RoleTestUtil.generateUniqueRoleName();

        Response response = RoleTestUtil.createRole(roleName, "Test system role", "system");
        response.then()
            .statusCode(201)
            .body("name", equalTo(roleName))
            .body("description", equalTo("Test system role"))
            .body("scope_type", equalTo("system"))
            .body("scope_id", nullValue())
            .body("immutable", equalTo(false));

        String roleId = response.jsonPath().getString("id");

        // Cleanup
        RoleTestUtil.deleteRole(roleId).then().statusCode(204);
    }

    @Test
    public void testCreateTenantGlobalRole() {
        String roleName = RoleTestUtil.generateUniqueRoleName();

        Response response = RoleTestUtil.createRole(roleName, "Test tenant global role", "tenant_global");
        response.then()
            .statusCode(201)
            .body("name", equalTo(roleName))
            .body("description", equalTo("Test tenant global role"))
            .body("scope_type", equalTo("tenant_global"))
            .body("scope_id", nullValue())
            .body("immutable", equalTo(false));

        String roleId = response.jsonPath().getString("id");

        // Cleanup
        RoleTestUtil.deleteRole(roleId).then().statusCode(204);
    }

    @Test
    public void testListRoles() {
        // Create a role first
        String roleName = RoleTestUtil.generateUniqueRoleName();
        Response createResponse = RoleTestUtil.createRole(roleName, "Test role for listing", "system");
        createResponse.then().statusCode(201);
        String roleId = createResponse.jsonPath().getString("id");

        // List roles
        RoleTestUtil.listRoles()
            .then()
            .statusCode(200)
            .body("items", notNullValue())
            .body("items.find { it.id == '%s' }.name".formatted(roleId), equalTo(roleName));

        // Cleanup
        RoleTestUtil.deleteRole(roleId).then().statusCode(204);
    }

    @Test
    public void testGetRole() {
        // Create a role first
        String roleName = RoleTestUtil.generateUniqueRoleName();
        Response createResponse = RoleTestUtil.createRole(roleName, "Test role for get", "system");
        createResponse.then().statusCode(201);
        String roleId = createResponse.jsonPath().getString("id");

        // Get the role
        RoleTestUtil.getRole(roleId)
            .then()
            .statusCode(200)
            .body("id", equalTo(roleId))
            .body("name", equalTo(roleName))
            .body("description", equalTo("Test role for get"))
            .body("scope_type", equalTo("system"));

        // Cleanup
        RoleTestUtil.deleteRole(roleId).then().statusCode(204);
    }

    @Test
    public void testUpdateRole() {
        // Create a role first
        String roleName = RoleTestUtil.generateUniqueRoleName();
        Response createResponse = RoleTestUtil.createRole(roleName, "Original description", "system");
        createResponse.then().statusCode(201);
        String roleId = createResponse.jsonPath().getString("id");

        // Update the role
        RoleTestUtil.updateRole(roleId, roleName + "-updated", "Updated description")
            .then()
            .statusCode(200)
            .body("id", equalTo(roleId))
            .body("name", equalTo(roleName + "-updated"))
            .body("description", equalTo("Updated description"));

        // Verify update
        RoleTestUtil.getRole(roleId)
            .then()
            .statusCode(200)
            .body("name", equalTo(roleName + "-updated"))
            .body("description", equalTo("Updated description"));

        // Cleanup
        RoleTestUtil.deleteRole(roleId).then().statusCode(204);
    }

    @Test
    public void testDeleteRole() {
        // Create a role first
        String roleName = RoleTestUtil.generateUniqueRoleName();
        Response createResponse = RoleTestUtil.createRole(roleName, "Test role for deletion", "system");
        createResponse.then().statusCode(201);
        String roleId = createResponse.jsonPath().getString("id");

        // Delete the role
        RoleTestUtil.deleteRole(roleId)
            .then()
            .statusCode(204);

        // Verify it's deleted
        RoleTestUtil.getRole(roleId)
            .then()
            .statusCode(404);
    }

    @Test
    public void testAddPermissionsToRole() {
        // Create a role first
        String roleName = RoleTestUtil.generateUniqueRoleName();
        Response createResponse = RoleTestUtil.createRole(roleName, "Test role for permissions", "system");
        createResponse.then().statusCode(201);
        String roleId = createResponse.jsonPath().getString("id");

        // Use permission action strings directly
        List<String> permissionsToAdd = Arrays.asList("system:settings", "provider:read");

        // Add permissions to role
        RoleTestUtil.addRolePermissions(roleId, permissionsToAdd)
            .then()
            .statusCode(200);

        // Verify permissions were added
        RoleTestUtil.listRolePermissions(roleId)
            .then()
            .statusCode(200)
            .body("items.size()", greaterThan(0));

        // Cleanup
        RoleTestUtil.deleteRole(roleId).then().statusCode(204);
    }

    @Test
    public void testRemovePermissionsFromRole() {
        // Create a role with permissions
        String roleName = RoleTestUtil.generateUniqueRoleName();
        List<String> permissionsToAdd = Arrays.asList("system:settings", "provider:read");

        Response createResponse = RoleTestUtil.createRole(roleName, "Test role for permission removal", "system", null, permissionsToAdd);
        createResponse.then().statusCode(201);
        String roleId = createResponse.jsonPath().getString("id");

        // Remove permissions from role
        RoleTestUtil.removeRolePermissions(roleId, permissionsToAdd)
            .then()
            .statusCode(200);

        // Verify permissions were removed
        RoleTestUtil.listRolePermissions(roleId)
            .then()
            .statusCode(200)
            .body("items.size()", equalTo(0));

        // Cleanup
        RoleTestUtil.deleteRole(roleId).then().statusCode(204);
    }

    @Test
    public void testCreateRoleBindings() {
        // Create a role first
        String roleName = RoleTestUtil.generateUniqueRoleName();
        Response createResponse = RoleTestUtil.createRole(roleName, "Test role for bindings", "system");
        createResponse.then().statusCode(201);
        String roleId = createResponse.jsonPath().getString("id");

        // Create role bindings
        String userId1 = RoleBindingTestUtil.generateUniqueUserId();
        String userId2 = RoleBindingTestUtil.generateUniqueUserId();
        List<String> userIds = Arrays.asList(userId1, userId2);

        RoleBindingTestUtil.createRoleBindings(roleId, userIds, "system", null)
            .then()
            .statusCode(201)
            .body("items.size()", equalTo(2));

        // Verify bindings were created
        RoleBindingTestUtil.listRoleBindings(roleId)
            .then()
            .statusCode(200)
            .body("items.size()", equalTo(2));

        // Cleanup - delete role (should cascade delete bindings)
        RoleTestUtil.deleteRole(roleId).then().statusCode(204);
    }

    @Test
    public void testBulkCreateRoleBindings() {
        // Create two roles first
        String roleName1 = RoleTestUtil.generateUniqueRoleName();
        String roleName2 = RoleTestUtil.generateUniqueRoleName();

        Response createResponse1 = RoleTestUtil.createRole(roleName1, "Test role 1 for bulk bindings", "system");
        createResponse1.then().statusCode(201);
        String roleId1 = createResponse1.jsonPath().getString("id");

        Response createResponse2 = RoleTestUtil.createRole(roleName2, "Test role 2 for bulk bindings", "system");
        createResponse2.then().statusCode(201);
        String roleId2 = createResponse2.jsonPath().getString("id");

        List<String> roleIds = Arrays.asList(roleId1, roleId2);

        // Create bulk role bindings
        String userId1 = RoleBindingTestUtil.generateUniqueUserId();
        String userId2 = RoleBindingTestUtil.generateUniqueUserId();
        List<String> userIds = Arrays.asList(userId1, userId2);

        RoleBindingTestUtil.bulkCreateRoleBindings(roleIds, userIds, "system", null)
            .then()
            .statusCode(201)
            .body("items.size()", equalTo(4)); // 2 roles * 2 users

        // Cleanup
        RoleTestUtil.deleteRole(roleId1).then().statusCode(204);
        RoleTestUtil.deleteRole(roleId2).then().statusCode(204);
    }

    @Test
    public void testListUserBindings() {
        // Create a role first
        String roleName = RoleTestUtil.generateUniqueRoleName();
        Response createResponse = RoleTestUtil.createRole(roleName, "Test role for user bindings", "system");
        createResponse.then().statusCode(201);
        String roleId = createResponse.jsonPath().getString("id");

        // Create role binding for a user
        String userId = RoleBindingTestUtil.generateUniqueUserId();
        List<String> userIds = Arrays.asList(userId);

        RoleBindingTestUtil.createRoleBindings(roleId, userIds, "system", null)
            .then()
            .statusCode(201);

        // List user bindings
        RoleBindingTestUtil.listUserBindings(userId)
            .then()
            .statusCode(200)
            .body("items.size()", equalTo(1))
            .body("items[0].role_id", equalTo(roleId))
            .body("items[0].subject_id", equalTo(userId));

        // Cleanup
        RoleTestUtil.deleteRole(roleId).then().statusCode(204);
    }

    @Test
    public void testUpdateRoleBinding() {
        // Create two roles first
        String roleName1 = RoleTestUtil.generateUniqueRoleName();
        String roleName2 = RoleTestUtil.generateUniqueRoleName();

        Response createResponse1 = RoleTestUtil.createRole(roleName1, "Test role 1 for binding update", "system");
        createResponse1.then().statusCode(201);
        String roleId1 = createResponse1.jsonPath().getString("id");

        Response createResponse2 = RoleTestUtil.createRole(roleName2, "Test role 2 for binding update", "system");
        createResponse2.then().statusCode(201);
        String roleId2 = createResponse2.jsonPath().getString("id");

        // Create role binding
        String userId = RoleBindingTestUtil.generateUniqueUserId();
        List<String> userIds = Arrays.asList(userId);

        Response bindingResponse = RoleBindingTestUtil.createRoleBindings(roleId1, userIds, "system", null);
        bindingResponse.then().statusCode(201);
        String bindingId = bindingResponse.jsonPath().getString("items[0].id");

        // Update the binding to use the second role
        RoleBindingTestUtil.updateRoleBinding(bindingId, roleId2)
            .then()
            .statusCode(200)
            .body("role_id", equalTo(roleId2))
            .body("subject_id", equalTo(userId));

        // Verify the update
        RoleBindingTestUtil.getRoleBinding(bindingId)
            .then()
            .statusCode(200)
            .body("role_id", equalTo(roleId2));

        // Cleanup
        RoleTestUtil.deleteRole(roleId1).then().statusCode(204);
        RoleTestUtil.deleteRole(roleId2).then().statusCode(204);
    }

    @Test
    public void testDeleteRoleBinding() {
        // Create a role first
        String roleName = RoleTestUtil.generateUniqueRoleName();
        Response createResponse = RoleTestUtil.createRole(roleName, "Test role for binding deletion", "system");
        createResponse.then().statusCode(201);
        String roleId = createResponse.jsonPath().getString("id");

        // Create role binding
        String userId = RoleBindingTestUtil.generateUniqueUserId();
        List<String> userIds = Arrays.asList(userId);

        Response bindingResponse = RoleBindingTestUtil.createRoleBindings(roleId, userIds, "system", null);
        bindingResponse.then().statusCode(201);
        String bindingId = bindingResponse.jsonPath().getString("items[0].id");

        // Delete the binding
        RoleBindingTestUtil.deleteRoleBinding(bindingId)
            .then()
            .statusCode(204);

        // Verify it's deleted
        RoleBindingTestUtil.getRoleBinding(bindingId)
            .then()
            .statusCode(404);

        // Cleanup
        RoleTestUtil.deleteRole(roleId).then().statusCode(204);
    }
}

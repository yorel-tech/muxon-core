package com.onetattva.infron.tests.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onetattva.infron.api.model.*;
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
        assertStatusCode(response, 201);
        response.then()
            .body("name", equalTo(roleName))
            .body("description", equalTo("Test system role"))
            .body("scope_type", equalTo("system"))
            .body("scope_id", nullValue())
            .body("immutable", equalTo(false));

        String roleId = response.jsonPath().getString("id");

        // Cleanup
        assertStatusCode(RoleTestUtil.deleteRole(roleId), 204);
    }

    @Test
    public void testCreateTenantGlobalRole() {
        String roleName = RoleTestUtil.generateUniqueRoleName();

        Response response = RoleTestUtil.createRole(roleName, "Test tenant global role", "tenant_global");
        assertStatusCode(response, 201);
        response.then()
            .body("name", equalTo(roleName))
            .body("description", equalTo("Test tenant global role"))
            .body("scope_type", equalTo("tenant_global"))
            .body("scope_id", nullValue())
            .body("immutable", equalTo(false));

        String roleId = response.jsonPath().getString("id");

        // Cleanup
        assertStatusCode(RoleTestUtil.deleteRole(roleId), 204);
    }

    @Test
    public void testListRoles() {
        // Create a role first
        String roleName = RoleTestUtil.generateUniqueRoleName();
        Response createResponse = RoleTestUtil.createRole(roleName, "Test role for listing", "system");
        assertStatusCode(createResponse, 201);
        String roleId = createResponse.jsonPath().getString("id");

        // List roles
        Response listResponse = RoleTestUtil.listRoles();
        assertStatusCode(listResponse, 200);
        listResponse.then()
            .body("items", notNullValue())
            .body("items.find { it.id == '%s' }.name".formatted(roleId), equalTo(roleName));

        // Cleanup
        assertStatusCode(RoleTestUtil.deleteRole(roleId), 204);
    }

    @Test
    public void testGetRole() {
        // Create a role first
        String roleName = RoleTestUtil.generateUniqueRoleName();
        Response createResponse = RoleTestUtil.createRole(roleName, "Test role for get", "system");
        assertStatusCode(createResponse, 201);
        String roleId = createResponse.jsonPath().getString("id");

        // Get the role
        Response getResponse = RoleTestUtil.getRole(roleId);
        assertStatusCode(getResponse, 200);
        getResponse.then()
            .body("id", equalTo(roleId))
            .body("name", equalTo(roleName))
            .body("description", equalTo("Test role for get"))
            .body("scope_type", equalTo("system"));

        // Cleanup
        assertStatusCode(RoleTestUtil.deleteRole(roleId), 204);
    }

    @Test
    public void testUpdateRole() {
        // Create a role first
        String roleName = RoleTestUtil.generateUniqueRoleName();
        Response createResponse = RoleTestUtil.createRole(roleName, "Original description", "system");
        assertStatusCode(createResponse, 201);
        String roleId = createResponse.jsonPath().getString("id");

        // Update the role
        Response updateResponse = RoleTestUtil.updateRole(roleId, roleName + "-updated", "Updated description");
        assertStatusCode(updateResponse, 200);
        updateResponse.then()
            .body("id", equalTo(roleId))
            .body("name", equalTo(roleName + "-updated"))
            .body("description", equalTo("Updated description"));

        // Verify update
        Response verifyResponse = RoleTestUtil.getRole(roleId);
        assertStatusCode(verifyResponse, 200);
        verifyResponse.then()
            .body("name", equalTo(roleName + "-updated"))
            .body("description", equalTo("Updated description"));

        // Cleanup
        assertStatusCode(RoleTestUtil.deleteRole(roleId), 204);
    }

    @Test
    public void testDeleteRole() {
        // Create a role first
        String roleName = RoleTestUtil.generateUniqueRoleName();
        Response createResponse = RoleTestUtil.createRole(roleName, "Test role for deletion", "system");
        assertStatusCode(createResponse, 201);
        String roleId = createResponse.jsonPath().getString("id");

        // Delete the role
        Response deleteResponse = RoleTestUtil.deleteRole(roleId);
        assertStatusCode(deleteResponse, 204);

        // Verify it's deleted
        Response verifyResponse = RoleTestUtil.getRole(roleId);
        assertStatusCode(verifyResponse, 404);
    }

    @Test
    public void testAddPermissionsToRole() {
        // Create a role first
        String roleName = RoleTestUtil.generateUniqueRoleName();
        Response createResponse = RoleTestUtil.createRole(roleName, "Test role for permissions", "system");
        assertStatusCode(createResponse, 201);
        String roleId = createResponse.jsonPath().getString("id");

        // Use permission action strings directly
        List<String> permissionsToAdd = Arrays.asList("system:settings", "provider:read");

        // Add permissions to role
        RolePermissionsUpdate update = new RolePermissionsUpdate();
        update.setPermissions(permissionsToAdd);
        Response addResponse = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body(update)
            .when()
            .post("/roles/" + roleId + "/permissions");
        assertStatusCode(addResponse, 200);

        // Verify permissions were added
        Response listResponse = RoleTestUtil.listRolePermissions(roleId);
        assertStatusCode(listResponse, 200);
        listResponse.then()
            .body("items.size()", greaterThan(0));

        // Cleanup
        assertStatusCode(RoleTestUtil.deleteRole(roleId), 204);
    }

    @Test
    public void testRemovePermissionsFromRole() {
        // Create a role with permissions
        String roleName = RoleTestUtil.generateUniqueRoleName();
        List<String> permissionsToAdd = Arrays.asList("system:settings", "provider:read");

        Response createResponse = RoleTestUtil.createRole(roleName, "Test role for permission removal", "system", null, permissionsToAdd);
        assertStatusCode(createResponse, 201);
        String roleId = createResponse.jsonPath().getString("id");

        // Remove permissions from role
        Response removeResponse = RoleTestUtil.removeRolePermissions(roleId, permissionsToAdd);
        assertStatusCode(removeResponse, 200);

        // Verify permissions were removed
        Response listResponse = RoleTestUtil.listRolePermissions(roleId);
        assertStatusCode(listResponse, 200);
        listResponse.then()
            .body("items.size()", equalTo(0));

        // Cleanup
        assertStatusCode(RoleTestUtil.deleteRole(roleId), 204);
    }

    @Test
    public void testCreateRoleBindings() {
        // Create a role first
        String roleName = RoleTestUtil.generateUniqueRoleName();
        Response createResponse = RoleTestUtil.createRole(roleName, "Test role for bindings", "system");
        assertStatusCode(createResponse, 201);
        String roleId = createResponse.jsonPath().getString("id");

        // Create role bindings
        String userId1 = RoleBindingTestUtil.generateUniqueUserId();
        String userId2 = RoleBindingTestUtil.generateUniqueUserId();
        List<String> userIds = Arrays.asList(userId1, userId2);

        Response bindingResponse = RoleBindingTestUtil.createRoleBindings(roleId, userIds, "system", null);
        assertStatusCode(bindingResponse, 201);
        bindingResponse.then()
            .body("items.size()", equalTo(2));

        // Verify bindings were created
        Response listResponse = RoleBindingTestUtil.listRoleBindings(roleId);
        assertStatusCode(listResponse, 200);
        listResponse.then()
            .body("items.size()", equalTo(2));

        // Cleanup - delete role (should cascade delete bindings)
        assertStatusCode(RoleTestUtil.deleteRole(roleId), 204);
    }

    @Test
    public void testBulkCreateRoleBindings() {
        // Create two roles first
        String roleName1 = RoleTestUtil.generateUniqueRoleName();
        String roleName2 = RoleTestUtil.generateUniqueRoleName();

        Response createResponse1 = RoleTestUtil.createRole(roleName1, "Test role 1 for bulk bindings", "system");
        assertStatusCode(createResponse1, 201);
        String roleId1 = createResponse1.jsonPath().getString("id");

        Response createResponse2 = RoleTestUtil.createRole(roleName2, "Test role 2 for bulk bindings", "system");
        assertStatusCode(createResponse2, 201);
        String roleId2 = createResponse2.jsonPath().getString("id");

        List<String> roleIds = Arrays.asList(roleId1, roleId2);

        // Create bulk role bindings
        String userId1 = RoleBindingTestUtil.generateUniqueUserId();
        String userId2 = RoleBindingTestUtil.generateUniqueUserId();
        List<String> userIds = Arrays.asList(userId1, userId2);

        Response bulkResponse = RoleBindingTestUtil.bulkCreateRoleBindings(roleIds, userIds, "system", null);
        assertStatusCode(bulkResponse, 201);
        bulkResponse.then()
            .body("items.size()", equalTo(4)); // 2 roles * 2 users

        // Cleanup
        assertStatusCode(RoleTestUtil.deleteRole(roleId1), 204);
        assertStatusCode(RoleTestUtil.deleteRole(roleId2), 204);
    }

    @Test
    public void testListUserBindings() {
        // Create a role first
        String roleName = RoleTestUtil.generateUniqueRoleName();
        Response createResponse = RoleTestUtil.createRole(roleName, "Test role for user bindings", "system");
        assertStatusCode(createResponse, 201);
        String roleId = createResponse.jsonPath().getString("id");

        // Create role binding for a user
        String userId = RoleBindingTestUtil.generateUniqueUserId();
        List<String> userIds = Arrays.asList(userId);

        Response bindingResponse = RoleBindingTestUtil.createRoleBindings(roleId, userIds, "system", null);
        assertStatusCode(bindingResponse, 201);

        // List user bindings
        Response listResponse = RoleBindingTestUtil.listUserBindings(userId);
        assertStatusCode(listResponse, 200);
        listResponse.then()
            .body("items.size()", equalTo(1))
            .body("items[0].role_id", equalTo(roleId))
            .body("items[0].subject_id", equalTo(userId));

        // Cleanup
        assertStatusCode(RoleTestUtil.deleteRole(roleId), 204);
    }

    @Test
    public void testUpdateRoleBinding() {
        // Create two roles first
        String roleName1 = RoleTestUtil.generateUniqueRoleName();
        String roleName2 = RoleTestUtil.generateUniqueRoleName();

        Response createResponse1 = RoleTestUtil.createRole(roleName1, "Test role 1 for binding update", "system");
        assertStatusCode(createResponse1, 201);
        String roleId1 = createResponse1.jsonPath().getString("id");

        Response createResponse2 = RoleTestUtil.createRole(roleName2, "Test role 2 for binding update", "system");
        assertStatusCode(createResponse2, 201);
        String roleId2 = createResponse2.jsonPath().getString("id");

        // Create role binding
        String userId = RoleBindingTestUtil.generateUniqueUserId();
        List<String> userIds = Arrays.asList(userId);

        Response bindingResponse = RoleBindingTestUtil.createRoleBindings(roleId1, userIds, "system", null);
        assertStatusCode(bindingResponse, 201);
        String bindingId = bindingResponse.jsonPath().getString("items[0].id");

        // Update the binding to use the second role
        Response updateResponse = RoleBindingTestUtil.updateRoleBinding(bindingId, roleId2);
        assertStatusCode(updateResponse, 200);
        updateResponse.then()
            .body("role_id", equalTo(roleId2))
            .body("subject_id", equalTo(userId));

        // Verify the update
        Response verifyResponse = RoleBindingTestUtil.getRoleBinding(bindingId);
        assertStatusCode(verifyResponse, 200);
        verifyResponse.then()
            .body("role_id", equalTo(roleId2));

        // Cleanup
        assertStatusCode(RoleTestUtil.deleteRole(roleId1), 204);
        assertStatusCode(RoleTestUtil.deleteRole(roleId2), 204);
    }

    @Test
    public void testDeleteRoleBinding() {
        // Create a role first
        String roleName = RoleTestUtil.generateUniqueRoleName();
        Response createResponse = RoleTestUtil.createRole(roleName, "Test role for binding deletion", "system");
        assertStatusCode(createResponse, 201);
        String roleId = createResponse.jsonPath().getString("id");

        // Create role binding
        String userId = RoleBindingTestUtil.generateUniqueUserId();
        List<String> userIds = Arrays.asList(userId);

        Response bindingResponse = RoleBindingTestUtil.createRoleBindings(roleId, userIds, "system", null);
        assertStatusCode(bindingResponse, 201);
        String bindingId = bindingResponse.jsonPath().getString("items[0].id");

        // Delete the binding
        Response deleteResponse = RoleBindingTestUtil.deleteRoleBinding(bindingId);
        assertStatusCode(deleteResponse, 204);

        // Verify it's deleted
        Response verifyResponse = RoleBindingTestUtil.getRoleBinding(bindingId);
        assertStatusCode(verifyResponse, 404);

        // Cleanup
        assertStatusCode(RoleTestUtil.deleteRole(roleId), 204);
    }
}

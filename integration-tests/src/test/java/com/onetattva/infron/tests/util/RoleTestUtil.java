package com.onetattva.infron.tests.util;

import tools.jackson.databind.ObjectMapper;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.tests.InfronEnvironment;
import io.restassured.response.Response;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;

public class RoleTestUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static InfronEnvironment environment;
    private static String baseUrl;
    private static String accessToken;

    public static void setup(InfronEnvironment env, String token) {
        environment = env;
        baseUrl = env.getCoreServicesUrl();
        accessToken = token;
    }

    public static Response createRole(String name, String description, String scopeType, String scopeId, List<String> permissionIds) {
        try {
            RoleCreate role = new RoleCreate();
            role.setName(name);
            role.setDescription(description);
            role.setScopeType(scopeType);
            role.setScopeId(scopeId == null ? null : UUID.fromString(scopeId));
            role.setPermissions(permissionIds);
            return given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(objectMapper.writeValueAsString(role))
                .when()
                .post(baseUrl + "/roles");
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize role", e);
        }
    }

    public static Response createRole(String name, String description, String scopeType) {
        return createRole(name, description, scopeType, null, null);
    }

    public static Response createRole(String name, String description, String scopeType, String scopeId) {
        return createRole(name, description, scopeType, scopeId, null);
    }

    public static Response listRoles() {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get(baseUrl + "/roles");
    }

    public static Response getRole(String roleId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get(baseUrl + "/roles/" + roleId);
    }

    public static Response updateRole(String roleId, String name, String description) {
        try {
            RoleUpdate update = new RoleUpdate();
            update.setName(name);
            update.setDescription(description);
            return given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(objectMapper.writeValueAsString(update))
                .when()
                .put(baseUrl + "/roles/" + roleId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize role update", e);
        }
    }

    public static Response deleteRole(String roleId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .delete(baseUrl + "/roles/" + roleId);
    }

    public static Response listRolePermissions(String roleId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get(baseUrl + "/roles/" + roleId + "/permissions");
    }

    public static Response addRolePermissions(String roleId, List<String> permissionActions) {
        try {
            RolePermissionsUpdate update = new RolePermissionsUpdate();
            update.setPermissions(permissionActions);
            return given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(objectMapper.writeValueAsString(update))
                .when()
                .post(baseUrl + "/roles/" + roleId + "/permissions");
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize role permissions update", e);
        }
    }

    public static Response removeRolePermissions(String roleId, List<String> permissionActions) {
        try {
            RolePermissionsUpdate update = new RolePermissionsUpdate();
            update.setPermissions(permissionActions);
            return given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(objectMapper.writeValueAsString(update))
                .when()
                .delete(baseUrl + "/roles/" + roleId + "/permissions");
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize role permissions update", e);
        }
    }

    public static Response createTenantRole(String tenantId, String name, String description, String scopeType, String scopeId, List<String> permissionIds) {
        try {
            RoleCreate role = new RoleCreate();
            role.setName(name);
            role.setDescription(description);
            role.setScopeType(scopeType);
            role.setScopeId(scopeId == null ? null : UUID.fromString(scopeId));
            role.setPermissions(permissionIds);
            return given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(objectMapper.writeValueAsString(role))
                .when()
                .post(baseUrl + "/tenants/" + tenantId + "/roles");
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize tenant role", e);
        }
    }

    public static Response createTenantRole(String tenantId, String name, String description, String scopeType, String scopeId) {
        return createTenantRole(tenantId, name, description, scopeType, scopeId, null);
    }

    public static Response listTenantRoles(String tenantId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get(baseUrl + "/tenants/" + tenantId + "/roles");
    }

    public static Response getTenantRole(String tenantId, String roleId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get(baseUrl + "/tenants/" + tenantId + "/roles/" + roleId);
    }

    public static Response updateTenantRole(String tenantId, String roleId, String name, String description) {
        try {
            RoleUpdate update = new RoleUpdate();
            update.setName(name);
            update.setDescription(description);
            return given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(objectMapper.writeValueAsString(update))
                .when()
                .put(baseUrl + "/tenants/" + tenantId + "/roles/" + roleId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize tenant role update", e);
        }
    }

    public static Response deleteTenantRole(String tenantId, String roleId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .delete(baseUrl + "/tenants/" + tenantId + "/roles/" + roleId);
    }

    public static Response listTenantRolePermissions(String tenantId, String roleId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get(baseUrl + "/tenants/" + tenantId + "/roles/" + roleId + "/permissions");
    }

    public static Response addTenantRolePermissions(String tenantId, String roleId, List<String> permissionActions) {
        try {
            RolePermissionsUpdate update = new RolePermissionsUpdate();
            update.setPermissions(permissionActions);
            return given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(objectMapper.writeValueAsString(update))
                .when()
                .post(baseUrl + "/tenants/" + tenantId + "/roles/" + roleId + "/permissions");
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize tenant role permissions update", e);
        }
    }

    public static Response removeTenantRolePermissions(String tenantId, String roleId, List<String> permissionActions) {
        try {
            RolePermissionsUpdate update = new RolePermissionsUpdate();
            update.setPermissions(permissionActions);
            return given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(objectMapper.writeValueAsString(update))
                .when()
                .delete(baseUrl + "/tenants/" + tenantId + "/roles/" + roleId + "/permissions");
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize tenant role permissions update", e);
        }
    }

    public static Response listPermissions() {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get(baseUrl + "/permissions");
    }

    public static String generateUniqueRoleName() {
        return "test-role-" + UUID.randomUUID().toString().substring(0, 8);
    }
}

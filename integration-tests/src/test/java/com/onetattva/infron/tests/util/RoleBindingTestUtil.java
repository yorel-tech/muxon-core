package com.onetattva.infron.tests.util;

import com.onetattva.infron.tests.InfronEnvironment;
import io.restassured.response.Response;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;

public class RoleBindingTestUtil {

    private static InfronEnvironment environment;
    private static String baseUrl;
    private static String accessToken;

    public static void setup(InfronEnvironment env, String token) {
        environment = env;
        baseUrl = env.getCoreServicesUrl();
        accessToken = token;
    }

    public static Response createRoleBindings(String roleId, List<String> userIds, String scopeType, String scopeId) {
        String bindings = userIds.stream()
            .map(userId -> """
                {
                    "role_id": "%s",
                    "subject_type": "user",
                    "subject_id": "%s",
                    "scope_type": "%s",
                    "scope_id": %s
                }
                """.formatted(roleId, userId, scopeType, scopeId != null ? "\"" + scopeId + "\"" : null))
            .reduce((a, b) -> a + "," + b)
            .orElse("");

        return given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body("""
                {
                    "bindings": [%s]
                }
                """.formatted(bindings))
            .when()
            .post(baseUrl + "/roles/" + roleId + "/bindings");
    }

    public static Response createTenantRoleBindings(String tenantId, String roleId, List<String> userIds, String scopeType, String scopeId) {
        String bindings = userIds.stream()
            .map(userId -> """
                {
                    "role_id": "%s",
                    "subject_type": "user",
                    "subject_id": "%s",
                    "scope_type": "%s",
                    "scope_id": %s
                }
                """.formatted(roleId, userId, scopeType, scopeId != null ? "\"" + scopeId + "\"" : null))
            .reduce((a, b) -> a + "," + b)
            .orElse("");

        return given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body("""
                {
                    "bindings": [%s]
                }
                """.formatted(bindings))
            .when()
            .post(baseUrl + "/tenants/" + tenantId + "/roles/" + roleId + "/bindings");
    }

    public static Response bulkCreateRoleBindings(List<String> roleIds, List<String> userIds, String scopeType, String scopeId) {
        String bindings = roleIds.stream()
            .flatMap(roleId -> userIds.stream()
                .map(userId -> """
                    {
                        "role_id": "%s",
                        "subject_type": "user",
                        "subject_id": "%s",
                        "scope_type": "%s",
                        "scope_id": %s
                    }
                    """.formatted(roleId, userId, scopeType, scopeId != null ? "\"" + scopeId + "\"" : null)))
            .reduce((a, b) -> a + "," + b)
            .orElse("");

        return given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body("""
                {
                    "bindings": [%s]
                }
                """.formatted(bindings))
            .when()
            .post(baseUrl + "/role-bindings");
    }

    public static Response listRoleBindings(String roleId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get(baseUrl + "/roles/" + roleId + "/bindings");
    }

    public static Response listTenantRoleBindings(String tenantId, String roleId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get(baseUrl + "/tenants/" + tenantId + "/roles/" + roleId + "/bindings");
    }

    public static Response listUserBindings(String userId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get(baseUrl + "/users/" + userId + "/bindings");
    }

    public static Response getRoleBinding(String bindingId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get(baseUrl + "/role-bindings/" + bindingId);
    }

    public static Response updateRoleBinding(String bindingId, String newRoleId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body("""
                {
                    "role_id": "%s"
                }
                """.formatted(newRoleId))
            .when()
            .put(baseUrl + "/role-bindings/" + bindingId);
    }

    public static Response deleteRoleBinding(String bindingId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .delete(baseUrl + "/role-bindings/" + bindingId);
    }

    public static String generateUniqueUserId() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8);
    }
}

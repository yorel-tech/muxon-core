package com.sal.muxon.tests.util;

import com.sal.muxon.tests.MuxonEnvironment;
import io.restassured.response.Response;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * OSS core exposes read-only roles from {@code RoleRegistry} (no role CRUD or bindings in core).
 */
public final class RoleTestUtil {

    private static String baseUrl;
    private static String accessToken;

    private RoleTestUtil() {
    }

    public static void setup(MuxonEnvironment env, String token) {
        baseUrl = env.getCoreServicesUrl();
        accessToken = token;
    }

    public static Response listRoles() {
        return given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(baseUrl + "/roles");
    }

    public static Response listTenantRoles(String tenantId) {
        return given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(baseUrl + "/tenants/" + tenantId + "/roles");
    }

    public static Response getMyCapabilities() {
        return given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(baseUrl + "/me/capabilities");
    }

    public static String generateUniqueRoleName() {
        return "test-role-" + UUID.randomUUID().toString().substring(0, 8);
    }
}

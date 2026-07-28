package com.scal.muxon.tests.tenant;

import com.scal.muxon.tests.BaseIntegrationTest;
import com.scal.muxon.tests.util.RoleTestUtil;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tenant-scoped role listing in OSS is registry-backed (read-only). Custom roles live in muxon-nexus.
 */
public class RoleTests extends BaseIntegrationTest {

    private static String tenantId;

    @BeforeAll
    public static void setup() {
        accessToken = getAccessToken();
        RoleTestUtil.setup(environment, accessToken);
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
    public void testListTenantRoles_returnsBuiltInRoles() {
        RoleTestUtil.listTenantRoles(tenantId)
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("total", equalTo(4))
                .body("items.find { it.name == 'workload:user' }", notNullValue());
    }
}

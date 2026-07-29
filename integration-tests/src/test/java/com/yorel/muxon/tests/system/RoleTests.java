package com.yorel.muxon.tests.system;

import com.yorel.muxon.tests.BaseIntegrationTest;
import com.yorel.muxon.tests.util.RoleTestUtil;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * OSS {@code /roles} and {@code /tenants/{id}/roles} are read-only (registry-backed).
 * Mutable RBAC is exercised in muxon-nexus integration tests.
 */
public class RoleTests extends BaseIntegrationTest {

    @BeforeAll
    public static void setup() {
        accessToken = getAccessToken();
        RoleTestUtil.setup(environment, accessToken);
    }

    @Test
    public void testListRoles_returnsBuiltInRegistryRoles() {
        Response response = RoleTestUtil.listRoles();
        assertStatusCode(response, 200);
        response.then()
                .body("items", notNullValue())
                .body("total", equalTo(4))
                .body("items.find { it.name == 'system:admin' }", notNullValue())
                .body("items.find { it.name == 'tenant:admin' }", notNullValue())
                .body("items.find { it.name == 'workload:operator' }", notNullValue())
                .body("items.find { it.name == 'workload:user' }", notNullValue())
                .body("items.every { it.immutable == true }", is(true));
    }

    @Test
    public void testListTenantRoles_returnsRegistryRoles() {
        Response tenants = given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/tenants");
        assertStatusCode(tenants, 200);
        String tenantId = tenants.jsonPath().getString("items[0].id");

        Response response = RoleTestUtil.listTenantRoles(tenantId);
        assertStatusCode(response, 200);
        response.then()
                .body("items", notNullValue())
                .body("total", equalTo(4))
                .body("items.find { it.name == 'tenant:admin' }", notNullValue());
    }

    @Test
    public void testGetMyCapabilities_forSystemAdmin() {
        Response response = RoleTestUtil.getMyCapabilities();
        assertStatusCode(response, 200);
        response.then()
                .body("vm", notNullValue())
                .body("vm.view", equalTo(true))
                .body("orchestration", notNullValue())
                .body("orchestration.viewTasks", equalTo(true));
    }
}

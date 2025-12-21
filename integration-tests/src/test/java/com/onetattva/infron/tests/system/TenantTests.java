package com.onetattva.infron.tests.system;

import com.onetattva.infron.tests.InfronEnvironment;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class TenantTests {

    private static String accessToken;
    private static String baseUrl;
    private static InfronEnvironment environment;

    @BeforeAll
    public static void setup() {
        environment = InfronEnvironment.getInstance();
        baseUrl = environment.getCoreServicesUrl();
        RestAssured.baseURI = baseUrl;

        // Get access token
        accessToken = getAccessToken();
    }

    private static String getAccessToken() {
        String keycloakUrl = environment.getKeycloakUrl();
        Response response = given()
            .contentType("application/x-www-form-urlencoded")
            .formParam("grant_type", "password")
            .formParam("client_id", "infron-web")
            .formParam("username", "admin@infron.dev")
            .formParam("password", "Infr0n@1234")
            .when()
            .post(keycloakUrl + "/realms/infron-dev/protocol/openid-connect/token");

        response.then().statusCode(200);
        return response.jsonPath().getString("access_token");
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
}

package com.scal.muxon.tests.tenant;

import com.scal.muxon.tests.BaseIntegrationTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for tenant settings functionality.
 * Tests tenant settings API endpoints including general, security, notifications, and appearance settings.
 */
public class TenantSettingsTests extends BaseIntegrationTest {

    private static String tenantId;

    @BeforeAll
    public static void setup() {
        accessToken = getAccessToken();

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
                    "displayName": "Test Tenant for Settings Tests"
                }
                """.formatted(tenantName))
            .when()
            .post("/tenants");

        response.then().statusCode(201);
        return response.jsonPath().getString("id");
    }

    // ==================== Tenant Settings (All) ====================

    @Test
    public void testGetTenantSettings() {
        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get("/tenants/{tenantId}/settings", tenantId);

        assertStatusCode(response, 200);
        response.then()
            .body("general", notNullValue())
            .body("security", notNullValue())
            .body("notifications", notNullValue())
            .body("appearance", notNullValue())
            .body("createdAt", notNullValue())
            .body("updatedAt", notNullValue());
    }

    @Test
    public void testGetTenantSettingsNotFound() {
        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get("/tenants/{tenantId}/settings", UUID.randomUUID());

        assertStatusCode(response, 404);
    }

    // ==================== Tenant General Settings ====================

    @Test
    public void testGetTenantGeneralSettings() {
        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get("/tenants/{tenantId}/settings/general", tenantId);

        assertStatusCode(response, 200);
        response.then()
            .body("name", notNullValue())
            .body("description", notNullValue())
            .body("defaultTimezone", notNullValue())
            .body("defaultLocale", notNullValue());
    }

    @Test
    public void testUpdateTenantGeneralSettings() {
        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body("""
                {
                    "name": "Tenant Platform",
                    "description": "Tenant-specific platform settings",
                    "contactEmail": "tenant@example.com",
                    "contactPhone": "+1-555-987-6543",
                    "defaultTimezone": "America/New_York",
                    "defaultLocale": "en-US"
                }
                """)
            .when()
            .put("/tenants/{tenantId}/settings/general", tenantId);

        assertStatusCode(response, 200);
        response.then()
            .body("name", equalTo("Tenant Platform"))
            .body("description", equalTo("Tenant-specific platform settings"))
            .body("contactEmail", equalTo("tenant@example.com"))
            .body("contactPhone", equalTo("+1-555-987-6543"))
            .body("defaultTimezone", equalTo("America/New_York"))
            .body("defaultLocale", equalTo("en-US"));
    }

    @Test
    public void testPatchTenantGeneralSettings() {
        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body("""
                {
                    "name": "Updated Tenant Name"
                }
                """)
            .when()
            .patch("/tenants/{tenantId}/settings/general", tenantId);

        assertStatusCode(response, 200);
        response.then()
            .body("name", equalTo("Updated Tenant Name"));
    }

    // ==================== Tenant Security Settings ====================

    @Test
    public void testGetTenantSecuritySettings() {
        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get("/tenants/{tenantId}/settings/security", tenantId);

        assertStatusCode(response, 200);
        response.then()
            .body("sessionTimeoutMinutes", notNullValue())
            .body("maxLoginAttempts", notNullValue())
            .body("lockoutDurationMinutes", notNullValue())
            .body("passwordMinLength", notNullValue())
            .body("passwordRequireUppercase", notNullValue())
            .body("passwordRequireLowercase", notNullValue())
            .body("passwordRequireDigit", notNullValue())
            .body("passwordRequireSymbol", notNullValue())
            .body("passwordExpiryDays", notNullValue())
            .body("apiRateLimitPerMinute", notNullValue());
    }

    @Test
    public void testUpdateTenantSecuritySettings() {
        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body("""
                {
                    "sessionTimeoutMinutes": 120,
                    "maxLoginAttempts": 3,
                    "lockoutDurationMinutes": 15,
                    "passwordMinLength": 10,
                    "passwordRequireUppercase": true,
                    "passwordRequireLowercase": true,
                    "passwordRequireDigit": true,
                    "passwordRequireSymbol": true,
                    "passwordExpiryDays": 60,
                    "apiRateLimitPerMinute": 50
                }
                """)
            .when()
            .put("/tenants/{tenantId}/settings/security", tenantId);

        assertStatusCode(response, 200);
        response.then()
            .body("sessionTimeoutMinutes", equalTo(120))
            .body("maxLoginAttempts", equalTo(3))
            .body("lockoutDurationMinutes", equalTo(15))
            .body("passwordMinLength", equalTo(10))
            .body("passwordRequireUppercase", equalTo(true))
            .body("passwordRequireLowercase", equalTo(true))
            .body("passwordRequireDigit", equalTo(true))
            .body("passwordRequireSymbol", equalTo(true))
            .body("passwordExpiryDays", equalTo(60))
            .body("apiRateLimitPerMinute", equalTo(50));
    }

    @Test
    public void testPatchTenantSecuritySettings() {
        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body("""
                {
                    "sessionTimeoutMinutes": 180
                }
                """)
            .when()
            .patch("/tenants/{tenantId}/settings/security", tenantId);

        assertStatusCode(response, 200);
        response.then()
            .body("sessionTimeoutMinutes", equalTo(180));
    }

    // ==================== Tenant Notification Settings ====================

    @Test
    public void testGetTenantNotificationSettings() {
        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get("/tenants/{tenantId}/settings/notifications", tenantId);

        assertStatusCode(response, 200);
        response.then()
            .body("smtpEnabled", notNullValue())
            .body("smtpPort", notNullValue())
            .body("smtpUseTls", notNullValue())
            .body("webhookEnabled", notNullValue());
    }

    @Test
    public void testUpdateTenantNotificationSettings() {
        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body("""
                {
                    "smtpEnabled": false,
                    "smtpHost": "smtp.tenant.com",
                    "smtpPort": 25,
                    "smtpUsername": "noreply@tenant.com",
                    "smtpPassword": "tenant-password",
                    "smtpFromEmail": "noreply@tenant.com",
                    "smtpUseTls": false,
                    "slackWebhookUrl": "https://hooks.slack.com/services/tenant/...",
                    "webhookEnabled": false
                }
                """)
            .when()
            .put("/tenants/{tenantId}/settings/notifications", tenantId);

        assertStatusCode(response, 200);
        response.then()
            .body("smtpEnabled", equalTo(false))
            .body("smtpHost", equalTo("smtp.tenant.com"))
            .body("smtpPort", equalTo(25))
            .body("smtpUsername", equalTo("noreply@tenant.com"))
            .body("smtpFromEmail", equalTo("noreply@tenant.com"))
            .body("smtpUseTls", equalTo(false))
            .body("webhookEnabled", equalTo(false));
    }

    @Test
    public void testPatchTenantNotificationSettings() {
        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body("""
                {
                    "smtpEnabled": true
                }
                """)
            .when()
            .patch("/tenants/{tenantId}/settings/notifications", tenantId);

        assertStatusCode(response, 200);
        response.then()
            .body("smtpEnabled", equalTo(true));
    }

    // ==================== Tenant Appearance Settings ====================

    @Test
    public void testGetTenantAppearanceSettings() {
        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get("/tenants/{tenantId}/settings/appearance", tenantId);

        assertStatusCode(response, 200);
        response.then()
            .body("theme", notNullValue())
            .body("primaryColor", notNullValue())
            .body("secondaryColor", notNullValue());
    }

    @Test
    public void testUpdateTenantAppearanceSettings() {
        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body("""
                {
                    "theme": "dark",
                    "logoUrl": "https://cdn.tenant.com/logo.png",
                    "faviconUrl": "https://cdn.tenant.com/favicon.ico",
                    "primaryColor": "#ff6b6b",
                    "secondaryColor": "#4ecdc4",
                    "customCssUrl": "https://cdn.tenant.com/custom.css"
                }
                """)
            .when()
            .put("/tenants/{tenantId}/settings/appearance", tenantId);

        assertStatusCode(response, 200);
        response.then()
            .body("theme", equalTo("dark"))
            .body("logoUrl", equalTo("https://cdn.tenant.com/logo.png"))
            .body("faviconUrl", equalTo("https://cdn.tenant.com/favicon.ico"))
            .body("primaryColor", equalTo("#ff6b6b"))
            .body("secondaryColor", equalTo("#4ecdc4"))
            .body("customCssUrl", equalTo("https://cdn.tenant.com/custom.css"));
    }

    @Test
    public void testPatchTenantAppearanceSettings() {
        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body("""
                {
                    "theme": "auto"
                }
                """)
            .when()
            .patch("/tenants/{tenantId}/settings/appearance", tenantId);

        assertStatusCode(response, 200);
        response.then()
            .body("theme", equalTo("auto"));
    }

    // ==================== Tenant Settings Inheritance Tests ====================

    @Test
    public void testTenantSettingsInheritFromSystemDefaults() {
        // Get tenant general settings before any updates
        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get("/tenants/{tenantId}/settings/general", tenantId);

        assertStatusCode(response, 200);
        // Tenant settings should have values (either inherited or set)
        response.then()
            .body("name", notNullValue())
            .body("defaultTimezone", notNullValue())
            .body("defaultLocale", notNullValue());
    }

    @Test
    public void testMultipleTenantsHaveIndependentSettings() {
        // Create a second tenant
        String tenantName2 = "test-tenant-2-" + UUID.randomUUID().toString().substring(0, 8);
        Response createResponse = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body("""
                {
                    "name": "%s",
                    "displayName": "Second Test Tenant"
                }
                """.formatted(tenantName2))
            .when()
            .post("/tenants");

        assertStatusCode(createResponse, 201);
        String tenantId2 = createResponse.jsonPath().getString("id");

        try {
            // Update first tenant's general settings
            given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("""
                    {
                        "name": "First Tenant Name"
                    }
                    """)
                .when()
                .put("/tenants/{tenantId}/settings/general", tenantId)
                .then()
                .statusCode(200)
                .body("name", equalTo("First Tenant Name"));

            // Update second tenant's general settings with different value
            given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("""
                    {
                        "name": "Second Tenant Name"
                    }
                    """)
                .when()
                .put("/tenants/{tenantId}/settings/general", tenantId2)
                .then()
                .statusCode(200)
                .body("name", equalTo("Second Tenant Name"));

            // Verify both tenants have different settings
            given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/tenants/{tenantId}/settings/general", tenantId)
                .then()
                .statusCode(200)
                .body("name", equalTo("First Tenant Name"));

            given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/tenants/{tenantId}/settings/general", tenantId2)
                .then()
                .statusCode(200)
                .body("name", equalTo("Second Tenant Name"));
        } finally {
            // Cleanup second tenant
            given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete("/tenants/{tenantId}", tenantId2)
                .then()
                .statusCode(204);
        }
    }
}

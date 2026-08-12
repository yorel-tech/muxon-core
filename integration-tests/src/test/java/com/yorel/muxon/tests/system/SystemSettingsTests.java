/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.tests.system;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.yorel.muxon.tests.BaseIntegrationTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for system settings functionality. Tests system settings API endpoints
 * including general, security, notifications, IdP, and appearance settings.
 */
public class SystemSettingsTests extends BaseIntegrationTest {

  @BeforeAll
  public static void setup() {
    accessToken = getAccessToken();
  }

  // ==================== System Settings (All) ====================

  @Test
  public void testGetSystemSettings() {
    Response response =
        given().header("Authorization", "Bearer " + accessToken).when().get("/system-settings");

    assertStatusCode(response, 200);
    response
        .then()
        .body("general", notNullValue())
        .body("security", notNullValue())
        .body("notifications", notNullValue())
        .body("appearance", notNullValue())
        .body("createdAt", notNullValue())
        .body("updatedAt", notNullValue());
  }

  // ==================== General Settings ====================

  @Test
  public void testGetGeneralSettings() {
    Response response =
        given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get("/system-settings/general");

    assertStatusCode(response, 200);
    response
        .then()
        .body("name", notNullValue())
        .body("description", notNullValue())
        .body("defaultTimezone", notNullValue())
        .body("defaultLocale", notNullValue());
  }

  @Test
  public void testUpdateGeneralSettings() {
    Response response =
        given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body(
                """
                {
                    "name": "Muxon Cloud Platform",
                    "description": "Multi-tenant cloud management platform",
                    "contactEmail": "admin@muxon.example",
                    "contactPhone": "+1-555-123-4567",
                    "defaultTimezone": "UTC",
                    "defaultLocale": "en-US"
                }
                """)
            .when()
            .put("/system-settings/general");

    assertStatusCode(response, 200);
    response
        .then()
        .body("name", equalTo("Muxon Cloud Platform"))
        .body("description", equalTo("Multi-tenant cloud management platform"))
        .body("contactEmail", equalTo("admin@muxon.example"))
        .body("contactPhone", equalTo("+1-555-123-4567"))
        .body("defaultTimezone", equalTo("UTC"))
        .body("defaultLocale", equalTo("en-US"));
  }

  @Test
  public void testPatchGeneralSettings() {
    Response response =
        given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body(
                """
                {
                    "name": "Updated Platform Name"
                }
                """)
            .when()
            .patch("/system-settings/general");

    assertStatusCode(response, 200);
    response.then().body("name", equalTo("Updated Platform Name"));
  }

  // ==================== Security Settings ====================

  @Test
  public void testGetSecuritySettings() {
    Response response =
        given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get("/system-settings/security");

    assertStatusCode(response, 200);
    response
        .then()
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
  public void testUpdateSecuritySettings() {
    Response response =
        given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body(
                """
                {
                    "sessionTimeoutMinutes": 60,
                    "maxLoginAttempts": 5,
                    "lockoutDurationMinutes": 30,
                    "passwordMinLength": 8,
                    "passwordRequireUppercase": true,
                    "passwordRequireLowercase": true,
                    "passwordRequireDigit": true,
                    "passwordRequireSymbol": false,
                    "passwordExpiryDays": 90,
                    "apiRateLimitPerMinute": 100
                }
                """)
            .when()
            .put("/system-settings/security");

    assertStatusCode(response, 200);
    response
        .then()
        .body("sessionTimeoutMinutes", equalTo(60))
        .body("maxLoginAttempts", equalTo(5))
        .body("lockoutDurationMinutes", equalTo(30))
        .body("passwordMinLength", equalTo(8))
        .body("passwordRequireUppercase", equalTo(true))
        .body("passwordRequireLowercase", equalTo(true))
        .body("passwordRequireDigit", equalTo(true))
        .body("passwordRequireSymbol", equalTo(false))
        .body("passwordExpiryDays", equalTo(90))
        .body("apiRateLimitPerMinute", equalTo(100));
  }

  @Test
  public void testPatchSecuritySettings() {
    Response response =
        given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body(
                """
                {
                    "sessionTimeoutMinutes": 90
                }
                """)
            .when()
            .patch("/system-settings/security");

    assertStatusCode(response, 200);
    response.then().body("sessionTimeoutMinutes", equalTo(90));
  }

  // ==================== Notification Settings ====================

  @Test
  public void testGetNotificationSettings() {
    Response response =
        given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get("/system-settings/notifications");

    assertStatusCode(response, 200);
    response
        .then()
        .body("smtpEnabled", notNullValue())
        .body("smtpPort", notNullValue())
        .body("smtpUseTls", notNullValue())
        .body("webhookEnabled", notNullValue());
  }

  @Test
  public void testUpdateNotificationSettings() {
    Response response =
        given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body(
                """
                {
                    "smtpEnabled": false,
                    "smtpHost": "smtp.gmail.com",
                    "smtpPort": 587,
                    "smtpUsername": "noreply@muxon.example",
                    "smtpPassword": "secret-password",
                    "smtpFromEmail": "noreply@muxon.example",
                    "smtpUseTls": true,
                    "slackWebhookUrl": "https://hooks.slack.com/services/...",
                    "webhookEnabled": false
                }
                """)
            .when()
            .put("/system-settings/notifications");

    assertStatusCode(response, 200);
    response
        .then()
        .body("smtpEnabled", equalTo(false))
        .body("smtpHost", equalTo("smtp.gmail.com"))
        .body("smtpPort", equalTo(587))
        .body("smtpUsername", equalTo("noreply@muxon.example"))
        .body("smtpFromEmail", equalTo("noreply@muxon.example"))
        .body("smtpUseTls", equalTo(true))
        .body("webhookEnabled", equalTo(false));
  }

  @Test
  public void testPatchNotificationSettings() {
    Response response =
        given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body(
                """
                {
                    "smtpEnabled": true
                }
                """)
            .when()
            .patch("/system-settings/notifications");

    assertStatusCode(response, 200);
    response.then().body("smtpEnabled", equalTo(true));
  }

  @Test
  public void testSendTestEmail() {
    Response response =
        given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body(
                """
                {
                    "to": "test@example.com",
                    "subject": "Muxon Test Email",
                    "body": "This is a test email from Muxon"
                }
                """)
            .when()
            .post("/system-settings/notifications/test-email");

    assertStatusCode(response, 200);
  }

  // ==================== IdP Settings ====================

  @Test
  public void testGetIdpSettings() {
    Response response =
        given().header("Authorization", "Bearer " + accessToken).when().get("/system-settings/idp");

    assertStatusCode(response, 200);
    response.then().body("enabled", notNullValue()).body("type", notNullValue());
  }

  @Test
  public void testUpdateIdpSettings() {
    Response response =
        given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body(
                """
                {
                    "enabled": false,
                    "type": "oidc",
                    "name": "Keycloak",
                    "issuerUrl": "https://keycloak.example.com/realms/muxon",
                    "clientId": "muxon-client",
                    "clientSecret": "client-secret",
                    "scopes": "openid,profile,email",
                    "autoProvisionUsers": true,
                    "jwksUri": "https://keycloak.example.com/realms/muxon/protocol/openid-connect/jwks",
                    "userinfoEndpoint": "https://keycloak.example.com/realms/muxon/protocol/openid-connect/userinfo"
                }
                """)
            .when()
            .put("/system-settings/idp");

    assertStatusCode(response, 200);
    response
        .then()
        .body("enabled", equalTo(false))
        .body("type", equalTo("oidc"))
        .body("name", equalTo("Keycloak"))
        .body("issuerUrl", equalTo("https://keycloak.example.com/realms/muxon"))
        .body("clientId", equalTo("muxon-client"))
        .body("autoProvisionUsers", equalTo(true));
  }

  @Test
  public void testPatchIdpSettings() {
    Response response =
        given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body(
                """
                {
                    "enabled": true
                }
                """)
            .when()
            .patch("/system-settings/idp");

    assertStatusCode(response, 200);
    response.then().body("enabled", equalTo(true));
  }

  @Test
  public void testValidateIdp() {
    Response response =
        given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body(
                """
                {
                    "issuerUrl": "https://keycloak.example.com/realms/muxon",
                    "clientId": "muxon-client",
                    "clientSecret": "client-secret"
                }
                """)
            .when()
            .post("/system-settings/idp/validate");

    // This may fail if the test issuer doesn't exist, but we test the endpoint
    response.then().statusCode(anyOf(is(200), is(400)));
  }

  @Test
  public void testDisableIdp() {
    Response response =
        given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .delete("/system-settings/idp");

    assertStatusCode(response, 204);
  }

  // ==================== Appearance Settings ====================

  @Test
  public void testGetAppearanceSettings() {
    Response response =
        given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get("/system-settings/appearance");

    assertStatusCode(response, 200);
    response
        .then()
        .body("theme", notNullValue())
        .body("primaryColor", notNullValue())
        .body("secondaryColor", notNullValue());
  }

  @Test
  public void testUpdateAppearanceSettings() {
    Response response =
        given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body(
                """
                {
                    "theme": "light",
                    "logoUrl": "https://cdn.example.com/logo.png",
                    "faviconUrl": "https://cdn.example.com/favicon.ico",
                    "primaryColor": "#3b82f6",
                    "secondaryColor": "#64748b",
                    "customCssUrl": "https://cdn.example.com/custom.css"
                }
                """)
            .when()
            .put("/system-settings/appearance");

    assertStatusCode(response, 200);
    response
        .then()
        .body("theme", equalTo("light"))
        .body("logoUrl", equalTo("https://cdn.example.com/logo.png"))
        .body("faviconUrl", equalTo("https://cdn.example.com/favicon.ico"))
        .body("primaryColor", equalTo("#3b82f6"))
        .body("secondaryColor", equalTo("#64748b"))
        .body("customCssUrl", equalTo("https://cdn.example.com/custom.css"));
  }

  @Test
  public void testPatchAppearanceSettings() {
    Response response =
        given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body(
                """
                {
                    "theme": "dark"
                }
                """)
            .when()
            .patch("/system-settings/appearance");

    assertStatusCode(response, 200);
    response.then().body("theme", equalTo("dark"));
  }
}

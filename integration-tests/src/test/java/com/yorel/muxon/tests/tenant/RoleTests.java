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
package com.yorel.muxon.tests.tenant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.yorel.muxon.tests.BaseIntegrationTest;
import com.yorel.muxon.tests.util.RoleTestUtil;
import io.restassured.response.Response;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tenant-scoped role listing in OSS is registry-backed (read-only). Custom roles live in
 * muxon-nexus.
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

    Response response =
        given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body(
                """
                        {
                            "name": "%s",
                            "displayName": "Test Tenant for Role Tests"
                        }
                        """
                    .formatted(tenantName))
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

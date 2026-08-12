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
package com.yorel.muxon.tests.util;

import static io.restassured.RestAssured.given;

import com.yorel.muxon.tests.MuxonEnvironment;
import io.restassured.response.Response;
import java.util.UUID;

/**
 * OSS core exposes read-only roles from {@code RoleRegistry} (no role CRUD or bindings in core).
 */
public final class RoleTestUtil {

  private static String baseUrl;
  private static String accessToken;

  private RoleTestUtil() {}

  public static void setup(MuxonEnvironment env, String token) {
    baseUrl = env.getCoreServicesUrl();
    accessToken = token;
  }

  public static Response listRoles() {
    return given().header("Authorization", "Bearer " + accessToken).when().get(baseUrl + "/roles");
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

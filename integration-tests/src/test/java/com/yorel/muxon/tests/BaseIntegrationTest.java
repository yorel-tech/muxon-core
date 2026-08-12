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
package com.yorel.muxon.tests;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.Container;

public abstract class BaseIntegrationTest {

  protected static String accessToken;
  protected static String baseUrl;
  protected static MuxonEnvironment environment;

  @BeforeAll
  public static void setupBase() throws Exception {
    environment = MuxonEnvironment.getInstance();
    environment.startInfrastructure();
    baseUrl = environment.getCoreServicesUrl();
    RestAssured.baseURI = baseUrl;
  }

  /**
   * Get access token for the given username and password
   *
   * @param username the username to authenticate
   * @param password the password to use
   * @return the access token
   */
  protected static String getAccessToken(String username, String password) {
    String tokenUrl = "http://keycloak:8085/realms/muxon-dev/protocol/openid-connect/token";
    String[] command = {
      "curl",
      "-s",
      "-X",
      "POST",
      tokenUrl,
      "-H",
      "Content-Type: " + RestConstants.CONTENT_TYPE_FORM_URLENCODED,
      "-d",
      "grant_type=" + RestConstants.GRANT_TYPE,
      "-d",
      "client_id=" + RestConstants.CLIENT_ID,
      "-d",
      "username=" + username,
      "-d",
      "password=" + password
    };

    Container.ExecResult result = null;
    try {
      result = environment.getCoreServices().execInContainer(command);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    if (result.getExitCode() != 0) {
      throw new RuntimeException("Failed to get access token: " + result.getStderr());
    }

    String output = result.getStdout();
    int start = output.indexOf("\"access_token\":\"") + 16;
    int end = output.indexOf("\"", start);
    return output.substring(start, end);
  }

  /**
   * Get access token using default admin credentials
   *
   * @return the access token
   */
  protected static String getAccessToken() {
    return getAccessToken(RestConstants.DEFAULT_USERNAME, RestConstants.DEFAULT_PASSWORD);
  }

  /**
   * Assert that the response has the expected status code, printing the response body on failure.
   *
   * @param response the response to check
   * @param expectedStatusCode the expected status code
   */
  protected static void assertStatusCode(Response response, int expectedStatusCode) {
    try {
      response.then().statusCode(expectedStatusCode);
    } catch (AssertionError e) {
      System.err.println("Server error response: " + response.getBody().asString());
      throw e;
    }
  }
}

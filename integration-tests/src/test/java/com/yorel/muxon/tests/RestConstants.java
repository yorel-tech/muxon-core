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

public class RestConstants {

  // Content types
  public static final String CONTENT_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded";
  public static final String CONTENT_TYPE_JSON = "application/json";

  // OAuth2 constants
  public static final String GRANT_TYPE = "password";
  public static final String CLIENT_ID = "muxon-web";
  public static final String REALM_PATH = "/realms/muxon-dev/protocol/openid-connect/token";

  // Authorization header
  public static final String AUTHORIZATION_BEARER_PREFIX = "Bearer ";

  // Default credentials
  public static final String DEFAULT_USERNAME = "admin@muxon.dev";
  public static final String DEFAULT_PASSWORD = MuxonEnvironment.DB_PASSWORD;
  public static final String LIMITED_ADMIN_USERNAME = "limited-admin@muxon.dev";
  public static final String LIMITED_ADMIN_PASSWORD = MuxonEnvironment.DB_PASSWORD;
}

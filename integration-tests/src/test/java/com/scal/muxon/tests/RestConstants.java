package com.scal.muxon.tests;

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

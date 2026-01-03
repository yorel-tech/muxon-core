package com.onetattva.infron.tests;

public class RestConstants {

    // Content types
    public static final String CONTENT_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String CONTENT_TYPE_JSON = "application/json";

    // OAuth2 constants
    public static final String GRANT_TYPE = "password";
    public static final String CLIENT_ID = "infron-web";
    public static final String REALM_PATH = "/realms/infron-dev/protocol/openid-connect/token";

    // Authorization header
    public static final String AUTHORIZATION_BEARER_PREFIX = "Bearer ";

    // Default credentials
    public static final String DEFAULT_USERNAME = "admin@infron.dev";
    public static final String DEFAULT_PASSWORD = InfronEnvironment.DB_PASSWORD;
    public static final String LIMITED_ADMIN_USERNAME = "limited-admin@infron.dev";
    public static final String LIMITED_ADMIN_PASSWORD = InfronEnvironment.DB_PASSWORD;
}

package com.onetattva.infron.tests;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;

import static io.restassured.RestAssured.given;

public abstract class BaseIntegrationTest {

    protected static String accessToken;
    protected static String baseUrl;
    protected static InfronEnvironment environment;

    @BeforeAll
    public static void setupBase() throws Exception {
        environment = InfronEnvironment.getInstance();
        environment.startInfrastructure();
        baseUrl = environment.getCoreServicesUrl();
        RestAssured.baseURI = baseUrl;
    }

    /**
     * Get access token for the given username and password
     * @param username the username to authenticate
     * @param password the password to use
     * @return the access token
     */
    protected static String getAccessToken(String username, String password) {
        String keycloakUrl = environment.getKeycloakUrl();
        Response response = given()
            .contentType(RestConstants.CONTENT_TYPE_FORM_URLENCODED)
            .formParam("grant_type", RestConstants.GRANT_TYPE)
            .formParam("client_id", RestConstants.CLIENT_ID)
            .formParam("username", username)
            .formParam("password", password)
            .when()
            .post(keycloakUrl + RestConstants.REALM_PATH);

        response.then().statusCode(200);
        return response.jsonPath().getString("access_token");
    }

    /**
     * Get access token using default admin credentials
     * @return the access token
     */
    protected static String getAccessToken() {
        return getAccessToken(RestConstants.DEFAULT_USERNAME, RestConstants.DEFAULT_PASSWORD);
    }
}

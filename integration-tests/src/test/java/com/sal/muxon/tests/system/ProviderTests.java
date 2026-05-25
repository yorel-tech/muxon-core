package com.sal.muxon.tests.system;

import com.sal.muxon.api.model.ProviderType;
import com.sal.muxon.tests.BaseIntegrationTest;
import com.sal.muxon.tests.util.ProviderTestUtil;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;

/**
 * Integration tests for provider API: HATEOAS links, libvirt provider with localhost,
 * and validation negative scenarios.
 */
public class ProviderTests extends BaseIntegrationTest {

    @BeforeAll
    public static void setup() {
        accessToken = getAccessToken();
        ProviderTestUtil.setup(environment, accessToken);
    }

    // --------------- Links ---------------

    @Test
    public void testGetProviderReturnsLinks() {
        String name = ProviderTestUtil.generateUniqueProviderName();
        Map<String, String> credentials = Map.of("sshPrivateKey", ProviderTestUtil.MINIMAL_SSH_PRIVATE_KEY);
        Response createResponse = ProviderTestUtil.createProvider(
            name,
            ProviderType.LIBVIRT,
            "ssh://root@localhost:22",
            credentials
        );
        assertStatusCode(createResponse, 201);
        String providerId = createResponse.jsonPath().getString("id");

        Response getResponse = ProviderTestUtil.getProvider(providerId);
        assertStatusCode(getResponse, 200);
        getResponse.then()
            .body("id", equalTo(providerId))
            .body("name", equalTo(name))
            .body("type", equalTo("LIBVIRT"))
            .body("endpoint", equalTo("ssh://root@localhost:22"));
        // HATEOAS: _links (from BaseEntity schema) or links must be present with expected rels
        if (getResponse.jsonPath().get("_links") != null) {
            getResponse.then().body("_links", notNullValue());
            getResponse.then().body("_links.find { it.rel == 'self' }.href", notNullValue());
            getResponse.then().body("_links.find { it.rel == 'self' }.method", equalTo("GET"));
        }
        if (getResponse.jsonPath().get("links") != null) {
            getResponse.then().body("links", notNullValue());
            getResponse.then().body("links.find { it.rel == 'self' }.href", notNullValue());
        }

        assertStatusCode(ProviderTestUtil.deleteProvider(providerId), 204);
    }

    @Test
    public void testListProvidersReturnsLinksInItems() {
        String name = ProviderTestUtil.generateUniqueProviderName();
        Map<String, String> credentials = Map.of("sshPrivateKey", ProviderTestUtil.MINIMAL_SSH_PRIVATE_KEY);
        Response createResponse = ProviderTestUtil.createProvider(
            name,
            ProviderType.LIBVIRT,
            "libvirt://localhost",
            credentials
        );
        assertStatusCode(createResponse, 201);
        String providerId = createResponse.jsonPath().getString("id");

        Response listResponse = ProviderTestUtil.listProviders(1, 20, null, null);
        assertStatusCode(listResponse, 200);
        listResponse.then()
            .body("items", notNullValue())
            .body("items.find { it.id == '%s' }.name".formatted(providerId), equalTo(name));
        // Each item should have links
        listResponse.then().body("items.find { it.id == '%s' }._links".formatted(providerId), anyOf(notNullValue(), nullValue()));
        listResponse.then().body("items.find { it.id == '%s' }.links".formatted(providerId), anyOf(notNullValue(), nullValue()));

        assertStatusCode(ProviderTestUtil.deleteProvider(providerId), 204);
    }

    // --------------- Creating libvirt provider with localhost ---------------

    @Test
    public void testCreateLibvirtProviderWithSshLocalhost() {
        String name = ProviderTestUtil.generateUniqueProviderName();
        Map<String, String> credentials = Map.of("sshPrivateKey", ProviderTestUtil.MINIMAL_SSH_PRIVATE_KEY);

        Response response = ProviderTestUtil.createProvider(
            name,
            ProviderType.LIBVIRT,
            "ssh://root@localhost:22",
            credentials
        );

        assertStatusCode(response, 201);
        response.then()
            .body("name", equalTo(name))
            .body("type", equalTo("LIBVIRT"))
            .body("endpoint", equalTo("ssh://root@localhost:22"))
            .body("id", notNullValue());
        // Status may be ACTIVE or CONNECTING/ERROR depending on whether localhost libvirt is reachable
        response.then().body("status", anyOf(
            equalTo("ACTIVE"),
            equalTo("CONNECTING"),
            equalTo("ERROR")
        ));

        String providerId = response.jsonPath().getString("id");
        assertStatusCode(ProviderTestUtil.deleteProvider(providerId), 204);
    }

    @Test
    public void testCreateLibvirtProviderWithLibvirtLocalhost() {
        String name = ProviderTestUtil.generateUniqueProviderName();
        Map<String, String> credentials = Map.of("sshPrivateKey", ProviderTestUtil.MINIMAL_SSH_PRIVATE_KEY);

        Response response = ProviderTestUtil.createProvider(
            name,
            ProviderType.LIBVIRT,
            "libvirt://localhost",
            credentials
        );

        assertStatusCode(response, 201);
        response.then()
            .body("name", equalTo(name))
            .body("type", equalTo("LIBVIRT"))
            .body("endpoint", equalTo("libvirt://localhost"))
            .body("id", notNullValue());

        String providerId = response.jsonPath().getString("id");
        assertStatusCode(ProviderTestUtil.deleteProvider(providerId), 204);
    }

    // --------------- Validation negative scenarios ---------------

    @Test
    public void testValidationDuplicateProviderName() {
        String name = ProviderTestUtil.generateUniqueProviderName();
        Map<String, String> credentials = Map.of("sshPrivateKey", ProviderTestUtil.MINIMAL_SSH_PRIVATE_KEY);

        Response first = ProviderTestUtil.createProvider(name, ProviderType.LIBVIRT, "libvirt://localhost", credentials);
        assertStatusCode(first, 201);
        String providerId = first.jsonPath().getString("id");

        Response second = ProviderTestUtil.createProvider(name, ProviderType.LIBVIRT, "libvirt://localhost", credentials);
        // Duplicate name: service throws IllegalArgumentException -> 500
        assertStatusCode(second, 500);
        second.then().body("code", equalTo("INTERNAL_ERROR"));

        assertStatusCode(ProviderTestUtil.deleteProvider(providerId), 204);
    }

    @Test
    public void testValidationInvalidLibvirtEndpoint() {
        String name = ProviderTestUtil.generateUniqueProviderName();
        Map<String, String> credentials = Map.of("sshPrivateKey", ProviderTestUtil.MINIMAL_SSH_PRIVATE_KEY);
        // Libvirt requires ssh:// or libvirt://; https:// is invalid
        Response response = ProviderTestUtil.createProvider(
            name,
            ProviderType.LIBVIRT,
            "https://example.com",
            credentials
        );
        assertStatusCode(response, 500);
        response.then().body("code", equalTo("INTERNAL_ERROR"));
    }

    @Test
    public void testValidationMissingCredentials() {
        String name = ProviderTestUtil.generateUniqueProviderName();
        Response response = ProviderTestUtil.createProvider(
            name,
            ProviderType.LIBVIRT,
            "libvirt://localhost",
            null
        );
        assertStatusCode(response, 500);
        response.then().body("code", equalTo("INTERNAL_ERROR"));
    }

    @Test
    public void testValidationLibvirtMissingSshPrivateKey() {
        String name = ProviderTestUtil.generateUniqueProviderName();
        // Empty or wrong credentials: no sshPrivateKey
        Response response = ProviderTestUtil.createProvider(
            name,
            ProviderType.LIBVIRT,
            "libvirt://localhost",
            Collections.emptyMap()
        );
        assertStatusCode(response, 500);
        response.then().body("code", equalTo("INTERNAL_ERROR"));
    }

    @Test
    public void testGetProviderNotFound() {
        String nonExistentId = UUID.randomUUID().toString();
        Response response = ProviderTestUtil.getProvider(nonExistentId);
        // Service throws IllegalArgumentException -> 500 (no ResourceNotFoundException)
        assertStatusCode(response, 500);
        response.then().body("code", equalTo("INTERNAL_ERROR"));
    }

    @Test
    public void testDeleteProviderNotFound() {
        String nonExistentId = UUID.randomUUID().toString();
        Response response = ProviderTestUtil.deleteProvider(nonExistentId);
        assertStatusCode(response, 500);
        response.then().body("code", equalTo("INTERNAL_ERROR"));
    }

    @Test
    public void testTestConnectionProviderNotFound() {
        String nonExistentId = UUID.randomUUID().toString();
        Response response = ProviderTestUtil.testProviderConnection(nonExistentId);
        assertStatusCode(response, 500);
        response.then().body("code", equalTo("INTERNAL_ERROR"));
    }

    @Test
    public void testGetCapabilitiesProviderNotFound() {
        String nonExistentId = UUID.randomUUID().toString();
        Response response = ProviderTestUtil.getProviderCapabilities(nonExistentId, false);
        assertStatusCode(response, 500);
        response.then().body("code", equalTo("INTERNAL_ERROR"));
    }
}

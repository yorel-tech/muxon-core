package com.onetattva.infron.core.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onetattva.infron.core.services.model.OidcUserInfo;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

public class OidcUserService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OidcUserService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public List<OidcUserInfo> getUsers(String issuerUri, String clientId, String clientSecret) {
        String accessToken = getAccessToken(issuerUri, clientId, clientSecret);
        String realm = extractRealm(issuerUri);
        String url = issuerUri.replace("/realms/" + realm, "/admin/realms/" + realm + "/users");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        try {
            JsonNode users = objectMapper.readTree(response.getBody());
            List<OidcUserInfo> userInfos = new ArrayList<>();
            for (JsonNode user : users) {
                OidcUserInfo info = new OidcUserInfo();
                info.setSub(user.get("id").asText());
                info.setPreferredUsername(user.get("username").asText());
                info.setEmail(user.get("email") != null ? user.get("email").asText() : null);
                info.setName(user.get("firstName") != null && user.get("lastName") != null ?
                    user.get("firstName").asText() + " " + user.get("lastName").asText() : null);
                userInfos.add(info);
            }
            return userInfos;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse users", e);
        }
    }

    public OidcUserInfo getUserByName(String name, String issuerUri, String clientId, String clientSecret) {
        String accessToken = getAccessToken(issuerUri, clientId, clientSecret);
        String realm = extractRealm(issuerUri);
        String url = issuerUri.replace("/realms/" + realm, "/admin/realms/" + realm + "/users?username=" + name);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        try {
            JsonNode users = objectMapper.readTree(response.getBody());
            if (users.isArray() && users.size() > 0) {
                JsonNode user = users.get(0);
                OidcUserInfo info = new OidcUserInfo();
                info.setSub(user.get("id").asText());
                info.setPreferredUsername(user.get("username").asText());
                info.setEmail(user.get("email") != null ? user.get("email").asText() : null);
                info.setName(user.get("firstName") != null && user.get("lastName") != null ?
                    user.get("firstName").asText() + " " + user.get("lastName").asText() : null);
                return info;
            }
            return null;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse user", e);
        }
    }

    public OidcUserInfo getUserByExternalId(String externalId, String issuerUri, String clientId, String clientSecret) {
        // Assuming externalId is the Keycloak user id
        String accessToken = getAccessToken(issuerUri, clientId, clientSecret);
        String realm = extractRealm(issuerUri);
        String url = issuerUri.replace("/realms/" + realm, "/admin/realms/" + realm + "/users/" + externalId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        try {
            JsonNode user = objectMapper.readTree(response.getBody());
            OidcUserInfo info = new OidcUserInfo();
            info.setSub(user.get("id").asText());
            info.setPreferredUsername(user.get("username").asText());
            info.setEmail(user.get("email") != null ? user.get("email").asText() : null);
            info.setName(user.get("firstName") != null && user.get("lastName") != null ?
                user.get("firstName").asText() + " " + user.get("lastName").asText() : null);
            return info;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse user", e);
        }
    }

    private String getAccessToken(String issuerUri, String clientId, String clientSecret) {
        String tokenUrl = issuerUri + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "client_credentials");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);
        try {
            JsonNode json = objectMapper.readTree(response.getBody());
            return json.get("access_token").asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse token response", e);
        }
    }

    private String extractRealm(String issuerUri) {
        // issuerUri is http://localhost:8085/realms/infron-dev
        String[] parts = issuerUri.split("/realms/");
        return parts[1];
    }
}

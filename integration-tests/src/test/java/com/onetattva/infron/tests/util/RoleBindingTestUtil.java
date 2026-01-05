package com.onetattva.infron.tests.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onetattva.infron.api.model.RoleBindingBulkCreate;
import com.onetattva.infron.api.model.RoleBindingCreateItem;
import com.onetattva.infron.api.model.RoleBindingCreateItem;
import com.onetattva.infron.api.model.RoleBindingUpdate;
import com.onetattva.infron.tests.InfronEnvironment;
import io.restassured.response.Response;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;

public class RoleBindingTestUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static InfronEnvironment environment;
    private static String baseUrl;
    private static String accessToken;

    public static void setup(InfronEnvironment env, String token) {
        environment = env;
        baseUrl = env.getCoreServicesUrl();
        accessToken = token;
    }

    public static Response createRoleBindings(String roleId, List<String> userIds, String scopeType, String scopeId) {
        try {
            RoleBindingBulkCreate bulkCreate = new RoleBindingBulkCreate();
            List<RoleBindingCreateItem> bindings = userIds.stream()
                .map(userId -> {
                    RoleBindingCreateItem binding = new RoleBindingCreateItem();
                    binding.setRoleId(UUID.fromString(roleId));
                    binding.setSubjectType(RoleBindingCreateItem.SubjectTypeEnum.USER);
                    binding.setSubjectId(userId);
                    binding.setScopeType(RoleBindingCreateItem.ScopeTypeEnum.fromValue(scopeType));
                    binding.setScopeId(scopeId == null ? null : UUID.fromString(scopeId));
                    return binding;
                })
                .toList();
            bulkCreate.setBindings(bindings);
            return given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(objectMapper.writeValueAsString(bulkCreate))
                .when()
                .post(baseUrl + "/roles/" + roleId + "/bindings");
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize role bindings", e);
        }
    }

    public static Response createTenantRoleBindings(String tenantId, String roleId, List<String> userIds, String scopeType, String scopeId) {
        try {
            RoleBindingBulkCreate bulkCreate = new RoleBindingBulkCreate();
            List<RoleBindingCreateItem> bindings = userIds.stream()
                .map(userId -> {
                    RoleBindingCreateItem binding = new RoleBindingCreateItem();
                    binding.setRoleId(UUID.fromString(roleId));
                    binding.setSubjectType(RoleBindingCreateItem.SubjectTypeEnum.USER);
                    binding.setSubjectId(userId);
                    binding.setScopeType(RoleBindingCreateItem.ScopeTypeEnum.fromValue(scopeType));
                    binding.setScopeId(scopeId == null ? null : UUID.fromString(scopeId));
                    return binding;
                })
                .toList();
            bulkCreate.setBindings(bindings);
            return given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(objectMapper.writeValueAsString(bulkCreate))
                .when()
                .post(baseUrl + "/tenants/" + tenantId + "/roles/" + roleId + "/bindings");
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize tenant role bindings", e);
        }
    }

    public static Response bulkCreateRoleBindings(List<String> roleIds, List<String> userIds, String scopeType, String scopeId) {
        try {
            RoleBindingBulkCreate bulkCreate = new RoleBindingBulkCreate();
            List<RoleBindingCreateItem> bindings = roleIds.stream()
                .flatMap(roleId -> userIds.stream()
                    .map(userId -> {
                        RoleBindingCreateItem binding = new RoleBindingCreateItem();
                        binding.setRoleId(UUID.fromString(roleId));
                        binding.setSubjectType(RoleBindingCreateItem.SubjectTypeEnum.USER);
                        binding.setSubjectId(userId);
                        binding.setScopeType(RoleBindingCreateItem.ScopeTypeEnum.fromValue(scopeType));
                        binding.setScopeId(scopeId == null ? null : UUID.fromString(scopeId));
                        return binding;
                    }))
                .toList();
            bulkCreate.setBindings(bindings);
            return given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(objectMapper.writeValueAsString(bulkCreate))
                .when()
                .post(baseUrl + "/role-bindings");
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize bulk role bindings", e);
        }
    }

    public static Response listRoleBindings(String roleId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get(baseUrl + "/roles/" + roleId + "/bindings");
    }

    public static Response listTenantRoleBindings(String tenantId, String roleId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get(baseUrl + "/tenants/" + tenantId + "/roles/" + roleId + "/bindings");
    }

    public static Response listUserBindings(String userId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get(baseUrl + "/users/" + userId + "/bindings");
    }

    public static Response getRoleBinding(String bindingId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get(baseUrl + "/role-bindings/" + bindingId);
    }

    public static Response updateRoleBinding(String bindingId, String newRoleId) {
        try {
            RoleBindingUpdate update = new RoleBindingUpdate();
            update.setRoleId(UUID.fromString(newRoleId));
            return given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(objectMapper.writeValueAsString(update))
                .when()
                .put(baseUrl + "/role-bindings/" + bindingId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize role binding update", e);
        }
    }

    public static Response deleteRoleBinding(String bindingId) {
        return given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .delete(baseUrl + "/role-bindings/" + bindingId);
    }

    public static String generateUniqueUserId() {
        return UUID.randomUUID().toString();
    }
}

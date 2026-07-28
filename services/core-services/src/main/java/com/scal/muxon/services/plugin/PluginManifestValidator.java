package com.scal.muxon.services.plugin;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates plugin manifests against the required schema before registration.
 * Returns structured validation errors so callers can return HTTP 400/422 with detail.
 */
@Component
public class PluginManifestValidator {

    private static final Set<String> ALLOWED_CAPABILITY_TYPES = Set.of(
            "runtime", "service", "resource-provider", "ui-extension"
    );

    private static final Set<String> RESERVED_KINDS = Set.of("vm");

    public record ValidationResult(boolean valid, List<String> errors) {}

    /**
     * Validates a parsed manifest map. Expected structure mirrors the PluginManifest OpenAPI schema.
     */
    @SuppressWarnings("unchecked")
    public ValidationResult validate(Map<String, Object> manifest) {
        List<String> errors = new ArrayList<>();

        if (manifest == null) {
            errors.add("manifest must not be null");
            return new ValidationResult(false, errors);
        }

        String apiVersion = (String) manifest.get("apiVersion");
        if (!"platform.io/v1".equals(apiVersion)) {
            errors.add("apiVersion must be 'platform.io/v1', got: " + apiVersion);
        }

        String kind = (String) manifest.get("kind");
        if (!"Plugin".equals(kind)) {
            errors.add("kind must be 'Plugin', got: " + kind);
        }

        Map<String, Object> metadata = (Map<String, Object>) manifest.get("metadata");
        if (metadata == null) {
            errors.add("metadata is required");
        } else {
            if (isBlank(metadata.get("name"))) {
                errors.add("metadata.name is required");
            }
            if (isBlank(metadata.get("version"))) {
                errors.add("metadata.version is required");
            }
        }

        Map<String, Object> spec = (Map<String, Object>) manifest.get("spec");
        if (spec == null) {
            errors.add("spec is required");
        } else {
            validateSpec(spec, errors);
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    @SuppressWarnings("unchecked")
    private void validateSpec(Map<String, Object> spec, List<String> errors) {
        Object capabilitiesObj = spec.get("capabilities");
        if (capabilitiesObj == null) {
            errors.add("spec.capabilities is required and must be a non-empty list");
        } else if (capabilitiesObj instanceof List<?> caps && !caps.isEmpty()) {
            for (int i = 0; i < caps.size(); i++) {
                Map<String, Object> cap = (Map<String, Object>) caps.get(i);
                String type = (String) cap.get("type");
                if (!ALLOWED_CAPABILITY_TYPES.contains(type)) {
                    errors.add("spec.capabilities[" + i + "].type '" + type +
                            "' is not a valid capability type; allowed: " + ALLOWED_CAPABILITY_TYPES);
                }
                if ("resource-provider".equals(type)) {
                    validateResourceProviderCapability(cap, i, errors);
                }
            }
        } else {
            errors.add("spec.capabilities must be a non-empty list");
        }

        boolean hasAddress = hasValue(spec, "grpc.address");
        boolean hasImage = hasValue(spec, "image");

        if (hasAddress && hasImage) {
            errors.add("spec cannot specify both grpc.address (externally-managed) and image (platform-managed)");
        }
    }

    @SuppressWarnings("unchecked")
    private void validateResourceProviderCapability(Map<String, Object> cap, int idx, List<String> errors) {
        Object kindsObj = cap.get("kinds");
        if (kindsObj instanceof List<?> kinds) {
            for (Object k : kinds) {
                if (RESERVED_KINDS.contains(k)) {
                    errors.add("spec.capabilities[" + idx + "] declares reserved kind '" + k +
                            "'; reserved kinds cannot be claimed by plugins");
                }
            }
        }
    }

    private boolean isBlank(Object value) {
        return value == null || value.toString().isBlank();
    }

    @SuppressWarnings("unchecked")
    private boolean hasValue(Map<String, Object> spec, String dotPath) {
        String[] parts = dotPath.split("\\.", 2);
        Object val = spec.get(parts[0]);
        if (val == null) return false;
        if (parts.length == 1) return !isBlank(val);
        if (val instanceof Map) {
            return hasValue((Map<String, Object>) val, parts[1]);
        }
        return false;
    }
}

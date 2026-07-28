package com.scal.muxon.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scal.muxon.customization.crypto.CustomizationSecretEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring service that wraps {@link CustomizationSecretEncryptor} to encrypt password
 * fields in the customization JSON before persisting and decrypt them before handing
 * off to the worker.
 *
 * <p>Password field paths operated on (relative to the root of the customization JSON):
 * <ul>
 *   <li>{@code windows.adminPassword}</li>
 *   <li>{@code windows.domainJoinPassword}</li>
 *   <li>{@code linux.users[].password}</li>
 * </ul>
 *
 * <p>When redacting for logs, these fields are replaced with {@code "[REDACTED]"}.
 */
@Service
public class CustomizationSecretService {

    private static final Logger log = LoggerFactory.getLogger(CustomizationSecretService.class);

    private static final List<String[]> SECRET_PATHS = List.of(
            new String[]{"windows", "adminPassword"},
            new String[]{"windows", "domainJoinPassword"}
    );
    private static final String REDACTED = "[REDACTED]";

    private final CustomizationSecretEncryptor encryptor;
    private final ObjectMapper objectMapper;

    public CustomizationSecretService(
            @Value("${muxon.customization.encryption-key:}") String base64Key,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        if (base64Key == null || base64Key.isBlank()) {
            log.warn("muxon.customization.encryption-key is not set — customization secrets will NOT be encrypted at rest");
            this.encryptor = null;
        } else {
            this.encryptor = new CustomizationSecretEncryptor(base64Key);
        }
    }

    /**
     * Encrypt all secret fields in the customization JSON string.
     * Returns the mutated JSON with encrypted values.
     * If encryption is not configured the original JSON is returned unchanged.
     */
    public String encryptSecrets(String customizationJson) {
        if (encryptor == null || customizationJson == null) return customizationJson;
        try {
            ObjectNode root = (ObjectNode) objectMapper.readTree(customizationJson);
            for (String[] path : SECRET_PATHS) {
                encryptField(root, path);
            }
            // Encrypt per-user passwords inside linux.users[]
            JsonNode linuxNode = root.get("linux");
            if (linuxNode instanceof ObjectNode linux) {
                JsonNode usersNode = linux.get("users");
                if (usersNode != null && usersNode.isArray()) {
                    for (JsonNode userNode : usersNode) {
                        if (userNode instanceof ObjectNode user && user.has("password")) {
                            String plain = user.get("password").asText(null);
                            if (plain != null && !plain.isBlank()) {
                                user.put("password", encryptor.encrypt(plain));
                            }
                        }
                    }
                }
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt customization secrets", e);
        }
    }

    /**
     * Decrypt all secret fields in the customization JSON string.
     * Returns the mutated JSON with decrypted (plaintext) values — for use by the worker only.
     */
    public String decryptSecrets(String customizationJson) {
        if (encryptor == null || customizationJson == null) return customizationJson;
        try {
            ObjectNode root = (ObjectNode) objectMapper.readTree(customizationJson);
            for (String[] path : SECRET_PATHS) {
                decryptField(root, path);
            }
            JsonNode linuxNode = root.get("linux");
            if (linuxNode instanceof ObjectNode linux) {
                JsonNode usersNode = linux.get("users");
                if (usersNode != null && usersNode.isArray()) {
                    for (JsonNode userNode : usersNode) {
                        if (userNode instanceof ObjectNode user && user.has("password")) {
                            String encrypted = user.get("password").asText(null);
                            if (encrypted != null && !encrypted.isBlank()) {
                                user.put("password", encryptor.decrypt(encrypted));
                            }
                        }
                    }
                }
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt customization secrets", e);
        }
    }

    /**
     * Return a log-safe copy of the customization JSON with all secret fields replaced
     * by {@code "[REDACTED]"}.  Never throws — falls back to a static placeholder on error.
     */
    public String redactForLog(String customizationJson) {
        if (customizationJson == null) return null;
        try {
            ObjectNode root = (ObjectNode) objectMapper.readTree(customizationJson);
            for (String[] path : SECRET_PATHS) {
                redactField(root, path);
            }
            JsonNode linuxNode = root.get("linux");
            if (linuxNode instanceof ObjectNode linux) {
                JsonNode usersNode = linux.get("users");
                if (usersNode != null && usersNode.isArray()) {
                    for (JsonNode userNode : usersNode) {
                        if (userNode instanceof ObjectNode user && user.has("password")) {
                            user.put("password", REDACTED);
                        }
                    }
                }
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            return "<customization json redaction failed>";
        }
    }

    // --- helpers ---

    private void encryptField(ObjectNode root, String[] path) {
        JsonNode parent = path.length > 1 ? root.get(path[0]) : root;
        if (!(parent instanceof ObjectNode p)) return;
        String leaf = path[path.length - 1];
        if (p.has(leaf)) {
            String plain = p.get(leaf).asText(null);
            if (plain != null && !plain.isBlank()) {
                p.put(leaf, encryptor.encrypt(plain));
            }
        }
    }

    private void decryptField(ObjectNode root, String[] path) {
        JsonNode parent = path.length > 1 ? root.get(path[0]) : root;
        if (!(parent instanceof ObjectNode p)) return;
        String leaf = path[path.length - 1];
        if (p.has(leaf)) {
            String encrypted = p.get(leaf).asText(null);
            if (encrypted != null && !encrypted.isBlank()) {
                p.put(leaf, encryptor.decrypt(encrypted));
            }
        }
    }

    private void redactField(ObjectNode root, String[] path) {
        JsonNode parent = path.length > 1 ? root.get(path[0]) : root;
        if (!(parent instanceof ObjectNode p)) return;
        String leaf = path[path.length - 1];
        if (p.has(leaf) && !p.get(leaf).isNull()) {
            p.put(leaf, REDACTED);
        }
    }
}

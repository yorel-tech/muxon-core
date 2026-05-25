package com.sal.muxon.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Shared helpers for Infron encryption configuration.
 * Used by both Spring Boot and Quarkus applications to derive
 * the encryption key and initialize EncryptionUtil.
 */
public final class MuxonCryptoConfigUtil {

    private static final String MUXON_PASSPHRASE_FILE = "/run/secrets/muxon-passphrase";

    private MuxonCryptoConfigUtil() {
    }

    /**
     * Initialize EncryptionUtil key using instance details and passphrase.
     *
     * @param instanceName muxon.instanceName (e.g. "local")
     * @param instanceId   muxon.instanceId (e.g. 19)
     */
    public static void initEncryptionKey(String instanceName, int instanceId) {
        String passphrase = readPassphrase();
        String encryptionKey = generateEncryptionKey(instanceName, instanceId, passphrase);
        EncryptionUtil.setKey(encryptionKey);
    }

    /**
     * Generate the deterministic encryption key string from instance details and passphrase.
     */
    public static String generateEncryptionKey(String instanceName, int instanceId, String passphrase) {
        try {
            String combined = instanceName + instanceId + passphrase;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            byte[] keyBytes = new byte[16];
            System.arraycopy(hash, 0, keyBytes, 0, 16);
            return Base64.getEncoder().encodeToString(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }

    private static String readPassphrase() {
        try {
            return Files.readString(Paths.get(MUXON_PASSPHRASE_FILE)).trim();
        } catch (IOException e) {
            String envPassphrase = System.getenv("MUXON_PASSPHRASE");
            if (envPassphrase == null || envPassphrase.isEmpty()) {
                throw new IllegalStateException("Passphrase not configured: expected file " +
                        MUXON_PASSPHRASE_FILE + " or MUXON_PASSPHRASE env var");
            }
            return envPassphrase;
        }
    }
}

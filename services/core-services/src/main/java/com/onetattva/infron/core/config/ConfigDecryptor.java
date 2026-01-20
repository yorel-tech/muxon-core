package com.onetattva.infron.core.config;

import com.onetattva.infron.core.common.EncryptionUtil;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Boot component that decrypts encrypted configuration values before database connection.
 * Runs during application startup to decrypt passwords and other sensitive values.
 */
public class ConfigDecryptor implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final String INFRON_PASSPHRASE_FILE = "/run/secrets/infron-passphrase";
    private static final String SPRING_DATASOURCE_PASSWORD = "spring.datasource.password";
    private static final String DECRYPTED_PROPERTIES = "decrypted-properties";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();

        // Read infron configuration from environment
        String instanceName = environment.getProperty("infron.instanceName", "local");
        String instanceIdStr = environment.getProperty("infron.instanceId", "1");
        int instanceId;
        try {
            instanceId = Integer.parseInt(instanceIdStr);
        } catch (NumberFormatException e) {
            instanceId = 1;
        }

        // Read passphrase from mounted file
        String passphrase;
        try {
            passphrase = Files.readString(Paths.get(INFRON_PASSPHRASE_FILE)).trim();
        } catch (IOException e) {
            // Fallback to a default for development - in production this should fail
            passphrase = System.getenv("INFRON_PASSPHRASE");
        }

        String encryptionKey = generateEncryptionKey(instanceName, instanceId, passphrase);
        EncryptionUtil.setKey(encryptionKey);

        // Decrypt datasource password if it's encrypted
        String dbPassword = environment.getProperty(SPRING_DATASOURCE_PASSWORD);
        if (dbPassword != null) {
            try {
                String decryptedPassword = EncryptionUtil.decrypt(dbPassword);
                // Add decrypted property to environment (override the encrypted one)
                Map<String, Object> decryptedProperties = new HashMap<>();
                decryptedProperties.put(SPRING_DATASOURCE_PASSWORD, decryptedPassword);
                environment.getPropertySources().addFirst(
                    new MapPropertySource(DECRYPTED_PROPERTIES, decryptedProperties)
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to decrypt database password", e);
            }
        }
    }

    private String generateEncryptionKey(String instanceName, int instanceId, String passphrase) {
        try {
            // Create a deterministic key from instance details and passphrase
            String combined = instanceName + instanceId + passphrase;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Take first 16 bytes for AES-128 and base64 encode
            byte[] keyBytes = new byte[16];
            System.arraycopy(hash, 0, keyBytes, 0, 16);
            return Base64.getEncoder().encodeToString(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }
}

package com.onetattva.infron.core.config;

import com.onetattva.infron.core.common.EncryptionUtil;
import com.onetattva.infron.core.common.InfronCryptoConfigUtil;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Boot component that decrypts encrypted configuration values before database connection.
 * Runs during application startup to decrypt passwords and other sensitive values.
 */
public class ConfigDecryptor implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

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

        // Initialize encryption key using shared utility
        InfronCryptoConfigUtil.initEncryptionKey(instanceName, instanceId);

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
                // Check if the password might be plain text (not encrypted)
                if (looksLikePlainText(dbPassword)) {
                    System.out.println("Warning: Database password appears to be plain text, not encrypted. Using as-is.");
                    // Password is likely plain text, no need to decrypt
                } else {
                    System.err.println("Warning: Failed to decrypt database password. Error: " + e.getMessage());
                    System.err.println("This could be due to:");
                    System.err.println("1. Wrong INFRON_PASSPHRASE environment variable");
                    System.err.println("2. Wrong instanceName/instanceId configuration");
                    System.err.println("3. Password was encrypted with different parameters");
                    System.err.println("4. Password is plain text but contains special characters");
                    System.err.println("Using password as-is as fallback...");
                }
                // As fallback, try to use the password as-is (plain text)
                // This allows the application to start if the password is not encrypted
            }
        }
    }

    /**
     * Helper method to determine if a password looks like plain text vs encrypted.
     * Encrypted passwords should be valid Base64 strings.
     */
    private boolean looksLikePlainText(String password) {
        if (password == null || password.isEmpty()) {
            return true;
        }
        
        try {
            // Try to decode as Base64 - if it fails, it's likely plain text
            java.util.Base64.getDecoder().decode(password);
            // If it decodes successfully, check if it contains common plain text patterns
            return password.contains("(") || password.contains(")") || 
                   password.contains("{") || password.contains("}") ||
                   password.contains(" ") || password.contains("@") ||
                   password.length() < 10; // Very short strings are likely plain text
        } catch (IllegalArgumentException e) {
            // Not valid Base64, so it's plain text
            return true;
        }
    }

}

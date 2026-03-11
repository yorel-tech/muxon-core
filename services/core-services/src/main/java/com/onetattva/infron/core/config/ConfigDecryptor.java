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
                throw new RuntimeException("Failed to decrypt database password", e);
            }
        }
    }

}

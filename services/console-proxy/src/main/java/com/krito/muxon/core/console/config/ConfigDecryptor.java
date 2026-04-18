package com.krito.muxon.core.console.config;

import com.krito.muxon.core.common.EncryptionUtil;
import com.krito.muxon.core.common.MuxonCryptoConfigUtil;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Decrypts encrypted configuration values before the DataSource is created.
 * Mirrors {@code com.krito.muxon.core.config.ConfigDecryptor} in core-services.
 */
public class ConfigDecryptor implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final String SPRING_DATASOURCE_PASSWORD = "spring.datasource.password";
    private static final String DECRYPTED_PROPERTIES = "decrypted-properties";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();

        String instanceName = environment.getProperty("infron.instanceName", "local");
        String instanceIdStr = environment.getProperty("infron.instanceId", "1");
        int instanceId;
        try {
            instanceId = Integer.parseInt(instanceIdStr);
        } catch (NumberFormatException e) {
            instanceId = 1;
        }

        MuxonCryptoConfigUtil.initEncryptionKey(instanceName, instanceId);

        String dbPassword = environment.getProperty(SPRING_DATASOURCE_PASSWORD);
        if (dbPassword != null) {
            try {
                String decryptedPassword = EncryptionUtil.decrypt(dbPassword);
                Map<String, Object> decryptedProperties = new HashMap<>();
                decryptedProperties.put(SPRING_DATASOURCE_PASSWORD, decryptedPassword);
                environment.getPropertySources().addFirst(
                    new MapPropertySource(DECRYPTED_PROPERTIES, decryptedProperties)
                );
            } catch (Exception e) {
                if (looksLikePlainText(dbPassword)) {
                    System.out.println("Warning: Database password appears to be plain text, not encrypted. Using as-is.");
                } else {
                    System.err.println("Warning: Failed to decrypt database password. Error: " + e.getMessage());
                    System.err.println("This could be due to:");
                    System.err.println("1. Wrong MUXON_PASSPHRASE environment variable");
                    System.err.println("2. Wrong instanceName/instanceId configuration");
                    System.err.println("3. Password was encrypted with different parameters");
                    System.err.println("4. Password is plain text but contains special characters");
                    System.err.println("Using password as-is as fallback...");
                }
            }
        }
    }

    private boolean looksLikePlainText(String password) {
        if (password == null || password.isEmpty()) {
            return true;
        }

        try {
            java.util.Base64.getDecoder().decode(password);
            return password.contains("(") || password.contains(")")
                || password.contains("{") || password.contains("}")
                || password.contains(" ") || password.contains("@")
                || password.length() < 10;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }
}

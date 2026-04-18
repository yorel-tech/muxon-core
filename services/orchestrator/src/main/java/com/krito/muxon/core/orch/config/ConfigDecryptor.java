package com.krito.muxon.core.orch.config;

import com.krito.muxon.core.common.EncryptionUtil;
import com.krito.muxon.core.common.MuxonCryptoConfigUtil;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Decrypts encrypted configuration values (e.g. datasource password) before the DataSource bean is created.
 * Runs on ApplicationEnvironmentPreparedEvent so decrypted values are available when HikariCP initializes.
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
                throw new RuntimeException("Failed to decrypt database password", e);
            }
        }
    }
}

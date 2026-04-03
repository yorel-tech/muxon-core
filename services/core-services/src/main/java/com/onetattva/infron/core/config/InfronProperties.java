package com.onetattva.infron.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code infron.instanceName} and {@code infron.instanceId} from application config.
 */
@ConfigurationProperties(prefix = "infron", ignoreUnknownFields = true)
public class InfronProperties {

    private String instanceName = "local";
    private int instanceId = 1;

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public int getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(int instanceId) {
        this.instanceId = instanceId;
    }
}

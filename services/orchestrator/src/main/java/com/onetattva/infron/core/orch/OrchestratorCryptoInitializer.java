package com.onetattva.infron.core.orch;

import com.onetattva.infron.core.common.InfronCryptoConfigUtil;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Quarkus-specific initializer for Infron encryption.
 * Initializes EncryptionUtil key at startup using shared crypto logic.
 */
@ApplicationScoped
@Startup
public class OrchestratorCryptoInitializer {

    @ConfigProperty(name = "infron.instanceName", defaultValue = "local")
    String instanceName;

    @ConfigProperty(name = "infron.instanceId", defaultValue = "1")
    int instanceId;

    public OrchestratorCryptoInitializer() {
        // default constructor for CDI
    }

    @jakarta.annotation.PostConstruct
    void init() {
        InfronCryptoConfigUtil.initEncryptionKey(instanceName, instanceId);
    }
}


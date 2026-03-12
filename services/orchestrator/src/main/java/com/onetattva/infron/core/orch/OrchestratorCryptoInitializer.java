package com.onetattva.infron.core.orch;

import com.onetattva.infron.core.common.InfronCryptoConfigUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Spring Boot initializer for Infron encryption.
 * Initializes EncryptionUtil key at startup using shared crypto logic.
 */
@Component
public class OrchestratorCryptoInitializer {

    @Value("${infron.instanceName:local}")
    String instanceName;

    @Value("${infron.instanceId:1}")
    int instanceId;

    @PostConstruct
    void init() {
        InfronCryptoConfigUtil.initEncryptionKey(instanceName, instanceId);
    }
}


package com.sal.muxon.orch;

import com.sal.muxon.common.MuxonCryptoConfigUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Spring Boot initializer for Infron encryption.
 * Initializes EncryptionUtil key at startup using shared crypto logic.
 */
@Component
public class OrchestratorCryptoInitializer {

    @Value("${muxon.instanceName:local}")
    String instanceName;

    @Value("${muxon.instanceId:1}")
    int instanceId;

    @PostConstruct
    void init() {
        MuxonCryptoConfigUtil.initEncryptionKey(instanceName, instanceId);
    }
}


package com.onetattva.infron.tests.deploy;

import com.onetattva.infron.tests.InfronEnvironment;
import org.junit.jupiter.api.Test;

public class InfraDeployerTests {

    @Test
    public void deployInfrastructure() throws Exception {
        InfronEnvironment env = InfronEnvironment.getInstance();

        // Start infrastructure if not already started
        env.startInfrastructure();

        // Test that infrastructure is deployed
        assert env.isPostgresRunning();
        assert env.isRedisRunning();
        assert env.isKeycloakRunning();
        assert env.isCoreServicesRunning();
    }
}

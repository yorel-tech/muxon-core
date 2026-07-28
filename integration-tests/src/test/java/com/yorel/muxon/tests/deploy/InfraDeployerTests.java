package com.yorel.muxon.tests.deploy;

import com.yorel.muxon.tests.MuxonEnvironment;
import org.junit.jupiter.api.Test;

public class InfraDeployerTests {

    @Test
    public void deployInfrastructure() throws Exception {
        MuxonEnvironment env = MuxonEnvironment.getInstance();

        // Start infrastructure if not already started
        env.startInfrastructure();

        // Test that infrastructure is deployed
        assert env.isPostgresRunning();
        assert env.isKeycloakRunning();
        assert env.isCoreServicesRunning();
    }
}

package com.yorel.muxon.deploy;

import com.yorel.muxon.tests.MuxonEnvironment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class InfraDeployer {

    @BeforeAll
    public static void setupInfrastructure() throws Exception {
        MuxonEnvironment.getInstance().startInfrastructure();
    }

    @Test
    public void deployInfrastructure() throws Exception {
        MuxonEnvironment env = MuxonEnvironment.getInstance();

        // Test that infrastructure is deployed
        assert env.isPostgresRunning();
        assert env.isKeycloakRunning();
        assert env.isCoreServicesRunning();
    }
}

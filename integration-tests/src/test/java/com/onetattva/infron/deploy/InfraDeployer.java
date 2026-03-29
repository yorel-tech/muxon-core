package com.onetattva.infron.deploy;

import com.onetattva.infron.tests.InfronEnvironment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class InfraDeployer {

    @BeforeAll
    public static void setupInfrastructure() throws Exception {
        InfronEnvironment.getInstance().startInfrastructure();
    }

    @Test
    public void deployInfrastructure() throws Exception {
        InfronEnvironment env = InfronEnvironment.getInstance();

        // Test that infrastructure is deployed
        assert env.isPostgresRunning();
        assert env.isKeycloakRunning();
        assert env.isCoreServicesRunning();
    }
}

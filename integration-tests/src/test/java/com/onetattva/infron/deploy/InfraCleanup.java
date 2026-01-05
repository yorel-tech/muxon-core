package com.onetattva.infron.deploy;

import com.onetattva.infron.tests.InfronEnvironment;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

public class InfraCleanup {

    @AfterAll
    public static void cleanupInfrastructure() throws Exception {
        InfronEnvironment.getInstance().collectCoreServicesLogs();
        InfronEnvironment.getInstance().stopInfrastructure();
    }

    @Test
    public void cleanupInfrastructureTest() throws Exception {
        // Test that infrastructure is cleaned up
        // Since stopInfrastructure is called in @AfterAll, this test can verify if needed
    }
}

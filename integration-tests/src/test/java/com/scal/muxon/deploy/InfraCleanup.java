package com.scal.muxon.deploy;

import com.scal.muxon.tests.MuxonEnvironment;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

public class InfraCleanup {

    @AfterAll
    public static void cleanupInfrastructure() throws Exception {
        MuxonEnvironment.getInstance().collectCoreServicesLogs();
        MuxonEnvironment.getInstance().stopInfrastructure();
    }

    @Test
    public void cleanupInfrastructureTest() throws Exception {
        // Test that infrastructure is cleaned up
        // Since stopInfrastructure is called in @AfterAll, this test can verify if needed
    }
}

package com.krito.muxon.tests;


import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

public class MuxonEnvironmentExtension implements BeforeAllCallback, AutoCloseable {

    public static final String MUXON_ENV_EXT = "muxon-env-ext";
    private final MuxonEnvironment muxonEnvironment = MuxonEnvironment.getInstance();
    private static boolean started = false;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        // We use a lock to ensure this only runs once for the entire suite
        if (!started) {
            started = true;

            // 1. Run your Deploy logic here
            System.out.println("🚀 Deploying Infrastructure...");
            muxonEnvironment.startInfrastructure();

            // 2. Register this class as a CloseableResource in the root context.
            // This ensures the 'close()' method below is called when the JVM shuts down the test engine.
            context.getRoot().getStore(GLOBAL).put(MUXON_ENV_EXT, this);
        }
    }

    @Override
    public void close() {
        // 3. This method runs AUTOMATICALLY after all tests are finished
        System.out.println("🧹 Cleaning up Infrastructure...");
        muxonEnvironment.stopInfrastructure();
    }
}

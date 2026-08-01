/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.tests;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

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

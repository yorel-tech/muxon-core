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
package com.yorel.muxon.deploy;

import com.yorel.muxon.tests.MuxonEnvironment;
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

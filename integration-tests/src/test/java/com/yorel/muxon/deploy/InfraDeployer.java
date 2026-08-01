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

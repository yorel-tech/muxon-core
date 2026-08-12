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
package com.yorel.muxon.orch;

import com.yorel.muxon.db.model.DatacenterEntity;
import com.yorel.muxon.db.model.NodeClusterEntity;
import com.yorel.muxon.db.model.ProviderEntity;
import com.yorel.muxon.db.model.QueueEntryEntity;
import com.yorel.muxon.db.model.TenantDatacenterGrantEntity;
import com.yorel.muxon.db.model.VmEntity;
import com.yorel.muxon.db.repository.DatacenterRepository;
import com.yorel.muxon.db.repository.QueueEntryRepository;
import com.yorel.muxon.db.repository.TenantDatacenterGrantRepository;
import com.yorel.muxon.db.repository.VmRepository;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * GraalVM native image runtime hints for the Orchestrator service. Registers reflection and
 * resource hints needed for native compilation.
 */
@Configuration
@ImportRuntimeHints(OrchestratorNativeHints.Registrar.class)
public class OrchestratorNativeHints {

  static class Registrar implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
      // Register reflection for JPA entities
      hints
          .reflection()
          .registerType(TypeReference.of(VmEntity.class))
          .registerType(TypeReference.of(TenantDatacenterGrantEntity.class))
          .registerType(TypeReference.of(DatacenterEntity.class))
          .registerType(TypeReference.of(NodeClusterEntity.class))
          .registerType(TypeReference.of(ProviderEntity.class))
          .registerType(TypeReference.of(QueueEntryEntity.class));

      // Register reflection for repositories
      hints
          .reflection()
          .registerType(TypeReference.of(VmRepository.class))
          .registerType(TypeReference.of(TenantDatacenterGrantRepository.class))
          .registerType(TypeReference.of(DatacenterRepository.class))
          .registerType(TypeReference.of(QueueEntryRepository.class));

      // Register resources for configuration files
      hints
          .resources()
          .registerPattern("application.yaml")
          .registerPattern("application.yml")
          .registerPattern("logback-spring.xml");

      // Register JNI for native libraries
      hints
          .jni()
          .registerType(TypeReference.of("org.postgresql.Driver"))
          .registerType(TypeReference.of("org.postgresql.core.ConnectionFactory"));

      // Register serialization for Jackson
      hints
          .reflection()
          .registerType(TypeReference.of("tools.jackson.databind.ObjectMapper"))
          .registerType(TypeReference.of("tools.jackson.databind.JsonSerializer"))
          .registerType(TypeReference.of("tools.jackson.databind.JsonDeserializer"));
    }
  }
}

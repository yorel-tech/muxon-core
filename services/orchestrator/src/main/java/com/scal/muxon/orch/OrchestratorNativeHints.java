package com.scal.muxon.orch;

import com.scal.muxon.db.model.*;
import com.scal.muxon.db.repository.*;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * GraalVM native image runtime hints for the Orchestrator service.
 * Registers reflection and resource hints needed for native compilation.
 */
@Configuration
@ImportRuntimeHints(OrchestratorNativeHints.Registrar.class)
public class OrchestratorNativeHints {

    static class Registrar implements RuntimeHintsRegistrar {
        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            // Register reflection for JPA entities
            hints.reflection()
                .registerType(TypeReference.of(VmEntity.class))
                .registerType(TypeReference.of(TenantDatacenterGrantEntity.class))
                .registerType(TypeReference.of(DatacenterEntity.class))
                .registerType(TypeReference.of(NodeClusterEntity.class))
                .registerType(TypeReference.of(ProviderEntity.class))
                .registerType(TypeReference.of(QueueEntryEntity.class));

            // Register reflection for repositories
            hints.reflection()
                .registerType(TypeReference.of(VmRepository.class))
                .registerType(TypeReference.of(TenantDatacenterGrantRepository.class))
                .registerType(TypeReference.of(DatacenterRepository.class))
                .registerType(TypeReference.of(QueueEntryRepository.class));

            // Register resources for configuration files
            hints.resources()
                .registerPattern("application.yaml")
                .registerPattern("application.yml")
                .registerPattern("logback-spring.xml");

            // Register JNI for native libraries
            hints.jni()
                .registerType(TypeReference.of("org.postgresql.Driver"))
                .registerType(TypeReference.of("org.postgresql.core.ConnectionFactory"));

            // Register serialization for Jackson
            hints.reflection()
                .registerType(TypeReference.of("tools.jackson.databind.ObjectMapper"))
                .registerType(TypeReference.of("tools.jackson.databind.JsonSerializer"))
                .registerType(TypeReference.of("tools.jackson.databind.JsonDeserializer"));
        }
    }
}

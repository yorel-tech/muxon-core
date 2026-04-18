package com.krito.muxon.core;

import org.springframework.boot.SpringApplication;
import com.krito.muxon.core.config.MuxonConsoleProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(MuxonConsoleProperties.class)
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.krito.muxon.db.repository")
@EntityScan(basePackages = "com.krito.muxon.db.model")
@ComponentScan(basePackages = {"com.krito.muxon.core.config",
        "com.krito.muxon.core.security",
        "com.krito.muxon.core.controllers",
        "com.krito.muxon.core.services",
        "com.krito.muxon.core.events",
        "com.krito.muxon.core.grpc",
        "com.krito.muxon.core.info",
        "com.krito.muxon.core.hateoas",
        "com.krito.muxon.core.auth",
        "com.krito.muxon.core.web",
        "com.krito.muxon.db.resolver"})
public class CoreServicesApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreServicesApplication.class, args);
    }
}

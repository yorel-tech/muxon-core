package com.onetattva.infron.core;

import org.springframework.boot.SpringApplication;
import com.onetattva.infron.core.config.InfronConsoleProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(InfronConsoleProperties.class)
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.onetattva.infron.db.repository")
@EntityScan(basePackages = "com.onetattva.infron.db.model")
@ComponentScan(basePackages = {"com.onetattva.infron.core.config",
        "com.onetattva.infron.core.security",
        "com.onetattva.infron.core.controllers",
        "com.onetattva.infron.core.services",
        "com.onetattva.infron.core.events",
        "com.onetattva.infron.core.grpc",
        "com.onetattva.infron.core.info",
        "com.onetattva.infron.core.hateoas",
        "com.onetattva.infron.core.auth",
        "com.onetattva.infron.core.web"})
public class CoreServicesApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreServicesApplication.class, args);
    }
}

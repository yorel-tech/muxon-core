package com.onetattva.infron.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.onetattva.infron.core.config",
        "com.onetattva.infron.core.security",
        "com.onetattva.infron.core.controllers",
        "com.onetattva.infron.core.services",
        "com.onetattva.infron.core.hateoas",
        "com.onetattva.infron.core.auth",
        "com.onetattva.infron.core.web",
        "com.onetattva.infron.core.providers",
        "com.onetattva.infron.db.repository"})
@EnableJpaRepositories(basePackages = {"com.onetattva.infron.db.repository"})
@EntityScan(basePackages = {"com.onetattva.infron.db.model"})
public class CoreServicesApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreServicesApplication.class, args);
    }
}

package com.krito.muxon;

import org.springframework.boot.SpringApplication;
import com.krito.muxon.config.MuxonConsoleProperties;
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
@ComponentScan(basePackages = {"com.krito.muxon.config",
        "com.krito.muxon.security",
        "com.krito.muxon.controllers",
        "com.krito.muxon.services",
        "com.krito.muxon.events",
        "com.krito.muxon.grpc",
        "com.krito.muxon.info",
        "com.krito.muxon.hateoas",
        "com.krito.muxon.auth",
        "com.krito.muxon.web",
        "com.krito.muxon.db.resolver"})
public class CoreServicesApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreServicesApplication.class, args);
    }
}

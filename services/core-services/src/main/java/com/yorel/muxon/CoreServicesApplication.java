package com.yorel.muxon;

import org.springframework.boot.SpringApplication;
import com.yorel.muxon.config.MuxonConsoleProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(MuxonConsoleProperties.class)
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.yorel.muxon.db.repository")
@EntityScan(basePackages = "com.yorel.muxon.db.model")
@ComponentScan(basePackages = {"com.yorel.muxon.config",
        "com.yorel.muxon.security",
        "com.yorel.muxon.controllers",
        "com.yorel.muxon.services",
        "com.yorel.muxon.events",
        "com.yorel.muxon.grpc",
        "com.yorel.muxon.info",
        "com.yorel.muxon.hateoas",
        "com.yorel.muxon.auth",
        "com.yorel.muxon.web",
        "com.yorel.muxon.db.resolver"})
public class CoreServicesApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreServicesApplication.class, args);
    }
}

package com.sal.muxon;

import org.springframework.boot.SpringApplication;
import com.sal.muxon.config.MuxonConsoleProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(MuxonConsoleProperties.class)
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.sal.muxon.db.repository")
@EntityScan(basePackages = "com.sal.muxon.db.model")
@ComponentScan(basePackages = {"com.sal.muxon.config",
        "com.sal.muxon.security",
        "com.sal.muxon.controllers",
        "com.sal.muxon.services",
        "com.sal.muxon.events",
        "com.sal.muxon.grpc",
        "com.sal.muxon.info",
        "com.sal.muxon.hateoas",
        "com.sal.muxon.auth",
        "com.sal.muxon.web",
        "com.sal.muxon.db.resolver"})
public class CoreServicesApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreServicesApplication.class, args);
    }
}

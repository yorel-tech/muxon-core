package com.scal.muxon;

import org.springframework.boot.SpringApplication;
import com.scal.muxon.config.MuxonConsoleProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(MuxonConsoleProperties.class)
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.scal.muxon.db.repository")
@EntityScan(basePackages = "com.scal.muxon.db.model")
@ComponentScan(basePackages = {"com.scal.muxon.config",
        "com.scal.muxon.security",
        "com.scal.muxon.controllers",
        "com.scal.muxon.services",
        "com.scal.muxon.events",
        "com.scal.muxon.grpc",
        "com.scal.muxon.info",
        "com.scal.muxon.hateoas",
        "com.scal.muxon.auth",
        "com.scal.muxon.web",
        "com.scal.muxon.db.resolver"})
public class CoreServicesApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreServicesApplication.class, args);
    }
}

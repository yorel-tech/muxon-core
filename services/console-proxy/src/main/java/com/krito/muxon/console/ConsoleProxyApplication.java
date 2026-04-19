package com.krito.muxon.console;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(ConsoleProxyProperties.class)
@EntityScan(basePackages = "com.krito.muxon.db.model")
@EnableJpaRepositories(basePackages = "com.krito.muxon.db.repository")
@EnableScheduling
public class ConsoleProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsoleProxyApplication.class, args);
    }
}

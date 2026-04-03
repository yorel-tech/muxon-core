package com.onetattva.infron.core.console;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(ConsoleProxyProperties.class)
@EntityScan(basePackages = "com.onetattva.infron.db.model")
@EnableJpaRepositories(basePackages = "com.onetattva.infron.db.repository")
@EnableScheduling
public class ConsoleProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsoleProxyApplication.class, args);
    }
}

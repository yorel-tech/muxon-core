package com.sal.muxon.orch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.sal.muxon")
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.sal.muxon.db.repository")
@EntityScan(basePackages = "com.sal.muxon.db.model")
public class OrchestratorApp {
    public static void main(String[] args) {
        SpringApplication.run(OrchestratorApp.class, args);
    }
}

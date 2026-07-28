package com.scal.muxon.orch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.scal.muxon")
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.scal.muxon.db.repository")
@EntityScan(basePackages = "com.scal.muxon.db.model")
public class OrchestratorApp {
    public static void main(String[] args) {
        SpringApplication.run(OrchestratorApp.class, args);
    }
}

package com.krito.muxon.core.orch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.krito.muxon.core",
        "com.krito.muxon.db"
})
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.krito.muxon.db.repository")
@EntityScan(basePackages = "com.krito.muxon.db.model")
public class OrchestratorApp {
    public static void main(String[] args) {
        SpringApplication.run(OrchestratorApp.class, args);
    }
}

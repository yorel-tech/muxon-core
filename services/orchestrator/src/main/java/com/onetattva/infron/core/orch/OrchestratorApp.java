package com.onetattva.infron.core.orch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.onetattva.infron.core",
        "com.onetattva.infron.db"
})
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.onetattva.infron.db.repository")
@EntityScan(basePackages = "com.onetattva.infron.db.model")
public class OrchestratorApp {
    public static void main(String[] args) {
        SpringApplication.run(OrchestratorApp.class, args);
    }
}

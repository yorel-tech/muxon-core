package com.onetattva.infron.core;

import com.onetattva.infron.db.queue.QueueDbConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(QueueDbConfiguration.class)
@ComponentScan(basePackages = {"com.onetattva.infron.core.config",
        "com.onetattva.infron.core.security",
        "com.onetattva.infron.core.controllers",
        "com.onetattva.infron.core.services",
        "com.onetattva.infron.core.info",
        "com.onetattva.infron.core.hateoas",
        "com.onetattva.infron.core.auth",
        "com.onetattva.infron.core.web",
        "com.onetattva.infron.db.repository"})
public class CoreServicesApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreServicesApplication.class, args);
    }
}

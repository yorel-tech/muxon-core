package com.onetattva.infron.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.onetattva.infron.common.Health;

import java.util.Map;

@SpringBootApplication
@ComponentScan(basePackages = {"com.onetattva.infron.gateway",
        "com.onetattva.infron.services",
        "com.onetattva.infron.db.repository"})
@EnableJpaRepositories(basePackages = {"com.onetattva.infron.db.repository"})
@EntityScan(basePackages = {"com.onetattva.infron.db.model"})
public class GatewayApp {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApp.class, args);
    }

    @RestController
    static class HealthController {
        @GetMapping("/healthz")
        public Health health() { return new Health("ok"); }
    }

    @GetMapping("/api/v1/me")
    public Map<String, Object> me(org.springframework.security.core.Authentication auth) {
        return Map.of(
                "name", auth.getName(),
                "authorities", auth.getAuthorities().stream().map(Object::toString).toList()
        );
    }

}

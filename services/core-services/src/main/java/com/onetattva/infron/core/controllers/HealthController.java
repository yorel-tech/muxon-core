package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.HealthApi;
import com.onetattva.infron.api.model.Health;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class HealthController implements HealthApi {

    @Override
    @GetMapping("/healthz")
    public ResponseEntity<Health> getHealthStatus() {
        // TODO: Implement actual health check
        Health health = new Health();
        health.setStatus("ok");
        return ResponseEntity.ok(health);
    }
}

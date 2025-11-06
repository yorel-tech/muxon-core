package com.onetattva.infron.gateway.controllers;

import com.onetattva.infron.api.HealthApi;
import com.onetattva.infron.api.model.Health;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController implements HealthApi {

    @Override
    public ResponseEntity<Health> getHealthStatus() {
        // TODO: Implement actual health check
        Health health = new Health();
        health.setStatus("ok");
        return ResponseEntity.ok(health);
    }
}

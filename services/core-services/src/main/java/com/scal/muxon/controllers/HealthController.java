package com.scal.muxon.controllers;

import com.scal.muxon.api.HealthApi;
import com.scal.muxon.api.model.Health;
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

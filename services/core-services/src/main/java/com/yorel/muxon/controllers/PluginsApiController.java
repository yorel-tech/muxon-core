package com.yorel.muxon.controllers;

import com.yorel.muxon.auth.Permission;
import com.yorel.muxon.auth.RequiresPermission;
import com.yorel.muxon.db.model.PluginCapabilityEntity;
import com.yorel.muxon.db.model.PluginEntity;
import com.yorel.muxon.services.plugin.PluginDuplicateException;
import com.yorel.muxon.services.plugin.PluginHealthMonitorService;
import com.yorel.muxon.services.plugin.PluginNotFoundException;
import com.yorel.muxon.services.plugin.PluginRegistrationException;
import com.yorel.muxon.services.plugin.PluginService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/plugins")
public class PluginsApiController {

    @Autowired private PluginService pluginService;
    @Autowired private PluginHealthMonitorService healthMonitorService;

    @PostMapping
    @RequiresPermission(Permission.PLUGIN_MANAGE)
    public ResponseEntity<PluginEntity> registerPlugin(@Valid @RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> manifest = extractManifest(body);
            PluginEntity plugin = pluginService.registerPlugin(manifest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("Location", "/api/v1/admin/plugins/" + plugin.getId())
                    .body(plugin);
        } catch (PluginDuplicateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (PluginRegistrationException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        }
    }

    @GetMapping
    @RequiresPermission(Permission.PLUGIN_READ)
    public ResponseEntity<List<PluginEntity>> listPlugins() {
        return ResponseEntity.ok(pluginService.listPlugins());
    }

    @GetMapping("/{pluginId}")
    @RequiresPermission(Permission.PLUGIN_READ)
    public ResponseEntity<PluginEntity> getPlugin(@PathVariable UUID pluginId) {
        try {
            return ResponseEntity.ok(pluginService.getPlugin(pluginId));
        } catch (PluginNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/{pluginId}/activate")
    @RequiresPermission(Permission.PLUGIN_MANAGE)
    public ResponseEntity<PluginEntity> activatePlugin(@PathVariable UUID pluginId) {
        try {
            return ResponseEntity.ok(pluginService.activatePlugin(pluginId));
        } catch (PluginNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (PluginRegistrationException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        }
    }

    @PostMapping("/{pluginId}/disable")
    @RequiresPermission(Permission.PLUGIN_MANAGE)
    public ResponseEntity<PluginEntity> disablePlugin(@PathVariable UUID pluginId) {
        try {
            return ResponseEntity.ok(pluginService.disablePlugin(pluginId));
        } catch (PluginNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @DeleteMapping("/{pluginId}")
    @RequiresPermission(Permission.PLUGIN_MANAGE)
    public ResponseEntity<Void> deletePlugin(@PathVariable UUID pluginId) {
        try {
            pluginService.deletePlugin(pluginId);
            return ResponseEntity.noContent().build();
        } catch (PluginNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (PluginRegistrationException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        }
    }

    @GetMapping("/{pluginId}/capabilities")
    @RequiresPermission(Permission.PLUGIN_READ)
    public ResponseEntity<List<PluginCapabilityEntity>> listCapabilities(@PathVariable UUID pluginId) {
        try {
            return ResponseEntity.ok(pluginService.listCapabilities(pluginId));
        } catch (PluginNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/{pluginId}/health-check")
    @RequiresPermission(Permission.PLUGIN_MANAGE)
    public ResponseEntity<Map<String, Object>> triggerHealthCheck(@PathVariable UUID pluginId) {
        try {
            return ResponseEntity.ok(healthMonitorService.checkNow(pluginId));
        } catch (PluginNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractManifest(Map<String, Object> body) {
        Object manifest = body.get("manifest");
        if (manifest instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return body;
    }
}

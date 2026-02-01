package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.*;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.core.services.ProvidersService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for managing infrastructure providers.
 */
@RestController
public class ProvidersController implements ProvidersApi {

    @Autowired
    private ProvidersService providersService;

    @Override
    public ResponseEntity<ProviderList> listProviders(
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "perPage", required = false, defaultValue = "20") Integer perPage,
            @RequestParam(value = "type", required = false) ProviderType type,
            @RequestParam(value = "status", required = false) ProviderStatus status
    ) {
        ProviderList providerList = providersService.listProviders(page, perPage, type, status, null);
        return ResponseEntity.ok(providerList);
    }

    @Override
    public ResponseEntity<Provider> createProvider(@Valid @RequestBody ProviderCreate providerCreate) {
        Provider provider = providersService.createProvider(providerCreate);
        return ResponseEntity.status(201).body(provider);
    }

    @Override
    public ResponseEntity<Provider> getProvider(@NotNull @PathVariable("providerId") UUID providerId) {
        Provider provider = providersService.getProvider(providerId);
        return ResponseEntity.ok(provider);
    }

    @Override
    public ResponseEntity<Provider> replaceProvider(
            @NotNull @PathVariable("providerId") UUID providerId,
            @Valid @RequestBody ProviderUpdate providerUpdate
    ) {
        Provider provider = providersService.replaceProvider(providerId, providerUpdate);
        return ResponseEntity.ok(provider);
    }

    @Override
    public ResponseEntity<Provider> updateProvider(
            @NotNull @PathVariable("providerId") UUID providerId,
            @Valid @RequestBody ProviderUpdate providerUpdate
    ) {
        Provider provider = providersService.updateProvider(providerId, providerUpdate);
        return ResponseEntity.ok(provider);
    }

    @Override
    public ResponseEntity<Void> deleteProvider(@NotNull @PathVariable("providerId") UUID providerId) {
        providersService.deleteProvider(providerId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ProviderConnectionTestResult> testProviderConnection(@NotNull @PathVariable("providerId") UUID providerId) {
        ProviderConnectionTestResult result = providersService.testProviderConnection(providerId);
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<ProviderCapabilities> getProviderCapabilities(
            @NotNull @PathVariable("providerId") UUID providerId,
            @RequestParam(value = "refresh", required = false, defaultValue = "false") Boolean refresh
    ) {
        ProviderCapabilities capabilities = providersService.getProviderCapabilities(providerId, refresh);
        return ResponseEntity.ok(capabilities);
    }
}

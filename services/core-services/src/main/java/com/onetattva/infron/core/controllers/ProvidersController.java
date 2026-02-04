package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.*;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.core.services.ProvidersService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for managing infrastructure providers.
 */
@RestController
public class ProvidersController implements ProvidersApi {

    private final ProvidersService providersService;

    public ProvidersController(final ProvidersService providersService) {
        this.providersService = providersService;
    }

    @Override
    public ResponseEntity<ProviderList> listProviders(Integer page, Integer perPage, ProviderType type, ProviderStatus status
    ) {
        ProviderList providerList = providersService.listProviders(page, perPage, type, status, null);
        return ResponseEntity.ok(providerList);
    }

    @Override
    public ResponseEntity<Provider> createProvider(ProviderCreate providerCreate) {
        Provider provider = providersService.createProvider(providerCreate);
        return ResponseEntity.status(201).body(provider);
    }

    @Override
    public ResponseEntity<Provider> getProvider(UUID providerId) {
        Provider provider = providersService.getProvider(providerId);
        return ResponseEntity.ok(provider);
    }

    @Override
    public ResponseEntity<Provider> replaceProvider(UUID providerId, ProviderUpdate providerUpdate
    ) {
        Provider provider = providersService.replaceProvider(providerId, providerUpdate);
        return ResponseEntity.ok(provider);
    }

    @Override
    public ResponseEntity<Provider> updateProvider(
            UUID providerId,
            ProviderUpdate providerUpdate
    ) {
        Provider provider = providersService.updateProvider(providerId, providerUpdate);
        return ResponseEntity.ok(provider);
    }

    @Override
    public ResponseEntity<Void> deleteProvider(UUID providerId) {
        providersService.deleteProvider(providerId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ProviderConnectionTestResult> testProviderConnection(UUID providerId) {
        ProviderConnectionTestResult result = providersService.testProviderConnection(providerId);
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<ProviderCapabilities> getProviderCapabilities(UUID providerId, Boolean refresh
    ) {
        ProviderCapabilities capabilities = providersService.getProviderCapabilities(providerId, refresh);
        return ResponseEntity.ok(capabilities);
    }
}

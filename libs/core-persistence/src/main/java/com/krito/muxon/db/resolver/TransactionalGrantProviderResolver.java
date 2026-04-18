package com.krito.muxon.db.resolver;

import com.krito.muxon.core.providers.TenantDatacenterGrantResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Runs {@link TenantDatacenterGrantResolver} inside a read transaction so lazy associations
 * (grant → datacenter → node cluster → provider) initialize correctly. Used from services whose
 * entrypoints are not transactional (e.g. orchestrator {@code VmTaskExecutor} self-invocations).
 */
@Service
public class TransactionalGrantProviderResolver {

    @Autowired
    private TenantDatacenterGrantResolver tenantDatacenterGrantResolver;

    @Transactional(readOnly = true)
    public Optional<String> resolveProviderId(UUID tenantDatacenterGrantId) {
        return tenantDatacenterGrantResolver.resolveProviderId(tenantDatacenterGrantId);
    }
}

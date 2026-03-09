'use client';

import { useState, useEffect, useCallback } from 'react';
import { apiGet } from '@/lib/api';
import { useTenantOptional } from '@/lib/tenant-context';

/**
 * Resolves the current tenant UUID for tenant portal pages.
 * Uses GET /api/v1/tenants/current (tenant-scoped; no provider-scoped list tenants API).
 * Passes the tenant slug from login when available so the backend returns the right tenant.
 */
export function useTenantId(): { tenantId: string | null; loading: boolean; error: string | null; refetch: () => void } {
  const { selectedTenantSlug } = useTenantOptional() ?? { selectedTenantSlug: null };
  const [tenantId, setTenantId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const resolve = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const slug = selectedTenantSlug;
      const url = slug
        ? `/api/v1/tenants/current?slug=${encodeURIComponent(slug)}`
        : '/api/v1/tenants/current';
      const current = await apiGet<{ id?: string }>(url);
      const id = (current as { id?: string })?.id;
      if (id) {
        setTenantId(id);
      } else {
        setTenantId(null);
        setError('Tenant not found');
      }
    } catch (e) {
      setTenantId(null);
      setError(e instanceof Error ? e.message : 'Failed to resolve tenant');
    } finally {
      setLoading(false);
    }
  }, [selectedTenantSlug]);

  useEffect(() => {
    resolve();
  }, [resolve]);

  return { tenantId, loading, error, refetch: resolve };
}

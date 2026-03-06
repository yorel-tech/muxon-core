'use client';

import { createContext, useContext, useEffect, useState, ReactNode } from 'react';

/** Matches backend InfoResponse (edition, capabilities, extras). */
export interface ProductInfoResponse {
  product?: string;
  edition?: string;
  version?: string;
  license?: unknown;
  capabilities?: string[];
  modules?: unknown[];
  extras?: Record<string, unknown>;
}

export interface ProductInfoState {
  /** Product edition: "core" | "nexus" | "enterprise" */
  edition: string;
  /** Capability identifiers (e.g. identity.rbac, compute.vm.snapshot) */
  capabilities: string[];
  /** Extra flags from backend (e.g. multipleDatacentersPerTenant) */
  extras: Record<string, unknown>;
  /** True when edition is nexus or enterprise */
  isEnterprise: boolean;
  /** True when tenant can have multiple datacenters (nexus); false for core */
  multipleDatacentersPerTenant: boolean;
  loading: boolean;
  error: string | null;
  /** Check if a capability string is present */
  hasCapability: (capability: string) => boolean;
}

const defaultState: ProductInfoState = {
  edition: 'core',
  capabilities: [],
  extras: {},
  isEnterprise: false,
  multipleDatacentersPerTenant: false,
  loading: true,
  error: null,
  hasCapability: () => false,
};

const ProductInfoContext = createContext<ProductInfoState | undefined>(undefined);

function deriveMultipleDatacenters(data: ProductInfoResponse): boolean {
  const ext = data.extras?.['multipleDatacentersPerTenant'];
  if (typeof ext === 'boolean') return ext;
  const ed = (data.edition ?? '').toLowerCase();
  return ed === 'nexus' || ed === 'enterprise';
}

function deriveIsEnterprise(data: ProductInfoResponse): boolean {
  const ed = (data.edition ?? '').toLowerCase();
  return ed === 'nexus' || ed === 'enterprise';
}

export function ProductInfoProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<ProductInfoState>(defaultState);

  useEffect(() => {
    let cancelled = false;

    async function fetchInfo() {
      try {
        const res = await fetch('/api/v1/info', { credentials: 'include' });
        if (cancelled) return;
        if (!res.ok) {
          setState((prev) => ({
            ...prev,
            loading: false,
            error: `Failed to load product info: ${res.status}`,
          }));
          return;
        }
        const data: ProductInfoResponse = await res.json();
        if (cancelled) return;

        const edition = data.edition ?? 'core';
        const capabilities = Array.isArray(data.capabilities) ? data.capabilities : [];
        const extras = data.extras && typeof data.extras === 'object' ? data.extras as Record<string, unknown> : {};
        const isEnterprise = deriveIsEnterprise(data);
        const multipleDatacentersPerTenant = deriveMultipleDatacenters(data);

        setState({
          edition,
          capabilities,
          extras,
          isEnterprise,
          multipleDatacentersPerTenant,
          loading: false,
          error: null,
          hasCapability: (cap: string) => capabilities.includes(cap),
        });
      } catch (err) {
        if (cancelled) return;
        console.error('Product info fetch error:', err);
        setState((prev) => ({
          ...prev,
          loading: false,
          error: err instanceof Error ? err.message : 'Failed to load product info',
        }));
      }
    }

    fetchInfo();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <ProductInfoContext.Provider value={state}>
      {children}
    </ProductInfoContext.Provider>
  );
}

export function useProductInfo(): ProductInfoState {
  const context = useContext(ProductInfoContext);
  if (context === undefined) {
    throw new Error('useProductInfo must be used within ProductInfoProvider');
  }
  return context;
}

'use client';

import { createContext, useContext, useEffect, useState, useCallback, ReactNode } from 'react';

const STORAGE_KEY = 'selectedTenantSlug';

interface TenantContextType {
  selectedTenantSlug: string | null;
  setSelectedTenantSlug: (slug: string | null) => void;
}

const TenantContext = createContext<TenantContextType | undefined>(undefined);

export function TenantProvider({ children }: { children: ReactNode }) {
  const [selectedTenantSlug, setState] = useState<string | null>(null);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    const slug = sessionStorage.getItem(STORAGE_KEY);
    setState(slug);
  }, []);

  const setSelectedTenantSlug = useCallback((slug: string | null) => {
    if (typeof window === 'undefined') return;
    if (slug) {
      sessionStorage.setItem(STORAGE_KEY, slug);
    } else {
      sessionStorage.removeItem(STORAGE_KEY);
    }
    setState(slug);
  }, []);

  return (
    <TenantContext.Provider value={{ selectedTenantSlug, setSelectedTenantSlug }}>
      {children}
    </TenantContext.Provider>
  );
}

export function useTenant() {
  const ctx = useContext(TenantContext);
  if (ctx === undefined) {
    throw new Error('useTenant must be used within TenantProvider');
  }
  return ctx;
}

export function useTenantOptional() {
  return useContext(TenantContext);
}

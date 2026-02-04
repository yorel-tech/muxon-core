'use client';

import { useEffect, useState } from 'react';
import { apiGet } from './api';

export interface SidebarCounts {
  users: number;
  tenants: number;
  vms: number;
}

export interface PaginatedResponse {
  total: number;
  page: number;
  perPage?: number;
  per_page?: number;
  items: any[];
}

export function useSidebarCounts(userRole: 'system' | 'tenant', enabled = true) {
  const [counts, setCounts] = useState<SidebarCounts>({
    users: 0,
    tenants: 0,
    vms: 0,
  });
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
  if (!enabled) return;

  const fetchCounts = async () => {
    setIsLoading(true);
    setError(null);

    try {
      // Fetch counts based on user role
      const fetchPromises: Promise<any>[] = [];

      if (userRole === 'system') {
        // System users: fetch users and tenants counts only
        fetchPromises.push(
          apiGet<PaginatedResponse>('/api/v1/system-users', {
            baseUrl: process.env.NEXT_PUBLIC_API_BASE_URL,
            requireAuth: true,
          }).catch((err) => {
            console.error('Error fetching users count:', err);
            return { total: 0 };
          })
        );

        fetchPromises.push(
          apiGet<PaginatedResponse>('/api/v1/tenants', {
            baseUrl: process.env.NEXT_PUBLIC_API_BASE_URL,
            requireAuth: true,
          }).catch((err) => {
            console.error('Error fetching tenants count:', err);
            return { total: 0 };
          })
        );
      } else {
        // Tenant users: fetch VMs count only
        fetchPromises.push(
          apiGet<PaginatedResponse>('/api/v1/vms', {
            baseUrl: process.env.NEXT_PUBLIC_API_BASE_URL,
            requireAuth: true,
          }).catch((err) => {
            console.error('Error fetching VMs count:', err);
            return { total: 0 };
          })
        );
      }

      const results = await Promise.all(fetchPromises);

      // Map results based on user role
      const newCounts: SidebarCounts = {
        users: 0,
        tenants: 0,
        vms: 0,
      };

      if (userRole === 'system') {
        newCounts.users = results[0]?.total ?? 0;
        newCounts.tenants = results[1]?.total ?? 0;
        // System users don't have VMs in sidebar
        newCounts.vms = 0;
      } else {
        // Tenant users only get VMs count
        newCounts.vms = results[0]?.total ?? 0;
      }

      setCounts(newCounts);
    } catch (err) {
      console.error('Error fetching sidebar counts:', err);
      setError(err instanceof Error ? err : new Error('Failed to fetch counts'));
    } finally {
      setIsLoading(false);
    }
  };

  fetchCounts();
}, [userRole, enabled]);

  return { counts, isLoading, error, refetch: () => {} };
}

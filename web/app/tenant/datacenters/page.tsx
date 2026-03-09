'use client';

import { useState, useEffect, useCallback } from 'react';
import { Card, CardContent } from '@/components/ui/atoms/card';
import { Table, Column } from '@/components/ui/organisms/table';
import { motion } from 'framer-motion';
import { Loader2 } from 'lucide-react';
import { apiGet } from '@/lib/api';
import { useTenantId } from '@/lib/use-tenant-id';

/** ResourceLimits per OpenAPI commons */
interface ResourceLimits {
  maxCpus?: number;
  maxMemoryGb?: number;
  maxStorageGb?: number;
  maxVms?: number;
  maxVolumes?: number;
  maxLoadBalancers?: number;
}

/** Tenant datacenter grant (from GET /api/v1/tenants/{tenantId}/datacenters) */
interface TenantDatacenterGrant {
  id: string;
  tenantId?: string;
  datacenterId: string;
  datacenter?: { id: string; name?: string; description?: string };
  access?: boolean;
  limits?: ResourceLimits;
}

function formatLimits(l: ResourceLimits | undefined): string {
  if (!l || (l.maxVms == null && l.maxCpus == null && l.maxMemoryGb == null && l.maxStorageGb == null)) return '—';
  const parts = [
    l.maxVms != null && `VMs: ${l.maxVms}`,
    l.maxCpus != null && `vCPUs: ${l.maxCpus}`,
    l.maxMemoryGb != null && `RAM: ${l.maxMemoryGb} GB`,
    l.maxStorageGb != null && `Storage: ${l.maxStorageGb} GB`,
  ].filter(Boolean);
  return parts.join(', ');
}

export default function TenantDatacentersPage() {
  const { tenantId, loading: tenantLoading, error: tenantError } = useTenantId();
  const [grants, setGrants] = useState<TenantDatacenterGrant[]>([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    if (!tenantId) {
      setGrants([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const data = await apiGet<{ items?: TenantDatacenterGrant[] } | TenantDatacenterGrant[]>(
        `/api/v1/tenants/${tenantId}/datacenters?perPage=100`
      );
      const list = Array.isArray(data) ? data : data?.items ?? [];
      const withAccess = list.filter((g: TenantDatacenterGrant) => g.access !== false);
      setGrants(withAccess);
    } catch {
      setGrants([]);
    } finally {
      setLoading(false);
    }
  }, [tenantId]);

  useEffect(() => {
    load();
  }, [load]);

  const columns: Column<TenantDatacenterGrant>[] = [
    {
      key: 'name',
      header: 'Name',
      cell: (row) => (
        <div className="font-medium text-gray-900 dark:text-gray-100">
          {row.datacenter?.name ?? row.datacenterId}
        </div>
      ),
      sortable: true,
    },
    {
      key: 'description',
      header: 'Description',
      cell: (row) => (
        <span className="text-gray-600 dark:text-gray-400 text-sm">
          {row.datacenter?.description ?? '—'}
        </span>
      ),
      sortable: true,
    },
    {
      key: 'limits',
      header: 'Limits',
      cell: (row) => (
        <span className="text-gray-600 dark:text-gray-400 text-sm">
          {formatLimits(row.limits)}
        </span>
      ),
      sortable: false,
    },
  ];

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="max-w-full px-3 py-8">
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="mb-8"
        >
          <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100">Datacenters</h1>
          <p className="text-gray-600 dark:text-gray-400 mt-2">Read-only view of datacenters available to your tenant</p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.1 }}
        >
          <Card className="dark:border-gray-700">
            <CardContent className="p-0">
              {tenantError && (
                <div className="p-4 text-sm text-amber-700 dark:text-amber-200 bg-amber-50 dark:bg-amber-900/20 border-b border-amber-200 dark:border-amber-800">
                  {tenantError}. Select a tenant from the system tenant list to open the portal.
                </div>
              )}
              {tenantLoading || loading ? (
                <div className="flex items-center justify-center py-12">
                  <Loader2 className="h-8 w-8 animate-spin text-gray-400" />
                </div>
              ) : !tenantId ? (
                <div className="py-12 text-center text-gray-500 dark:text-gray-400 text-sm">
                  No tenant selected. Open the tenant portal from a tenant in the system area.
                </div>
              ) : (
                <Table
                  columns={columns}
                  data={grants}
                  emptyMessage="No datacenters available"
                  overflowVisibleColumnKeys={[]}
                />
              )}
            </CardContent>
          </Card>
        </motion.div>
      </div>
    </div>
  );
}

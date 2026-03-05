'use client';

import { useState, useEffect, useCallback } from 'react';
import { Card, CardContent } from '@/components/ui/atoms/card';
import { Table, Column } from '@/components/ui/organisms/table';
import { Badge } from '@/components/ui/atoms/badge';
import { Button } from '@/components/ui/atoms/button';
import { Input } from '@/components/ui/atoms/input';
import { Modal } from '@/components/ui/molecules/modal';
import { motion } from 'framer-motion';
import { Plus, Loader2 } from 'lucide-react';
import { apiGet, apiPost } from '@/lib/api';

interface VmRow {
  id: string;
  name: string;
  status?: string;
  flavor?: string;
  image?: string;
  datacenter?: string;
  createdAt?: string;
}

/** Tenant datacenter grant (from GET /api/v1/tenants/{id}/datacenters) */
interface TenantDatacenterGrant {
  id: string;
  tenantId?: string;
  datacenterId: string;
  datacenter?: { id: string; name?: string; description?: string };
  access?: boolean;
}

/** Default VM spec for Create VM (matches OpenAPI VmSpec / Libvirt) */
function defaultVmSpec() {
  return {
    cpu: { cores: 2, sockets: 1, threads: 1, type: 'EMULATED' as const },
    memory: { size_mb: 2048, overcommit_ratio: 1.0 },
    storage: [
      { type: 'root' as const, size_gb: 20, bootable: true },
    ],
    network: [
      { type: 'primary' as const, network: 'default', ip_allocation: 'dhcp' as const },
    ],
    os: { type: 'linux' as const, distribution: 'ubuntu', version: '22.04' },
  };
}

export default function TenantVmsPage() {
  const [vms, setVms] = useState<VmRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [grants, setGrants] = useState<TenantDatacenterGrant[]>([]);
  const [grantsLoading, setGrantsLoading] = useState(false);
  const [createName, setCreateName] = useState('');
  const [createGrantId, setCreateGrantId] = useState('');
  const [createSubmitting, setCreateSubmitting] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  const loadVms = useCallback(async () => {
    try {
      const data = await apiGet<{ items?: VmRow[]; total?: number }>('/api/v1/vms');
      const list = Array.isArray(data) ? data : data?.items ?? [];
      setVms(
        list.map((vm: Record<string, unknown>) => ({
          id: String(vm.id ?? ''),
          name: String(vm.name ?? vm.id ?? ''),
          status: (vm.status as string) ?? 'unknown',
          flavor: (vm.flavor as string) ?? (vm.vmClass as string),
          image: (vm.image as string) ?? (vm.imageId as string),
          datacenter: (vm.datacenterId as string) ?? (vm.datacenter as string),
          createdAt: vm.createdAt as string,
        }))
      );
    } catch {
      setVms([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadVms();
  }, [loadVms]);

  const loadGrants = useCallback(async () => {
    setGrantsLoading(true);
    try {
      const current = await apiGet<{ id?: string }>('/api/v1/tenants/current').catch(() => ({}));
      const tenantId = (current as { id?: string })?.id;
      if (!tenantId) {
        setGrants([]);
        return;
      }
      const data = await apiGet<{ items?: TenantDatacenterGrant[] }>(
        `/api/v1/tenants/${tenantId}/datacenters?perPage=100`
      );
      const list = Array.isArray(data) ? data : data?.items ?? [];
      const withId = list.filter((g) => g.access !== false && g.id);
      setGrants(withId);
      if (withId.length > 0 && !createGrantId) {
        setCreateGrantId(withId[0].id);
      }
    } catch {
      setGrants([]);
    } finally {
      setGrantsLoading(false);
    }
  }, [createGrantId]);

  useEffect(() => {
    if (createModalOpen) {
      loadGrants();
    }
  }, [createModalOpen, loadGrants]);

  const handleCreateVm = async () => {
    const name = createName.trim();
    if (!name) {
      setCreateError('Name is required.');
      return;
    }
    if (name.length > 63) {
      setCreateError('Name must be at most 63 characters.');
      return;
    }
    if (!createGrantId) {
      setCreateError('Please select a datacenter.');
      return;
    }
    setCreateError(null);
    setCreateSubmitting(true);
    try {
      await apiPost('/api/v1/vms', {
        name,
        tenant_datacenter_grant_id: createGrantId,
        spec: defaultVmSpec(),
      });
      setCreateModalOpen(false);
      setCreateName('');
      setCreateGrantId(grants[0]?.id ?? '');
      await loadVms();
    } catch (e) {
      setCreateError(e instanceof Error ? e.message : 'Failed to create VM.');
    } finally {
      setCreateSubmitting(false);
    }
  };

  const getStatusVariant = (status: string): 'success' | 'warning' | 'destructive' | 'default' => {
    if (status === 'running' || status === 'active') return 'success';
    if (status === 'stopped' || status === 'paused') return 'default';
    if (status === 'error' || status === 'failed') return 'destructive';
    return 'warning';
  };

  const columns: Column<VmRow>[] = [
    {
      key: 'name',
      header: 'Name',
      cell: (row) => <div className="font-medium text-gray-900 dark:text-gray-100">{row.name}</div>,
      sortable: true,
    },
    {
      key: 'status',
      header: 'Status',
      cell: (row) => <Badge variant={getStatusVariant(row.status ?? '')}>{row.status ?? '—'}</Badge>,
      sortable: true,
    },
    {
      key: 'flavor',
      header: 'Flavor',
      cell: (row) => <span className="text-gray-600 dark:text-gray-400 text-sm">{row.flavor ?? '—'}</span>,
      sortable: true,
    },
    {
      key: 'image',
      header: 'Image',
      cell: (row) => <span className="text-gray-600 dark:text-gray-400 text-sm truncate max-w-[120px] block">{row.image ?? '—'}</span>,
      sortable: true,
    },
    {
      key: 'datacenter',
      header: 'Datacenter',
      cell: (row) => <span className="text-gray-600 dark:text-gray-400 text-sm">{row.datacenter ?? '—'}</span>,
      sortable: true,
    },
    {
      key: 'createdAt',
      header: 'Created',
      cell: (row) => (
        <span className="text-gray-500 dark:text-gray-400 text-sm">
          {row.createdAt ? new Date(row.createdAt).toLocaleDateString() : '—'}
        </span>
      ),
      sortable: true,
    },
  ];

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="max-w-full px-3 py-8">
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="mb-8 flex items-center justify-between"
        >
          <div>
            <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100">Virtual Machines</h1>
            <p className="text-gray-600 dark:text-gray-400 mt-2">Manage your tenant VMs</p>
          </div>
          <Button
            onClick={() => setCreateModalOpen(true)}
            className="flex items-center gap-2"
          >
            <Plus size={18} />
            Create VM
          </Button>
        </motion.div>

        <Modal
          isOpen={createModalOpen}
          onClose={() => !createSubmitting && setCreateModalOpen(false)}
          title="Create VM"
          size="lg"
        >
          <div className="space-y-4">
            <Input
              label="VM name"
              placeholder="my-vm"
              value={createName}
              onChange={(e) => setCreateName(e.target.value)}
              fullWidth
              disabled={createSubmitting}
            />
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                Datacenter
              </label>
              <select
                className="w-full rounded-md border border-gray-300 bg-white px-4 py-2 text-gray-900 shadow-sm focus:border-primary-500 focus:ring-primary-500 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-100"
                value={createGrantId}
                onChange={(e) => setCreateGrantId(e.target.value)}
                disabled={createSubmitting || grantsLoading}
              >
                <option value="">Select datacenter</option>
                {grants.map((g) => (
                  <option key={g.id} value={g.id}>
                    {g.datacenter?.name ?? g.datacenterId}
                  </option>
                ))}
              </select>
              {grantsLoading && (
                <p className="mt-1 text-sm text-gray-500">Loading datacenters…</p>
              )}
            </div>
            <p className="text-sm text-gray-500 dark:text-gray-400">
              Default: 2 vCPU, 2 GB RAM, 20 GB root disk, Ubuntu 22.04.
            </p>
            {createError && (
              <p className="text-sm text-red-600 dark:text-red-400">{createError}</p>
            )}
            <div className="flex justify-end gap-2 pt-2">
              <Button
                variant="secondary"
                onClick={() => setCreateModalOpen(false)}
                disabled={createSubmitting}
              >
                Cancel
              </Button>
              <Button
                onClick={handleCreateVm}
                disabled={createSubmitting || grantsLoading || grants.length === 0}
                isLoading={createSubmitting}
              >
                {createSubmitting ? 'Creating…' : 'Create VM'}
              </Button>
            </div>
          </div>
        </Modal>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.1 }}
        >
          <Card className="dark:border-gray-700">
            <CardContent className="p-0">
              {loading ? (
                <div className="flex items-center justify-center py-12">
                  <Loader2 className="h-8 w-8 animate-spin text-gray-400" />
                </div>
              ) : (
                <Table
                  columns={columns}
                  data={vms}
                  emptyMessage="No VMs yet. Create one to get started."
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

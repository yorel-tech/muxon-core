'use client';

import { useState, useEffect, useCallback } from 'react';
import { Card, CardContent } from '@/components/ui/atoms/card';
import { Table, Column } from '@/components/ui/organisms/table';
import { Badge } from '@/components/ui/atoms/badge';
import { Button } from '@/components/ui/atoms/button';
import { Input } from '@/components/ui/atoms/input';
import { Modal } from '@/components/ui/molecules/modal';
import { motion } from 'framer-motion';
import { Plus, Loader2, ChevronLeft, ChevronRight, Trash2 } from 'lucide-react';
import { apiGet, apiPost } from '@/lib/api';
import { useTenantId } from '@/lib/use-tenant-id';

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

/** Form types aligned with OpenAPI vms.yaml (ComputeSpec, StorageSpec, NetworkSpec, OsSpec) */
interface DiskSpec {
  sizeMb: number;
  storageClass?: string;
}
interface NicSpec {
  isPrimary: boolean;
  network?: string;
  ip_allocation: 'dhcp' | 'static' | 'pool';
  ip_address?: string | null;
}
interface ComputeSpec {
  cpus: number;
  memorySizeMb: number;
}
interface StorageSpec {
  disks: DiskSpec[];
  vmStorageClass?: string;
}
interface NetworkSpec {
  nics: NicSpec[];
}
interface OsSpec {
  type: 'linux' | 'windows' | 'bsd';
  distribution?: string;
  version?: string;
}
interface VmSpec {
  compute: ComputeSpec;
  storage: StorageSpec;
  network?: NetworkSpec;
  os?: OsSpec;
}
interface VmCreateForm {
  name: string;
  description?: string;
  tenant_datacenter_grant_id: string;
  spec: VmSpec;
}

const WIZARD_STEPS = ['Basics', 'Compute', 'Storage', 'Network', 'OS', 'Review'] as const;
const NAME_PATTERN = /^[a-zA-Z0-9]([-a-zA-Z0-9]*[a-zA-Z0-9])?$/;

function defaultVmSpec(): VmSpec {
  return {
    compute: { cpus: 2, memorySizeMb: 2048 },
    storage: {
      disks: [{ sizeMb: 20 * 1024, storageClass: undefined }],
      vmStorageClass: undefined,
    },
    network: {
      nics: [{ isPrimary: true, network: 'default', ip_allocation: 'dhcp' }],
    },
    os: { type: 'linux', distribution: 'ubuntu', version: '22.04' },
  };
}

function defaultCreateForm(initialGrantId: string): VmCreateForm {
  return {
    name: '',
    description: '',
    tenant_datacenter_grant_id: initialGrantId,
    spec: defaultVmSpec(),
  };
}

export default function TenantVmsPage() {
  const { tenantId } = useTenantId();
  const [vms, setVms] = useState<VmRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [grants, setGrants] = useState<TenantDatacenterGrant[]>([]);
  const [grantsLoading, setGrantsLoading] = useState(false);
  const [wizardStep, setWizardStep] = useState(0);
  const [createForm, setCreateForm] = useState<VmCreateForm>(() => defaultCreateForm(''));
  const [createSubmitting, setCreateSubmitting] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  const loadVms = useCallback(async () => {
    try {
      const data = await apiGet<{ items?: unknown[]; total?: number }>('/api/v1/vms');
      const list = Array.isArray(data) ? data : data?.items ?? [];
      setVms(
        (list as Record<string, unknown>[]).map((vm) => ({
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
    if (!tenantId) {
      setGrants([]);
      return;
    }
    setGrantsLoading(true);
    try {
      const data = await apiGet<{ items?: TenantDatacenterGrant[] }>(
        `/api/v1/tenants/${tenantId}/datacenters?perPage=100`
      );
      const list = Array.isArray(data) ? data : data?.items ?? [];
      const withId = list.filter((g) => g.access !== false && g.id);
      setGrants(withId);
      if (withId.length > 0) {
        setCreateForm((f) => (f.tenant_datacenter_grant_id ? f : { ...f, tenant_datacenter_grant_id: withId[0].id }));
      }
    } catch {
      setGrants([]);
    } finally {
      setGrantsLoading(false);
    }
  }, [tenantId]);

  useEffect(() => {
    if (createModalOpen && tenantId) {
      loadGrants();
    }
  }, [createModalOpen, tenantId, loadGrants]);

  const handleOpenWizard = () => {
    setCreateModalOpen(true);
    setWizardStep(0);
    setCreateError(null);
    setCreateForm(defaultCreateForm(grants[0]?.id ?? ''));
  };

  const handleCloseWizard = () => {
    if (!createSubmitting) {
      setCreateModalOpen(false);
      setWizardStep(0);
      setCreateError(null);
    }
  };

  function validateBasics(): string | null {
    const name = createForm.name.trim();
    if (!name) return 'Name is required.';
    if (name.length > 63) return 'Name must be at most 63 characters.';
    if (!NAME_PATTERN.test(name)) return 'Name must use only letters, numbers, and hyphens (e.g. my-vm).';
    if (!createForm.tenant_datacenter_grant_id) return 'Please select a datacenter.';
    return null;
  }

  function validateCompute(): string | null {
    const { cpus, memorySizeMb } = createForm.spec.compute;
    if (cpus < 1 || cpus > 128) return 'CPUs must be between 1 and 128.';
    if (memorySizeMb < 512 || memorySizeMb > 1048576) return 'Memory must be between 512 MB and 1048576 MB.';
    return null;
  }

  function validateStorage(): string | null {
    const { disks } = createForm.spec.storage;
    if (!disks.length) return 'Add at least one disk.';
    for (let i = 0; i < disks.length; i++) {
      if (disks[i].sizeMb < 1) return `Disk ${i + 1}: size must be at least 1 MB.`;
    }
    return null;
  }

  const handleNext = () => {
    setCreateError(null);
    if (wizardStep === 0) {
      const err = validateBasics();
      if (err) {
        setCreateError(err);
        return;
      }
    } else if (wizardStep === 1) {
      const err = validateCompute();
      if (err) {
        setCreateError(err);
        return;
      }
    } else if (wizardStep === 2) {
      const err = validateStorage();
      if (err) {
        setCreateError(err);
        return;
      }
    }
    setWizardStep((s) => Math.min(s + 1, WIZARD_STEPS.length - 1));
  };

  const handleCreateVm = async () => {
    const err = validateBasics() ?? validateCompute() ?? validateStorage();
    if (err) {
      setCreateError(err);
      return;
    }
    setCreateError(null);
    setCreateSubmitting(true);
    try {
      const payload = {
        name: createForm.name.trim(),
        description: createForm.description?.trim() || undefined,
        tenant_datacenter_grant_id: createForm.tenant_datacenter_grant_id,
        spec: {
          compute: createForm.spec.compute,
          storage: createForm.spec.storage,
          network: createForm.spec.network?.nics?.length ? { nics: createForm.spec.network.nics } : undefined,
          os: createForm.spec.os,
        },
      };
      await apiPost('/api/v1/vms', payload);
      handleCloseWizard();
      setCreateForm(defaultCreateForm(grants[0]?.id ?? ''));
      await loadVms();
    } catch (e) {
      setCreateError(e instanceof Error ? e.message : 'Failed to create VM.');
    } finally {
      setCreateSubmitting(false);
    }
  };

  const getStatusVariant = (status: string): 'success' | 'warning' | 'error' | 'default' => {
    if (status === 'running' || status === 'active') return 'success';
    if (status === 'stopped' || status === 'paused') return 'default';
    if (status === 'error' || status === 'failed') return 'error';
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
            onClick={handleOpenWizard}
            className="flex items-center gap-2"
          >
            <Plus size={18} />
            Create VM
          </Button>
        </motion.div>

        <Modal
          isOpen={createModalOpen}
          onClose={handleCloseWizard}
          title="Create VM"
          size="xl"
        >
          <div className="flex flex-col max-h-[80vh]">
            {/* Step indicator */}
            <div className="flex flex-wrap gap-x-4 gap-y-1 border-b border-gray-200 dark:border-gray-700 pb-3 mb-4">
              {WIZARD_STEPS.map((label, i) => (
                <span
                  key={label}
                  className={`text-sm ${i === wizardStep ? 'font-medium text-primary-600 dark:text-primary-400' : 'text-gray-500 dark:text-gray-400'}`}
                >
                  {i + 1}. {label}
                </span>
              ))}
            </div>

            {createError && (
              <div className="rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 px-4 py-2 text-sm text-red-800 dark:text-red-200 mb-4">
                {createError}
              </div>
            )}

            <div className="flex-1 overflow-y-auto min-h-0 space-y-4">
              {/* Step 0: Basics */}
              {wizardStep === 0 && (
                <div className="space-y-4">
                  <Input
                    label="VM name"
                    placeholder="my-vm"
                    value={createForm.name}
                    onChange={(e) => setCreateForm((f) => ({ ...f, name: e.target.value }))}
                    fullWidth
                    disabled={createSubmitting}
                  />
                  <Input
                    label="Description (optional)"
                    placeholder="Short description"
                    value={createForm.description ?? ''}
                    onChange={(e) => setCreateForm((f) => ({ ...f, description: e.target.value }))}
                    fullWidth
                    disabled={createSubmitting}
                  />
                  <div>
                    <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                      Datacenter
                    </label>
                    <select
                      className="w-full rounded-md border border-gray-300 bg-white px-4 py-2 text-gray-900 shadow-sm focus:border-primary-500 focus:ring-primary-500 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-100"
                      value={createForm.tenant_datacenter_grant_id}
                      onChange={(e) => setCreateForm((f) => ({ ...f, tenant_datacenter_grant_id: e.target.value }))}
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
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    Name: letters, numbers, hyphens only; 1–63 characters.
                  </p>
                </div>
              )}

              {/* Step 1: Compute */}
              {wizardStep === 1 && (
                <div className="space-y-4">
                  <Input
                    label="CPUs"
                    type="number"
                    value={String(createForm.spec.compute.cpus)}
                    onChange={(e) =>
                      setCreateForm((f) => ({
                        ...f,
                        spec: {
                          ...f.spec,
                          compute: { ...f.spec.compute, cpus: Math.max(1, Math.min(128, parseInt(e.target.value, 10) || 1)) },
                        },
                      }))
                    }
                    fullWidth
                    disabled={createSubmitting}
                  />
                  <Input
                    label="Memory (MB)"
                    type="number"
                    value={String(createForm.spec.compute.memorySizeMb)}
                    onChange={(e) =>
                      setCreateForm((f) => ({
                        ...f,
                        spec: {
                          ...f.spec,
                          compute: { ...f.spec.compute, memorySizeMb: Math.max(512, Math.min(1048576, parseInt(e.target.value, 10) || 512)) },
                        },
                      }))
                    }
                    fullWidth
                    disabled={createSubmitting}
                  />
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    CPUs: 1–128. Memory: 512 MB–1 TB.
                  </p>
                </div>
              )}

              {/* Step 2: Storage */}
              {wizardStep === 2 && (
                <div className="space-y-4">
                  <div>
                    <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
                      VM storage class (optional)
                    </label>
                    <Input
                      placeholder="e.g. ssd, hdd"
                      value={createForm.spec.storage.vmStorageClass ?? ''}
                      onChange={(e) =>
                        setCreateForm((f) => ({
                          ...f,
                          spec: {
                            ...f.spec,
                            storage: { ...f.spec.storage, vmStorageClass: e.target.value || undefined },
                          },
                        }))
                      }
                      fullWidth
                      disabled={createSubmitting}
                    />
                  </div>
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Disks</label>
                      <Button
                        type="button"
                        variant="secondary"
                        size="sm"
                        onClick={() =>
                          setCreateForm((f) => ({
                            ...f,
                            spec: {
                              ...f.spec,
                              storage: {
                                ...f.spec.storage,
                                disks: [...f.spec.storage.disks, { sizeMb: 20 * 1024 }],
                              },
                            },
                          }))
                        }
                        disabled={createSubmitting}
                      >
                        Add disk
                      </Button>
                    </div>
                    <div className="space-y-2">
                      {createForm.spec.storage.disks.map((disk, idx) => (
                        <div key={idx} className="flex gap-2 items-center rounded border border-gray-200 dark:border-gray-700 p-2">
                          <Input
                            type="number"
                            placeholder="Size (MB)"
                            value={String(disk.sizeMb)}
                            onChange={(e) =>
                              setCreateForm((f) => {
                                const disks = [...f.spec.storage.disks];
                                disks[idx] = { ...disks[idx], sizeMb: Math.max(1, parseInt(e.target.value, 10) || 1) };
                                return { ...f, spec: { ...f.spec, storage: { ...f.spec.storage, disks } } };
                              })
                            }
                            className="w-32"
                            disabled={createSubmitting}
                          />
                          <Input
                            placeholder="Storage class"
                            value={disk.storageClass ?? ''}
                            onChange={(e) =>
                              setCreateForm((f) => {
                                const disks = [...f.spec.storage.disks];
                                disks[idx] = { ...disks[idx], storageClass: e.target.value || undefined };
                                return { ...f, spec: { ...f.spec, storage: { ...f.spec.storage, disks } } };
                              })
                            }
                            className="flex-1"
                            disabled={createSubmitting}
                          />
                          <button
                            type="button"
                            onClick={() =>
                              setCreateForm((f) => ({
                                ...f,
                                spec: {
                                  ...f.spec,
                                  storage: {
                                    ...f.spec.storage,
                                    disks: f.spec.storage.disks.filter((_, i) => i !== idx),
                                  },
                                },
                              }))
                            }
                            disabled={createSubmitting || createForm.spec.storage.disks.length <= 1}
                            className="p-2 text-gray-500 hover:text-red-600 rounded"
                            aria-label="Remove disk"
                          >
                            <Trash2 size={18} />
                          </button>
                        </div>
                      ))}
                    </div>
                    <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">At least one disk required. Size in MB.</p>
                  </div>
                </div>
              )}

              {/* Step 3: Network */}
              {wizardStep === 3 && (
                <div className="space-y-4">
                  <div className="flex items-center justify-between mb-2">
                    <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Network interfaces</label>
                    <Button
                      type="button"
                      variant="secondary"
                      size="sm"
                      onClick={() =>
                        setCreateForm((f) => ({
                          ...f,
                          spec: {
                            ...f.spec,
                            network: {
                              nics: [...(f.spec.network?.nics ?? []), { isPrimary: false, network: '', ip_allocation: 'dhcp' }],
                            },
                          },
                        }))
                      }
                      disabled={createSubmitting}
                    >
                      Add NIC
                    </Button>
                  </div>
                  {(createForm.spec.network?.nics?.length ?? 0) === 0 ? (
                    <p className="text-sm text-gray-500">No NICs. Add one or leave empty to use defaults.</p>
                  ) : (
                    <div className="space-y-2">
                      {(createForm.spec.network?.nics ?? []).map((nic, idx) => (
                        <div key={idx} className="rounded border border-gray-200 dark:border-gray-700 p-3 space-y-2">
                          <div className="flex items-center gap-2">
                            <label className="flex items-center gap-1.5 text-sm">
                              <input
                                type="checkbox"
                                checked={nic.isPrimary}
                                onChange={(e) =>
                                  setCreateForm((f) => {
                                    const nics = [...(f.spec.network?.nics ?? [])];
                                    nics[idx] = { ...nics[idx], isPrimary: e.target.checked };
                                    return { ...f, spec: { ...f.spec, network: { nics } } };
                                  })
                                }
                                disabled={createSubmitting}
                              />
                              Primary
                            </label>
                            <button
                              type="button"
                              onClick={() =>
                                setCreateForm((f) => ({
                                  ...f,
                                  spec: {
                                    ...f.spec,
                                    network: { nics: (f.spec.network?.nics ?? []).filter((_, i) => i !== idx) },
                                  },
                                }))
                              }
                              disabled={createSubmitting || (createForm.spec.network?.nics?.length ?? 0) <= 1}
                              className="p-1 text-gray-500 hover:text-red-600 rounded"
                              aria-label="Remove NIC"
                            >
                              <Trash2 size={16} />
                            </button>
                          </div>
                          <Input
                            label="Network name or ID"
                            placeholder="default"
                            value={nic.network ?? ''}
                            onChange={(e) =>
                              setCreateForm((f) => {
                                const nics = [...(f.spec.network?.nics ?? [])];
                                nics[idx] = { ...nics[idx], network: e.target.value || undefined };
                                return { ...f, spec: { ...f.spec, network: { nics } } };
                              })
                            }
                            fullWidth
                            disabled={createSubmitting}
                          />
                          <div className="grid grid-cols-2 gap-2">
                            <div>
                              <label className="mb-1 block text-xs text-gray-500">IP allocation</label>
                              <select
                                className="w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-800 dark:text-gray-100"
                                value={nic.ip_allocation}
                                onChange={(e) =>
                                  setCreateForm((f) => {
                                    const nics = [...(f.spec.network?.nics ?? [])];
                                    nics[idx] = { ...nics[idx], ip_allocation: e.target.value as 'dhcp' | 'static' | 'pool' };
                                    return { ...f, spec: { ...f.spec, network: { nics } } };
                                  })
                                }
                                disabled={createSubmitting}
                              >
                                <option value="dhcp">DHCP</option>
                                <option value="static">Static</option>
                                <option value="pool">Pool</option>
                              </select>
                            </div>
                            {nic.ip_allocation === 'static' && (
                              <Input
                                label="IP address"
                                placeholder="192.168.1.10"
                                value={nic.ip_address ?? ''}
                                onChange={(e) =>
                                  setCreateForm((f) => {
                                    const nics = [...(f.spec.network?.nics ?? [])];
                                    nics[idx] = { ...nics[idx], ip_address: e.target.value || null };
                                    return { ...f, spec: { ...f.spec, network: { nics } } };
                                  })
                                }
                                fullWidth
                                disabled={createSubmitting}
                              />
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}

              {/* Step 4: OS */}
              {wizardStep === 4 && (
                <div className="space-y-4">
                  <div>
                    <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">OS type</label>
                    <select
                      className="w-full rounded-md border border-gray-300 bg-white px-4 py-2 text-gray-900 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-100"
                      value={createForm.spec.os?.type ?? 'linux'}
                      onChange={(e) =>
                        setCreateForm((f) => ({
                          ...f,
                          spec: {
                            ...f.spec,
                            os: { ...f.spec.os!, type: e.target.value as 'linux' | 'windows' | 'bsd' },
                          },
                        }))
                      }
                      disabled={createSubmitting}
                    >
                      <option value="linux">Linux</option>
                      <option value="windows">Windows</option>
                      <option value="bsd">BSD</option>
                    </select>
                  </div>
                  <Input
                    label="Distribution (optional)"
                    placeholder="e.g. ubuntu, centos, windows-server"
                    value={createForm.spec.os?.distribution ?? ''}
                    onChange={(e) =>
                      setCreateForm((f) => ({
                        ...f,
                        spec: { ...f.spec, os: { ...f.spec.os!, distribution: e.target.value || undefined } },
                      }))
                    }
                    fullWidth
                    disabled={createSubmitting}
                  />
                  <Input
                    label="Version (optional)"
                    placeholder="e.g. 22.04"
                    value={createForm.spec.os?.version ?? ''}
                    onChange={(e) =>
                      setCreateForm((f) => ({
                        ...f,
                        spec: { ...f.spec, os: { ...f.spec.os!, version: e.target.value || undefined } },
                      }))
                    }
                    fullWidth
                    disabled={createSubmitting}
                  />
                </div>
              )}

              {/* Step 5: Review */}
              {wizardStep === 5 && (
                <div className="space-y-3 text-sm">
                  <p><span className="font-medium text-gray-700 dark:text-gray-300">Name:</span> {createForm.name || '—'}</p>
                  <p><span className="font-medium text-gray-700 dark:text-gray-300">Datacenter:</span> {grants.find((g) => g.id === createForm.tenant_datacenter_grant_id)?.datacenter?.name ?? createForm.tenant_datacenter_grant_id ?? '—'}</p>
                  <p><span className="font-medium text-gray-700 dark:text-gray-300">Compute:</span> {createForm.spec.compute.cpus} CPUs, {createForm.spec.compute.memorySizeMb} MB RAM</p>
                  <p><span className="font-medium text-gray-700 dark:text-gray-300">Storage:</span> {createForm.spec.storage.disks.length} disk(s) — {createForm.spec.storage.disks.map((d) => `${d.sizeMb} MB`).join(', ')}</p>
                  <p><span className="font-medium text-gray-700 dark:text-gray-300">Network:</span> {(createForm.spec.network?.nics?.length ?? 0) > 0 ? createForm.spec.network!.nics.length + ' NIC(s)' : 'default'}</p>
                  <p><span className="font-medium text-gray-700 dark:text-gray-300">OS:</span> {createForm.spec.os?.type ?? 'linux'} {[createForm.spec.os?.distribution, createForm.spec.os?.version].filter(Boolean).join(' ')}</p>
                </div>
              )}
            </div>

            <div className="flex justify-between gap-2 pt-4 mt-4 border-t border-gray-200 dark:border-gray-700">
              <Button
                variant="secondary"
                onClick={() => (wizardStep > 0 ? setWizardStep((s) => s - 1) : handleCloseWizard())}
                disabled={createSubmitting}
              >
                {wizardStep > 0 ? (
                  <>
                    <ChevronLeft size={18} className="mr-1" />
                    Back
                  </>
                ) : (
                  'Cancel'
                )}
              </Button>
              {wizardStep < WIZARD_STEPS.length - 1 ? (
                <Button onClick={handleNext} disabled={createSubmitting || grantsLoading}>
                  Next
                  <ChevronRight size={18} className="ml-1" />
                </Button>
              ) : (
                <Button
                  onClick={handleCreateVm}
                  disabled={createSubmitting || grants.length === 0}
                  isLoading={createSubmitting}
                >
                  {createSubmitting ? 'Creating…' : 'Create VM'}
                </Button>
              )}
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

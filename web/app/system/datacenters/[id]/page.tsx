'use client';

import { useState, useEffect, use } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { Card, CardContent, CardHeader } from '@/components/ui/atoms/card';
import { Button } from '@/components/ui/atoms/button';
import { Input } from '@/components/ui/atoms/input';
import { motion, AnimatePresence } from 'framer-motion';
import {
  ArrowLeft,
  Loader2,
  Pencil,
  Settings,
  FileText,
  Server,
  X,
} from 'lucide-react';
import { apiGet, apiPut, executeLinkAction } from '@/lib/api';
import { findLink, normalizeEntityLinks } from '@/lib/hateoas';
import { DetailRow, formatDetailDate } from '@/components/entity-detail/DetailRow';
import { Tabs } from '@/components/ui/molecules/tabs';
import type { Link } from '@/types/provider';

interface DatacenterDetail {
  id: string;
  name: string;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
  capacity?: {
    totalCpus?: number;
    totalMemoryGb?: number;
    totalStorageGb?: number;
    availableCpus?: number;
    availableMemoryGb?: number;
    availableStorageGb?: number;
    usedCpus?: number;
    usedMemoryGb?: number;
    usedStorageGb?: number;
  };
  settings?: {
    providerType?: string;
    vmClasses?: string[];
    storageClasses?: string[];
    networkDomains?: string[];
    providerSpecificSettings?: Record<string, unknown>;
  };
  metadata?: Record<string, string>;
  nodeCluster?: { id: string; name?: string };
  _links?: Link[];
}

export default function DatacenterDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const searchParams = useSearchParams();
  const fromTenant = searchParams.get('from') === 'tenant';
  const tenantId = searchParams.get('tenantId');
  const backToTenant = fromTenant && tenantId;
  const [datacenter, setDatacenter] = useState<DatacenterDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [editModal, setEditModal] = useState<'general' | 'settings' | 'metadata' | null>(null);
  const [editName, setEditName] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [editVmClasses, setEditVmClasses] = useState('');
  const [editStorageClasses, setEditStorageClasses] = useState('');
  const [editNetworkDomains, setEditNetworkDomains] = useState('');
  const [editProviderSpecificSettings, setEditProviderSpecificSettings] = useState('');
  const [editMetadata, setEditMetadata] = useState<{ key: string; value: string }[]>([]);

  const links = normalizeEntityLinks(datacenter ?? {});
  const editLink = findLink(links, 'edit');
  const replaceSettingsLink = findLink(links, 'replaceSettings');
  const updateMetadataLink = findLink(links, 'updateMetadata');
  // If backend doesn't return replaceSettings/updateMetadata, allow Edit when general edit is allowed (same permission)
  const canEditSettings = replaceSettingsLink?.enabled ?? editLink?.enabled ?? false;
  const canEditMetadata = updateMetadataLink?.enabled ?? editLink?.enabled ?? false;

  useEffect(() => {
    let cancelled = false;
    async function fetchOne() {
      setLoading(true);
      setError(null);
      try {
        const data = await apiGet(`/api/v1/datacenters/${id}`);
        if (!cancelled) setDatacenter(data as DatacenterDetail);
      } catch (e) {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : 'Failed to load datacenter');
          setDatacenter(null);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    fetchOne();
    return () => {
      cancelled = true;
    };
  }, [id]);

  const refetch = async () => {
    try {
      const data = await apiGet(`/api/v1/datacenters/${id}`);
      setDatacenter(data as DatacenterDetail);
    } catch {
      // keep current state
    }
  };

  const handleEditGeneral = async () => {
    if (!editLink || !datacenter) return;
    setActionLoading('edit');
    try {
      await executeLinkAction(editLink, {
        name: editName,
        description: editDescription || undefined,
        capacity: datacenter.capacity,
        settings: datacenter.settings,
      });
      setEditModal(null);
      await refetch();
    } catch (e) {
      alert(e instanceof Error ? e.message : 'Update failed');
    } finally {
      setActionLoading(null);
    }
  };

  const handleReplaceSettings = async () => {
    if (!canEditSettings) return;
    setActionLoading('settings');
    try {
      const vmClasses = editVmClasses
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean);
      const storageClasses = editStorageClasses
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean);
      const networkDomains = editNetworkDomains
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean);
      let providerSpecificSettings: Record<string, unknown> = {};
      if (editProviderSpecificSettings.trim()) {
        try {
          providerSpecificSettings = JSON.parse(editProviderSpecificSettings) as Record<string, unknown>;
        } catch {
          alert('Provider-specific settings must be valid JSON');
          setActionLoading(null);
          return;
        }
      }
      const payload = {
        ...(datacenter?.settings?.providerType != null && { providerType: datacenter.settings.providerType }),
        vmClasses,
        storageClasses,
        networkDomains,
        providerSpecificSettings,
      };
      if (replaceSettingsLink?.enabled) {
        await executeLinkAction(replaceSettingsLink, payload);
      } else {
        await apiPut(`/api/v1/datacenters/${id}/settings`, payload);
      }
      setEditModal(null);
      await refetch();
    } catch (e) {
      alert(e instanceof Error ? e.message : 'Update failed');
    } finally {
      setActionLoading(null);
    }
  };

  const handleUpdateMetadata = async () => {
    if (!canEditMetadata) return;
    setActionLoading('metadata');
    try {
      const payload: Record<string, string> = {};
      editMetadata.forEach(({ key, value }) => {
        if (key.trim()) payload[key.trim()] = value;
      });
      if (updateMetadataLink?.enabled) {
        await executeLinkAction(updateMetadataLink, payload);
      } else {
        await apiPut(`/api/v1/datacenters/${id}/metadata`, payload);
      }
      setEditModal(null);
      await refetch();
    } catch (e) {
      alert(e instanceof Error ? e.message : 'Update failed');
    } finally {
      setActionLoading(null);
    }
  };

  const openEditGeneral = () => {
    setEditName(datacenter?.name ?? '');
    setEditDescription(datacenter?.description ?? '');
    setEditModal('general');
  };

  const openEditSettings = () => {
    const s = datacenter?.settings ?? {};
    setEditVmClasses(Array.isArray(s.vmClasses) ? s.vmClasses.join(', ') : '');
    setEditStorageClasses(Array.isArray(s.storageClasses) ? s.storageClasses.join(', ') : '');
    setEditNetworkDomains(Array.isArray(s.networkDomains) ? s.networkDomains.join(', ') : '');
    setEditProviderSpecificSettings(
      s.providerSpecificSettings && Object.keys(s.providerSpecificSettings).length > 0
        ? JSON.stringify(s.providerSpecificSettings, null, 2)
        : '{}'
    );
    setEditModal('settings');
  };

  const openEditMetadata = () => {
    const meta = datacenter?.metadata ?? {};
    setEditMetadata(
      Object.keys(meta).length
        ? Object.entries(meta).map(([key, value]) => ({ key, value }))
        : [{ key: '', value: '' }]
    );
    setEditModal('metadata');
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-gray-400" />
      </div>
    );
  }

  if (error || !datacenter) {
    return (
      <div className="min-h-screen bg-gray-50 px-3 py-8">
        <Link
          href={backToTenant ? `/system/tenants/${tenantId}?tab=datacenters` : '/system/datacenters'}
          className="inline-flex items-center gap-2 text-gray-600 hover:text-gray-900 mb-6"
        >
          <ArrowLeft size={20} />
          {backToTenant ? 'Back to Tenant' : 'Back to Datacenters'}
        </Link>
        <Card>
          <CardContent className="p-8 text-center">
            <p className="text-red-600">{error ?? 'Datacenter not found'}</p>
            <p className="text-sm text-gray-500 mt-2">The requested datacenter may not exist or you may not have access.</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  const cap = datacenter.capacity ?? {};
  const settings = datacenter.settings ?? {};
  const metadata = datacenter.metadata ?? {};

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-4xl mx-auto px-3 py-8">
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-6"
        >
          <Link
            href={backToTenant ? `/system/tenants/${tenantId}?tab=datacenters` : '/system/datacenters'}
            className="inline-flex items-center gap-2 text-gray-600 hover:text-gray-900 transition-colors"
          >
            <ArrowLeft size={20} />
            <span className="font-medium">{backToTenant ? 'Back to Tenant' : 'Back to Datacenters'}</span>
          </Link>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.05 }}
          className="mb-6"
        >
          <h1 className="text-2xl font-bold text-gray-900">{datacenter.name}</h1>
          {datacenter.description && (
            <p className="text-gray-600 mt-1">{datacenter.description}</p>
          )}
        </motion.div>

        <Tabs
          variant="underline"
          defaultTab="general"
          tabs={[
            {
              id: 'general',
              label: 'General',
              icon: <Server className="h-4 w-4" />,
              content: (
                <Card bordered shadow="md">
                  <CardHeader className="flex flex-row items-center justify-between">
                    <span className="font-semibold text-gray-900">General</span>
                    <Button
                      variant="secondary"
                      size="sm"
                      disabled={!editLink?.enabled}
                      title={!editLink?.enabled ? editLink?.reason : undefined}
                      onClick={openEditGeneral}
                    >
                      <Pencil className="h-4 w-4 mr-1" />
                      Edit
                    </Button>
                  </CardHeader>
                  <CardContent className="pt-2">
                    <div className="space-y-0">
                      <DetailRow label="ID" value={datacenter.id} />
                      <DetailRow label="Name" value={datacenter.name} />
                      <DetailRow label="Description" value={datacenter.description} />
                      <DetailRow label="Created" value={formatDetailDate(datacenter.createdAt)} />
                      <DetailRow label="Updated" value={formatDetailDate(datacenter.updatedAt)} />
                      {datacenter.nodeCluster && (
                        <DetailRow label="Node cluster" value={datacenter.nodeCluster.name ?? datacenter.nodeCluster.id} />
                      )}
                      <DetailRow
                        label="Capacity (CPUs)"
                        value={cap.totalCpus != null ? `${cap.usedCpus ?? 0} / ${cap.totalCpus} (avail. ${cap.availableCpus ?? '—'})` : '—'}
                      />
                      <DetailRow
                        label="Memory (GB)"
                        value={cap.totalMemoryGb != null ? `${cap.usedMemoryGb ?? 0} / ${cap.totalMemoryGb} (avail. ${cap.availableMemoryGb ?? '—'})` : '—'}
                      />
                      <DetailRow
                        label="Storage (GB)"
                        value={cap.totalStorageGb != null ? `${cap.usedStorageGb ?? 0} / ${cap.totalStorageGb} (avail. ${cap.availableStorageGb ?? '—'})` : '—'}
                      />
                    </div>
                  </CardContent>
                </Card>
              ),
            },
            {
              id: 'settings',
              label: 'Settings',
              icon: <Settings className="h-4 w-4" />,
              content: (
                <Card bordered shadow="md">
                  <CardHeader className="flex flex-row items-center justify-between">
                    <span className="font-semibold text-gray-900">Settings</span>
                    <Button
                      variant="secondary"
                      size="sm"
                      disabled={!canEditSettings}
                      title={!canEditSettings ? (replaceSettingsLink?.reason ?? editLink?.reason) : undefined}
                      onClick={openEditSettings}
                    >
                      <Pencil className="h-4 w-4 mr-1" />
                      Edit
                    </Button>
                  </CardHeader>
                  <CardContent className="pt-2">
                    <div className="space-y-0">
                      <DetailRow label="Provider type" value={settings.providerType} />
                      <DetailRow label="VM classes" value={settings.vmClasses?.length ? settings.vmClasses.join(', ') : '—'} />
                      <DetailRow label="Storage classes" value={settings.storageClasses?.length ? settings.storageClasses.join(', ') : '—'} />
                      <DetailRow label="Network domains" value={settings.networkDomains?.length ? settings.networkDomains.join(', ') : '—'} />
                      {settings.providerSpecificSettings && Object.keys(settings.providerSpecificSettings).length > 0 && (
                        <DetailRow
                          label="Provider-specific"
                          value={<pre className="text-xs bg-gray-50 p-2 rounded overflow-auto max-h-32">{JSON.stringify(settings.providerSpecificSettings, null, 2)}</pre>}
                        />
                      )}
                    </div>
                  </CardContent>
                </Card>
              ),
            },
            {
              id: 'metadata',
              label: 'Metadata',
              icon: <FileText className="h-4 w-4" />,
              content: (
                <Card bordered shadow="md">
                  <CardHeader className="flex flex-row items-center justify-between">
                    <span className="font-semibold text-gray-900">Metadata</span>
                    <Button
                      variant="secondary"
                      size="sm"
                      disabled={!canEditMetadata}
                      title={!canEditMetadata ? (updateMetadataLink?.reason ?? editLink?.reason) : undefined}
                      onClick={openEditMetadata}
                    >
                      <Pencil className="h-4 w-4 mr-1" />
                      Edit
                    </Button>
                  </CardHeader>
                  <CardContent className="pt-2">
                    {Object.keys(metadata).length === 0 ? (
                      <p className="text-sm text-gray-500 py-2">No metadata</p>
                    ) : (
                      <div className="space-y-0">
                        {Object.entries(metadata).map(([k, v]) => (
                          <DetailRow key={k} label={k} value={v} />
                        ))}
                      </div>
                    )}
                  </CardContent>
                </Card>
              ),
            },
          ]}
        />
      </div>

      {/* Edit modals */}
      <AnimatePresence>
        {editModal === 'general' && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50"
            onClick={() => setEditModal(null)}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="bg-white rounded-xl shadow-xl w-full max-w-md p-6"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold">Edit datacenter</h3>
                <button onClick={() => setEditModal(null)} className="p-1 rounded hover:bg-gray-100">
                  <X className="h-5 w-5" />
                </button>
              </div>
              <div className="space-y-3">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Name</label>
                  <Input
                    value={editName}
                    onChange={(e) => setEditName(e.target.value)}
                    placeholder="Datacenter name"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                  <Input
                    value={editDescription}
                    onChange={(e) => setEditDescription(e.target.value)}
                    placeholder="Description"
                  />
                </div>
              </div>
              <div className="flex justify-end gap-2 mt-6">
                <Button variant="secondary" onClick={() => setEditModal(null)}>
                  Cancel
                </Button>
                <Button
                  onClick={handleEditGeneral}
                  disabled={actionLoading === 'edit' || !editName.trim()}
                >
                  {actionLoading === 'edit' ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    'Save'
                  )}
                </Button>
              </div>
            </motion.div>
          </motion.div>
        )}

        {editModal === 'settings' && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50"
            onClick={() => setEditModal(null)}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="bg-white rounded-xl shadow-xl w-full max-w-lg max-h-[90vh] overflow-y-auto p-6"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold">Edit datacenter settings</h3>
                <button onClick={() => setEditModal(null)} className="p-1 rounded hover:bg-gray-100">
                  <X className="h-5 w-5" />
                </button>
              </div>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">VM classes (comma-separated)</label>
                  <Input
                    value={editVmClasses}
                    onChange={(e) => setEditVmClasses(e.target.value)}
                    placeholder="e.g. small, medium, large"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Storage classes (comma-separated)</label>
                  <Input
                    value={editStorageClasses}
                    onChange={(e) => setEditStorageClasses(e.target.value)}
                    placeholder="e.g. gold, silver"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Network domains (comma-separated)</label>
                  <Input
                    value={editNetworkDomains}
                    onChange={(e) => setEditNetworkDomains(e.target.value)}
                    placeholder="e.g. private, public"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Provider-specific settings (JSON)</label>
                  <textarea
                    className="w-full min-h-[120px] px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent text-sm font-mono"
                    value={editProviderSpecificSettings}
                    onChange={(e) => setEditProviderSpecificSettings(e.target.value)}
                    placeholder='{"key": "value"}'
                    spellCheck={false}
                  />
                </div>
              </div>
              <div className="flex justify-end gap-2 mt-6">
                <Button variant="secondary" onClick={() => setEditModal(null)}>
                  Cancel
                </Button>
                <Button onClick={handleReplaceSettings} disabled={actionLoading === 'settings'}>
                  {actionLoading === 'settings' ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    'Save'
                  )}
                </Button>
              </div>
            </motion.div>
          </motion.div>
        )}

        {editModal === 'metadata' && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50"
            onClick={() => setEditModal(null)}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="bg-white rounded-xl shadow-xl w-full max-w-lg max-h-[90vh] overflow-y-auto p-6"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold">Update metadata</h3>
                <button onClick={() => setEditModal(null)} className="p-1 rounded hover:bg-gray-100">
                  <X className="h-5 w-5" />
                </button>
              </div>
              <div className="space-y-2 mb-4">
                {editMetadata.map((row, i) => (
                  <div key={i} className="flex gap-2">
                    <Input
                      placeholder="Key"
                      value={row.key}
                      onChange={(e) =>
                        setEditMetadata((prev) =>
                          prev.map((r, j) => (j === i ? { ...r, key: e.target.value } : r))
                        )
                      }
                      className="flex-1"
                    />
                    <Input
                      placeholder="Value"
                      value={row.value}
                      onChange={(e) =>
                        setEditMetadata((prev) =>
                          prev.map((r, j) => (j === i ? { ...r, value: e.target.value } : r))
                        )
                      }
                      className="flex-1"
                    />
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() =>
                        setEditMetadata((prev) => prev.filter((_, j) => j !== i))
                      }
                      disabled={editMetadata.length <= 1}
                    >
                      Remove
                    </Button>
                  </div>
                ))}
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => setEditMetadata((prev) => [...prev, { key: '', value: '' }])}
                >
                  Add row
                </Button>
              </div>
              <div className="flex justify-end gap-2">
                <Button variant="secondary" onClick={() => setEditModal(null)}>
                  Cancel
                </Button>
                <Button onClick={handleUpdateMetadata} disabled={actionLoading === 'metadata'}>
                  {actionLoading === 'metadata' ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    'Update metadata'
                  )}
                </Button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

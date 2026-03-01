'use client';

import { useState, useEffect } from 'react';
import { Card, CardContent } from '@/components/ui/atoms/card';
import { Table, Column } from '@/components/ui/organisms/table';
import { Badge } from '@/components/ui/atoms/badge';
import { Dropdown, DropdownOption } from '@/components/ui/molecules/dropdown';
import { Button } from '@/components/ui/atoms/button';
import { Input } from '@/components/ui/atoms/input';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Plus,
  X,
  Loader2,
  CheckCircle2,
  MapPin,
  Activity,
} from 'lucide-react';
import { RowActionsTrigger } from '@/components/DynamicContextMenu';
import { apiGet, apiPost } from '@/lib/api';
import { executeLinkAction } from '@/lib/api';
import { buildRowActionOptions, normalizeEntityLinks, getNavigationPath } from '@/lib/hateoas';
import type { Link } from '@/types/provider';

export interface Datacenter extends Record<string, any> {
  id: string;
  name: string;
  type: 'libvirt' | 'proxmox' | 'kubernetes';
  status: 'connected' | 'disconnected' | 'syncing' | 'error';
  health: 'healthy' | 'degraded' | 'down';
  region: string;
  location: string;
  providerId: string;
  totalCapacity: number;
  usedCapacity: number;
  totalNodes: number;
  activeNodes: number;
  createdAt: string;
  lastSync: string;
  _links?: Link[];
  [key: string]: any;
}

export default function DatacentersPage() {
  const [datacenters, setDatacenters] = useState<Datacenter[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedDatacenter, setSelectedDatacenter] = useState<string | null>(null);

  // Wizard state
  const [isWizardOpen, setIsWizardOpen] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [datacenterName, setDatacenterName] = useState<string>('');
  const [datacenterDescription, setDatacenterDescription] = useState<string>('');
  const [providerTypeFilter, setProviderTypeFilter] = useState<string>('');
  const [selectedProviderId, setSelectedProviderId] = useState<string>('');
  const [selectedNodeClusterId, setSelectedNodeClusterId] = useState<string>('');
  const [totalCpus, setTotalCpus] = useState<string>('1000');
  const [totalMemoryGb, setTotalMemoryGb] = useState<string>('4096');
  const [totalStorageGb, setTotalStorageGb] = useState<string>('20000');
  const [providers, setProviders] = useState<{ id: string; name: string; type?: string }[]>([]);
  const [clusters, setClusters] = useState<{ id: string; name: string }[]>([]);
  const [loadingProviders, setLoadingProviders] = useState(false);
  const [loadingClusters, setLoadingClusters] = useState(false);

  // Fetch datacenters on mount
  useEffect(() => {
    fetchDatacenters();
  }, []);

  const fetchDatacenters = async () => {
    setIsLoading(true);
    try {
      const data = await apiGet('/api/v1/datacenters');
      // API returns { total, page, perPage, items: [...] }
      const datacentersList = Array.isArray(data) ? data : (data?.items || []);
      
      // Map backend data to frontend Datacenter interface; preserve _links from backend (HateoasLinkEnrichmentAdvice)
      const mappedDatacenters = datacentersList.map((dc: any) => ({
        id: dc.id,
        name: dc.name,
        type: dc.settings?.providerType || 'libvirt',
        status: 'connected', // Backend doesn't provide this yet, default to connected
        health: 'healthy', // Backend doesn't provide this yet, default to healthy
        region: dc.metadata?.region || '',
        location: dc.description || '',
        providerId: dc.id, // Use id as providerId for now
        totalCapacity: dc.capacity?.totalCpus || 0,
        usedCapacity: 0, // Backend doesn't provide this yet
        totalNodes: 0, // Backend doesn't provide this yet
        activeNodes: 0, // Backend doesn't provide this yet
        createdAt: dc.createdAt || '',
        lastSync: dc.updatedAt || '',
        _links: normalizeEntityLinks(dc),
      }));
      
      setDatacenters(mappedDatacenters);
    } catch (error) {
      console.error('Error fetching datacenters:', error);
      setDatacenters([]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleRowAction = async (rel: string, datacenter: Datacenter, link?: Link) => {
    if (!link) return;
    if (rel === 'delete' && !window.confirm(`Are you sure you want to delete datacenter "${datacenter.name}"?`)) {
      return;
    }
    try {
      if (rel === 'self' && link.method === 'GET') {
        const path = getNavigationPath(link);
        window.location.href = path.startsWith('http') ? path : path;
        return;
      }
      await executeLinkAction(link, link.method !== 'GET' && link.method !== 'DELETE' ? {} : undefined);
      if (['delete', 'edit', 'update'].includes(rel)) {
        await fetchDatacenters();
      }
    } catch (error) {
      console.error(`Datacenter action ${rel} failed:`, error);
      alert(`Failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  };

  const getContextMenuOptions = (datacenter: Datacenter): DropdownOption[] =>
    buildRowActionOptions(datacenter, 'datacenter', normalizeEntityLinks(datacenter), handleRowAction);

  const getStatusBadgeVariant = (status: Datacenter['status']) => {
    switch (status) {
      case 'connected':
        return 'success';
      case 'disconnected':
        return 'error';
      case 'syncing':
        return 'info';
      case 'error':
        return 'error';
      default:
        return 'default';
    }
  };

  const getHealthBadgeVariant = (health: Datacenter['health']) => {
    switch (health) {
      case 'healthy':
        return 'success';
      case 'degraded':
        return 'warning';
      case 'down':
        return 'error';
      default:
        return 'default';
    }
  };

  const getTypeLabel = (type: Datacenter['type']) => {
    switch (type) {
      case 'proxmox':
        return 'Proxmox';
      case 'libvirt':
        return 'Libvirt';
      case 'kubernetes':
        return 'Kubernetes';
      default:
        return type;
    }
  };

  const handleOpenWizard = async () => {
    setIsWizardOpen(true);
    setProviders([]);
    setClusters([]);
    setSelectedProviderId('');
    setSelectedNodeClusterId('');
    await fetchProviders();
  };

  const fetchProviders = async () => {
    setLoadingProviders(true);
    try {
      const data = await apiGet('/api/v1/providers?perPage=200');
      const list = Array.isArray(data) ? data : data?.items ?? [];
      setProviders(list.map((p: any) => ({ id: p.id, name: p.name, type: p.type })));
    } catch (e) {
      console.error('Failed to fetch providers', e);
      setProviders([]);
    } finally {
      setLoadingProviders(false);
    }
  };

  const fetchClustersForProvider = async (providerId: string) => {
    if (!providerId) {
      setClusters([]);
      return;
    }
    setLoadingClusters(true);
    try {
      const data = await apiGet(`/api/v1/providers/${providerId}/node-clusters?perPage=200`);
      const list = Array.isArray(data) ? data : data?.items ?? [];
      setClusters(list.map((c: any) => ({ id: c.id, name: c.name })));
    } catch (e) {
      console.error('Failed to fetch node clusters', e);
      setClusters([]);
    } finally {
      setLoadingClusters(false);
    }
  };

  const handleCloseWizard = () => {
    setIsWizardOpen(false);
    setDatacenterName('');
    setDatacenterDescription('');
    setProviderTypeFilter('');
    setSelectedProviderId('');
    setSelectedNodeClusterId('');
    setTotalCpus('1000');
    setTotalMemoryGb('4096');
    setTotalStorageGb('20000');
    setClusters([]);
  };

  const filteredProviders =
    providerTypeFilter === ''
      ? providers
      : providers.filter((p) => String(p.type).toLowerCase() === providerTypeFilter.toLowerCase());

  const handleSaveDatacenter = async () => {
    if (!datacenterName.trim()) {
      alert('Datacenter name is required');
      return;
    }
    if (!selectedNodeClusterId) {
      alert('Please select a node cluster');
      return;
    }
    const cpus = parseInt(totalCpus, 10);
    const mem = parseInt(totalMemoryGb, 10);
    const storage = parseInt(totalStorageGb, 10);
    if (isNaN(cpus) || cpus < 1 || isNaN(mem) || mem < 1 || isNaN(storage) || storage < 1) {
      alert('Capacity must be positive numbers (CPUs, memory GB, storage GB)');
      return;
    }

    setIsSaving(true);
    try {
      const datacenterData = {
        name: datacenterName.trim(),
        description: datacenterDescription.trim() || undefined,
        nodeClusterId: selectedNodeClusterId,
        capacity: {
          totalCpus: cpus,
          totalMemoryGb: mem,
          totalStorageGb: storage,
        },
        settings: {
          vmClasses: ['small', 'medium', 'large'],
          storageClasses: ['gold', 'silver'],
          networkDomains: ['private', 'public'],
        },
      };

      await apiPost('/api/v1/datacenters', datacenterData);
      await fetchDatacenters();
      handleCloseWizard();
    } catch (error) {
      console.error('Error creating datacenter:', error);
      let errorMessage = 'Failed to create datacenter';
      if (error instanceof Error) {
        errorMessage = error.message;
        const match = errorMessage.match(/"message"\s*:\s*"([^"]+)"/);
        if (match) errorMessage = match[1];
      }
      alert(errorMessage);
    } finally {
      setIsSaving(false);
    }
  };

  const columns: Column<Datacenter>[] = [
    {
      key: 'actions',
      header: '',
      cell: (row: Datacenter) => (
        <div className="flex justify-start" onClick={(e) => e.stopPropagation()}>
          <Dropdown
            trigger={<RowActionsTrigger title="Actions" />}
            options={getContextMenuOptions(row)}
            position="right"
            usePortal={true}
          />
        </div>
      ),
      sortable: false,
    },
    {
      key: 'name',
      header: 'Name',
      cell: (row: Datacenter) => (
        <div className="font-medium text-gray-900">{row.name}</div>
      ),
      sortable: true,
    },
    {
      key: 'type',
      header: 'Type',
      cell: (row: Datacenter) => (
        <Badge variant="info">{getTypeLabel(row.type)}</Badge>
      ),
      sortable: true,
    },
    {
      key: 'status',
      header: 'Status',
      cell: (row: Datacenter) => (
        <Badge variant={getStatusBadgeVariant(row.status)}>
          {row.status}
        </Badge>
      ),
      sortable: true,
    },
    {
      key: 'health',
      header: 'Health',
      cell: (row: Datacenter) => (
        <Badge variant={getHealthBadgeVariant(row.health)}>
          {row.health}
        </Badge>
      ),
      sortable: true,
    },
    {
      key: 'region',
      header: 'Region',
      cell: (row: Datacenter) => (
        <div className="flex items-center gap-1">
          <MapPin className="w-4 h-4 text-gray-500" />
          <span className="text-gray-600">{row.region}</span>
        </div>
      ),
      sortable: true,
    },
    {
      key: 'location',
      header: 'Location',
      cell: (row: Datacenter) => (
        <div className="flex items-center gap-1">
          <Activity className="w-4 h-4 text-gray-500" />
          <span className="text-gray-600">{row.location}</span>
        </div>
      ),
      sortable: true,
    },
    {
      key: 'capacity',
      header: 'Capacity',
      cell: (row: Datacenter) => (
        <span className="text-gray-600">{row.usedCapacity} / {row.totalCapacity}</span>
      ),
      sortable: true,
    },
    {
      key: 'nodes',
      header: 'Nodes',
      cell: (row: Datacenter) => (
        <span className="text-gray-600">{row.activeNodes} / {row.totalNodes}</span>
      ),
      sortable: true,
    },
  ];

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-full px-3 py-8">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="mb-8"
        >
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">
                Datacenters
              </h1>
              <p className="text-gray-600 mt-2">
                Manage and monitor your cloud infrastructure datacenters
              </p>
            </div>
            <button
              onClick={handleOpenWizard}
              className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors font-medium"
            >
              <Plus size={18} />
              <span>Add Datacenter</span>
            </button>
          </div>
        </motion.div>

        {/* Datacenters Table */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.1 }}
        >
          <Card>
            <CardContent className="p-0">
              {isLoading ? (
                <div className="flex items-center justify-center py-12">
                  <Loader2 className="h-8 w-8 animate-spin text-gray-400" />
                </div>
              ) : (
                <Table
                  columns={columns}
                  data={datacenters}
                  emptyMessage="No datacenters configured"
                  onRowClick={(row) => handleViewDetails(row)}
                  overflowVisibleColumnKeys={['actions']}
                />
              )}
            </CardContent>
          </Card>
        </motion.div>
      </div>

      {/* Add Datacenter Wizard Modal */}
      <AnimatePresence mode="wait">
        {isWizardOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50"
            onClick={(e) => e.stopPropagation()}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              transition={{ duration: 0.2 }}
              className="bg-white rounded-xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-y-auto"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between">
                <h2 className="text-xl font-semibold text-gray-900">
                  Add Datacenter
                </h2>
                <button
                  onClick={handleCloseWizard}
                  className="p-2 rounded-lg hover:bg-gray-100 transition-colors"
                >
                  <X className="h-5 w-5 text-gray-500" />
                </button>
              </div>
              <div className="p-6 space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Provider type (filter)
                  </label>
                  <select
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
                    value={providerTypeFilter}
                    onChange={(e) => setProviderTypeFilter(e.target.value)}
                  >
                    <option value="">All</option>
                    <option value="libvirt">Libvirt</option>
                    <option value="proxmox">Proxmox</option>
                    <option value="kubernetes">Kubernetes</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Provider *
                  </label>
                  <select
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
                    value={selectedProviderId}
                    onChange={(e) => {
                      const id = e.target.value;
                      setSelectedProviderId(id);
                      setSelectedNodeClusterId('');
                      fetchClustersForProvider(id);
                    }}
                    disabled={loadingProviders}
                  >
                    <option value="">Select provider</option>
                    {filteredProviders.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name} {p.type ? `(${p.type})` : ''}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Node cluster *
                  </label>
                  <select
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
                    value={selectedNodeClusterId}
                    onChange={(e) => setSelectedNodeClusterId(e.target.value)}
                    disabled={!selectedProviderId || loadingClusters}
                  >
                    <option value="">Select node cluster</option>
                    {clusters.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Datacenter name *
                  </label>
                  <Input
                    type="text"
                    placeholder="My Datacenter"
                    value={datacenterName}
                    onChange={(e) => setDatacenterName(e.target.value)}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Description
                  </label>
                  <Input
                    type="text"
                    placeholder="e.g. Virginia, USA"
                    value={datacenterDescription}
                    onChange={(e) => setDatacenterDescription(e.target.value)}
                  />
                </div>
                <div className="grid grid-cols-3 gap-3">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Total CPUs *
                    </label>
                    <Input
                      type="number"
                      min={1}
                      placeholder="1000"
                      value={totalCpus}
                      onChange={(e) => setTotalCpus(e.target.value)}
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Memory (GB) *
                    </label>
                    <Input
                      type="number"
                      min={1}
                      placeholder="4096"
                      value={totalMemoryGb}
                      onChange={(e) => setTotalMemoryGb(e.target.value)}
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Storage (GB) *
                    </label>
                    <Input
                      type="number"
                      min={1}
                      placeholder="20000"
                      value={totalStorageGb}
                      onChange={(e) => setTotalStorageGb(e.target.value)}
                    />
                  </div>
                </div>
              </div>
              <div className="sticky bottom-0 bg-white border-t border-gray-200 px-6 py-4 flex justify-end gap-3">
                <Button
                  variant="secondary"
                  onClick={handleCloseWizard}
                >
                  Cancel
                </Button>
                <Button
                  onClick={handleSaveDatacenter}
                  disabled={isSaving}
                >
                  {isSaving ? (
                    <>
                      <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                      Saving...
                    </>
                  ) : (
                    <>
                      Save & Continue
                      <CheckCircle2 className="h-4 w-4 ml-2" />
                    </>
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

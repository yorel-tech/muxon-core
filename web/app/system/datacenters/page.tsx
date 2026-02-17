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
  MoreHorizontal,
  Eye,
  Pencil,
  RefreshCw,
  Ban,
  Trash2,
  X,
  Loader2,
  CheckCircle2,
  MapPin,
  Activity,
} from 'lucide-react';
import { apiGet, apiPost, apiPut, apiDelete } from '@/lib/api';

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
  const [datacenterType, setDatacenterType] = useState<'proxmox' | 'libvirt' | 'kubernetes'>('libvirt');
  const [datacenterLocation, setDatacenterLocation] = useState<string>('');
  const [datacenterCapacity, setDatacenterCapacity] = useState<string>('');

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
      
      // Map backend data to frontend Datacenter interface
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
      }));
      
      setDatacenters(mappedDatacenters);
    } catch (error) {
      console.error('Error fetching datacenters:', error);
      setDatacenters([]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleViewDetails = (datacenter: Datacenter) => {
    console.log('View details for:', datacenter.id);
    // Navigate to datacenter details page
  };

  const handleEdit = (datacenter: Datacenter) => {
    console.log('Edit datacenter:', datacenter.id);
    // Open edit modal
  };

  const handleSync = async (datacenter: Datacenter) => {
    console.log('Sync datacenter:', datacenter.id);
    try {
      await apiPost(`/api/v1/datacenters/${datacenter.id}/sync`, {});
      await fetchDatacenters();
    } catch (error) {
      console.error('Error syncing datacenter:', error);
      alert('Failed to sync datacenter.');
    }
  };

  const handleDisable = async (datacenter: Datacenter) => {
    console.log('Disable datacenter:', datacenter.id);
    // Note: Backend doesn't have a status field yet, this is a placeholder
    alert('Disable functionality not yet implemented in the backend');
  };

  const handleDelete = async (datacenter: Datacenter) => {
    if (window.confirm(`Are you sure you want to delete datacenter "${datacenter.name}"?`)) {
      try {
        await apiDelete(`/api/v1/datacenters/${datacenter.id}`);
        await fetchDatacenters();
      } catch (error) {
        console.error('Error deleting datacenter:', error);
        alert('Failed to delete datacenter.');
      }
    }
  };

  const getContextMenuOptions = (datacenter: Datacenter): DropdownOption[] => [
    {
      label: 'View Details',
      icon: <Eye size={14} />,
      onClick: () => handleViewDetails(datacenter),
    },
    {
      label: 'Edit',
      icon: <Pencil size={14} />,
      onClick: () => handleEdit(datacenter),
    },
    {
      label: 'Sync',
      icon: <RefreshCw size={14} />,
      onClick: () => handleSync(datacenter),
    },
    {
      label: 'Disable',
      icon: <Ban size={14} />,
      variant: 'warning',
      onClick: () => handleDisable(datacenter),
    },
    {
      label: 'Delete',
      icon: <Trash2 size={14} />,
      variant: 'danger',
      onClick: () => handleDelete(datacenter),
    },
  ];

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

  const handleOpenWizard = () => {
    setIsWizardOpen(true);
  };

  const handleCloseWizard = () => {
    setIsWizardOpen(false);
    // Reset form
    setDatacenterName('');
    setDatacenterType('libvirt');
    setDatacenterLocation('');
    setDatacenterCapacity('');
  };

  const handleSaveDatacenter = async () => {
    if (!datacenterName) {
      alert('Datacenter name is required');
      return;
    }

    setIsSaving(true);
    try {
      const datacenterData = {
        name: datacenterName,
        providerType: datacenterType,
        description: datacenterLocation || undefined,
        settings: {
          providerType: datacenterType,
          defaultCpuOvercommitRatio: 4.0,
          defaultMemoryOvercommitRatio: 1.5,
          vmClasses: ['small', 'medium', 'large'],
          storageClasses: ['gold', 'silver'],
          networkDomains: ['private', 'public'],
        },
        capacity: datacenterCapacity ? {
          totalCpus: parseInt(datacenterCapacity),
          totalMemoryGb: 4096,
          totalStorageGb: 20000,
        } : undefined,
      };

      await apiPost('/api/v1/datacenters', datacenterData);
      await fetchDatacenters();
      handleCloseWizard();
    } catch (error) {
      console.error('Error creating datacenter:', error);
      let errorMessage = 'Failed to create datacenter';
      if (error instanceof Error) {
        errorMessage = error.message;
        // Try to extract the actual message from the error
        if (errorMessage.includes('"message"')) {
          try {
            const match = errorMessage.match(/"message"\s*:\s*"([^"]+)"/);
            if (match) {
              errorMessage = match[1];
            }
          } catch (e) {
            // If parsing fails, use the original message
          }
        }
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
            trigger={
              <button className="p-1.5 rounded hover:bg-gray-100 transition-colors">
                <MoreHorizontal size={16} className="text-gray-600" />
              </button>
            }
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
                    Datacenter Type
                  </label>
                  <select
                    className="w-full px-4 py-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
                    value={datacenterType}
                    onChange={(e) => setDatacenterType(e.target.value as any)}
                  >
                    <option value="proxmox">Proxmox</option>
                    <option value="libvirt">Libvirt</option>
                    <option value="kubernetes">Kubernetes</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Datacenter Name *
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
                    placeholder="Virginia, USA"
                    value={datacenterLocation}
                    onChange={(e) => setDatacenterLocation(e.target.value)}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Capacity (cores)
                  </label>
                  <Input
                    type="number"
                    placeholder="1000"
                    value={datacenterCapacity}
                    onChange={(e) => setDatacenterCapacity(e.target.value)}
                  />
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

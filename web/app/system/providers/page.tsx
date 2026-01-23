'use client';

import { useState } from 'react';
import { motion } from 'framer-motion';
import { Plus, Server, MoreVertical, Search, Filter, RefreshCw, Trash2, Power, Edit, ExternalLink, AlertTriangle, CheckCircle } from 'lucide-react';
import { Button } from '@components/ui/atoms/button';
import { Card, CardContent, CardHeader } from '@components/ui/atoms/card';
import { Badge } from '@components/ui/atoms/badge';
import { Input } from '@components/ui/atoms/input';
import { Table } from '@components/ui/organisms/table';
import { Modal } from '@components/ui/molecules/modal';
import { Dropdown } from '@components/ui/molecules/dropdown';
import { Toast } from '@components/ui/molecules/toast';
import { Sidebar } from '@components/ui/organisms/sidebar';
import { Header } from '@components/ui/organisms/header';

interface Provider {
  id: string;
  name: string;
  type: 'libvirt' | 'proxmox' | 'vsphere' | 'aws' | 'azure' | 'gcp';
  status: 'connected' | 'disconnected' | 'error';
  endpoint: string;
  datacenters: string;
  lastSync: string;
  health: 'healthy' | 'degraded' | 'unhealthy';
}

export default function ProvidersPage() {
  const [showAddModal, setShowAddModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [selectedProvider, setSelectedProvider] = useState<Provider | null>(null);
  const [showToast, setShowToast] = useState(false);
  const [toastMessage, setToastMessage] = useState('');
  const [toastVariant, setToastVariant] = useState<'success' | 'error'>('success');
  const [filterQuery, setFilterQuery] = useState('');
  const [filterStatus, setFilterStatus] = useState<'all' | 'connected' | 'disconnected' | 'error'>('all');

  // Mock data
  const [providers, setProviders] = useState<Provider[]>([
    {
      id: '1',
      name: 'Production Cluster',
      type: 'libvirt',
      status: 'connected',
      endpoint: 'libvirt://192.168.1.100',
      datacenters: '3',
      lastSync: '2024-01-10T10:30:00Z',
      health: 'healthy',
    },
    {
      id: '2',
      name: 'Development Cluster',
      type: 'libvirt',
      status: 'connected',
      endpoint: 'libvirt://192.168.1.101',
      datacenters: '2',
      lastSync: '2024-01-10T09:45:00Z',
      health: 'healthy',
    },
    {
      id: '3',
      name: 'Proxmox VE',
      type: 'proxmox',
      status: 'disconnected',
      endpoint: 'https://proxmox.example.com:8006',
      datacenters: '1',
      lastSync: '2024-01-09T14:20:00Z',
      health: 'unhealthy',
    },
    {
      id: '4',
      name: 'AWS US-East',
      type: 'aws',
      status: 'connected',
      endpoint: 'us-east-1.amazonaws.com',
      datacenters: '5',
      lastSync: '2024-01-10T11:15:00Z',
      health: 'healthy',
    },
  ]);

  const [newProvider, setNewProvider] = useState<Partial<Provider>>({
    name: '',
    type: 'libvirt',
    endpoint: '',
  });

  const filteredProviders = providers.filter((provider) => {
    const matchesQuery = provider.name.toLowerCase().includes(filterQuery.toLowerCase());
    const matchesStatus = filterStatus === 'all' || provider.status === filterStatus;
    return matchesQuery && matchesStatus;
  });

  const getStatusBadge = (status: Provider['status']) => {
    switch (status) {
      case 'connected':
        return <Badge variant="success">Connected</Badge>;
      case 'disconnected':
        return <Badge variant="warning">Disconnected</Badge>;
      case 'error':
        return <Badge variant="error">Error</Badge>;
      default:
        return <Badge variant="default">{status}</Badge>;
    }
  };

  const getHealthBadge = (health: Provider['health']) => {
    switch (health) {
      case 'healthy':
        return <Badge variant="success" size="sm">Healthy</Badge>;
      case 'degraded':
        return <Badge variant="warning" size="sm">Degraded</Badge>;
      case 'unhealthy':
        return <Badge variant="error" size="sm">Unhealthy</Badge>;
      default:
        return <Badge variant="default" size="sm">{health}</Badge>;
    }
  };

  const getProviderIcon = (type: Provider['type']) => {
    switch (type) {
      case 'libvirt':
        return <Server className="h-4 w-4" />;
      case 'proxmox':
        return <Server className="h-4 w-4" />;
      case 'vsphere':
        return <Server className="h-4 w-4" />;
      case 'aws':
        return <ExternalLink className="h-4 w-4" />;
      case 'azure':
        return <ExternalLink className="h-4 w-4" />;
      case 'gcp':
        return <ExternalLink className="h-4 w-4" />;
      default:
        return <Server className="h-4 w-4" />;
    }
  };

  const handleAddProvider = () => {
    setNewProvider({ name: '', type: 'libvirt', endpoint: '' });
    setShowAddModal(true);
  };

  const handleEditProvider = (provider: Provider) => {
    setSelectedProvider(provider);
    setNewProvider({
      name: provider.name,
      type: provider.type,
      endpoint: provider.endpoint,
    });
    setShowEditModal(true);
  };

  const handleDeleteProvider = (provider: Provider) => {
    setSelectedProvider(provider);
    setShowDeleteModal(true);
  };

  const handleSaveProvider = () => {
    if (!newProvider.name || !newProvider.endpoint) {
      setShowToast(true);
      setToastMessage('Please fill in all required fields');
      setToastVariant('error');
      setTimeout(() => setShowToast(false), 3000);
      return;
    }

    if (selectedProvider) {
      // Edit existing provider
      setProviders((prev) =>
        prev.map((p) =>
          p.id === selectedProvider.id
            ? {
                ...p,
                ...(newProvider.name !== undefined ? { name: newProvider.name } : {}),
                ...(newProvider.type !== undefined ? { type: newProvider.type as Provider['type'] } : {}),
                ...(newProvider.endpoint !== undefined ? { endpoint: newProvider.endpoint } : {}),
              }
            : p,
        ),
      );
      setShowToast(true);
      setToastMessage('Provider updated successfully');
      setToastVariant('success');
    } else {
      // Add new provider - construct full Provider object without spreading Partial
      const newId = (providers.length + 1).toString();
      const newProviderObj: Provider = {
        id: newId,
        name: newProvider.name || 'New Provider',
        type: newProvider.type || 'libvirt',
        status: 'disconnected',
        endpoint: newProvider.endpoint || '',
        datacenters: '0',
        lastSync: new Date().toISOString(),
        health: 'unhealthy',
      };
      setProviders((prev) => [...prev, newProviderObj]);
      setShowToast(true);
      setToastMessage('Provider added successfully');
      setToastVariant('success');
    }

    setShowAddModal(false);
    setShowEditModal(false);
    setSelectedProvider(null);
    setTimeout(() => setShowToast(false), 3000);
  };

  const handleConfirmDelete = () => {
    if (selectedProvider) {
      setProviders((prev) => prev.filter((p) => p.id !== selectedProvider.id));
      setShowToast(true);
      setToastMessage('Provider deleted successfully');
      setToastVariant('success');
      setShowDeleteModal(false);
      setSelectedProvider(null);
      setTimeout(() => setShowToast(false), 3000);
    }
  };

  const handleSyncProvider = (provider: Provider) => {
    setProviders((prev) =>
      prev.map((p) =>
        p.id === provider.id
          ? { ...p, lastSync: new Date().toISOString() }
          : p,
      ),
    );
  };

  const handleConnectProvider = (provider: Provider) => {
    setProviders((prev) =>
      prev.map((p) =>
        p.id === provider.id ? { ...p, status: 'connected' } : p,
      ),
    );
  };

  const handleDisconnectProvider = (provider: Provider) => {
    setProviders((prev) =>
      prev.map((p) =>
        p.id === provider.id ? { ...p, status: 'disconnected' } : p,
      ),
    );
  };

  const columns = [
    {
      key: 'name',
      header: 'Provider Name',
      cell: (row: Provider) => (
        <div className="flex items-center gap-3">
          {getProviderIcon(row.type)}
          <span className="font-medium">{row.name}</span>
        </div>
      ),
      sortable: true,
    },
    {
      key: 'type',
      header: 'Type',
      cell: (row: Provider) => (
        <span className="capitalize text-gray-600">{row.type}</span>
      ),
      sortable: true,
    },
    {
      key: 'status',
      header: 'Status',
      cell: (row: Provider) => getStatusBadge(row.status),
      sortable: true,
    },
    {
      key: 'endpoint',
      header: 'Endpoint',
      cell: (row: Provider) => (
        <span className="font-mono text-sm text-gray-600">{row.endpoint}</span>
      ),
      sortable: false,
    },
    {
      key: 'datacenters',
      header: 'Datacenters',
      cell: (row: Provider) => (
        <span className="text-gray-900">{row.datacenters}</span>
      ),
      sortable: true,
    },
    {
      key: 'health',
      header: 'Health',
      cell: (row: Provider) => getHealthBadge(row.health),
      sortable: true,
    },
    {
      key: 'lastSync',
      header: 'Last Sync',
      cell: (row: Provider) => (
        <span className="text-sm text-gray-600">{row.lastSync}</span>
      ),
      sortable: true,
    },
    {
      key: 'actions',
      header: 'Actions',
      cell: (row: Provider) => (
        <div className="flex items-center gap-2">
          <Dropdown
            options={[
              {
                label: 'Sync',
                icon: <RefreshCw size={16} />,
                onClick: () => handleSyncProvider(row),
              },
              {
                label: 'Connect',
                onClick: () => handleConnectProvider(row),
                disabled: row.status === 'connected',
              },
              {
                label: 'Disconnect',
                onClick: () => handleDisconnectProvider(row),
                disabled: row.status !== 'connected',
              },
              {
                label: 'Edit',
                icon: <Edit size={16} />,
                onClick: () => handleEditProvider(row),
              },
              {
                label: 'Delete',
                icon: <Trash2 size={16} />,
                onClick: () => handleDeleteProvider(row),
                variant: 'danger',
              },
            ]}
          >
            <button className="p-1 hover:bg-gray-100 rounded">
                <MoreVertical size={16} />
              </button>
          </Dropdown>
        </div>
      ),
      sortable: false,
    },
  ];

  return (
    <div className="min-h-screen bg-gray-50">
      <Sidebar isOpen={false} onClose={() => {}} userRole="system" />
      <div className="flex flex-col">
        <Header />
        <main className="flex-1 overflow-auto">
          <div className="max-w-7xl mx-auto p-8">
            {/* Page Header */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5 }}
              className="mb-8"
            >
              <div className="flex items-center justify-between">
                <div>
                  <h1 className="text-3xl font-bold text-gray-900">Providers</h1>
                  <p className="text-gray-600 mt-1">
                    Manage cloud provider connections for datacenter and compute resources
                  </p>
                </div>
                <div className="flex gap-3">
                  <Button onClick={handleAddProvider} leftIcon={<Plus size={16} />}>
                    Add Provider
                  </Button>
                  <Button variant="secondary" leftIcon={<RefreshCw size={16} />}>
                    Refresh
                  </Button>
                </div>
              </div>
            </motion.div>

            {/* Stats Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
              <Card>
                <CardContent className="p-6">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm text-gray-600">Total Providers</p>
                      <p className="text-2xl font-bold text-gray-900">{providers.length}</p>
                    </div>
                    <Server className="h-8 w-8 text-gray-400" />
                  </div>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-6">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm text-gray-600">Connected</p>
                      <p className="text-2xl font-bold text-green-600">
                        {providers.filter((p) => p.status === 'connected').length}
                      </p>
                    </div>
                    <CheckCircle className="h-8 w-8 text-green-500" />
                  </div>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-6">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm text-gray-600">Disconnected</p>
                      <p className="text-2xl font-bold text-orange-600">
                        {providers.filter((p) => p.status === 'disconnected').length}
                      </p>
                    </div>
                    <AlertTriangle className="h-8 w-8 text-orange-500" />
                  </div>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-6">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm text-gray-600">Errors</p>
                      <p className="text-2xl font-bold text-red-600">
                        {providers.filter((p) => p.status === 'error').length}
                      </p>
                    </div>
                    <AlertTriangle className="h-8 w-8 text-red-500" />
                  </div>
                </CardContent>
              </Card>
            </div>

            {/* Filters */}
            <Card className="mb-6">
              <CardContent>
                <div className="flex flex-wrap items-center gap-4">
                  <div className="flex-1 min-w-64">
                    <Input
                      placeholder="Search providers..."
                      value={filterQuery}
                      onChange={(e) => setFilterQuery(e.target.value)}
                      variant="search"
                      leftIcon={<Search size={16} />}
                    />
                  </div>
                  <div className="flex items-center gap-2">
                    <Dropdown
                      options={[
                        { label: 'All Status', onClick: () => setFilterStatus('all') },
                        { label: 'Connected', onClick: () => setFilterStatus('connected') },
                        { label: 'Disconnected', onClick: () => setFilterStatus('disconnected') },
                        { label: 'Errors', onClick: () => setFilterStatus('error') },
                      ]}
                    >
                      <Button variant="secondary" leftIcon={<Filter size={16} />}>
                        {filterStatus === 'all' && 'All Status'}
                        {filterStatus === 'connected' && 'Connected'}
                        {filterStatus === 'disconnected' && 'Disconnected'}
                        {filterStatus === 'error' && 'Errors'}
                      </Button>
                    </Dropdown>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Providers Table */}
            <Card>
              <CardHeader>Providers</CardHeader>
              <CardContent>
                <Table
                  columns={columns}
                  data={filteredProviders}
                  onRowClick={(row) => console.log('Clicked provider:', row)}
                  filterQuery={filterQuery}
                  emptyMessage="No providers found"
                />
              </CardContent>
            </Card>
          </div>
        </main>
      </div>

      {/* Add Provider Modal */}
      <Modal
        isOpen={showAddModal}
        onClose={() => {
          setShowAddModal(false);
          setSelectedProvider(null);
        }}
        title="Add New Provider"
        size="md"
      >
        <div className="space-y-4">
          <div>
            <label htmlFor="name">Provider Name</label>
            <Input
              id="name"
              placeholder="e.g., Production Cluster"
              value={newProvider.name}
              onChange={(e) => setNewProvider({ ...newProvider, name: e.target.value })}
            />
          </div>
          <div>
            <label htmlFor="type">Provider Type</label>
            <select
              id="type"
              className="w-full px-3 py-2 border border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={newProvider.type}
              onChange={(e) => setNewProvider({ ...newProvider, type: e.target.value as any })}
            >
              <option value="libvirt">Libvirt</option>
              <option value="proxmox">Proxmox</option>
              <option value="vsphere">VMware vSphere</option>
              <option value="aws">AWS</option>
              <option value="azure">Azure</option>
              <option value="gcp">Google Cloud</option>
            </select>
          </div>
          <div>
            <label htmlFor="endpoint">Endpoint URL</label>
            <Input
              id="endpoint"
              placeholder="e.g., libvirt://192.168.1.100 or https://proxmox.example.com:8006"
              value={newProvider.endpoint}
              onChange={(e) => setNewProvider({ ...newProvider, endpoint: e.target.value })}
            />
          </div>
          <div className="flex justify-end gap-3 pt-4">
            <Button variant="secondary" onClick={() => setShowAddModal(false)}>
              Cancel
            </Button>
            <Button onClick={handleSaveProvider}>Add Provider</Button>
          </div>
        </div>
      </Modal>

      {/* Edit Provider Modal */}
      <Modal
        isOpen={showEditModal}
        onClose={() => {
          setShowEditModal(false);
          setSelectedProvider(null);
        }}
        title="Edit Provider"
        size="md"
      >
        <div className="space-y-4">
          <div>
            <label htmlFor="edit-name">Provider Name</label>
            <Input
              id="edit-name"
              placeholder="e.g., Production Cluster"
              value={newProvider.name}
              onChange={(e) => setNewProvider({ ...newProvider, name: e.target.value })}
            />
          </div>
          <div>
            <label htmlFor="edit-type">Provider Type</label>
            <select
              id="edit-type"
              className="w-full px-3 py-2 border border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={newProvider.type}
              onChange={(e) => setNewProvider({ ...newProvider, type: e.target.value as any })}
            >
              <option value="libvirt">Libvirt</option>
              <option value="proxmox">Proxmox</option>
              <option value="vsphere">VMware vSphere</option>
              <option value="aws">AWS</option>
              <option value="azure">Azure</option>
              <option value="gcp">Google Cloud</option>
            </select>
          </div>
          <div>
            <label htmlFor="edit-endpoint">Endpoint URL</label>
            <Input
              id="edit-endpoint"
              placeholder="e.g., libvirt://192.168.1.100 or https://proxmox.example.com:8006"
              value={newProvider.endpoint}
              onChange={(e) => setNewProvider({ ...newProvider, endpoint: e.target.value })}
            />
          </div>
          <div className="flex justify-end gap-3 pt-4">
            <Button variant="secondary" onClick={() => setShowEditModal(false)}>
              Cancel
            </Button>
            <Button onClick={handleSaveProvider}>Save Changes</Button>
          </div>
        </div>
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={showDeleteModal}
        onClose={() => {
          setShowDeleteModal(false);
          setSelectedProvider(null);
        }}
        title="Delete Provider"
        size="sm"
      >
        <div className="space-y-4">
          <p className="text-gray-700">
            Are you sure you want to delete <strong>{selectedProvider?.name}</strong>? This action cannot be undone.
          </p>
          <div className="flex justify-end gap-3 pt-4">
            <Button variant="secondary" onClick={() => setShowDeleteModal(false)}>
              Cancel
            </Button>
            <Button variant="danger" onClick={handleConfirmDelete}>
              Delete Provider
            </Button>
          </div>
        </div>
      </Modal>

      {/* Toast Notification */}
      {showToast && (
        <Toast
          isOpen={showToast}
          message={toastMessage}
          variant={toastVariant}
          onClose={() => setShowToast(false)}
        />
      )}
    </div>
  );
}

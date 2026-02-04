'use client';

import { useState } from 'react';
import { Card, CardContent, CardHeader } from '@/components/ui/atoms/card';
import { Table, Column } from '@/components/ui/organisms/table';
import { Badge } from '@/components/ui/atoms/badge';
import { Dropdown, DropdownOption } from '@/components/ui/molecules/dropdown';
import { motion } from 'framer-motion';
import {
  Plus,
  MoreHorizontal,
  Eye,
  Pencil,
  RefreshCw,
  Plug,
  Ban,
  Trash2,
} from 'lucide-react';

export interface Provider extends Record<string, any> {
  id: string;
  name: string;
  type: 'proxmox' | 'libvirt' | 'kubernetes';
  status: 'online' | 'offline' | 'degraded';
  nodes: number;
  vms: number;
  region?: string;
  endpoint?: string;
  lastSync?: string;
  capabilities: {
    vmLifecycle: boolean;
    snapshots: boolean;
    backups: boolean;
  };
  [key: string]: any;
}

// Mock providers data
const mockProviders: Provider[] = [
  {
    id: '1',
    name: 'Primary Datacenter',
    type: 'proxmox',
    status: 'online',
    nodes: 5,
    vms: 23,
    region: 'us-east-1',
    endpoint: 'https://proxmox-primary.example.com:8006',
    lastSync: '2 minutes ago',
    capabilities: {
      vmLifecycle: true,
      snapshots: true,
      backups: true,
    },
  },
  {
    id: '2',
    name: 'Secondary Datacenter',
    type: 'libvirt',
    status: 'online',
    nodes: 3,
    vms: 12,
    region: 'us-west-2',
    endpoint: 'qemu+tcp://libvirt-secondary.example.com/system',
    lastSync: '5 minutes ago',
    capabilities: {
      vmLifecycle: true,
      snapshots: true,
      backups: false,
    },
  },
  {
    id: '3',
    name: 'Kubernetes Cluster',
    type: 'kubernetes',
    status: 'online',
    nodes: 3,
    vms: 15,
    region: 'eu-west-1',
    endpoint: 'https://k8s-cluster.example.com:6443',
    lastSync: '10 minutes ago',
    capabilities: {
      vmLifecycle: true,
      snapshots: true,
      backups: true,
    },
  },
];

export default function ProvidersPage() {
  const [providers, setProviders] = useState<Provider[]>(mockProviders);
  const [selectedProvider, setSelectedProvider] = useState<string | null>(null);

  const handleViewDetails = (provider: Provider) => {
    console.log('View details for:', provider.id);
    // Navigate to provider details page
  };

  const handleEdit = (provider: Provider) => {
    console.log('Edit provider:', provider.id);
    // Open edit modal
  };

  const handleSync = (provider: Provider) => {
    console.log('Sync provider:', provider.id);
    // Trigger sync
  };

  const handleTestConnection = (provider: Provider) => {
    console.log('Test connection for:', provider.id);
    // Test connectivity
  };

  const handleDisable = (provider: Provider) => {
    console.log('Disable provider:', provider.id);
    // Disable provider
  };

  const handleDelete = (provider: Provider) => {
    console.log('Delete provider:', provider.id);
    // Delete provider with confirmation
  };

  const getContextMenuOptions = (provider: Provider): DropdownOption[] => [
    {
      label: 'View Details',
      icon: <Eye size={14} />,
      onClick: () => handleViewDetails(provider),
    },
    {
      label: 'Edit',
      icon: <Pencil size={14} />,
      onClick: () => handleEdit(provider),
    },
    {
      label: 'Sync',
      icon: <RefreshCw size={14} />,
      onClick: () => handleSync(provider),
    },
    {
      label: 'Test Connection',
      icon: <Plug size={14} />,
      onClick: () => handleTestConnection(provider),
    },
    {
      label: 'Disable',
      icon: <Ban size={14} />,
      variant: 'warning',
      onClick: () => handleDisable(provider),
    },
    {
      label: 'Delete',
      icon: <Trash2 size={14} />,
      variant: 'danger',
      onClick: () => handleDelete(provider),
    },
  ];

  const getStatusBadgeVariant = (status: Provider['status']) => {
    switch (status) {
      case 'online':
        return 'success';
      case 'offline':
        return 'error';
      case 'degraded':
        return 'warning';
      default:
        return 'default';
    }
  };

  const getTypeLabel = (type: Provider['type']) => {
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

  const columns: Column<Provider>[] = [
    {
      key: 'name',
      header: 'Name',
      cell: (row: Provider) => (
        <div className="font-medium text-gray-900">{row.name}</div>
      ),
      sortable: true,
    },
    {
      key: 'type',
      header: 'Type',
      cell: (row: Provider) => (
        <Badge variant="info">{getTypeLabel(row.type)}</Badge>
      ),
      sortable: true,
    },
    {
      key: 'status',
      header: 'Status',
      cell: (row: Provider) => (
        <Badge variant={getStatusBadgeVariant(row.status)}>
          {row.status}
        </Badge>
      ),
      sortable: true,
    },
    {
      key: 'nodes',
      header: 'Nodes',
      cell: (row: Provider) => (
        <span className="text-gray-600">{row.nodes}</span>
      ),
      sortable: true,
    },
    {
      key: 'vms',
      header: 'VMs',
      cell: (row: Provider) => (
        <span className="text-gray-600">{row.vms}</span>
      ),
      sortable: true,
    },
    {
      key: 'actions',
      header: '',
      cell: (row: Provider) => (
        <div className="flex justify-end">
          <Dropdown
            trigger={
              <button className="p-1.5 rounded hover:bg-gray-100 transition-colors">
                <MoreHorizontal size={16} className="text-gray-600" />
              </button>
            }
            options={getContextMenuOptions(row)}
            position="right"
          />
        </div>
      ),
      sortable: false,
    },
  ];

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
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
                Providers
              </h1>
              <p className="text-gray-600 mt-2">
                Manage your cloud infrastructure providers
              </p>
            </div>
            <button className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors font-medium">
              <Plus size={18} />
              <span>Add Provider</span>
            </button>
          </div>
        </motion.div>

        {/* Providers Table */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.1 }}
        >
          <Card>
            <CardContent className="p-0">
              <Table
                columns={columns}
                data={providers}
                emptyMessage="No providers configured"
                onRowClick={(row) => handleViewDetails(row)}
              />
            </CardContent>
          </Card>
        </motion.div>
      </div>
    </div>
  );
}

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
  Ban,
  Trash2,
} from 'lucide-react';

export interface Tenant extends Record<string, any> {
  id: string;
  name: string;
  slug: string;
  status: 'active' | 'suspended' | 'pending';
  users: number;
  datacenters: number;
  vms: number;
  createdAt: string;
  settings: {
    idpId?: string;
    quotas: {
      vms: number;
      vcpus: number;
      memory: number;
      storage: number;
    };
  };
  [key: string]: any;
}

// Mock tenants data
const mockTenants: Tenant[] = [
  {
    id: '1',
    name: 'Acme Corporation',
    slug: 'acme-corp',
    status: 'active',
    users: 12,
    datacenters: 2,
    vms: 15,
    createdAt: '2024-01-15T10:00:00Z',
    settings: {
      idpId: 'idp-1',
      quotas: {
        vms: 50,
        vcpus: 200,
        memory: 512,
        storage: 2048,
      },
    },
  },
  {
    id: '2',
    name: 'TechStart Inc',
    slug: 'techstart',
    status: 'active',
    users: 5,
    datacenters: 1,
    vms: 8,
    createdAt: '2024-02-01T14:30:00Z',
    settings: {
      idpId: 'idp-1',
      quotas: {
        vms: 25,
        vcpus: 100,
        memory: 256,
        storage: 1024,
      },
    },
  },
  {
    id: '3',
    name: 'Globex Industries',
    slug: 'globex',
    status: 'active',
    users: 23,
    datacenters: 3,
    vms: 42,
    createdAt: '2023-11-20T09:15:00Z',
    settings: {
      idpId: 'idp-2',
      quotas: {
        vms: 100,
        vcpus: 500,
        memory: 1024,
        storage: 4096,
      },
    },
  },
  {
    id: '4',
    name: 'Startup Labs',
    slug: 'startup-labs',
    status: 'pending',
    users: 3,
    datacenters: 0,
    vms: 0,
    createdAt: '2024-01-28T16:45:00Z',
    settings: {
      idpId: 'idp-1',
      quotas: {
        vms: 10,
        vcpus: 40,
        memory: 128,
        storage: 512,
      },
    },
  },
];

export default function TenantsPage() {
  const [tenants, setTenants] = useState<Tenant[]>(mockTenants);

  const handleViewDetails = (tenant: Tenant) => {
    console.log('View details for:', tenant.id);
    // Navigate to tenant details page
  };

  const handleEdit = (tenant: Tenant) => {
    console.log('Edit tenant:', tenant.id);
    // Open edit modal
  };

  const handleSync = (tenant: Tenant) => {
    console.log('Sync tenant:', tenant.id);
    // Trigger sync
  };

  const handleSuspend = (tenant: Tenant) => {
    console.log('Suspend tenant:', tenant.id);
    // Suspend tenant
  };

  const handleDelete = (tenant: Tenant) => {
    console.log('Delete tenant:', tenant.id);
    // Delete tenant with confirmation
  };

  const getContextMenuOptions = (tenant: Tenant): DropdownOption[] => [
    {
      label: 'View Details',
      icon: <Eye size={14} />,
      onClick: () => handleViewDetails(tenant),
    },
    {
      label: 'Edit',
      icon: <Pencil size={14} />,
      onClick: () => handleEdit(tenant),
    },
    {
      label: 'Sync',
      icon: <RefreshCw size={14} />,
      onClick: () => handleSync(tenant),
    },
    {
      label: 'Suspend',
      icon: <Ban size={14} />,
      variant: 'warning',
      onClick: () => handleSuspend(tenant),
    },
    {
      label: 'Delete',
      icon: <Trash2 size={14} />,
      variant: 'danger',
      onClick: () => handleDelete(tenant),
    },
  ];

  const getStatusBadgeVariant = (status: Tenant['status']) => {
    switch (status) {
      case 'active':
        return 'success';
      case 'suspended':
        return 'warning';
      case 'pending':
        return 'info';
      default:
        return 'default';
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  const columns: Column<Tenant>[] = [
    {
      key: 'name',
      header: 'Name',
      cell: (row: Tenant) => (
        <div className="font-medium text-gray-900">{row.name}</div>
      ),
      sortable: true,
    },
    {
      key: 'users',
      header: 'Users',
      cell: (row: Tenant) => (
        <span className="text-gray-600">{row.users}</span>
      ),
      sortable: true,
    },
    {
      key: 'datacenters',
      header: 'DCs',
      cell: (row: Tenant) => (
        <span className="text-gray-600">{row.datacenters}</span>
      ),
      sortable: true,
    },
    {
      key: 'vms',
      header: 'VMs',
      cell: (row: Tenant) => (
        <span className="text-gray-600">{row.vms}</span>
      ),
      sortable: true,
    },
    {
      key: 'status',
      header: 'Status',
      cell: (row: Tenant) => (
        <Badge variant={getStatusBadgeVariant(row.status)}>
          {row.status}
        </Badge>
      ),
      sortable: true,
    },
    {
      key: 'actions',
      header: '',
      cell: (row: Tenant) => (
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
                Tenants
              </h1>
              <p className="text-gray-600 mt-2">
                Manage organizations and their resources
              </p>
            </div>
            <button className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors font-medium">
              <Plus size={18} />
              <span>Add Tenant</span>
            </button>
          </div>
        </motion.div>

        {/* Tenants Table */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.1 }}
        >
          <Card>
            <CardContent className="p-0">
              <Table
                columns={columns}
                data={tenants}
                emptyMessage="No tenants configured"
                onRowClick={(row) => handleViewDetails(row)}
              />
            </CardContent>
          </Card>
        </motion.div>
      </div>
    </div>
  );
}

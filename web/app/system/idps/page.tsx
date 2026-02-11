'use client';

import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader } from '@/components/ui/atoms/card';
import { Table, Column } from '@/components/ui/organisms/table';
import { Badge } from '@/components/ui/atoms/badge';
import { Dropdown, DropdownOption } from '@/components/ui/molecules/dropdown';
import { motion } from 'framer-motion';
import {
  Shield,
  Plus,
  Edit,
  Trash2,
  MoreHorizontal,
  Eye,
  RefreshCw,
  Ban,
  Loader2,
} from 'lucide-react';
import { apiGet, apiPost, apiPut, apiDelete } from '@/lib/api';

interface IdpServer {
  id: string;
  name: string;
  protocol?: string;
  enabled: boolean;
  isSystem: boolean;
}

interface IdpUser {
  sub: string;
  preferredUsername: string;
  preferred_username?: string;
  email: string;
  name?: string;
}

const DEFAULT_IDP_ID = '62083d54-cb8c-521f-9374-65e9f21c8991';

export default function IdpsPage() {
  const [idpServers, setIdpServers] = useState<IdpServer[]>([]);
  const [users, setUsers] = useState<IdpUser[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedIdp, setSelectedIdp] = useState<string>(DEFAULT_IDP_ID);

  // Fetch IDP servers and users on mount
  useEffect(() => {
    fetchIdpData();
  }, []);

  const fetchIdpData = async () => {
    setIsLoading(true);
    try {
      // Fetch IDP settings to get the configured IDP (same logic as page.tsx)
      const idpData = await apiGet('/api/v1/system-settings/idp');
      if (idpData && idpData.name) {
        // Create IDP server entry from the configured IDP
        const idpServer: IdpServer = {
          id: DEFAULT_IDP_ID,
          name: idpData.name,
          protocol: idpData.type || 'OIDC',
          enabled: idpData.enabled || false,
          isSystem: true,
        };
        setIdpServers([idpServer]);
        setSelectedIdp(DEFAULT_IDP_ID);

        // Fetch IDP users for the configured IDP
        await fetchIdpUsers(DEFAULT_IDP_ID);
      } else {
        setIdpServers([]);
        setUsers([]);
      }
    } catch (error) {
      console.error('Error fetching IDP data:', error);
      setIdpServers([]);
      setUsers([]);
    } finally {
      setIsLoading(false);
    }
  };

  const fetchIdpUsers = async (idpId: string) => {
    try {
      // Use the IDP's ID to fetch users (same logic as page.tsx)
      const data = await apiGet(`/api/v1/idps/${idpId}/users`);
      // Handle both array and wrapped response formats
      // API returns OidcUserList with 'items' property
      const usersList = Array.isArray(data) ? data : (data.items || []);
      // Map snake_case properties to camelCase if needed (same logic as page.tsx)
      const mappedUsers = usersList.map((user: any) => ({
        sub: user.sub,
        preferredUsername: user.preferredUsername || user.preferred_username,
        email: user.email,
        name: user.name,
      }));
      setUsers(mappedUsers);
    } catch (error) {
      console.error('Error fetching IDP users:', error);
      setUsers([]);
    }
  };

  const handleViewDetails = (idp: IdpServer) => {
    console.log('View details for:', idp.id);
    // Navigate to IDP details page
  };

  const handleEdit = (idp: IdpServer) => {
    console.log('Edit IDP:', idp.id);
    // Open edit modal
  };

  const handleSync = async (idp: IdpServer) => {
    console.log('Sync IDP:', idp.id);
    try {
      await apiPost(`/api/v1/idps/${idp.id}/sync`, {});
      await fetchIdpData();
    } catch (error) {
      console.error('Error syncing IDP:', error);
      alert('Failed to sync IDP.');
    }
  };

  const handleDisable = async (idp: IdpServer) => {
    console.log('Disable IDP:', idp.id);
    try {
      await apiPut(`/api/v1/idps/${idp.id}`, { enabled: false });
      await fetchIdpData();
    } catch (error) {
      console.error('Error disabling IDP:', error);
      alert('Failed to disable IDP.');
    }
  };

  const handleDelete = async (idp: IdpServer) => {
    if (window.confirm(`Are you sure you want to delete IDP "${idp.name}"?`)) {
      try {
        await apiDelete(`/api/v1/idps/${idp.id}`);
        await fetchIdpData();
      } catch (error) {
        console.error('Error deleting IDP:', error);
        alert('Failed to delete IDP.');
      }
    }
  };

  const getContextMenuOptions = (idp: IdpServer): DropdownOption[] => [
    {
      label: 'View Details',
      icon: <Eye size={14} />,
      onClick: () => handleViewDetails(idp),
    },
    {
      label: 'Edit',
      icon: <Edit size={14} />,
      onClick: () => handleEdit(idp),
    },
    {
      label: 'Sync',
      icon: <RefreshCw size={14} />,
      onClick: () => handleSync(idp),
    },
    {
      label: 'Disable',
      icon: <Ban size={14} />,
      variant: 'warning',
      onClick: () => handleDisable(idp),
    },
    {
      label: 'Delete',
      icon: <Trash2 size={14} />,
      variant: 'danger',
      onClick: () => handleDelete(idp),
    },
  ];

  const idpColumns: Column<IdpServer>[] = [
    {
      key: 'actions',
      header: '',
      cell: (row: IdpServer) => (
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
      cell: (row: IdpServer) => (
        <div className="flex items-center gap-2">
          <span className="font-medium text-gray-900 dark:text-gray-100">{row.name}</span>
        </div>
      ),
      sortable: true,
    },
    {
      key: 'protocol',
      header: 'Protocol',
      cell: (row: IdpServer) => (
        <Badge variant="default">{row.protocol || 'N/A'}</Badge>
      ),
      sortable: true,
    },
    {
      key: 'enabled',
      header: 'Status',
      cell: (row: IdpServer) => (
        <Badge
          variant={row.enabled ? 'success' : 'default'}
        >
          {row.enabled ? 'Enabled' : 'Disabled'}
        </Badge>
      ),
      sortable: true,
    },
  ];

  const userColumns: Column<IdpUser>[] = [
    {
      key: 'preferredUsername',
      header: 'Username',
      cell: (row: IdpUser) => (
        <span className="font-medium text-gray-900 dark:text-gray-100">{row.preferredUsername}</span>
      ),
      sortable: true,
    },
    {
      key: 'name',
      header: 'Name',
      cell: (row: IdpUser) => (
        <span className="text-gray-600 dark:text-gray-400">{row.name || '-'}</span>
      ),
      sortable: true,
    },
    {
      key: 'email',
      header: 'Email',
      cell: (row: IdpUser) => (
        <span className="text-gray-600 dark:text-gray-400">{row.email}</span>
      ),
      sortable: true,
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
                Identity Providers
              </h1>
              <p className="text-gray-600 mt-2">
                Manage your identity providers for user authentication
              </p>
            </div>
            <button className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors font-medium">
              <Plus size={18} />
              <span>Add Provider</span>
            </button>
          </div>
        </motion.div>

        {/* IDP Servers Table */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.1 }}
        >
          <Card>
            <CardHeader>
              <h2 className="text-xl font-semibold text-gray-900">
                Identity Provider Servers
              </h2>
            </CardHeader>
            <CardContent className="p-0">
              {isLoading ? (
                <div className="flex items-center justify-center py-12">
                  <Loader2 className="h-8 w-8 animate-spin text-gray-400" />
                </div>
              ) : (
                <Table
                  columns={idpColumns}
                  data={idpServers}
                  emptyMessage="No IDP servers configured"
                  onRowClick={(row) => handleViewDetails(row)}
                  overflowVisibleColumnKeys={['actions']}
                />
              )}
            </CardContent>
          </Card>
        </motion.div>

        {/* Users Table */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.2 }}
        >
          <Card>
            <CardHeader>
              <h2 className="text-xl font-semibold text-gray-900">
                IDP Users
              </h2>
            </CardHeader>
            <CardContent className="p-0">
              {isLoading ? (
                <div className="flex items-center justify-center py-12">
                  <Loader2 className="h-8 w-8 animate-spin text-gray-400" />
                </div>
              ) : (
                <Table
                  columns={userColumns}
                  data={users}
                  emptyMessage="No users found"
                />
              )}
            </CardContent>
          </Card>
        </motion.div>
      </div>
    </div>
  );
}

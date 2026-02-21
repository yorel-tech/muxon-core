'use client';

import { useState, useEffect } from 'react';
import { Card, CardContent } from '@/components/ui/atoms/card';
import { Table, Column } from '@/components/ui/organisms/table';
import { Badge } from '@/components/ui/atoms/badge';
import { Button } from '@/components/ui/atoms/button';
import { Input } from '@/components/ui/atoms/input';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Plus,
  MoreHorizontal,
  Eye,
  Pencil,
  RefreshCw,
  Plug,
  Ban,
  Trash2,
  X,
  Loader2,
  CheckCircle2,
} from 'lucide-react';
import { DynamicContextMenu } from '@/components/DynamicContextMenu';
import { ActionButton } from '@/components/ActionButton';
import { fetchProvidersWithLinks } from '@/lib/api';
import { usePermissions } from '@/hooks/usePermissions';
import { Provider } from '@/types/provider';

export default function ProvidersPage() {
  const [providers, setProviders] = useState<Provider[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  // Wizard state
  const [isWizardOpen, setIsWizardOpen] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [providerName, setProviderName] = useState<string>('');
  const [providerType, setProviderType] = useState<'proxmox' | 'libvirt' | 'kubernetes'>('libvirt');
  const [providerEndpoint, setProviderEndpoint] = useState<string>('');
  const [providerUsername, setProviderUsername] = useState<string>('');
  const [providerPassword, setProviderPassword] = useState<string>('');
  const [providerDescription, setProviderDescription] = useState<string>('');

  // Fetch providers on mount
  useEffect(() => {
    fetchProviders();
  }, []);

  const fetchProviders = async () => {
    setIsLoading(true);
    try {
      const providersList = await fetchProvidersWithLinks();
      setProviders(providersList);
    } catch (error) {
      console.error('Error fetching providers:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleAction = async (action: string, provider: Provider) => {
    setActionLoading(`${provider.id}-${action}`);
    
    try {
      const link = provider._links.find(l => l.rel === action);
      if (!link) return;

      // Handle navigation actions
      if (link.method === 'GET' && link.href.startsWith('/system/')) {
        window.location.href = link.href;
        return;
      }

      // Handle API actions
      const response = await fetch(link.href, {
        method: link.method,
        headers: { 
          'Content-Type': 'application/json',
        },
        body: link.method !== 'GET' && link.method !== 'DELETE' ? JSON.stringify({}) : undefined
      });
      
      if (!response.ok) {
        throw new Error(`Action failed: ${link.title}`);
      }
      
      // Show success message (for now, using alert - will be replaced with toast)
      alert(`${link.title} completed successfully`);
      
      // Refresh providers list for actions that modify state
      if (['sync', 'delete', 'enable', 'disable'].includes(action)) {
        await fetchProviders();
      }
    } catch (error) {
      console.error(`Action ${action} failed:`, error);
      alert(`Failed to ${action}: ${error instanceof Error ? error.message : 'Unknown error'}`);
    } finally {
      setActionLoading(null);
    }
  };

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

  const handleOpenWizard = () => {
    setIsWizardOpen(true);
  };

  const handleCloseWizard = () => {
    setIsWizardOpen(false);
    // Reset form
    setProviderName('');
    setProviderType('libvirt');
    setProviderEndpoint('');
    setProviderUsername('');
    setProviderPassword('');
    setProviderDescription('');
  };

  const handleSaveProvider = async () => {
    if (!providerName || !providerEndpoint) {
      alert('Provider name and endpoint are required');
      return;
    }

    setIsSaving(true);
    try {
      const providerData = {
        name: providerName,
        type: providerType.toUpperCase(),
        endpoint: providerEndpoint,
        credentials: {
          username: providerUsername,
          password: providerPassword,
        },
        description: providerDescription || undefined,
      };

      const response = await fetch('/api/v1/providers', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(providerData),
      });

      if (!response.ok) {
        throw new Error('Failed to create provider');
      }

      await fetchProviders();
      handleCloseWizard();
    } catch (error) {
      console.error('Error creating provider:', error);
      let errorMessage = 'Failed to create provider';
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

  const columns: Column<Provider>[] = [
    {
      key: 'actions',
      header: '',
      cell: (row: Provider) => (
        <div className="flex justify-start" onClick={(e) => e.stopPropagation()}>
          <DynamicContextMenu 
            entity={row} 
            onAction={handleAction}
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
        <span className="text-gray-600">{row.nodes ?? '-'}</span>
      ),
      sortable: true,
    },
    {
      key: 'vms',
      header: 'VMs',
      cell: (row: Provider) => (
        <span className="text-gray-600">{row.vms ?? '-'}</span>
      ),
      sortable: true,
    },
    {
      key: 'quickActions',
      header: 'Quick Actions',
      cell: (row: Provider) => {
        const { canPerformAction } = usePermissions(row);
        
        return (
          <div className="flex gap-2" onClick={(e) => e.stopPropagation()}>
            {canPerformAction('sync') && (
              <ActionButton
                entity={row}
                action="sync"
                size="sm"
                variant="secondary"
                loading={actionLoading === `${row.id}-sync`}
              >
                <RefreshCw size={14} />
              </ActionButton>
            )}
            {canPerformAction('testConnection') && (
              <ActionButton
                entity={row}
                action="testConnection"
                size="sm"
                variant="secondary"
                loading={actionLoading === `${row.id}-testConnection`}
              >
                <Plug size={14} />
              </ActionButton>
            )}
          </div>
        );
      },
      sortable: false,
    },
  ];

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-full px-3 py-8">
        {/* Header with enhanced actions */}
        <motion.div className="mb-8">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">Providers</h1>
              <p className="text-gray-600 mt-2">
                Manage your cloud infrastructure providers
              </p>
            </div>
            <button
              onClick={handleOpenWizard}
              className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors font-medium"
            >
              <Plus size={18} />
              <span>Add Provider</span>
            </button>
          </div>
        </motion.div>

        {/* Enhanced Table with HATEOAS support */}
        <motion.div>
          <Card>
            <CardContent className="p-0">
              {isLoading ? (
                <div className="flex items-center justify-center py-12">
                  <Loader2 className="h-8 w-8 animate-spin text-gray-400" />
                </div>
              ) : (
                <Table
                  columns={columns}
                  data={providers}
                  emptyMessage="No providers configured"
                  onRowClick={(row) => handleAction('viewDetails', row)}
                  overflowVisibleColumnKeys={['actions', 'quickActions']}
                />
              )}
            </CardContent>
          </Card>
        </motion.div>
      </div>

      {/* Enhanced Wizard Modal */}
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
                  Add Provider
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
                    Provider Type
                  </label>
                  <select
                    className="w-full px-4 py-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
                    value={providerType}
                    onChange={(e) => setProviderType(e.target.value as any)}
                  >
                    <option value="proxmox">Proxmox</option>
                    <option value="libvirt">Libvirt</option>
                    <option value="kubernetes">Kubernetes</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Provider Name *
                  </label>
                  <Input
                    type="text"
                    placeholder="My Libvirt Provider"
                    value={providerName}
                    onChange={(e) => setProviderName(e.target.value)}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Endpoint *
                  </label>
                  <Input
                    type="text"
                    placeholder={
                      providerType === 'proxmox'
                        ? 'https://proxmox.example.com:8006/api2/json'
                        : providerType === 'kubernetes'
                        ? 'https://kubernetes.example.com:6443'
                        : 'ssh://user@host:port or libvirt://system'
                    }
                    value={providerEndpoint}
                    onChange={(e) => setProviderEndpoint(e.target.value)}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Username
                  </label>
                  <Input
                    type="text"
                    placeholder="root"
                    value={providerUsername}
                    onChange={(e) => setProviderUsername(e.target.value)}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Password
                  </label>
                  <Input
                    type="password"
                    placeholder="••••••••••••"
                    value={providerPassword}
                    onChange={(e) => setProviderPassword(e.target.value)}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Description (optional)
                  </label>
                  <Input
                    type="text"
                    placeholder="Main production datacenter"
                    value={providerDescription}
                    onChange={(e) => setProviderDescription(e.target.value)}
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
                  onClick={handleSaveProvider}
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

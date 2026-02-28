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
  Plug,
  Trash2,
  X,
  Loader2,
  CheckCircle2,
  Layers,
  ChevronRight,
  ChevronLeft,
} from 'lucide-react';
import { DynamicContextMenu } from '@/components/DynamicContextMenu';
import { ActionButton } from '@/components/ActionButton';
import { fetchProvidersWithLinks, executeLinkAction, apiGet, apiPost } from '@/lib/api';
import { usePermissions } from '@/hooks/usePermissions';
import { Provider } from '@/types/provider';

export default function ProvidersPage() {
  const [providers, setProviders] = useState<Provider[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  // Wizard state (add + edit)
  const [editingProvider, setEditingProvider] = useState<Provider | null>(null);
  const [isWizardOpen, setIsWizardOpen] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [providerName, setProviderName] = useState<string>('');
  const [providerType, setProviderType] = useState<'proxmox' | 'libvirt' | 'kubernetes'>('libvirt');
  const [providerEndpoint, setProviderEndpoint] = useState<string>('');
  const [providerUsername, setProviderUsername] = useState<string>('');
  const [providerPassword, setProviderPassword] = useState<string>('');
  const [providerDescription, setProviderDescription] = useState<string>('');

  // Libvirt multi-step wizard (only when adding and type is Libvirt)
  const [libvirtStep, setLibvirtStep] = useState<1 | 2 | 3>(1);
  const [libvirtClusterName, setLibvirtClusterName] = useState<string>('default');
  const [libvirtClusterDescription, setLibvirtClusterDescription] = useState<string>('');
  const [libvirtNodes, setLibvirtNodes] = useState<Array<{ id: string; name: string; host: string; user: string; sshKey: string; port: string }>>([]);

  // Capabilities modal
  const [capabilitiesOpen, setCapabilitiesOpen] = useState(false);
  const [capabilitiesData, setCapabilitiesData] = useState<any>(null);
  const [capabilitiesLoading, setCapabilitiesLoading] = useState(false);
  const [capabilitiesProviderName, setCapabilitiesProviderName] = useState<string>('');

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
      const link = provider._links.find((l) => l.rel === action);
      if (!link) return;

      // Edit: open wizard with current provider details (reuse add-provider popup)
      if (action === 'edit') {
        setActionLoading(null);
        const full = await apiGet<Provider>(`/api/v1/providers/${provider.id}`);
        setProviderName(full.name ?? '');
        setProviderType((full.type ?? 'libvirt').toLowerCase() as 'proxmox' | 'libvirt' | 'kubernetes');
        setProviderEndpoint(full.endpoint ?? '');
        setProviderUsername((full as any).credentials?.username ?? '');
        setProviderPassword(''); // never pre-fill password
        setProviderDescription(full.description ?? '');
        setEditingProvider(provider);
        setIsWizardOpen(true);
        return;
      }

      // Capabilities: fetch and show in modal
      if (action === 'capabilities') {
        setActionLoading(null);
        setCapabilitiesProviderName(provider.name);
        setCapabilitiesOpen(true);
        setCapabilitiesData(null);
        setCapabilitiesLoading(true);
        try {
          const data = await executeLinkAction(link);
          setCapabilitiesData(data);
        } catch (e) {
          setCapabilitiesData({ error: String(e) });
        } finally {
          setCapabilitiesLoading(false);
        }
        return;
      }

      // Navigation (e.g. view details)
      if (link.method === 'GET' && link.href.startsWith('/system/')) {
        window.location.href = link.href;
        return;
      }

      // Other API actions
      await executeLinkAction(link, link.method !== 'GET' && link.method !== 'DELETE' ? {} : undefined);
      alert(`${link.title} completed successfully`);
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
    setEditingProvider(null);
    setProviderName('');
    setProviderType('libvirt');
    setProviderEndpoint('');
    setProviderUsername('');
    setProviderPassword('');
    setProviderDescription('');
    setLibvirtStep(1);
    setLibvirtClusterName('default');
    setLibvirtClusterDescription('');
    setLibvirtNodes([]);
    setIsWizardOpen(true);
  };

  const handleCloseWizard = () => {
    setIsWizardOpen(false);
    setEditingProvider(null);
    setProviderName('');
    setProviderType('libvirt');
    setProviderEndpoint('');
    setProviderUsername('');
    setProviderPassword('');
    setProviderDescription('');
    setLibvirtStep(1);
    setLibvirtClusterName('default');
    setLibvirtClusterDescription('');
    setLibvirtNodes([]);
  };

  const isLibvirtWizard = !editingProvider && providerType === 'libvirt';
  const canLibvirtNextStep1 = !!providerName.trim();
  const canLibvirtNextStep2 = !!libvirtClusterName.trim();
  const canLibvirtSave = libvirtNodes.length > 0 && libvirtNodes.every((n) => n.host.trim() && n.user.trim() && n.sshKey.trim());

  const addLibvirtNode = () => {
    setLibvirtNodes((prev) => [...prev, { id: crypto.randomUUID(), name: '', host: '', user: '', sshKey: '', port: '22' }]);
  };
  const removeLibvirtNode = (id: string) => {
    setLibvirtNodes((prev) => prev.filter((n) => n.id !== id));
  };
  const updateLibvirtNode = (id: string, field: string, value: string) => {
    setLibvirtNodes((prev) => prev.map((n) => (n.id === id ? { ...n, [field]: value } : n)));
  };

  const handleSaveProvider = async () => {
    if (editingProvider) {
      if (!providerName || !providerEndpoint) {
        alert('Provider name and endpoint are required');
        return;
      }
      setIsSaving(true);
      try {
        const editLink = editingProvider._links.find((l) => l.rel === 'edit');
        if (!editLink) throw new Error('Edit link not available');
        await executeLinkAction(editLink, {
          name: providerName,
          type: providerType.toUpperCase(),
          endpoint: providerEndpoint,
          credentials: providerUsername || providerPassword ? { username: providerUsername, password: providerPassword } : undefined,
          description: providerDescription || undefined,
        });
        await fetchProviders();
        handleCloseWizard();
      } catch (error) {
        console.error('Error saving provider:', error);
        alert(error instanceof Error ? error.message : 'Failed to update provider');
      } finally {
        setIsSaving(false);
      }
      return;
    }

    if (isLibvirtWizard) {
      if (!canLibvirtSave) {
        alert('Add at least one node with host, user, and SSH key.');
        return;
      }
      const first = libvirtNodes[0];
      const port = first.port?.trim() || '22';
      const endpoint = `ssh://${first.user}@${first.host}:${port}`;
      const credentials = { sshPrivateKey: first.sshKey };
      setIsSaving(true);
      try {
        const providerRes = await apiPost<{ id: string }>('/api/v1/providers', {
          name: providerName,
          type: 'LIBVIRT',
          endpoint,
          credentials,
          description: providerDescription || undefined,
        });
        const providerId = providerRes.id;
        const clusterRes = await apiPost<{ id: string }>(`/api/v1/providers/${providerId}/node-clusters`, {
          name: libvirtClusterName,
          description: libvirtClusterDescription || undefined,
        });
        const clusterId = clusterRes.id;
        for (const node of libvirtNodes) {
          const nodePort = node.port?.trim() || '22';
          await apiPost(`/api/v1/providers/${providerId}/nodes`, {
            name: node.name?.trim() || node.host,
            clusterId,
            credentials: { host: node.host, user: node.user, sshPrivateKey: node.sshKey, port: nodePort },
          });
        }
        await fetchProviders();
        handleCloseWizard();
      } catch (error) {
        console.error('Error creating Libvirt provider:', error);
        alert(error instanceof Error ? error.message : 'Failed to create provider');
      } finally {
        setIsSaving(false);
      }
      return;
    }

    if (!providerName || !providerEndpoint) {
      alert('Provider name and endpoint are required');
      return;
    }
    setIsSaving(true);
    try {
      await apiPost('/api/v1/providers', {
        name: providerName,
        type: providerType.toUpperCase(),
        endpoint: providerEndpoint,
        credentials: providerUsername || providerPassword ? { username: providerUsername, password: providerPassword } : undefined,
        description: providerDescription || undefined,
      });
      await fetchProviders();
      handleCloseWizard();
    } catch (error) {
      console.error('Error creating provider:', error);
      alert(error instanceof Error ? error.message : 'Failed to create provider');
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
            {canPerformAction('testConnection') && (
              <ActionButton
                entity={row}
                action="testConnection"
                size="sm"
                variant="secondary"
                loading={actionLoading === `${row.id}-testConnection`}
                className="inline-flex items-center gap-1"
              >
                <Plug size={14} />
                <span className="sr-only">Test connection</span>
              </ActionButton>
            )}
            {canPerformAction('capabilities') && (
              <button
                type="button"
                onClick={() => handleAction('capabilities', row)}
                disabled={actionLoading !== null}
                className="inline-flex items-center gap-1 px-2 py-1.5 text-sm rounded-md border border-gray-300 bg-white hover:bg-gray-50 text-gray-700"
                title="Show capabilities"
              >
                <Layers size={14} />
                <span className="sr-only">Show capabilities</span>
              </button>
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
                <div className="flex items-center gap-4">
                  <h2 className="text-xl font-semibold text-gray-900">
                    {editingProvider ? 'Edit Provider' : 'Add Provider'}
                  </h2>
                  {isLibvirtWizard && (
                    <div className="flex items-center gap-1 text-sm text-gray-500">
                      <span className={libvirtStep === 1 ? 'font-medium text-primary-600' : ''}>1. Basics</span>
                      <ChevronRight className="h-4 w-4" />
                      <span className={libvirtStep === 2 ? 'font-medium text-primary-600' : ''}>2. Cluster</span>
                      <ChevronRight className="h-4 w-4" />
                      <span className={libvirtStep === 3 ? 'font-medium text-primary-600' : ''}>3. Nodes</span>
                    </div>
                  )}
                </div>
                <button onClick={handleCloseWizard} className="p-2 rounded-lg hover:bg-gray-100 transition-colors">
                  <X className="h-5 w-5 text-gray-500" />
                </button>
              </div>
              <div className="p-6 space-y-4">
                {isLibvirtWizard ? (
                  <>
                    {libvirtStep === 1 && (
                      <>
                        <div>
                          <label className="block text-sm font-medium text-gray-700 mb-2">Provider Type</label>
                          <select
                            className="w-full px-4 py-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
                            value={providerType}
                            onChange={(e) => {
                              const v = e.target.value as 'proxmox' | 'libvirt' | 'kubernetes';
                              setProviderType(v);
                              if (v !== 'libvirt') setLibvirtStep(1);
                            }}
                          >
                            <option value="proxmox">Proxmox</option>
                            <option value="libvirt">Libvirt</option>
                            <option value="kubernetes">Kubernetes</option>
                          </select>
                          <p className="text-xs text-gray-500 mt-1">Choose Libvirt for multi-step setup (cluster + nodes).</p>
                        </div>
                        <div>
                          <label className="block text-sm font-medium text-gray-700 mb-2">Provider Name *</label>
                          <Input
                            placeholder="My Libvirt Provider"
                            value={providerName}
                            onChange={(e) => setProviderName(e.target.value)}
                          />
                        </div>
                        <div>
                          <label className="block text-sm font-medium text-gray-700 mb-2">Description (optional)</label>
                          <Input
                            placeholder="e.g. Main KVM host cluster"
                            value={providerDescription}
                            onChange={(e) => setProviderDescription(e.target.value)}
                          />
                        </div>
                      </>
                    )}
                    {libvirtStep === 2 && (
                      <>
                        <div>
                          <label className="block text-sm font-medium text-gray-700 mb-2">Cluster name *</label>
                          <Input
                            placeholder="default"
                            value={libvirtClusterName}
                            onChange={(e) => setLibvirtClusterName(e.target.value)}
                          />
                          <p className="text-xs text-gray-500 mt-1">Default cluster for this provider. You can change it.</p>
                        </div>
                        <div>
                          <label className="block text-sm font-medium text-gray-700 mb-2">Cluster description (optional)</label>
                          <Input
                            placeholder="Optional description"
                            value={libvirtClusterDescription}
                            onChange={(e) => setLibvirtClusterDescription(e.target.value)}
                          />
                        </div>
                      </>
                    )}
                    {libvirtStep === 3 && (
                      <>
                        <p className="text-sm text-gray-600">Add at least one node. Connection is via SSH (host, user, SSH key).</p>
                        {libvirtNodes.map((node) => (
                          <div key={node.id} className="border border-gray-200 rounded-lg p-4 space-y-3">
                            <div className="flex justify-between items-center">
                              <span className="text-sm font-medium text-gray-700">Node {libvirtNodes.indexOf(node) + 1}</span>
                              <button
                                type="button"
                                onClick={() => removeLibvirtNode(node.id)}
                                className="text-red-600 hover:text-red-800 text-sm"
                              >
                                Remove
                              </button>
                            </div>
                            <div className="grid grid-cols-2 gap-3">
                              <div>
                                <label className="block text-xs font-medium text-gray-500 mb-1">Name (optional)</label>
                                <Input
                                  placeholder="e.g. node-01"
                                  value={node.name}
                                  onChange={(e) => updateLibvirtNode(node.id, 'name', e.target.value)}
                                />
                              </div>
                              <div>
                                <label className="block text-xs font-medium text-gray-500 mb-1">Host *</label>
                                <Input
                                  placeholder="hostname or IP"
                                  value={node.host}
                                  onChange={(e) => updateLibvirtNode(node.id, 'host', e.target.value)}
                                />
                              </div>
                              <div>
                                <label className="block text-xs font-medium text-gray-500 mb-1">User *</label>
                                <Input
                                  placeholder="ssh user"
                                  value={node.user}
                                  onChange={(e) => updateLibvirtNode(node.id, 'user', e.target.value)}
                                />
                              </div>
                              <div>
                                <label className="block text-xs font-medium text-gray-500 mb-1">SSH port</label>
                                <Input
                                  placeholder="22"
                                  value={node.port}
                                  onChange={(e) => updateLibvirtNode(node.id, 'port', e.target.value)}
                                />
                              </div>
                            </div>
                            <div>
                              <label className="block text-xs font-medium text-gray-500 mb-1">SSH private key *</label>
                              <textarea
                                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm font-mono min-h-[80px]"
                                placeholder="Paste private key (PEM)"
                                value={node.sshKey}
                                onChange={(e) => updateLibvirtNode(node.id, 'sshKey', e.target.value)}
                              />
                            </div>
                          </div>
                        ))}
                        <Button type="button" variant="secondary" onClick={addLibvirtNode} className="w-full">
                          <Plus className="h-4 w-4 mr-2" />
                          Add node
                        </Button>
                      </>
                    )}
                  </>
                ) : (
                  <>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">Provider Type</label>
                      <select
                        className={`w-full px-4 py-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent ${editingProvider ? 'bg-gray-100 cursor-not-allowed' : ''}`}
                        value={providerType}
                        onChange={(e) => !editingProvider && setProviderType(e.target.value as 'proxmox' | 'libvirt' | 'kubernetes')}
                        disabled={!!editingProvider}
                      >
                        <option value="proxmox">Proxmox</option>
                        <option value="libvirt">Libvirt</option>
                        <option value="kubernetes">Kubernetes</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">Provider Name *</label>
                      <Input placeholder="My Provider" value={providerName} onChange={(e) => setProviderName(e.target.value)} />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">Endpoint *</label>
                      <Input
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
                      <label className="block text-sm font-medium text-gray-700 mb-2">Username</label>
                      <Input placeholder="root" value={providerUsername} onChange={(e) => setProviderUsername(e.target.value)} />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">Password</label>
                      <Input type="password" placeholder="••••••••" value={providerPassword} onChange={(e) => setProviderPassword(e.target.value)} />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">Description (optional)</label>
                      <Input placeholder="Main production datacenter" value={providerDescription} onChange={(e) => setProviderDescription(e.target.value)} />
                    </div>
                  </>
                )}
              </div>
              <div className="sticky bottom-0 bg-white border-t border-gray-200 px-6 py-4 flex justify-between">
                <div>
                  {isLibvirtWizard && libvirtStep > 1 && (
                    <Button variant="secondary" onClick={() => setLibvirtStep((s) => (s - 1) as 1 | 2 | 3)}>
                      <ChevronLeft className="h-4 w-4 mr-1" />
                      Back
                    </Button>
                )}
                </div>
                <div className="flex gap-3 ml-auto">
                  <Button variant="secondary" onClick={handleCloseWizard}>
                    Cancel
                  </Button>
                  {isLibvirtWizard && libvirtStep < 3 ? (
                    <Button
                      onClick={() => setLibvirtStep((s) => (s + 1) as 1 | 2 | 3)}
                      disabled={libvirtStep === 1 ? !canLibvirtNextStep1 : !canLibvirtNextStep2}
                    >
                      Next
                      <ChevronRight className="h-4 w-4 ml-1" />
                    </Button>
                  ) : (
                    <Button onClick={handleSaveProvider} disabled={isSaving || (isLibvirtWizard && !canLibvirtSave)}>
                      {isSaving ? (
                        <>
                          <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                          Saving...
                        </>
                      ) : (
                        <>
                          {editingProvider ? 'Save' : 'Save'}
                          <CheckCircle2 className="h-4 w-4 ml-2" />
                        </>
                      )}
                    </Button>
                  )}
                </div>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Capabilities modal */}
      <AnimatePresence mode="wait">
        {capabilitiesOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50"
            onClick={() => setCapabilitiesOpen(false)}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="bg-white rounded-xl shadow-2xl w-full max-w-2xl max-h-[85vh] overflow-hidden flex flex-col"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
                <h2 className="text-xl font-semibold text-gray-900">
                  Capabilities – {capabilitiesProviderName}
                </h2>
                <button
                  type="button"
                  onClick={() => setCapabilitiesOpen(false)}
                  className="p-2 rounded-lg hover:bg-gray-100"
                >
                  <X className="h-5 w-5 text-gray-500" />
                </button>
              </div>
              <div className="p-6 overflow-auto flex-1 min-h-0">
                {capabilitiesLoading ? (
                  <div className="flex items-center justify-center py-12">
                    <Loader2 className="h-8 w-8 animate-spin text-gray-400" />
                  </div>
                ) : capabilitiesData ? (
                  <pre className="text-sm text-gray-700 bg-gray-50 p-4 rounded-lg overflow-auto max-h-[60vh]">
                    {typeof capabilitiesData === 'object' && !capabilitiesData.error
                      ? JSON.stringify(capabilitiesData, null, 2)
                      : String(capabilitiesData.error ?? capabilitiesData)}
                  </pre>
                ) : null}
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

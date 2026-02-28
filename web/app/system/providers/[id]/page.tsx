'use client';

import { useState, useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  Activity, 
  Server, 
  Cpu, 
  Database, 
  Settings, 
  FileText,
  Eye,
  Pencil,
  RefreshCw,
  Plug,
  Ban,
  Trash2,
  Plus
} from 'lucide-react';
import { Card, CardContent } from '@/components/ui/atoms/card';
import { Badge } from '@/components/ui/atoms/badge';
import { Button } from '@/components/ui/atoms/button';
import { Tabs } from '@/components/ui/molecules/tabs';
import { BreadcrumbNavigation } from '@/components/BreadcrumbNavigation';
import { DynamicContextMenu } from '@/components/DynamicContextMenu';
import { ActionButton } from '@/components/ActionButton';
import { Provider, NodeCluster, Node, BreadcrumbItem } from '@/types/provider';
import { usePermissions } from '@/hooks/usePermissions';
import { executeLinkAction } from '@/lib/api';
import { getMockClustersWithLinks, getMockNodesWithLinks } from '@/lib/mockData';

interface ProviderDetailsPageProps {
  params: { id: string };
}

export default function ProviderDetailsPage({ params }: ProviderDetailsPageProps) {
  const [provider, setProvider] = useState<Provider | null>(null);
  const [clusters, setClusters] = useState<NodeCluster[]>([]);
  const [nodes, setNodes] = useState<Node[]>([]);
  const [activeTab, setActiveTab] = useState('overview');
  const [isLoading, setIsLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  const router = useRouter();
  const providerId = params.id;

  useEffect(() => {
    fetchProviderDetails();
  }, [providerId]);

  const fetchProviderDetails = async () => {
    setIsLoading(true);
    try {
      // Mock provider data - in real implementation, this would be an API call
      const mockProviders = [
        {
          id: '1',
          name: 'Production Proxmox',
          type: 'proxmox',
          status: 'online',
          nodes: 3,
          vms: 15,
          region: 'us-east',
          endpoint: 'https://proxmox.example.com:8006/api2/json',
          lastSync: '2024-01-15T10:30:00Z',
          description: 'Main production cluster',
          capabilities: {
            vmLifecycle: true,
            snapshots: true,
            backups: true,
          },
          _links: [] // Will be populated by mock data function
        },
        {
          id: '2',
          name: 'Development Libvirt',
          type: 'libvirt',
          status: 'offline',
          nodes: 1,
          vms: 5,
          region: 'us-west',
          endpoint: 'libvirt://system',
          lastSync: '2024-01-14T15:45:00Z',
          description: 'Development environment',
          capabilities: {
            vmLifecycle: true,
            snapshots: false,
            backups: false,
          },
          _links: []
        },
        {
          id: '3',
          name: 'Staging Kubernetes',
          type: 'kubernetes',
          status: 'degraded',
          nodes: 2,
          vms: 8,
          region: 'eu-central',
          endpoint: 'https://k8s-staging.example.com:6443',
          lastSync: '2024-01-15T08:20:00Z',
          description: 'Staging environment for testing',
          capabilities: {
            vmLifecycle: true,
            snapshots: true,
            backups: true,
          },
          _links: []
        }
      ];

      const foundProvider = mockProviders.find(p => p.id === providerId);
      if (foundProvider) {
        setProvider(foundProvider as Provider);
        
        // Load related data based on provider type
        if (foundProvider.type === 'libvirt') {
          setClusters(getMockClustersWithLinks());
          setNodes(getMockNodesWithLinks());
        }
      }
    } catch (error) {
      console.error('Error fetching provider details:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleAction = async (action: string, entity: Provider | NodeCluster | Node) => {
    setActionLoading(`${entity.id}-${action}`);
    try {
      const link = entity._links.find((l) => l.rel === action);
      if (!link) return;

      if (link.method === 'GET' && link.href.startsWith('/system/')) {
        router.push(link.href);
        return;
      }

      await executeLinkAction(link, link.method !== 'GET' && link.method !== 'DELETE' ? {} : undefined);
      alert(`${link.title} completed successfully`);
      if (['sync', 'delete', 'enable', 'disable'].includes(action)) {
        await fetchProviderDetails();
      }
    } catch (error) {
      console.error(`Action ${action} failed:`, error);
      alert(`Failed to ${action}: ${error instanceof Error ? error.message : 'Unknown error'}`);
    } finally {
      setActionLoading(null);
    }
  };

  const tabs = [
    { key: 'overview', label: 'Overview', icon: Activity },
    { key: 'clusters', label: 'Clusters', icon: Server, count: clusters.length },
    { key: 'nodes', label: 'Nodes', icon: Cpu, count: nodes.length },
    { key: 'vms', label: 'VMs', icon: Database, count: provider?.vms || 0 },
    { key: 'settings', label: 'Settings', icon: Settings },
    { key: 'logs', label: 'Logs', icon: FileText }
  ];

  const breadcrumbItems: BreadcrumbItem[] = [
    { label: 'Providers', href: '/system/providers' },
    { label: provider?.name || 'Loading...', active: true }
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

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (!provider) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <h2 className="text-2xl font-bold text-gray-900 mb-4">Provider Not Found</h2>
          <p className="text-gray-600 mb-6">The provider you're looking for doesn't exist or has been deleted.</p>
          <Button onClick={() => router.push('/system/providers')}>
            Back to Providers
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-full px-3 py-8">
        {/* Breadcrumb Navigation */}
        <BreadcrumbNavigation items={breadcrumbItems} />
        
        {/* Provider Header */}
        <motion.div className="mt-6 mb-8">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">{provider.name}</h1>
              <p className="text-gray-600 mt-2">{provider.description}</p>
            </div>
            <DynamicContextMenu entity={provider} onAction={handleAction} />
          </div>
        </motion.div>

        {/* Tab Navigation */}
        <div className="border-b border-gray-200 mb-8">
          <nav className="flex space-x-8">
            {tabs.map(tab => (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                className={`py-2 px-1 border-b-2 font-medium text-sm transition-colors ${
                  activeTab === tab.key
                    ? 'border-primary-500 text-primary-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                <div className="flex items-center space-x-2">
                  <tab.icon size={16} />
                  <span>{tab.label}</span>
                  {tab.count !== undefined && (
                    <Badge variant="secondary">{tab.count}</Badge>
                  )}
                </div>
              </button>
            ))}
          </nav>
        </div>

        {/* Tab Content */}
        <AnimatePresence mode="wait">
          <motion.div
            key={activeTab}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.2 }}
          >
            {activeTab === 'overview' && (
              <Card>
                <CardContent className="p-6">
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    <div>
                      <h3 className="text-lg font-semibold text-gray-900 mb-4">Basic Information</h3>
                      <dl className="space-y-2">
                        <div className="flex justify-between">
                          <dt className="text-sm font-medium text-gray-500">Type</dt>
                          <dd className="text-sm text-gray-900">{getTypeLabel(provider.type)}</dd>
                        </div>
                        <div className="flex justify-between">
                          <dt className="text-sm font-medium text-gray-500">Status</dt>
                          <dd>
                            <Badge variant={getStatusBadgeVariant(provider.status)}>
                              {provider.status}
                            </Badge>
                          </dd>
                        </div>
                        <div className="flex justify-between">
                          <dt className="text-sm font-medium text-gray-500">Region</dt>
                          <dd className="text-sm text-gray-900">{provider.region || '-'}</dd>
                        </div>
                        <div className="flex justify-between">
                          <dt className="text-sm font-medium text-gray-500">Last Sync</dt>
                          <dd className="text-sm text-gray-900">
                            {provider.lastSync ? new Date(provider.lastSync).toLocaleString() : 'Never'}
                          </dd>
                        </div>
                      </dl>
                    </div>
                    
                    <div>
                      <h3 className="text-lg font-semibold text-gray-900 mb-4">Resources</h3>
                      <dl className="space-y-2">
                        <div className="flex justify-between">
                          <dt className="text-sm font-medium text-gray-500">Nodes</dt>
                          <dd className="text-sm text-gray-900">{provider.nodes || 0}</dd>
                        </div>
                        <div className="flex justify-between">
                          <dt className="text-sm font-medium text-gray-500">VMs</dt>
                          <dd className="text-sm text-gray-900">{provider.vms || 0}</dd>
                        </div>
                      </dl>
                    </div>
                    
                    <div>
                      <h3 className="text-lg font-semibold text-gray-900 mb-4">Capabilities</h3>
                      <dl className="space-y-2">
                        <div className="flex justify-between">
                          <dt className="text-sm font-medium text-gray-500">VM Lifecycle</dt>
                          <dd>
                            <Badge variant={provider.capabilities?.vmLifecycle ? 'success' : 'error'}>
                              {provider.capabilities?.vmLifecycle ? 'Enabled' : 'Disabled'}
                            </Badge>
                          </dd>
                        </div>
                        <div className="flex justify-between">
                          <dt className="text-sm font-medium text-gray-500">Snapshots</dt>
                          <dd>
                            <Badge variant={provider.capabilities?.snapshots ? 'success' : 'error'}>
                              {provider.capabilities?.snapshots ? 'Enabled' : 'Disabled'}
                            </Badge>
                          </dd>
                        </div>
                        <div className="flex justify-between">
                          <dt className="text-sm font-medium text-gray-500">Backups</dt>
                          <dd>
                            <Badge variant={provider.capabilities?.backups ? 'success' : 'error'}>
                              {provider.capabilities?.backups ? 'Enabled' : 'Disabled'}
                            </Badge>
                          </dd>
                        </div>
                      </dl>
                    </div>
                    
                    <div>
                      <h3 className="text-lg font-semibold text-gray-900 mb-4">Connection</h3>
                      <dl className="space-y-2">
                        <div>
                          <dt className="text-sm font-medium text-gray-500">Endpoint</dt>
                          <dd className="text-sm text-gray-900 break-all mt-1">{provider.endpoint}</dd>
                        </div>
                      </dl>
                    </div>
                  </div>
                </CardContent>
              </Card>
            )}
            
            {activeTab === 'clusters' && (
              <Card>
                <CardContent className="p-6">
                  <div className="flex items-center justify-between mb-6">
                    <h3 className="text-lg font-semibold text-gray-900">Clusters</h3>
                    <ActionButton
                      entity={provider}
                      action="addCluster"
                      size="sm"
                    >
                      <Plus size={14} />
                      Add Cluster
                    </ActionButton>
                  </div>
                  {clusters.length === 0 ? (
                    <div className="text-center py-12">
                      <Server className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                      <p className="text-gray-600">No clusters configured</p>
                    </div>
                  ) : (
                    <div className="space-y-4">
                      {clusters.map(cluster => (
                        <div key={cluster.id} className="border border-gray-200 rounded-lg p-4">
                          <div className="flex items-center justify-between">
                            <div>
                              <h4 className="font-medium text-gray-900">{cluster.name}</h4>
                              <p className="text-sm text-gray-600">{cluster.description}</p>
                            </div>
                            <DynamicContextMenu entity={cluster} onAction={handleAction} />
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </CardContent>
              </Card>
            )}
            
            {activeTab === 'nodes' && (
              <Card>
                <CardContent className="p-6">
                  <div className="flex items-center justify-between mb-6">
                    <h3 className="text-lg font-semibold text-gray-900">Nodes</h3>
                    <ActionButton
                      entity={provider}
                      action="addNode"
                      size="sm"
                    >
                      <Plus size={14} />
                      Add Node
                    </ActionButton>
                  </div>
                  {nodes.length === 0 ? (
                    <div className="text-center py-12">
                      <Cpu className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                      <p className="text-gray-600">No nodes configured</p>
                    </div>
                  ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                      {nodes.map(node => (
                        <div key={node.id} className="border border-gray-200 rounded-lg p-4">
                          <div className="flex items-center justify-between mb-2">
                            <h4 className="font-medium text-gray-900">{node.name}</h4>
                            <DynamicContextMenu entity={node} onAction={handleAction} />
                          </div>
                          <div className="space-y-2 text-sm">
                            <div className="flex justify-between">
                              <span className="text-gray-500">Status:</span>
                              <Badge variant={node.status === 'online' ? 'success' : node.status === 'offline' ? 'error' : 'warning'}>
                                {node.status}
                              </Badge>
                            </div>
                            <div className="flex justify-between">
                              <span className="text-gray-500">VMs:</span>
                              <span className="text-gray-900">{node.vms || 0}</span>
                            </div>
                            <div className="flex justify-between">
                              <span className="text-gray-500">CPU:</span>
                              <span className="text-gray-900">{node.cpu?.cores || 0} cores</span>
                            </div>
                            <div className="flex justify-between">
                              <span className="text-gray-500">Memory:</span>
                              <span className="text-gray-900">
                                {node.memory ? `${Math.round(node.memory.used / 1024)}GB / ${Math.round(node.memory.total / 1024)}GB` : '-'}
                              </span>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </CardContent>
              </Card>
            )}
            
            {activeTab === 'vms' && (
              <Card>
                <CardContent className="p-6">
                  <div className="text-center py-12">
                    <Database className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                    <p className="text-gray-600">VM management coming soon</p>
                  </div>
                </CardContent>
              </Card>
            )}
            
            {activeTab === 'settings' && (
              <Card>
                <CardContent className="p-6">
                  <div className="text-center py-12">
                    <Settings className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                    <p className="text-gray-600">Provider settings coming soon</p>
                  </div>
                </CardContent>
              </Card>
            )}
            
            {activeTab === 'logs' && (
              <Card>
                <CardContent className="p-6">
                  <div className="text-center py-12">
                    <FileText className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                    <p className="text-gray-600">Provider logs coming soon</p>
                  </div>
                </CardContent>
              </Card>
            )}
          </motion.div>
        </AnimatePresence>
      </div>
    </div>
  );
}

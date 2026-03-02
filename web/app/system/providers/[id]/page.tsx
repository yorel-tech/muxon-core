'use client';

import { useState, useEffect, use } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { motion } from 'framer-motion';
import {
  Activity,
  Server,
  Cpu,
  Database,
  Settings,
  FileText,
  Plus,
  ArrowLeft,
  Loader2,
} from 'lucide-react';
import { Card, CardContent, CardHeader } from '@/components/ui/atoms/card';
import { Badge } from '@/components/ui/atoms/badge';
import { Button } from '@/components/ui/atoms/button';
import { DynamicContextMenu } from '@/components/DynamicContextMenu';
import { ActionButton } from '@/components/ActionButton';
import { DetailRow, formatDetailDate } from '@/components/entity-detail/DetailRow';
import { Tabs } from '@/components/ui/molecules/tabs';
import { Provider, NodeCluster, Node } from '@/types/provider';
import { executeLinkAction } from '@/lib/api';
import { getMockClustersWithLinks, getMockNodesWithLinks } from '@/lib/mockData';

interface ProviderDetailsPageProps {
  params: Promise<{ id: string }>;
}

export default function ProviderDetailsPage({ params }: ProviderDetailsPageProps) {
  const { id: providerId } = use(params);

  const [provider, setProvider] = useState<Provider | null>(null);
  const [clusters, setClusters] = useState<NodeCluster[]>([]);
  const [nodes, setNodes] = useState<Node[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  const router = useRouter();

  useEffect(() => {
    let cancelled = false;
    async function fetchProviderDetails() {
      setIsLoading(true);
      try {
        const mockProviders: Partial<Provider>[] = [
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
            capabilities: { vmLifecycle: true, snapshots: true, backups: true },
            _links: [],
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
            capabilities: { vmLifecycle: true, snapshots: false, backups: false },
            _links: [],
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
            capabilities: { vmLifecycle: true, snapshots: true, backups: true },
            _links: [],
          },
        ];

        const found = mockProviders.find((p) => p.id === providerId);
        if (!cancelled && found) {
          setProvider(found as Provider);
          if (found.type === 'libvirt') {
            setClusters(getMockClustersWithLinks());
            setNodes(getMockNodesWithLinks());
          } else {
            setClusters([]);
            setNodes([]);
          }
        }
      } catch (error) {
        console.error('Error fetching provider details:', error);
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }
    fetchProviderDetails();
    return () => {
      cancelled = true;
    };
  }, [providerId]);

  const handleAction = async (action: string, entity: Provider | NodeCluster | Node) => {
    setActionLoading(`${entity.id}-${action}`);
    try {
      const link = entity._links?.find((l) => l.rel === action);
      if (!link) return;

      if (link.method === 'GET' && link.href.startsWith('/system/')) {
        router.push(link.href);
        return;
      }

      await executeLinkAction(link, link.method !== 'GET' && link.method !== 'DELETE' ? {} : undefined);
      alert(`${link.title || action} completed successfully`);
      if (['sync', 'delete', 'enable', 'disable'].includes(action)) {
        const mockProviders = [
          { id: providerId, name: provider?.name, type: provider?.type, status: provider?.status, nodes: provider?.nodes, vms: provider?.vms, region: provider?.region, endpoint: provider?.endpoint, lastSync: provider?.lastSync, description: provider?.description, capabilities: provider?.capabilities, _links: provider?._links ?? [] },
        ];
        const found = mockProviders.find((p) => p.id === providerId);
        if (found) setProvider(found as Provider);
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
        return type ?? '—';
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-gray-400" />
      </div>
    );
  }

  if (!provider) {
    return (
      <div className="min-h-screen bg-gray-50 px-3 py-8">
        <Link
          href="/system/providers"
          className="inline-flex items-center gap-2 text-gray-600 hover:text-gray-900 mb-6"
        >
          <ArrowLeft size={20} />
          Back to Providers
        </Link>
        <Card>
          <CardContent className="p-8 text-center">
            <p className="text-gray-900 font-medium">Provider not found</p>
            <p className="text-sm text-gray-500 mt-2">
              The provider you're looking for doesn't exist or has been deleted.
            </p>
            <Button className="mt-4" onClick={() => router.push('/system/providers')}>
              Back to Providers
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-4xl mx-auto px-3 py-8">
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-6"
        >
          <Link
            href="/system/providers"
            className="inline-flex items-center gap-2 text-gray-600 hover:text-gray-900 transition-colors"
          >
            <ArrowLeft size={20} />
            <span className="font-medium">Back to Providers</span>
          </Link>
        </motion.div>

        {/* Title: no card, same as datacenter/tenant */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.05 }}
          className="mb-6"
        >
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">{provider.name}</h1>
              {provider.description && (
                <p className="text-gray-600 mt-1">{provider.description}</p>
              )}
            </div>
            <DynamicContextMenu entity={provider} onAction={handleAction} />
          </div>
        </motion.div>

        <Tabs
          variant="underline"
          defaultTab="overview"
          tabs={[
            {
              id: 'overview',
              label: 'Overview',
              icon: <Activity className="h-4 w-4" />,
              content: (
                <Card bordered shadow="md">
                  <CardHeader><span className="font-semibold text-gray-900">Overview</span></CardHeader>
                  <CardContent className="pt-2">
                    <div className="space-y-0">
                      <DetailRow label="ID" value={provider.id} />
                      <DetailRow label="Type" value={getTypeLabel(provider.type)} />
                      <DetailRow label="Status" value={provider.status ? <Badge variant={getStatusBadgeVariant(provider.status)}>{provider.status}</Badge> : '—'} />
                      <DetailRow label="Region" value={provider.region ?? '—'} />
                      <DetailRow label="Last sync" value={formatDetailDate(provider.lastSync)} />
                      <DetailRow label="Nodes" value={provider.nodes ?? 0} />
                      <DetailRow label="VMs" value={provider.vms ?? 0} />
                      <DetailRow label="VM lifecycle" value={provider.capabilities?.vmLifecycle != null ? <Badge variant={provider.capabilities.vmLifecycle ? 'success' : 'error'}>{provider.capabilities.vmLifecycle ? 'Enabled' : 'Disabled'}</Badge> : '—'} />
                      <DetailRow label="Snapshots" value={provider.capabilities?.snapshots != null ? <Badge variant={provider.capabilities.snapshots ? 'success' : 'error'}>{provider.capabilities.snapshots ? 'Enabled' : 'Disabled'}</Badge> : '—'} />
                      <DetailRow label="Backups" value={provider.capabilities?.backups != null ? <Badge variant={provider.capabilities.backups ? 'success' : 'error'}>{provider.capabilities.backups ? 'Enabled' : 'Disabled'}</Badge> : '—'} />
                      <DetailRow label="Endpoint" value={provider.endpoint ?? '—'} />
                    </div>
                  </CardContent>
                </Card>
              ),
            },
            {
              id: 'clusters',
              label: 'Clusters',
              icon: <Server className="h-4 w-4" />,
              badge: clusters.length,
              content: (
                <Card bordered shadow="md">
                  <CardHeader className="flex flex-row items-center justify-between">
                    <span className="font-semibold text-gray-900">Clusters</span>
                    <ActionButton entity={provider} action="addCluster" size="sm"><Plus className="h-4 w-4 mr-1" /> Add Cluster</ActionButton>
                  </CardHeader>
                  <CardContent className="pt-2">
                    {clusters.length === 0 ? (
                      <div className="py-8 text-center text-gray-500"><Server className="h-10 w-10 mx-auto mb-2 text-gray-300" /><p className="text-sm">No clusters configured</p></div>
                    ) : (
                      <div className="space-y-4">
                        {clusters.map((cluster) => (
                          <div key={cluster.id} className="border border-gray-200 rounded-lg p-4">
                            <div className="flex items-center justify-between">
                              <div><h4 className="font-medium text-gray-900">{cluster.name}</h4>{cluster.description && <p className="text-sm text-gray-600">{cluster.description}</p>}</div>
                              <DynamicContextMenu entity={cluster} onAction={handleAction} />
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </CardContent>
                </Card>
              ),
            },
            {
              id: 'nodes',
              label: 'Nodes',
              icon: <Cpu className="h-4 w-4" />,
              badge: nodes.length,
              content: (
                <Card bordered shadow="md">
                  <CardHeader className="flex flex-row items-center justify-between">
                    <span className="font-semibold text-gray-900">Nodes</span>
                    <ActionButton entity={provider} action="addNode" size="sm"><Plus className="h-4 w-4 mr-1" /> Add Node</ActionButton>
                  </CardHeader>
                  <CardContent className="pt-2">
                    {nodes.length === 0 ? (
                      <div className="py-8 text-center text-gray-500"><Cpu className="h-10 w-10 mx-auto mb-2 text-gray-300" /><p className="text-sm">No nodes configured</p></div>
                    ) : (
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        {nodes.map((node) => (
                          <div key={node.id} className="border border-gray-200 rounded-lg p-4">
                            <div className="flex items-center justify-between mb-2"><h4 className="font-medium text-gray-900">{node.name}</h4><DynamicContextMenu entity={node} onAction={handleAction} /></div>
                            <div className="space-y-0 text-sm">
                              <DetailRow label="Status" value={node.status ? <Badge variant={node.status === 'online' ? 'success' : node.status === 'offline' ? 'error' : 'warning'}>{node.status}</Badge> : '—'} />
                              <DetailRow label="VMs" value={node.vms ?? 0} />
                              <DetailRow label="CPU" value={node.cpu?.cores ? `${node.cpu.cores} cores` : '—'} />
                              <DetailRow label="Memory" value={node.memory ? `${Math.round(node.memory.used / 1024)} / ${Math.round(node.memory.total / 1024)} GB` : '—'} />
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </CardContent>
                </Card>
              ),
            },
            {
              id: 'vms',
              label: 'VMs',
              icon: <Database className="h-4 w-4" />,
              content: (
                <Card bordered shadow="md">
                  <CardHeader><span className="font-semibold text-gray-900">VMs</span></CardHeader>
                  <CardContent className="pt-2">
                    <div className="py-8 text-center text-gray-500"><Database className="h-10 w-10 mx-auto mb-2 text-gray-300" /><p className="text-sm">VM management coming soon</p></div>
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
                  <CardHeader><span className="font-semibold text-gray-900">Settings</span></CardHeader>
                  <CardContent className="pt-2">
                    <div className="py-8 text-center text-gray-500"><Settings className="h-10 w-10 mx-auto mb-2 text-gray-300" /><p className="text-sm">Provider settings coming soon</p></div>
                  </CardContent>
                </Card>
              ),
            },
            {
              id: 'logs',
              label: 'Logs',
              icon: <FileText className="h-4 w-4" />,
              content: (
                <Card bordered shadow="md">
                  <CardHeader><span className="font-semibold text-gray-900">Logs</span></CardHeader>
                  <CardContent className="pt-2">
                    <div className="py-8 text-center text-gray-500"><FileText className="h-10 w-10 mx-auto mb-2 text-gray-300" /><p className="text-sm">Provider logs coming soon</p></div>
                  </CardContent>
                </Card>
              ),
            },
          ]}
        />
      </div>
    </div>
  );
}

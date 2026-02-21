import { Provider, NodeCluster, Node, Link } from '@/types/provider';

/**
 * Generate default HATEOAS links for a provider
 */
export const generateDefaultLinks = (provider: Partial<Provider>): Link[] => {
  const baseLinks: Link[] = [
    {
      rel: 'self',
      href: `/api/v1/providers/${provider.id}`,
      method: 'GET',
      title: 'View Details',
      enabled: true
    },
    {
      rel: 'edit',
      href: `/api/v1/providers/${provider.id}`,
      method: 'PUT',
      title: 'Edit',
      enabled: true,
      reason: 'Requires provider:update permission'
    },
    {
      rel: 'sync',
      href: `/api/v1/providers/${provider.id}/sync`,
      method: 'POST',
      title: 'Sync',
      enabled: provider.status !== 'offline',
      reason: provider.status === 'offline' ? 'Provider is offline' : undefined
    },
    {
      rel: 'testConnection',
      href: `/api/v1/providers/${provider.id}/test-connection`,
      method: 'POST',
      title: 'Test Connection',
      enabled: true
    },
    {
      rel: 'overview',
      href: `/system/providers/${provider.id}`,
      method: 'GET',
      title: 'Overview',
      enabled: true
    },
    {
      rel: 'settings',
      href: `/system/providers/${provider.id}/settings`,
      method: 'GET',
      title: 'Settings',
      enabled: true,
      reason: 'Requires provider:admin permission'
    },
    {
      rel: 'logs',
      href: `/system/providers/${provider.id}/logs`,
      method: 'GET',
      title: 'Logs',
      enabled: true
    }
  ];

  // Add provider-specific actions
  if (provider.type === 'libvirt') {
    baseLinks.push(
      {
        rel: 'addCluster',
        href: `/api/v1/providers/${provider.id}/node-clusters`,
        method: 'POST',
        title: 'Add Cluster',
        enabled: true,
        reason: 'Requires cluster:create permission'
      },
      {
        rel: 'addNode',
        href: `/api/v1/providers/${provider.id}/nodes`,
        method: 'POST',
        title: 'Add Node',
        enabled: true,
        reason: 'Requires node:create permission'
      }
    );
  }

  if (provider.type === 'proxmox') {
    baseLinks.push(
      {
        rel: 'addNode',
        href: `/api/v1/providers/${provider.id}/nodes`,
        method: 'POST',
        title: 'Add Node',
        enabled: true,
        reason: 'Requires node:create permission'
      }
    );
  }

  // Add delete action with conditions
  baseLinks.push({
    rel: 'delete',
    href: `/api/v1/providers/${provider.id}`,
    method: 'DELETE',
    title: 'Delete',
    enabled: (provider.nodes || 0) === 0 && (provider.vms || 0) === 0,
    reason: ((provider.nodes || 0) > 0 || (provider.vms || 0) > 0) 
      ? 'Cannot delete provider with active resources' 
      : 'Requires provider:delete permission'
  });

  return baseLinks;
};

/**
 * Generate default links for node clusters
 */
export const generateClusterLinks = (cluster: Partial<NodeCluster>): Link[] => [
  {
    rel: 'self',
    href: `/api/v1/providers/${cluster.providerId}/clusters/${cluster.id}`,
    method: 'GET',
    title: 'View Details',
    enabled: true
  },
  {
    rel: 'edit',
    href: `/api/v1/providers/${cluster.providerId}/clusters/${cluster.id}`,
    method: 'PUT',
    title: 'Edit',
    enabled: true,
    reason: 'Requires cluster:update permission'
  },
  {
    rel: 'addNode',
    href: `/api/v1/providers/${cluster.providerId}/clusters/${cluster.id}/nodes`,
    method: 'POST',
    title: 'Add Node',
    enabled: true,
    reason: 'Requires node:create permission'
  },
  {
    rel: 'delete',
    href: `/api/v1/providers/${cluster.providerId}/clusters/${cluster.id}`,
    method: 'DELETE',
    title: 'Delete',
    enabled: (cluster.nodes || 0) === 0,
    reason: (cluster.nodes || 0) > 0 
      ? 'Cannot delete cluster with active nodes' 
      : 'Requires cluster:delete permission'
  }
];

/**
 * Generate default links for nodes
 */
export const generateNodeLinks = (node: Partial<Node>): Link[] => [
  {
    rel: 'self',
    href: `/api/v1/providers/${node.providerId}/nodes/${node.id}`,
    method: 'GET',
    title: 'View Details',
    enabled: true
  },
  {
    rel: 'edit',
    href: `/api/v1/providers/${node.providerId}/nodes/${node.id}`,
    method: 'PUT',
    title: 'Edit',
    enabled: true,
    reason: 'Requires node:update permission'
  },
  {
    rel: 'maintenance',
    href: `/api/v1/providers/${node.providerId}/nodes/${node.id}/maintenance`,
    method: 'POST',
    title: 'Enter Maintenance',
    enabled: node.status !== 'maintenance',
    reason: node.status === 'maintenance' ? 'Node already in maintenance' : undefined
  },
  {
    rel: 'delete',
    href: `/api/v1/providers/${node.providerId}/nodes/${node.id}`,
    method: 'DELETE',
    title: 'Delete',
    enabled: (node.vms || 0) === 0,
    reason: (node.vms || 0) > 0 
      ? 'Cannot delete node with active VMs' 
      : 'Requires node:delete permission'
  }
];

/**
 * Mock providers with HATEOAS links
 */
export const getMockProvidersWithLinks = (): Provider[] => [
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
    _links: generateDefaultLinks({
      id: '1',
      name: 'Production Proxmox',
      type: 'proxmox',
      status: 'online',
      nodes: 3,
      vms: 15
    })
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
    _links: generateDefaultLinks({
      id: '2',
      name: 'Development Libvirt',
      type: 'libvirt',
      status: 'offline',
      nodes: 1,
      vms: 5
    })
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
    _links: generateDefaultLinks({
      id: '3',
      name: 'Staging Kubernetes',
      type: 'kubernetes',
      status: 'degraded',
      nodes: 2,
      vms: 8
    })
  },
  {
    id: '4',
    name: 'Empty Test Provider',
    type: 'libvirt',
    status: 'online',
    nodes: 0,
    vms: 0,
    region: 'us-central',
    endpoint: 'libvirt://test',
    lastSync: '2024-01-15T12:00:00Z',
    description: 'Empty provider for testing deletion',
    capabilities: {
      vmLifecycle: true,
      snapshots: true,
      backups: false,
    },
    _links: generateDefaultLinks({
      id: '4',
      name: 'Empty Test Provider',
      type: 'libvirt',
      status: 'online',
      nodes: 0,
      vms: 0
    })
  }
];

/**
 * Mock node clusters with HATEOAS links
 */
export const getMockClustersWithLinks = (): NodeCluster[] => [
  {
    id: 'cluster-1',
    name: 'Production Cluster',
    providerId: '2',
    status: 'online',
    nodes: 3,
    vms: 12,
    description: 'Main production cluster',
    _links: generateClusterLinks({
      id: 'cluster-1',
      name: 'Production Cluster',
      providerId: '2',
      status: 'online',
      nodes: 3,
      vms: 12
    })
  },
  {
    id: 'cluster-2',
    name: 'Development Cluster',
    providerId: '2',
    status: 'offline',
    nodes: 1,
    vms: 2,
    description: 'Development cluster',
    _links: generateClusterLinks({
      id: 'cluster-2',
      name: 'Development Cluster',
      providerId: '2',
      status: 'offline',
      nodes: 1,
      vms: 2
    })
  }
];

/**
 * Mock nodes with HATEOAS links
 */
export const getMockNodesWithLinks = (): Node[] => [
  {
    id: 'node-1',
    name: 'prod-node-01',
    clusterId: 'cluster-1',
    providerId: '2',
    status: 'online',
    cpu: { cores: 8, usage: 65 },
    memory: { total: 32768, used: 21299, available: 11469 },
    storage: { total: 1000000, used: 750000, available: 250000 },
    vms: 5,
    description: 'Production node 1',
    _links: generateNodeLinks({
      id: 'node-1',
      name: 'prod-node-01',
      clusterId: 'cluster-1',
      providerId: '2',
      status: 'online',
      vms: 5
    })
  },
  {
    id: 'node-2',
    name: 'prod-node-02',
    clusterId: 'cluster-1',
    providerId: '2',
    status: 'maintenance',
    cpu: { cores: 8, usage: 0 },
    memory: { total: 32768, used: 8192, available: 24576 },
    storage: { total: 1000000, used: 400000, available: 600000 },
    vms: 0,
    description: 'Production node 2 (maintenance)',
    _links: generateNodeLinks({
      id: 'node-2',
      name: 'prod-node-02',
      clusterId: 'cluster-1',
      providerId: '2',
      status: 'maintenance',
      vms: 0
    })
  }
];
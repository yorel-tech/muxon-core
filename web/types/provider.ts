// Enhanced Provider interface with HATEOAS support
export interface Link {
  rel: string;
  href: string;
  method: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  title: string;
  enabled: boolean;
  reason?: string;
}

export interface Provider extends Record<string, any> {
  id: string;
  name: string;
  type: 'proxmox' | 'libvirt';
  status: 'online' | 'offline' | 'degraded';
  nodes?: number;
  vms?: number;
  region?: string;
  endpoint?: string;
  lastSync?: string;
  description?: string;
  capabilities?: {
    vmLifecycle: boolean;
    snapshots: boolean;
    backups: boolean;
  };
  _links: Link[]; // HATEOAS links array
}

// Extended interfaces for hierarchical navigation
export interface NodeCluster extends Record<string, any> {
  id: string;
  name: string;
  providerId: string;
  status: 'online' | 'offline' | 'degraded';
  nodes?: number;
  vms?: number;
  description?: string;
  _links: Link[];
}

export interface Node extends Record<string, any> {
  id: string;
  name: string;
  clusterId?: string;
  providerId: string;
  status: 'online' | 'offline' | 'maintenance';
  cpu?: {
    cores: number;
    usage: number;
  };
  memory?: {
    total: number;
    used: number;
    available: number;
  };
  storage?: {
    total: number;
    used: number;
    available: number;
  };
  vms?: number;
  description?: string;
  _links: Link[];
}

// Breadcrumb navigation interface
export interface BreadcrumbItem {
  label: string;
  href?: string;
  active?: boolean;
  disabled?: boolean;
}
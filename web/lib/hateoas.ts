import { Link } from '@/types/provider';
import { 
  Eye, 
  Pencil, 
  Trash2, 
  RefreshCw, 
  Plug, 
  Ban, 
  Server, 
  Cpu, 
  MoreHorizontal,
  Plus,
  Settings,
  FileText,
  Database,
  Activity
} from 'lucide-react';

/**
 * Find a specific link by its rel attribute
 */
export const findLink = (links: Link[], rel: string): Link | undefined => {
  return links.find(link => link.rel === rel);
};

/**
 * Execute an action using a HATEOAS link
 */
export const executeAction = async (link: Link, payload?: any) => {
  const response = await fetch(link.href, {
    method: link.method,
    headers: { 
      'Content-Type': 'application/json',
    },
    body: payload ? JSON.stringify(payload) : undefined
  });
  
  if (!response.ok) {
    throw new Error(`Action failed: ${link.title}`);
  }
  
  return response.json();
};

/**
 * Get the appropriate icon for a given action rel
 */
export const getActionIcon = (rel: string) => {
  const iconMap: Record<string, any> = {
    self: Eye,
    edit: Pencil,
    delete: Trash2,
    sync: RefreshCw,
    testConnection: Plug,
    disable: Ban,
    enable: RefreshCw,
    addCluster: Server,
    addNode: Cpu,
    viewDetails: Eye,
    create: Plus,
    settings: Settings,
    logs: FileText,
    vms: Database,
    overview: Activity
  };
  return iconMap[rel] || MoreHorizontal;
};

/**
 * Get all enabled actions from links
 */
export const getEnabledActions = (links: Link[]): Link[] => {
  return links.filter(link => link.enabled && link.rel !== 'self');
};

/**
 * Check if a specific action is available and enabled
 */
export const isActionEnabled = (links: Link[], rel: string): boolean => {
  const link = findLink(links, rel);
  return link?.enabled ?? false;
};

/**
 * Get the reason why an action is disabled
 */
export const getActionReason = (links: Link[], rel: string): string | undefined => {
  const link = findLink(links, rel);
  return link?.reason;
};

/**
 * Group actions by category for better organization
 */
export const groupActionsByCategory = (links: Link[]): Record<string, Link[]> => {
  const actions = getEnabledActions(links);
  
  return {
    primary: actions.filter(link => ['self', 'viewDetails', 'edit'].includes(link.rel)),
    management: actions.filter(link => ['sync', 'testConnection', 'enable', 'disable'].includes(link.rel)),
    creation: actions.filter(link => ['addCluster', 'addNode', 'create'].includes(link.rel)),
    destructive: actions.filter(link => ['delete'].includes(link.rel)),
    navigation: actions.filter(link => ['settings', 'logs', 'overview', 'vms'].includes(link.rel))
  };
};

/**
 * Sort actions by priority for display order
 */
export const sortActionsByPriority = (links: Link[]): Link[] => {
  const priorityOrder = [
    'viewDetails', 'edit', 'sync', 'testConnection', 
    'addCluster', 'addNode', 'enable', 'disable', 
    'settings', 'logs', 'overview', 'vms', 'delete'
  ];
  
  return links.sort((a, b) => {
    const aIndex = priorityOrder.indexOf(a.rel);
    const bIndex = priorityOrder.indexOf(b.rel);
    
    if (aIndex === -1 && bIndex === -1) return 0;
    if (aIndex === -1) return 1;
    if (bIndex === -1) return -1;
    
    return aIndex - bIndex;
  });
};

/**
 * Extract API endpoint from link for navigation
 */
export const getNavigationPath = (link: Link): string => {
  // Convert API href to frontend route
  if (link.href.startsWith('/api/')) {
    return link.href.replace('/api/v1', '/system');
  }
  return link.href;
};
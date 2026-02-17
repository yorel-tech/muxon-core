'use client';

import { useState, use } from 'react';
import Link from 'next/link';
import { Card, CardContent, CardHeader } from '@/components/ui/atoms/card';
import { Badge } from '@/components/ui/atoms/badge';
import { motion } from 'framer-motion';
import {
  ArrowLeft,
  Server,
  Database,
  Cpu,
  MemoryStick,
  Activity,
  RefreshCw,
  Settings,
  FileText,
  Clock,
  CheckCircle2,
  XCircle,
  MoreHorizontal,
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

// Mock provider data
const mockProvider: Provider = {
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
};

const recentActivities = [
  {
    id: '1',
    type: 'sync',
    message: 'Provider synced successfully',
    time: '2 minutes ago',
    status: 'success',
  },
  {
    id: '2',
    type: 'vm',
    message: 'VM web-server-01 created',
    time: '15 minutes ago',
    status: 'success',
  },
  {
    id: '3',
    type: 'node',
    message: 'Node node-03 went offline',
    time: '1 hour ago',
    status: 'error',
  },
];

export default function ProviderDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [activeTab, setActiveTab] = useState('overview');
  const [provider] = useState<Provider>(mockProvider);

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

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-full px-3 py-8">
        {/* Back Button */}
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
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

        {/* Provider Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.1 }}
          className="mb-6"
        >
          <Card>
            <CardContent className="p-6">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-4">
                    <h1 className="text-3xl font-bold text-gray-900">
                      {provider.name}
                    </h1>
                    <Badge variant={getStatusBadgeVariant(provider.status)}>
                      {provider.status}
                    </Badge>
                  </div>
                  <div className="flex items-center gap-2 text-sm text-gray-500">
                    <span>{getTypeLabel(provider.type)}</span>
                    <span>•</span>
                    <span>Last sync: {provider.lastSync}</span>
                  </div>
                  <div className="mt-4 p-4 bg-gray-50 rounded-lg">
                    <p className="text-sm text-gray-600">
                      <span className="font-medium">Endpoint:</span> {provider.endpoint}
                    </p>
                  </div>
                </div>
                <button className="p-2 rounded-lg hover:bg-gray-100 transition-colors">
                  <MoreHorizontal className="h-5 w-5 text-gray-600" />
                </button>
              </div>
            </CardContent>
          </Card>
        </motion.div>

        {/* Stats Cards */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.2 }}
          className="grid gap-4 mb-6 grid-cols-2 lg:grid-cols-4"
        >
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-50">
                  <Server className="h-5 w-5 text-primary-600" />
                </div>
                <div>
                  <p className="text-sm text-gray-500">Nodes</p>
                  <p className="text-2xl font-bold text-gray-900">{provider.nodes}</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-purple-50">
                  <Database className="h-5 w-5 text-purple-600" />
                </div>
                <div>
                  <p className="text-sm text-gray-500">VMs</p>
                  <p className="text-2xl font-bold text-gray-900">{provider.vms}</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-green-50">
                  <Cpu className="h-5 w-5 text-green-600" />
                </div>
                <div>
                  <p className="text-sm text-gray-500">vCPUs</p>
                  <p className="text-2xl font-bold text-gray-900">45</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-orange-50">
                  <MemoryStick className="h-5 w-5 text-orange-600" />
                </div>
                <div>
                  <p className="text-sm text-gray-500">Memory</p>
                  <p className="text-2xl font-bold text-gray-900">128 GB</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </motion.div>

        {/* Tab Content - Manual implementation */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.3 }}
        >
          {/* Tab Navigation */}
          <div className="flex gap-2 mb-6 border-b border-gray-200">
            {['overview', 'nodes', 'vms', 'settings', 'logs'].map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={`
                  px-4 py-2 text-sm font-medium transition-colors
                  ${activeTab === tab 
                    ? 'text-primary-600 border-b-2 border-primary-600' 
                    : 'text-gray-600 border-b-2 border-transparent hover:text-gray-900'}
                `}
              >
                {tab.charAt(0).toUpperCase() + tab.slice(1)}
              </button>
            ))}
          </div>

          {/* Tab Content */}
          {activeTab === 'overview' && (
            <div className="space-y-6">
              {/* Health Metrics */}
              <Card>
                <CardHeader>Health Metrics</CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    <div>
                      <div className="flex justify-between mb-2">
                        <span className="text-sm font-medium text-gray-700">CPU Usage</span>
                        <span className="text-sm font-semibold text-gray-900">45%</span>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-2">
                        <div className="bg-primary-600 h-2 rounded-full" style={{ width: '45%' }} />
                      </div>
                    </div>
                    <div>
                      <div className="flex justify-between mb-2">
                        <span className="text-sm font-medium text-gray-700">Memory Usage</span>
                        <span className="text-sm font-semibold text-gray-900">62%</span>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-2">
                        <div className="bg-purple-600 h-2 rounded-full" style={{ width: '62%' }} />
                      </div>
                    </div>
                    <div>
                      <div className="flex justify-between mb-2">
                        <span className="text-sm font-medium text-gray-700">Storage Usage</span>
                        <span className="text-sm font-semibold text-gray-900">38%</span>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-2">
                        <div className="bg-green-600 h-2 rounded-full" style={{ width: '38%' }} />
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>

              {/* Capabilities */}
              <Card>
                <CardHeader>Capabilities</CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    <div className="flex items-center gap-3">
                      {provider.capabilities.vmLifecycle ? (
                        <CheckCircle2 className="h-5 w-5 text-green-600" />
                      ) : (
                        <XCircle className="h-5 w-5 text-red-600" />
                      )}
                      <span className="text-gray-700">VM Lifecycle Management</span>
                    </div>
                    <div className="flex items-center gap-3">
                      {provider.capabilities.snapshots ? (
                        <CheckCircle2 className="h-5 w-5 text-green-600" />
                      ) : (
                        <XCircle className="h-5 w-5 text-red-600" />
                      )}
                      <span className="text-gray-700">Snapshots</span>
                    </div>
                    <div className="flex items-center gap-3">
                      {provider.capabilities.backups ? (
                        <CheckCircle2 className="h-5 w-5 text-green-600" />
                      ) : (
                        <XCircle className="h-5 w-5 text-red-600" />
                      )}
                      <span className="text-gray-700">Backups</span>
                    </div>
                  </div>
                </CardContent>
              </Card>

              {/* Recent Activity */}
              <Card>
                <CardHeader>Recent Activity</CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    {recentActivities.map((activity) => (
                      <div
                        key={activity.id}
                        className="flex items-start gap-3 p-3 rounded-lg border border-gray-100"
                      >
                        <div className="flex-shrink-0">
                          {activity.status === 'success' ? (
                            <CheckCircle2 className="h-5 w-5 text-green-600" />
                          ) : (
                            <XCircle className="h-5 w-5 text-red-600" />
                          )}
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="text-sm text-gray-900">{activity.message}</p>
                          <div className="flex items-center gap-2 mt-1">
                            <Clock size={14} className="text-gray-400" />
                            <p className="text-xs text-gray-500">{activity.time}</p>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            </div>
          )}

          {activeTab === 'nodes' && (
            <Card>
              <CardContent className="p-12 text-center text-gray-500">
                <Server size={48} className="mx-auto mb-4 text-gray-300" />
                <p>Nodes list will be displayed here</p>
              </CardContent>
            </Card>
          )}

          {activeTab === 'vms' && (
            <Card>
              <CardContent className="p-12 text-center text-gray-500">
                <Database size={48} className="mx-auto mb-4 text-gray-300" />
                <p>VMs list will be displayed here</p>
              </CardContent>
            </Card>
          )}

          {activeTab === 'settings' && (
            <Card>
              <CardContent className="p-12 text-center text-gray-500">
                <Settings size={48} className="mx-auto mb-4 text-gray-300" />
                <p>Provider settings will be displayed here</p>
              </CardContent>
            </Card>
          )}

          {activeTab === 'logs' && (
            <Card>
              <CardContent className="p-12 text-center text-gray-500">
                <FileText size={48} className="mx-auto mb-4 text-gray-300" />
                <p>Activity logs will be displayed here</p>
              </CardContent>
            </Card>
          )}
        </motion.div>
      </div>
    </div>
  );
}

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
  Users,
  Settings,
  Shield,
  BarChart3,
  FileText,
  CheckCircle2,
  XCircle,
  MoreHorizontal,
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

// Mock tenant data
const mockTenant: Tenant = {
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
};

const recentActivities = [
  {
    id: '1',
    type: 'user',
    message: 'User john@example.com added',
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
    type: 'datacenter',
    message: 'Datacenter dc-01 assigned',
    time: '1 hour ago',
    status: 'success',
  },
];

export default function TenantDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [activeTab, setActiveTab] = useState('overview');
  const [tenant] = useState<Tenant>(mockTenant);

  const tabs = [
    { id: 'overview', label: 'Overview' },
    { id: 'datacenters', label: 'Datacenters' },
    { id: 'vms', label: 'VMs' },
    { id: 'users', label: 'Users' },
    { id: 'settings', label: 'Settings' },
    { id: 'idp', label: 'Identity Provider' },
    { id: 'quotas', label: 'Quotas' },
    { id: 'audit', label: 'Audit Log' },
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
            href="/system/tenants"
            className="inline-flex items-center gap-2 text-gray-600 hover:text-gray-900 transition-colors"
          >
            <ArrowLeft size={20} />
            <span className="font-medium">Back to Tenants</span>
          </Link>
        </motion.div>

        {/* Tenant Header */}
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
                      {tenant.name}
                    </h1>
                    <Badge variant={getStatusBadgeVariant(tenant.status)}>
                      {tenant.status}
                    </Badge>
                  </div>
                  <div className="flex items-center gap-2 text-sm text-gray-500">
                    <span>Slug: {tenant.slug}</span>
                    <span>•</span>
                    <span>Created: {formatDate(tenant.createdAt)}</span>
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
                  <Users className="h-5 w-5 text-primary-600" />
                </div>
                <div>
                  <p className="text-sm text-gray-500">Users</p>
                  <p className="text-2xl font-bold text-gray-900">{tenant.users}</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-purple-50">
                  <Server className="h-5 w-5 text-purple-600" />
                </div>
                <div>
                  <p className="text-sm text-gray-500">Datacenters</p>
                  <p className="text-2xl font-bold text-gray-900">{tenant.datacenters}</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-green-50">
                  <Database className="h-5 w-5 text-green-600" />
                </div>
                <div>
                  <p className="text-sm text-gray-500">VMs</p>
                  <p className="text-2xl font-bold text-gray-900">{tenant.vms}</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-orange-50">
                  <BarChart3 className="h-5 w-5 text-orange-600" />
                </div>
                <div>
                  <p className="text-sm text-gray-500">Quota Usage</p>
                  <p className="text-2xl font-bold text-gray-900">30%</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </motion.div>

        {/* Tab Navigation */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.3 }}
        >
          <div className="flex gap-2 mb-6 border-b border-gray-200">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`
                  px-4 py-2 text-sm font-medium transition-colors
                  ${activeTab === tab.id 
                    ? 'text-primary-600 border-b-2 border-primary-600' 
                    : 'text-gray-600 border-b-2 border-transparent hover:text-gray-900'}
                `}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </motion.div>

        {/* Tab Content */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.4 }}
          className="mt-6"
        >
          {activeTab === 'overview' && (
            <div className="space-y-6">
              {/* Quota Usage */}
              <Card>
                <CardHeader>Quota Usage</CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    <div>
                      <div className="flex justify-between mb-2">
                        <span className="text-sm font-medium text-gray-700">VMs</span>
                        <span className="text-sm text-gray-500">
                          {tenant.vms} / {tenant.settings.quotas.vms}
                        </span>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-2">
                        <div 
                          className="bg-primary-600 h-2 rounded-full" 
                          style={{ width: `${(tenant.vms / tenant.settings.quotas.vms * 100).toFixed(0)}%` }} 
                        />
                      </div>
                    </div>
                    <div>
                      <div className="flex justify-between mb-2">
                        <span className="text-sm font-medium text-gray-700">vCPUs</span>
                        <span className="text-sm text-gray-500">
                          45 / {tenant.settings.quotas.vcpus}
                        </span>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-2">
                        <div 
                          className="bg-purple-600 h-2 rounded-full" 
                          style={{ width: `${(45 / tenant.settings.quotas.vcpus * 100).toFixed(0)}%` }} 
                        />
                      </div>
                    </div>
                    <div>
                      <div className="flex justify-between mb-2">
                        <span className="text-sm font-medium text-gray-700">Memory</span>
                        <span className="text-sm text-gray-500">
                          256 GB / {tenant.settings.quotas.memory} GB
                        </span>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-2">
                        <div 
                          className="bg-green-600 h-2 rounded-full" 
                          style={{ width: `${(256 / tenant.settings.quotas.memory * 100).toFixed(0)}%` }} 
                        />
                      </div>
                    </div>
                    <div>
                      <div className="flex justify-between mb-2">
                        <span className="text-sm font-medium text-gray-700">Storage</span>
                        <span className="text-sm text-gray-500">
                          768 GB / {tenant.settings.quotas.storage} GB
                        </span>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-2">
                        <div 
                          className="bg-orange-600 h-2 rounded-full" 
                          style={{ width: `${(768 / tenant.settings.quotas.storage * 100).toFixed(0)}%` }} 
                        />
                      </div>
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
                          <p className="text-xs text-gray-500 mt-1">{activity.time}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            </div>
          )}

          {activeTab === 'datacenters' && (
            <Card>
              <CardContent className="p-12 text-center text-gray-500">
                <Server size={48} className="mx-auto mb-4 text-gray-300" />
                <p>Tenant datacenters will be displayed here</p>
              </CardContent>
            </Card>
          )}

          {activeTab === 'vms' && (
            <Card>
              <CardContent className="p-12 text-center text-gray-500">
                <Database size={48} className="mx-auto mb-4 text-gray-300" />
                <p>Tenant VMs will be displayed here</p>
              </CardContent>
            </Card>
          )}

          {activeTab === 'users' && (
            <Card>
              <CardContent className="p-12 text-center text-gray-500">
                <Users size={48} className="mx-auto mb-4 text-gray-300" />
                <p>Tenant users will be displayed here</p>
              </CardContent>
            </Card>
          )}

          {activeTab === 'settings' && (
            <Card>
              <CardContent className="p-12 text-center text-gray-500">
                <Settings size={48} className="mx-auto mb-4 text-gray-300" />
                <p>Tenant settings will be displayed here</p>
              </CardContent>
            </Card>
          )}

          {activeTab === 'idp' && (
            <Card>
              <CardContent className="p-12 text-center text-gray-500">
                <Shield size={48} className="mx-auto mb-4 text-gray-300" />
                <p>Identity Provider configuration will be displayed here</p>
              </CardContent>
            </Card>
          )}

          {activeTab === 'quotas' && (
            <Card>
              <CardHeader>Quota Limits</CardHeader>
              <CardContent>
                <div className="grid gap-4 grid-cols-1 sm:grid-cols-2">
                  <div className="p-4 bg-gray-50 rounded-lg">
                    <div className="flex items-center gap-3 mb-2">
                      <Database className="h-8 w-8 text-primary-600" />
                      <span className="text-gray-700">VMs</span>
                    </div>
                    <p className="text-3xl font-bold text-gray-900">{tenant.settings.quotas.vms}</p>
                    <p className="text-sm text-gray-500">Maximum virtual machines</p>
                  </div>
                  <div className="p-4 bg-gray-50 rounded-lg">
                    <div className="flex items-center gap-3 mb-2">
                      <BarChart3 className="h-8 w-8 text-purple-600" />
                      <span className="text-gray-700">vCPUs</span>
                    </div>
                    <p className="text-3xl font-bold text-gray-900">{tenant.settings.quotas.vcpus}</p>
                    <p className="text-sm text-gray-500">Maximum CPU cores</p>
                  </div>
                  <div className="p-4 bg-gray-50 rounded-lg">
                    <div className="flex items-center gap-3 mb-2">
                      <Server className="h-8 w-8 text-green-600" />
                      <span className="text-gray-700">Memory</span>
                    </div>
                    <p className="text-3xl font-bold text-gray-900">{tenant.settings.quotas.memory} GB</p>
                    <p className="text-sm text-gray-500">Maximum memory allocation</p>
                  </div>
                  <div className="p-4 bg-gray-50 rounded-lg">
                    <div className="flex items-center gap-3 mb-2">
                      <Users className="h-8 w-8 text-orange-600" />
                      <span className="text-gray-700">Storage</span>
                    </div>
                    <p className="text-3xl font-bold text-gray-900">{tenant.settings.quotas.storage} GB</p>
                    <p className="text-sm text-gray-500">Maximum storage allocation</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          )}

          {activeTab === 'audit' && (
            <Card>
              <CardContent className="p-12 text-center text-gray-500">
                <FileText size={48} className="mx-auto mb-4 text-gray-300" />
                <p>Audit log will be displayed here</p>
              </CardContent>
            </Card>
          )}
        </motion.div>
      </div>
    </div>
  );
}

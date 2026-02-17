'use client';

import { Card, CardContent, CardHeader } from '@/components/ui/atoms/card';
import { Table, Column } from '@/components/ui/organisms/table';
import { Badge } from '@/components/ui/atoms/badge';
import { motion } from 'framer-motion';
import { 
  Server, 
  Database, 
  Users, 
  Building2,
  Activity,
  TrendingUp,
  TrendingDown,
  AlertTriangle,
  CheckCircle2,
  MoreHorizontal,
  Search,
  Filter,
  RefreshCw,
  KeyRound
} from 'lucide-react';
 
interface Datacenter extends Record<string, any> {
  id: string;
  name: string;
  type: string;
  status: 'online' | 'offline' | 'warning';
  nodes: number;
  vms: number;
  [key: string]: any;
}
 
interface RecentActivity extends Record<string, any> {
  id: string;
  type: string;
  message: string;
  time: string;
  status: 'success' | 'error' | 'warning';
  [key: string]: any;
}
 
interface SystemHealth extends Record<string, any> {
  component: string;
  status: 'healthy' | 'degraded' | 'down';
  uptime: string;
  [key: string]: any;
}
 
const mockDatacenters: Datacenter[] = [
  {
    id: '1',
    name: 'Primary Datacenter',
    type: 'Proxmox',
    status: 'online',
    nodes: 5,
    vms: 23,
  },
  {
    id: '2',
    name: 'Secondary Datacenter',
    type: 'Libvirt',
    status: 'online',
    nodes: 3,
    vms: 12,
  },
  {
    id: '3',
    name: 'Cloud Provider AWS',
    type: 'AWS',
    status: 'warning',
    nodes: 0,
    vms: 8,
  },
];
 
const mockActivity: RecentActivity[] = [
  {
    id: '1',
    type: 'VM',
    message: 'VM web-server-01 created successfully',
    time: '2 minutes ago',
    status: 'success',
  },
  {
    id: '2',
    type: 'User',
    message: 'New user alice@example.com added',
    time: '15 minutes ago',
    status: 'success',
  },
  {
    id: '3',
    type: 'Datacenter',
    message: 'Primary Datacenter connection restored',
    time: '1 hour ago',
    status: 'success',
  },
  {
    id: '4',
    type: 'System',
    message: 'Backup job completed',
    time: '2 hours ago',
    status: 'success',
  },
  {
    id: '5',
    type: 'Error',
    message: 'Failed to connect to AWS provider',
    time: '3 hours ago',
    status: 'error',
  },
];
 
const mockSystemHealth: SystemHealth[] = [
  {
    component: 'API Server',
    status: 'healthy',
    uptime: '99.9%',
  },
  {
    component: 'Database',
    status: 'healthy',
    uptime: '99.8%',
  },
  {
    component: 'Redis Cache',
    status: 'healthy',
    uptime: '99.9%',
  },
  {
    component: 'Message Queue',
    status: 'degraded',
    uptime: '98.5%',
  },
];
 
export default function ConfiguredSystemDashboardPage() {
  const datacenterColumns: Column<Datacenter>[] = [
    {
      key: 'name',
      header: 'Name',
      cell: (row: Datacenter) => (
        <div className="font-medium text-gray-900">{row.name}</div>
      ),
      sortable: true,
    },
    {
      key: 'type',
      header: 'Type',
      cell: (row: Datacenter) => (
        <Badge variant="info">{row.type}</Badge>
      ),
      sortable: true,
    },
    {
      key: 'status',
      header: 'Status',
      cell: (row: Datacenter) => (
        <Badge 
          variant={row.status === 'online' ? 'success' : row.status === 'warning' ? 'warning' : 'error'}
        >
          {row.status}
        </Badge>
      ),
      sortable: true,
    },
    {
      key: 'nodes',
      header: 'Nodes',
      cell: (row: Datacenter) => (
        <span className="text-gray-600">{row.nodes}</span>
      ),
      sortable: true,
    },
    {
      key: 'vms',
      header: 'VMs',
      cell: (row: Datacenter) => (
        <span className="text-gray-600">{row.vms}</span>
      ),
      sortable: true,
    },
  ];
 
  const activityColumns: Column<RecentActivity>[] = [
    {
      key: 'type',
      header: 'Type',
      cell: (row: RecentActivity) => (
        <Badge variant="default">{row.type}</Badge>
      ),
      sortable: true,
    },
    {
      key: 'message',
      header: 'Activity',
      cell: (row: RecentActivity) => (
        <div className="max-w-md">
          <p className="text-gray-900">{row.message}</p>
        </div>
      ),
      sortable: true,
    },
    {
      key: 'time',
      header: 'Time',
      cell: (row: RecentActivity) => (
        <span className="text-sm text-gray-500">{row.time}</span>
      ),
      sortable: true,
    },
    {
      key: 'status',
      header: 'Status',
      cell: (row: RecentActivity) => (
        <div className="flex items-center gap-2">
          {row.status === 'success' && (
            <CheckCircle2 className="h-4 w-4 text-green-600" />
          )}
          {row.status === 'error' && (
            <AlertTriangle className="h-4 w-4 text-red-600" />
          )}
          {row.status === 'warning' && (
            <AlertTriangle className="h-4 w-4 text-orange-600" />
          )}
        </div>
      ),
      sortable: true,
    },
  ];
 
  const getStatusColor = (status: string) => {
    switch (status) {
      case 'healthy':
        return 'text-green-600 bg-green-50';
      case 'degraded':
        return 'text-orange-600 bg-orange-50';
      case 'down':
        return 'text-red-600 bg-red-50';
      default:
        return 'text-gray-600 bg-gray-50';
    }
  };
 
  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-full px-3 py-8">
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
                System Dashboard
              </h1>
              <p className="text-gray-600 mt-2">
                Overview of your cloud infrastructure
              </p>
            </div>
            <div className="flex items-center gap-2">
              <button className="p-2 rounded-lg hover:bg-gray-100 transition-colors">
                <RefreshCw className="h-5 w-5 text-gray-600" />
              </button>
              <button className="p-2 rounded-lg hover:bg-gray-100 transition-colors">
                <MoreHorizontal className="h-5 w-5 text-gray-600" />
              </button>
            </div>
          </div>
        </motion.div>
 
        {/* Stats Cards */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.1 }}
          className="grid gap-6 mb-8 sm:grid-cols-2 lg:grid-cols-4"
        >
          <Card className="hover:shadow-lg transition-shadow">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-blue-50">
                    <Server className="h-6 w-6 text-primary-600" />
                  </div>
                  <div>
                    <p className="text-sm text-gray-500">Datacenters</p>
                    <p className="text-2xl font-bold text-gray-900">3</p>
                  </div>
                </div>
                <TrendingUp className="h-5 w-5 text-green-600" />
              </div>
            </CardContent>
          </Card>
          <Card className="hover:shadow-lg transition-shadow">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-purple-50">
                    <Database className="h-6 w-6 text-purple-600" />
                  </div>
                  <div>
                    <p className="text-sm text-gray-500">VMs</p>
                    <p className="text-2xl font-bold text-gray-900">43</p>
                  </div>
                </div>
                <TrendingUp className="h-5 w-5 text-green-600" />
              </div>
            </CardContent>
          </Card>
          <Card className="hover:shadow-lg transition-shadow">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-green-50">
                    <Users className="h-6 w-6 text-green-600" />
                  </div>
                  <div>
                    <p className="text-sm text-gray-500">System Users</p>
                    <p className="text-2xl font-bold text-gray-900">12</p>
                  </div>
                </div>
                <TrendingUp className="h-5 w-5 text-green-600" />
              </div>
            </CardContent>
          </Card>
          <Card className="hover:shadow-lg transition-shadow">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-orange-50">
                    <Building2 className="h-6 w-6 text-orange-600" />
                  </div>
                  <div>
                    <p className="text-sm text-gray-500">Tenants</p>
                    <p className="text-2xl font-bold text-gray-900">8</p>
                  </div>
                </div>
                <TrendingUp className="h-5 w-5 text-green-600" />
              </div>
            </CardContent>
          </Card>
        </motion.div>
 
        {/* Main Content Grid */}
        <div className="grid gap-6 lg:grid-cols-3">
          {/* Datacenters */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.2 }}
            className="lg:col-span-2"
          >
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <h2 className="text-xl font-semibold text-gray-900">
                    Datacenters
                  </h2>
                  <div className="flex items-center gap-2">
                    <button className="p-1.5 rounded-md hover:bg-gray-100 transition-colors">
                      <Filter className="h-4 w-4 text-gray-600" />
                    </button>
                    <button className="p-1.5 rounded-md hover:bg-gray-100 transition-colors">
                      <RefreshCw className="h-4 w-4 text-gray-600" />
                    </button>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="p-0">
                <Table
                  columns={datacenterColumns}
                  data={mockDatacenters}
                  emptyMessage="No datacenters configured"
                  onRowClick={(row) => console.log('Clicked row:', row)}
                />
              </CardContent>
            </Card>
          </motion.div>
 
          {/* System Health */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.3 }}
          >
            <Card>
              <CardHeader>
                <h2 className="text-xl font-semibold text-gray-900">
                  System Health
                </h2>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {mockSystemHealth.map((item, index) => (
                    <div
                      key={item.component}
                      className="flex items-center justify-between p-4 rounded-lg border border-gray-100"
                    >
                      <div className="flex items-center gap-3">
                        <Activity className={`h-5 w-5 ${getStatusColor(item.status)}`} />
                        <div>
                          <p className="font-medium text-gray-900">{item.component}</p>
                          <p className="text-sm text-gray-500">
                            {item.status === 'healthy' && 'All systems operational'}
                            {item.status === 'degraded' && 'Some issues detected'}
                            {item.status === 'down' && 'Service unavailable'}
                          </p>
                        </div>
                      </div>
                      <div className="text-right">
                        <p className={`text-sm font-semibold ${getStatusColor(item.status)}`}>
                          {item.status}
                        </p>
                        <p className="text-xs text-gray-500">{item.uptime}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </motion.div>
 
          {/* Recent Activity */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.4 }}
          >
            <Card>
              <CardHeader>
                <h2 className="text-xl font-semibold text-gray-900">
                  Recent Activity
                </h2>
              </CardHeader>
              <CardContent className="p-0">
                <Table
                  columns={activityColumns}
                  data={mockActivity}
                  emptyMessage="No recent activity"
                />
              </CardContent>
            </Card>
          </motion.div>
 
          {/* Quick Actions */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.5 }}
          >
            <Card>
              <CardHeader>
                <h2 className="text-xl font-semibold text-gray-900">
                  Quick Actions
                </h2>
              </CardHeader>
              <CardContent>
                <div className="grid gap-3 sm:grid-cols-2">
                  <button className="flex items-center gap-3 p-4 rounded-lg border border-gray-200 hover:border-primary-300 hover:bg-gray-50 transition-colors text-left">
                    <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-50">
                      <Database className="h-5 w-5 text-primary-600" />
                    </div>
                    <div>
                      <p className="font-medium text-gray-900">Add Datacenter</p>
                      <p className="text-sm text-gray-500">Connect new infrastructure</p>
                    </div>
                  </button>
                  <button className="flex items-center gap-3 p-4 rounded-lg border border-gray-200 hover:border-primary-300 hover:bg-gray-50 transition-colors text-left">
                    <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-purple-50">
                      <Users className="h-5 w-5 text-purple-600" />
                    </div>
                    <div>
                      <p className="font-medium text-gray-900">Add User</p>
                      <p className="text-sm text-gray-500">Create system user</p>
                    </div>
                  </button>
                  <button className="flex items-center gap-3 p-4 rounded-lg border border-gray-200 hover:border-primary-300 hover:bg-gray-50 transition-colors text-left">
                    <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-green-50">
                      <Building2 className="h-5 w-5 text-green-600" />
                    </div>
                    <div>
                      <p className="font-medium text-gray-900">Create Tenant</p>
                      <p className="text-sm text-gray-500">Add organization</p>
                    </div>
                  </button>
                  <button className="flex items-center gap-3 p-4 rounded-lg border border-gray-200 hover:border-primary-300 hover:bg-gray-50 transition-colors text-left">
                    <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-orange-50">
                      <KeyRound className="h-5 w-5 text-orange-600" />
                    </div>
                    <div>
                      <p className="font-medium text-gray-900">Configure IDP</p>
                      <p className="text-sm text-gray-500">Set up authentication</p>
                    </div>
                  </button>
                  <button className="flex items-center gap-3 p-4 rounded-lg border border-gray-200 hover:border-primary-300 hover:bg-gray-50 transition-colors text-left">
                    <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-pink-50">
                      <Server className="h-5 w-5 text-pink-600" />
                    </div>
                    <div>
                      <p className="font-medium text-gray-900">View Logs</p>
                      <p className="text-sm text-gray-500">System activity</p>
                    </div>
                  </button>
                </div>
              </CardContent>
            </Card>
          </motion.div>
        </div>
      </div>
    </div>
  );
}

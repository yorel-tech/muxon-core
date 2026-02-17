'use client';

import { useState } from 'react';
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
  CheckCircle2,
  RefreshCw,
  MoreHorizontal,
  Menu,
} from 'lucide-react';
import { AlertsCard, Alert } from '@/components/ui/organisms/alerts-card';
import { QuickActions, defaultQuickActions } from '@/components/ui/organisms/quick-actions';

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

// Mock alerts data
const mockAlerts: Alert[] = [
  {
    id: '1',
    severity: 'critical',
    title: 'Provider Unreachable',
    message: 'Provider 3 (AWS) is not accessible. Connection timeout.',
    timestamp: '5 minutes ago',
    source: 'provider-3',
    action: {
      label: 'View Provider',
      onClick: () => console.log('Navigate to provider-3'),
    },
    dismissible: true,
  },
  {
    id: '2',
    severity: 'warning',
    title: 'Capacity Warning',
    message: 'Datacenter 1 has reached 95% capacity. Consider scaling.',
    timestamp: '15 minutes ago',
    source: 'datacenter-1',
    action: {
      label: 'View Datacenter',
      onClick: () => console.log('Navigate to datacenter-1'),
    },
    dismissible: true,
  },
  {
    id: '3',
    severity: 'warning',
    title: 'High Memory Usage',
    message: 'Datacenter 2 memory usage is at 85%. Monitor closely.',
    timestamp: '1 hour ago',
    source: 'datacenter-2',
    dismissible: true,
  },
  {
    id: '4',
    severity: 'info',
    title: 'Backup Completed',
    message: 'Daily backup job completed successfully.',
    timestamp: '2 hours ago',
    source: 'system',
    dismissible: true,
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

const getStatusColor = (status: string) => {
  switch (status) {
    case 'healthy':
      return 'text-success-600 bg-success-50 dark:text-success-400 dark:bg-success-900/30';
    case 'degraded':
      return 'text-warning-600 bg-warning-50 dark:text-warning-400 dark:bg-warning-900/30';
    case 'down':
      return 'text-error-600 bg-error-50 dark:text-error-400 dark:bg-error-900/30';
    default:
      return 'text-gray-600 bg-gray-50 dark:text-gray-400 dark:bg-gray-800';
  }
};

export default function SystemDashboardPage() {
  const [alerts, setAlerts] = useState<Alert[]>(mockAlerts);

  const handleDismissAlert = (alertId: string) => {
    setAlerts((prev) => prev.filter((alert) => alert.id !== alertId));
  };

  const handleAlertAction = (alertId: string) => {
    const alert = alerts.find((a) => a.id === alertId);
    alert?.action?.onClick();
  };

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
          <p className="text-gray-900 dark:text-gray-100">{row.message}</p>
        </div>
      ),
      sortable: true,
    },
    {
      key: 'time',
      header: 'Time',
      cell: (row: RecentActivity) => (
        <span className="text-sm text-gray-500 dark:text-gray-400">{row.time}</span>
      ),
      sortable: true,
    },
    {
      key: 'status',
      header: 'Status',
      cell: (row: RecentActivity) => (
        <div className="flex items-center gap-2">
          {row.status === 'success' && (
            <CheckCircle2 className="h-4 w-4 text-success-600 dark:text-success-400" />
          )}
          {row.status === 'error' && (
            <MoreHorizontal className="h-4 w-4 text-error-600 dark:text-error-400" />
          )}
          {row.status === 'warning' && (
            <MoreHorizontal className="h-4 w-4 text-warning-600 dark:text-warning-400" />
          )}
        </div>
      ),
      sortable: true,
    },
  ];

  return (
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
              <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100">
                System Dashboard
              </h1>
              <p className="text-gray-600 dark:text-gray-400 mt-2">
                Overview of your cloud infrastructure
              </p>
            </div>
            <div className="flex items-center gap-2">
              <button className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors">
                <RefreshCw className="h-5 w-5 text-gray-600 dark:text-gray-400" />
              </button>
              <button className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors">
                <MoreHorizontal className="h-5 w-5 text-gray-600 dark:text-gray-400" />
              </button>
            </div>
          </div>
        </motion.div>

        {/* Stats Cards */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.1 }}
          className="grid gap-6 mb-8 grid-cols-1 sm:grid-cols-2 lg:grid-cols-4"
        >
          <Card className="hover:shadow-lg dark:hover:shadow-gray-700/50 transition-shadow">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary-50 dark:bg-primary-900/30">
                    <Server className="h-6 w-6 text-primary-600 dark:text-primary-400" />
                  </div>
                  <div>
                    <p className="text-sm text-gray-500 dark:text-gray-400">Datacenters</p>
                    <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">3</p>
                  </div>
                </div>
                <TrendingUp className="h-5 w-5 text-success-600 dark:text-success-400" />
              </div>
            </CardContent>
          </Card>
          <Card className="hover:shadow-lg dark:hover:shadow-gray-700/50 transition-shadow">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-nexus-50 dark:bg-nexus-900/30">
                    <Database className="h-6 w-6 text-nexus-600 dark:text-nexus-400" />
                  </div>
                  <div>
                    <p className="text-sm text-gray-500 dark:text-gray-400">VMs</p>
                    <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">43</p>
                  </div>
                </div>
                <TrendingUp className="h-5 w-5 text-success-600 dark:text-success-400" />
              </div>
            </CardContent>
          </Card>
          <Card className="hover:shadow-lg dark:hover:shadow-gray-700/50 transition-shadow">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-success-50 dark:bg-success-900/30">
                    <Users className="h-6 w-6 text-success-600 dark:text-success-400" />
                  </div>
                  <div>
                    <p className="text-sm text-gray-500 dark:text-gray-400">System Users</p>
                    <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">12</p>
                  </div>
                </div>
                <TrendingUp className="h-5 w-5 text-success-600 dark:text-success-400" />
              </div>
            </CardContent>
          </Card>
          <Card className="hover:shadow-lg dark:hover:shadow-gray-700/50 transition-shadow">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-warning-50 dark:bg-warning-900/30">
                    <Building2 className="h-6 w-6 text-warning-600 dark:text-warning-400" />
                  </div>
                  <div>
                    <p className="text-sm text-gray-500 dark:text-gray-400">Tenants</p>
                    <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">8</p>
                  </div>
                </div>
                <TrendingUp className="h-5 w-5 text-success-600 dark:text-success-400" />
              </div>
            </CardContent>
          </Card>
        </motion.div>

        {/* Main Content Grid */}
        <div className="grid gap-6 lg:grid-cols-3 mb-6">
          {/* Alerts Card - Spans 2/3 */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.2 }}
            className="lg:col-span-2"
          >
            <AlertsCard
              alerts={alerts}
              onDismiss={handleDismissAlert}
              onAction={handleAlertAction}
            />
          </motion.div>

          {/* System Health Card - Spans 1/3, same height as Alerts */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.3 }}
          >
            <Card className="dark:border-gray-700">
              <CardHeader>
                <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">
                  System Health
                </h2>
              </CardHeader>
              <CardContent className="p-0">
                <div className="p-4 space-y-4">
                  {mockSystemHealth.map((item) => (
                    <div
                      key={item.component}
                      className="flex items-center justify-between p-4 rounded-lg border border-gray-100 dark:border-gray-700"
                    >
                      <div className="flex items-center gap-3">
                        <Activity className={`h-5 w-5 ${getStatusColor(item.status)}`} />
                        <div>
                          <p className="font-medium text-gray-900 dark:text-gray-100">{item.component}</p>
                          <p className="text-sm text-gray-500 dark:text-gray-400">
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
                        <p className="text-xs text-gray-500 dark:text-gray-400">{item.uptime}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </motion.div>
        </div>

        {/* Quick Actions - Single Row */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.4 }}
          className="mb-6"
        >
          <QuickActions actions={defaultQuickActions} />
        </motion.div>

        {/* Recent Activity - Full Width */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.5 }}
        >
          <Card className="dark:border-gray-700">
            <CardHeader>
              <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">
                Recent Activity
              </h2>
            </CardHeader>
            <CardContent className="p-0">
              <Table
                columns={activityColumns}
                data={mockActivity}
                emptyMessage="No recent activity"
                onRowClick={(row) => console.log('Clicked row:', row)}
              />
            </CardContent>
          </Card>
        </motion.div>
        </div>
  );
}

'use client';

import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader } from '@/components/ui/atoms/card';
import { Badge } from '@/components/ui/atoms/badge';
import { motion } from 'framer-motion';
import {
  Server,
  Database,
  Cpu,
  HardDrive,
  Activity,
  TrendingUp,
  Bell,
  Loader2,
} from 'lucide-react';
import { AlertsCard, Alert } from '@/components/ui/organisms/alerts-card';
import { apiGet } from '@/lib/api';

interface QuotaUsage {
  vms: { used: number; limit: number };
  vcpus: { used: number; limit: number };
  memoryGb: { used: number; limit: number };
  storageGb: { used: number; limit: number };
}

const placeholderAlerts: Alert[] = [
  {
    id: '1',
    severity: 'info',
    title: 'Welcome',
    message: 'Your tenant dashboard shows resource usage and quotas.',
    timestamp: 'Just now',
    source: 'system',
    dismissible: true,
  },
];

export default function TenantDashboardPage() {
  const [quota, setQuota] = useState<QuotaUsage>({
    vms: { used: 0, limit: 0 },
    vcpus: { used: 0, limit: 0 },
    memoryGb: { used: 0, limit: 0 },
    storageGb: { used: 0, limit: 0 },
  });
  const [alerts, setAlerts] = useState<Alert[]>(placeholderAlerts);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const [vmsRes, tenantRes] = await Promise.all([
          apiGet<{ items?: unknown[]; total?: number }>('/api/v1/vms').catch(() => ({ items: [], total: 0 })),
          apiGet<{ settings?: { quotas?: Record<string, number> } }>('/api/v1/tenants/current').catch(() => ({})),
        ]);
        const vmsTotal = typeof vmsRes?.total === 'number' ? vmsRes.total : (vmsRes?.items?.length ?? 0);
        const quotas = tenantRes?.settings?.quotas ?? {};
        setQuota({
          vms: { used: vmsTotal, limit: quotas.vms ?? 0 },
          vcpus: { used: 0, limit: quotas.vcpus ?? 0 },
          memoryGb: { used: 0, limit: quotas.memory ?? 0 },
          storageGb: { used: 0, limit: quotas.storage ?? 0 },
        });
      } catch {
        setQuota((q) => q);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const handleDismissAlert = (id: string) => {
    setAlerts((prev) => prev.filter((a) => a.id !== id));
  };

  const pct = (used: number, limit: number) => (limit > 0 ? Math.round((used / limit) * 100) : 0);
  const quotaPct = pct(quota.vms.used, quota.vms.limit);

  if (loading) {
    return (
      <div className="max-w-full px-3 py-8 flex items-center justify-center min-h-[200px]">
        <Loader2 className="h-8 w-8 animate-spin text-gray-400" />
      </div>
    );
  }

  return (
    <div className="max-w-full px-3 py-8">
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="mb-8"
      >
        <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100">Tenant Dashboard</h1>
        <p className="text-gray-600 dark:text-gray-400 mt-2">Overview of resource consumption, limits, and notifications</p>
      </motion.div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.1 }}
        className="grid gap-6 mb-8 grid-cols-1 sm:grid-cols-2 lg:grid-cols-4"
      >
        <Card className="hover:shadow-lg dark:border-gray-700 transition-shadow">
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary-50 dark:bg-primary-900/30">
                  <Database className="h-6 w-6 text-primary-600 dark:text-primary-400" />
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">VMs</p>
                  <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">
                    {quota.vms.used} {quota.vms.limit > 0 && `/ ${quota.vms.limit}`}
                  </p>
                </div>
              </div>
              {quota.vms.limit > 0 && (
                <Badge variant={quotaPct >= 90 ? 'warning' : quotaPct >= 100 ? 'destructive' : 'default'}>
                  {quotaPct}%
                </Badge>
              )}
            </div>
          </CardContent>
        </Card>
        <Card className="hover:shadow-lg dark:border-gray-700 transition-shadow">
          <CardContent className="p-6">
            <div className="flex items-center gap-3">
              <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-nexus-50 dark:bg-nexus-900/30">
                <Cpu className="h-6 w-6 text-nexus-600 dark:text-nexus-400" />
              </div>
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">vCPUs</p>
                <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">
                  {quota.vcpus.used} {quota.vcpus.limit > 0 && `/ ${quota.vcpus.limit}`}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
        <Card className="hover:shadow-lg dark:border-gray-700 transition-shadow">
          <CardContent className="p-6">
            <div className="flex items-center gap-3">
              <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-success-50 dark:bg-success-900/30">
                <Activity className="h-6 w-6 text-success-600 dark:text-success-400" />
              </div>
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Memory (GB)</p>
                <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">
                  {quota.memoryGb.used} {quota.memoryGb.limit > 0 && `/ ${quota.memoryGb.limit}`}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
        <Card className="hover:shadow-lg dark:border-gray-700 transition-shadow">
          <CardContent className="p-6">
            <div className="flex items-center gap-3">
              <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-warning-50 dark:bg-warning-900/30">
                <HardDrive className="h-6 w-6 text-warning-600 dark:text-warning-400" />
              </div>
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Storage (GB)</p>
                <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">
                  {quota.storageGb.used} {quota.storageGb.limit > 0 && `/ ${quota.storageGb.limit}`}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      </motion.div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.2 }}
      >
        <AlertsCard alerts={alerts} onDismiss={handleDismissAlert} onAction={() => {}} />
      </motion.div>
    </div>
  );
}

'use client';

import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader } from '@/components/ui/atoms/card';
import { motion } from 'framer-motion';
import { SlidersHorizontal, Cpu, Database, HardDrive, Activity, Loader2 } from 'lucide-react';
import { apiGet } from '@/lib/api';

interface Quotas {
  vms?: number;
  vcpus?: number;
  memory?: number;
  storage?: number;
}

export default function TenantAdministrationPage() {
  const [quotas, setQuotas] = useState<Quotas>({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const data = await apiGet<{ settings?: { quotas?: Quotas } }>('/api/v1/tenants/current');
        setQuotas(data?.settings?.quotas ?? {});
      } catch {
        setQuotas({});
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

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
        <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100">Administration</h1>
        <p className="text-gray-600 dark:text-gray-400 mt-2">Quotas and organization-level settings</p>
      </motion.div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.1 }}
      >
        <Card className="dark:border-gray-700">
          <CardHeader>
            <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">Quotas</h2>
            <p className="text-sm text-gray-500 dark:text-gray-400">Current limits for your tenant (read-only). Contact your administrator to request changes.</p>
          </CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div className="flex items-center gap-3 p-4 rounded-lg bg-gray-50 dark:bg-gray-800">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary-50 dark:bg-primary-900/30">
                <Database className="h-5 w-5 text-primary-600 dark:text-primary-400" />
              </div>
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Max VMs</p>
                <p className="text-xl font-bold text-gray-900 dark:text-gray-100">{quotas.vms ?? '—'}</p>
              </div>
            </div>
            <div className="flex items-center gap-3 p-4 rounded-lg bg-gray-50 dark:bg-gray-800">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-nexus-50 dark:bg-nexus-900/30">
                <Cpu className="h-5 w-5 text-nexus-600 dark:text-nexus-400" />
              </div>
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Max vCPUs</p>
                <p className="text-xl font-bold text-gray-900 dark:text-gray-100">{quotas.vcpus ?? '—'}</p>
              </div>
            </div>
            <div className="flex items-center gap-3 p-4 rounded-lg bg-gray-50 dark:bg-gray-800">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-success-50 dark:bg-success-900/30">
                <Activity className="h-5 w-5 text-success-600 dark:text-success-400" />
              </div>
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Max Memory (GB)</p>
                <p className="text-xl font-bold text-gray-900 dark:text-gray-100">{quotas.memory ?? '—'}</p>
              </div>
            </div>
            <div className="flex items-center gap-3 p-4 rounded-lg bg-gray-50 dark:bg-gray-800">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-warning-50 dark:bg-warning-900/30">
                <HardDrive className="h-5 w-5 text-warning-600 dark:text-warning-400" />
              </div>
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Max Storage (GB)</p>
                <p className="text-xl font-bold text-gray-900 dark:text-gray-100">{quotas.storage ?? '—'}</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </motion.div>
    </div>
  );
}

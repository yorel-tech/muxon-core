'use client';

import { useState, useEffect } from 'react';
import { Card, CardContent } from '@/components/ui/atoms/card';
import { Table, Column } from '@/components/ui/organisms/table';
import { Badge } from '@/components/ui/atoms/badge';
import { motion } from 'framer-motion';
import { Server, Loader2 } from 'lucide-react';
import { apiGet } from '@/lib/api';

interface DatacenterRow {
  id: string;
  name: string;
  description?: string;
  region?: string;
  status?: string;
}

export default function TenantDatacentersPage() {
  const [datacenters, setDatacenters] = useState<DatacenterRow[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const data = await apiGet<{ items?: DatacenterRow[] }>('/api/v1/datacenters');
        const list = Array.isArray(data) ? data : data?.items ?? [];
        setDatacenters(
          list.map((dc: Record<string, unknown>) => ({
            id: String(dc.id ?? ''),
            name: String(dc.name ?? dc.id ?? ''),
            description: dc.description as string | undefined,
            region: (dc.metadata as Record<string, string>)?.region,
            status: 'available',
          }))
        );
      } catch {
        setDatacenters([]);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const columns: Column<DatacenterRow>[] = [
    {
      key: 'name',
      header: 'Name',
      cell: (row) => <div className="font-medium text-gray-900 dark:text-gray-100">{row.name}</div>,
      sortable: true,
    },
    {
      key: 'description',
      header: 'Description',
      cell: (row) => <span className="text-gray-600 dark:text-gray-400 text-sm">{row.description ?? '—'}</span>,
      sortable: true,
    },
    {
      key: 'region',
      header: 'Region',
      cell: (row) => <span className="text-gray-600 dark:text-gray-400 text-sm">{row.region ?? '—'}</span>,
      sortable: true,
    },
    {
      key: 'status',
      header: 'Status',
      cell: (row) => (
        <Badge variant="success">{row.status ?? 'available'}</Badge>
      ),
      sortable: true,
    },
  ];

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="max-w-full px-3 py-8">
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="mb-8"
        >
          <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100">Datacenters</h1>
          <p className="text-gray-600 dark:text-gray-400 mt-2">Read-only view of datacenters available to your tenant</p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.1 }}
        >
          <Card className="dark:border-gray-700">
            <CardContent className="p-0">
              {loading ? (
                <div className="flex items-center justify-center py-12">
                  <Loader2 className="h-8 w-8 animate-spin text-gray-400" />
                </div>
              ) : (
                <Table
                  columns={columns}
                  data={datacenters}
                  emptyMessage="No datacenters available"
                  overflowVisibleColumnKeys={[]}
                />
              )}
            </CardContent>
          </Card>
        </motion.div>
      </div>
    </div>
  );
}

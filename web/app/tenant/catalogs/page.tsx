'use client';

import { useState, useEffect } from 'react';
import { Card, CardContent } from '@/components/ui/atoms/card';
import { Table, Column } from '@/components/ui/organisms/table';
import { motion } from 'framer-motion';
import { BookMarked, Loader2 } from 'lucide-react';
import { apiGet } from '@/lib/api';

interface CatalogRow {
  id: string;
  name: string;
  description?: string;
  type?: string;
}

export default function TenantCatalogsPage() {
  const [catalogs, setCatalogs] = useState<CatalogRow[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const data = await apiGet<{ items?: CatalogRow[] }>('/api/v1/catalogs');
        const list = Array.isArray(data) ? data : data?.items ?? [];
        setCatalogs(
          list.map((c: Record<string, unknown>) => ({
            id: String(c.id ?? ''),
            name: String(c.name ?? c.id ?? ''),
            description: c.description as string | undefined,
            type: c.type as string | undefined,
          }))
        );
      } catch {
        setCatalogs([]);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const columns: Column<CatalogRow>[] = [
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
      key: 'type',
      header: 'Type',
      cell: (row) => <span className="text-gray-600 dark:text-gray-400 text-sm">{row.type ?? '—'}</span>,
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
          <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100">Catalogs</h1>
          <p className="text-gray-600 dark:text-gray-400 mt-2">Available catalogs and templates</p>
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
                  data={catalogs}
                  emptyMessage="No catalogs available"
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

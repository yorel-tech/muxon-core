'use client';

import { useState, useEffect } from 'react';
import { Card, CardContent } from '@/components/ui/atoms/card';
import { Table, Column } from '@/components/ui/organisms/table';
import { Badge } from '@/components/ui/atoms/badge';
import { motion } from 'framer-motion';
import { Shield, Loader2 } from 'lucide-react';
import { apiGet } from '@/lib/api';

interface RoleRow {
  id: string;
  name: string;
  description?: string;
  permissions?: string[];
}

export default function TenantRolesPage() {
  const [roles, setRoles] = useState<RoleRow[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const data = await apiGet<{ items?: RoleRow[] }>('/api/v1/roles');
        const list = Array.isArray(data) ? data : data?.items ?? [];
        setRoles(
          list.map((r: Record<string, unknown>) => ({
            id: String(r.id ?? r.name ?? ''),
            name: String(r.name ?? r.id ?? ''),
            description: r.description as string | undefined,
            permissions: (r.permissions as string[]) ?? [],
          }))
        );
      } catch {
        setRoles([]);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const columns: Column<RoleRow>[] = [
    {
      key: 'name',
      header: 'Role',
      cell: (row) => (
        <div className="font-medium text-gray-900 dark:text-gray-100">{row.name}</div>
      ),
      sortable: true,
    },
    {
      key: 'description',
      header: 'Description',
      cell: (row) => <span className="text-gray-600 dark:text-gray-400 text-sm">{row.description ?? '—'}</span>,
      sortable: true,
    },
    {
      key: 'permissions',
      header: 'Permissions',
      cell: (row) => (
        <div className="flex flex-wrap gap-1">
          {(row.permissions ?? []).length === 0 ? (
            <span className="text-gray-400 text-sm">—</span>
          ) : (
            (row.permissions ?? []).slice(0, 5).map((p) => (
              <Badge key={p} variant="default" className="text-xs">
                {p}
              </Badge>
            ))
          )}
          {(row.permissions ?? []).length > 5 && (
            <Badge variant="secondary">+{(row.permissions ?? []).length - 5} more</Badge>
          )}
        </div>
      ),
      sortable: false,
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
          <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100">Roles</h1>
          <p className="text-gray-600 dark:text-gray-400 mt-2">Tenant roles and permissions</p>
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
                  data={roles}
                  emptyMessage="No roles defined"
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

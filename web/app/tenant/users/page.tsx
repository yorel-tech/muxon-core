'use client';

import { useState, useEffect } from 'react';
import { Card, CardContent } from '@/components/ui/atoms/card';
import { Table, Column } from '@/components/ui/organisms/table';
import { Badge } from '@/components/ui/atoms/badge';
import { motion } from 'framer-motion';
import { Loader2 } from 'lucide-react';
import { apiGet } from '@/lib/api';

interface TenantUserRow {
  user_id: string;
  username?: string;
  email?: string;
  display_name?: string;
  role_name?: string;
  scope_type?: string;
  expires_at?: string | null;
}

export default function TenantUsersPage() {
  const [users, setUsers] = useState<TenantUserRow[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const current = await apiGet<{ id?: string }>('/api/v1/tenants/current').catch(() => ({}));
        const tenantId = (current as { id?: string })?.id;
        if (tenantId) {
          const data = await apiGet<{ items?: TenantUserRow[] }>(`/api/v1/tenants/${tenantId}/users`);
          const list = Array.isArray(data) ? data : data?.items ?? [];
          setUsers(list);
        } else {
          setUsers([]);
        }
      } catch {
        setUsers([]);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const columns: Column<TenantUserRow>[] = [
    {
      key: 'display_name',
      header: 'Name',
      cell: (row) => <div className="font-medium text-gray-900 dark:text-gray-100">{row.display_name ?? row.username ?? row.user_id}</div>,
      sortable: true,
    },
    {
      key: 'email',
      header: 'Email',
      cell: (row) => <span className="text-gray-600 dark:text-gray-400 text-sm">{row.email ?? '—'}</span>,
      sortable: true,
    },
    {
      key: 'role_name',
      header: 'Role',
      cell: (row) => <Badge variant="default">{row.role_name ?? '—'}</Badge>,
      sortable: true,
    },
    {
      key: 'scope_type',
      header: 'Scope',
      cell: (row) => <span className="text-gray-500 dark:text-gray-400 text-sm">{row.scope_type ?? '—'}</span>,
      sortable: true,
    },
    {
      key: 'expires_at',
      header: 'Expires',
      cell: (row) => (
        <span className="text-gray-500 dark:text-gray-400 text-sm">
          {row.expires_at ? new Date(row.expires_at).toLocaleDateString() : '—'}
        </span>
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
          <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100">Users</h1>
          <p className="text-gray-600 dark:text-gray-400 mt-2">Tenant users and their roles</p>
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
                  data={users}
                  emptyMessage="No users in this tenant"
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

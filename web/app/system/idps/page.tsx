'use client';

import { Card, CardContent, CardHeader } from '@/components/ui/atoms/card';
import { Table, Column } from '@/components/ui/organisms/table';
import { Badge } from '@/components/ui/atoms/badge';
import { motion } from 'framer-motion';
import {
  Shield,
  Plus,
  Edit,
  Trash2,
  MoreHorizontal,
} from 'lucide-react';
import { useState } from 'react';

interface IdpServer {
  id: string;
  name: string;
  protocol?: string;
  enabled: boolean;
  isSystem: boolean;
}

interface IdpUser {
  sub: string;
  preferredUsername: string;
  email: string;
  name?: string;
}

// Mock data
const mockIdpServers: IdpServer[] = [
  {
    id: '1',
    name: 'Keycloak',
    protocol: 'OIDC',
    enabled: true,
    isSystem: true,
  },
  {
    id: '2',
    name: 'Azure AD',
    protocol: 'OIDC',
    enabled: false,
    isSystem: false,
  },
];

const mockUsers: IdpUser[] = [
  {
    sub: 'user-1',
    preferredUsername: 'alice',
    email: 'alice@example.com',
    name: 'Alice Johnson',
  },
  {
    sub: 'user-2',
    preferredUsername: 'bob',
    email: 'bob@example.com',
    name: 'Bob Smith',
  },
  {
    sub: 'user-3',
    preferredUsername: 'charlie',
    email: 'charlie@example.com',
  },
];

const idpColumns: Column<IdpServer>[] = [
  {
    key: 'name',
    header: 'Name',
    cell: (row: IdpServer) => (
      <div className="flex items-center gap-2">
        <span className="font-medium text-gray-900 dark:text-gray-100">{row.name}</span>
      </div>
    ),
    sortable: true,
  },
  {
    key: 'protocol',
    header: 'Protocol',
    cell: (row: IdpServer) => (
      <Badge variant="default">{row.protocol || 'N/A'}</Badge>
    ),
    sortable: true,
  },
  {
    key: 'enabled',
    header: 'Status',
    cell: (row: IdpServer) => (
      <Badge
        variant={row.enabled ? 'success' : 'default'}
      >
        {row.enabled ? 'Enabled' : 'Disabled'}
      </Badge>
    ),
    sortable: true,
  },
  {
    key: 'actions',
    header: 'Actions',
    cell: () => (
      <div className="flex items-center gap-2">
        <button className="p-1.5 rounded hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors">
          <Edit className="h-4 w-4 text-gray-600 dark:text-gray-400" />
        </button>
        <button className="p-1.5 rounded hover:bg-red-50 dark:hover:bg-red-900/30 transition-colors">
          <Trash2 className="h-4 w-4 text-error-600 dark:text-error-400" />
        </button>
      </div>
    ),
  },
];

export default function IdpsPage() {
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
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
                  Identity Providers
                </h1>
                <p className="text-gray-600 dark:text-gray-400 mt-2">
                  Manage your identity providers for user authentication
                </p>
              </div>
              <button className="p-2 rounded-lg bg-primary-600 hover:bg-primary-700 text-white transition-colors">
                <Plus className="h-5 w-5" />
                <span className="ml-2">Add Provider</span>
              </button>
            </div>
          </motion.div>

          {/* IDP Servers Table */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.1 }}
          >
            <Card>
              <CardHeader>
                <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">
                  Identity Provider Servers
                </h2>
              </CardHeader>
              <CardContent className="p-0">
                <Table
                  columns={idpColumns}
                  data={mockIdpServers}
                  emptyMessage="No IDP servers configured"
                />
              </CardContent>
            </Card>
          </motion.div>

          {/* Users Table */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.2 }}
          >
            <Card>
              <CardHeader>
                <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">
                  IDP Users
                </h2>
              </CardHeader>
              <CardContent className="p-0">
                <Table
                  columns={idpColumns}
                  data={mockUsers}
                  emptyMessage="No users found"
                />
              </CardContent>
            </Card>
          </motion.div>
        </div>
  );
}

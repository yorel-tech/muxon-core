'use client';

import { Card, CardContent, CardHeader } from '@/components/ui/atoms/card';
import { Table, Column } from '@/components/ui/organisms/table';
import { Badge } from '@/components/ui/atoms/badge';
import { motion } from 'framer-motion';
import {
  Users,
  Plus,
  Edit,
  Trash2,
  MoreHorizontal,
  Shield,
  UserPlus,
} from 'lucide-react';
import { useState } from 'react';

interface SystemUser {
  id: string;
  name: string;
  email: string;
  role: string;
  status: 'active' | 'inactive';
  createdAt: string;
}

// Mock data
const mockUsers: SystemUser[] = [
  {
    id: '1',
    name: 'Alice Johnson',
    email: 'alice@example.com',
    role: 'System Admin',
    status: 'active',
    createdAt: '2024-01-15',
  },
  {
    id: '2',
    name: 'Bob Smith',
    email: 'bob@example.com',
    role: 'System Admin',
    status: 'active',
    createdAt: '2024-01-10',
  },
  {
    id: '3',
    name: 'Charlie Brown',
    email: 'charlie@example.com',
    role: 'System User',
    status: 'inactive',
    createdAt: '2024-01-05',
  },
  {
    id: '4',
    name: 'Diana Prince',
    email: 'diana@example.com',
    role: 'System User',
    status: 'active',
    createdAt: '2024-01-03',
  },
];

const userColumns: Column<SystemUser>[] = [
  {
    key: 'name',
    header: 'Name',
    cell: (row: SystemUser) => (
      <div className="flex items-center gap-2">
        <span className="font-medium text-gray-900 dark:text-gray-100">{row.name}</span>
      </div>
    ),
    sortable: true,
  },
  {
    key: 'email',
    header: 'Email',
    cell: (row: SystemUser) => (
      <span className="text-sm text-gray-600 dark:text-gray-400">{row.email}</span>
    ),
    sortable: true,
  },
  {
    key: 'role',
    header: 'Role',
    cell: (row: SystemUser) => (
      <Badge variant="default">{row.role}</Badge>
    ),
    sortable: true,
  },
  {
    key: 'status',
    header: 'Status',
    cell: (row: SystemUser) => (
      <Badge
        variant={row.status === 'active' ? 'success' : 'default'}
      >
        {row.status}
      </Badge>
    ),
    sortable: true,
  },
  {
    key: 'createdAt',
    header: 'Created',
    cell: (row: SystemUser) => (
      <span className="text-sm text-gray-600 dark:text-gray-400">{row.createdAt}</span>
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

export default function UsersPage() {
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
                  System Users
                </h1>
                <p className="text-gray-600 dark:text-gray-400 mt-2">
                  Manage system-level users and their permissions
                </p>
              </div>
              <button className="p-2 rounded-lg bg-primary-600 hover:bg-primary-700 text-white transition-colors">
                <UserPlus className="h-5 w-5" />
                <span className="ml-2">Add User</span>
              </button>
            </div>
          </motion.div>

          {/* Users Table */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.1 }}
          >
            <Card>
              <CardHeader>
                <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">
                  System Users
                </h2>
              </CardHeader>
              <CardContent className="p-0">
                <Table
                  columns={userColumns}
                  data={mockUsers}
                  emptyMessage="No users found"
                />
              </CardContent>
            </Card>
          </motion.div>
        </div>
  );
}

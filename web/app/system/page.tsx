'use client';

import { Card, CardContent, CardHeader } from '@/components/ui/atoms/card';
import { Button } from '@/components/ui/atoms/button';
import { motion } from 'framer-motion';
import { 
  Server, 
  Database, 
  Shield, 
  Key, 
  Users, 
  Building2,
  ChevronRight,
  CheckCircle2,
  AlertCircle,
  Clock
} from 'lucide-react';

export default function SystemDashboardPage() {
  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="mb-8"
        >
          <h1 className="text-3xl font-bold text-gray-900">
            Welcome to Infron
          </h1>
          <p className="text-gray-600 mt-2">
            Get started by configuring your cloud infrastructure
          </p>
        </motion.div>

        {/* Setup Progress */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.1 }}
          className="mb-8"
        >
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <h2 className="text-xl font-semibold text-gray-900">
                  Setup Progress
                </h2>
                <span className="text-sm text-gray-500">
                  0 of 5 completed
                </span>
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="flex items-center gap-4">
                  <div className="flex h-8 w-8 items-center justify-center rounded-full border-2 border-gray-200 bg-gray-100">
                    <span className="text-sm font-medium text-gray-500">1</span>
                  </div>
                  <div className="flex-1">
                    <p className="font-medium text-gray-900">Configure Identity Provider</p>
                    <p className="text-sm text-gray-500">Set up Keycloak or other OIDC provider</p>
                  </div>
                  <ChevronRight className="h-5 w-5 text-gray-400" />
                </div>
                <div className="flex items-center gap-4">
                  <div className="flex h-8 w-8 items-center justify-center rounded-full border-2 border-gray-200 bg-gray-100">
                    <span className="text-sm font-medium text-gray-500">2</span>
                  </div>
                  <div className="flex-1">
                    <p className="font-medium text-gray-900">Add Datacenter</p>
                    <p className="text-sm text-gray-500">Connect your first datacenter or cloud provider</p>
                  </div>
                  <ChevronRight className="h-5 w-5 text-gray-400" />
                </div>
                <div className="flex items-center gap-4">
                  <div className="flex h-8 w-8 items-center justify-center rounded-full border-2 border-gray-200 bg-gray-100">
                    <span className="text-sm font-medium text-gray-500">3</span>
                  </div>
                  <div className="flex-1">
                    <p className="font-medium text-gray-900">Create First User</p>
                    <p className="text-sm text-gray-500">Add a system user account</p>
                  </div>
                  <ChevronRight className="h-5 w-5 text-gray-400" />
                </div>
                <div className="flex items-center gap-4">
                  <div className="flex h-8 w-8 items-center justify-center rounded-full border-2 border-gray-200 bg-gray-100">
                    <span className="text-sm font-medium text-gray-500">4</span>
                  </div>
                  <div className="flex-1">
                    <p className="font-medium text-gray-900">Configure Network</p>
                    <p className="text-sm text-gray-500">Set up networking for your infrastructure</p>
                  </div>
                  <ChevronRight className="h-5 w-5 text-gray-400" />
                </div>
                <div className="flex items-center gap-4">
                  <div className="flex h-8 w-8 items-center justify-center rounded-full border-2 border-gray-200 bg-gray-100">
                    <span className="text-sm font-medium text-gray-500">5</span>
                  </div>
                  <div className="flex-1">
                    <p className="font-medium text-gray-900">Create Tenant</p>
                    <p className="text-sm text-gray-500">Add your first tenant organization</p>
                  </div>
                  <ChevronRight className="h-5 w-5 text-gray-400" />
                </div>
              </div>
            </CardContent>
          </Card>
        </motion.div>

        {/* Quick Actions */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.2 }}
          className="mb-8"
        >
          <Card>
            <CardHeader>
              <h2 className="text-xl font-semibold text-gray-900">
                Quick Actions
              </h2>
            </CardHeader>
            <CardContent>
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                <Button
                  variant="secondary"
                  className="flex h-full flex-col items-center justify-center gap-3 p-6"
                  leftIcon={<Key className="h-6 w-6" />}
                >
                  <span className="font-medium">Configure IDP</span>
                  <span className="text-sm text-gray-500">Set up authentication</span>
                </Button>
                <Button
                  variant="secondary"
                  className="flex h-full flex-col items-center justify-center gap-3 p-6"
                  leftIcon={<Database className="h-6 w-6" />}
                >
                  <span className="font-medium">Add Datacenter</span>
                  <span className="text-sm text-gray-500">Connect infrastructure</span>
                </Button>
                <Button
                  variant="secondary"
                  className="flex h-full flex-col items-center justify-center gap-3 p-6"
                  leftIcon={<Users className="h-6 w-6" />}
                >
                  <span className="font-medium">Create User</span>
                  <span className="text-sm text-gray-500">Add system user</span>
                </Button>
                <Button
                  variant="secondary"
                  className="flex h-full flex-col items-center justify-center gap-3 p-6"
                  leftIcon={<Building2 className="h-6 w-6" />}
                >
                  <span className="font-medium">Create Tenant</span>
                  <span className="text-sm text-gray-500">Add organization</span>
                </Button>
              </div>
            </CardContent>
          </Card>
        </motion.div>

        {/* Getting Started Guide */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.3 }}
        >
          <Card>
            <CardHeader>
              <div className="flex items-center gap-2">
                <Clock className="h-5 w-5 text-primary-600" />
                <h2 className="text-xl font-semibold text-gray-900">
                  Getting Started
                </h2>
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-6">
                <div className="flex gap-4">
                  <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-lg bg-blue-50">
                    <Key className="h-5 w-5 text-primary-600" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-1">1. Configure Identity Provider</h3>
                    <p className="text-gray-600 text-sm">
                      Set up Keycloak or another OIDC provider to enable user authentication. This is required before you can create users or tenants.
                    </p>
                  </div>
                </div>
                <div className="flex gap-4">
                  <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-lg bg-green-50">
                    <Database className="h-5 w-5 text-green-600" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-1">2. Add Datacenter</h3>
                    <p className="text-gray-600 text-sm">
                      Connect your first datacenter or cloud provider (Proxmox, Libvirt) to manage your infrastructure resources.
                    </p>
                  </div>
                </div>
                <div className="flex gap-4">
                  <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-lg bg-purple-50">
                    <Users className="h-5 w-5 text-purple-600" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-1">3. Create System Users</h3>
                    <p className="text-gray-600 text-sm">
                      Add system user accounts with appropriate roles to manage your Infron installation.
                    </p>
                  </div>
                </div>
                <div className="flex gap-4">
                  <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-lg bg-orange-50">
                    <Shield className="h-5 w-5 text-orange-600" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-1">4. Configure Network</h3>
                    <p className="text-gray-600 text-sm">
                      Set up networking for your infrastructure to enable communication between resources.
                    </p>
                  </div>
                </div>
                <div className="flex gap-4">
                  <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-lg bg-pink-50">
                    <Building2 className="h-5 w-5 text-pink-600" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-1">5. Create Tenant</h3>
                    <p className="text-gray-600 text-sm">
                      Create your first tenant organization and assign users to it.
                    </p>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </motion.div>

        {/* Resources */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.4 }}
        >
          <Card>
            <CardHeader>
              <h2 className="text-xl font-semibold text-gray-900">
                Resources
              </h2>
            </CardHeader>
            <CardContent>
              <div className="grid gap-4 sm:grid-cols-1 lg:grid-cols-2">
                <a
                  href="https://docs.infron.io"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="group block rounded-lg border border-gray-200 p-6 hover:border-primary-300 hover:bg-gray-50 transition-colors"
                >
                  <div className="flex items-start gap-4">
                    <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-lg bg-gray-100 group-hover:bg-primary-100 transition-colors">
                      <Server className="h-5 w-5 text-gray-600 group-hover:text-primary-600" />
                    </div>
                    <div>
                      <h3 className="font-semibold text-gray-900 group-hover:text-primary-600 mb-1">
                        Documentation
                      </h3>
                      <p className="text-sm text-gray-500">
                        Complete guides on setting up and using Infron
                      </p>
                    </div>
                  </div>
                </a>
                <a
                  href="https://github.com/onetattva/infron"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="group block rounded-lg border border-gray-200 p-6 hover:border-primary-300 hover:bg-gray-50 transition-colors"
                >
                  <div className="flex items-start gap-4">
                    <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-lg bg-gray-100 group-hover:bg-primary-100 transition-colors">
                      <CheckCircle2 className="h-5 w-5 text-gray-600 group-hover:text-primary-600" />
                    </div>
                    <div>
                      <h3 className="font-semibold text-gray-900 group-hover:text-primary-600 mb-1">
                        Community Support
                      </h3>
                      <p className="text-sm text-gray-500">
                        Get help from the Infron community
                      </p>
                    </div>
                  </div>
                </a>
              </div>
            </CardContent>
          </Card>
        </motion.div>
      </div>
    </div>
  );
}

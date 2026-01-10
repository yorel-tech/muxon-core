'use client';

import { Card, CardContent, CardHeader } from '@/components/ui/atoms/card';
import { Button } from '@/components/ui/atoms/button';
import { Input } from '@/components/ui/atoms/input';
import { motion, AnimatePresence } from 'framer-motion';
import { useState, useEffect } from 'react';
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
  Clock,
  X,
  ArrowRight,
  Loader2,
  Cloud
} from 'lucide-react';

interface SetupStep {
  id: string;
  title: string;
  description: string;
  icon: React.ReactNode;
  completed: boolean;
}

export default function SystemDashboardPage() {
  const [setupSteps, setSetupSteps] = useState<SetupStep[]>([
    {
      id: 'idp',
      title: 'Configure Identity Provider',
      description: 'Set up Keycloak or other OIDC provider to enable user authentication. This is required before you can create users or tenants.',
      icon: <Key className="h-6 w-6" />,
      completed: false,
    },
    {
      id: 'system-users',
      title: 'Create System Users',
      description: 'Add system user accounts with appropriate roles to manage your Infron installation.',
      icon: <Users className="h-6 w-6" />,
      completed: false,
    },
    {
      id: 'provider',
      title: 'Add Provider',
      description: 'Add a provider (Proxmox, Libvirt, etc.) which is required before creating datacenters.',
      icon: <Server className="h-6 w-6" />,
      completed: false,
    },
    {
      id: 'datacenter',
      title: 'Add Datacenter',
      description: 'Connect your first datacenter or cloud provider (Proxmox, Libvirt) to manage your infrastructure resources.',
      icon: <Database className="h-6 w-6" />,
      completed: false,
    },
    {
      id: 'tenant',
      title: 'Create Tenant',
      description: 'Create your first tenant organization and assign users to it.',
      icon: <Building2 className="h-6 w-6" />,
      completed: false,
    },
  ]);

  const [activeWizard, setActiveWizard] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  // Check configuration status on mount
  useEffect(() => {
    checkConfigurationStatus();
  }, []);

  const checkConfigurationStatus = async () => {
    try {
      // Check IDP configuration - using a health check approach since IDP API doesn't exist yet
      const idpResponse = await fetch('/api/v1/healthz');
      const idpConfigured = idpResponse.ok;
      setSetupSteps(prev => prev.map(step =>
        step.id === 'idp' ? { ...step, completed: idpConfigured } : step
      ));

      // Check system users - using role bindings as proxy since users API doesn't exist
      const bindingsResponse = await fetch('/api/v1/role-bindings');
      if (bindingsResponse.ok) {
        const bindingsData = await bindingsResponse.json();
        const hasSystemUsers = bindingsData && bindingsData.items && bindingsData.items.length > 0;
        setSetupSteps(prev => prev.map(step =>
          step.id === 'system-users' ? { ...step, completed: hasSystemUsers } : step
        ));
      }

      // Check providers - API doesn't exist yet, skip for now
      // TODO: Implement provider API endpoint

      // Check datacenters
      const datacentersResponse = await fetch('/api/v1/datacenters');
      if (datacentersResponse.ok) {
        const datacentersData = await datacentersResponse.json();
        const hasDatacenters = datacentersData && datacentersData.items && datacentersData.items.length > 0;
        setSetupSteps(prev => prev.map(step =>
          step.id === 'datacenter' ? { ...step, completed: hasDatacenters } : step
        ));
      }

      // Check tenants
      const tenantsResponse = await fetch('/api/v1/tenants');
      if (tenantsResponse.ok) {
        const tenantsData = await tenantsResponse.json();
        const hasTenants = tenantsData && tenantsData.items && tenantsData.items.length > 0;
        setSetupSteps(prev => prev.map(step =>
          step.id === 'tenant' ? { ...step, completed: hasTenants } : step
        ));
      }
    } catch (error) {
      console.error('Error checking configuration status:', error);
    }
  };

  const completedCount = setupSteps.filter(step => step.completed).length;

  const handleStepClick = (stepId: string) => {
    setActiveWizard(stepId);
  };

  const handleWizardClose = () => {
    setActiveWizard(null);
  };

  const handleSkipToDashboard = () => {
    window.location.href = '/system/dashboard';
  };

  const renderWizard = () => {
    switch (activeWizard) {
      case 'idp':
        return (
          <div className="space-y-6">
            <div className="bg-gradient-to-br from-blue-50 to-indigo-50 rounded-lg p-6">
              <Key className="h-12 w-12 text-primary-600 mx-auto mb-4" />
              <h3 className="text-xl font-semibold text-center text-gray-900 mb-2">
                Configure Identity Provider
              </h3>
              <p className="text-center text-gray-600 text-sm">
                Set up Keycloak or another OIDC provider to enable user authentication.
              </p>
            </div>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Identity Provider Type
                </label>
                <select className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent">
                  <option>Keycloak</option>
                  <option>Auth0</option>
                  <option>Okta</option>
                  <option>Custom OIDC</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Issuer URL
                </label>
                <Input
                  type="url"
                  placeholder="https://your-idp.com/realms/infron"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Client ID
                </label>
                <Input
                  type="text"
                  placeholder="your-client-id"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Client Secret
                </label>
                <Input
                  type="password"
                  placeholder="•••••••••••"
                />
              </div>
            </div>
          </div>
        );

      case 'system-users':
        return (
          <div className="space-y-6">
            <div className="bg-gradient-to-br from-green-50 to-teal-50 rounded-lg p-6">
              <Users className="h-12 w-12 text-green-600 mx-auto mb-4" />
              <h3 className="text-xl font-semibold text-center text-gray-900 mb-2">
                Create System Users
              </h3>
              <p className="text-center text-gray-600 text-sm">
                Add system user accounts with appropriate roles to manage your Infron installation.
              </p>
            </div>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Username
                </label>
                <Input
                  type="text"
                  placeholder="admin"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Email
                </label>
                <Input
                  type="email"
                  placeholder="admin@example.com"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Display Name
                </label>
                <Input
                  type="text"
                  placeholder="System Administrator"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Role
                </label>
                <select className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent">
                  <option>System Administrator</option>
                  <option>System Operator</option>
                  <option>System Viewer</option>
                </select>
              </div>
            </div>
          </div>
        );

      case 'provider':
        return (
          <div className="space-y-6">
            <div className="bg-gradient-to-br from-purple-50 to-pink-50 rounded-lg p-6">
              <Server className="h-12 w-12 text-purple-600 mx-auto mb-4" />
              <h3 className="text-xl font-semibold text-center text-gray-900 mb-2">
                Add Provider
              </h3>
              <p className="text-center text-gray-600 text-sm">
                Add a provider (Proxmox, Libvirt, etc.) which is required before creating datacenters.
              </p>
            </div>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Provider Name
                </label>
                <Input
                  type="text"
                  placeholder="My Proxmox Provider"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Provider Type
                </label>
                <select className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent">
                  <option>Proxmox VE</option>
                  <option>Libvirt / KVM</option>
                  <option>VMware vSphere</option>
                  <option>OpenStack</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  API Endpoint
                </label>
                <Input
                  type="url"
                  placeholder="https://provider.example.com:8006"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Username
                </label>
                <Input
                  type="text"
                  placeholder="root@pam"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Password / API Token
                </label>
                <Input
                  type="password"
                  placeholder="•••••••••••"
                />
              </div>
            </div>
          </div>
        );

      case 'datacenter':
        return (
          <div className="space-y-6">
            <div className="bg-gradient-to-br from-orange-50 to-amber-50 rounded-lg p-6">
              <Database className="h-12 w-12 text-orange-600 mx-auto mb-4" />
              <h3 className="text-xl font-semibold text-center text-gray-900 mb-2">
                Add Datacenter
              </h3>
              <p className="text-center text-gray-600 text-sm">
                Connect your first datacenter to start managing your infrastructure resources.
              </p>
            </div>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Datacenter Name
                </label>
                <Input
                  type="text"
                  placeholder="Primary Datacenter"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Provider
                </label>
                <select className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent">
                  <option>Select a provider...</option>
                  <option>My Proxmox Provider</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Description (optional)
                </label>
                <Input
                  type="text"
                  placeholder="Main production datacenter"
                />
              </div>
            </div>
          </div>
        );

      case 'tenant':
        return (
          <div className="space-y-6">
            <div className="bg-gradient-to-br from-pink-50 to-rose-50 rounded-lg p-6">
              <Building2 className="h-12 w-12 text-pink-600 mx-auto mb-4" />
              <h3 className="text-xl font-semibold text-center text-gray-900 mb-2">
                Create Tenant
              </h3>
              <p className="text-center text-gray-600 text-sm">
                Create your first tenant organization and assign users to it.
              </p>
            </div>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Tenant Name
                </label>
                <Input
                  type="text"
                  placeholder="Acme Corp"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Description (optional)
                </label>
                <Input
                  type="text"
                  placeholder="Acme Corporation tenant"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Admin Email
                </label>
                <Input
                  type="email"
                  placeholder="admin@acme.com"
                />
              </div>
            </div>
          </div>
        );

      default:
        return null;
    }
  };

  const handleSave = async () => {
    setIsSaving(true);
    try {
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1500));
      
      // Mark step as completed
      setSetupSteps(prev => prev.map(step =>
        step.id === activeWizard ? { ...step, completed: true } : step
      ));
      
      setActiveWizard(null);
      // Refresh configuration status
      await checkConfigurationStatus();
    } catch (error) {
      console.error('Error saving configuration:', error);
    } finally {
      setIsSaving(false);
    }
  };

  const getIconColorClass = (step: SetupStep) => {
    if (step.completed) {
      return 'bg-green-100 text-green-600';
    }
    return 'bg-gray-100 text-gray-400';
  };

  const getTextColorClass = (step: SetupStep) => {
    if (step.completed) {
      return 'text-green-600';
    }
    return 'text-gray-400';
  };

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
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">
                Welcome to Infron
              </h1>
              <p className="text-gray-600 mt-2">
                Get started by configuring your cloud infrastructure
              </p>
            </div>
            <Button
              variant="secondary"
              onClick={handleSkipToDashboard}
              className="flex items-center gap-2"
            >
              Skip to Dashboard
              <ArrowRight className="h-4 w-4" />
            </Button>
          </div>
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
                <div className="flex items-center gap-2">
                  <Clock className="h-5 w-5 text-primary-600" />
                  <h2 className="text-xl font-semibold text-gray-900">
                    Setup Progress
                  </h2>
                </div>
                <span className="text-sm text-gray-500">
                  {completedCount} of {setupSteps.length} completed
                </span>
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                {setupSteps.map((step, index) => (
                  <motion.button
                    key={step.id}
                    onClick={() => handleStepClick(step.id)}
                    disabled={step.completed}
                    className={`w-full flex items-center gap-4 p-4 rounded-lg border transition-all text-left ${
                      step.completed
                        ? 'border-green-200 bg-green-50/50 cursor-default'
                        : 'border-gray-200 hover:border-primary-300 hover:bg-gray-50 cursor-pointer'
                    }`}
                    whileHover={!step.completed ? { scale: 1.01 } : {}}
                    whileTap={!step.completed ? { scale: 0.99 } : {}}
                  >
                    <div className={`flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-lg ${getIconColorClass(step)}`}>
                      {step.completed ? (
                        <CheckCircle2 className="h-5 w-5" />
                      ) : (
                        step.icon
                      )}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <h3 className={`font-semibold ${step.completed ? 'text-green-700' : 'text-gray-900'}`}>
                          {step.title}
                        </h3>
                        {step.completed && (
                          <span className="text-xs font-medium text-green-600 bg-green-100 px-2 py-0.5 rounded-full">
                            Complete
                          </span>
                        )}
                      </div>
                      <p className="text-sm text-gray-500 mt-0.5 line-clamp-1">
                        {step.description}
                      </p>
                    </div>
                    {!step.completed && (
                      <ChevronRight className={`h-5 w-5 ${getTextColorClass(step)}`} />
                    )}
                  </motion.button>
                ))}
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
                  onClick={() => handleStepClick('idp')}
                  disabled={setupSteps.find(s => s.id === 'idp')?.completed}
                  leftIcon={<Key className="h-6 w-6" />}
                >
                  <span className="font-medium">Configure IDP</span>
                  <span className="text-sm text-gray-500">Set up authentication</span>
                </Button>
                <Button
                  variant="secondary"
                  className="flex h-full flex-col items-center justify-center gap-3 p-6"
                  onClick={() => handleStepClick('provider')}
                  disabled={setupSteps.find(s => s.id === 'provider')?.completed}
                  leftIcon={<Server className="h-6 w-6" />}
                >
                  <span className="font-medium">Add Provider</span>
                  <span className="text-sm text-gray-500">Connect infrastructure</span>
                </Button>
                <Button
                  variant="secondary"
                  className="flex h-full flex-col items-center justify-center gap-3 p-6"
                  onClick={() => handleStepClick('system-users')}
                  disabled={setupSteps.find(s => s.id === 'system-users')?.completed}
                  leftIcon={<Users className="h-6 w-6" />}
                >
                  <span className="font-medium">Create Users</span>
                  <span className="text-sm text-gray-500">Add system user</span>
                </Button>
                <Button
                  variant="secondary"
                  className="flex h-full flex-col items-center justify-center gap-3 p-6"
                  onClick={() => handleStepClick('datacenter')}
                  disabled={setupSteps.find(s => s.id === 'datacenter')?.completed}
                  leftIcon={<Database className="h-6 w-6" />}
                >
                  <span className="font-medium">Add Datacenter</span>
                  <span className="text-sm text-gray-500">Connect resources</span>
                </Button>
              </div>
            </CardContent>
          </Card>
        </motion.div>

        {/* Resources */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.3 }}
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

      {/* Inline Wizard Modal */}
      <AnimatePresence>
        {activeWizard && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50"
            onClick={handleWizardClose}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              transition={{ duration: 0.2 }}
              className="bg-white rounded-xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-y-auto"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between">
                <h2 className="text-xl font-semibold text-gray-900">
                  {setupSteps.find(s => s.id === activeWizard)?.title}
                </h2>
                <button
                  onClick={handleWizardClose}
                  className="p-2 rounded-lg hover:bg-gray-100 transition-colors"
                >
                  <X className="h-5 w-5 text-gray-500" />
                </button>
              </div>
              <div className="p-6">
                {renderWizard()}
              </div>
              <div className="sticky bottom-0 bg-white border-t border-gray-200 px-6 py-4 flex justify-end gap-3">
                <Button
                  variant="secondary"
                  onClick={handleWizardClose}
                >
                  Cancel
                </Button>
                <Button
                  onClick={handleSave}
                  disabled={isSaving}
                >
                  {isSaving ? (
                    <>
                      <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                      Saving...
                    </>
                  ) : (
                    <>
                      Save & Continue
                      <ChevronRight className="h-4 w-4 ml-2" />
                    </>
                  )}
                </Button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

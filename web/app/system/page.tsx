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

interface BootstrapStatusDto {
  systemStatus: 'NOTREADY' | 'BOOTSTRAPPED' | 'READY';
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
  const [bootstrapStatus, setBootstrapStatus] = useState<BootstrapStatusDto | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Derived state from bootstrapStatus
  const isBootstrapped = bootstrapStatus?.systemStatus === 'BOOTSTRAPPED' || bootstrapStatus?.systemStatus === 'READY';
  const isReady = bootstrapStatus?.systemStatus === 'READY';

  // Message constants to avoid JSX parsing issues with curly braces
  const bootstrappedMessage = 'Your system has been pre-configured. Review and complete the remaining setup steps below.';
  const notBootstrappedMessage = 'Get started by configuring your cloud infrastructure';

  // Action text constants to avoid JSX parsing issues with curly braces
  const idpActionText = isBootstrapped ? 'Edit' : 'Set up';
  const providerActionText = isBootstrapped ? 'Add' : 'Connect';
  const systemUsersActionText = isBootstrapped ? 'Edit' : 'Create';

  // Check bootstrap status on mount
  useEffect(() => {
    fetchBootstrapStatus();
  }, []);

  const fetchBootstrapStatus = async () => {
    try {
      const response = await fetch('/api/v1/status');
      if (!response.ok) {
        throw new Error('Failed to fetch bootstrap status');
      }
      const status: BootstrapStatusDto = await response.json();
      setBootstrapStatus(status);
    } catch (error) {
      console.error('Error fetching bootstrap status:', error);
      setBootstrapStatus({ systemStatus: 'NOTREADY' });
    } finally {
      setIsLoading(false);
    }
  };

  const handleStepClick = (stepId: string) => {
    setActiveWizard(stepId);
  };

  const handleWizardClose = () => {
    setActiveWizard(null);
  };

  const handleSkipToDashboard = () => {
    window.location.href = '/dashboard/system';
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
      fetchBootstrapStatus();
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

  const renderWizard = () => {
    if (!activeWizard) return null;

    const step = setupSteps.find(s => s.id === activeWizard);
    if (!step) return null;

    switch (activeWizard) {
      case 'idp':
        return (
          <div className="space-y-6">
            <div className="bg-white rounded-lg border border-gray-200 p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-xl font-semibold text-gray-900">
                  {idpActionText} Identity Provider
                </h2>
                <Button
                  variant="secondary"
                  onClick={handleWizardClose}
                  className="text-gray-500 hover:text-gray-700"
                >
                  <X className="h-4 w-4" />
                </Button>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Provider Type
                </label>
                <select className="w-full px-4 py-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent">
                  <option value="keycloak">Keycloak</option>
                  <option value="azuread">Azure AD</option>
                  <option value="okta">Okta</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Issuer URL
                </label>
                <Input
                  type="text"
                  placeholder="https://keycloak.example.com/realms"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Client ID
                </label>
                <Input
                  type="text"
                  placeholder="infron-web"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Client Secret
                </label>
                <Input
                  type="password"
                  placeholder="••••••••••••••"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Realm
                </label>
                <Input
                  type="text"
                  placeholder="infron-dev"
                />
              </div>
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <Button onClick={handleWizardClose} variant="secondary">
                Cancel
              </Button>
              <Button onClick={handleSave} disabled={isSaving}>
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
          </div>
        );

      case 'system-users':
        return (
          <div className="space-y-6">
            <div className="bg-white rounded-lg border border-gray-200 p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-xl font-semibold text-gray-900">
                  {systemUsersActionText} System Users
                </h2>
                <Button
                  variant="secondary"
                  onClick={handleWizardClose}
                  className="text-gray-500 hover:text-gray-700"
                >
                  <X className="h-4 w-4" />
                </Button>
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
                    placeholder="admin@infron.com"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Password
                  </label>
                  <Input
                    type="password"
                    placeholder="••••••••••••••"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    First Name
                  </label>
                  <Input
                    type="text"
                    placeholder="System"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Last Name
                  </label>
                  <Input
                    type="text"
                    placeholder="Administrator"
                  />
                </div>
              </div>
              <div className="flex justify-end gap-3 mt-6">
                <Button onClick={handleWizardClose} variant="secondary">
                  Cancel
                </Button>
                <Button onClick={handleSave} disabled={isSaving}>
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
            </div>
          </div>
        );

      case 'provider':
        return (
          <div className="space-y-6">
            <div className="bg-white rounded-lg border border-gray-200 p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-xl font-semibold text-gray-900">
                  {providerActionText} Infrastructure Provider
                </h2>
                <Button
                  variant="secondary"
                  onClick={handleWizardClose}
                  className="text-gray-500 hover:text-gray-700"
                >
                  <X className="h-4 w-4" />
                </Button>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Provider Type
                </label>
                <select className="w-full px-4 py-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent">
                  <option value="proxmox">Proxmox</option>
                  <option value="libvirt">Libvirt</option>
                  <option value="vmware">VMware</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Hostname / IP
                </label>
                <Input
                  type="text"
                  placeholder="192.168.1.100"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Username
                </label>
                <Input
                  type="text"
                  placeholder="root"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Password
                </label>
                <Input
                  type="password"
                  placeholder="••••••••••••••"
                />
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
            <div className="flex justify-end gap-3 mt-6">
              <Button onClick={handleWizardClose} variant="secondary">
                Cancel
              </Button>
              <Button onClick={handleSave} disabled={isSaving}>
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
          </div>
        );

      case 'datacenter':
        return (
          <div className="space-y-6">
            <div className="bg-white rounded-lg border border-gray-200 p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-xl font-semibold text-gray-900">
                  {isBootstrapped ? 'Edit' : 'Add'} Datacenter
                </h2>
                <Button
                  variant="secondary"
                  onClick={handleWizardClose}
                  className="text-gray-500 hover:text-gray-700"
                >
                  <X className="h-4 w-4" />
                </Button>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Datacenter Type
                </label>
                <select className="w-full px-4 py-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent">
                  <option value="proxmox">Proxmox</option>
                  <option value="libvirt">Libvirt</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Hostname / IP
                </label>
                <Input
                  type="text"
                  placeholder="192.168.1.100"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Username
                </label>
                <Input
                  type="text"
                  placeholder="root"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Password
                </label>
                <Input
                  type="password"
                  placeholder="•••••••••••••"
                />
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
            <div className="flex justify-end gap-3 mt-6">
              <Button onClick={handleWizardClose} variant="secondary">
                Cancel
              </Button>
              <Button onClick={handleSave} disabled={isSaving}>
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
          </div>
        );

      case 'tenant':
        return (
          <div className="space-y-6">
            <div className="rounded-lg p-6" style={{ background: 'linear-gradient(to bottom right, #fdf2f8, #ffe4e6)' }}>
              <Building2 className="h-12 w-12 text-pink-600 mx-auto mb-4" />
              <h3 className="text-xl font-semibold text-center text-gray-900 mb-2">
                Create Tenant
              </h3>
              <p className="text-center text-gray-600 text-sm">
                Create your first tenant organization and assign users to it.
              </p>
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
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Admin Password
                  </label>
                  <Input
                    type="password"
                    placeholder="••••••••••••••"
                  />
                </div>
              </div>
              <div className="flex justify-end gap-3 mt-6">
                <Button onClick={handleWizardClose} variant="secondary">
                  Cancel
                </Button>
                <Button onClick={handleSave} disabled={isSaving}>
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
            </div>
          </div>
        );

      default:
        return null;
    }
  };

  const completedCount = setupSteps.filter((s) => s.completed).length;

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
                {isBootstrapped ? 'System Setup' : 'Welcome to Infron'}
              </h1>
              <p className="text-gray-600 mt-2">
                {isBootstrapped ? (
                  'Your system has been pre-configured. Review and complete the remaining setup steps below.'
                ) : (
                  'Get started by configuring your cloud infrastructure'
                )}
              </p>
            </div>
            <Button
              variant="secondary"
              onClick={handleSkipToDashboard}
              disabled={!isReady}
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
                <div>
                  <span className="text-sm text-gray-500">
                    {completedCount} of {setupSteps.length} completed
                  </span>
                </div>
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
                      <h3 className={`font-semibold ${getTextColorClass(step)}`}>
                        {step.title}
                      </h3>
                      {step.completed && (
                        <span className="text-xs font-medium bg-green-100 text-green-600 px-2 py-0.5 rounded-full">
                          Complete
                        </span>
                      )}
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
                {isBootstrapped ? 'Quick Actions' : 'Configure Identity Provider'}
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
                  <span className="font-medium">{idpActionText}</span>
                  <span className="text-sm text-gray-500">
                    {idpActionText} authentication
                  </span>
                </Button>
                <Button
                  variant="secondary"
                  className="flex h-full flex-col items-center justify-center gap-3 p-6"
                  onClick={() => handleStepClick('provider')}
                  disabled={!isBootstrapped}
                  leftIcon={<Server className="h-6 w-6" />}
                >
                  <span className="font-medium">{providerActionText}</span>
                  <span className="text-sm text-gray-500">
                    {providerActionText} infrastructure
                  </span>
                </Button>
                <Button
                  variant="secondary"
                  className="flex h-full flex-col items-center justify-center gap-3 p-6"
                  onClick={() => handleStepClick('system-users')}
                  disabled={!isBootstrapped}
                  leftIcon={<Users className="h-6 w-6" />}
                >
                  <span className="font-medium">{systemUsersActionText}</span>
                  <span className="text-sm text-gray-500">
                    {systemUsersActionText}
                  </span>
                </Button>
                <Button
                  variant="secondary"
                  className="flex h-full flex-col items-center justify-center gap-3 p-6"
                  onClick={() => handleStepClick('datacenter')}
                  disabled={!isBootstrapped}
                  leftIcon={<Database className="h-6 w-6" />}
                >
                  <span className="font-medium">Add Datacenter</span>
                  <span className="text-sm text-gray-500">
                    Connect resources
                  </span>
                </Button>
                <Button
                  variant="secondary"
                  className="flex h-full flex-col items-center justify-center gap-3 p-6"
                  onClick={() => handleStepClick('tenant')}
                  disabled={!isBootstrapped}
                  leftIcon={<Building2 className="h-6 w-6" />}
                >
                  <span className="font-medium">Create Tenant</span>
                  <span className="text-sm text-gray-500">
                    Create tenant
                  </span>
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

        {/* Inline Wizard Modal */}
        <AnimatePresence mode="wait">
          {activeWizard && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50"
              onClick={(e) => e.stopPropagation()}
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
    </div>
    );
}

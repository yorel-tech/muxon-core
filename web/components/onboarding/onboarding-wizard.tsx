'use client';

import { motion, AnimatePresence } from 'framer-motion';
import { useState } from 'react';
import { Check, ChevronRight, ChevronLeft, Settings, Server, Users, Database, Key } from 'lucide-react';
import { Button } from '@/components/ui/atoms/button';
import { Card, CardContent, CardHeader } from '@/components/ui/atoms/card';
import { ProgressBar } from '@/components/ui/atoms/progress-bar';

export interface OnboardingStep {
  id: string;
  title: string;
  description: string;
  icon: React.ReactNode;
  completed: boolean;
}

export interface OnboardingWizardProps {
  onComplete: () => void;
  onSkip: () => void;
}

const steps: OnboardingStep[] = [
  {
    id: 'welcome',
    title: 'Welcome to Infron',
    description: 'Let\'s get your cloud infrastructure configured in just a few minutes.',
    icon: <Server className="h-12 w-12" />,
    completed: false,
  },
  {
    id: 'identity-provider',
    title: 'Configure Identity Provider',
    description: 'Set up an identity provider (like Keycloak) to manage user authentication and access.',
    icon: <Key className="h-12 w-12" />,
    completed: false,
  },
  {
    id: 'datacenter',
    title: 'Add Your First Datacenter',
    description: 'Connect your first datacenter to start managing your infrastructure.',
    icon: <Database className="h-12 w-12" />,
    completed: false,
  },
  {
    id: 'users',
    title: 'Create Your First User',
    description: 'Add a system user to help manage your cloud infrastructure.',
    icon: <Users className="h-12 w-12" />,
    completed: false,
  },
  {
    id: 'complete',
    title: 'You\'re All Set!',
    description: 'Your Infron instance is ready. Start managing your cloud infrastructure.',
    icon: <Check className="h-12 w-12" />,
    completed: false,
  },
];

export function OnboardingWizard({ onComplete, onSkip }: OnboardingWizardProps) {
  const [currentStepIndex, setCurrentStepIndex] = useState(0);
  const [completedSteps, setCompletedSteps] = useState<Set<string>>(new Set());
  const [isAnimating, setIsAnimating] = useState(false);

  const currentStep = steps[currentStepIndex];
  const progress = ((completedSteps.size + (isAnimating ? 1 : 0)) / steps.length) * 100;

  const handleNext = () => {
    setIsAnimating(true);
    setTimeout(() => {
      if (currentStepIndex < steps.length - 1) {
        setCompletedSteps((prev) => new Set(prev).add(currentStep.id));
        setCurrentStepIndex((prev) => prev + 1);
      }
      setIsAnimating(false);
    }, 300);
  };

  const handleBack = () => {
    setIsAnimating(true);
    setTimeout(() => {
      if (currentStepIndex > 0) {
        setCurrentStepIndex((prev) => prev - 1);
      }
      setIsAnimating(false);
    }, 300);
  };

  const handleComplete = () => {
    setCompletedSteps((prev) => new Set(prev).add(currentStep.id));
    onComplete();
  };

  const handleSkip = () => {
    onSkip();
  };

  const renderStepContent = () => {
    switch (currentStep.id) {
      case 'welcome':
        return (
          <div className="space-y-6">
            <div className="bg-gradient-to-br from-blue-50 to-indigo-50 rounded-lg p-8">
              <Server className="h-16 w-16 text-primary mx-auto mb-4" />
              <h3 className="text-xl font-semibold text-center text-gray-900">
                Welcome to Infron Cloud Management
              </h3>
              <p className="text-center text-gray-600">
                We'll guide you through the initial setup process to get your cloud infrastructure
                up and running. This should take about 5 minutes.
              </p>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <Card className="hover:shadow-md transition-shadow">
                <CardContent className="p-6">
                  <h4 className="font-medium mb-2">System User Dashboard</h4>
                  <p className="text-sm text-gray-600">
                    Manage providers, datacenters, users, and tenants from one centralized dashboard.
                  </p>
                </CardContent>
              </Card>
              <Card className="hover:shadow-md transition-shadow">
                <CardContent className="p-6">
                  <h4 className="font-medium mb-2">Multi-Tenancy</h4>
                  <p className="text-sm text-gray-600">
                    Create and manage multiple tenants with their own isolated resources and users.
                  </p>
                </CardContent>
              </Card>
            </div>
          </div>
        );

      case 'identity-provider':
        return (
          <div className="space-y-6">
            <div className="bg-gradient-to-br from-purple-50 to-pink-50 rounded-lg p-8">
              <Key className="h-16 w-16 text-purple-600 mx-auto mb-4" />
              <h3 className="text-xl font-semibold text-center text-gray-900">
                Configure Identity Provider
              </h3>
              <p className="text-center text-gray-600">
                Infron uses OpenID Connect (OIDC) for authentication. We recommend Keycloak as your
                identity provider.
              </p>
            </div>
            <Card>
              <CardContent className="space-y-4">
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
                  <input
                    type="url"
                    placeholder="https://your-idp.com/realms/infron"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Client ID
                  </label>
                  <input
                    type="text"
                    placeholder="your-client-id"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
                  />
                </div>
              </CardContent>
            </Card>
          </div>
        );

      case 'datacenter':
        return (
          <div className="space-y-6">
            <div className="bg-gradient-to-br from-green-50 to-teal-50 rounded-lg p-8">
              <Database className="h-16 w-16 text-green-600 mx-auto mb-4" />
              <h3 className="text-xl font-semibold text-center text-gray-900">
                Add Your First Datacenter
              </h3>
              <p className="text-center text-gray-600">
                Connect your first datacenter to start provisioning virtual machines and networks.
                Infron supports multiple providers including Proxmox and Libvirt.
              </p>
            </div>
            <Card>
              <CardContent className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Datacenter Name
                  </label>
                  <input
                    type="text"
                    placeholder="Primary Datacenter"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
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
                    Connection Details
                  </label>
                  <input
                    type="text"
                    placeholder="https://datacenter.example.com:8006"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
                  />
                </div>
              </CardContent>
            </Card>
          </div>
        );

      case 'users':
        return (
          <div className="space-y-6">
            <div className="bg-gradient-to-br from-orange-50 to-amber-50 rounded-lg p-8">
              <Users className="h-16 w-16 text-orange-600 mx-auto mb-4" />
              <h3 className="text-xl font-semibold text-center text-gray-900">
                Create Your First User
              </h3>
              <p className="text-center text-gray-600">
                Create a system user account to manage your Infron instance. This user will have
                full administrative access to all resources.
              </p>
            </div>
            <Card>
              <CardContent className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Username
                  </label>
                  <input
                    type="text"
                    placeholder="admin"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Email
                  </label>
                  <input
                    type="email"
                    placeholder="admin@example.com"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Password
                  </label>
                  <input
                    type="password"
                    placeholder="•••••••••••"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
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
              </CardContent>
            </Card>
          </div>
        );

      case 'complete':
        return (
          <div className="space-y-6">
            <div className="bg-gradient-to-br from-green-50 to-emerald-50 rounded-lg p-8 text-center">
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ duration: 0.5, type: 'spring' }}
              >
                <Check className="h-20 w-20 text-green-600 mx-auto mb-4" />
              </motion.div>
              <h3 className="text-2xl font-bold text-center text-gray-900 mb-2">
                Setup Complete!
              </h3>
              <p className="text-center text-gray-600 mb-8">
                Your Infron instance is now configured and ready to use. Here's what you can do next:
              </p>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 max-w-3xl mx-auto">
                <Card className="hover:shadow-lg transition-shadow">
                  <CardContent className="p-6 text-center">
                    <Server className="h-8 w-8 text-primary mx-auto mb-3" />
                    <h4 className="font-medium mb-2">Add More Datacenters</h4>
                    <p className="text-sm text-gray-600">
                      Connect additional datacenters to expand your infrastructure.
                    </p>
                  </CardContent>
                </Card>
                <Card className="hover:shadow-lg transition-shadow">
                  <CardContent className="p-6 text-center">
                    <Users className="h-8 w-8 text-primary mx-auto mb-3" />
                    <h4 className="font-medium mb-2">Create Tenants</h4>
                    <p className="text-sm text-gray-600">
                      Set up tenants and invite users to your organization.
                    </p>
                  </CardContent>
                </Card>
                <Card className="hover:shadow-lg transition-shadow">
                  <CardContent className="p-6 text-center">
                    <Settings className="h-8 w-8 text-primary mx-auto mb-3" />
                    <h4 className="font-medium mb-2">Configure Settings</h4>
                    <p className="text-sm text-gray-600">
                      Customize your instance settings and preferences.
                    </p>
                  </CardContent>
                </Card>
                <Card className="hover:shadow-lg transition-shadow">
                  <CardContent className="p-6 text-center">
                    <Key className="h-8 w-8 text-primary mx-auto mb-3" />
                    <h4 className="font-medium mb-2">View Documentation</h4>
                    <p className="text-sm text-gray-600">
                      Learn more about Infron features and capabilities.
                    </p>
                  </CardContent>
                </Card>
              </div>
            </div>
          </div>
        );

      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100 flex items-center justify-center p-4">
      <Card className="w-full max-w-4xl">
        <CardHeader className="border-b border-gray-200 px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-2">
                {steps.map((step, index) => (
                  <div
                    key={step.id}
                    className={`h-2 rounded-full transition-all ${
                      index <= currentStepIndex
                        ? 'bg-primary'
                        : index === currentStepIndex - 1
                        ? 'bg-primary/50'
                        : 'bg-gray-200'
                    }`}
                    style={{ width: '32px' }}
                  />
                ))}
              </div>
              <div className="flex-1">
                <ProgressBar value={progress} className="h-2" />
              </div>
            </div>
            <button
              onClick={handleSkip}
              className="text-sm text-gray-500 hover:text-gray-700 transition-colors"
            >
              Skip setup
            </button>
          </div>
        </CardHeader>
        <CardContent className="p-8">
          <AnimatePresence mode="wait">
            <motion.div
              key={currentStep.id}
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              transition={{ duration: 0.3 }}
            >
              <div className="mb-8">
                <div className="flex items-center gap-4 mb-4">
                  <div className={`p-3 rounded-lg ${
                    completedSteps.has(currentStep.id) || currentStep.id === 'complete'
                      ? 'bg-green-100 text-green-700'
                      : 'bg-gray-100 text-gray-600'
                  }`}>
                    {currentStep.icon}
                  </div>
                  <div>
                    <h2 className="text-2xl font-bold text-gray-900">
                      Step {currentStepIndex + 1} of {steps.length}
                    </h2>
                    <p className="text-gray-600">{currentStep.title}</p>
                  </div>
                </div>
                <p className="text-gray-500">{currentStep.description}</p>
              </div>
              {renderStepContent()}
              <div className="flex justify-between mt-8 pt-6 border-t border-gray-200">
                {currentStepIndex > 0 && (
                  <Button
                    variant="secondary"
                    onClick={handleBack}
                    disabled={isAnimating}
                  >
                    <ChevronLeft className="h-4 w-4 mr-2" />
                    Back
                  </Button>
                )}
                {currentStep.id === 'complete' ? (
                  <Button onClick={handleComplete} isLoading={isAnimating}>
                    Get Started
                    <ChevronRight className="h-4 w-4 ml-2" />
                  </Button>
                ) : (
                  <Button onClick={handleNext} disabled={isAnimating}>
                    Next
                    <ChevronRight className="h-4 w-4 ml-2" />
                  </Button>
                )}
              </div>
            </motion.div>
          </AnimatePresence>
        </CardContent>
      </Card>
    </div>
  );
}

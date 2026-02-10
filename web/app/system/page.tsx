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
import { apiGet, apiPost, apiPut } from '@/lib/api';

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

interface IdpConfig {
  providerType: string;
  issuerUrl: string;
  clientId: string;
  clientSecret: string;
  realm: string;
  name?: string;
}

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
  preferred_username?: string; // API returns snake_case, but we'll map to camelCase
  email: string;
  name?: string;
}

interface RoleBindingCreateItem {
  roleId: string;
  subjectType: 'user' | 'group' | 'service_account';
  subjectId: string;
  scopeType: 'system' | 'tenant' | 'tenant_global';
  scopeId?: string;
  expiresAt?: string;
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
      title: 'Add System Users',
      description: 'Select users from your configured Identity Provider to add as system users.',
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
  const [idpConfig, setIdpConfig] = useState<IdpConfig | null>(null);
  // IDP form state
  const [idpName, setIdpName] = useState<string>('Keycloak');
  const [idpIssuerUrl, setIdpIssuerUrl] = useState<string>('');
  const [idpClientId, setIdpClientId] = useState<string>('');
  const [idpClientSecret, setIdpClientSecret] = useState<string>('');
  const [idpScopes, setIdpScopes] = useState<string>('openid,profile,email');
  const [idpAutoProvisionUsers, setIdpAutoProvisionUsers] = useState<boolean>(true);
  const [availableUsers, setAvailableUsers] = useState<IdpUser[]>([]);
  const [selectedUserIds, setSelectedUserIds] = useState<Set<string>>(new Set());
  const [existingSystemUserIds, setExistingSystemUserIds] = useState<Set<string>>(new Set());
  const [selectedIdp, setSelectedIdp] = useState<string>(''); // ID of selected IDP
  const [idpServers, setIdpServers] = useState<IdpServer[]>([]); // List of available IDP servers
  const [isLoadingUsers, setIsLoadingUsers] = useState(false);
  const [isFetchingConfig, setIsFetchingConfig] = useState(false);
  const [isFetchingIdpServers, setIsFetchingIdpServers] = useState(false);
  const [systemAdminRoleId, setSystemAdminRoleId] = useState<string | null>(null);
  const DEFAULT_IDP_ID = '62083d54-cb8c-521f-9374-65e9f21c8991';
  
  // Datacenter wizard state
  const [selectedProviderType, setSelectedProviderType] = useState<string>('libvirt');
  const [selectedProvider, setSelectedProvider] = useState<string>('');
  const [availableProviders, setAvailableProviders] = useState<any[]>([]);
  const [isLoadingProviders, setIsLoadingProviders] = useState(false);
  
  // Provider wizard form state
  const [providerName, setProviderName] = useState<string>('');
  const [providerEndpoint, setProviderEndpoint] = useState<string>('');
  const [providerUsername, setProviderUsername] = useState<string>('');
  const [providerPassword, setProviderPassword] = useState<string>('');
  const [providerDescription, setProviderDescription] = useState<string>('');
  
  // Datacenter wizard form state
  const [datacenterName, setDatacenterName] = useState<string>('');
  const [datacenterDescription, setDatacenterDescription] = useState<string>('');
  const [datacenterCpuOvercommit, setDatacenterCpuOvercommit] = useState<string>('4.0');
  const [datacenterMemoryOvercommit, setDatacenterMemoryOvercommit] = useState<string>('1.5');
  const [datacenterVmClasses, setDatacenterVmClasses] = useState<string>('small,medium,large');
  const [datacenterStorageClasses, setDatacenterStorageClasses] = useState<string>('gold,silver,bronze');
  const [datacenterNetworkDomains, setDatacenterNetworkDomains] = useState<string>('private,public');
  
  // Tenant wizard form state
  const [tenantName, setTenantName] = useState<string>('');
  const [tenantDisplayName, setTenantDisplayName] = useState<string>('');
  const [tenantDescription, setTenantDescription] = useState<string>('');
  const [tenantAdminEmail, setTenantAdminEmail] = useState<string>('');
  const [tenantAdminPassword, setTenantAdminPassword] = useState<string>('');
  
  // Derived state from bootstrapStatus
  const isBootstrapped = bootstrapStatus?.systemStatus === 'BOOTSTRAPPED' || bootstrapStatus?.systemStatus === 'READY';
  const isReady = bootstrapStatus?.systemStatus === 'READY';
  
  // Message constants to avoid JSX parsing issues with curly braces
  const bootstrappedMessage = 'Your system has been pre-configured. Review and complete the remaining setup steps below.';
  const notBootstrappedMessage = 'Get started by configuring your cloud infrastructure';
  
  // Action text constants to avoid JSX parsing issues with curly braces
  const idpActionText = isBootstrapped ? 'Edit' : 'Set up';
  const providerActionText = isBootstrapped ? 'Add' : 'Connect';
  const systemUsersActionText = isBootstrapped ? 'Edit' : 'Add';
  
  // Check bootstrap status on mount
  useEffect(() => {
    fetchBootstrapStatus();
    fetchExistingEntities();
  }, []);

  // Redirect to dashboard if status is READY
  useEffect(() => {
    if (isReady) {
      window.location.href = '/system/dashboard';
    }
  }, [isReady]);
  
  // Update setup steps when bootstrap status changes
  useEffect(() => {
    if (bootstrapStatus) {
      const isBootstrapped = bootstrapStatus.systemStatus === 'BOOTSTRAPPED' || bootstrapStatus.systemStatus === 'READY';
      setSetupSteps(prev => prev.map(step => {
        if (isBootstrapped && (step.id === 'idp' || step.id === 'system-users')) {
          return { ...step, completed: true };
        }
        return step;
      }));
      
      // Fetch existing configurations if bootstrapped
      if (isBootstrapped) {
        fetchExistingConfigs();
      }
    }
  }, [bootstrapStatus]);
  
  // Fetch existing entities to mark steps as completed
  const [existingProviders, setExistingProviders] = useState(false);
  const [existingDatacenters, setExistingDatacenters] = useState(false);
  const [existingTenants, setExistingTenants] = useState(false);
  
  const fetchExistingEntities = async () => {
    try {
      // Check if providers exist
      const providersData = await apiGet('/api/v1/providers');
      const providers = Array.isArray(providersData) ? providersData : (providersData?.items || []);
      const hasProviders = providers.length > 0;
      setExistingProviders(hasProviders);
      console.log('Providers data:', providersData, 'Has providers:', hasProviders);

      // Check if datacenters exist
      const datacentersData = await apiGet('/api/v1/datacenters');
      const datacenters = Array.isArray(datacentersData) ? datacentersData : (datacentersData?.items || []);
      const hasDatacenters = datacenters.length > 0;
      setExistingDatacenters(hasDatacenters);
      console.log('Datacenters data:', datacentersData, 'Has datacenters:', hasDatacenters);

      // Check if tenants exist
      const tenantsData = await apiGet('/api/v1/tenants');
      const tenants = Array.isArray(tenantsData) ? tenantsData : (tenantsData?.items || []);
      const hasTenants = tenants.length > 0;
      setExistingTenants(hasTenants);
      console.log('Tenants data:', tenantsData, 'Has tenants:', hasTenants);
    } catch (error) {
      console.error('Error fetching existing entities:', error);
    }
  };
  
  // Update setup steps when existing entities change
  useEffect(() => {
    if (existingProviders) {
      setSetupSteps(prev => prev.map(step => {
        if (step.id === 'provider') {
          return { ...step, completed: true };
        }
        return step;
      }));
    }
  }, [existingProviders]);
  
  useEffect(() => {
    if (existingDatacenters) {
      setSetupSteps(prev => prev.map(step => {
        if (step.id === 'datacenter') {
          return { ...step, completed: true };
        }
        return step;
      }));
    }
  }, [existingDatacenters]);
  
  useEffect(() => {
    if (existingTenants) {
      setSetupSteps(prev => prev.map(step => {
        if (step.id === 'tenant') {
          return { ...step, completed: true };
        }
        return step;
      }));
    }
  }, [existingTenants]);
  
  // Fetch IDP servers when bootstrapped
  useEffect(() => {
    if (isBootstrapped) {
      fetchIdpServers();
    }
  }, [isBootstrapped]);
  
  // Fetch IDP users when system-users wizard is opened
  useEffect(() => {
    if (activeWizard === 'system-users' && isBootstrapped) {
      // Fetch system admin role ID
      fetchSystemAdminRole();
      // Fetch existing system users to mark them as selected
      fetchExistingSystemUsers();
      // Fetch IDP users for the selected IDP
      if (selectedIdp) {
        fetchIdpUsers();
      }
    }
  }, [activeWizard, isBootstrapped, selectedIdp]);
  
  // Fetch providers when datacenter wizard is opened and provider type changes
  useEffect(() => {
    if (activeWizard === 'datacenter' && selectedProviderType) {
      fetchProvidersByType(selectedProviderType);
    }
  }, [activeWizard, selectedProviderType]);
  
  const fetchProvidersByType = async (type: string) => {
    setIsLoadingProviders(true);
    try {
      // Backend expects uppercase enum values for providers API
      const data = await apiGet(`/api/v1/providers?type=${type.toUpperCase()}`);
      const providers = Array.isArray(data) ? data : (data?.items || []);
      setAvailableProviders(providers);
      
      // Keep the selected provider if it's still in the list, otherwise reset
      const selectedProviderStillValid = selectedProvider && providers.some((p: any) => p.id === selectedProvider);
      if (!selectedProviderStillValid) {
        // Auto-select the first provider if only one is available
        if (providers.length === 1) {
          setSelectedProvider(providers[0].id);
        } else {
          setSelectedProvider('');
        }
      }
    } catch (error) {
      console.error('Error fetching providers:', error);
      setAvailableProviders([]);
      setSelectedProvider('');
    } finally {
      setIsLoadingProviders(false);
    }
  };
  
  const fetchBootstrapStatus = async () => {
    try {
      const status: BootstrapStatusDto = await apiGet('/api/v1/status');
      setBootstrapStatus(status);
    } catch (error) {
      console.error('Error fetching bootstrap status:', error);
      setBootstrapStatus({ systemStatus: 'NOTREADY' });
    } finally {
      setIsLoading(false);
    }
  };
  
  const fetchIdpServers = async () => {
    setIsFetchingIdpServers(true);
    try {
      // Fetch IDP settings to get the configured IDP
      const idpData = await apiGet('/api/v1/system-settings/idp');
      if (idpData && idpData.name) {
        // Create IDP server entry from the configured IDP
        const idpServer: IdpServer = {
          id: DEFAULT_IDP_ID,
          name: idpData.name,
          protocol: idpData.type || 'OIDC',
          enabled: idpData.enabled || false,
          isSystem: true,
        };
        setIdpServers([idpServer]);
        // Set the IDP as selected
        setSelectedIdp(DEFAULT_IDP_ID);
      }
    } catch (error) {
      console.error('Error fetching IDP servers:', error);
      setIdpServers([]);
    } finally {
      setIsFetchingIdpServers(false);
    }
  };
  
  const fetchExistingConfigs = async () => {
    setIsFetchingConfig(true);
    try {
      // Fetch IDP configuration
      const idpData = await apiGet('/api/v1/system-settings/idp');
      setIdpConfig({
        providerType: idpData.type || 'oidc',
        issuerUrl: idpData.issuerUrl || '',
        clientId: idpData.clientId || '',
        clientSecret: idpData.clientSecret || '',
        realm: '',
        name: idpData.name,
      });
      // Set form state from fetched config
      setIdpName(idpData.name || 'Keycloak');
      setIdpIssuerUrl(idpData.issuerUrl || '');
      setIdpClientId(idpData.clientId || '');
      setIdpClientSecret(idpData.clientSecret || '');
      setIdpScopes(idpData.scopes || 'openid,profile,email');
      setIdpAutoProvisionUsers(idpData.autoProvisionUsers !== undefined ? idpData.autoProvisionUsers : true);

      // Set selected IDP from config (use the IDP ID)
      if (idpData.name) {
        setSelectedIdp(DEFAULT_IDP_ID);
      }

      // Fetch existing system users
      const usersData = await apiGet('/api/v1/system-users');
      const users = usersData.items || [];
      if (users.length > 0) {
        const existingUserIds = users.map((u: any) => u.external_id);
        setSelectedUserIds(new Set(existingUserIds));
      }
    } catch (error) {
      console.error('Error fetching existing configurations:', error);
    } finally {
      setIsFetchingConfig(false);
    }
  };
  
  const fetchExistingSystemUsers = async () => {
    try {
      // Fetch existing system users to mark them as selected
      const usersData = await apiGet('/api/v1/system-users');
      const users = usersData.items || [];
      if (users.length > 0) {
        const existingUserIds = users.map((u: any) => u.external_id);
        setExistingSystemUserIds(new Set(existingUserIds));
        setSelectedUserIds(new Set(existingUserIds));
      } else {
        setExistingSystemUserIds(new Set());
        setSelectedUserIds(new Set());
      }
    } catch (error) {
      console.error('Error fetching existing system users:', error);
    }
  };
  
  const fetchIdpUsers = async () => {
    setIsLoadingUsers(true);
    try {
      // Use the selected IDP's ID to fetch users
      const data = await apiGet(`/api/v1/idp/${selectedIdp}/users`);
      // Handle both array and wrapped response formats
      // API returns OidcUserList with 'items' property
      const users = Array.isArray(data) ? data : (data.items || []);
      // Map snake_case properties to camelCase if needed
      const mappedUsers = users.map((user: any) => ({
        sub: user.sub,
        preferredUsername: user.preferredUsername || user.preferred_username,
        email: user.email,
        name: user.name,
      }));
      setAvailableUsers(mappedUsers);
    } catch (error) {
      console.error('Error fetching IDP users:', error);
      setAvailableUsers([]);
    } finally {
      setIsLoadingUsers(false);
    }
  };

  const fetchSystemAdminRole = async () => {
    try {
      // Fetch roles to find the system:admin role UUID
      const data = await apiGet('/api/v1/roles?scopeType=system');
      const roles = Array.isArray(data) ? data : (data?.items || []);
      const systemAdminRole = roles.find((r: any) => r.name === 'system:admin');
      if (systemAdminRole) {
        setSystemAdminRoleId(systemAdminRole.id);
      } else {
        console.error('System admin role not found');
      }
    } catch (error) {
      console.error('Error fetching system admin role:', error);
    }
  };
  
  const toggleUserSelection = (userId: string) => {
    // Prevent deselecting existing system users
    if (existingSystemUserIds.has(userId)) {
      return; // Cannot deselect already added system users
    }
    setSelectedUserIds(prev => {
      const newSet = new Set(prev);
      if (newSet.has(userId)) {
        newSet.delete(userId);
      } else {
        newSet.add(userId);
      }
      return newSet;
    });
  };
  
  const handleIdpChange = (idpId: string) => {
    setSelectedIdp(idpId);
    // Fetch users for the selected IDP
    if (idpId) {
      fetchIdpUsers();
    }
  };
  
  const handleStepClick = (stepId: string) => {
    setActiveWizard(stepId);
  };
  
  const handleWizardClose = () => {
    setActiveWizard(null);
  };
  
  const handleSkipToDashboard = async () => {
    try {
      // Mark bootstrap as READY
      await apiPut('/api/v1/status/ready');
      // Redirect to dashboard
      window.location.href = '/system/dashboard';
    } catch (error) {
      console.error('Error marking bootstrap as ready:', error);
      alert('Failed to mark system as ready. Please try again.');
    }
  };
  
  const handleSave = async () => {
    setIsSaving(true);
    try {
      if (activeWizard === 'system-users') {
        // Only save newly selected users (not existing system users)
        const newUserIds = Array.from(selectedUserIds).filter(id => !existingSystemUserIds.has(id));
        if (!systemAdminRoleId) {
          throw new Error('System admin role not found. Please refresh the page and try again.');
        }
        const bindings: RoleBindingCreateItem[] = newUserIds.map(subjectId => ({
          roleId: systemAdminRoleId,
          subjectType: 'user',
          subjectId: subjectId,
          scopeType: 'system',
        }));
        if (bindings.length > 0) {
          await apiPost('/api/v1/system-users', { bindings });
        }
      } else if (activeWizard === 'provider') {
        // Create provider
        if (!providerName || !providerEndpoint) {
          throw new Error('Provider name and endpoint are required');
        }
        const providerData = {
          name: providerName,
          type: selectedProviderType.toUpperCase(),
          endpoint: providerEndpoint,
          credentials: {
            username: providerUsername,
            password: providerPassword,
          },
          description: providerDescription,
        };
        await apiPost('/api/v1/providers', providerData);
      } else if (activeWizard === 'datacenter') {
        // Create datacenter
        if (!datacenterName || !selectedProvider) {
          throw new Error('Datacenter name and provider are required');
        }
        const datacenterData = {
          name: datacenterName,
          providerType: selectedProviderType.toLowerCase(),
          providerId: selectedProvider,
          description: datacenterDescription,
          settings: {
            defaultCpuOvercommitRatio: parseFloat(datacenterCpuOvercommit) || 4.0,
            defaultMemoryOvercommitRatio: parseFloat(datacenterMemoryOvercommit) || 1.5,
            vmClasses: datacenterVmClasses.split(',').map(s => s.trim()),
            storageClasses: datacenterStorageClasses.split(',').map(s => s.trim()),
            networkDomains: datacenterNetworkDomains.split(',').map(s => s.trim()),
          },
        };
        await apiPost('/api/v1/datacenters', datacenterData);
      } else if (activeWizard === 'tenant') {
        // Create tenant
        if (!tenantName) {
          throw new Error('Tenant name is required');
        }
        const tenantData = {
          name: tenantName,
          displayName: tenantDisplayName || undefined,
          description: tenantDescription,
        };
        await apiPost('/api/v1/tenants', tenantData);
      } else if (activeWizard === 'idp') {
        // Save IDP configuration
        const idpData = {
          type: 'oidc',
          name: idpName,
          issuerUrl: idpIssuerUrl,
          clientId: idpClientId,
          clientSecret: idpClientSecret,
          scopes: idpScopes,
          autoProvisionUsers: idpAutoProvisionUsers,
          enabled: true,
        };
        await apiPut('/api/v1/system-settings/idp', idpData);
      }

      // Mark step as completed
      setSetupSteps(prev => prev.map(step =>
        step.id === activeWizard ? { ...step, completed: true } : step
      ));

      setActiveWizard(null);
      // Refresh configuration status
      fetchBootstrapStatus();
      // Refresh existing entities to mark steps as completed
      fetchExistingEntities();
    } catch (error) {
      console.error('Error saving configuration:', error);
      let errorMessage = 'Failed to save configuration';
      if (error instanceof Error) {
        // Try to extract the actual message from the error
        errorMessage = error.message;
        // If the error contains JSON, try to parse it and extract the message
        if (errorMessage.includes('"message"')) {
          try {
            const match = errorMessage.match(/"message"\s*:\s*"([^"]+)"/);
            if (match) {
              errorMessage = match[1];
            }
          } catch (e) {
            // If parsing fails, use the original message
          }
        }
      }
      alert(errorMessage);
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
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Provider Type
              </label>
              <div className="px-4 py-2 bg-gray-100 rounded-lg text-gray-700">
                OIDC (OpenID Connect)
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Provider Name
              </label>
              <Input
                type="text"
                placeholder="Keycloak"
                value={idpName}
                onChange={(e) => setIdpName(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Configured IDP: {idpConfig?.name || 'None'}
              </label>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Issuer URL
              </label>
              <Input
                type="text"
                placeholder="https://keycloak.example.com/realms/infron"
                value={idpIssuerUrl}
                onChange={(e) => setIdpIssuerUrl(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Client ID
              </label>
              <Input
                type="text"
                placeholder="infron-client"
                value={idpClientId}
                onChange={(e) => setIdpClientId(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Client Secret
              </label>
              <Input
                type="password"
                placeholder="•••••••••••••"
                value={idpClientSecret}
                onChange={(e) => setIdpClientSecret(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Scopes
              </label>
              <Input
                type="text"
                placeholder="openid,profile,email"
                value={idpScopes}
                onChange={(e) => setIdpScopes(e.target.value)}
              />
            </div>
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="autoProvisionUsers"
                checked={idpAutoProvisionUsers}
                onChange={(e) => setIdpAutoProvisionUsers(e.target.checked)}
                className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary"
              />
              <label htmlFor="autoProvisionUsers" className="text-sm font-medium text-gray-700">
                Auto-provision users
              </label>
            </div>
          </div>
        );
 
      case 'system-users':
        return (
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Identity Provider
              </label>
              <select
                className="w-full px-4 py-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
                value={selectedIdp}
                onChange={(e) => handleIdpChange(e.target.value)}
                disabled={isFetchingIdpServers || idpServers.length === 0}
              >
                {isFetchingIdpServers ? (
                  <option value="" disabled>Loading...</option>
                ) : idpServers.length === 0 ? (
                  <option value="" disabled>No IDP configured</option>
                ) : (
                  idpServers.map((idp) => (
                    <option key={idp.id} value={idp.id}>
                      {idp.name} {idp.protocol ? ` (${idp.protocol})` : ''}
                    </option>
                  ))
                )}
              </select>
            </div>
 
            <div>
              <div className="flex justify-between items-center mb-2">
                <label className="block text-sm font-medium text-gray-700">
                  Available Users
                </label>
                <span className="text-sm text-gray-500">
                  Selected: {selectedUserIds.size} user(s)
                </span>
              </div>
              {isLoadingUsers ? (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="h-6 w-6 animate-spin text-gray-400" />
                </div>
              ) : !selectedIdp ? (
                <div className="text-center py-8 text-gray-500">
                  Please select an Identity Provider first.
                </div>
              ) : availableUsers.length === 0 ? (
                <div className="text-center py-8 text-gray-500">
                  No users found. Make sure your Identity Provider is configured correctly.
                </div>
              ) : (
                <div className="space-y-2 max-h-80 overflow-y-auto">
                  {availableUsers.map(user => {
                    const isExisting = existingSystemUserIds.has(user.sub);
                    const isSelected = selectedUserIds.has(user.sub);
                    return (
                      <div
                        key={user.sub}
                        onClick={() => toggleUserSelection(user.sub)}
                        className={`flex items-center gap-3 p-3 rounded-lg border cursor-pointer transition-all ${
                          isSelected
                            ? 'bg-green-50 border-green-200'
                            : 'bg-white border-gray-200 hover:border-primary-300'
                        } ${isExisting ? 'cursor-default' : ''}`}
                      >
                        <div className="flex-shrink-0">
                          {isSelected ? (
                            <CheckCircle2 className={`h-5 w-5 ${isExisting ? 'text-green-600' : 'text-green-600'}`} />
                          ) : (
                            <div className="h-5 w-5 border-2 border-gray-300 rounded" />
                          )}
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="font-medium text-gray-900">
                            {user.name || user.preferredUsername}
                          </div>
                          <div className="text-sm text-gray-500">{user.email}</div>
                        </div>
                        {isExisting ? (
                          <span className="text-xs font-medium bg-blue-100 text-blue-600 px-2 py-0.5 rounded-full">
                            Added
                          </span>
                        ) : isSelected ? (
                          <span className="text-xs font-medium bg-green-100 text-green-600 px-2 py-0.5 rounded-full">
                            Selected
                          </span>
                        ) : null}
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        );
 
      case 'provider':
        return (
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Provider Type
              </label>
              <select
                className="w-full px-4 py-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
                value={selectedProviderType}
                onChange={(e) => setSelectedProviderType(e.target.value)}
              >
                <option value="proxmox">Proxmox</option>
                <option value="libvirt">Libvirt</option>
                <option value="kubernetes">Kubernetes</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Provider Name *
              </label>
              <Input
                type="text"
                placeholder="My Libvirt Provider"
                value={providerName}
                onChange={(e) => setProviderName(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Endpoint *
              </label>
              <Input
                type="text"
                placeholder={
                  selectedProviderType === 'PROXMOX'
                    ? 'https://proxmox.example.com:8006/api2/json'
                    : selectedProviderType === 'KUBERNETES'
                    ? 'https://kubernetes.example.com:6443'
                    : 'ssh://user@host:port or libvirt://system'
                }
                value={providerEndpoint}
                onChange={(e) => setProviderEndpoint(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Username
              </label>
              <Input
                type="text"
                placeholder="root"
                value={providerUsername}
                onChange={(e) => setProviderUsername(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Password
              </label>
              <Input
                type="password"
                placeholder="••••••••••••"
                value={providerPassword}
                onChange={(e) => setProviderPassword(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Description (optional)
              </label>
              <Input
                type="text"
                placeholder="Main production datacenter"
                value={providerDescription}
                onChange={(e) => setProviderDescription(e.target.value)}
              />
            </div>
          </div>
        );
 
      case 'datacenter':
        return (
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Datacenter Name *
              </label>
              <Input
                type="text"
                placeholder="Main Production Datacenter"
                value={datacenterName}
                onChange={(e) => setDatacenterName(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Provider Type *
              </label>
              <select
                className="w-full px-4 py-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
                value={selectedProviderType}
                onChange={(e) => setSelectedProviderType(e.target.value)}
              >
                <option value="libvirt">Libvirt</option>
                <option value="proxmox">Proxmox</option>
                <option value="kubernetes">Kubernetes</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Provider *
              </label>
              <select
                className="w-full px-4 py-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
                value={selectedProvider}
                onChange={(e) => setSelectedProvider(e.target.value)}
                disabled={isLoadingProviders || availableProviders.length === 0}
              >
                {isLoadingProviders ? (
                  <option value="" disabled>Loading providers...</option>
                ) : availableProviders.length === 0 ? (
                  <option value="" disabled>No providers available. Please add a provider first.</option>
                ) : (
                  availableProviders.map((provider) => (
                    <option key={provider.id} value={provider.id}>
                      {provider.name}
                    </option>
                  ))
                )}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Description (optional)
              </label>
              <Input
                type="text"
                placeholder="Main production datacenter for VM workloads"
                value={datacenterDescription}
                onChange={(e) => setDatacenterDescription(e.target.value)}
              />
            </div>
            <div className="border-t border-gray-200 pt-4">
              <h4 className="text-sm font-medium text-gray-700 mb-3">Datacenter Settings (Optional)</h4>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    CPU Overcommit Ratio
                  </label>
                  <Input
                    type="text"
                    placeholder="4.0"
                    value={datacenterCpuOvercommit}
                    onChange={(e) => setDatacenterCpuOvercommit(e.target.value)}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Memory Overcommit Ratio
                  </label>
                  <Input
                    type="text"
                    placeholder="1.5"
                    value={datacenterMemoryOvercommit}
                    onChange={(e) => setDatacenterMemoryOvercommit(e.target.value)}
                  />
                </div>
              </div>
              <div className="mt-4">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    VM Classes (comma-separated)
                  </label>
                  <Input
                    type="text"
                    placeholder="small,medium,large"
                    value={datacenterVmClasses}
                    onChange={(e) => setDatacenterVmClasses(e.target.value)}
                  />
              </div>
              <div className="mt-4">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Storage Classes (comma-separated)
                  </label>
                  <Input
                    type="text"
                    placeholder="gold,silver,bronze"
                    value={datacenterStorageClasses}
                    onChange={(e) => setDatacenterStorageClasses(e.target.value)}
                  />
              </div>
              <div className="mt-4">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Network Domains (comma-separated)
                  </label>
                  <Input
                    type="text"
                    placeholder="private,public"
                    value={datacenterNetworkDomains}
                    onChange={(e) => setDatacenterNetworkDomains(e.target.value)}
                  />
              </div>
            </div>
          </div>
        );
 
      case 'tenant':
        return (
          <div className="space-y-4">
            <div className="rounded-lg p-6 text-center" style={{ background: 'linear-gradient(to bottom right, #fdf2f8, #ffe4e6)' }}>
              <Building2 className="h-12 w-12 text-pink-600 mx-auto mb-4" />
              <h3 className="text-xl font-semibold text-gray-900 mb-2">
                Create Tenant
              </h3>
              <p className="text-gray-600 text-sm mb-4">
                Create your first tenant organization and assign users to it.
              </p>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Tenant Name *
              </label>
              <Input
                type="text"
                placeholder="Acme Corp"
                value={tenantName}
                onChange={(e) => setTenantName(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Display Name (optional)
              </label>
              <Input
                type="text"
                placeholder="ACME Corporation"
                value={tenantDisplayName}
                onChange={(e) => setTenantDisplayName(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Description (optional)
              </label>
              <Input
                type="text"
                placeholder="Acme Corporation tenant"
                value={tenantDescription}
                onChange={(e) => setTenantDescription(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Admin Email
              </label>
              <Input
                type="email"
                placeholder="admin@acme.com"
                value={tenantAdminEmail}
                onChange={(e) => setTenantAdminEmail(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Admin Password
              </label>
              <Input
                type="password"
                placeholder="••••••••••••"
                value={tenantAdminPassword}
                onChange={(e) => setTenantAdminPassword(e.target.value)}
              />
            </div>
          </div>
        );
 
      default:
        return null;
    }
  };
  
  const completedCount = setupSteps.filter((s) => s.completed).length;
  
  // Don't render the page if bootstrap status is READY
  if (isReady) {
    return null;
  }
  
  // Show loading state while checking bootstrap status
  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-gray-400" />
      </div>
    );
  }
  
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
              disabled={!setupSteps.find(s => s.id === 'idp')?.completed || !setupSteps.find(s => s.id === 'system-users')?.completed}
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
                    className={`w-full flex items-center gap-4 p-4 rounded-lg border transition-all text-left ${
                      step.completed
                        ? 'border-green-200 bg-green-50/50 cursor-pointer hover:border-green-300'
                        : 'border-gray-200 hover:border-primary-300 hover:bg-gray-50 cursor-pointer'
                    }`}
                    whileHover={{ scale: 1.01 }}
                    whileTap={{ scale: 0.99 }}
                  >
                    <div className={`flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-lg ${getIconColorClass(step)}`}>
                      {step.icon}
                    </div>
                    <div className="flex-1 min-w-0">
                      <h3 className={`font-semibold ${getTextColorClass(step)}`}>
                        {step.title}
                      </h3>
                      {step.completed && (
                        <span className="text-xs font-medium bg-green-100 text-green-600 px-2 py-0.5 rounded-full inline-flex items-center gap-1">
                          Complete
                          <CheckCircle2 className="h-3 w-3" />
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
                    {systemUsersActionText} system users
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
                        Get help from Infron community
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
                    disabled={isSaving || (activeWizard === 'system-users' && Array.from(selectedUserIds).filter(id => !existingSystemUserIds.has(id)).length === 0)}
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

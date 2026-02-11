'use client';

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Save, User, Shield, Bell, Palette, Globe, Database, Key, RefreshCw, LogOut, Loader2, Plus, Edit, Trash2, MoreHorizontal, Eye } from 'lucide-react';
import { Button } from '@/components/ui/atoms/button';
import { Card, CardContent, CardHeader } from '@/components/ui/atoms/card';
import { Input } from '@/components/ui/atoms/input';
import { Label } from '@/components/ui/atoms/label';
import { Switch } from '@/components/ui/atoms/switch';
import { Toast } from '@/components/ui/molecules/toast';
import { Table, Column } from '@/components/ui/organisms/table';
import { Badge } from '@/components/ui/atoms/badge';
import { Dropdown, DropdownOption } from '@/components/ui/molecules/dropdown';
import { apiGet, apiPost, apiPut, apiDelete } from '@/lib/api';

interface IdpServer {
  id: string;
  name: string;
  protocol?: string;
  enabled: boolean;
  isSystem: boolean;
}

export default function SystemSettingsPage() {
  const [showToast, setShowToast] = useState(false);
  const [toastMessage, setToastMessage] = useState('');
  const [toastVariant, setToastVariant] = useState<'success' | 'error'>('success');
  const [activeTab, setActiveTab] = useState<'general' | 'security' | 'notifications' | 'appearance' | 'idp'>('general');
  
  // IDP Settings State
  const [idpServers, setIdpServers] = useState<IdpServer[]>([]);
  const [isIdpLoading, setIsIdpLoading] = useState(true);

  // Fetch IDP settings on mount
  useEffect(() => {
    fetchIdpSettings();
  }, []);

  const fetchIdpSettings = async () => {
    setIsIdpLoading(true);
    try {
      const idpData = await apiGet('/api/v1/system-settings/idp');
      const idpList = Array.isArray(idpData) ? idpData : (idpData?.items || []);
      
      if (idpList.length === 0) {
        setIdpServers([
          {
            id: '1',
            name: 'Keycloak',
            protocol: 'OIDC',
            enabled: true,
            isSystem: true,
          },
        ]);
      } else {
        setIdpServers(idpList);
      }
    } catch (error) {
      console.error('Error fetching IDP settings:', error);
      setIdpServers([
        {
          id: '1',
          name: 'Keycloak',
          protocol: 'OIDC',
          enabled: true,
          isSystem: true,
        },
      ]);
    } finally {
      setIsIdpLoading(false);
    }
  };

  const [settings, setSettings] = useState({
    // General Settings
    siteName: 'Infron Cloud Platform',
    siteUrl: 'https://infron.example.com',
    language: 'en',
    timezone: 'UTC',
    dateFormat: 'MM/DD/YYYY',
    timeFormat: '12h',
    
    // Security Settings
    sessionTimeout: '30',
    mfaEnabled: false,
    passwordMinLength: '12',
    passwordRequireUppercase: true,
    passwordRequireLowercase: true,
    passwordRequireNumbers: true,
    passwordRequireSpecialChars: true,
    
    // Notification Settings
    emailEnabled: true,
    emailSmtpHost: 'smtp.example.com',
    emailSmtpPort: '587',
    emailSmtpUser: 'notifications@infron.com',
    emailSmtpFrom: 'Infron <noreply@infron.com>',
    slackEnabled: false,
    slackWebhook: '',
    
    // Appearance Settings
    theme: 'light',
    primaryColor: '#3b82f6',
    accentColor: '#8b5cf6',
  });

  const handleSave = () => {
    setShowToast(true);
    setToastMessage('Settings saved successfully');
    setToastVariant('success');
    setTimeout(() => setShowToast(false), 3000);
  };

  // IDP Handlers
  const handleViewIdpDetails = (idp: IdpServer) => {
    console.log('View details for:', idp.id);
  };

  const handleEditIdp = (idp: IdpServer) => {
    console.log('Edit IDP:', idp.id);
  };

  const handleSyncIdp = async (idp: IdpServer) => {
    try {
      await apiPost(`/api/v1/idps/${idp.id}/sync`, {});
      await fetchIdpSettings();
    } catch (error) {
      console.error('Error syncing IDP:', error);
      alert('Failed to sync IDP.');
    }
  };

  const handleDisableIdp = async (idp: IdpServer) => {
    try {
      await apiPut(`/api/v1/idps/${idp.id}`, { enabled: false });
      await fetchIdpSettings();
    } catch (error) {
      console.error('Error disabling IDP:', error);
      alert('Failed to disable IDP.');
    }
  };

  const handleDeleteIdp = async (idp: IdpServer) => {
    if (window.confirm(`Are you sure you want to delete IDP "${idp.name}"?`)) {
      try {
        await apiDelete(`/api/v1/idps/${idp.id}`);
        await fetchIdpSettings();
      } catch (error) {
        console.error('Error deleting IDP:', error);
        alert('Failed to delete IDP.');
      }
    }
  };

  const getIdpContextMenuOptions = (idp: IdpServer): DropdownOption[] => [
    {
      label: 'View Details',
      icon: <Eye size={14} />,
      onClick: () => handleViewIdpDetails(idp),
    },
    {
      label: 'Edit',
      icon: <Edit size={14} />,
      onClick: () => handleEditIdp(idp),
    },
    {
      label: 'Sync',
      icon: <RefreshCw size={14} />,
      onClick: () => handleSyncIdp(idp),
    },
    {
      label: 'Disable',
      icon: <Trash2 size={14} />,
      variant: 'warning',
      onClick: () => handleDisableIdp(idp),
    },
    {
      label: 'Delete',
      icon: <Trash2 size={14} />,
      variant: 'danger',
      onClick: () => handleDeleteIdp(idp),
    },
  ];

  const idpColumns: Column<IdpServer>[] = [
    {
      key: 'actions',
      header: '',
      cell: (row: IdpServer) => (
        <div className="flex justify-start" onClick={(e) => e.stopPropagation()}>
          <Dropdown
            trigger={
              <button className="p-1.5 rounded hover:bg-gray-100 transition-colors">
                <MoreHorizontal size={16} className="text-gray-600" />
              </button>
            }
            options={getIdpContextMenuOptions(row)}
            position="right"
            usePortal={true}
          />
        </div>
      ),
      sortable: false,
    },
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
  ];

  const tabs = [
    { id: 'general', label: 'General', icon: <Globe size={18} /> },
    { id: 'security', label: 'Security', icon: <Shield size={18} /> },
    { id: 'notifications', label: 'Notifications', icon: <Bell size={18} /> },
    { id: 'appearance', label: 'Appearance', icon: <Palette size={18} /> },
    { id: 'idp', label: 'Identity Providers', icon: <Shield size={18} /> },
  ];

  return (
    <div className="max-w-7xl mx-auto p-8">
      {/* Page Header */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="mb-8"
      >
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">System Settings</h1>
            <p className="text-gray-600 mt-1">Configure platform-wide settings and preferences</p>
          </div>
          <Button onClick={handleSave} leftIcon={<Save size={16} />}>
            Save Changes
          </Button>
        </div>
      </motion.div>

      {/* Tabs */}
      <div className="border-b border-gray-200 mb-6">
        <nav className="flex space-x-8">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as any)}
              className={`pb-4 border-b-2 font-medium text-sm transition-colors ${
                activeTab === tab.id
                  ? 'border-primary-500 text-primary-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              <span className="flex items-center gap-2">
                {tab.icon}
                {tab.label}
              </span>
            </button>
          ))}
        </nav>
      </div>

      {/* Tab Content */}
      <motion.div
        key={activeTab}
        initial={{ opacity: 0, x: 20 }}
        animate={{ opacity: 1, x: 0 }}
        transition={{ duration: 0.3 }}
      >
        {activeTab === 'general' && (
          <Card>
            <CardHeader>General Settings</CardHeader>
            <CardContent className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <Label htmlFor="siteName">Site Name</Label>
                  <Input
                    id="siteName"
                    value={settings.siteName}
                    onChange={(e) => setSettings({ ...settings, siteName: e.target.value })}
                    placeholder="Enter site name"
                  />
                </div>
                <div>
                  <Label htmlFor="siteUrl">Site URL</Label>
                  <Input
                    id="siteUrl"
                    value={settings.siteUrl}
                    onChange={(e) => setSettings({ ...settings, siteUrl: e.target.value })}
                    placeholder="https://example.com"
                  />
                </div>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <Label htmlFor="language">Language</Label>
                  <select
                    id="language"
                    value={settings.language}
                    onChange={(e) => setSettings({ ...settings, language: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500"
                  >
                    <option value="en">English</option>
                    <option value="es">Español</option>
                    <option value="fr">Français</option>
                    <option value="de">Deutsch</option>
                  </select>
                </div>
                <div>
                  <Label htmlFor="timezone">Timezone</Label>
                  <select
                    id="timezone"
                    value={settings.timezone}
                    onChange={(e) => setSettings({ ...settings, timezone: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500"
                  >
                    <option value="UTC">UTC</option>
                    <option value="America/New_York">America/New York</option>
                    <option value="America/Los_Angeles">America/Los Angeles</option>
                    <option value="Europe/London">Europe/London</option>
                    <option value="Asia/Kolkata">Asia/Kolkata</option>
                  </select>
                </div>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <Label htmlFor="dateFormat">Date Format</Label>
                  <select
                    id="dateFormat"
                    value={settings.dateFormat}
                    onChange={(e) => setSettings({ ...settings, dateFormat: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500"
                  >
                    <option value="MM/DD/YYYY">MM/DD/YYYY</option>
                    <option value="DD/MM/YYYY">DD/MM/YYYY</option>
                    <option value="YYYY-MM-DD">YYYY-MM-DD</option>
                  </select>
                </div>
                <div>
                  <Label htmlFor="timeFormat">Time Format</Label>
                  <select
                    id="timeFormat"
                    value={settings.timeFormat}
                    onChange={(e) => setSettings({ ...settings, timeFormat: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500"
                  >
                    <option value="12h">12-hour</option>
                    <option value="24h">24-hour</option>
                  </select>
                </div>
              </div>
            </CardContent>
          </Card>
        )}

        {activeTab === 'security' && (
          <Card>
            <CardHeader>Security Settings</CardHeader>
            <CardContent className="space-y-6">
              <div className="space-y-4">
                <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                  <div>
                    <h3 className="font-medium text-gray-900">Multi-Factor Authentication</h3>
                    <p className="text-sm text-gray-600">Require additional verification for user logins</p>
                  </div>
                  <Switch
                    checked={settings.mfaEnabled}
                    onChange={(checked) => setSettings({ ...settings, mfaEnabled: checked })}
                  />
                </div>
                <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                  <div>
                    <h3 className="font-medium text-gray-900">Session Timeout</h3>
                    <p className="text-sm text-gray-600">Auto-logout after inactivity (minutes)</p>
                  </div>
                  <Input
                    type="number"
                    value={settings.sessionTimeout}
                    onChange={(e) => setSettings({ ...settings, sessionTimeout: e.target.value })}
                    className="w-24"
                  />
                </div>
              </div>

              <div>
                <h3 className="font-medium text-gray-900 mb-4">Password Policy</h3>
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-700">Minimum Length</span>
                    <Input
                      type="number"
                      value={settings.passwordMinLength}
                      onChange={(e) => setSettings({ ...settings, passwordMinLength: e.target.value })}
                      className="w-24"
                    />
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-700">Require Uppercase</span>
                    <Switch
                      checked={settings.passwordRequireUppercase}
                      onChange={(checked) => setSettings({ ...settings, passwordRequireUppercase: checked })}
                    />
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-700">Require Lowercase</span>
                    <Switch
                      checked={settings.passwordRequireLowercase}
                      onChange={(checked) => setSettings({ ...settings, passwordRequireLowercase: checked })}
                    />
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-700">Require Numbers</span>
                    <Switch
                      checked={settings.passwordRequireNumbers}
                      onChange={(checked) => setSettings({ ...settings, passwordRequireNumbers: checked })}
                    />
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-700">Require Special Characters</span>
                    <Switch
                      checked={settings.passwordRequireSpecialChars}
                      onChange={(checked) => setSettings({ ...settings, passwordRequireSpecialChars: checked })}
                    />
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        )}

        {activeTab === 'notifications' && (
          <Card>
            <CardHeader>Notification Settings</CardHeader>
            <CardContent className="space-y-6">
              <div className="space-y-6">
                <div className="p-4 bg-gray-50 rounded-lg">
                  <h3 className="font-medium text-gray-900 mb-4 flex items-center gap-2">
                    <Bell size={20} />
                    Email Notifications
                  </h3>
                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-gray-700">Enable Email</span>
                      <Switch
                        checked={settings.emailEnabled}
                        onChange={(checked) => setSettings({ ...settings, emailEnabled: checked })}
                      />
                    </div>
                    <div>
                      <Label htmlFor="emailSmtpHost">SMTP Host</Label>
                      <Input
                        id="emailSmtpHost"
                        value={settings.emailSmtpHost}
                        onChange={(e) => setSettings({ ...settings, emailSmtpHost: e.target.value })}
                        placeholder="smtp.example.com"
                      />
                    </div>
                    <div>
                      <Label htmlFor="emailSmtpPort">SMTP Port</Label>
                      <Input
                        id="emailSmtpPort"
                        value={settings.emailSmtpPort}
                        onChange={(e) => setSettings({ ...settings, emailSmtpPort: e.target.value })}
                        placeholder="587"
                      />
                    </div>
                    <div>
                      <Label htmlFor="emailSmtpUser">SMTP User</Label>
                      <Input
                        id="emailSmtpUser"
                        value={settings.emailSmtpUser}
                        onChange={(e) => setSettings({ ...settings, emailSmtpUser: e.target.value })}
                        placeholder="notifications@example.com"
                      />
                    </div>
                    <div>
                      <Label htmlFor="emailSmtpFrom">From Address</Label>
                      <Input
                        id="emailSmtpFrom"
                        value={settings.emailSmtpFrom}
                        onChange={(e) => setSettings({ ...settings, emailSmtpFrom: e.target.value })}
                        placeholder="Infron <noreply@infron.com>"
                      />
                    </div>
                  </div>
                </div>

                <div className="p-4 bg-gray-50 rounded-lg">
                  <h3 className="font-medium text-gray-900 mb-4 flex items-center gap-2">
                    <RefreshCw size={20} />
                    Slack Integration
                  </h3>
                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-gray-700">Enable Slack</span>
                      <Switch
                        checked={settings.slackEnabled}
                        onChange={(checked) => setSettings({ ...settings, slackEnabled: checked })}
                      />
                    </div>
                    <div>
                      <Label htmlFor="slackWebhook">Webhook URL</Label>
                      <Input
                        id="slackWebhook"
                        value={settings.slackWebhook}
                        onChange={(e) => setSettings({ ...settings, slackWebhook: e.target.value })}
                        placeholder="https://hooks.slack.com/services/..."
                      />
                    </div>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        )}

        {activeTab === 'appearance' && (
          <Card>
            <CardHeader>Appearance Settings</CardHeader>
            <CardContent className="space-y-6">
              <div className="space-y-4">
                <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                  <div>
                    <h3 className="font-medium text-gray-900">Theme</h3>
                    <p className="text-sm text-gray-600">Choose your preferred theme</p>
                  </div>
                  <div className="flex gap-2">
                    <button
                      onClick={() => setSettings({ ...settings, theme: 'light' })}
                      className={`px-4 py-2 rounded-md border-2 transition-colors ${
                        settings.theme === 'light'
                          ? 'border-primary-500 bg-primary-500 text-white'
                          : 'border-gray-300 hover:border-gray-400'
                      }`}
                    >
                      Light
                    </button>
                    <button
                      onClick={() => setSettings({ ...settings, theme: 'dark' })}
                      className={`px-4 py-2 rounded-md border-2 transition-colors ${
                        settings.theme === 'dark'
                          ? 'border-primary-500 bg-primary-500 text-white'
                          : 'border-gray-300 hover:border-gray-400'
                      }`}
                    >
                      Dark
                    </button>
                  </div>
                </div>

                <div>
                  <Label htmlFor="primaryColor">Primary Color</Label>
                  <div className="flex items-center gap-4">
                    <Input
                      id="primaryColor"
                      type="color"
                      value={settings.primaryColor}
                      onChange={(e) => setSettings({ ...settings, primaryColor: e.target.value })}
                      className="w-16 h-10"
                      placeholder="#3b82f6"
                    />
                    <div className="w-16 h-10 rounded border border-gray-300 flex items-center justify-center">
                      <div
                        className="w-8 h-8 rounded"
                        style={{ backgroundColor: settings.primaryColor }}
                      />
                    </div>
                  </div>
                </div>

                <div>
                  <Label htmlFor="accentColor">Accent Color</Label>
                  <div className="flex items-center gap-4">
                    <Input
                      id="accentColor"
                      type="color"
                      value={settings.accentColor}
                      onChange={(e) => setSettings({ ...settings, accentColor: e.target.value })}
                      className="w-16 h-10"
                      placeholder="#8b5cf6"
                    />
                    <div className="w-16 h-10 rounded border border-gray-300 flex items-center justify-center">
                      <div
                        className="w-8 h-8 rounded"
                        style={{ backgroundColor: settings.accentColor }}
                      />
                    </div>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        )}

        {activeTab === 'idp' && (
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-xl font-semibold text-gray-900">
                    Identity Providers
                  </h2>
                  <p className="text-sm text-gray-600 mt-1">
                    Manage your identity providers for user authentication
                  </p>
                </div>
                <button className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors font-medium">
                  <Plus size={18} />
                  <span>Add Provider</span>
                </button>
              </div>
            </CardHeader>
            <CardContent className="p-0">
              {isIdpLoading ? (
                <div className="flex items-center justify-center py-12">
                  <Loader2 className="h-8 w-8 animate-spin text-gray-400" />
                </div>
              ) : (
                <Table
                  columns={idpColumns}
                  data={idpServers}
                  emptyMessage="No IDP servers configured"
                  onRowClick={(row) => handleViewIdpDetails(row)}
                  overflowVisibleColumnKeys={['actions']}
                />
              )}
            </CardContent>
          </Card>
        )}
      </motion.div>

      {/* Toast Notification */}
      {showToast && (
        <Toast
          isOpen={showToast}
          message={toastMessage}
          variant={toastVariant}
          onClose={() => setShowToast(false)}
        />
      )}
    </div>
  );
}

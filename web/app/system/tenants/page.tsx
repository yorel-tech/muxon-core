'use client';

import { useState, useEffect } from 'react';
import { Card, CardContent } from '@/components/ui/atoms/card';
import { Table, Column } from '@/components/ui/organisms/table';
import { Badge } from '@/components/ui/atoms/badge';
import { Dropdown, DropdownOption } from '@/components/ui/molecules/dropdown';
import { Button } from '@/components/ui/atoms/button';
import { Input } from '@/components/ui/atoms/input';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Plus,
  Loader2,
  X,
  CheckCircle2,
  ChevronRight,
  ChevronLeft,
} from 'lucide-react';
import { RowActionsTrigger } from '@/components/DynamicContextMenu';
import { apiGet, apiPost } from '@/lib/api';
import { executeLinkAction } from '@/lib/api';
import { buildRowActionOptions, normalizeEntityLinks, getNavigationPath } from '@/lib/hateoas';
import type { Link } from '@/types/provider';

export interface Tenant extends Record<string, any> {
  id: string;
  name: string;
  displayName?: string;
  slug?: string;
  status: 'active' | 'inactive' | 'suspended' | 'pending';
  users: number;
  datacenters: number;
  vms: number;
  createdAt: string;
  _links?: Link[];
  settings: {
    idpId?: string;
    quotas: {
      vms: number;
      vcpus: number;
      memory: number;
      storage: number;
    };
  };
  [key: string]: any;
}


/** TenantCreate model per OpenAPI: name (required), displayName (optional), metadata (optional) */
export interface TenantCreateForm {
  name: string;
  displayName: string;
  metadata: Record<string, string>;
}

const WIZARD_STEPS = ['Basic info', 'Metadata (optional)'] as const;

export default function TenantsPage() {
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Add tenant wizard state
  const [isWizardOpen, setIsWizardOpen] = useState(false);
  const [wizardStep, setWizardStep] = useState(0);
  const [isSaving, setIsSaving] = useState(false);
  const [wizardError, setWizardError] = useState<string | null>(null);
  const [createForm, setCreateForm] = useState<TenantCreateForm>({
    name: '',
    displayName: '',
    metadata: {},
  });
  const [metadataEntries, setMetadataEntries] = useState<{ key: string; value: string }[]>([{ key: '', value: '' }]);

  // Fetch tenants on mount
  useEffect(() => {
    fetchTenants();
  }, []);

  const fetchTenants = async () => {
    setIsLoading(true);
    try {
      const data = await apiGet('/api/v1/tenants');
      const tenantsList = Array.isArray(data) ? data : (data?.items || []);
      
      // If API fails or returns empty, use mock data for testing
      if (tenantsList.length === 0) {
        setTenants([
          {
            id: '1',
            name: 'Acme Corporation',
            slug: 'acme-corp',
            status: 'active',
            users: 12,
            datacenters: 2,
            vms: 15,
            createdAt: '2024-01-15T10:00:00Z',
            settings: {
              idpId: 'idp-1',
              quotas: {
                vms: 50,
                vcpus: 200,
                memory: 512,
                storage: 2048,
              },
            },
          },
          {
            id: '2',
            name: 'TechStart Inc',
            slug: 'techstart',
            status: 'active',
            users: 5,
            datacenters: 1,
            vms: 8,
            createdAt: '2024-02-01T14:30:00Z',
            settings: {
              idpId: 'idp-1',
              quotas: {
                vms: 25,
                vcpus: 100,
                memory: 256,
                storage: 1024,
              },
            },
          },
          {
            id: '3',
            name: 'Globex Industries',
            slug: 'globex',
            status: 'active',
            users: 23,
            datacenters: 3,
            vms: 42,
            createdAt: '2023-11-20T09:15:00Z',
            settings: {
              idpId: 'idp-2',
              quotas: {
                vms: 100,
                vcpus: 500,
                memory: 1024,
                storage: 4096,
              },
            },
          },
          {
            id: '4',
            name: 'Startup Labs',
            slug: 'startup-labs',
            status: 'pending',
            users: 3,
            datacenters: 0,
            vms: 0,
            createdAt: '2024-01-28T16:45:00Z',
            settings: {
              idpId: 'idp-1',
              quotas: {
                vms: 10,
                vcpus: 40,
                memory: 128,
                storage: 512,
              },
            },
          },
        ]);
      } else {
        setTenants(
          tenantsList.map((t: any) => ({ ...t, _links: normalizeEntityLinks(t) }))
        );
      }
    } catch (error) {
      console.error('Error fetching tenants:', error);
      // Use mock data even on error for testing
      setTenants([
        {
          id: '1',
          name: 'Acme Corporation',
          slug: 'acme-corp',
          status: 'active',
          users: 12,
          datacenters: 2,
          vms: 15,
          createdAt: '2024-01-15T10:00:00Z',
          settings: {
            idpId: 'idp-1',
            quotas: {
              vms: 50,
              vcpus: 200,
              memory: 512,
              storage: 2048,
            },
          },
        },
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleOpenWizard = () => {
    setIsWizardOpen(true);
    setWizardStep(0);
    setWizardError(null);
    setCreateForm({ name: '', displayName: '', metadata: {} });
    setMetadataEntries([{ key: '', value: '' }]);
  };

  const handleCloseWizard = () => {
    if (!isSaving) {
      setIsWizardOpen(false);
      setWizardStep(0);
      setWizardError(null);
    }
  };

  const handleSaveTenant = async () => {
    const name = createForm.name.trim();
    if (!name) {
      setWizardError('Tenant name is required.');
      return;
    }
    // Name should be slug-like (lowercase, alphanumeric, hyphens)
    if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(name.toLowerCase())) {
      setWizardError('Name must be a valid slug (e.g. acme-corp): lowercase letters, numbers, and hyphens only.');
      return;
    }

    setWizardError(null);
    setIsSaving(true);
    try {
      const metadata: Record<string, string> = {};
      metadataEntries.forEach(({ key, value }) => {
        const k = key.trim();
        if (k) metadata[k] = value.trim();
      });
      const payload = {
        name,
        displayName: createForm.displayName.trim() || undefined,
        ...(Object.keys(metadata).length > 0 ? { metadata } : {}),
      };
      await apiPost('/api/v1/tenants', payload);
      await fetchTenants();
      handleCloseWizard();
    } catch (error) {
      console.error('Error creating tenant:', error);
      let message = 'Failed to create tenant.';
      if (error instanceof Error && error.message) {
        const m = error.message;
        const jsonMatch = m.match(/\s-\s(\{.*\})$/);
        if (jsonMatch) {
          try {
            const body = JSON.parse(jsonMatch[1]) as { message?: string };
            if (body.message) message = body.message;
          } catch {
            message = m;
          }
        } else {
          message = m;
        }
      }
      setWizardError(message);
    } finally {
      setIsSaving(false);
    }
  };

  const addMetadataRow = () => setMetadataEntries((prev) => [...prev, { key: '', value: '' }]);
  const removeMetadataRow = (index: number) =>
    setMetadataEntries((prev) => (prev.length > 1 ? prev.filter((_, i) => i !== index) : prev));
  const updateMetadataEntry = (index: number, field: 'key' | 'value', value: string) =>
    setMetadataEntries((prev) => prev.map((e, i) => (i === index ? { ...e, [field]: value } : e)));

  const handleRowAction = async (rel: string, tenant: Tenant, link?: Link) => {
    if (!link) return;
    if (rel === 'delete' && !window.confirm(`Are you sure you want to delete tenant "${tenant.displayName ?? tenant.name}"?`)) {
      return;
    }
    try {
      if (rel === 'self' && link.method === 'GET') {
        const path = getNavigationPath(link);
        window.location.href = path.startsWith('http') ? path : path;
        return;
      }
      await executeLinkAction(link, link.method !== 'GET' && link.method !== 'DELETE' ? {} : undefined);
      if (['delete', 'edit', 'update'].includes(rel)) {
        await fetchTenants();
      }
    } catch (error) {
      console.error(`Tenant action ${rel} failed:`, error);
      alert(`Failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  };

  const getContextMenuOptions = (tenant: Tenant): DropdownOption[] =>
    buildRowActionOptions(tenant, 'tenant', normalizeEntityLinks(tenant), handleRowAction);

  const getStatusBadgeVariant = (status: Tenant['status']) => {
    switch (status) {
      case 'active':
        return 'success';
      case 'suspended':
        return 'warning';
      case 'inactive':
        return 'default';
      case 'pending':
        return 'info';
      default:
        return 'default';
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  const columns: Column<Tenant>[] = [
    {
      key: 'actions',
      header: '',
      cell: (row: Tenant) => (
        <div className="flex justify-start" onClick={(e) => e.stopPropagation()}>
          <Dropdown
            trigger={<RowActionsTrigger title="Actions" />}
            options={getContextMenuOptions(row)}
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
      cell: (row: Tenant) => (
        <div className="font-medium text-gray-900">{row.displayName ?? row.name}</div>
      ),
      sortable: true,
    },
    {
      key: 'users',
      header: 'Users',
      cell: (row: Tenant) => (
        <span className="text-gray-600">{row.users}</span>
      ),
      sortable: true,
    },
    {
      key: 'datacenters',
      header: 'DCs',
      cell: (row: Tenant) => (
        <span className="text-gray-600">{row.datacenters}</span>
      ),
      sortable: true,
    },
    {
      key: 'vms',
      header: 'VMs',
      cell: (row: Tenant) => (
        <span className="text-gray-600">{row.vms}</span>
      ),
      sortable: true,
    },
    {
      key: 'status',
      header: 'Status',
      cell: (row: Tenant) => (
        <Badge variant={getStatusBadgeVariant(row.status)}>
          {row.status}
        </Badge>
      ),
      sortable: true,
    },
  ];

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-full px-3 py-8">
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
                Tenants
              </h1>
              <p className="text-gray-600 mt-2">
                Manage organizations and their resources
              </p>
            </div>
            <button
              onClick={handleOpenWizard}
              className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors font-medium"
            >
              <Plus size={18} />
              <span>Add Tenant</span>
            </button>
          </div>
        </motion.div>

        {/* Tenants Table */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.1 }}
        >
          <Card>
            <CardContent className="p-0">
              {isLoading ? (
                <div className="flex items-center justify-center py-12">
                  <Loader2 className="h-8 w-8 animate-spin text-gray-400" />
                </div>
              ) : (
                <Table
                  columns={columns}
                  data={tenants}
                  emptyMessage="No tenants configured"
                  onRowClick={(row) => handleViewDetails(row)}
                  overflowVisibleColumnKeys={['actions']}
                />
              )}
            </CardContent>
          </Card>
        </motion.div>
      </div>

      {/* Add Tenant Wizard Modal */}
      <AnimatePresence mode="wait">
        {isWizardOpen && (
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
              className="bg-white rounded-xl shadow-2xl w-full max-w-xl max-h-[90vh] overflow-y-auto flex flex-col"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between">
                <h2 className="text-xl font-semibold text-gray-900">Add Tenant</h2>
                <button
                  onClick={handleCloseWizard}
                  disabled={isSaving}
                  className="p-2 rounded-lg hover:bg-gray-100 transition-colors disabled:opacity-50"
                >
                  <X className="h-5 w-5 text-gray-500" />
                </button>
              </div>

              {/* Step indicator */}
              <div className="px-6 py-2 border-b border-gray-100 flex gap-2">
                {WIZARD_STEPS.map((label, i) => (
                  <span
                    key={label}
                    className={`text-sm ${i === wizardStep ? 'font-medium text-primary-600' : 'text-gray-500'}`}
                  >
                    {i + 1}. {label}
                  </span>
                ))}
              </div>

              <div className="p-6 space-y-4 flex-1 overflow-y-auto">
                {wizardError && (
                  <div className="rounded-lg bg-red-50 border border-red-200 px-4 py-2 text-sm text-red-800">
                    {wizardError}
                  </div>
                )}

                {wizardStep === 0 && (
                  <div className="space-y-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        Name (slug) *
                      </label>
                      <Input
                        type="text"
                        placeholder="acme-corp"
                        value={createForm.name}
                        onChange={(e) => setCreateForm((f) => ({ ...f, name: e.target.value }))}
                      />
                      <p className="mt-1 text-xs text-gray-500">
                        Unique identifier: lowercase letters, numbers, hyphens only (e.g. acme-corp).
                      </p>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        Display name
                      </label>
                      <Input
                        type="text"
                        placeholder="ACME Corporation"
                        value={createForm.displayName}
                        onChange={(e) => setCreateForm((f) => ({ ...f, displayName: e.target.value }))}
                      />
                    </div>
                  </div>
                )}

                {wizardStep === 1 && (
                  <div className="space-y-3">
                    <p className="text-sm text-gray-600">
                      Add optional key-value metadata for this tenant.
                    </p>
                    {metadataEntries.map((entry, index) => (
                      <div key={index} className="flex gap-2 items-center">
                        <Input
                          type="text"
                          placeholder="Key"
                          value={entry.key}
                          onChange={(e) => updateMetadataEntry(index, 'key', e.target.value)}
                          className="flex-1"
                        />
                        <Input
                          type="text"
                          placeholder="Value"
                          value={entry.value}
                          onChange={(e) => updateMetadataEntry(index, 'value', e.target.value)}
                          className="flex-1"
                        />
                        <button
                          type="button"
                          onClick={() => removeMetadataRow(index)}
                          className="p-2 text-gray-500 hover:text-red-600 rounded"
                          aria-label="Remove row"
                        >
                          <Trash2 size={18} />
                        </button>
                      </div>
                    ))}
                    <Button variant="secondary" onClick={addMetadataRow} type="button">
                      Add metadata row
                    </Button>
                  </div>
                )}
              </div>

              <div className="sticky bottom-0 bg-white border-t border-gray-200 px-6 py-4 flex justify-between">
                <Button
                  variant="secondary"
                  onClick={() => (wizardStep > 0 ? setWizardStep((s) => s - 1) : handleCloseWizard())}
                  disabled={isSaving}
                >
                  {wizardStep > 0 ? (
                    <>
                      <ChevronLeft size={18} className="mr-1" />
                      Back
                    </>
                  ) : (
                    'Cancel'
                  )}
                </Button>
                {wizardStep < WIZARD_STEPS.length - 1 ? (
                  <Button
                    onClick={() => {
                      const name = createForm.name.trim();
                      if (!name) setWizardError('Tenant name is required.');
                      else if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(name.toLowerCase())) {
                        setWizardError('Name must be a valid slug (e.g. acme-corp).');
                      } else {
                        setWizardError(null);
                        setWizardStep((s) => s + 1);
                      }
                    }}
                    disabled={isSaving}
                  >
                    Next
                    <ChevronRight size={18} className="ml-1" />
                  </Button>
                ) : (
                  <Button onClick={handleSaveTenant} disabled={isSaving}>
                    {isSaving ? (
                      <>
                        <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                        Creating...
                      </>
                    ) : (
                      <>
                        Create tenant
                        <CheckCircle2 size={18} className="ml-1" />
                      </>
                    )}
                  </Button>
                )}
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

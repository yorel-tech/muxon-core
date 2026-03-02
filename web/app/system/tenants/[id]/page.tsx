'use client';

import { useState, useEffect, use } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Card, CardContent, CardHeader } from '@/components/ui/atoms/card';
import { Button } from '@/components/ui/atoms/button';
import { Input } from '@/components/ui/atoms/input';
import { Badge } from '@/components/ui/atoms/badge';
import { Table, Column } from '@/components/ui/organisms/table';
import { Dropdown, DropdownOption } from '@/components/ui/molecules/dropdown';
import { RowActionsTrigger } from '@/components/DynamicContextMenu';
import { motion, AnimatePresence } from 'framer-motion';
import {
  ArrowLeft,
  Loader2,
  Pencil,
  Trash2,
  Building2,
  X,
  BarChart3,
  Server,
  Users,
  Database,
  Shield,
  UserPlus,
  FileText,
  Plus,
  Settings,
  Sliders,
  ExternalLink,
} from 'lucide-react';
import { apiGet, apiPut, apiPost, apiPatch, apiDelete, executeLinkAction } from '@/lib/api';
import { findLink, normalizeEntityLinks } from '@/lib/hateoas';
import { DetailRow, formatDetailDate } from '@/components/entity-detail/DetailRow';
import { Tabs } from '@/components/ui/molecules/tabs';
import type { Link as HateoasLink } from '@/types/provider';

interface TenantDetail {
  id: string;
  name: string;
  displayName?: string;
  description?: string;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
  metadata?: Record<string, string>;
  _links?: HateoasLink[];
}

/** Matches API TenantDatacenterGrant: datacenter is read-only (name = datacenter name) */
interface TenantDatacenterGrant {
  tenantId?: string;
  datacenterId: string;
  datacenter?: { id: string; name?: string; description?: string };
  access?: boolean;
  limits?: ResourceLimits;
  overrideSettings?: DatacenterSettings;
  enabledFeatures?: string[];
  createdAt?: string;
  updatedAt?: string;
}

interface TenantDatacenterGrantList {
  items?: TenantDatacenterGrant[];
  total?: number;
  page?: number;
  perPage?: number;
}

interface IdpSettings {
  type?: string;
  issuerUrl?: string;
  metadataUrl?: string;
  autoProvisionUsers?: boolean;
  justInTimeProvisioning?: boolean;
  mfaEnforced?: boolean;
}

interface TenantSettings {
  idp?: IdpSettings;
  notifications?: unknown;
  lease?: unknown;
  audit?: { auditRetentionDays?: number; enableApiAudit?: boolean };
}

interface TenantUser {
  user_id: string;
  username?: string;
  email?: string;
  display_name?: string;
  role_name?: string;
  scope_type?: string;
  scope_id?: string | null;
  expires_at?: string | null;
}

interface UserListResponse {
  items?: TenantUser[];
  total?: number;
  page?: number;
  perPage?: number;
}

/** ResourceLimits per OpenAPI commons */
interface ResourceLimits {
  maxCpus?: number;
  maxMemoryGb?: number;
  maxStorageGb?: number;
  maxVms?: number;
  maxVolumes?: number;
  maxLoadBalancers?: number;
}

/** DatacenterSettings override on grant (optional fields) */
interface DatacenterSettings {
  vmClasses?: string[];
  storageClasses?: string[];
  networkDomains?: string[];
  providerSpecificSettings?: Record<string, unknown>;
}

interface DatacenterListItem {
  id: string;
  name: string;
  description?: string;
  capacity?: { totalCpus?: number; totalMemoryGb?: number; totalStorageGb?: number };
}

export default function TenantDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const router = useRouter();
  const [tenant, setTenant] = useState<TenantDetail | null>(null);
  const [grants, setGrants] = useState<TenantDatacenterGrantList | null>(null);
  const [settings, setSettings] = useState<TenantSettings | null>(null);
  const [userList, setUserList] = useState<UserListResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [editModal, setEditModal] = useState(false);
  const [editDisplayName, setEditDisplayName] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [userRoleModal, setUserRoleModal] = useState<{ user: TenantUser; newRole: string } | null>(null);
  const [addUserModal, setAddUserModal] = useState(false);
  const [addDatacenterModal, setAddDatacenterModal] = useState(false);

  // Add Datacenter wizard state
  const [addDcWizardStep, setAddDcWizardStep] = useState(0);
  const [availableDatacenters, setAvailableDatacenters] = useState<DatacenterListItem[]>([]);
  const [addDcLoadingDatacenters, setAddDcLoadingDatacenters] = useState(false);
  const [selectedDatacenterId, setSelectedDatacenterId] = useState<string | null>(null);
  const [grantAccess, setGrantAccess] = useState(true);
  const [limitMaxCpus, setLimitMaxCpus] = useState<string>('');
  const [limitMaxMemoryGb, setLimitMaxMemoryGb] = useState<string>('');
  const [limitMaxStorageGb, setLimitMaxStorageGb] = useState<string>('');
  const [limitMaxVms, setLimitMaxVms] = useState<string>('');
  const [limitMaxVolumes, setLimitMaxVolumes] = useState<string>('');
  const [limitMaxLoadBalancers, setLimitMaxLoadBalancers] = useState<string>('');
  const [addDcError, setAddDcError] = useState<string | null>(null);
  const [addDcSubmitting, setAddDcSubmitting] = useState(false);
  const [editGrantLimitsModal, setEditGrantLimitsModal] = useState<TenantDatacenterGrant | null>(null);
  const [editGrantSettingsModal, setEditGrantSettingsModal] = useState<TenantDatacenterGrant | null>(null);
  const [editLimitsForm, setEditLimitsForm] = useState<Record<string, string>>({});
  const [editSettingsForm, setEditSettingsForm] = useState<{ vmClassesStr: string; storageClassesStr: string; networkDomainsStr: string }>({ vmClassesStr: '', storageClassesStr: '', networkDomainsStr: '' });
  const [editGrantSubmitting, setEditGrantSubmitting] = useState(false);
  const [editGrantError, setEditGrantError] = useState<string | null>(null);

  const links = normalizeEntityLinks(tenant ?? {});
  const updateLink = findLink(links, 'update');
  const deleteLink = findLink(links, 'delete');

  const fetchTenant = async () => {
    const data = await apiGet(`/api/v1/tenants/${id}`);
    setTenant(data as TenantDetail);
  };

  const fetchGrants = async () => {
    try {
      const data = await apiGet(`/api/v1/tenants/${id}/datacenters?perPage=100`);
      setGrants(data as TenantDatacenterGrantList);
    } catch {
      setGrants({ items: [], total: 0 });
    }
  };

  const fetchSettings = async () => {
    try {
      const data = await apiGet(`/api/v1/tenants/${id}/settings`);
      setSettings(data as TenantSettings);
    } catch {
      setSettings(null);
    }
  };

  const fetchUsers = async () => {
    try {
      const data = await apiGet(`/api/v1/tenants/${id}/users?perPage=100`);
      setUserList(data as UserListResponse);
    } catch {
      setUserList({ items: [], total: 0 });
    }
  };

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const data = await apiGet(`/api/v1/tenants/${id}`);
        if (!cancelled) setTenant(data as TenantDetail);
        await Promise.all([
          fetchGrants().then(() => {}),
          fetchSettings().then(() => {}),
          fetchUsers().then(() => {}),
        ]);
      } catch (e) {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : 'Failed to load tenant');
          setTenant(null);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, [id]);

  const refetch = async () => {
    await fetchTenant();
    await fetchGrants();
    await fetchSettings();
    await fetchUsers();
  };

  const handleUpdate = async () => {
    if (!updateLink || !tenant) return;
    setActionLoading('update');
    try {
      await executeLinkAction(updateLink, {
        displayName: editDisplayName || undefined,
        description: editDescription || undefined,
      });
      setEditModal(false);
      await refetch();
    } catch (e) {
      alert(e instanceof Error ? e.message : 'Update failed');
    } finally {
      setActionLoading(null);
    }
  };

  const handleDelete = async () => {
    if (!deleteLink || !tenant) return;
    if (!confirm(`Delete tenant "${tenant.displayName || tenant.name}"? This cannot be undone.`)) return;
    setActionLoading('delete');
    try {
      await executeLinkAction(deleteLink);
      window.location.href = '/system/tenants';
    } catch (e) {
      alert(e instanceof Error ? e.message : 'Delete failed');
      setActionLoading(null);
    }
  };

  const openAddDatacenterModal = () => {
    setAddDatacenterModal(true);
    setAddDcWizardStep(0);
    setSelectedDatacenterId(null);
    setGrantAccess(true);
    setLimitMaxCpus('');
    setLimitMaxMemoryGb('');
    setLimitMaxStorageGb('');
    setLimitMaxVms('');
    setLimitMaxVolumes('');
    setLimitMaxLoadBalancers('');
    setAddDcError(null);
    setAddDcSubmitting(false);
    setAvailableDatacenters([]);
    setAddDcLoadingDatacenters(true);
    (async () => {
      try {
        const data = await apiGet<{ items?: DatacenterListItem[] }>('/api/v1/datacenters?perPage=200');
        const items = data?.items ?? [];
        const grantedIds = new Set((grants?.items ?? []).map((g) => g.datacenterId));
        setAvailableDatacenters(items.filter((dc) => !grantedIds.has(dc.id)));
      } catch {
        setAvailableDatacenters([]);
      } finally {
        setAddDcLoadingDatacenters(false);
      }
    })();
  };

  const closeAddDatacenterModal = () => {
    setAddDatacenterModal(false);
    setAddDcWizardStep(0);
    setAddDcError(null);
  };

  const parseOptionalInt = (s: string): number | undefined => {
    const v = s.trim();
    if (v === '') return undefined;
    const n = parseInt(v, 10);
    return Number.isNaN(n) || n < 0 ? undefined : n;
  };

  const handleCreateTenantDatacenterGrant = async () => {
    if (!selectedDatacenterId) return;
    setAddDcSubmitting(true);
    setAddDcError(null);
    try {
      const limits: ResourceLimits = {};
      const maxCpus = parseOptionalInt(limitMaxCpus);
      const maxMemoryGb = parseOptionalInt(limitMaxMemoryGb);
      const maxStorageGb = parseOptionalInt(limitMaxStorageGb);
      const maxVms = parseOptionalInt(limitMaxVms);
      const maxVolumes = parseOptionalInt(limitMaxVolumes);
      const maxLoadBalancers = parseOptionalInt(limitMaxLoadBalancers);
      if (maxCpus != null) limits.maxCpus = maxCpus;
      if (maxMemoryGb != null) limits.maxMemoryGb = maxMemoryGb;
      if (maxStorageGb != null) limits.maxStorageGb = maxStorageGb;
      if (maxVms != null) limits.maxVms = maxVms;
      if (maxVolumes != null) limits.maxVolumes = maxVolumes;
      if (maxLoadBalancers != null) limits.maxLoadBalancers = maxLoadBalancers;

      const body = {
        tenantId: id,
        datacenterId: selectedDatacenterId,
        access: grantAccess,
        ...(Object.keys(limits).length > 0 ? { limits } : {}),
      };
      await apiPost(`/api/v1/tenants/${id}/datacenters`, body);
      await fetchGrants();
      closeAddDatacenterModal();
    } catch (e) {
      setAddDcError(e instanceof Error ? e.message : 'Failed to add datacenter');
    } finally {
      setAddDcSubmitting(false);
    }
  };

  const openEditGrantLimits = (g: TenantDatacenterGrant) => {
    setEditGrantLimitsModal(g);
    setEditLimitsForm({
      maxCpus: g.limits?.maxCpus?.toString() ?? '',
      maxMemoryGb: g.limits?.maxMemoryGb?.toString() ?? '',
      maxStorageGb: g.limits?.maxStorageGb?.toString() ?? '',
      maxVms: g.limits?.maxVms?.toString() ?? '',
      maxVolumes: g.limits?.maxVolumes?.toString() ?? '',
      maxLoadBalancers: g.limits?.maxLoadBalancers?.toString() ?? '',
    });
    setEditGrantError(null);
  };

  const openEditGrantSettings = (g: TenantDatacenterGrant) => {
    setEditGrantSettingsModal(g);
    setEditSettingsForm({
      vmClassesStr: g.overrideSettings?.vmClasses?.join(', ') ?? '',
      storageClassesStr: g.overrideSettings?.storageClasses?.join(', ') ?? '',
      networkDomainsStr: g.overrideSettings?.networkDomains?.join(', ') ?? '',
    });
    setEditGrantError(null);
  };

  const handleUpdateGrantLimits = async () => {
    const g = editGrantLimitsModal;
    if (!g || !id) return;
    setEditGrantSubmitting(true);
    setEditGrantError(null);
    try {
      const limits: ResourceLimits = {};
      const maxCpus = parseOptionalInt(editLimitsForm.maxCpus ?? '');
      const maxMemoryGb = parseOptionalInt(editLimitsForm.maxMemoryGb ?? '');
      const maxStorageGb = parseOptionalInt(editLimitsForm.maxStorageGb ?? '');
      const maxVms = parseOptionalInt(editLimitsForm.maxVms ?? '');
      const maxVolumes = parseOptionalInt(editLimitsForm.maxVolumes ?? '');
      const maxLoadBalancers = parseOptionalInt(editLimitsForm.maxLoadBalancers ?? '');
      if (maxCpus != null) limits.maxCpus = maxCpus;
      if (maxMemoryGb != null) limits.maxMemoryGb = maxMemoryGb;
      if (maxStorageGb != null) limits.maxStorageGb = maxStorageGb;
      if (maxVms != null) limits.maxVms = maxVms;
      if (maxVolumes != null) limits.maxVolumes = maxVolumes;
      if (maxLoadBalancers != null) limits.maxLoadBalancers = maxLoadBalancers;
      await apiPatch(`/api/v1/tenants/${id}/datacenters/${g.datacenterId}`, { tenantId: id, datacenterId: g.datacenterId, access: g.access ?? true, limits, enabledFeatures: g.enabledFeatures ?? [], overrideSettings: g.overrideSettings });
      setEditGrantLimitsModal(null);
      await fetchGrants();
    } catch (e) {
      setEditGrantError(e instanceof Error ? e.message : 'Failed to update limits');
    } finally {
      setEditGrantSubmitting(false);
    }
  };

  const handleUpdateGrantSettings = async () => {
    const g = editGrantSettingsModal;
    if (!g || !id) return;
    setEditGrantSubmitting(true);
    setEditGrantError(null);
    try {
      const overrideSettings: DatacenterSettings = {
        vmClasses: editSettingsForm.vmClassesStr.split(',').map((s) => s.trim()).filter(Boolean),
        storageClasses: editSettingsForm.storageClassesStr.split(',').map((s) => s.trim()).filter(Boolean),
        networkDomains: editSettingsForm.networkDomainsStr.split(',').map((s) => s.trim()).filter(Boolean),
      };
      await apiPatch(`/api/v1/tenants/${id}/datacenters/${g.datacenterId}`, { tenantId: id, datacenterId: g.datacenterId, access: g.access ?? true, limits: g.limits, enabledFeatures: g.enabledFeatures ?? [], overrideSettings });
      setEditGrantSettingsModal(null);
      await fetchGrants();
    } catch (e) {
      setEditGrantError(e instanceof Error ? e.message : 'Failed to update settings');
    } finally {
      setEditGrantSubmitting(false);
    }
  };

  const handleUpdateUserRole = async () => {
    if (!userRoleModal || !id) return;
    setActionLoading(`role-${userRoleModal.user.user_id}`);
    try {
      await apiPut(`/api/v1/tenants/${id}/users/${userRoleModal.user.user_id}`, {
        role_name: userRoleModal.newRole,
      });
      setUserRoleModal(null);
      await fetchUsers();
    } catch (e) {
      alert(e instanceof Error ? e.message : 'Update role failed');
    } finally {
      setActionLoading(null);
    }
  };

  const handleRemoveUser = async (userId: string) => {
    if (!confirm('Remove this user from the tenant?')) return;
    setActionLoading(`del-${userId}`);
    try {
      await apiDelete(`/api/v1/tenants/${id}/users/${userId}`);
      await fetchUsers();
    } catch (e) {
      alert(e instanceof Error ? e.message : 'Remove failed');
    } finally {
      setActionLoading(null);
    }
  };

  const openEdit = () => {
    setEditDisplayName(tenant?.displayName ?? '');
    setEditDescription(tenant?.description ?? '');
    setEditModal(true);
  };

  const getStatusVariant = (status: string | undefined) => {
    switch (status) {
      case 'active':
        return 'success';
      case 'inactive':
        return 'default';
      case 'suspended':
        return 'warning';
      default:
        return 'default';
    }
  };

  const getGrantContextMenuOptions = (g: TenantDatacenterGrant): DropdownOption[] => [
    { label: 'Edit limits', icon: <Sliders className="w-4 h-4" />, onClick: () => openEditGrantLimits(g) },
    { label: 'Edit settings', icon: <Settings className="w-4 h-4" />, onClick: () => openEditGrantSettings(g) },
    { label: 'View datacenter', icon: <ExternalLink className="w-4 h-4" />, onClick: () => router.push(`/system/datacenters/${g.datacenterId}`) },
  ];

  const grantColumns: Column<TenantDatacenterGrant>[] = [
    {
      key: 'actions',
      header: '',
      cell: (row: TenantDatacenterGrant) => (
        <div className="flex justify-start" onClick={(e) => e.stopPropagation()}>
          <Dropdown
            trigger={<RowActionsTrigger title="Actions" />}
            options={getGrantContextMenuOptions(row)}
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
      cell: (row: TenantDatacenterGrant) => (
        <div className="font-medium text-gray-900">{row.datacenter?.name ?? row.datacenterId}</div>
      ),
      sortable: true,
    },
    {
      key: 'description',
      header: 'Description',
      cell: (row: TenantDatacenterGrant) => (
        <span className="text-gray-600 text-sm">{row.datacenter?.description ?? '—'}</span>
      ),
      sortable: false,
    },
    {
      key: 'limits',
      header: 'Limits',
      cell: (row: TenantDatacenterGrant) => {
        const l = row.limits;
        if (!l || (l.maxVms == null && l.maxCpus == null && l.maxMemoryGb == null)) return <span className="text-gray-400">—</span>;
        const parts = [l.maxVms != null && `VMs: ${l.maxVms}`, l.maxCpus != null && `vCPUs: ${l.maxCpus}`, l.maxMemoryGb != null && `RAM: ${l.maxMemoryGb} GB`].filter(Boolean);
        return <span className="text-gray-600 text-sm">{parts.join(', ')}</span>;
      },
      sortable: false,
    },
  ];

  const usersTotal = userList?.total ?? userList?.items?.length ?? 0;
  const grantsTotal = grants?.total ?? grants?.items?.length ?? 0;
  const limits = settings?.idp ? undefined : (grants?.items?.[0]?.limits ?? {}); // first grant limits for overview
  const vmsUsed = 0; // placeholder until we have tenant VM count API
  const vcpusUsed = 0;
  const memoryUsedGb = 0;
  const storageUsedGb = 0;
  const vmsLimit = limits?.maxVms ?? 0;
  const vcpusLimit = limits?.maxCpus ?? 0;
  const memoryLimitGb = limits?.maxMemoryGb ?? 0;
  const storageLimitGb = limits?.maxStorageGb ?? 0;
  const quotaPct = vmsLimit > 0 ? Math.min(100, Math.round((vmsUsed / vmsLimit) * 100)) : 0;

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-gray-400" />
      </div>
    );
  }

  if (error || !tenant) {
    return (
      <div className="min-h-screen bg-gray-50 px-3 py-8">
        <Link href="/system/tenants" className="inline-flex items-center gap-2 text-gray-600 hover:text-gray-900 mb-6">
          <ArrowLeft size={20} />
          Back to Tenants
        </Link>
        <Card>
          <CardContent className="p-8 text-center">
            <p className="text-red-600">{error ?? 'Tenant not found'}</p>
            <p className="text-sm text-gray-500 mt-2">The requested tenant may not exist or you may not have access.</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-4xl mx-auto px-3 py-8">
        <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }} className="mb-6">
          <Link href="/system/tenants" className="inline-flex items-center gap-2 text-gray-600 hover:text-gray-900 transition-colors">
            <ArrowLeft size={20} />
            <span className="font-medium">Back to Tenants</span>
          </Link>
        </motion.div>

        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.05 }} className="mb-6">
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-2xl font-bold text-gray-900">{tenant.displayName || tenant.name}</h1>
            {tenant.status && <Badge variant={getStatusVariant(tenant.status)}>{tenant.status}</Badge>}
          </div>
          {tenant.description && <p className="text-gray-600 mt-1">{tenant.description}</p>}
        </motion.div>

        <Tabs
          variant="underline"
          defaultTab="overview"
          tabs={[
            {
              id: 'overview',
              label: 'Overview',
              icon: <BarChart3 className="h-4 w-4" />,
              content: (
                <Card bordered shadow="md">
                  <CardContent className="pt-6">
                    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                      <div className="flex items-center gap-3 p-3 rounded-lg bg-gray-50">
                        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-50"><Users className="h-5 w-5 text-blue-600" /></div>
                        <div><p className="text-sm text-gray-500">Users</p><p className="text-xl font-bold text-gray-900">{usersTotal}</p></div>
                      </div>
                      <div className="flex items-center gap-3 p-3 rounded-lg bg-gray-50">
                        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-purple-50"><Server className="h-5 w-5 text-purple-600" /></div>
                        <div><p className="text-sm text-gray-500">Datacenters</p><p className="text-xl font-bold text-gray-900">{grantsTotal}</p></div>
                      </div>
                      <div className="flex items-center gap-3 p-3 rounded-lg bg-gray-50">
                        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-green-50"><Database className="h-5 w-5 text-green-600" /></div>
                        <div><p className="text-sm text-gray-500">VMs</p><p className="text-xl font-bold text-gray-900">{vmsUsed}</p></div>
                      </div>
                      <div className="flex items-center gap-3 p-3 rounded-lg bg-gray-50">
                        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-orange-50"><BarChart3 className="h-5 w-5 text-orange-600" /></div>
                        <div><p className="text-sm text-gray-500">Quota usage</p><p className="text-xl font-bold text-gray-900">{vmsLimit > 0 ? `${quotaPct}%` : '—'}</p></div>
                      </div>
                    </div>
                    {(vmsLimit > 0 || vcpusLimit > 0 || memoryLimitGb > 0 || storageLimitGb > 0) && (
                      <div className="mt-4 space-y-3 pt-4 border-t border-gray-100">
                        {vmsLimit > 0 && (<div><div className="flex justify-between text-sm mb-1"><span className="font-medium text-gray-700">VMs</span><span className="text-gray-500">{vmsUsed} / {vmsLimit}</span></div><div className="w-full bg-gray-200 rounded-full h-2"><div className="bg-primary-600 h-2 rounded-full" style={{ width: `${quotaPct}%` }} /></div></div>)}
                        {vcpusLimit > 0 && (<div><div className="flex justify-between text-sm mb-1"><span className="font-medium text-gray-700">vCPUs</span><span className="text-gray-500">{vcpusUsed} / {vcpusLimit}</span></div><div className="w-full bg-gray-200 rounded-full h-2"><div className="bg-purple-600 h-2 rounded-full" style={{ width: `${vcpusLimit ? Math.min(100, Math.round((vcpusUsed / vcpusLimit) * 100)) : 0}%` }} /></div></div>)}
                        {memoryLimitGb > 0 && (<div><div className="flex justify-between text-sm mb-1"><span className="font-medium text-gray-700">Memory (GB)</span><span className="text-gray-500">{memoryUsedGb} / {memoryLimitGb}</span></div><div className="w-full bg-gray-200 rounded-full h-2"><div className="bg-green-600 h-2 rounded-full" style={{ width: `${memoryLimitGb ? Math.min(100, Math.round((memoryUsedGb / memoryLimitGb) * 100)) : 0}%` }} /></div></div>)}
                        {storageLimitGb > 0 && (<div><div className="flex justify-between text-sm mb-1"><span className="font-medium text-gray-700">Storage (GB)</span><span className="text-gray-500">{storageUsedGb} / {storageLimitGb}</span></div><div className="w-full bg-gray-200 rounded-full h-2"><div className="bg-orange-600 h-2 rounded-full" style={{ width: `${storageLimitGb ? Math.min(100, Math.round((storageUsedGb / storageLimitGb) * 100)) : 0}%` }} /></div></div>)}
                      </div>
                    )}
                  </CardContent>
                </Card>
              ),
            },
            {
              id: 'general',
              label: 'General',
              icon: <Building2 className="h-4 w-4" />,
              content: (
                <Card bordered shadow="md">
                  <CardHeader className="flex flex-row items-center justify-between">
                    <span className="font-semibold text-gray-900">General</span>
                    <div className="flex items-center gap-2">
                      <Button variant="secondary" size="sm" disabled={!updateLink?.enabled} title={!updateLink?.enabled ? updateLink?.reason : undefined} onClick={openEdit}><Pencil className="h-4 w-4 mr-1" /> Edit</Button>
                      <Button variant="secondary" size="sm" disabled={!deleteLink?.enabled} title={!deleteLink?.enabled ? deleteLink?.reason : undefined} onClick={handleDelete} className="text-red-600 hover:text-red-700 hover:bg-red-50">{actionLoading === 'delete' ? <Loader2 className="h-4 w-4 animate-spin" /> : <><Trash2 className="h-4 w-4 mr-1" /> Delete</>}</Button>
                    </div>
                  </CardHeader>
                  <CardContent className="pt-2">
                    <div className="space-y-0">
                      <DetailRow label="Name" value={tenant.name} />
                      <DetailRow label="Display name" value={tenant.displayName} />
                      <DetailRow label="Description" value={tenant.description} />
                      <DetailRow label="Status" value={tenant.status} />
                      <DetailRow label="Created" value={formatDetailDate(tenant.createdAt)} />
                      <DetailRow label="Updated" value={formatDetailDate(tenant.updatedAt)} />
                    </div>
                  </CardContent>
                </Card>
              ),
            },
            {
              id: 'datacenters',
              label: 'Datacenters',
              icon: <Server className="h-4 w-4" />,
              badge: grantsTotal,
              content: (
                <Card bordered shadow="md">
                  <CardHeader className="flex flex-row items-center justify-between">
                    <span className="font-semibold text-gray-900">Datacenters</span>
                    <Button variant="secondary" size="sm" onClick={openAddDatacenterModal}>
                      <Plus className="h-4 w-4 mr-1" />
                      Add datacenter
                    </Button>
                  </CardHeader>
                  <CardContent className="p-0">
                    <Table
                      columns={grantColumns}
                      data={grants?.items ?? []}
                      emptyMessage="No datacenters assigned to this tenant."
                      overflowVisibleColumnKeys={['actions']}
                    />
                  </CardContent>
                </Card>
              ),
            },
            {
              id: 'idp',
              label: 'IDP settings',
              icon: <Shield className="h-4 w-4" />,
              content: (
                <Card bordered shadow="md">
                  <CardHeader className="flex flex-row items-center gap-2"><span className="font-semibold text-gray-900">IDP settings</span><span className="text-xs text-gray-500 font-normal">(read-only)</span></CardHeader>
                  <CardContent className="pt-2">
                    {!settings?.idp ? (
                      <p className="text-sm text-gray-500 py-2">No IDP settings or using system default.</p>
                    ) : (
                      <div className="space-y-0">
                        <DetailRow label="Type" value={settings.idp.type} />
                        <DetailRow label="Issuer URL" value={settings.idp.issuerUrl} />
                        <DetailRow label="Metadata URL" value={settings.idp.metadataUrl} />
                        <DetailRow label="Auto-provision users" value={settings.idp.autoProvisionUsers != null ? String(settings.idp.autoProvisionUsers) : '—'} />
                        <DetailRow label="Just-in-time provisioning" value={settings.idp.justInTimeProvisioning != null ? String(settings.idp.justInTimeProvisioning) : '—'} />
                        <DetailRow label="MFA enforced" value={settings.idp.mfaEnforced != null ? String(settings.idp.mfaEnforced) : '—'} />
                      </div>
                    )}
                  </CardContent>
                </Card>
              ),
            },
            {
              id: 'users',
              label: 'Users',
              icon: <Users className="h-4 w-4" />,
              badge: usersTotal,
              content: (
                <Card bordered shadow="md">
                  <CardHeader className="flex flex-row items-center justify-between">
                    <span className="font-semibold text-gray-900">Users</span>
                    <Button variant="secondary" size="sm" onClick={() => setAddUserModal(true)}><UserPlus className="h-4 w-4 mr-1" /> Add user</Button>
                  </CardHeader>
                  <CardContent className="pt-2">
                    {!userList?.items?.length ? (
                      <p className="text-sm text-gray-500 py-4">No users in this tenant.</p>
                    ) : (
                      <ul className="divide-y divide-gray-100">
                        {userList.items.map((u) => (
                          <li key={u.user_id} className="py-3 flex flex-wrap items-center justify-between gap-2">
                            <div>
                              <span className="font-medium text-gray-900">{u.display_name || u.username || u.email || u.user_id}</span>
                              {u.role_name && <Badge variant="secondary" className="ml-2">{u.role_name}</Badge>}
                              {u.email && <span className="text-sm text-gray-500 block">{u.email}</span>}
                            </div>
                            <div className="flex items-center gap-1">
                              <Button variant="secondary" size="sm" onClick={() => setUserRoleModal({ user: u, newRole: u.role_name || '' })} disabled={actionLoading !== null}>Edit role</Button>
                              <Button variant="secondary" size="sm" onClick={() => handleRemoveUser(u.user_id)} disabled={actionLoading === `del-${u.user_id}`} className="text-red-600 hover:text-red-700 hover:bg-red-50">{actionLoading === `del-${u.user_id}` ? <Loader2 className="h-4 w-4 animate-spin" /> : 'Remove'}</Button>
                            </div>
                          </li>
                        ))}
                      </ul>
                    )}
                  </CardContent>
                </Card>
              ),
            },
            {
              id: 'audit',
              label: 'Audit logs',
              icon: <FileText className="h-4 w-4" />,
              content: (
                <Card bordered shadow="md">
                  <CardHeader><span className="font-semibold text-gray-900">Audit logs</span></CardHeader>
                  <CardContent className="pt-2">
                    <div className="py-8 text-center text-gray-500">
                      <FileText className="h-10 w-10 mx-auto mb-2 text-gray-300" />
                      <p className="text-sm">Audit log will be displayed here.</p>
                    </div>
                  </CardContent>
                </Card>
              ),
            },
          ]}
        />
      </div>

      {/* Add datacenter wizard modal */}
      <AnimatePresence>
        {addDatacenterModal && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50" onClick={closeAddDatacenterModal}>
            <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} className="bg-white rounded-xl shadow-xl w-full max-w-lg p-6 max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold">Add datacenter</h3>
                <button onClick={closeAddDatacenterModal} className="p-1 rounded hover:bg-gray-100"><X className="h-5 w-5" /></button>
              </div>
              <div className="flex gap-2 mb-4 text-sm">
                <span className={addDcWizardStep === 0 ? 'font-medium text-primary-600' : 'text-gray-500'}>1. Select datacenter</span>
                <span className="text-gray-300">→</span>
                <span className={addDcWizardStep === 1 ? 'font-medium text-primary-600' : 'text-gray-500'}>2. Set limits</span>
              </div>
              {addDcError && (
                <p className="text-sm text-red-600 mb-3 bg-red-50 border border-red-200 rounded px-3 py-2">{addDcError}</p>
              )}
              {addDcWizardStep === 0 && (
                <div className="space-y-3">
                  <p className="text-sm text-gray-500">Choose a datacenter to grant this tenant access to. Already assigned datacenters are not listed.</p>
                  {addDcLoadingDatacenters ? (
                    <div className="flex items-center gap-2 py-6 text-gray-500"><Loader2 className="h-5 w-5 animate-spin" /> Loading datacenters…</div>
                  ) : availableDatacenters.length === 0 ? (
                    <p className="text-sm text-gray-500 py-4">No datacenters available to add. All datacenters may already be assigned, or none exist.</p>
                  ) : (
                    <ul className="border border-gray-200 rounded-lg divide-y divide-gray-100 max-h-56 overflow-y-auto">
                      {availableDatacenters.map((dc) => (
                        <li key={dc.id}>
                          <button
                            type="button"
                            onClick={() => setSelectedDatacenterId(dc.id)}
                            className={`w-full text-left px-3 py-2.5 flex items-center justify-between gap-2 hover:bg-gray-50 ${selectedDatacenterId === dc.id ? 'bg-primary-50 border-l-2 border-primary-600' : ''}`}
                          >
                            <span className="font-medium text-gray-900">{dc.name}</span>
                            {dc.capacity && (dc.capacity.totalCpus != null || dc.capacity.totalMemoryGb != null) && (
                              <span className="text-xs text-gray-500">
                                {[dc.capacity.totalCpus != null && `${dc.capacity.totalCpus} CPUs`, dc.capacity.totalMemoryGb != null && `${dc.capacity.totalMemoryGb} GB RAM`].filter(Boolean).join(', ')}
                              </span>
                            )}
                          </button>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              )}
              {addDcWizardStep === 1 && selectedDatacenterId && (
                <div className="space-y-4">
                  <p className="text-sm text-gray-500">
                    Set resource limits for this tenant in <strong>{availableDatacenters.find((dc) => dc.id === selectedDatacenterId)?.name ?? selectedDatacenterId}</strong>. Leave blank for no limit.
                  </p>
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input type="checkbox" checked={grantAccess} onChange={(e) => setGrantAccess(e.target.checked)} className="rounded border-gray-300" />
                    <span className="text-sm font-medium text-gray-700">Grant access</span>
                  </label>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Max vCPUs</label>
                      <Input type="number" min={0} value={limitMaxCpus} onChange={(e) => setLimitMaxCpus(e.target.value)} placeholder="No limit" />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Max memory (GB)</label>
                      <Input type="number" min={0} value={limitMaxMemoryGb} onChange={(e) => setLimitMaxMemoryGb(e.target.value)} placeholder="No limit" />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Max storage (GB)</label>
                      <Input type="number" min={0} value={limitMaxStorageGb} onChange={(e) => setLimitMaxStorageGb(e.target.value)} placeholder="No limit" />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Max VMs</label>
                      <Input type="number" min={0} value={limitMaxVms} onChange={(e) => setLimitMaxVms(e.target.value)} placeholder="No limit" />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Max volumes</label>
                      <Input type="number" min={0} value={limitMaxVolumes} onChange={(e) => setLimitMaxVolumes(e.target.value)} placeholder="No limit" />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Max load balancers</label>
                      <Input type="number" min={0} value={limitMaxLoadBalancers} onChange={(e) => setLimitMaxLoadBalancers(e.target.value)} placeholder="No limit" />
                    </div>
                  </div>
                </div>
              )}
              <div className="flex justify-end gap-2 mt-6">
                {addDcWizardStep === 0 ? (
                  <>
                    <Button variant="secondary" onClick={closeAddDatacenterModal}>Cancel</Button>
                    <Button
                      onClick={() => setAddDcWizardStep(1)}
                      disabled={addDcLoadingDatacenters || !selectedDatacenterId || availableDatacenters.length === 0}
                    >
                      Next: Set limits
                    </Button>
                  </>
                ) : (
                  <>
                    <Button variant="secondary" onClick={() => setAddDcWizardStep(0)} disabled={addDcSubmitting}>Back</Button>
                    <Button onClick={handleCreateTenantDatacenterGrant} disabled={addDcSubmitting}>
                      {addDcSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : 'Add datacenter'}
                    </Button>
                  </>
                )}
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Edit grant limits modal */}
      <AnimatePresence>
        {editGrantLimitsModal && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50" onClick={() => setEditGrantLimitsModal(null)}>
            <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} className="bg-white rounded-xl shadow-xl w-full max-w-lg p-6 max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold">Edit resource limits — {editGrantLimitsModal.datacenter?.name ?? editGrantLimitsModal.datacenterId}</h3>
                <button onClick={() => setEditGrantLimitsModal(null)} className="p-1 rounded hover:bg-gray-100"><X className="h-5 w-5" /></button>
              </div>
              {editGrantError && <p className="text-sm text-red-600 mb-3 bg-red-50 border border-red-200 rounded px-3 py-2">{editGrantError}</p>}
              <div className="grid grid-cols-2 gap-3">
                <div><label className="block text-sm font-medium text-gray-700 mb-1">Max vCPUs</label><Input type="number" min={0} value={editLimitsForm.maxCpus ?? ''} onChange={(e) => setEditLimitsForm((f) => ({ ...f, maxCpus: e.target.value }))} placeholder="No limit" /></div>
                <div><label className="block text-sm font-medium text-gray-700 mb-1">Max memory (GB)</label><Input type="number" min={0} value={editLimitsForm.maxMemoryGb ?? ''} onChange={(e) => setEditLimitsForm((f) => ({ ...f, maxMemoryGb: e.target.value }))} placeholder="No limit" /></div>
                <div><label className="block text-sm font-medium text-gray-700 mb-1">Max storage (GB)</label><Input type="number" min={0} value={editLimitsForm.maxStorageGb ?? ''} onChange={(e) => setEditLimitsForm((f) => ({ ...f, maxStorageGb: e.target.value }))} placeholder="No limit" /></div>
                <div><label className="block text-sm font-medium text-gray-700 mb-1">Max VMs</label><Input type="number" min={0} value={editLimitsForm.maxVms ?? ''} onChange={(e) => setEditLimitsForm((f) => ({ ...f, maxVms: e.target.value }))} placeholder="No limit" /></div>
                <div><label className="block text-sm font-medium text-gray-700 mb-1">Max volumes</label><Input type="number" min={0} value={editLimitsForm.maxVolumes ?? ''} onChange={(e) => setEditLimitsForm((f) => ({ ...f, maxVolumes: e.target.value }))} placeholder="No limit" /></div>
                <div><label className="block text-sm font-medium text-gray-700 mb-1">Max load balancers</label><Input type="number" min={0} value={editLimitsForm.maxLoadBalancers ?? ''} onChange={(e) => setEditLimitsForm((f) => ({ ...f, maxLoadBalancers: e.target.value }))} placeholder="No limit" /></div>
              </div>
              <div className="flex justify-end gap-2 mt-6">
                <Button variant="secondary" onClick={() => setEditGrantLimitsModal(null)}>Cancel</Button>
                <Button onClick={handleUpdateGrantLimits} disabled={editGrantSubmitting}>{editGrantSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : 'Save limits'}</Button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Edit grant settings modal */}
      <AnimatePresence>
        {editGrantSettingsModal && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50" onClick={() => setEditGrantSettingsModal(null)}>
            <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} className="bg-white rounded-xl shadow-xl w-full max-w-lg p-6 max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold">Edit datacenter settings — {editGrantSettingsModal.datacenter?.name ?? editGrantSettingsModal.datacenterId}</h3>
                <button onClick={() => setEditGrantSettingsModal(null)} className="p-1 rounded hover:bg-gray-100"><X className="h-5 w-5" /></button>
              </div>
              <p className="text-sm text-gray-500 mb-3">Override settings for this tenant in this datacenter. Comma-separated values.</p>
              {editGrantError && <p className="text-sm text-red-600 mb-3 bg-red-50 border border-red-200 rounded px-3 py-2">{editGrantError}</p>}
              <div className="space-y-3">
                <div><label className="block text-sm font-medium text-gray-700 mb-1">VM classes</label><Input value={editSettingsForm.vmClassesStr} onChange={(e) => setEditSettingsForm((f) => ({ ...f, vmClassesStr: e.target.value }))} placeholder="e.g. default, large" /></div>
                <div><label className="block text-sm font-medium text-gray-700 mb-1">Storage classes</label><Input value={editSettingsForm.storageClassesStr} onChange={(e) => setEditSettingsForm((f) => ({ ...f, storageClassesStr: e.target.value }))} placeholder="e.g. standard, ssd" /></div>
                <div><label className="block text-sm font-medium text-gray-700 mb-1">Network domains</label><Input value={editSettingsForm.networkDomainsStr} onChange={(e) => setEditSettingsForm((f) => ({ ...f, networkDomainsStr: e.target.value }))} placeholder="e.g. default, dmz" /></div>
              </div>
              <div className="flex justify-end gap-2 mt-6">
                <Button variant="secondary" onClick={() => setEditGrantSettingsModal(null)}>Cancel</Button>
                <Button onClick={handleUpdateGrantSettings} disabled={editGrantSubmitting}>{editGrantSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : 'Save settings'}</Button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Edit modal */}
      <AnimatePresence>
        {editModal && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50" onClick={() => setEditModal(false)}>
            <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }} className="bg-white rounded-xl shadow-xl w-full max-w-md p-6" onClick={(e) => e.stopPropagation()}>
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold">Edit tenant</h3>
                <button onClick={() => setEditModal(false)} className="p-1 rounded hover:bg-gray-100"><X className="h-5 w-5" /></button>
              </div>
              <p className="text-sm text-gray-500 mb-4">Name cannot be changed. You can update display name and description.</p>
              <div className="space-y-3">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Display name</label>
                  <Input value={editDisplayName} onChange={(e) => setEditDisplayName(e.target.value)} placeholder="Display name" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                  <Input value={editDescription} onChange={(e) => setEditDescription(e.target.value)} placeholder="Description" />
                </div>
              </div>
              <div className="flex justify-end gap-2 mt-6">
                <Button variant="secondary" onClick={() => setEditModal(false)}>Cancel</Button>
                <Button onClick={handleUpdate} disabled={actionLoading === 'update'}>
                  {actionLoading === 'update' ? <Loader2 className="h-4 w-4 animate-spin" /> : 'Save'}
                </Button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Edit user role modal */}
      <AnimatePresence>
        {userRoleModal && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50" onClick={() => setUserRoleModal(null)}>
            <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} className="bg-white rounded-xl shadow-xl w-full max-w-md p-6" onClick={(e) => e.stopPropagation()}>
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold">Edit user role</h3>
                <button onClick={() => setUserRoleModal(null)} className="p-1 rounded hover:bg-gray-100"><X className="h-5 w-5" /></button>
              </div>
              <p className="text-sm text-gray-600 mb-2">{userRoleModal.user.display_name || userRoleModal.user.username} — {userRoleModal.user.role_name}</p>
              <div className="space-y-2">
                <label className="block text-sm font-medium text-gray-700">Role</label>
                <Input value={userRoleModal.newRole} onChange={(e) => setUserRoleModal({ ...userRoleModal, newRole: e.target.value })} placeholder="e.g. tenant:admin" />
              </div>
              <div className="flex justify-end gap-2 mt-6">
                <Button variant="secondary" onClick={() => setUserRoleModal(null)}>Cancel</Button>
                <Button onClick={handleUpdateUserRole} disabled={actionLoading !== null || !userRoleModal.newRole.trim()}>
                  {actionLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : 'Save'}
                </Button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Add user modal — placeholder: would need RoleBindingBulkCreate + listIdpUsers or picker */}
      <AnimatePresence>
        {addUserModal && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50" onClick={() => setAddUserModal(false)}>
            <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} className="bg-white rounded-xl shadow-xl w-full max-w-md p-6" onClick={(e) => e.stopPropagation()}>
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold">Add user</h3>
                <button onClick={() => setAddUserModal(false)} className="p-1 rounded hover:bg-gray-100"><X className="h-5 w-5" /></button>
              </div>
              <p className="text-sm text-gray-500">Add user flow: select from IDP users and assign role. Coming soon.</p>
              <div className="flex justify-end gap-2 mt-6">
                <Button variant="secondary" onClick={() => setAddUserModal(false)}>Close</Button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

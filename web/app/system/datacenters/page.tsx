'use client';

import { useState } from 'react';
import { motion } from 'framer-motion';
import { 
  Plus, 
  Server, 
  MapPin, 
  MoreVertical, 
  Edit, 
  Trash2, 
  RefreshCw,
  AlertCircle,
  CheckCircle,
  Clock,
  HardDrive,
  Activity,
  ArrowRight,
  Search,
  Filter,
  ChevronDown,
  Settings
} from 'lucide-react';
import { Button } from '@components/ui/atoms/button';
import { Card, CardContent, CardHeader } from '@components/ui/atoms/card';
import { Input } from '@components/ui/atoms/input';
import { Badge } from '@components/ui/atoms/badge';
import { Switch } from '@components/ui/atoms/switch';
import { Dropdown } from '@components/ui/molecules/dropdown';
import { Table } from '@components/ui/organisms/table';
import { Sidebar } from '@components/ui/organisms/sidebar';
import { Header } from '@components/ui/organisms/header';

interface Datacenter {
  id: string;
  name: string;
  type: 'libvirt' | 'proxmox' | 'vsphere' | 'aws' | 'azure' | 'gcp';
  status: 'connected' | 'disconnected' | 'syncing' | 'error';
  health: 'healthy' | 'degraded' | 'down';
  region: string;
  location: string;
  providerId: string;
  totalCapacity: number;
  usedCapacity: number;
  totalNodes: number;
  activeNodes: number;
  createdAt: string;
  lastSync: string;
}

interface DatacenterStats {
  total: number;
  connected: number;
  disconnected: number;
  healthy: number;
  degraded: number;
  down: number;
}

const mockDatacenters: Datacenter[] = [
  {
    id: 'dc-1',
    name: 'US-East-Primary',
    type: 'aws',
    status: 'connected',
    health: 'healthy',
    region: 'us-east-1',
    location: 'Virginia, USA',
    providerId: 'aws-1',
    totalCapacity: 1000,
    usedCapacity: 642,
    totalNodes: 50,
    activeNodes: 48,
    createdAt: '2024-01-15T10:30:00Z',
    lastSync: '2024-01-10T14:22:00Z',
  },
  {
    id: 'dc-2',
    name: 'US-West-Secondary',
    type: 'aws',
    status: 'connected',
    health: 'healthy',
    region: 'us-west-2',
    location: 'Oregon, USA',
    providerId: 'aws-1',
    totalCapacity: 500,
    usedCapacity: 234,
    totalNodes: 25,
    activeNodes: 23,
    createdAt: '2024-01-20T09:15:00Z',
    lastSync: '2024-01-10T14:25:00Z',
  },
  {
    id: 'dc-3',
    name: 'EU-Central',
    type: 'proxmox',
    status: 'connected',
    health: 'degraded',
    region: 'eu-central-1',
    location: 'Frankfurt, Germany',
    providerId: 'proxmox-1',
    totalCapacity: 200,
    usedCapacity: 156,
    totalNodes: 10,
    activeNodes: 8,
    createdAt: '2024-01-18T11:45:00Z',
    lastSync: '2024-01-10T14:30:00Z',
  },
  {
    id: 'dc-4',
    name: 'Asia-Pacific',
    type: 'aws',
    status: 'disconnected',
    health: 'down',
    region: 'ap-southeast-1',
    location: 'Singapore',
    providerId: 'aws-2',
    totalCapacity: 300,
    usedCapacity: 0,
    totalNodes: 15,
    activeNodes: 0,
    createdAt: '2024-01-22T08:00:00Z',
    lastSync: '2024-01-10T14:28:00Z',
  },
  {
    id: 'dc-5',
    name: 'Local Development',
    type: 'libvirt',
    status: 'connected',
    health: 'healthy',
    region: 'local',
    location: 'On-Premise',
    providerId: 'libvirt-1',
    totalCapacity: 100,
    usedCapacity: 45,
    totalNodes: 5,
    activeNodes: 5,
    createdAt: '2024-01-25T16:20:00Z',
    lastSync: '2024-01-10T14:32:00Z',
  },
];

export default function DatacentersPage() {
  const [showToast, setShowToast] = useState(false);
  const [toastMessage, setToastMessage] = useState('');
  const [toastVariant, setToastVariant] = useState<'success' | 'error'>('success');
  const [filterQuery, setFilterQuery] = useState('');
  const [selectedDatacenter, setSelectedDatacenter] = useState<Datacenter | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingDatacenter, setEditingDatacenter] = useState<Datacenter | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [filterStatus, setFilterStatus] = useState<string>('all');
  
  const stats: DatacenterStats = {
    total: mockDatacenters.length,
    connected: mockDatacenters.filter(dc => dc.status === 'connected').length,
    disconnected: mockDatacenters.filter(dc => dc.status === 'disconnected').length,
    healthy: mockDatacenters.filter(dc => dc.health === 'healthy').length,
    degraded: mockDatacenters.filter(dc => dc.health === 'degraded').length,
    down: mockDatacenters.filter(dc => dc.health === 'down').length,
  };
  
  const filteredDatacenters = filterQuery || filterStatus !== 'all'
    ? mockDatacenters.filter(dc =>
        (filterQuery ? dc.name.toLowerCase().includes(filterQuery.toLowerCase()) ||
        dc.region.toLowerCase().includes(filterQuery.toLowerCase()) ||
        dc.location.toLowerCase().includes(filterQuery.toLowerCase()) : true) &&
        (filterStatus === 'all' || dc.status === filterStatus)
      )
    : mockDatacenters;
  
  // Helper functions
  const getStatusIcon = (status: string, health: string) => {
    if (status === 'connected' && health === 'healthy') {
      return <CheckCircle className="w-4 h-4 text-green-600" />;
    }
    if (status === 'connected' && health === 'degraded') {
      return <AlertCircle className="w-4 h-4 text-yellow-600" />;
    }
    if (status === 'disconnected' || health === 'down') {
      return <AlertCircle className="w-4 h-4 text-red-600" />;
    }
    if (status === 'syncing') {
      return <RefreshCw className="w-4 h-4 text-blue-600 animate-spin" />;
    }
    return <Clock className="w-4 h-4 text-gray-400" />;
  };
  
  const getHealthBadge = (health: string) => {
    const healthLabels: Record<string, string> = {
      healthy: 'Healthy',
      degraded: 'Degraded',
      down: 'Down',
    };
    const healthVariants: Record<string, 'success' | 'warning' | 'error'> = {
      healthy: 'success',
      degraded: 'warning',
      down: 'error',
    };
    return (
      <Badge variant={healthVariants[health]}>
        {healthLabels[health]}
      </Badge>
    );
  };
  
  const handleAddDatacenter = () => {
    setIsModalOpen(true);
    setEditingDatacenter(null);
  };
  
  const handleEdit = (datacenter: Datacenter) => {
    setEditingDatacenter(datacenter);
    setIsModalOpen(true);
  };
  
  const handleSync = (datacenter: Datacenter) => {
    setShowToast(true);
    setToastMessage(`Syncing ${datacenter.name}...`);
    setToastVariant('success');
    setTimeout(() => setShowToast(false), 3000);
  };
  
  const handleDelete = (datacenter: Datacenter) => {
    setSelectedDatacenter(datacenter);
    setIsDeleting(true);
  };
  
  const confirmDelete = () => {
    if (!selectedDatacenter) return;
    
    setShowToast(true);
    setToastMessage(`Datacenter "${selectedDatacenter.name}" deleted successfully`);
    setToastVariant('success');
    setSelectedDatacenter(null);
    setIsDeleting(false);
    setIsModalOpen(false);
    setTimeout(() => setShowToast(false), 3000);
  };
  
  const handleModalClose = () => {
    setIsModalOpen(false);
    setEditingDatacenter(null);
    setSelectedDatacenter(null);
    setIsDeleting(false);
  };
  
  // Column definitions - defined after helper functions
  const datacentersColumns = [
    {
      key: 'name',
      header: 'Name',
      cell: (row: Datacenter) => (
        <div className="flex items-center gap-2">
          <span className="font-medium">{row.name}</span>
          <Badge variant="info">{row.type}</Badge>
        </div>
      ),
      sortable: true,
    },
    {
      key: 'status',
      header: 'Status',
      cell: (row: Datacenter) => (
        <div className="flex items-center gap-2">
          {getStatusIcon(row.status, row.health)}
          <span className="font-medium">{row.status}</span>
          {getHealthBadge(row.health)}
        </div>
      ),
    },
    {
      key: 'region',
      header: 'Region',
      cell: (row: Datacenter) => (
        <div className="flex items-center gap-1">
          <MapPin className="w-4 h-4 text-gray-500" />
          <span>{row.region}</span>
        </div>
      ),
    },
    {
      key: 'location',
      header: 'Location',
      cell: (row: Datacenter) => (
        <div className="flex items-center gap-1">
          <Activity className="w-4 h-4 text-gray-500" />
          <span className="text-gray-600">{row.location}</span>
        </div>
      ),
    },
    {
      key: 'capacity',
      header: 'Capacity',
      cell: (row: Datacenter) => (
        <div className="text-right">
          <span className="font-medium">{row.usedCapacity} / {row.totalCapacity}</span>
          <div className="text-sm text-gray-500">
            {row.usedCapacity} cores
          </div>
        </div>
      ),
    },
    {
      key: 'nodes',
      header: 'Nodes',
      cell: (row: Datacenter) => (
        <div className="text-right">
          <span className="font-medium">{row.activeNodes} / {row.totalNodes}</span>
          <div className="text-sm text-gray-500">
            {row.activeNodes} active
          </div>
        </div>
      ),
    },
    {
      key: 'health',
      header: 'Health',
      cell: (row: Datacenter) => (
        <div className="flex items-center justify-end gap-2">
          {getStatusIcon(row.health, row.status)}
          <span className="font-medium">{row.health}</span>
          {getHealthBadge(row.health)}
        </div>
      ),
    },
    {
      key: 'actions',
      header: 'Actions',
      cell: (row: Datacenter) => (
        <div className="flex items-center justify-end gap-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => handleEdit(row)}
            leftIcon={<Edit className="w-4 h-4" />}
          >
            Edit
          </Button>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => handleSync(row)}
            leftIcon={<RefreshCw className="w-4 h-4" />}
          >
            Sync
          </Button>
          <Button
            variant="danger"
            size="sm"
            onClick={() => handleDelete(row)}
            leftIcon={<Trash2 className="w-4 h-4" />}
          >
            Delete
          </Button>
        </div>
      ),
    },
  ];
  
  return (
    <div className="min-h-screen bg-gray-50">
      <Sidebar isOpen={false} onClose={() => {}} userRole="system" />
      <div className="flex flex-col">
        <Header />
        <main className="flex-1 overflow-auto">
          <div className="max-w-7xl mx-auto p-8">
            {/* Page Header */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5 }}
              className="mb-8"
            >
              <div className="flex items-center justify-between">
                <div>
                  <h1 className="text-3xl font-bold text-gray-900">Datacenters</h1>
                  <p className="text-gray-600 mt-1">Manage and monitor your cloud infrastructure datacenters</p>
                </div>
                <Button onClick={handleAddDatacenter} leftIcon={<Plus size={16} />}>
                  Add Datacenter
                </Button>
              </div>
            </motion.div>
            
            {/* Stats Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5 }}
                className="bg-white rounded-lg border border-gray-200 p-6 shadow-sm"
              >
                <div className="flex items-center gap-4">
                  <div className="p-3 bg-blue-50 rounded-lg">
                    <Server className="w-8 h-8 text-blue-600" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold text-gray-900">{stats.total}</p>
                    <p className="text-sm text-gray-600">Total Datacenters</p>
                  </div>
                </div>
              </motion.div>
              
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5 }}
                className="bg-white rounded-lg border border-gray-200 p-6 shadow-sm"
              >
                <div className="flex items-center gap-4">
                  <div className="p-3 bg-green-50 rounded-lg">
                    <CheckCircle className="w-8 h-8 text-green-600" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold text-gray-900">{stats.connected}</p>
                    <p className="text-sm text-gray-600">Connected</p>
                  </div>
                </div>
              </motion.div>
              
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5 }}
                className="bg-white rounded-lg border border-gray-200 p-6 shadow-sm"
              >
                <div className="flex items-center gap-4">
                  <div className="p-3 bg-red-50 rounded-lg">
                    <AlertCircle className="w-8 h-8 text-red-600" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold text-gray-900">{stats.disconnected}</p>
                    <p className="text-sm text-gray-600">Disconnected</p>
                  </div>
                </div>
              </motion.div>
              
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5 }}
                className="bg-white rounded-lg border border-gray-200 p-6 shadow-sm"
              >
                <div className="flex items-center gap-4">
                  <div className="p-3 bg-yellow-50 rounded-lg">
                    <AlertCircle className="w-8 h-8 text-yellow-600" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold text-gray-900">{stats.degraded}</p>
                    <p className="text-sm text-gray-600">Degraded</p>
                  </div>
                </div>
              </motion.div>
              
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5 }}
                className="bg-white rounded-lg border border-gray-200 p-6 shadow-sm"
              >
                <div className="flex items-center gap-4">
                  <div className="p-3 bg-gray-100 rounded-lg">
                    <HardDrive className="w-8 h-8 text-gray-600" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold text-gray-900">{stats.down}</p>
                    <p className="text-sm text-gray-600">Down</p>
                  </div>
                </div>
              </motion.div>
            </div>
            
            {/* Filter Bar */}
            <motion.div
              initial={{ opacity: 0, y: 40 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5 }}
              className="mb-6"
            >
              <div className="flex items-center justify-between gap-4">
                <div className="flex-1">
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 w-5 h-5 text-gray-400" />
                    <Input
                      variant="search"
                      placeholder="Search datacenters..."
                      value={filterQuery}
                      onChange={(e) => setFilterQuery(e.target.value)}
                      className="pl-10"
                    />
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <Dropdown
                    options={[
                      { label: 'All Statuses', value: 'all' },
                      { label: 'Connected', value: 'connected' },
                      { label: 'Disconnected', value: 'disconnected' },
                      { label: 'Syncing', value: 'syncing' },
                      { label: 'Error', value: 'error' },
                    ]}
                    value={filterStatus}
                    onChange={setFilterStatus}
                    placeholder="All Statuses"
                  />
                  <Button variant="ghost" size="sm" leftIcon={<Filter className="w-4 h-4" />}>
                    Filter
                  </Button>
                </div>
              </div>
            </motion.div>
            
            {/* Datacenters Table */}
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <h2 className="text-lg font-semibold">Datacenters ({filteredDatacenters.length})</h2>
                  <div className="flex items-center gap-2">
                    <Button variant="ghost" size="sm" leftIcon={<RefreshCw className="w-4 h-4" />}>
                      Refresh
                    </Button>
                    <Button variant="ghost" size="sm" leftIcon={<Settings className="w-4 h-4" />}>
                      Settings
                    </Button>
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                <Table
                  columns={datacentersColumns}
                  data={filteredDatacenters}
                  onRowClick={(row) => setSelectedDatacenter(row)}
                  emptyMessage="No datacenters found matching your search criteria."
                  isLoading={false}
                />
              </CardContent>
            </Card>
          </div>
        </main>
      </div>
      
      {/* Add/Edit Datacenter Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.2 }}
            className="bg-white rounded-lg shadow-xl max-w-lg w-full"
          >
            <div className="p-6">
              <h2 className="text-xl font-semibold mb-4">
                {editingDatacenter ? 'Edit Datacenter' : 'Add Datacenter'}
              </h2>
              
              <div className="space-y-4">
                <div>
                  <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">Name</label>
                  <Input
                    id="name"
                    placeholder="Enter datacenter name"
                    defaultValue={editingDatacenter?.name || ''}
                  />
                </div>
                
                <div>
                  <label htmlFor="type" className="block text-sm font-medium text-gray-700 mb-1">Type</label>
                  <Dropdown
                    options={[
                      { label: 'Libvirt', value: 'libvirt' },
                      { label: 'Proxmox', value: 'proxmox' },
                      { label: 'VMware vSphere', value: 'vsphere' },
                      { label: 'AWS', value: 'aws' },
                      { label: 'Azure', value: 'azure' },
                      { label: 'Google Cloud', value: 'gcp' },
                    ]}
                    value={editingDatacenter?.type || ''}
                  />
                </div>
                
                <div>
                  <label htmlFor="region" className="block text-sm font-medium text-gray-700 mb-1">Region</label>
                  <Input
                    id="region"
                    placeholder="Enter region (e.g., us-east-1)"
                    defaultValue={editingDatacenter?.region || ''}
                  />
                </div>
                
                <div>
                  <label htmlFor="location" className="block text-sm font-medium text-gray-700 mb-1">Location</label>
                  <Input
                    id="location"
                    placeholder="Enter location (e.g., Virginia, USA)"
                    defaultValue={editingDatacenter?.location || ''}
                  />
                </div>
                
                <div>
                  <label htmlFor="capacity" className="block text-sm font-medium text-gray-700 mb-1">Capacity (cores)</label>
                  <Input
                    id="capacity"
                    type="number"
                    placeholder="Enter capacity"
                    defaultValue={editingDatacenter?.totalCapacity || ''}
                  />
                </div>
                
                <div>
                  <label htmlFor="provider" className="block text-sm font-medium text-gray-700 mb-1">Provider</label>
                  <Dropdown
                    options={[
                      { label: 'AWS', value: 'aws-1' },
                      { label: 'Azure', value: 'azure-1' },
                      { label: 'GCP', value: 'gcp-1' },
                      { label: 'Libvirt', value: 'libvirt-1' },
                      { label: 'Proxmox', value: 'proxmox-1' },
                    ]}
                    value={editingDatacenter?.providerId || ''}
                  />
                </div>
              </div>
              
              <div className="flex justify-end gap-3 pt-6">
                <Button variant="ghost" onClick={handleModalClose}>
                  Cancel
                </Button>
                <Button onClick={handleModalClose}>
                  {editingDatacenter ? 'Save Changes' : 'Add Datacenter'}
                </Button>
              </div>
            </div>
          </motion.div>
        </div>
      )}
      
      {/* Delete Confirmation Modal */}
      {isDeleting && selectedDatacenter && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.2 }}
            className="bg-white rounded-lg shadow-xl max-w-md w-full"
          >
            <div className="p-6">
              <div className="flex items-center gap-4 mb-4">
                <div className="p-3 bg-red-50 rounded-full">
                  <Trash2 className="w-8 h-8 text-red-600" />
                </div>
                <div>
                  <h2 className="text-xl font-semibold">Delete Datacenter</h2>
                  <p className="text-gray-600">
                    Are you sure you want to delete <strong>{selectedDatacenter.name}</strong>?
                  </p>
                </div>
              </div>
              
              <p className="text-gray-600 mb-6">
                This action cannot be undone. All resources associated with this datacenter will be permanently removed.
              </p>
              
              <div className="flex justify-end gap-3">
                <Button variant="ghost" onClick={handleModalClose}>
                  Cancel
                </Button>
                <Button variant="danger" onClick={confirmDelete}>
                  Delete
                </Button>
              </div>
            </div>
          </motion.div>
        </div>
      )}
      
      {/* Toast Notification */}
      {showToast && (
        <div className="fixed bottom-4 right-4 z-50">
          <motion.div
            initial={{ opacity: 0, y: 100 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 100 }}
            transition={{ duration: 0.3 }}
            className={`px-6 py-3 rounded-lg shadow-lg ${
              toastVariant === 'success' ? 'bg-green-50 border-green-200' : 'bg-red-50 border-red-200'
            }`}
          >
            <div className="flex items-center gap-3">
              {toastVariant === 'success' ? (
                <CheckCircle className="w-5 h-5 text-green-600" />
              ) : (
                <AlertCircle className="w-5 h-5 text-red-600" />
              )}
              <p className="font-medium">{toastMessage}</p>
            </div>
          </motion.div>
        </div>
      )}
    </div>
  );
}

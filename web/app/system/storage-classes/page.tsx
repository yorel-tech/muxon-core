"use client";

import { useState } from "react";
import { Plus, RefreshCw, Settings } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import StorageClassList from "./components/StorageClassList";
import ProviderStorageList from "./components/ProviderStorageList";
import StorageOverrideList from "./components/StorageOverrideList";
import StorageClassForm from "./components/StorageClassForm";
import { useStorageClasses } from "./hooks/useStorageClasses";

export default function StorageClassesPage() {
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [editingClass, setEditingClass] = useState<any>(null);
  const { refetch } = useStorageClasses();

  const handleCreate = () => {
    setEditingClass(null);
    setShowCreateForm(true);
  };

  const handleEdit = (storageClass: any) => {
    setEditingClass(storageClass);
    setShowCreateForm(true);
  };

  const handleClose = () => {
    setShowCreateForm(false);
    setEditingClass(null);
    refetch();
  };

  return (
    <div className="container mx-auto py-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Storage Management</h1>
          <p className="text-muted-foreground mt-1">
            Manage storage classes, provider storage, and overrides
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => refetch()}>
            <RefreshCw className="h-4 w-4 mr-2" />
            Refresh
          </Button>
        </div>
      </div>

      <Tabs defaultValue="storage-classes" className="space-y-4">
        <TabsList>
          <TabsTrigger value="storage-classes">Storage Classes</TabsTrigger>
          <TabsTrigger value="provider-storage">Provider Storage</TabsTrigger>
          <TabsTrigger value="overrides">Overrides</TabsTrigger>
        </TabsList>

        <TabsContent value="storage-classes" className="space-y-4">
          <div className="flex justify-between items-center">
            <p className="text-sm text-muted-foreground">
              Define storage capabilities and constraints for volume provisioning
            </p>
            <Button onClick={handleCreate}>
              <Plus className="h-4 w-4 mr-2" />
              Create Storage Class
            </Button>
          </div>
          <StorageClassList onEdit={handleEdit} />
        </TabsContent>

        <TabsContent value="provider-storage" className="space-y-4">
          <p className="text-sm text-muted-foreground">
            View discovered storage from infrastructure providers
          </p>
          <ProviderStorageList />
        </TabsContent>

        <TabsContent value="overrides" className="space-y-4">
          <p className="text-sm text-muted-foreground">
            Manually map storage classes to specific provider storage
          </p>
          <StorageOverrideList />
        </TabsContent>
      </Tabs>

      {showCreateForm && (
        <StorageClassForm
          storageClass={editingClass}
          onClose={handleClose}
        />
      )}
    </div>
  );
}

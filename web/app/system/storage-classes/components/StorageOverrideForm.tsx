"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useStorageClasses } from "../hooks/useStorageClasses";
import { useProviderStorage } from "../hooks/useProviderStorage";
import { useCreateStorageOverride, useUpdateStorageOverride } from "../hooks/useStorageOverrides";
import { Badge } from "@/components/ui/badge";
import { X } from "lucide-react";

interface StorageOverrideFormProps {
  override?: any;
  onClose: () => void;
}

export default function StorageOverrideForm({ override, onClose }: StorageOverrideFormProps) {
  const [formData, setFormData] = useState({
    storageClassName: "",
    providerType: "libvirt",
    providerStorageNames: [] as string[],
    priority: 100,
  });

  const { data: storageClasses } = useStorageClasses();
  const { data: providerStorage } = useProviderStorage(null);
  const createMutation = useCreateStorageOverride();
  const updateMutation = useUpdateStorageOverride();

  useEffect(() => {
    if (override) {
      setFormData({
        storageClassName: override.storageClassName || "",
        providerType: override.providerType || "libvirt",
        providerStorageNames: override.providerStorageNames || [],
        priority: override.priority || 100,
      });
    }
  }, [override]);

  const availableStorage = providerStorage?.filter(
    (s: any) => s.providerType === formData.providerType
  ) || [];

  const handleAddStorage = (storageName: string) => {
    if (!formData.providerStorageNames.includes(storageName)) {
      setFormData({
        ...formData,
        providerStorageNames: [...formData.providerStorageNames, storageName],
      });
    }
  };

  const handleRemoveStorage = (storageName: string) => {
    setFormData({
      ...formData,
      providerStorageNames: formData.providerStorageNames.filter((n) => n !== storageName),
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      if (override) {
        await updateMutation.mutateAsync({ id: override.id, data: formData });
      } else {
        await createMutation.mutateAsync(formData);
      }
      onClose();
    } catch (error) {
      console.error("Failed to save storage override:", error);
    }
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>
            {override ? "Edit Storage Override" : "Create Storage Override"}
          </DialogTitle>
          <DialogDescription>
            Manually map a storage class to specific provider storage
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="space-y-4">
            <div>
              <Label htmlFor="storageClassName">Storage Class</Label>
              <Select
                value={formData.storageClassName}
                onValueChange={(value) => setFormData({ ...formData, storageClassName: value })}
                disabled={!!override}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select storage class" />
                </SelectTrigger>
                <SelectContent>
                  {storageClasses?.map((sc: any) => (
                    <SelectItem key={sc.name} value={sc.name}>
                      {sc.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div>
              <Label htmlFor="providerType">Provider Type</Label>
              <Select
                value={formData.providerType}
                onValueChange={(value) => setFormData({ ...formData, providerType: value, providerStorageNames: [] })}
                disabled={!!override}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="libvirt">Libvirt</SelectItem>
                  <SelectItem value="proxmox">Proxmox</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div>
              <Label>Provider Storage Names</Label>
              <div className="space-y-2">
                <Select onValueChange={handleAddStorage}>
                  <SelectTrigger>
                    <SelectValue placeholder="Add storage..." />
                  </SelectTrigger>
                  <SelectContent>
                    {availableStorage.map((storage: any) => (
                      <SelectItem key={storage.id} value={storage.name}>
                        {storage.name} ({storage.storageType})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>

                <div className="flex gap-2 flex-wrap min-h-[40px] border rounded-md p-2">
                  {formData.providerStorageNames.length === 0 ? (
                    <span className="text-sm text-muted-foreground">No storage selected</span>
                  ) : (
                    formData.providerStorageNames.map((name) => (
                      <Badge key={name} variant="secondary" className="gap-1">
                        {name}
                        <X
                          className="h-3 w-3 cursor-pointer"
                          onClick={() => handleRemoveStorage(name)}
                        />
                      </Badge>
                    ))
                  )}
                </div>
              </div>
            </div>

            <div>
              <Label htmlFor="priority">Priority</Label>
              <Input
                id="priority"
                type="number"
                value={formData.priority}
                onChange={(e) => setFormData({ ...formData, priority: parseInt(e.target.value) || 100 })}
              />
            </div>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" disabled={createMutation.isPending || updateMutation.isPending}>
              {createMutation.isPending || updateMutation.isPending ? "Saving..." : "Save"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

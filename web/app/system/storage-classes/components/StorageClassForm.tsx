"use client";

import { useState, useEffect } from "react";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
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
import { useCreateStorageClass, useUpdateStorageClass } from "../hooks/useStorageClasses";
import CapabilityEditor from "./CapabilityEditor";
import ConstraintEditor from "./ConstraintEditor";

interface StorageClassFormProps {
  storageClass?: any;
  onClose: () => void;
}

export default function StorageClassForm({ storageClass, onClose }: StorageClassFormProps) {
  const [formData, setFormData] = useState({
    name: "",
    type: "block",
    tier: "balanced",
    description: "",
    capabilities: {},
    constraints: {},
  });

  const createMutation = useCreateStorageClass();
  const updateMutation = useUpdateStorageClass();

  useEffect(() => {
    if (storageClass) {
      setFormData({
        name: storageClass.name || "",
        type: storageClass.type || "block",
        tier: storageClass.tier || "balanced",
        description: storageClass.description || "",
        capabilities: storageClass.capabilities || {},
        constraints: storageClass.constraints || {},
      });
    }
  }, [storageClass]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      if (storageClass) {
        await updateMutation.mutateAsync({ name: storageClass.name, data: formData });
      } else {
        await createMutation.mutateAsync(formData);
      }
      onClose();
    } catch (error) {
      console.error("Failed to save storage class:", error);
    }
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>
            {storageClass ? "Edit Storage Class" : "Create Storage Class"}
          </DialogTitle>
          <DialogDescription>
            Define storage capabilities and constraints for volume provisioning
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="space-y-4">
            <div>
              <Label htmlFor="name">Name</Label>
              <Input
                id="name"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder="fast-ssd"
                required
                disabled={!!storageClass}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="type">Type</Label>
                <Select
                  value={formData.type}
                  onValueChange={(value) => setFormData({ ...formData, type: value })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="block">Block</SelectItem>
                    <SelectItem value="file">File</SelectItem>
                    <SelectItem value="object">Object</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div>
                <Label htmlFor="tier">Tier</Label>
                <Select
                  value={formData.tier}
                  onValueChange={(value) => setFormData({ ...formData, tier: value })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="performance">Performance</SelectItem>
                    <SelectItem value="balanced">Balanced</SelectItem>
                    <SelectItem value="capacity">Capacity</SelectItem>
                    <SelectItem value="archive">Archive</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div>
              <Label htmlFor="description">Description</Label>
              <Textarea
                id="description"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                placeholder="High-performance SSD storage for databases"
                rows={2}
              />
            </div>

            <div>
              <Label>Capabilities</Label>
              <CapabilityEditor
                capabilities={formData.capabilities}
                onChange={(capabilities) => setFormData({ ...formData, capabilities })}
              />
            </div>

            <div>
              <Label>Constraints</Label>
              <ConstraintEditor
                constraints={formData.constraints}
                onChange={(constraints) => setFormData({ ...formData, constraints })}
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

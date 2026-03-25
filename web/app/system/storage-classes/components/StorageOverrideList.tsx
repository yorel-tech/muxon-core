"use client";

import { useState } from "react";
import { Plus, Edit, Trash2, Settings } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { useStorageOverrides, useDeleteStorageOverride } from "../hooks/useStorageOverrides";
import StorageOverrideForm from "./StorageOverrideForm";

export default function StorageOverrideList() {
  const { data: overrides, isLoading, error } = useStorageOverrides();
  const deleteMutation = useDeleteStorageOverride();
  const [showForm, setShowForm] = useState(false);
  const [editingOverride, setEditingOverride] = useState<any>(null);

  const handleCreate = () => {
    setEditingOverride(null);
    setShowForm(true);
  };

  const handleEdit = (override: any) => {
    setEditingOverride(override);
    setShowForm(true);
  };

  const handleDelete = async (id: string) => {
    if (confirm("Are you sure you want to delete this override?")) {
      await deleteMutation.mutateAsync(id);
    }
  };

  const handleClose = () => {
    setShowForm(false);
    setEditingOverride(null);
  };

  if (isLoading) {
    return <div className="text-center py-8">Loading storage overrides...</div>;
  }

  if (error) {
    return (
      <Alert variant="destructive">
        <AlertDescription>Failed to load storage overrides: {error.message}</AlertDescription>
      </Alert>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <Button onClick={handleCreate}>
          <Plus className="h-4 w-4 mr-2" />
          Create Override
        </Button>
      </div>

      {!overrides || overrides.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <Settings className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
            <p className="text-muted-foreground">No storage overrides configured</p>
            <p className="text-sm text-muted-foreground mt-1">
              Create an override to manually map storage classes to provider storage
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {overrides.map((override: any) => (
            <Card key={override.id}>
              <CardHeader>
                <div className="flex items-start justify-between">
                  <div>
                    <CardTitle className="text-lg">{override.storageClassName}</CardTitle>
                    <CardDescription>Provider: {override.providerType}</CardDescription>
                  </div>
                  <div className="flex gap-1">
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => handleEdit(override)}
                    >
                      <Edit className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => handleDelete(override.id)}
                    >
                      <Trash2 className="h-4 w-4 text-destructive" />
                    </Button>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-3">
                <div>
                  <p className="text-sm font-medium mb-2">Mapped Storage</p>
                  <div className="flex gap-2 flex-wrap">
                    {override.providerStorageNames?.map((name: string) => (
                      <Badge key={name} variant="outline">
                        {name}
                      </Badge>
                    ))}
                  </div>
                </div>
                <div className="text-sm text-muted-foreground">
                  Priority: {override.priority || 100}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {showForm && (
        <StorageOverrideForm
          override={editingOverride}
          onClose={handleClose}
        />
      )}
    </div>
  );
}

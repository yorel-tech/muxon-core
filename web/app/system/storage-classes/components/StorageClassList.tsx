"use client";

import { useState } from "react";
import { Edit, Trash2, HardDrive } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { useStorageClasses, useDeleteStorageClass } from "../hooks/useStorageClasses";
import { Alert, AlertDescription } from "@/components/ui/alert";

interface StorageClassListProps {
  onEdit: (storageClass: any) => void;
}

export default function StorageClassList({ onEdit }: StorageClassListProps) {
  const { data: storageClasses, isLoading, error } = useStorageClasses();
  const deleteStorageClass = useDeleteStorageClass();

  const handleDelete = async (name: string) => {
    if (confirm(`Are you sure you want to delete storage class "${name}"?`)) {
      await deleteStorageClass.mutateAsync(name);
    }
  };

  if (isLoading) {
    return <div className="text-center py-8">Loading storage classes...</div>;
  }

  if (error) {
    return (
      <Alert variant="destructive">
        <AlertDescription>Failed to load storage classes: {error.message}</AlertDescription>
      </Alert>
    );
  }

  if (!storageClasses || storageClasses.length === 0) {
    return (
      <Card>
        <CardContent className="py-12 text-center">
          <HardDrive className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
          <p className="text-muted-foreground">No storage classes defined</p>
          <p className="text-sm text-muted-foreground mt-1">
            Create a storage class to define storage capabilities
          </p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
      {storageClasses.map((sc: any) => (
        <Card key={sc.name} className="hover:shadow-lg transition-shadow">
          <CardHeader>
            <div className="flex items-start justify-between">
              <div>
                <CardTitle className="text-lg">{sc.name}</CardTitle>
                <CardDescription>{sc.description || "No description"}</CardDescription>
              </div>
              <div className="flex gap-1">
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => onEdit(sc)}
                >
                  <Edit className="h-4 w-4" />
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => handleDelete(sc.name)}
                >
                  <Trash2 className="h-4 w-4 text-destructive" />
                </Button>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex gap-2 flex-wrap">
              <Badge variant="outline">{sc.type}</Badge>
              <Badge variant="secondary">{sc.tier}</Badge>
            </div>

            {sc.capabilities && (
              <div>
                <p className="text-sm font-medium mb-2">Capabilities</p>
                <div className="flex gap-2 flex-wrap">
                  {Object.entries(sc.capabilities).map(([key, value]) => (
                    <Badge key={key} variant="default">
                      {key}: {String(value)}
                    </Badge>
                  ))}
                </div>
              </div>
            )}

            {sc.constraints && Object.keys(sc.constraints).length > 0 && (
              <div>
                <p className="text-sm font-medium mb-2">Constraints</p>
                <div className="text-sm text-muted-foreground">
                  {Object.entries(sc.constraints).map(([key, value]) => (
                    <div key={key}>
                      {key}: {String(value)}
                    </div>
                  ))}
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      ))}
    </div>
  );
}

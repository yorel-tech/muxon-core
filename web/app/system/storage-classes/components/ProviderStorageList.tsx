"use client";

import { useState } from "react";
import { RefreshCw, Database, CheckCircle, XCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { useProviderStorage, useSyncProviderStorage } from "../hooks/useProviderStorage";
import { useProviders } from "@/hooks/useProviders";

export default function ProviderStorageList() {
  const { data: providers } = useProviders();
  const [selectedProvider, setSelectedProvider] = useState<string | null>(null);
  const { data: providerStorage, isLoading, error } = useProviderStorage(selectedProvider);
  const syncMutation = useSyncProviderStorage();

  const handleSync = async (providerId: string) => {
    await syncMutation.mutateAsync(providerId);
  };

  if (isLoading) {
    return <div className="text-center py-8">Loading provider storage...</div>;
  }

  if (error) {
    return (
      <Alert variant="destructive">
        <AlertDescription>Failed to load provider storage: {error.message}</AlertDescription>
      </Alert>
    );
  }

  return (
    <div className="space-y-4">
      {/* Provider Selection */}
      <div className="flex gap-2 flex-wrap">
        {providers?.items?.map((provider: any) => (
          <Button
            key={provider.id}
            variant={selectedProvider === provider.id ? "default" : "outline"}
            onClick={() => setSelectedProvider(provider.id)}
          >
            {provider.name}
          </Button>
        ))}
      </div>

      {selectedProvider && (
        <div className="flex justify-end">
          <Button
            onClick={() => handleSync(selectedProvider)}
            disabled={syncMutation.isPending}
          >
            <RefreshCw className={`h-4 w-4 mr-2 ${syncMutation.isPending ? "animate-spin" : ""}`} />
            Sync Storage
          </Button>
        </div>
      )}

      {/* Storage List */}
      {!providerStorage || providerStorage.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <Database className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
            <p className="text-muted-foreground">
              {selectedProvider ? "No storage discovered for this provider" : "Select a provider to view storage"}
            </p>
            {selectedProvider && (
              <Button
                variant="outline"
                className="mt-4"
                onClick={() => handleSync(selectedProvider)}
              >
                Sync Storage
              </Button>
            )}
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {providerStorage.map((storage: any) => (
            <Card key={storage.id}>
              <CardHeader>
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <CardTitle className="text-lg">{storage.name}</CardTitle>
                    <CardDescription>{storage.storageType}</CardDescription>
                  </div>
                  {storage.enabled ? (
                    <CheckCircle className="h-5 w-5 text-green-500" />
                  ) : (
                    <XCircle className="h-5 w-5 text-red-500" />
                  )}
                </div>
              </CardHeader>
              <CardContent className="space-y-3">
                <div>
                  <p className="text-sm font-medium mb-2">Capabilities</p>
                  <div className="flex gap-2 flex-wrap">
                    {storage.capabilities && Object.entries(storage.capabilities).map(([key, value]) => (
                      <Badge key={key} variant="secondary">
                        {key}: {String(value)}
                      </Badge>
                    ))}
                  </div>
                </div>

                {storage.metrics && (
                  <div>
                    <p className="text-sm font-medium mb-2">Metrics</p>
                    <div className="text-sm text-muted-foreground space-y-1">
                      {storage.metrics.free_gb !== undefined && (
                        <div>Free: {storage.metrics.free_gb} GB</div>
                      )}
                      {storage.metrics.total_gb !== undefined && (
                        <div>Total: {storage.metrics.total_gb} GB</div>
                      )}
                      {storage.metrics.estimated_iops !== undefined && (
                        <div>IOPS: {storage.metrics.estimated_iops.toLocaleString()}</div>
                      )}
                    </div>
                  </div>
                )}

                {storage.nodeId && (
                  <div className="text-xs text-muted-foreground">
                    Node: {storage.nodeId}
                  </div>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}

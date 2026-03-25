import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";

const API_BASE = "/api/v1/provider-storage";

export function useProviderStorage(providerId: string | null) {
  return useQuery({
    queryKey: ["provider-storage", providerId],
    queryFn: async () => {
      if (!providerId) return [];
      const res = await fetch(`${API_BASE}/provider/${providerId}`);
      if (!res.ok) throw new Error("Failed to fetch provider storage");
      return res.json();
    },
    enabled: !!providerId,
  });
}

export function useSyncProviderStorage() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (providerId: string) => {
      const res = await fetch(`${API_BASE}/provider/${providerId}/sync`, {
        method: "POST",
      });
      if (!res.ok) throw new Error("Failed to sync provider storage");
      return res.json();
    },
    onSuccess: (_, providerId) => {
      queryClient.invalidateQueries({ queryKey: ["provider-storage", providerId] });
    },
  });
}

export function useSyncAllProviders() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async () => {
      const res = await fetch(`${API_BASE}/sync-all`, {
        method: "POST",
      });
      if (!res.ok) throw new Error("Failed to sync all providers");
      return res.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["provider-storage"] });
    },
  });
}

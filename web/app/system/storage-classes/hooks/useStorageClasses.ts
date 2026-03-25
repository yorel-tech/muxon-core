import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";

const API_BASE = "/api/v1";

export function useStorageClasses() {
  return useQuery({
    queryKey: ["storage-classes"],
    queryFn: async () => {
      const res = await fetch(`${API_BASE}/storage-classes`);
      if (!res.ok) throw new Error("Failed to fetch storage classes");
      return res.json();
    },
  });
}

export function useCreateStorageClass() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (data: any) => {
      const res = await fetch(`${API_BASE}/storage-classes`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data),
      });
      if (!res.ok) throw new Error("Failed to create storage class");
      return res.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["storage-classes"] });
    },
  });
}

export function useUpdateStorageClass() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ name, data }: { name: string; data: any }) => {
      const res = await fetch(`${API_BASE}/storage-classes/${name}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data),
      });
      if (!res.ok) throw new Error("Failed to update storage class");
      return res.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["storage-classes"] });
    },
  });
}

export function useDeleteStorageClass() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (name: string) => {
      const res = await fetch(`${API_BASE}/storage-classes/${name}`, {
        method: "DELETE",
      });
      if (!res.ok) throw new Error("Failed to delete storage class");
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["storage-classes"] });
    },
  });
}

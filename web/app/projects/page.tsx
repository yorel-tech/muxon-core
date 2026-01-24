'use client';
import { useQuery } from '@tanstack/react-query';
import { apiGet } from '@/lib/api';

async function fetchProjects() {
  const base = process.env.NEXT_PUBLIC_API_BASE!;
  return apiGet(`${base}/api/v1/projects`);
}

export default function Projects() {
  const { data, isLoading, error } = useQuery({ queryKey: ['projects'], queryFn: fetchProjects });
  return (
    <main className="max-w-3xl mx-auto py-10">
      <h1 className="text-2xl font-bold">Projects</h1>
      {isLoading && <p className="mt-4">Loading...</p>}
      {error && <p className="mt-4 text-red-600">Error loading projects</p>}
      <ul className="mt-6 space-y-2">
        {(data || []).map((p: any) => (
          <li key={p.id} className="p-3 bg-white rounded border">{p.name}</li>
        ))}
      </ul>
    </main>
  );
}

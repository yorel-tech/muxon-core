'use client';

/**
 * Single row for entity detail sections (label + value).
 * Used consistently across Datacenter, Tenant, and Provider detail pages.
 */
export function DetailRow({
  label,
  value,
}: {
  label: string;
  value: React.ReactNode;
}) {
  return (
    <div className="flex flex-wrap gap-x-2 py-1.5 border-b border-gray-100 last:border-0">
      <span className="text-sm font-medium text-gray-500 min-w-[140px]">{label}</span>
      <span className="text-sm text-gray-900">{value ?? '—'}</span>
    </div>
  );
}

export function formatDetailDate(s: string | undefined): string {
  if (!s) return '—';
  try {
    return new Date(s).toLocaleString();
  } catch {
    return s;
  }
}

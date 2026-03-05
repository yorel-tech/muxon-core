'use client';

import { useState } from 'react';
import { usePathname } from 'next/navigation';
import { CollapsibleSidebar } from '@/components/ui/organisms/collapsible-sidebar';
import { TenantProvider } from '@/lib/tenant-context';

export default function TenantLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const pathname = usePathname();
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);
  const isLoginPage = pathname === '/tenant/login';

  if (isLoginPage) {
    return <TenantProvider>{children}</TenantProvider>;
  }

  return (
    <TenantProvider>
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
        <CollapsibleSidebar
          isOpen={isSidebarOpen}
          onToggle={() => setIsSidebarOpen(!isSidebarOpen)}
          userRole="tenant"
        />
        <div className={`transition-all duration-300 ${isSidebarOpen ? 'ml-64' : 'ml-16'}`}>
          {children}
        </div>
      </div>
    </TenantProvider>
  );
}

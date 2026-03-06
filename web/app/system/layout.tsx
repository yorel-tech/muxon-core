'use client';

import { useState, useEffect } from 'react';
import { CollapsibleSidebar } from '@/components/ui/organisms/collapsible-sidebar';
import { ProductInfoProvider, useProductInfo } from '@/lib/product-info-context';

function SystemLayoutInner({
  children,
  isSidebarOpen,
  onToggleSidebar,
}: {
  children: React.ReactNode;
  isSidebarOpen: boolean;
  onToggleSidebar: () => void;
}) {
  const { edition, isEnterprise } = useProductInfo();

  useEffect(() => {
    const root = document.documentElement;
    if (edition) {
      root.dataset.edition = edition;
    }
    return () => {
      delete root.dataset.edition;
    };
  }, [edition]);

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <CollapsibleSidebar
        isOpen={isSidebarOpen}
        onToggle={onToggleSidebar}
        userRole="system"
        isEnterprise={isEnterprise}
      />
      <div className={`transition-all duration-300 ${isSidebarOpen ? 'ml-64' : 'ml-16'}`}>
        {children}
      </div>
    </div>
  );
}

export default function SystemLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);

  return (
    <ProductInfoProvider>
      <SystemLayoutInner
        isSidebarOpen={isSidebarOpen}
        onToggleSidebar={() => setIsSidebarOpen(!isSidebarOpen)}
      >
        {children}
      </SystemLayoutInner>
    </ProductInfoProvider>
  );
}

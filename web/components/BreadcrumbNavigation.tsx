import React from 'react';
import Link from 'next/link';
import { BreadcrumbItem } from '@/types/provider';

interface BreadcrumbNavigationProps {
  items: BreadcrumbItem[];
  className?: string;
  separator?: string;
  maxItems?: number;
}

export const BreadcrumbNavigation: React.FC<BreadcrumbNavigationProps> = ({ 
  items, 
  className = '',
  separator = '/',
  maxItems 
}) => {
  // If maxItems is specified, truncate the breadcrumb
  let displayItems = items;
  if (maxItems && items.length > maxItems) {
    const firstItem = items[0];
    const lastItems = items.slice(-(maxItems - 2));
    displayItems = [
      firstItem,
      { label: '...', disabled: true },
      ...lastItems
    ];
  }

  return (
    <nav className={`flex items-center space-x-2 text-sm ${className}`}>
      {displayItems.map((item, index) => (
        <React.Fragment key={index}>
          {index > 0 && (
            <span className="text-gray-400 font-medium">{separator}</span>
          )}
          {item.href && !item.disabled ? (
            <Link
              href={item.href}
              className={`${
                item.active 
                  ? 'text-gray-900 font-medium' 
                  : 'text-gray-600 hover:text-gray-900 transition-colors'
              }`}
            >
              {item.label}
            </Link>
          ) : (
            <span className={item.active ? 'text-gray-900 font-medium' : 'text-gray-600'}>
              {item.label}
            </span>
          )}
        </React.Fragment>
      ))}
    </nav>
  );
};

/**
 * Compact breadcrumb for mobile or tight spaces
 */
export const CompactBreadcrumb: React.FC<BreadcrumbNavigationProps> = (props) => {
  return (
    <BreadcrumbNavigation 
      {...props}
      maxItems={3}
      className="text-xs"
    />
  );
};

/**
 * Breadcrumb with home icon
 */
export const BreadcrumbWithHome: React.FC<BreadcrumbNavigationProps> = ({ 
  items, 
  ...props 
}) => {
  const itemsWithHome = [
    { label: 'Home', href: '/system' },
    ...items
  ];

  return (
    <BreadcrumbNavigation 
      items={itemsWithHome}
      {...props}
    />
  );
};

/**
 * Breadcrumb for provider hierarchy
 */
export const ProviderBreadcrumb: React.FC<{
  providerName?: string;
  clusterName?: string;
  nodeName?: string;
  currentView?: string;
}> = ({ 
  providerName, 
  clusterName, 
  nodeName, 
  currentView = 'overview' 
}) => {
  const items: BreadcrumbItem[] = [
    { label: 'Providers', href: '/system/providers' }
  ];

  if (providerName) {
    items.push({ 
      label: providerName, 
      href: `/system/providers/${providerName}`,
      active: !clusterName && !nodeName
    });
  }

  if (clusterName) {
    items.push({ 
      label: clusterName, 
      href: `/system/providers/${providerName}/clusters/${clusterName}`,
      active: !nodeName
    });
  }

  if (nodeName) {
    items.push({ 
      label: nodeName, 
      href: `/system/providers/${providerName}/clusters/${clusterName}/nodes/${nodeName}`,
      active: true
    });
  }

  // Add current view if it's not the default
  if (currentView !== 'overview') {
    items.push({ 
      label: currentView.charAt(0).toUpperCase() + currentView.slice(1), 
      active: true 
    });
  }

  return <BreadcrumbNavigation items={items} />;
};
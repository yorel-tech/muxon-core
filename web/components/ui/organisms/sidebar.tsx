'use client';

import { motion, AnimatePresence } from 'framer-motion';
import { forwardRef, useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  LayoutDashboard,
  Settings,
  Server,
  Database,
  Users,
  Building2,
  Shield,
  Cloud,
  ChevronRight,
  ChevronDown,
  ChevronLeft,
  LogOut,
} from 'lucide-react';
import { cn } from '@/lib/utils';

export interface SidebarItem {
  id: string;
  label: string;
  icon: React.ReactNode;
  href: string;
  badge?: number;
  children?: SidebarItem[];
  isEnterprise?: boolean;
}

export interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
  userRole: 'system' | 'tenant';
  isEnterprise?: boolean;
  className?: string;
}

const systemUserItems: SidebarItem[] = [
  {
    id: 'dashboard',
    label: 'Dashboard',
    icon: <LayoutDashboard size={20} />,
    href: '/system/dashboard',
  },
  {
    id: 'providers',
    label: 'Providers',
    icon: <Cloud size={20} />,
    href: '/providers',
  },
  {
    id: 'datacenters',
    label: 'Datacenters',
    icon: <Server size={20} />,
    href: '/datacenters',
  },
  {
    id: 'idps',
    label: 'Identity Providers',
    icon: <Shield size={20} />,
    href: '/idps',
  },
  {
    id: 'users',
    label: 'Users',
    icon: <Users size={20} />,
    href: '/users',
    badge: 5,
  },
  {
    id: 'tenants',
    label: 'Tenants',
    icon: <Building2 size={20} />,
    href: '/tenants',
    badge: 3,
  },
  {
    id: 'settings',
    label: 'Settings',
    icon: <Settings size={20} />,
    href: '/settings',
  },
];

const tenantUserItems: SidebarItem[] = [
  {
    id: 'dashboard',
    label: 'Dashboard',
    icon: <LayoutDashboard size={20} />,
    href: '/tenant/dashboard',
  },
  {
    id: 'datacenters',
    label: 'Datacenters',
    icon: <Server size={20} />,
    href: '/tenant/datacenters',
  },
  {
    id: 'vms',
    label: 'Virtual Machines',
    icon: <Database size={20} />,
    href: '/tenant/vms',
    badge: 12,
  },
  {
    id: 'networks',
    label: 'Networks',
    icon: <Cloud size={20} />,
    href: '/tenant/networks',
  },
  {
    id: 'settings',
    label: 'Settings',
    icon: <Settings size={20} />,
    href: '/tenant/settings',
  },
];

const enterpriseItems: SidebarItem[] = [
  {
    id: 'billing',
    label: 'Billing',
    icon: <Database size={20} />,
    href: '/billing',
    isEnterprise: true,
  },
];

export const Sidebar = forwardRef<HTMLDivElement, SidebarProps>(
  ({ isOpen, onClose, userRole, isEnterprise = false, className = '' }: SidebarProps, ref) => {
    const pathname = usePathname();
  const [expandedItems, setExpandedItems] = useState<Set<string>>(new Set());

    const toggleExpand = (id: string) => {
      setExpandedItems((prev) => {
        const newSet = new Set(prev);
        if (newSet.has(id)) {
          newSet.delete(id);
        } else {
          newSet.add(id);
        }
        return newSet;
      });
    };

    const isActive = (href: string) => pathname === href;

    const renderSidebarItem = (item: SidebarItem, depth = 0) => {
      const hasChildren = item.children && item.children.length > 0;
      const isExpanded = expandedItems.has(item.id);

      return (
        <div key={item.id}>
          <Link
            href={item.href}
            onClick={onClose}
            className={cn(
              'flex items-center gap-3 px-3 py-2.5 rounded-lg transition-colors',
              'hover:bg-gray-100',
              isActive ? 'bg-primary-50 text-primary-700' : 'text-gray-700',
              depth > 0 && 'ml-4',
            )}
          >
            <div className="flex items-center gap-3 flex-1">
              <div className="flex items-center gap-3 text-gray-400">
                {item.icon}
              </div>
              <span className="text-sm font-medium">{item.label}</span>
            </div>
            {item.badge && (
              <span className="ml-auto bg-primary-100 text-primary-700 text-xs font-medium px-2 py-0.5 rounded-full">
                {item.badge}
              </span>
            )}
            {hasChildren && (
              <button
                onClick={(e) => {
                  e.preventDefault();
                  toggleExpand(item.id);
                }}
                className="p-1 hover:bg-gray-100 rounded-md transition-colors"
                aria-expanded={isExpanded}
              >
                {isExpanded ? (
                  <ChevronDown size={16} />
                ) : (
                  <ChevronRight size={16} />
                )}
              </button>
            )}
          </Link>
          {hasChildren && (
            <AnimatePresence>
              {isExpanded && (
                <motion.div
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: 'auto', opacity: 1 }}
                  exit={{ height: 0, opacity: 0 }}
                  transition={{ duration: 0.2 }}
                  className="ml-4 overflow-hidden"
                >
                  {item.children.map((child) => renderSidebarItem(child, depth + 1))}
                </motion.div>
              )}
            </AnimatePresence>
          )}
        </div>
      );
    };

    const items = userRole === 'system' ? systemUserItems : tenantUserItems;
    const enterpriseFeatures = isEnterprise ? enterpriseItems : [];

    return (
      <motion.div
        ref={ref}
        initial={{ x: '-100%' }}
        animate={{ x: isOpen ? '0%' : '-100%' }}
        transition={{ type: 'spring', damping: 30, stiffness: 300 }}
        className={cn(
          'fixed inset-y-0 left-0 z-40 bg-white border-r border-gray-200 w-64 transform',
          className,
        )}
      >
        <div className="flex flex-col h-full">
          {/* Logo / Brand */}
          <div className="flex items-center gap-3 p-6 border-b border-gray-200">
            <div className="flex items-center gap-2">
              <div className="h-8 w-8 bg-gradient-to-br from-primary-500 to-nexus-500 rounded-lg" />
              <span className="text-lg font-bold text-gray-900">infron</span>
              {isEnterprise && (
                <span className="ml-2 text-xs font-medium px-2 py-0.5 bg-nexus-100 text-nexus-700 rounded-full">
                  Enterprise
                </span>
              )}
            </div>
          </div>

          {/* Navigation */}
          <nav className="flex-1 overflow-y-auto p-4">
            <ul className="space-y-1">
              {items.map((item) => (
                <li key={item.id}>{renderSidebarItem(item)}</li>
              ))}
              {enterpriseFeatures.map((item) => (
                <li key={item.id}>{renderSidebarItem(item)}</li>
              ))}
            </ul>
          </nav>

          {/* User Section */}
          <div className="border-t border-gray-200 p-4">
            <div className="flex items-center gap-3 mb-4">
              <div className="h-8 w-8 bg-gray-200 rounded-full" />
              <div className="flex-1">
                <p className="text-sm font-medium text-gray-900">John Doe</p>
                <p className="text-xs text-gray-500">System Admin</p>
              </div>
            </div>
            <button className="flex items-center gap-3 w-full px-3 py-2 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-100 transition-colors">
              <Settings size={16} />
              <span>Settings</span>
            </button>
            <button className="flex items-center gap-3 w-full px-3 py-2 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-100 transition-colors">
              <LogOut size={16} />
              <span>Sign Out</span>
            </button>
          </div>
        </div>
      </motion.div>
    );
  });

Sidebar.displayName = 'Sidebar';

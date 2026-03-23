'use client';

import { motion, AnimatePresence } from 'framer-motion';
import { forwardRef, useState, useEffect } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useRouter } from 'next/navigation';
import { useSidebarCounts } from '@/lib/use-sidebar-counts';
import { useAuth } from '@/lib/auth-context';
import { fetchOidcConfigIfNeeded, getUserManager } from '@/lib/oidc';
import {
  LayoutDashboard,
  Settings,
  Server,
  Database,
  Users,
  Building2,
  Shield,
  Cloud,
  BookMarked,
  SlidersHorizontal,
  ChevronRight,
  ChevronDown,
  ChevronLeft,
  LogOut,
  BookOpen,
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

// Base sidebar items without badges
const baseSystemUserItems: Omit<SidebarItem, 'badge'>[] = [
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
    id: 'users',
    label: 'Users',
    icon: <Users size={20} />,
    href: '/users',
  },
  {
    id: 'tenants',
    label: 'Tenants',
    icon: <Building2 size={20} />,
    href: '/tenants',
  },
  {
    id: 'settings',
    label: 'Settings',
    icon: <Settings size={20} />,
    href: '/settings',
  },
  {
    id: 'swagger',
    label: 'API Documentation',
    icon: <BookOpen size={20} />,
    href: '/swagger-ui',
  },
];

const baseTenantUserItems: Omit<SidebarItem, 'badge'>[] = [
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
    label: 'VMs',
    icon: <Database size={20} />,
    href: '/tenant/vms',
  },
  {
    id: 'catalogs',
    label: 'Catalogs',
    icon: <BookMarked size={20} />,
    href: '/tenant/catalogs',
  },
  {
    id: 'users',
    label: 'Users',
    icon: <Users size={20} />,
    href: '/tenant/users',
  },
  {
    id: 'roles',
    label: 'Roles',
    icon: <Shield size={20} />,
    href: '/tenant/roles',
  },
  {
    id: 'administration',
    label: 'Administration',
    icon: <SlidersHorizontal size={20} />,
    href: '/tenant/administration',
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
    const router = useRouter();
    const { user } = useAuth();
    const [expandedItems, setExpandedItems] = useState<Set<string>>(new Set());
    const { counts } = useSidebarCounts(userRole, isOpen);

    const handleSignOut = async () => {
      try {
        await fetchOidcConfigIfNeeded();
        const manager = getUserManager();
        if (manager) {
          await manager.signoutRedirect();
        }
      } catch (error) {
        console.error('Error signing out:', error);
      }
    };

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
              isActive(item.href) ? 'bg-primary-50 text-primary-700' : 'text-gray-700',
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
                  {item.children?.map((child) => renderSidebarItem(child, depth + 1))}
                </motion.div>
              )}
            </AnimatePresence>
          )}
        </div>
      );
    };

    // Build sidebar items with dynamic badges
    const items: SidebarItem[] = (userRole === 'system' ? baseSystemUserItems : baseTenantUserItems).map((item) => {
      const newItem = { ...item } as SidebarItem;
      
      // Add badges based on item ID and user role
      if (userRole === 'system') {
        if (item.id === 'users' && counts.users > 0) {
          newItem.badge = counts.users;
        } else if (item.id === 'tenants' && counts.tenants > 0) {
          newItem.badge = counts.tenants;
        }
      } else {
        if (item.id === 'vms' && counts.vms > 0) {
          newItem.badge = counts.vms;
        }
      }
      
      return newItem;
    });
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
              <div className="h-8 w-8 rounded-lg" style={{ background: 'linear-gradient(to bottom right, #3b82f6, #a855f7)' }} />
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
              {user?.avatar ? (
                <img
                  src={user.avatar}
                  alt={user.name}
                  className="h-8 w-8 rounded-full object-cover"
                />
              ) : (
                <div className="h-8 w-8 bg-gray-200 rounded-full flex items-center justify-center">
                  <span className="text-sm font-medium text-gray-600">
                    {user?.name?.charAt(0).toUpperCase() || 'U'}
                  </span>
                </div>
              )}
              <div className="flex-1">
                <p className="text-sm font-medium text-gray-900">{user?.name || 'User'}</p>
                <p className="text-xs text-gray-500 capitalize">
                  {userRole === 'system' ? 'System Admin' : 'Tenant User'}
                </p>
              </div>
            </div>
            <button className="flex items-center gap-3 w-full px-3 py-2 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-100 transition-colors">
              <Settings size={16} />
              <span>Settings</span>
            </button>
            <button
              onClick={handleSignOut}
              className="flex items-center gap-3 w-full px-3 py-2 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-100 transition-colors"
            >
              <LogOut size={16} />
              <span>Sign Out</span>
            </button>
          </div>
        </div>
      </motion.div>
    );
  });

Sidebar.displayName = 'Sidebar';

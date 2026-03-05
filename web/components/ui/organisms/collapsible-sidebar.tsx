'use client';

import { motion, AnimatePresence } from 'framer-motion';
import { forwardRef, useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useSidebarCounts } from '@/lib/use-sidebar-counts';
import { useAuth } from '@/lib/auth-context';
import { getUserManager } from '@/lib/oidc';
import {
  LayoutDashboard,
  Settings,
  Server,
  Cloud,
  Users,
  Building2,
  Shield,
  BookOpen,
  BookMarked,
  ChevronLeft,
  ChevronRight,
  LogOut,
  SlidersHorizontal,
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

export interface CollapsibleSidebarProps {
  isOpen: boolean;
  onToggle: () => void;
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
    href: '/system/providers',
  },
  {
    id: 'tenants',
    label: 'Tenants',
    icon: <Building2 size={20} />,
    href: '/system/tenants',
  },
  {
    id: 'datacenters',
    label: 'Datacenters',
    icon: <Server size={20} />,
    href: '/system/datacenters',
  },
  {
    id: 'users',
    label: 'Users',
    icon: <Users size={20} />,
    href: '/system/users',
  },
  {
    id: 'settings',
    label: 'System Settings',
    icon: <Settings size={20} />,
    href: '/system/settings',
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
    icon: <Cloud size={20} />,
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

export const CollapsibleSidebar = forwardRef<HTMLDivElement, CollapsibleSidebarProps>(
  ({ isOpen, onToggle, userRole, isEnterprise = false, className = '' }: CollapsibleSidebarProps, ref) => {
    const pathname = usePathname();
    const { user } = useAuth();
    const [expandedItems, setExpandedItems] = useState<Set<string>>(new Set());
    const { counts } = useSidebarCounts(userRole, isOpen);

    const handleSignOut = async () => {
      try {
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
            className={cn(
              'flex items-center gap-3 py-2.5 rounded-lg transition-colors',
              'hover:bg-gray-700',
              isOpen ? 'px-3' : 'justify-center px-0',
              isActive(item.href) ? 'bg-primary-600 text-white' : 'text-gray-400',
              depth > 0 && 'ml-4',
            )}
          >
            <div className="flex items-center gap-3 flex-shrink-0">
              {item.icon}
            </div>
            <AnimatePresence mode="wait">
              {isOpen && (
                <motion.span
                  initial={{ opacity: 0, width: 0 }}
                  animate={{ opacity: 1, width: 'auto' }}
                  exit={{ opacity: 0, width: 0 }}
                  transition={{ duration: 0.2 }}
                  className="text-sm font-medium whitespace-nowrap overflow-hidden"
                >
                  {item.label}
                </motion.span>
              )}
            </AnimatePresence>
            {isOpen && item.badge && (
              <motion.span
                initial={{ opacity: 0, scale: 0 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0 }}
                transition={{ duration: 0.2 }}
                className="ml-auto bg-primary-600 text-white text-xs font-medium px-2 py-0.5 rounded-full flex-shrink-0"
              >
                {item.badge}
              </motion.span>
            )}
          </Link>
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

    return (
      <motion.div
        ref={ref}
        animate={{ width: isOpen ? 256 : 64 }}
        transition={{ type: 'spring', damping: 30, stiffness: 300 }}
        className={cn(
          'fixed inset-y-0 left-0 z-40 bg-gray-800 border-r border-gray-700',
          className,
        )}
      >
        <div className="flex flex-col h-full">
          {/* Logo / Brand */}
          <div className={cn(
            'flex items-center justify-center border-b border-gray-700',
            isOpen ? 'p-6 justify-between' : 'py-4'
          )}>
            <div className={cn(
              'flex items-center gap-2 overflow-hidden',
              isOpen ? '' : 'justify-center'
            )}>
              <div className="flex-shrink-0 h-8 w-8 rounded-lg" style={{ background: 'linear-gradient(to bottom right, #3b82f6, #a855f7)' }} />
              <AnimatePresence mode="wait">
                {isOpen && (
                  <motion.div
                    initial={{ opacity: 0, x: -10 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: -10 }}
                    transition={{ duration: 0.2 }}
                    className="flex items-center gap-2 overflow-hidden"
                  >
                    <span className="text-lg font-bold text-white whitespace-nowrap">infron</span>
                    {isEnterprise && (
                      <span className="ml-2 text-xs font-medium px-2 py-0.5 bg-nexus-100 text-nexus-700 rounded-full whitespace-nowrap">
                        Enterprise
                      </span>
                    )}
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          </div>

          {/* Navigation */}
          <nav className="flex-1 overflow-y-auto p-4">
            <ul className="space-y-1">
              {items.map((item) => (
                <li key={item.id}>{renderSidebarItem(item)}</li>
              ))}
            </ul>
          </nav>

          {/* User Section */}
          <div className={cn(
            'border-t border-gray-700',
            isOpen ? 'p-4' : 'py-4'
          )}>
            <div className={cn(
              'flex items-center gap-3 mb-4',
              isOpen ? '' : 'justify-center'
            )}>
              {user?.avatar ? (
                <img
                  src={user.avatar}
                  alt={user.name}
                  className="flex-shrink-0 h-8 w-8 rounded-full object-cover"
                />
              ) : (
                <div className="flex-shrink-0 h-8 w-8 rounded-full bg-gray-600 flex items-center justify-center">
                  <span className="text-sm font-medium text-gray-300">
                    {user?.name?.charAt(0).toUpperCase() || 'U'}
                  </span>
                </div>
              )}
              <AnimatePresence mode="wait">
                {isOpen && (
                  <motion.div
                    initial={{ opacity: 0, x: -10 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: -10 }}
                    transition={{ duration: 0.2 }}
                    className="flex-1 overflow-hidden"
                  >
                    <p className="text-sm font-medium text-gray-300 whitespace-nowrap">{user?.name || 'User'}</p>
                    <p className="text-xs text-gray-500 whitespace-nowrap capitalize">
                      {userRole === 'system' ? 'System Admin' : 'Tenant User'}
                    </p>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
            <button className={cn(
              'flex items-center gap-3 w-full py-2 rounded-lg text-sm font-medium text-gray-300 hover:bg-gray-700 transition-colors',
              isOpen ? 'px-3' : 'justify-center px-0'
            )}>
              <Settings size={16} className="flex-shrink-0" />
              <AnimatePresence mode="wait">
                {isOpen && (
                  <motion.span
                    initial={{ opacity: 0, width: 0 }}
                    animate={{ opacity: 1, width: 'auto' }}
                    exit={{ opacity: 0, width: 0 }}
                    transition={{ duration: 0.2 }}
                    className="whitespace-nowrap"
                  >
                    Settings
                  </motion.span>
                )}
              </AnimatePresence>
            </button>
            <button
              onClick={handleSignOut}
              className={cn(
                'flex items-center gap-3 w-full py-2 rounded-lg text-sm font-medium text-gray-300 hover:bg-gray-700 transition-colors',
                isOpen ? 'px-3' : 'justify-center px-0'
              )}
            >
              <LogOut size={16} className="flex-shrink-0" />
              <AnimatePresence mode="wait">
                {isOpen && (
                  <motion.span
                    initial={{ opacity: 0, width: 0 }}
                    animate={{ opacity: 1, width: 'auto' }}
                    exit={{ opacity: 0, width: 0 }}
                    transition={{ duration: 0.2 }}
                    className="whitespace-nowrap"
                  >
                    Sign Out
                  </motion.span>
                )}
              </AnimatePresence>
            </button>
          </div>
        </div>

        {/* Toggle Button */}
        <button
          onClick={onToggle}
          className="absolute top-1/2 -right-3 w-6 h-6 rounded-full flex items-center justify-center shadow bg-gray-600 hover:bg-gray-700 transition-colors z-50"
          aria-label={isOpen ? 'Collapse sidebar' : 'Expand sidebar'}
        >
          {isOpen ? <ChevronLeft size={12} className="text-gray-300" /> : <ChevronRight size={12} className="text-gray-300" />}
        </button>
      </motion.div>
    );
  });

CollapsibleSidebar.displayName = 'CollapsibleSidebar';

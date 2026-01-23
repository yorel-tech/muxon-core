'use client';

import { motion, AnimatePresence } from 'framer-motion';
import { forwardRef, useState } from 'react';
import { Bell, Search, Menu, User, LogOut, Settings, ChevronDown } from 'lucide-react';
import { cn } from '@/lib/utils';

export interface HeaderProps {
  user?: {
    name: string;
    email?: string;
    avatar?: string;
  };
  notifications?: number;
  onSearch?: (query: string) => void;
  onMenuClick?: () => void;
  onSettingsClick?: () => void;
  onLogout?: () => void;
  className?: string;
}

export const Header = forwardRef<HTMLDivElement, HeaderProps>(
  ({ user, notifications = 0, onSearch, onMenuClick, onSettingsClick, onLogout, className = '' }: HeaderProps, ref,
) => {
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  return (
    <header
      ref={ref}
      className={cn(
        'sticky top-0 z-30 bg-white border-b border-gray-200 shadow-sm',
        className,
      )}
    >
      <div className="flex items-center justify-between px-6 py-4">
        {/* Left: Logo and Search */}
        <div className="flex items-center gap-6">
          <div className="flex items-center gap-2">
            <div className="h-8 w-8 rounded-lg" style={{ background: 'linear-gradient(to bottom right, #3b82f6, #9333ea)' }} />
            <span className="text-xl font-bold text-gray-900">infron</span>
            {user?.email === 'admin@infron.dev' && (
              <span className="ml-2 text-xs px-2 py-0.5 bg-nexus-100 text-nexus-700 rounded-full font-medium">
                Enterprise
              </span>
            )}
          </div>
          <div className="relative">
            <motion.button
              onClick={() => setIsSearchOpen(!isSearchOpen)}
              className="p-2 rounded-md hover:bg-gray-100 transition-colors"
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
            >
              <Search size={20} className="text-gray-500" />
            </motion.button>
            <AnimatePresence>
              {isSearchOpen && (
                <motion.div
                  initial={{ opacity: 0, y: -10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -10 }}
                  transition={{ duration: 0.2 }}
                  className="absolute top-full left-0 mt-2 w-96 bg-white rounded-lg shadow-xl border border-gray-200 p-2"
                >
                  <input
                    type="search"
                    placeholder="Search..."
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500"
                    autoFocus
                  />
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </div>

        {/* Right: Notifications, Menu, User */}
        <div className="flex items-center gap-4">
          {/* Notifications */}
          <button className="relative p-2 rounded-md hover:bg-gray-100 transition-colors">
            <Bell size={20} className="text-gray-600" />
            {notifications > 0 && (
              <span className="absolute -top-1 -right-1 h-4 w-4 bg-error-500 rounded-full text-xs font-medium text-white flex items-center justify-center">
                {notifications > 99 ? '99+' : notifications}
              </span>
            )}
          </button>

          {/* Menu */}
          <button
            onClick={onMenuClick}
            className="p-2 rounded-md hover:bg-gray-100 transition-colors"
          >
            <Menu size={20} className="text-gray-600" />
          </button>

          {/* User */}
          <button className="flex items-center gap-2 p-2 rounded-md hover:bg-gray-100 transition-colors">
            {user?.avatar ? (
              <img
                src={user.avatar}
                alt={user.name}
                className="h-8 w-8 rounded-full object-cover"
              />
            ) : (
              <div className="h-8 w-8 rounded-full flex items-center justify-center text-white font-medium" style={{ background: 'linear-gradient(to bottom right, #3b82f6, #9333ea)' }}>
                {user?.name?.charAt(0).toUpperCase()}
              </div>
            )}
            <ChevronDown size={16} className="text-gray-400" />
          </button>

          {/* Actions */}
          <div className="border-l border-gray-200 pl-4">
            <button
              onClick={onSettingsClick}
              className="p-2 rounded-md hover:bg-gray-100 transition-colors"
              aria-label="Settings"
            >
              <Settings size={20} className="text-gray-600" />
            </button>
            <button
              onClick={onLogout}
              className="p-2 rounded-md hover:bg-red-50 hover:bg-red-100 transition-colors text-error-600"
              aria-label="Sign out"
            >
              <LogOut size={20} />
              <span className="ml-2 hidden sm:inline">Sign Out</span>
            </button>
          </div>
        </div>
      </div>
    </header>
  );
});

Header.displayName = 'Header';

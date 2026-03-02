'use client';

import { motion } from 'framer-motion';
import { forwardRef, useState, type ReactNode } from 'react';
import { cn } from '@/lib/utils';

export interface Tab {
  id: string;
  label: string;
  content: ReactNode;
  disabled?: boolean;
  icon?: ReactNode;
  badge?: number | string;
}

export interface TabsProps {
  tabs: Tab[];
  defaultTab?: string;
  onChange?: (tabId: string) => void;
  className?: string;
  variant?: 'default' | 'pills' | 'underline';
}

export const Tabs = forwardRef<HTMLDivElement, TabsProps>(
  ({ tabs, defaultTab, onChange, className = '', variant = 'default' }: TabsProps, ref,
) => {
  const [activeTab, setActiveTab] = useState(defaultTab || tabs[0]?.id);

  const handleTabChange = (tabId: string) => {
    setActiveTab(tabId);
    onChange?.(tabId);
  };

  return (
    <div ref={ref} className={cn('w-full', className)}>
      {/* Tab Headers */}
      <div className={cn('flex space-x-1 mb-4', {
        'flex-wrap': variant === 'pills',
        'border-b border-gray-200': variant === 'underline',
      })}>
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => !tab.disabled && handleTabChange(tab.id)}
            disabled={tab.disabled}
            className={cn(
              'relative px-4 py-2 text-sm font-medium transition-colors',
              {
                'border-b-2 border-transparent': variant === 'underline',
                'rounded-full': variant === 'pills',
                'hover:bg-gray-100': !tab.disabled,
                'text-gray-600 hover:text-gray-900': !tab.disabled,
                'text-gray-400 cursor-not-allowed': tab.disabled,
                'text-primary-600 border-primary-600 bg-primary-50': activeTab === tab.id && variant === 'default',
                'text-primary-600': activeTab === tab.id && variant === 'pills',
                'border-b-2 border-primary-600': activeTab === tab.id && variant === 'underline',
              },
            )}
          >
            <span className="flex items-center gap-2">
              {tab.icon && <span className="text-gray-500">{tab.icon}</span>}
              <span>{tab.label}</span>
              {tab.badge && (
                <span className="ml-2 px-2 py-0.5 text-xs bg-gray-200 text-gray-600 rounded-full">
                  {tab.badge}
                </span>
              )}
            </span>
            {activeTab === tab.id && variant === 'underline' && (
              <motion.div
                layoutId="activeTab"
                className="absolute bottom-0 left-0 right-0 h-0.5 bg-primary-600"
                initial={false}
                animate={{ opacity: 1 }}
                transition={{ duration: 0.2 }}
              />
            )}
          </button>
        ))}
      </div>

      {/* Tab Content - use exclusive visibility so active tab is shown (hidden + block conflict in Tailwind can hide content) */}
      <div className="relative">
        {tabs.map((tab) => (
          <div
            key={tab.id}
            className={activeTab === tab.id ? 'block' : 'hidden'}
            role="tabpanel"
            aria-hidden={activeTab !== tab.id}
          >
            {tab.content}
          </div>
        ))}
      </div>
    </div>
  );
});

Tabs.displayName = 'Tabs';

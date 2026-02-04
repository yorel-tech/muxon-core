'use client';

import { motion } from 'framer-motion';
import { Server, Users, Building2, KeyRound, FileText } from 'lucide-react';
import { Card, CardContent, CardHeader } from '@/components/ui/atoms/card';

export interface QuickAction {
  id: string;
  label: string;
  description: string;
  icon: React.ReactNode;
  onClick: () => void;
  color: 'blue' | 'purple' | 'green' | 'orange' | 'pink';
}

export interface QuickActionsProps {
  actions: QuickAction[];
  className?: string;
}

const getColorClasses = (color: QuickAction['color']) => {
  switch (color) {
    case 'blue':
      return {
        bg: 'bg-blue-50',
        icon: 'text-primary-600',
        hoverBg: 'hover:bg-blue-50',
        hoverBorder: 'hover:border-primary-300',
      };
    case 'purple':
      return {
        bg: 'bg-purple-50',
        icon: 'text-purple-600',
        hoverBg: 'hover:bg-purple-50',
        hoverBorder: 'hover:border-purple-300',
      };
    case 'green':
      return {
        bg: 'bg-green-50',
        icon: 'text-green-600',
        hoverBg: 'hover:bg-green-50',
        hoverBorder: 'hover:border-green-300',
      };
    case 'orange':
      return {
        bg: 'bg-orange-50',
        icon: 'text-orange-600',
        hoverBg: 'hover:bg-orange-50',
        hoverBorder: 'hover:border-orange-300',
      };
    case 'pink':
      return {
        bg: 'bg-pink-50',
        icon: 'text-pink-600',
        hoverBg: 'hover:bg-pink-50',
        hoverBorder: 'hover:border-pink-300',
      };
  }
};

export const QuickActions = ({ actions, className = '' }: QuickActionsProps) => {
  return (
    <Card className={className}>
      <CardHeader>
        <h2 className="text-xl font-semibold text-gray-900">Quick Actions</h2>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
          {actions.map((action, index) => {
            const colors = getColorClasses(action.color);
            return (
              <motion.button
                key={action.id}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.2, delay: index * 0.05 }}
                onClick={action.onClick}
                className={`
                  flex items-center gap-3 p-4 rounded-lg border border-gray-200 
                  ${colors.hoverBg} ${colors.hoverBorder}
                  transition-colors text-left w-full
                `}
              >
                <div className={`flex h-10 w-10 items-center justify-center rounded-lg ${colors.bg} flex-shrink-0`}>
                  <span className={colors.icon}>{action.icon}</span>
                </div>
                <div className="min-w-0 flex-1">
                  <p className="font-medium text-gray-900 text-sm truncate">{action.label}</p>
                  <p className="text-xs text-gray-500 truncate">{action.description}</p>
                </div>
              </motion.button>
            );
          })}
        </div>
      </CardContent>
    </Card>
  );
};

// Default quick actions for system dashboard
export const defaultQuickActions: QuickAction[] = [
  {
    id: 'add-datacenter',
    label: 'Add Datacenter',
    description: 'Connect new infrastructure',
    icon: <Server size={20} />,
    onClick: () => console.log('Add Datacenter clicked'),
    color: 'blue',
  },
  {
    id: 'add-user',
    label: 'Add User',
    description: 'Create system user',
    icon: <Users size={20} />,
    onClick: () => console.log('Add User clicked'),
    color: 'purple',
  },
  {
    id: 'create-tenant',
    label: 'Create Tenant',
    description: 'Add organization',
    icon: <Building2 size={20} />,
    onClick: () => console.log('Create Tenant clicked'),
    color: 'green',
  },
  {
    id: 'configure-idp',
    label: 'Configure IDP',
    description: 'Set up authentication',
    icon: <KeyRound size={20} />,
    onClick: () => console.log('Configure IDP clicked'),
    color: 'orange',
  },
  {
    id: 'view-logs',
    label: 'View Logs',
    description: 'System activity',
    icon: <FileText size={20} />,
    onClick: () => console.log('View Logs clicked'),
    color: 'pink',
  },
];

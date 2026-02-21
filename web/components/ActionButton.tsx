import React, { useState } from 'react';
import { Button } from '@/components/ui/atoms/button';
import { usePermissions } from '@/hooks/usePermissions';
import { executeAction } from '@/lib/hateoas';
import { Link } from '@/types/provider';

interface ActionButtonProps {
  entity: { _links: Link[] };
  action: string;
  children: React.ReactNode;
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  disabled?: boolean;
  loading?: boolean;
  onSuccess?: (result?: any) => void;
  onError?: (error: Error) => void;
  onComplete?: () => void;
  confirmMessage?: string;
  className?: string;
  icon?: React.ReactNode;
  showIcon?: boolean;
  payload?: any;
}

export const ActionButton: React.FC<ActionButtonProps> = ({
  entity,
  action,
  children,
  variant = 'primary',
  size = 'md',
  disabled = false,
  loading = false,
  onSuccess,
  onError,
  onComplete,
  confirmMessage,
  className = '',
  icon,
  showIcon = true,
  payload
}) => {
  const [isLoading, setIsLoading] = useState(false);
  const { getActionLink, canPerformAction, getDisabledReason } = usePermissions(entity);
  const link = getActionLink(action);

  if (!link) return null;

  const isActionEnabled = canPerformAction(action);
  const disabledReason = getDisabledReason(action);

  const handleClick = async () => {
    if (!isActionEnabled || isLoading || loading) return;

    // Show confirmation dialog if provided
    if (confirmMessage) {
      const confirmed = window.confirm(confirmMessage);
      if (!confirmed) return;
    }

    setIsLoading(true);
    
    try {
      const result = await executeAction(link, payload);
      onSuccess?.(result);
    } catch (error) {
      console.error(`Action ${action} failed:`, error);
      onError?.(error as Error);
    } finally {
      setIsLoading(false);
      onComplete?.();
    }
  };

  // Determine the actual variant based on action type and state
  const getButtonVariant = () => {
    if (!isActionEnabled) return 'secondary';
    
    // Use explicit variant if provided
    if (variant !== 'primary') return variant;
    
    // Auto-determine variant based on action type
    switch (action) {
      case 'delete':
        return 'danger';
      case 'disable':
        return 'secondary';
      case 'sync':
      case 'testConnection':
        return 'secondary';
      default:
        return 'primary';
    }
  };

  // Get the icon for this action
  const getActionIcon = () => {
    if (icon) return icon;
    
    // Import icons dynamically to avoid circular dependencies
    const icons = {
      Eye: () => import('lucide-react').then(mod => <mod.Eye size={16} />),
      Pencil: () => import('lucide-react').then(mod => <mod.Pencil size={16} />),
      Trash2: () => import('lucide-react').then(mod => <mod.Trash2 size={16} />),
      RefreshCw: () => import('lucide-react').then(mod => <mod.RefreshCw size={16} />),
      Plug: () => import('lucide-react').then(mod => <mod.Plug size={16} />),
      Ban: () => import('lucide-react').then(mod => <mod.Ban size={16} />),
      Server: () => import('lucide-react').then(mod => <mod.Server size={16} />),
      Cpu: () => import('lucide-react').then(mod => <mod.Cpu size={16} />),
      Plus: () => import('lucide-react').then(mod => <mod.Plus size={16} />),
      Settings: () => import('lucide-react').then(mod => <mod.Settings size={16} />),
      FileText: () => import('lucide-react').then(mod => <mod.FileText size={16} />),
      Database: () => import('lucide-react').then(mod => <mod.Database size={16} />),
      Activity: () => import('lucide-react').then(mod => <mod.Activity size={16} />),
    };
    
    const IconComponent = icons[action as keyof typeof icons];
    return IconComponent ? <IconComponent /> : null;
  };

  const buttonContent = (
    <>
      {showIcon && getActionIcon()}
      {children}
      {(isLoading || loading) && (
        <svg 
          className="animate-spin -mr-1 ml-2 h-4 w-4" 
          xmlns="http://www.w3.org/2000/svg" 
          fill="none" 
          viewBox="0 0 24 24"
        >
          <circle 
            className="opacity-25" 
            cx="12" 
            cy="12" 
            r="10" 
            stroke="currentColor" 
            strokeWidth="4"
          />
          <path 
            className="opacity-75" 
            fill="currentColor" 
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
          />
        </svg>
      )}
    </>
  );

  return (
    <Button
      onClick={handleClick}
      disabled={disabled || !isActionEnabled || isLoading || loading}
      aria-label={disabledReason || link.title}
      variant={getButtonVariant()}
      size={size}
      className={className}
    >
      {buttonContent}
    </Button>
  );
};

/**
 * Specialized ActionButton for destructive actions
 */
export const DestructiveActionButton: React.FC<Omit<ActionButtonProps, 'variant'>> = (props) => {
  return (
    <ActionButton
      {...props}
      variant="danger"
      confirmMessage={`Are you sure you want to ${props.action}? This action cannot be undone.`}
    />
  );
};

/**
 * Specialized ActionButton for management actions
 */
export const ManagementActionButton: React.FC<Omit<ActionButtonProps, 'variant'>> = (props) => {
  return <ActionButton {...props} variant="secondary" />;
};

/**
 * Specialized ActionButton for primary actions
 */
export const PrimaryActionButton: React.FC<Omit<ActionButtonProps, 'variant'>> = (props) => {
  return <ActionButton {...props} variant="primary" />;
};

/**
 * Icon-only version of ActionButton
 */
export const IconButton: React.FC<Omit<ActionButtonProps, 'showIcon'>> = (props) => {
  return (
    <ActionButton
      {...props}
      showIcon={true}
      size="sm"
      className={`p-2 ${props.className}`}
    />
  );
};
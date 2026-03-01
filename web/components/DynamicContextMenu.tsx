import React from 'react';
import { Dropdown, DropdownOption } from '@/components/ui/molecules/dropdown';
import { usePermissions } from '@/hooks/usePermissions';
import { getActionIcon, sortActionsByPriority } from '@/lib/hateoas';
import { Link } from '@/types/provider';

/** Shared three-dot row actions trigger used as first column on list pages (providers, tenants, datacenters, etc.) */
export const RowActionsTrigger: React.FC<{ title?: string; className?: string }> = ({
  title = 'Actions',
  className = '',
}) => (
  <button
    type="button"
    className={`p-1.5 rounded hover:bg-gray-100 transition-colors ${className}`.trim()}
    title={title}
  >
    <svg
      className="w-4 h-4 text-gray-600"
      fill="none"
      stroke="currentColor"
      viewBox="0 0 24 24"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z"
      />
    </svg>
  </button>
);

interface DynamicContextMenuProps {
  entity: { _links: Link[]; id: string; name: string };
  onAction: (action: string, entity: any) => void;
  position?: 'left' | 'right';
  usePortal?: boolean;
  showDisabled?: boolean;
  categorize?: boolean;
}

export const DynamicContextMenu: React.FC<DynamicContextMenuProps> = ({ 
  entity, 
  onAction,
  position = 'right',
  usePortal = true,
  showDisabled = false,
  categorize = false
}) => {
  const { enabledActions, actionsByCategory, getDisabledReason } = usePermissions(entity);
  const sortedActions = sortActionsByPriority(enabledActions);

  const createOptionFromLink = (link: Link): DropdownOption => ({
    label: link.title,
    icon: React.createElement(getActionIcon(link.rel), { size: 14 }),
    onClick: () => onAction(link.rel, entity),
    disabled: !link.enabled,
    description: link.reason || undefined,
    variant: link.rel === 'delete' ? 'danger' :
            link.rel === 'disable' ? 'warning' :
            'default'
  });

  const createDisabledOption = (link: Link): DropdownOption => ({
    label: link.title,
    icon: React.createElement(getActionIcon(link.rel), { size: 14 }),
    onClick: () => {}, // No action for disabled items
    disabled: true,
    description: link.reason || 'Action not available',
    variant: 'default'
  });

  let options: DropdownOption[] = [];

  if (categorize) {
    // Group actions by category
    const categories = [
      { key: 'primary', label: 'Actions', items: actionsByCategory.primary },
      { key: 'management', label: 'Management', items: actionsByCategory.management },
      { key: 'creation', label: 'Create', items: actionsByCategory.creation },
      { key: 'navigation', label: 'Navigate', items: actionsByCategory.navigation },
      { key: 'destructive', label: 'Danger Zone', items: actionsByCategory.destructive }
    ];

    categories.forEach(category => {
      if (category.items.length > 0) {
        if (options.length > 0) {
          // Add separator between categories
          options.push({ label: '─', disabled: true, onClick: () => {} });
        }
        
        category.items.forEach(link => {
          options.push(createOptionFromLink(link));
        });
      }
    });

    // Add disabled actions if requested
    if (showDisabled) {
      const allLinks = entity._links || [];
      const disabledLinks = allLinks.filter(link => !link.enabled);
      
      if (disabledLinks.length > 0) {
        if (options.length > 0) {
          options.push({ label: '─', disabled: true, onClick: () => {} });
        }
        
        disabledLinks.forEach(link => {
          options.push(createDisabledOption(link));
        });
      }
    }
  } else {
    // Simple flat list
    sortedActions.forEach(link => {
      options.push(createOptionFromLink(link));
    });

    // Add disabled actions if requested
    if (showDisabled) {
      const allLinks = entity._links || [];
      const disabledLinks = allLinks.filter(link => !link.enabled);
      
      if (disabledLinks.length > 0) {
        if (options.length > 0) {
          options.push({ label: '─', disabled: true, onClick: () => {} });
        }
        
        disabledLinks.forEach(link => {
          options.push(createDisabledOption(link));
        });
      }
    }
  }

  // If no actions available, show a message
  if (options.length === 0) {
    options.push({
      label: 'No actions available',
      icon: null,
      onClick: () => {},
      disabled: true
    });
  }

  return (
    <Dropdown
      trigger={
        <RowActionsTrigger
          title={enabledActions.length > 0 ? `${enabledActions.length} actions available` : 'No actions available'}
        />
      }
      options={options}
      position={position}
      usePortal={usePortal}
    />
  );
};

/**
 * Compact version of DynamicContextMenu for use in tight spaces
 */
export const CompactContextMenu: React.FC<Omit<DynamicContextMenuProps, 'categorize'>> = (props) => {
  return <DynamicContextMenu {...props} categorize={false} showDisabled={false} />;
};

/**
 * Full-featured version of DynamicContextMenu with all options
 */
export const FullContextMenu: React.FC<DynamicContextMenuProps> = (props) => {
  return <DynamicContextMenu {...props} categorize={true} showDisabled={true} />;
};
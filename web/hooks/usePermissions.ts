import { useMemo } from 'react';
import { Link } from '@/types/provider';
import { 
  findLink, 
  isActionEnabled, 
  getActionReason, 
  getEnabledActions,
  groupActionsByCategory,
  sortActionsByPriority 
} from '@/lib/hateoas';

interface UsePermissionsProps {
  _links?: Link[];
}

/**
 * Hook for managing permissions and actions based on HATEOAS links
 */
export const usePermissions = ({ _links = [] }: UsePermissionsProps) => {
  const links = useMemo(() => _links, [_links]);

  /**
   * Check if a user can perform a specific action
   */
  const canPerformAction = useMemo(() => 
    (action: string): boolean => {
      return isActionEnabled(links, action);
    }, [links]
  );

  /**
   * Get the reason why an action is disabled
   */
  const getDisabledReason = useMemo(() =>
    (action: string): string | undefined => {
      return getActionReason(links, action);
    }, [links]
  );

  /**
   * Get the link object for a specific action
   */
  const getActionLink = useMemo(() => 
    (action: string): Link | undefined => {
      return findLink(links, action);
    }, [links]
  );

  /**
   * Get all enabled actions
   */
  const enabledActions = useMemo(() => {
    return getEnabledActions(links);
  }, [links]);

  /**
   * Get actions grouped by category
   */
  const actionsByCategory = useMemo(() => {
    return groupActionsByCategory(links);
  }, [links]);

  /**
   * Get sorted actions for display
   */
  const sortedActions = useMemo(() => {
    return sortActionsByPriority(enabledActions);
  }, [enabledActions]);

  /**
   * Check if any destructive actions are available
   */
  const hasDestructiveActions = useMemo(() => {
    return actionsByCategory.destructive.length > 0;
  }, [actionsByCategory]);

  /**
   * Check if any management actions are available
   */
  const hasManagementActions = useMemo(() => {
    return actionsByCategory.management.length > 0;
  }, [actionsByCategory]);

  /**
   * Check if any creation actions are available
   */
  const hasCreationActions = useMemo(() => {
    return actionsByCategory.creation.length > 0;
  }, [actionsByCategory]);

  /**
   * Get primary actions (view, edit)
   */
  const primaryActions = useMemo(() => {
    return actionsByCategory.primary;
  }, [actionsByCategory]);

  /**
   * Check if entity has any enabled actions
   */
  const hasAnyActions = useMemo(() => {
    return enabledActions.length > 0;
  }, [enabledActions]);

  /**
   * Get action count for badges/indicators
   */
  const actionCount = useMemo(() => {
    return enabledActions.length;
  }, [enabledActions]);

  return {
    // Basic permission checks
    canPerformAction,
    getDisabledReason,
    getActionLink,
    
    // Action collections
    enabledActions,
    sortedActions,
    actionsByCategory,
    primaryActions,
    
    // Convenience checks
    hasAnyActions,
    hasDestructiveActions,
    hasManagementActions,
    hasCreationActions,
    actionCount,
    
    // Raw links for advanced usage
    links
  };
};

/**
 * Hook for managing permissions on multiple entities
 */
export const useBulkPermissions = (entities: { _links?: Link[] }[]) => {
  const entityPermissions = useMemo(() => {
    return entities.map(entity => usePermissions(entity));
  }, [entities]);

  /**
   * Check if action is available on all entities
   */
  const canPerformOnAll = useMemo(() => 
    (action: string): boolean => {
      return entityPermissions.every(permissions => 
        permissions.canPerformAction(action)
      );
    }, [entityPermissions]
  );

  /**
   * Check if action is available on any entity
   */
  const canPerformOnAny = useMemo(() => 
    (action: string): boolean => {
      return entityPermissions.some(permissions => 
        permissions.canPerformAction(action)
      );
    }, [entityPermissions]
  );

  /**
   * Get common actions available on all entities
   */
  const commonActions = useMemo(() => {
    if (entityPermissions.length === 0) return [];
    
    const firstEntityActions = new Set(
      entityPermissions[0].enabledActions.map(link => link.rel)
    );
    
    return entityPermissions[0].enabledActions.filter(link => 
      entityPermissions.every(permissions => 
        permissions.canPerformAction(link.rel)
      )
    );
  }, [entityPermissions]);

  return {
    entityPermissions,
    canPerformOnAll,
    canPerformOnAny,
    commonActions
  };
};
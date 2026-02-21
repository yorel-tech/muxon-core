import { create } from 'zustand';

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message?: string;
  duration?: number;
  action?: {
    label: string;
    onClick: () => void;
  };
}

interface ToastStore {
  toasts: ToastMessage[];
  add: (toast: Omit<ToastMessage, 'id'>) => void;
  remove: (id: string) => void;
  clear: () => void;
}

/**
 * Toast notification system using Zustand for state management
 */
export const useToastStore = create<ToastStore>((set, get) => ({
  toasts: [],
  
  add: (toast) => {
    const id = Math.random().toString(36).substr(2, 9);
    const newToast: ToastMessage = {
      id,
      duration: toast.duration || 5000,
      ...toast,
    };
    
    set((state) => ({
      toasts: [...state.toasts, newToast]
    }));
    
    // Auto-remove toast after duration
    setTimeout(() => {
      set((state) => ({
        toasts: state.toasts.filter((t) => t.id !== id)
      }));
    }, newToast.duration);
  },
  
  remove: (id) => {
    set((state) => ({
      toasts: state.toasts.filter((t) => t.id !== id)
    }));
  },
  
  clear: () => {
    set({ toasts: [] });
  },
}));

/**
 * Hook for using toast notifications
 */
export const useToast = () => {
  const { toasts, add, remove, clear } = useToastStore();
  
  const toast = {
    success: (title: string, message?: string) => 
      add({ type: 'success', title, message }),
    error: (title: string, message?: string) => 
      add({ type: 'error', title, message, duration: 7000 }),
    warning: (title: string, message?: string) => 
      add({ type: 'warning', title, message, duration: 6000 }),
    info: (title: string, message?: string) => 
      add({ type: 'info', title, message }),
    
    // Custom toast with action
    custom: (toast: Omit<ToastMessage, 'id'>) => add(toast),
  };
  
  return {
    toasts,
    toast,
    remove,
    clear,
  };
};

/**
 * Convenience functions for common toast patterns
 */
export const toastHelpers = {
  // Success messages
  providerCreated: (name: string) => ({
    type: 'success' as const,
    title: 'Provider Created',
    message: `Provider "${name}" has been successfully created.`,
  }),
  
  providerDeleted: (name: string) => ({
    type: 'success' as const,
    title: 'Provider Deleted',
    message: `Provider "${name}" has been successfully deleted.`,
  }),
  
  providerSynced: (name: string) => ({
    type: 'success' as const,
    title: 'Provider Synced',
    message: `Provider "${name}" has been successfully synced.`,
  }),
  
  actionCompleted: (action: string, target: string) => ({
    type: 'success' as const,
    title: 'Action Completed',
    message: `${action} action completed successfully for ${target}.`,
  }),
  
  // Error messages
  actionFailed: (action: string, target: string, error?: string) => ({
    type: 'error' as const,
    title: 'Action Failed',
    message: `Failed to ${action} ${target}${error ? `: ${error}` : ''}`,
  }),
  
  networkError: (action: string) => ({
    type: 'error' as const,
    title: 'Network Error',
    message: `Failed to ${action}. Please check your connection and try again.`,
  }),
  
  // Warning messages
  resourceNotEmpty: (resourceType: string) => ({
    type: 'warning' as const,
    title: 'Resource Not Empty',
    message: `Cannot delete ${resourceType} with active resources.`,
  }),
  
  // Info messages
  loadingData: (dataType: string) => ({
    type: 'info' as const,
    title: 'Loading Data',
    message: `Loading ${dataType}...`,
  }),
};
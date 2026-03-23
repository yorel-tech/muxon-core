'use client';

import { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { fetchOidcConfigIfNeeded, getUserManager } from './oidc';

interface AuthContextType {
  isAuthenticated: boolean;
  isLoading: boolean;
  checkAuth: () => Promise<void>;
  user?: {
    name: string;
    email: string;
    avatar?: string;
  };
  userRole?: 'system' | 'tenant';
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [user, setUser] = useState<{ name: string; email: string; avatar?: string }>();
  const [userRole, setUserRole] = useState<'system' | 'tenant'>('tenant');

  const checkAuth = async () => {
    setIsLoading(true);
    try {
      await fetchOidcConfigIfNeeded();
      const um = getUserManager();
      if (!um) {
        setIsAuthenticated(false);
        setUser(undefined);
        return;
      }
      const userData = await um.getUser();
      if (userData) {
        setIsAuthenticated(true);
        setUser({
          name: userData.profile.name || userData.profile.preferred_username || 'User',
          email: userData.profile.email || '',
          avatar: userData.profile.picture,
        });
        
        // Extract roles from OIDC token
        // Roles can be in different locations depending on IDP configuration
        const profile = userData.profile as any;
        const roles = profile.roles || profile.realm_access?.roles || [];
        
        // Determine user role based on roles
        // 'system:admin' maps to 'system', other roles map to 'tenant'
        const hasSystemRole = roles.some((role: string) =>
          role === 'system:admin' || role.startsWith('system:')
        );
        setUserRole(hasSystemRole ? 'system' : 'tenant');
      } else {
        setIsAuthenticated(false);
        setUser(undefined);
      }
    } catch (error) {
      console.error('Error checking auth:', error);
      setIsAuthenticated(false);
      setUser(undefined);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    checkAuth();
  }, []);

  return (
    <AuthContext.Provider value={{ isAuthenticated, isLoading, checkAuth, user, userRole }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}

/**
 * Authenticated API client wrapper
 * Automatically adds Bearer token from OIDC session to all requests
 */

import { getUserManager, clearUserSession } from './oidc';
import { Link } from '@/types/provider';

// Global auth state for components to check
let authCheckPromise: Promise<void> | null = null;

/**
 * Trigger auth check across all components after session changes
 */
export function triggerAuthCheck() {
  // Dispatch a custom event to notify all components to re-check auth
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('infron:auth-changed'));
  }
}

export interface ApiRequestOptions extends RequestInit {
  /**
   * Whether to require authentication. If true and no token is available,
   * the request will fail. Default: true.
   */
  requireAuth?: boolean;
  /**
   * Base URL for the API. If not provided, uses NEXT_PUBLIC_API_BASE env var
   * or defaults to relative paths for Next.js API routes.
   */
  baseUrl?: string;
}

/**
 * Get the current access token from OIDC session
 */
async function getAccessToken(): Promise<string | null> {
  const um = getUserManager();
  if (!um) return null;

  try {
    const user = await um.getUser();
    return user?.access_token || null;
  } catch (error) {
    console.error('Error getting access token:', error);
    return null;
  }
}

/**
 * Convert HeadersInit to Record<string, string>
 */
function headersToRecord(headers: HeadersInit | undefined): Record<string, string> {
  if (!headers) return {};

  if (headers instanceof Headers) {
    const record: Record<string, string> = {};
    headers.forEach((value, key) => {
      record[key] = value;
    });
    return record;
  }

  if (Array.isArray(headers)) {
    const record: Record<string, string> = {};
    headers.forEach(([key, value]) => {
      record[key] = value;
    });
    return record;
  }

  return headers as Record<string, string>;
}

/**
 * Make an authenticated API request
 * Automatically adds Bearer token from OIDC session
 */
export async function apiRequest<T = any>(
  input: RequestInfo | URL,
  options: ApiRequestOptions = {}
): Promise<T> {
  const {
    requireAuth = true,
    baseUrl,
    headers: customHeaders,
    ...fetchOptions
  } = options;

  // Get access token
  const token = await getAccessToken();

  // Check if auth is required but no token is available
  if (requireAuth && !token) {
    throw new Error('Authentication required but no access token available');
  }

  // Build URL
  let url: string;
  if (typeof input === 'string') {
    // If baseUrl is provided and input is a relative path, prepend baseUrl
    if (baseUrl && !input.startsWith('http')) {
      url = `${baseUrl}${input.startsWith('/') ? '' : '/'}${input}`;
    } else if (!input.startsWith('http') && !input.startsWith('/')) {
      // Default to relative path for Next.js API routes
      url = `/api/${input}`;
    } else {
      url = input;
    }
  } else {
    url = input.toString();
  }

  // Build headers with Authorization
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...headersToRecord(customHeaders),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  // Make the request
  const response = await fetch(url, {
    ...fetchOptions,
    headers,
  });

  // Handle non-OK responses
  if (!response.ok) {
    // Handle 401 Unauthorized - token expired or invalid
    if (response.status === 401) {
      // Clear the user session and redirect to home page
      await clearUserSession();
      window.location.href = '/';
      // Throw to prevent further processing
      throw new Error('Authentication expired. Redirecting to login...');
    }
    
    const errorText = await response.text();
    throw new Error(
      `API request failed: ${response.status} ${response.statusText}${errorText ? ` - ${errorText}` : ''}`
    );
  }

  // No content (e.g. 204)
  if (response.status === 204) {
    return undefined as T;
  }
  // Parse response
  const contentType = response.headers.get('content-type');
  if (contentType?.includes('application/json')) {
    return response.json();
  }
  return response.text() as T;
}

/**
 * Convenience method for GET requests
 */
export async function apiGet<T = any>(
  input: RequestInfo | URL,
  options?: Omit<ApiRequestOptions, 'method'>
): Promise<T> {
  return apiRequest<T>(input, { ...options, method: 'GET' });
}

/**
 * Convenience method for POST requests
 */
export async function apiPost<T = any>(
  input: RequestInfo | URL,
  body?: any,
  options?: Omit<ApiRequestOptions, 'method' | 'body'>
): Promise<T> {
  return apiRequest<T>(input, {
    ...options,
    method: 'POST',
    body: body ? JSON.stringify(body) : undefined,
  });
}

/**
 * Convenience method for PUT requests
 */
export async function apiPut<T = any>(
  input: RequestInfo | URL,
  body?: any,
  options?: Omit<ApiRequestOptions, 'method' | 'body'>
): Promise<T> {
  return apiRequest<T>(input, {
    ...options,
    method: 'PUT',
    body: body ? JSON.stringify(body) : undefined,
  });
}

/**
 * Convenience method for DELETE requests
 */
export async function apiDelete<T = any>(
  input: RequestInfo | URL,
  options?: Omit<ApiRequestOptions, 'method'>
): Promise<T> {
  return apiRequest<T>(input, { ...options, method: 'DELETE' });
}

/**
 * Convenience method for PATCH requests
 */
export async function apiPatch<T = any>(
  input: RequestInfo | URL,
  body?: any,
  options?: Omit<ApiRequestOptions, 'method' | 'body'>
): Promise<T> {
  return apiRequest<T>(input, {
    ...options,
    method: 'PATCH',
    body: body ? JSON.stringify(body) : undefined,
  });
}

/**
 * Enhanced API client with HATEOAS support
 */
export async function apiGetWithLinks<T extends { _links?: Link[] }>(
  input: RequestInfo | URL,
  options?: Omit<ApiRequestOptions, 'method'>
): Promise<T> {
  const response = await apiGet<T>(input, options);
  
  // Ensure _links array exists
  if (response && !response._links) {
    (response as any)._links = [];
  }
  
  return response;
}

/**
 * Normalize link href so requests go through the app (same-origin) and get the auth token.
 * If the backend returns an absolute URL (e.g. http://localhost:8080/api/v1/...), use the pathname
 * so the request hits Next.js /api/v1 proxy and is authenticated.
 */
function normalizeLinkHref(href: string): string {
  if (typeof href !== 'string' || !href.startsWith('http')) {
    return href;
  }
  try {
    const u = new URL(href);
    return u.pathname + u.search;
  } catch {
    return href;
  }
}

/**
 * Execute an action using a HATEOAS link (authenticated, via same-origin proxy when possible).
 */
export async function executeLinkAction<T = any>(
  link: Link,
  payload?: any,
  options?: Omit<ApiRequestOptions, 'method' | 'body' | 'url'>
): Promise<T> {
  const url = normalizeLinkHref(link.href);
  return apiRequest<T>(url, {
    ...options,
    method: link.method,
    body: payload ? JSON.stringify(payload) : undefined,
  });
}

/**
 * Enhanced fetch providers with HATEOAS support
 * Fetches from backend /api/v1/providers (proxied via Next.js rewrites)
 */
export async function fetchProvidersWithLinks(): Promise<any[]> {
  const data = await apiGetWithLinks('/api/v1/providers');
  const providersList = Array.isArray(data) ? data : ((data as any)?.items || []);
  // Ensure all providers have _links array
  return providersList.map((provider: any) => ({
    ...provider,
    _links: provider._links || generateDefaultLinks(provider),
  }));
}

/**
 * Generate default HATEOAS links for a provider
 */
function generateDefaultLinks(provider: any): Link[] {
  const baseLinks: Link[] = [
    {
      rel: 'self',
      href: `/api/v1/providers/${provider.id}`,
      method: 'GET',
      title: 'View Details',
      enabled: true
    },
    {
      rel: 'edit',
      href: `/api/v1/providers/${provider.id}`,
      method: 'PUT',
      title: 'Edit',
      enabled: true,
      reason: 'Requires provider:update permission'
    },
    {
      rel: 'sync',
      href: `/api/v1/providers/${provider.id}/sync`,
      method: 'POST',
      title: 'Sync',
      enabled: provider.status !== 'offline',
      reason: provider.status === 'offline' ? 'Provider is offline' : undefined
    },
    {
      rel: 'testConnection',
      href: `/api/v1/providers/${provider.id}/test-connection`,
      method: 'POST',
      title: 'Test Connection',
      enabled: true
    }
  ];

  // Add provider-specific actions
  if (provider.type === 'libvirt') {
    baseLinks.push(
      {
        rel: 'addCluster',
        href: `/api/v1/providers/${provider.id}/node-clusters`,
        method: 'POST',
        title: 'Add Cluster',
        enabled: true,
        reason: 'Requires cluster:create permission'
      },
      {
        rel: 'addNode',
        href: `/api/v1/providers/${provider.id}/nodes`,
        method: 'POST',
        title: 'Add Node',
        enabled: true,
        reason: 'Requires node:create permission'
      }
    );
  }

  // Add delete action with conditions
  baseLinks.push({
    rel: 'delete',
    href: `/api/v1/providers/${provider.id}`,
    method: 'DELETE',
    title: 'Delete',
    enabled: (provider.nodes || 0) === 0 && (provider.vms || 0) === 0,
    reason: ((provider.nodes || 0) > 0 || (provider.vms || 0) > 0)
      ? 'Cannot delete provider with active resources'
      : 'Requires provider:delete permission'
  });

  return baseLinks;
}


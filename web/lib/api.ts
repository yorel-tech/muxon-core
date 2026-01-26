/**
 * Authenticated API client wrapper
 * Automatically adds Bearer token from OIDC session to all requests
 */

import { getUserManager, clearUserSession } from './oidc';

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

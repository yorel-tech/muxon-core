// No 'use client' here is fine; we guard access to window.
// If you prefer, you can add 'use client' at the top to force client-only.

import { UserManager, WebStorageStateStore, Log } from 'oidc-client-ts';

let _manager: UserManager | null = null;

/** Filled after fetchOidcConfigIfNeeded() when client bundle has no NEXT_PUBLIC_* (common in dev / Turbopack). */
type OidcEnv = {
  authority: string;
  client_id: string;
  redirect_uri: string;
  post_logout_redirect_uri: string;
  scope: string;
};

let _runtimeEnv: OidcEnv | null = null;

/** Shown when required NEXT_PUBLIC_OIDC_* vars are missing or empty (restart dev server after editing .env.local). */
export const OIDC_NOT_CONFIGURED_MESSAGE =
  'OIDC is not configured. Copy web/.env.example to .env.local and set NEXT_PUBLIC_OIDC_AUTHORITY, NEXT_PUBLIC_OIDC_CLIENT_ID, NEXT_PUBLIC_OIDC_REDIRECT_URI, and NEXT_PUBLIC_OIDC_POST_LOGOUT_REDIRECT_URI, then restart the dev server. If you already set these in .env.local, restart the dev server—Next.js only picks up changes to NEXT_PUBLIC_* when the dev server starts.';

function readOidcEnvFromProcess(): OidcEnv {
  return {
    authority: (process.env.NEXT_PUBLIC_OIDC_AUTHORITY || '').trim(),
    client_id: (process.env.NEXT_PUBLIC_OIDC_CLIENT_ID || '').trim(),
    redirect_uri: (process.env.NEXT_PUBLIC_OIDC_REDIRECT_URI || '').trim(),
    post_logout_redirect_uri: (process.env.NEXT_PUBLIC_OIDC_POST_LOGOUT_REDIRECT_URI || '').trim(),
    scope: (process.env.NEXT_PUBLIC_OIDC_SCOPE || 'openid profile email').trim(),
  };
}

function readOidcEnv(): OidcEnv {
  if (_runtimeEnv) return _runtimeEnv;
  return readOidcEnvFromProcess();
}

/**
 * Loads OIDC settings from the Next server (reads .env.local at runtime). Call before getUserManager when
 * NEXT_PUBLIC_* is missing in the browser bundle.
 */
export async function fetchOidcConfigIfNeeded(): Promise<boolean> {
  if (typeof window === 'undefined') return false;
  if (_runtimeEnv) return isOidcConfigured();
  const fromProcess = readOidcEnvFromProcess();
  if (
    fromProcess.authority &&
    fromProcess.client_id &&
    fromProcess.redirect_uri &&
    fromProcess.post_logout_redirect_uri
  ) {
    return true;
  }
  try {
    const res = await fetch('/api/oidc-config', { cache: 'no-store' });
    if (!res.ok) return false;
    const j = (await res.json()) as {
      authority?: string;
      clientId?: string;
      redirectUri?: string;
      postLogoutRedirectUri?: string;
      scope?: string;
    };
    _runtimeEnv = {
      authority: (j.authority || '').trim(),
      client_id: (j.clientId || '').trim(),
      redirect_uri: (j.redirectUri || '').trim(),
      post_logout_redirect_uri: (j.postLogoutRedirectUri || '').trim(),
      scope: (j.scope || 'openid profile email').trim(),
    };
    _manager = null;
    return isOidcConfigured();
  } catch {
    return false;
  }
}

/** True when all required public OIDC env vars are set (inlined at build time in Next.js). */
export function isOidcConfigured(): boolean {
  const e = readOidcEnv();
  return Boolean(e.authority && e.client_id && e.redirect_uri && e.post_logout_redirect_uri);
}

export function getUserManager(): UserManager | null {
  // Only construct in the browser
  if (typeof window === 'undefined') return null;

  if (!isOidcConfigured()) return null;

  if (_manager) return _manager;

  Log.setLogger(console);
  Log.setLevel(Log.INFO);

  const { authority, client_id, redirect_uri, post_logout_redirect_uri, scope } = readOidcEnv();

  _manager = new UserManager({
    authority,
    client_id,
    redirect_uri,
    post_logout_redirect_uri,
    response_type: 'code',
    scope,
    // Construct storage only when window exists
    userStore: new WebStorageStateStore({ store: window.sessionStorage }),
  });

  return _manager;
}

/**
 * Clear the current user session
 * Removes the user from storage and clears the session
 */
export async function clearUserSession(): Promise<void> {
  const manager = getUserManager();
  if (manager) {
    try {
      await manager.removeUser();
    } catch (error) {
      console.error('Error clearing user session:', error);
    }
  }
}

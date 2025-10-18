// No 'use client' here is fine; we guard access to window.
// If you prefer, you can add 'use client' at the top to force client-only.

import { UserManager, WebStorageStateStore, Log } from 'oidc-client-ts';

let _manager: UserManager | null = null;

export function getUserManager(): UserManager | null {
  // Only construct in the browser
  if (typeof window === 'undefined') return null;

  if (_manager) return _manager;

  Log.setLogger(console);
  Log.setLevel(Log.INFO);

  const authority = process.env.NEXT_PUBLIC_OIDC_AUTHORITY!;
  const client_id = process.env.NEXT_PUBLIC_OIDC_CLIENT_ID!;
  const redirect_uri = process.env.NEXT_PUBLIC_OIDC_REDIRECT_URI!;
  const post_logout_redirect_uri = process.env.NEXT_PUBLIC_OIDC_POST_LOGOUT_REDIRECT_URI!;
  const scope = process.env.NEXT_PUBLIC_OIDC_SCOPE || 'openid profile email';

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

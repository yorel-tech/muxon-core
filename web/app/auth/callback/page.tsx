'use client';
import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { fetchOidcConfigIfNeeded, getUserManager } from '@lib/oidc';
import { apiGet } from '@lib/api';

interface BootstrapStatusDto {
  systemStatus: 'NOTREADY' | 'BOOTSTRAPPED' | 'READY';
}

export default function Callback() {
  const router = useRouter();

  useEffect(() => {
    let cancelled = false;
    (async () => {
      await fetchOidcConfigIfNeeded();
      if (cancelled) return;
      const um = getUserManager();
      if (!um) return;
      um.signinRedirectCallback()
        .then(async () => {
          if (cancelled) return;
          const loginType = sessionStorage.getItem('loginType') || 'system';
          sessionStorage.removeItem('loginType');

          if (loginType === 'tenant') {
            router.replace('/tenant/dashboard');
          } else {
            try {
              const status: BootstrapStatusDto = await apiGet('/api/v1/status');
              if (status.systemStatus === 'READY') {
                router.replace('/system/dashboard');
              } else {
                router.replace('/system');
              }
            } catch (error) {
              console.error('Error fetching bootstrap status:', error);
              router.replace('/system');
            }
          }
        })
        .catch((e) => {
          console.error('OIDC callback failed', e);
          router.replace('/');
        });
    })();
    return () => {
      cancelled = true;
    };
  }, [router]);

  return <main className="p-8">Signing you in…</main>;
}

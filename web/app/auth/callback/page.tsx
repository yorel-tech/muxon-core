'use client';
import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { getUserManager } from '@lib/oidc';
import { apiGet } from '@lib/api';

interface BootstrapStatusDto {
  systemStatus: 'NOTREADY' | 'BOOTSTRAPPED' | 'READY';
}

export default function Callback() {
  const router = useRouter();

  useEffect(() => {
    const um = getUserManager();
    if (!um) return; // Should only render on client, but guard anyway
    um.signinRedirectCallback()
      .then(async () => {
        // Get the login type from session storage
        const loginType = sessionStorage.getItem('loginType') || 'system';
        // Clear the stored login type
        sessionStorage.removeItem('loginType');
        
        // Redirect based on login type
        if (loginType === 'tenant') {
          router.replace('/tenant/dashboard');
        } else {
          // Check bootstrap status for system login
          try {
            const status: BootstrapStatusDto = await apiGet('/api/v1/status');
            // If bootstrap status is READY, redirect directly to dashboard
            if (status.systemStatus === 'READY') {
              router.replace('/system/dashboard');
            } else {
              router.replace('/system');
            }
          } catch (error) {
            console.error('Error fetching bootstrap status:', error);
            // On error, default to system page
            router.replace('/system');
          }
        }
      })
      .catch((e) => {
        console.error('OIDC callback failed', e);
        router.replace('/');
      });
  }, [router]);

  return <main className="p-8">Signing you in…</main>;
}

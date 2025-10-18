'use client';
import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { getUserManager } from '@lib/oidc';

export default function Callback() {
  const router = useRouter();

  useEffect(() => {
    const um = getUserManager();
    if (!um) return; // Should only render on client, but guard anyway
    um.signinRedirectCallback()
      .then(() => router.replace('/projects'))
      .catch((e) => {
        console.error('OIDC callback failed', e);
        router.replace('/');
      });
  }, [router]);

  return <main className="p-8">Signing you in…</main>;
}

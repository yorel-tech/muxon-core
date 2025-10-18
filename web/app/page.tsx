'use client';
import { useEffect, useState } from 'react';
import { getUserManager } from '@lib/oidc';

export default function Home() {
  const [name, setName] = useState<string | null>(null);

  useEffect(() => {
    const um = getUserManager();
    if (!um) return; // SSR or not in browser yet
    um.getUser().then(u => {
      setName(u?.profile?.email || u?.profile?.preferred_username || null);
    });
  }, []);

  const onLogin = () => getUserManager()?.signinRedirect();
  const onLogout = () => getUserManager()?.signoutRedirect();

  return (
    <main className="max-w-3xl mx-auto py-20">
      <h1 className="text-4xl font-bold">Infron Core</h1>
      <p className="mt-4 text-lg">Private cloud control, from first principles.</p>

      <div className="mt-8 space-x-4">
        {!name ? (
          <button className="px-4 py-2 rounded bg-black text-white" onClick={onLogin}>
            Login
          </button>
        ) : (
          <>
            <span className="mr-4">Hello, {name}</span>
            <button className="px-4 py-2 rounded border" onClick={onLogout}>
              Logout
            </button>
          </>
        )}
        <a className="underline ml-4" href="/projects">Projects</a>
      </div>
    </main>
  );
}

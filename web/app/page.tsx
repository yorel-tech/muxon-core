'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { fetchOidcConfigIfNeeded, getUserManager, OIDC_NOT_CONFIGURED_MESSAGE } from '@lib/oidc';

type LoginType = 'tenant' | 'system';

export default function Home() {
  const router = useRouter();
  const [name, setName] = useState<string | null>(null);
  const [mounted, setMounted] = useState(false);
  const [loginType, setLoginType] = useState<LoginType>('tenant');
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [oidcError, setOidcError] = useState<string | null>(null);

  useEffect(() => {
    setMounted(true);
    (async () => {
      await fetchOidcConfigIfNeeded();
      const um = getUserManager();
      if (!um) return;
      const u = await um.getUser();
      setName(u?.profile?.email || u?.profile?.preferred_username || null);
    })();
  }, []);

  const onLogin = async (type: LoginType) => {
    const ok = await fetchOidcConfigIfNeeded();
    if (!ok) {
      setOidcError(OIDC_NOT_CONFIGURED_MESSAGE);
      return;
    }
    const um = getUserManager();
    if (!um) {
      setOidcError(OIDC_NOT_CONFIGURED_MESSAGE);
      return;
    }
    setOidcError(null);
    sessionStorage.setItem('loginType', type);
    setShowLoginModal(false);
    um.signinRedirect();
  };

  const onLogout = async () => {
    await fetchOidcConfigIfNeeded();
    await getUserManager()?.signoutRedirect();
  };

  if (!mounted) {
    return null;
  }

  return (
    <div className="min-h-screen relative overflow-hidden">
      {/* Animated gradient background */}
      <div className="absolute inset-0" style={{ background: 'linear-gradient(to bottom right, #0f172a, #581c87, #0f172a)' }}>
        {/* Animated overlay pattern */}
        <div className="absolute inset-0 opacity-30">
          <div className="absolute inset-0" style={{
            backgroundImage: `radial-gradient(circle at 25% 25%, rgba(139, 92, 246, 0.3) 0%, transparent 50%),
                              radial-gradient(circle at 75% 75%, rgba(59, 130, 246, 0.3) 0%, transparent 50%)`,
            animation: 'pulse-glow 8s ease-in-out infinite',
          }}></div>
        </div>

        {/* Grid pattern overlay */}
        <div className="absolute inset-0 opacity-10" style={{
          backgroundImage: `linear-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px),
                            linear-gradient(90deg, rgba(255, 255, 255, 0.05) 1px, transparent 1px)`,
          backgroundSize: '50px 50px',
        }}></div>
      </div>

      {/* Main content */}
      <div className="relative z-10">
        {/* Navigation */}
        <nav className="flex items-center justify-between px-8 py-6">
          <div className="flex items-center space-x-2">
            <div className="w-10 h-10 rounded-lg flex items-center justify-center" style={{ background: 'linear-gradient(to bottom right, #8b5cf6, #3b82f6)' }}>
              <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
              </svg>
            </div>
            <span className="text-xl font-bold text-white">Infron</span>
          </div>
          <div className="flex items-center space-x-4">
            {name ? (
              <>
                <span className="text-gray-300">{name}</span>
                <button
                  onClick={onLogout}
                  className="px-4 py-2 rounded-lg bg-white/10 hover:bg-white/20 text-white transition-colors"
                >
                  Logout
                </button>
              </>
            ) : (
              <button
                onClick={() => {
                  setOidcError(null);
                  setShowLoginModal(true);
                }}
                className="px-4 py-2 rounded-lg bg-white/10 hover:bg-white/20 text-white transition-colors"
              >
                Login
              </button>
            )}
          </div>
        </nav>

        {/* Hero section */}
        <main className="flex flex-col items-center justify-center px-8 py-20">
          <div className="max-w-4xl text-center">
            <h1 className="text-5xl md:text-6xl font-bold text-white mb-6">
              Infrastructure Management Platform
            </h1>
            <p className="text-xl text-gray-300 mb-8">
              Deploy, manage, and scale your infrastructure with ease
            </p>
            {name ? (
              <div className="flex justify-center space-x-4">
                <a
                  href="/system"
                  className="px-6 py-3 rounded-lg bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 text-white font-semibold transition-all"
                >
                  System Dashboard
                </a>
              </div>
            ) : (
              <button
                onClick={() => {
                  setOidcError(null);
                  setShowLoginModal(true);
                }}
                className="px-8 py-4 rounded-lg bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 text-white font-semibold text-lg transition-all"
              >
                Get Started
              </button>
            )}
          </div>
        </main>
      </div>

      {/* Login modal */}
      {showLoginModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50">
          <div className="bg-white rounded-xl shadow-2xl max-w-md w-full p-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-6">Login to Infron</h2>
            {oidcError && (
              <div className="mb-4 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-950">
                {oidcError}
              </div>
            )}
            <div className="space-y-4">
              <button
                onClick={() => {
                  setShowLoginModal(false);
                  router.push('/tenant/login');
                }}
                className="w-full px-6 py-4 rounded-lg border-2 border-gray-200 hover:border-purple-500 hover:bg-purple-50 transition-all text-left"
              >
                <div className="font-semibold text-gray-900">Tenant Login</div>
                <div className="text-sm text-gray-500">Access your tenant dashboard</div>
              </button>
              <button
                onClick={() => onLogin('system')}
                className="w-full px-6 py-4 rounded-lg border-2 border-gray-200 hover:border-blue-500 hover:bg-blue-50 transition-all text-left"
              >
                <div className="font-semibold text-gray-900">System Login</div>
                <div className="text-sm text-gray-500">Access system administration</div>
              </button>
            </div>
            <button
              onClick={() => setShowLoginModal(false)}
              className="mt-6 w-full px-4 py-2 text-gray-500 hover:text-gray-700 transition-colors"
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      <style jsx>{`
        @keyframes pulse-glow {
          0%, 100% {
            opacity: 0.3;
          }
          50% {
            opacity: 0.6;
          }
        }
      `}</style>
    </div>
  );
}

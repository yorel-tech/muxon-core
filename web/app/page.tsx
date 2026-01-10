'use client';
import { useEffect, useState } from 'react';
import { getUserManager } from '@lib/oidc';

type LoginType = 'tenant' | 'system';

export default function Home() {
  const [name, setName] = useState<string | null>(null);
  const [mounted, setMounted] = useState(false);
  const [loginType, setLoginType] = useState<LoginType>('tenant');
  const [showLoginModal, setShowLoginModal] = useState(false);

  useEffect(() => {
    setMounted(true);
    const um = getUserManager();
    if (!um) return; // SSR or not in browser yet
    um.getUser().then(u => {
      setName(u?.profile?.email || u?.profile?.preferred_username || null);
    });
  }, []);

  const onLogin = (type: LoginType) => {
    // Store login type in session storage for use after redirect
    sessionStorage.setItem('loginType', type);
    getUserManager()?.signinRedirect();
  };

  const onLogout = () => getUserManager()?.signoutRedirect();

  if (!mounted) {
    return null;
  }

  return (
    <div className="min-h-screen relative overflow-hidden">
      {/* Animated gradient background */}
      <div className="absolute inset-0 bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900">
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
            <div className="w-10 h-10 bg-gradient-to-br from-violet-500 to-blue-500 rounded-lg flex items-center justify-center">
              <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
              </svg>
            </div>
            <span className="text-xl font-bold text-white">Infron Core</span>
          </div>
          <div className="flex items-center space-x-4">
            {name ? (
              <>
                <span className="text-gray-300">{name}</span>
                <button 
                  className="px-4 py-2 rounded-lg border border-white/20 text-white hover:bg-white/10 transition-all duration-300"
                  onClick={onLogout}
                >
                  Logout
                </button>
              </>
            ) : (
              <button
                className="px-6 py-2 rounded-lg bg-gradient-to-r from-violet-600 to-blue-600 text-white font-medium hover:from-violet-700 hover:to-blue-700 transition-all duration-300 shadow-lg shadow-violet-500/25"
                onClick={() => setShowLoginModal(true)}
              >
                Login
              </button>
            )}
          </div>
        </nav>

        {/* Hero section */}
        <main className="max-w-7xl mx-auto px-8 py-20">
          <div className="text-center">
            <h1 className="text-5xl md:text-7xl font-bold text-white mb-6 leading-tight">
              Private Cloud Control,
              <br />
              <span className="bg-gradient-to-r from-violet-400 to-blue-400 bg-clip-text text-transparent">
                From First Principles
              </span>
            </h1>
            <p className="text-xl md:text-2xl text-gray-300 max-w-3xl mx-auto mb-12">
              Build, deploy, and manage your infrastructure with complete control. 
              Open-source, self-hosted, and designed for modern cloud-native operations.
            </p>
            
            {!name && (
              <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
                <button
                  className="px-8 py-4 rounded-xl bg-gradient-to-r from-violet-600 to-blue-600 text-white font-semibold text-lg hover:from-violet-700 hover:to-blue-700 transition-all duration-300 shadow-xl shadow-violet-500/30 hover:shadow-violet-500/50 transform hover:-translate-y-1"
                  onClick={() => setShowLoginModal(true)}
                >
                  Get Started
                </button>
              </div>
            )}

            {name && (
              <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
                <a
                  href="/system"
                  className="px-8 py-4 rounded-xl bg-gradient-to-r from-violet-600 to-blue-600 text-white font-semibold text-lg hover:from-violet-700 hover:to-blue-700 transition-all duration-300 shadow-xl shadow-violet-500/30 hover:shadow-violet-500/50 transform hover:-translate-y-1"
                >
                  Go to Dashboard
                </a>
              </div>
            )}
          </div>

          {/* Feature cards */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mt-24">
            <div className="group p-8 rounded-2xl bg-white/5 backdrop-blur-sm border border-white/10 hover:bg-white/10 transition-all duration-300 transform hover:-translate-y-2">
              <div className="w-14 h-14 rounded-xl bg-gradient-to-br from-violet-500/20 to-blue-500/20 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform duration-300">
                <svg className="w-7 h-7 text-violet-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                </svg>
              </div>
              <h3 className="text-xl font-semibold text-white mb-3">Secure & Private</h3>
              <p className="text-gray-400">Complete control over your data with enterprise-grade security and self-hosted deployment options.</p>
            </div>

            <div className="group p-8 rounded-2xl bg-white/5 backdrop-blur-sm border border-white/10 hover:bg-white/10 transition-all duration-300 transform hover:-translate-y-2">
              <div className="w-14 h-14 rounded-xl bg-gradient-to-br from-blue-500/20 to-cyan-500/20 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform duration-300">
                <svg className="w-7 h-7 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                </svg>
              </div>
              <h3 className="text-xl font-semibold text-white mb-3">Lightning Fast</h3>
              <p className="text-gray-400">Optimized performance with modern architecture for rapid deployment and scaling of your infrastructure.</p>
            </div>

            <div className="group p-8 rounded-2xl bg-white/5 backdrop-blur-sm border border-white/10 hover:bg-white/10 transition-all duration-300 transform hover:-translate-y-2">
              <div className="w-14 h-14 rounded-xl bg-gradient-to-br from-cyan-500/20 to-emerald-500/20 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform duration-300">
                <svg className="w-7 h-7 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
              </div>
              <h3 className="text-xl font-semibold text-white mb-3">Flexible & Extensible</h3>
              <p className="text-gray-400">Modular architecture that adapts to your needs with support for multiple providers and integrations.</p>
            </div>
          </div>

          {/* Stats section */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8 mt-24 pt-16 border-t border-white/10">
            <div className="text-center">
              <div className="text-4xl font-bold text-white mb-2">100%</div>
              <div className="text-gray-400">Open Source</div>
            </div>
            <div className="text-center">
              <div className="text-4xl font-bold text-white mb-2">Multi</div>
              <div className="text-gray-400">Cloud Support</div>
            </div>
            <div className="text-center">
              <div className="text-4xl font-bold text-white mb-2">RBAC</div>
              <div className="text-gray-400">Enterprise Ready</div>
            </div>
            <div className="text-center">
              <div className="text-4xl font-bold text-white mb-2">API</div>
              <div className="text-gray-400">First Design</div>
            </div>
          </div>
        </main>

        {/* Footer */}
        <footer className="px-8 py-8 border-t border-white/10">
          <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between">
            <p className="text-gray-400 text-sm">© 2024 Infron Core. Built for modern infrastructure.</p>
            <div className="flex items-center space-x-6 mt-4 md:mt-0">
              <a href="#" className="text-gray-400 hover:text-white transition-colors">Documentation</a>
              <a href="#" className="text-gray-400 hover:text-white transition-colors">GitHub</a>
              <a href="#" className="text-gray-400 hover:text-white transition-colors">Support</a>
            </div>
          </div>
        </footer>
      </div>

      <style jsx global>{`
        @keyframes pulse-glow {
          0%, 100% {
            opacity: 0.3;
            transform: scale(1);
          }
          50% {
            opacity: 0.5;
            transform: scale(1.1);
          }
        }
      `}</style>

      {/* Login Type Selection Modal */}
      {showLoginModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
          <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full p-8 animate-in fade-in zoom-in duration-200">
            <h2 className="text-2xl font-bold text-gray-900 mb-2">Select Login Type</h2>
            <p className="text-gray-600 mb-6">Choose how you want to access Infron Core</p>
            
            <div className="space-y-3">
              <button
                onClick={() => onLogin('tenant')}
                className={`w-full p-4 rounded-xl border-2 transition-all duration-200 flex items-start gap-4 ${
                  loginType === 'tenant'
                    ? 'border-violet-500 bg-violet-50'
                    : 'border-gray-200 hover:border-violet-300 hover:bg-gray-50'
                }`}
              >
                <div className="w-12 h-12 rounded-lg bg-gradient-to-br from-violet-500 to-blue-500 flex items-center justify-center flex-shrink-0">
                  <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                  </svg>
                </div>
                <div className="text-left">
                  <h3 className="font-semibold text-gray-900">Tenant Login</h3>
                  <p className="text-sm text-gray-600 mt-1">Access your organization's resources and manage your infrastructure</p>
                </div>
              </button>

              <button
                onClick={() => onLogin('system')}
                className={`w-full p-4 rounded-xl border-2 transition-all duration-200 flex items-start gap-4 ${
                  loginType === 'system'
                    ? 'border-violet-500 bg-violet-50'
                    : 'border-gray-200 hover:border-violet-300 hover:bg-gray-50'
                }`}
              >
                <div className="w-12 h-12 rounded-lg bg-gradient-to-br from-slate-700 to-slate-900 flex items-center justify-center flex-shrink-0">
                  <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.065-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                </div>
                <div className="text-left">
                  <h3 className="font-semibold text-gray-900">System Login</h3>
                  <p className="text-sm text-gray-600 mt-1">Administrator access to manage system-wide settings and configurations</p>
                </div>
              </button>
            </div>

            <button
              onClick={() => setShowLoginModal(false)}
              className="w-full mt-6 px-4 py-3 rounded-lg border border-gray-300 text-gray-700 font-medium hover:bg-gray-50 transition-colors"
            >
              Cancel
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

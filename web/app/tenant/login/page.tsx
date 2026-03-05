'use client';

import { FormEvent, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { getUserManager } from '@lib/oidc';
import { Input } from '@/components/ui/atoms/input';
import { Button } from '@/components/ui/atoms/button';

const TENANT_SLUG_REGEX = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;

export default function TenantLoginPage() {
  const router = useRouter();
  const [tenantName, setTenantName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    // If already authenticated, send the user to the tenant dashboard
    const checkExistingSession = async () => {
      try {
        const um = getUserManager();
        const user = await um?.getUser();
        if (user) {
          router.replace('/tenant/dashboard');
        }
      } catch (e) {
        // Ignore errors here; user can still attempt login
        console.error('Error checking existing session', e);
      }
    };

    checkExistingSession();
  }, [router]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const value = tenantName.trim();

    if (!value) {
      setError('Tenant name is required.');
      return;
    }

    if (!TENANT_SLUG_REGEX.test(value.toLowerCase())) {
      setError('Tenant name must be a valid slug (e.g. acme-corp): lowercase letters, numbers, and hyphens only.');
      return;
    }

    setError(null);
    setIsSubmitting(true);

    try {
      if (typeof window !== 'undefined') {
        sessionStorage.setItem('loginType', 'tenant');
        sessionStorage.setItem('selectedTenantSlug', value.toLowerCase());
      }

      const um = getUserManager();
      await um?.signinRedirect();
    } catch (err) {
      console.error('Failed to start tenant login', err);
      setError('Failed to start login. Please try again.');
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-950 via-slate-900 to-violet-900 px-4">
      <div className="max-w-md w-full bg-white/10 backdrop-blur-xl rounded-2xl border border-white/10 shadow-2xl p-8">
        <div className="flex items-center mb-6">
          <div
            className="h-10 w-10 rounded-xl flex items-center justify-center mr-3"
            style={{ background: 'linear-gradient(to bottom right, #8b5cf6, #3b82f6)' }}
          >
            <span className="text-white font-bold text-lg">I</span>
          </div>
          <div>
            <h1 className="text-xl font-semibold text-white">Infron Tenant Login</h1>
            <p className="text-sm text-slate-300">Enter your tenant name to continue</p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-100 mb-2">
              Tenant name (slug)
            </label>
            <Input
              type="text"
              value={tenantName}
              onChange={(e) => setTenantName(e.target.value)}
              placeholder="acme-corp"
              className="bg-slate-900/60 border-slate-600 text-white placeholder:text-slate-500"
            />
            <p className="mt-1 text-xs text-slate-300">
              Use the slug defined when your tenant was created (e.g. <code>acme-corp</code>).
            </p>
          </div>

          {error && (
            <div className="rounded-lg bg-red-500/10 border border-red-500/60 px-3 py-2 text-sm text-red-100">
              {error}
            </div>
          )}

          <div className="flex flex-col gap-3 pt-2">
            <Button
              type="submit"
              disabled={isSubmitting}
              className="w-full bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 text-white border-0"
            >
              {isSubmitting ? 'Redirecting…' : 'Continue to Login'}
            </Button>
            <Button
              type="button"
              variant="secondary"
              onClick={() => router.push('/')}
              disabled={isSubmitting}
              className="w-full"
            >
              Back to Home
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}


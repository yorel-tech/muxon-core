'use client';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
//import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { useState } from 'react';
import { AuthProvider } from '@lib/auth-context';

export default function Providers({ children }: { children: React.ReactNode }) {
    // ensure a single QueryClient per browser tab
    const [queryClient] = useState(() => new QueryClient({
        defaultOptions: {
            queries: {
                staleTime: 30_000,
                refetchOnWindowFocus: false
            }
        }
    }));

    return (
        <AuthProvider>
            <QueryClientProvider client={queryClient}>
                {children}
                {/* Optional but handy in dev */}
                {/*<ReactQueryDevtools initialIsOpen={false} />*/}
            </QueryClientProvider>
        </AuthProvider>
    );
}


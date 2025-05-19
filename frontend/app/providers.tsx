"use client"

import { SessionProvider } from "next-auth/react"
import { useEffect } from "react"
import { AuthErrorProvider, useAuthError } from "@/lib/context/AuthErrorContext"
import { authErrorHandler } from "@/lib/services/apiClient"
import dynamic from 'next/dynamic'
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import logger from "@/lib/utils/logger"
import { AuthStateWrapper } from "./components/auth/AuthStateWrapper"

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5,
      retry: 2,
    },
  },
})

const ToastProvider = dynamic(
  () => import('@/app/components/ui/toast').then(mod => mod.ToastProvider),
  { ssr: false }
);

/**
 * Connect the AuthErrorProvider to the authErrorHandler singleton
 */
function AuthErrorConnector({ children }: { children: React.ReactNode }) {
  const { handleAuthError } = useAuthError();
  
  useEffect(() => {
    authErrorHandler.setHandler(handleAuthError);
    
    return () => {
      authErrorHandler.setHandler(async () => {
        logger.warn('Auth error handler unregistered');
      });
    };
  }, [handleAuthError]);
  
  return <>{children}</>;
}

export function Providers({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <QueryClientProvider client={queryClient}>
      <SessionProvider>
        <AuthErrorProvider>
          <AuthErrorConnector>
            <AuthStateWrapper>
              <ToastProvider />
              {children}
            </AuthStateWrapper>
          </AuthErrorConnector>
        </AuthErrorProvider>
      </SessionProvider>
    </QueryClientProvider>
  )
} 
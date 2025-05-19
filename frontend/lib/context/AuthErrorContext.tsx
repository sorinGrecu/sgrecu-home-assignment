"use client";

import React, {createContext, useCallback, useContext} from 'react';
import {signOut} from 'next-auth/react';
import {useRouter} from 'next/navigation';
import logger from '@/lib/utils/logger';

interface AuthErrorContextType {
    /**
     * Handle an authentication error by logging out and redirecting to login page
     */
    handleAuthError: (error?: string) => Promise<void>;
}

const AuthErrorContext = createContext<AuthErrorContextType | undefined>(undefined);

/**
 * Provider component that wraps your app and provides auth error handling
 */
export function AuthErrorProvider({children}: { children: React.ReactNode }) {
    const router = useRouter();

    const handleAuthError = useCallback(async (error = 'session_expired') => {
        logger.warn(`Auth error detected: ${error}`);

        try {
            await signOut({redirect: false});

            router.push(`/?error=${error}`);
        } catch (e) {
            logger.error('Error during logout process:', e);
            window.location.href = `/?error=${error}`;
        }
    }, [router]);

    const value = {handleAuthError};

    return (<AuthErrorContext.Provider value={value}>
            {children}
        </AuthErrorContext.Provider>);
}

/**
 * Custom hook to use the auth error context
 */
export function useAuthError() {
    const context = useContext(AuthErrorContext);

    if (context === undefined) {
        throw new Error('useAuthError must be used within an AuthErrorProvider');
    }

    return context;
} 
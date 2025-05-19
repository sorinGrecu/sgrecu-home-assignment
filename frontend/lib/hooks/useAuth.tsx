"use client";

import {signIn, signOut, useSession} from "next-auth/react";
import {useMemo} from "react";

export interface UseAuthResult {
    /**
     * Whether the user is authenticated
     */
    isAuthenticated: boolean;

    /**
     * Whether authentication is in a loading state
     */
    loading: boolean;

    /**
     * The authenticated user data
     */
    user: {
        id: string; name?: string | null; email?: string | null; image?: string | null;
    } | null;

    /**
     * JWT token for backend API calls
     */
    token?: string;

    /**
     * Sign in with Google
     * @param callbackUrl Optional URL to redirect to after successful sign-in
     */
    signIn: (callbackUrl?: string) => Promise<void>;

    /**
     * Sign out
     * @param callbackUrl Optional URL to redirect to after sign-out
     */
    signOut: (callbackUrl?: string) => Promise<void>;
}

/**
 * Hook for authentication state and operations
 */
export function useAuth(): UseAuthResult {
    const {data: session, status} = useSession();
    const loading = status === "loading";
    const isAuthenticated = status === "authenticated";

    return useMemo(() => {
        const login = async (callbackUrl?: string): Promise<void> => {
            await signIn("google", {callbackUrl});
        };

        const logout = async (callbackUrl?: string): Promise<void> => {
            await signOut({callbackUrl});
        };

        return {
            isAuthenticated, loading, user: isAuthenticated && session?.user?.id ? {
                id: session.user.id, name: session.user.name, email: session.user.email, image: session.user.image,
            } : null, token: session?.backendToken, signIn: login, signOut: logout,
        };
    }, [isAuthenticated, loading, session]);
} 
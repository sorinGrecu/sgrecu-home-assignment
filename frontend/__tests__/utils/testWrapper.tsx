import React, {ReactNode} from 'react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';

jest.mock('next-auth/react', () => ({
    useSession: jest.fn(() => ({
        data: {
            expires: '2100-01-01T00:00:00.000Z',
            user: {id: 'test-user', name: 'Test User', email: 'test@example.com'},
            backendToken: 'mock-token',
        }, status: 'authenticated'
    })),
    getSession: jest.fn(() => Promise.resolve({
        expires: '2100-01-01T00:00:00.000Z',
        user: {id: 'test-user', name: 'Test User', email: 'test@example.com'},
        backendToken: 'mock-token',
    })),
    signIn: jest.fn(),
    signOut: jest.fn(),
    SessionProvider: ({children}: { children: ReactNode }) => <>{children}</>,
}));

export const mockSession = {
    expires: '2100-01-01T00:00:00.000Z',
    user: {id: 'test-user', name: 'Test User', email: 'test@example.com'},
    backendToken: 'mock-token',
};

interface WrapperProps {
    children: ReactNode;
    initialSession?: any;
}

/**
 * Create a new QueryClient for each test
 */
export function createQueryClient() {
    return new QueryClient({
        defaultOptions: {
            queries: {
                retry: false,
                retryDelay: 0,
                gcTime: 0,
                staleTime: 0,
                refetchOnWindowFocus: false,
                refetchOnReconnect: false,
                refetchOnMount: false,
                networkMode: 'always',
            }, mutations: {
                retry: false, retryDelay: 0, networkMode: 'always',
            }
        }
    });
}

/**
 * Test wrapper that provides Query Client and Session contexts
 */
export function TestWrapper({children}: WrapperProps) {
    const queryClient = createQueryClient();

    return (<QueryClientProvider client={queryClient}>
        {children}
    </QueryClientProvider>);
}

/**
 * Create a wrapper function for renderHook
 */
export function createTestQueryWrapper(initialSession = mockSession) {
    return ({children}: { children: ReactNode }) => (<TestWrapper initialSession={initialSession}>
        {children}
    </TestWrapper>);
} 
/**
 * INTEGRATION TEST - uses real API clients with mocked responses
 */
process.env.NEXT_PUBLIC_BACKEND_URL = 'http://localhost:8080';
process.env.NEXT_PUBLIC_BASE_URL = 'http://localhost:3000';

import React from 'react';

jest.mock('@/lib/env', () => ({
  clientEnv: {
    NEXT_PUBLIC_BACKEND_URL: 'http://localhost:8080',
    NEXT_PUBLIC_BASE_URL: 'http://localhost:3000',
  },
  env: {
    NEXT_PUBLIC_BACKEND_URL: 'http://localhost:8080',
    NEXT_PUBLIC_BASE_URL: 'http://localhost:3000',
    NODE_ENV: 'test',
  },
}));

jest.mock('@/lib/config', () => ({
  BACKEND_URL: 'http://localhost:8080',
  API_TIMEOUT: 10000,
  API_HEADERS: { 'Content-Type': 'application/json' },
  APP_BASE_URL: 'http://localhost:3000',
  IS_PRODUCTION: false,
}));

jest.mock('next-auth/react', () => ({
  useSession: jest.fn(() => ({
    data: {
      expires: '2100-01-01T00:00:00.000Z',
      user: { id: 'test-user', name: 'Test User', email: 'test@example.com' },
      backendToken: 'mock-token',
    },
    status: 'authenticated'
  })),
  getSession: jest.fn(() => Promise.resolve({
    expires: '2100-01-01T00:00:00.000Z',
    user: { id: 'test-user', name: 'Test User', email: 'test@example.com' },
    backendToken: 'mock-token',
  })),
  signIn: jest.fn(),
  signOut: jest.fn(),
}));

const fakeConvos = [
  { id: 'aaa', title: 'First', updatedAt: new Date().toISOString() },
  { id: 'bbb', title: 'Second', updatedAt: new Date().toISOString() },
];

const fakeMessages = {
  'aaa': [
    { role: 'USER', content: 'Hello', conversationId: 'aaa', createdAt: new Date().toISOString() },
    { role: 'ASSISTANT', content: 'Hi there', conversationId: 'aaa', createdAt: new Date().toISOString() }
  ],
  'empty': []
};

import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useConversations, useConversation, useConversationMessages } from '@/lib/hooks/useConversations';

jest.mock('@/lib/utils/logger', () => ({
  __esModule: true,
  default: {
    debug: jest.fn(),
    info: jest.fn(),
    warn: jest.fn(),
    error: jest.fn(),
  }
}));

const createWrapper = () => {
  const queryClient = new QueryClient({
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
      },
      mutations: {
        retry: false,
        retryDelay: 0,
        networkMode: 'always',
      }
    }
  });
  
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );
};

import { apiClient } from '@/lib/services/apiClient';

describe('Conversation hooks integration', () => {
  let originalGet: typeof apiClient.get;
  
  beforeEach(() => {
    if (!originalGet) {
      originalGet = apiClient.get;
    }
    
    apiClient.get = jest.fn().mockImplementation(async (url) => {
      if (url === '/api/conversations') {
        return fakeConvos;
      }
      
      if (url.match(/\/api\/conversations\/([^\/]+)$/)) {
        const id = url.split('/').pop();
        const convo = fakeConvos.find(c => c.id === id);
        
        if (!convo) {
          const error = new Error('Not found');
          Object.defineProperty(error, 'response', {
            value: { status: 404 },
            configurable: true,
            writable: true
          });
          throw error;
        }
        
        return convo;
      }
      
      if (url.match(/\/api\/conversations\/([^\/]+)\/messages$/)) {
        const id = url.split('/').slice(-2)[0];
        return fakeMessages[id as keyof typeof fakeMessages] || [];
      }
      
      throw new Error(`Unexpected URL: ${url}`);
    });
  });
  
  afterAll(() => {
    if (originalGet) {
      apiClient.get = originalGet;
    }
  });

  describe('useConversations', () => {
    it('fetches and returns all conversations', async () => {
      const { result } = renderHook(() => useConversations(), {
        wrapper: createWrapper(),
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true), { timeout: 2000 });
      
      expect(result.current.data?.length).toBe(2);
      expect(result.current.data?.[0].id).toBe('aaa');
      expect(result.current.data?.[1].id).toBe('bbb');
      expect(apiClient.get).toHaveBeenCalledWith('/api/conversations');
    });
    
    it('handles API errors', async () => {
      const errorGetImplementation = jest.fn().mockImplementation((url) => {
        if (url === '/api/conversations') {
          const error = new Error('Server error');
          Object.defineProperty(error, 'response', {
            value: { status: 500 },
            configurable: true,
            writable: true
          });
          return Promise.reject(error);
        }
        throw new Error('Unexpected URL in test: ' + url);
      });
      
      apiClient.get = errorGetImplementation;
      
      let successCalled = false;
      let errorCalled = false;
      
      const { result } = renderHook(
        () => {
          const query = useConversations();
          
          if (query.isSuccess) successCalled = true;
          if (query.error) errorCalled = true;
          
          return query;
        },
        { wrapper: createWrapper() }
      );
      
      await waitFor(() => expect(errorGetImplementation).toHaveBeenCalled(), { timeout: 3000 });
      
      await waitFor(
        () => expect(result.current.isLoading).toBe(false),
        { timeout: 3000 }
      );
      
      expect(errorGetImplementation).toHaveBeenCalledWith('/api/conversations');
      
      expect(successCalled).toBe(false);
      expect(result.current.data).toBeUndefined();
      expect(result.current.error).toBeDefined();
    });
  });
  
  describe('useConversation', () => {
    it('fetches a single conversation by ID', async () => {
      const { result } = renderHook(() => useConversation('aaa'), {
        wrapper: createWrapper(),
      });
      
      await waitFor(() => expect(result.current.isSuccess).toBe(true), { timeout: 2000 });
      
      expect(result.current.data?.id).toBe('aaa');
      expect(result.current.data?.title).toBe('First');
      expect(apiClient.get).toHaveBeenCalledWith('/api/conversations/aaa');
    });
    
    it('handles 404 for non-existent conversation', async () => {
      let successCalled = false;
      let errorCalled = false;
      
      const { result } = renderHook(
        () => {
          const query = useConversation('non-existent');
          
          if (query.isSuccess) successCalled = true;
          if (query.error) errorCalled = true;
          
          return query;
        },
        { wrapper: createWrapper() }
      );
      
      await waitFor(() => expect(apiClient.get).toHaveBeenCalled(), { timeout: 3000 });
      
      await waitFor(
        () => expect(result.current.isLoading).toBe(false),
        { timeout: 3000 }
      );
      
      expect(apiClient.get).toHaveBeenCalledWith('/api/conversations/non-existent');
      
      expect(successCalled).toBe(false);
      expect(result.current.data).toBeUndefined();
      expect(result.current.error).toBeDefined();
    });
  });
  
  describe('useConversationMessages', () => {
    it('fetches messages for a conversation', async () => {
      const { result } = renderHook(() => useConversationMessages('aaa'), {
        wrapper: createWrapper(),
      });
      
      await waitFor(() => expect(result.current.isSuccess).toBe(true), { timeout: 2000 });
      
      expect(result.current.data?.length).toBe(2);
      expect(result.current.data?.[0].content).toBe('Hello');
      expect(result.current.data?.[1].content).toBe('Hi there');
      expect(apiClient.get).toHaveBeenCalledWith('/api/conversations/aaa/messages');
    });
    
    it('returns empty array for conversation with no messages', async () => {
      const { result } = renderHook(() => useConversationMessages('empty'), {
        wrapper: createWrapper(),
      });
      
      await waitFor(() => expect(result.current.isSuccess).toBe(true), { timeout: 2000 });
      
      expect(result.current.data).toEqual([]);
      expect(apiClient.get).toHaveBeenCalledWith('/api/conversations/empty/messages');
    });
  });
}); 
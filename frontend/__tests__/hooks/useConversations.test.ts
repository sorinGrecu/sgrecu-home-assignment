import { mockClientEnv, mockApiConfig } from '../mocks/envMock';
import { createTestQueryWrapper, mockSession } from '../utils/testWrapper';

jest.mock('@/lib/env', () => ({
  clientEnv: mockClientEnv,
  env: mockClientEnv,
}));

jest.mock('@/lib/config', () => mockApiConfig);

jest.mock('@/lib/services/apiClient', () => ({
  apiClient: {
    get: jest.fn(),
    post: jest.fn(),
  }
}));

jest.mock('next-auth/react', () => ({
  useSession: jest.fn(() => ({
    data: mockSession,
    status: 'authenticated',
  })),
  signIn: jest.fn(),
  signOut: jest.fn(),
}));

jest.mock('@/lib/utils/logger', () => ({
  __esModule: true,
  default: {
    debug: jest.fn(),
    info: jest.fn(),
    warn: jest.fn(),
    error: jest.fn(),
  }
}));

import { renderHook, waitFor } from '@testing-library/react';

import * as hooks from '@/lib/hooks/useConversations';
const { apiClient } = require('@/lib/services/apiClient');

const fakeConvos = [
  { id: 'aaa', title: 'First', updatedAt: new Date().toISOString() },
  { id: 'bbb', title: 'Second', updatedAt: new Date().toISOString() },
];

const fakeMessages = [
  { role: 'USER', content: 'Hello', conversationId: 'aaa', createdAt: new Date().toISOString() },
  { role: 'ASSISTANT', content: 'Hi there', conversationId: 'aaa', createdAt: new Date().toISOString() }
];

describe('Conversation hooks', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('useConversations', () => {
    it('fetches and returns all conversations', async () => {
      apiClient.get.mockResolvedValueOnce(fakeConvos);

      const { result } = renderHook(() => hooks.useConversations(), {
        wrapper: createTestQueryWrapper(),
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true), { timeout: 1000 });
      
      expect(result.current.data?.length).toBe(2);
      expect(result.current.data?.[0].id).toBe('aaa');
      expect(result.current.data?.[1].id).toBe('bbb');
      expect(apiClient.get).toHaveBeenCalledWith('/api/conversations');
    });
    
    it('handles API errors', async () => {
      const serverError = new Error('Server error');
      apiClient.get.mockRejectedValue(serverError);
      
      let successCalled = false;
      let errorCalled = false;
      
      const { result } = renderHook(
        () => {
          const query = hooks.useConversations();
          
          if (query.isSuccess) successCalled = true;
          if (query.error) errorCalled = true;
          
          return query;
        },
        { wrapper: createTestQueryWrapper() }
      );
      
      await waitFor(() => expect(apiClient.get).toHaveBeenCalled(), { timeout: 3000 });
      
      await waitFor(
        () => expect(result.current.isLoading).toBe(false),
        { timeout: 3000 }
      );
      
      expect(apiClient.get).toHaveBeenCalledWith('/api/conversations');
      
      expect(successCalled).toBe(false);
      expect(result.current.data).toBeUndefined();
      expect(result.current.error).toBeDefined();
    });
  });
  
  describe('useConversation', () => {
    it('fetches a single conversation by ID', async () => {
      apiClient.get.mockResolvedValueOnce(fakeConvos[0]);
      
      const { result } = renderHook(() => hooks.useConversation('aaa'), {
        wrapper: createTestQueryWrapper(),
      });
      
      await waitFor(() => expect(result.current.isSuccess).toBe(true), { timeout: 1000 });
      
      expect(result.current.data?.id).toBe('aaa');
      expect(result.current.data?.title).toBe('First');
      expect(apiClient.get).toHaveBeenCalledWith('/api/conversations/aaa');
    });
    
    it('handles 404 for non-existent conversation', async () => {
      const notFoundError = new Error('Not found');
      Object.defineProperty(notFoundError, 'response', {
        value: { status: 404 },
        configurable: true,
        writable: true
      });
      apiClient.get.mockRejectedValue(notFoundError);
      
      let successCalled = false;
      let errorCalled = false;
      
      const { result } = renderHook(
        () => {
          const query = hooks.useConversation('non-existent');
          
          if (query.isSuccess) successCalled = true;
          if (query.error) errorCalled = true;
          
          return query;
        },
        { wrapper: createTestQueryWrapper() }
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
      apiClient.get.mockResolvedValueOnce(fakeMessages);
      
      const { result } = renderHook(() => hooks.useConversationMessages('aaa'), {
        wrapper: createTestQueryWrapper(),
      });
      
      await waitFor(() => expect(result.current.isSuccess).toBe(true), { timeout: 1000 });
      
      expect(result.current.data?.length).toBe(2);
      expect(result.current.data?.[0].content).toBe('Hello');
      expect(result.current.data?.[1].content).toBe('Hi there');
      expect(apiClient.get).toHaveBeenCalledWith('/api/conversations/aaa/messages');
    });
    
    it('returns empty array for conversation with no messages', async () => {
      apiClient.get.mockResolvedValueOnce([]);
      
      const { result } = renderHook(() => hooks.useConversationMessages('empty'), {
        wrapper: createTestQueryWrapper(),
      });
      
      await waitFor(() => expect(result.current.isSuccess).toBe(true), { timeout: 1000 });
      
      expect(result.current.data).toEqual([]);
      expect(apiClient.get).toHaveBeenCalledWith('/api/conversations/empty/messages');
    });
  });
}); 
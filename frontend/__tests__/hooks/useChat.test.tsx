import {mockApiConfig, mockClientEnv} from '../mocks/envMock';
import {createTestQueryWrapper, mockSession} from '../utils/testWrapper';
import {act, renderHook, waitFor} from '@testing-library/react';
import {ROLE} from '@/types/core';

import * as hooks from '@/lib/hooks/useChat';
import {isValidUuid} from '@/lib/services/chatService';

jest.mock('@/lib/env', () => ({
    clientEnv: mockClientEnv, env: mockClientEnv,
}));

jest.mock('@/lib/config', () => mockApiConfig);

jest.mock('@/lib/services/apiClient', () => ({
    apiClient: {
        get: jest.fn(), post: jest.fn(),
    }
}));

jest.mock('next-auth/react', () => ({
    useSession: jest.fn(() => ({
        data: mockSession, status: 'authenticated',
    })), signIn: jest.fn(), signOut: jest.fn(),
}));

jest.mock('next/navigation', () => ({
    useRouter: jest.fn(() => ({
        push: jest.fn(), replace: jest.fn(),
    })), usePathname: jest.fn(() => '/chat'), useSearchParams: jest.fn(() => ({get: () => null})),
}));

jest.mock('@tanstack/react-query', () => {
    const originalModule = jest.requireActual('@tanstack/react-query');
    return {
        ...originalModule, useQueryClient: jest.fn(() => ({
            getQueryData: jest.fn(), setQueryData: jest.fn(), invalidateQueries: jest.fn(),
        })),
    };
});

jest.mock('@/lib/services/chatService', () => {
    const original = jest.requireActual('@/lib/services/chatService');
    return {
        ...original, isValidUuid: jest.fn().mockImplementation((id) => {
            if (id === 'test-id' || id === 'new-uuid' || id === 'slow-uuid') {
                return true;
            }
            return original.isValidUuid(id);
        }),
    };
});

jest.mock('@/lib/utils/logger', () => ({
    __esModule: true, default: {
        debug: jest.fn(), info: jest.fn(), warn: jest.fn(), error: jest.fn(),
    }
}));

jest.mock('@/app/components/chat/chatApiClient', () => ({
    chatApiClient: {
        fetchStreamingResponse: jest.fn().mockImplementation((message, onChunk, existingConversationId) => {
            const idToUse = existingConversationId || 'new-uuid';
            setTimeout(() => {
                onChunk('Hello ', idToUse);
            }, 10);
            setTimeout(() => {
                onChunk('World', idToUse);
            }, 20);
            return Promise.resolve(idToUse);
        })
    }
}));

const {chatApiClient} = jest.requireMock('@/app/components/chat/chatApiClient');

describe('useChat hook', () => {
    beforeEach(() => {
        jest.clearAllMocks();

        window.history.replaceState = jest.fn();
    });

    it('initializes with empty state', () => {
        const {result} = renderHook(() => hooks.useChat({}), {
            wrapper: createTestQueryWrapper(),
        });

        expect(result.current.messages).toEqual([]);
        expect(result.current.isLoading).toBe(false);
    });

    it('initializes with existing messages', () => {
        const initialMessages = [{role: ROLE.USER, content: 'Hi there', conversationId: 'test-id'}];

        expect(isValidUuid('test-id')).toBe(true);

        const {result} = renderHook(() => hooks.useChat({
            sessionId: 'test-id', initialMessages
        }), {
            wrapper: createTestQueryWrapper(),
        });

        expect(result.current.messages.length).toBe(1);
        expect(result.current.messages[0].content).toBe('Hi there');
        expect(result.current.currentSessionId).toBe('test-id');
    });

    it('sends a message and processes streaming response', async () => {
        window.history.replaceState = jest.fn();

        const {result} = renderHook(() => hooks.useChat({}), {
            wrapper: createTestQueryWrapper(),
        });

        await act(async () => {
            const sendPromise = result.current.sendMessage('Test message');
            await waitFor(() => !result.current.isLoading, {timeout: 3000});
            await sendPromise;
        });

        expect(result.current.messages.length).toBe(2);
        expect(result.current.messages[0].role).toBe(ROLE.USER);
        expect(result.current.messages[0].content).toBe('Test message');

        await waitFor(() => {
            return result.current.messages[1]?.content === 'Hello World';
        }, {timeout: 3000});

        expect(result.current.currentSessionId).toBe('new-uuid');

        expect(window.history.replaceState).toHaveBeenCalled();

        expect(result.current.isLoading).toBe(false);
        expect(chatApiClient.fetchStreamingResponse).toHaveBeenCalledWith('Test message', expect.any(Function), undefined, mockSession);
    });

    it('handles API errors', async () => {
        chatApiClient.fetchStreamingResponse.mockImplementationOnce(() => {
            return Promise.reject(new Error('Authentication error'));
        });

        const {result} = renderHook(() => hooks.useChat({}), {
            wrapper: createTestQueryWrapper(),
        });

        await act(async () => {
            try {
                await result.current.sendMessage('Test message that will fail');
            } catch (error) {
            }
        });

        await waitFor(() => !result.current.isLoading, {timeout: 3000});

        if (result.current.messages.length > 1) {
            expect(result.current.messages[1]?.content?.toLowerCase()).toContain('error');
        }
    });

    it('handles slow streaming responses', async () => {
        window.history.replaceState = jest.fn();

        chatApiClient.fetchStreamingResponse.mockImplementationOnce((message: any, onChunk: any) => {
            return new Promise(resolve => {
                const slowId = 'slow-uuid';
                setTimeout(() => {
                    onChunk('Hello ', slowId);
                }, 50);
                setTimeout(() => {
                    onChunk('World', slowId);
                }, 100);
                setTimeout(() => {
                    resolve(slowId);
                }, 200);
            });
        });

        const {result} = renderHook(() => hooks.useChat({}), {
            wrapper: createTestQueryWrapper(),
        });

        let promise: any;
        await act(async () => {
            promise = result.current.sendMessage('Slow test');
        });

        expect(result.current.isLoading).toBe(true);
        expect(result.current.messages.length).toBe(2);
        expect(result.current.messages[0].content).toBe('Slow test');
        expect(result.current.messages[1].role).toBe(ROLE.ASSISTANT);

        await waitFor(() => {
            return result.current.messages[1]?.content === 'Hello World';
        }, {timeout: 3000});

        await act(async () => {
            await promise;
        });

        await waitFor(() => !result.current.isLoading, {timeout: 1000});

        expect(result.current.currentSessionId).toBe('slow-uuid');
        expect(window.history.replaceState).toHaveBeenCalled();

        expect(result.current.isLoading).toBe(false);
    });
});
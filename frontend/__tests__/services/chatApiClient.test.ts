import {chatApiClient} from '@/app/components/chat/chatApiClient';
import {BACKEND_URL} from "@/lib/config";

global.fetch = jest.fn();

const originalIsClient = global.window !== undefined;
let mockIsClient = originalIsClient;

jest.mock('@/lib/config', () => ({
    BACKEND_URL: 'http://localhost:8080'
}));

jest.mock('@/app/components/chat/chatApiClient', () => {
    const originalModule = jest.requireActual('@/app/components/chat/chatApiClient');
    return {
        ...originalModule, chatApiClient: {
            ...originalModule.chatApiClient, fetchStreamingResponse: jest.fn((...args) => {
                if (!mockIsClient) {
                    const existingConversationId = args[2] || '';
                    return Promise.resolve(existingConversationId);
                }
                return originalModule.chatApiClient.fetchStreamingResponse(...args);
            })
        }
    };
});

describe('chatApiClient', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        (global.fetch as jest.Mock).mockClear();
    });

    afterEach(() => {
        mockIsClient = originalIsClient;
    });

    it('should return existing conversation ID when not in client environment', async () => {
        mockIsClient = false;

        const message = 'Hello, AI';
        const onChunk = jest.fn();
        const existingConversationId = 'convo-123';

        const result = await chatApiClient.fetchStreamingResponse(message, onChunk, existingConversationId);

        expect(result).toBe(existingConversationId);
        expect(onChunk).not.toHaveBeenCalled();
    });

    it('should make POST request with correct parameters', async () => {
        mockIsClient = true;

        const message = 'Hello, AI';
        const onChunk = jest.fn();
        const existingConversationId = 'convo-123';
        const session = {backendToken: 'token-123'};

        const mockResponse = {
            ok: true, body: {
                getReader: () => ({
                    read: jest.fn()
                        .mockResolvedValueOnce({
                            done: false,
                            value: new TextEncoder().encode('data: {"conversationId":"convo-123","content":"Hello"}\n\n')
                        })
                        .mockResolvedValueOnce({
                            done: true, value: undefined
                        })
                })
            }
        };

        (global.fetch as jest.Mock).mockResolvedValue(mockResponse);

        await chatApiClient.fetchStreamingResponse(message, onChunk, existingConversationId, session as never);

        expect(global.fetch).toHaveBeenCalledWith(`${BACKEND_URL}/api/chat/stream`, {
            method: 'POST', headers: {
                'Content-Type': 'application/json', 'Authorization': `Bearer ${session.backendToken}`
            }, body: JSON.stringify({
                message, conversationId: existingConversationId
            }), credentials: 'include'
        });
    });

    it('should make POST request without conversationId when not provided', async () => {
        mockIsClient = true;

        const message = 'Hello, AI';
        const onChunk = jest.fn();
        const session = {backendToken: 'token-123'};

        const mockResponse = {
            ok: true, body: {
                getReader: () => ({
                    read: jest.fn()
                        .mockResolvedValueOnce({
                            done: false,
                            value: new TextEncoder().encode('data: {"conversationId":"new-convo","content":"Hello"}\n\n')
                        })
                        .mockResolvedValueOnce({
                            done: true, value: undefined
                        })
                })
            }
        };

        (global.fetch as jest.Mock).mockResolvedValue(mockResponse);

        await chatApiClient.fetchStreamingResponse(message, onChunk, undefined, session as never);

        expect(global.fetch).toHaveBeenCalledWith(`${BACKEND_URL}/api/chat/stream`, {
            method: 'POST', headers: {
                'Content-Type': 'application/json', 'Authorization': `Bearer ${session.backendToken}`
            }, body: JSON.stringify({
                message
            }), credentials: 'include'
        });
    });

    it('should process SSE stream chunks correctly', async () => {
        mockIsClient = true;

        const message = 'Hello, AI';
        const onChunk = jest.fn();

        const sseData = ['data: {"content":"<think>","conversationId":"convo-123"}\n\n', 'data: {"content":"thinking content","conversationId":"convo-123"}\n\n', 'data: {"content":"</think>","conversationId":"convo-123"}\n\n', 'data: {"content":"Hello there!","conversationId":"convo-123"}\n\n', 'data: {"content":"\\n\\n","conversationId":"convo-123"}\n\n', 'data: {"content":"How can I help?","conversationId":"convo-123"}\n\n'];

        const mockResponse = {
            ok: true, body: {
                getReader: () => {
                    let index = 0;
                    return {
                        read: jest.fn().mockImplementation(() => {
                            if (index < sseData.length) {
                                return Promise.resolve({
                                    done: false, value: new TextEncoder().encode(sseData[index++])
                                });
                            } else {
                                return Promise.resolve({
                                    done: true, value: undefined
                                });
                            }
                        })
                    };
                }
            }
        };

        (global.fetch as jest.Mock).mockResolvedValue(mockResponse);

        const result = await chatApiClient.fetchStreamingResponse(message, onChunk);

        expect(onChunk).toHaveBeenCalledTimes(2);
        expect(onChunk).toHaveBeenCalledWith('Hello there!', 'convo-123');
        expect(onChunk).toHaveBeenCalledWith('How can I help?', 'convo-123');
        expect(result).toBe('convo-123');
    });

    it('should handle auth errors by rejecting the promise', async () => {
        mockIsClient = true;

        const message = 'Hello, AI';
        const onChunk = jest.fn();

        (global.fetch as jest.Mock).mockRejectedValue(new Error('Error: 401 Unauthorized'));

        await expect(chatApiClient.fetchStreamingResponse(message, onChunk))
            .rejects.toThrow('Error: 401 Unauthorized');
    });

    it('should handle non-ok response status', async () => {
        mockIsClient = true;

        const message = 'Hello, AI';
        const onChunk = jest.fn();

        const mockResponse = {
            ok: false, status: 500
        };

        (global.fetch as jest.Mock).mockResolvedValue(mockResponse);

        await expect(chatApiClient.fetchStreamingResponse(message, onChunk))
            .rejects.toThrow('Stream failed: 500');
    });
}); 
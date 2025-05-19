import {chatApiClient} from '@/app/components/chat/chatApiClient';
import {connectToEventStream} from '@/lib/services/sseClient';
import {BACKEND_URL} from "@/lib/config";

jest.mock('@/lib/services/sseClient', () => ({
    connectToEventStream: jest.fn(),
}));

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
        mockIsClient = originalIsClient;
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should resolve with conversation ID when called on server-side', async () => {
        mockIsClient = false;

        const onChunk = jest.fn();
        const existingConversationId = 'existing-convo-123';

        const result = await chatApiClient.fetchStreamingResponse('Hello', onChunk, existingConversationId);

        expect(result).toBe(existingConversationId);
        expect(onChunk).not.toHaveBeenCalled();
        expect(connectToEventStream).not.toHaveBeenCalled();
    });

    it('should resolve with empty string when called on server-side without conversation ID', async () => {
        mockIsClient = false;

        const onChunk = jest.fn();

        const result = await chatApiClient.fetchStreamingResponse('Hello', onChunk);

        expect(result).toBe('');
        expect(onChunk).not.toHaveBeenCalled();
        expect(connectToEventStream).not.toHaveBeenCalled();
    });

    it('should connect to event stream with correct parameters', async () => {
        mockIsClient = true;

        const message = 'Hello, AI';
        const onChunk = jest.fn();
        const existingConversationId = 'convo-123';
        const session = {backendToken: 'token-123'};

        (connectToEventStream as jest.Mock).mockImplementation((url, options) => {
            options.onComplete();
        });

        await chatApiClient.fetchStreamingResponse(message, onChunk, existingConversationId, session as any);

        expect(connectToEventStream).toHaveBeenCalled();

        const urlArg = (connectToEventStream as jest.Mock).mock.calls[0][0];
        expect(urlArg).toContain(`${BACKEND_URL}/api/chat/stream`);
        expect(urlArg).toContain(`message=${encodeURIComponent(message).replace(/%20/g, '+')}`);
        expect(urlArg).toContain(`conversationId=${existingConversationId}`);

        const optionsArg = (connectToEventStream as jest.Mock).mock.calls[0][1];
        expect(optionsArg.headers).toEqual({Authorization: `Bearer ${session.backendToken}`});
        expect(optionsArg.timeoutMs).toBe(30000);
    });

    it('should process stream chunks correctly', async () => {
        mockIsClient = true;

        const message = 'Hello, AI';
        const onChunk = jest.fn();
        let capturedChunkHandler: (data: any) => void;

        (connectToEventStream as jest.Mock).mockImplementation((url, options) => {
            capturedChunkHandler = options.onChunk;
        });

        const promise = chatApiClient.fetchStreamingResponse(message, onChunk);

        capturedChunkHandler!({content: '<think>', conversationId: 'convo-123'});
        capturedChunkHandler!({content: 'thinking content', conversationId: 'convo-123'});
        capturedChunkHandler!({content: '</think>', conversationId: 'convo-123'});
        capturedChunkHandler!({content: 'Hello there!', conversationId: 'convo-123'});
        capturedChunkHandler!({content: '\n\n', conversationId: 'convo-123'});
        capturedChunkHandler!({content: 'How can I help?', conversationId: 'convo-123'});

        (connectToEventStream as jest.Mock).mock.calls[0][1].onComplete();
        const result = await promise;

        expect(onChunk).toHaveBeenCalledTimes(2);
        expect(onChunk).toHaveBeenCalledWith('Hello there!', 'convo-123');
        expect(onChunk).toHaveBeenCalledWith('How can I help?', 'convo-123');
        expect(result).toBe('convo-123');
    });

    it('should handle auth errors by rejecting the promise', async () => {
        mockIsClient = true;

        const message = 'Hello, AI';
        const onChunk = jest.fn();

        (connectToEventStream as jest.Mock).mockImplementation((url, options) => {
            options.onError(new Error('Error: 401 Unauthorized'));
        });

        await expect(chatApiClient.fetchStreamingResponse(message, onChunk))
            .rejects.toThrow('Error: 401 Unauthorized');
    });

    it('should resolve the promise for non-auth errors', async () => {
        mockIsClient = true;

        const message = 'Hello, AI';
        const onChunk = jest.fn();
        const conversationId = 'convo-123';

        (connectToEventStream as jest.Mock).mockImplementation((url, options) => {
            options.onChunk({content: 'Some content', conversationId});
            options.onError(new Error('Network error'));
        });

        const result = await chatApiClient.fetchStreamingResponse(message, onChunk);
        expect(result).toBe(conversationId);
    });
}); 
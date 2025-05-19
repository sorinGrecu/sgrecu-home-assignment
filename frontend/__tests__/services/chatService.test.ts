import {ROLE} from '@/types/core';
import {mockSession} from '../utils/testWrapper';
import {mockApiConfig, mockClientEnv} from '../mocks/envMock';

jest.mock('@/lib/env', () => ({
    clientEnv: mockClientEnv, env: mockClientEnv,
}));

jest.mock('@/lib/config', () => mockApiConfig);

const {chatService, formatMessage, isValidUuid, createConversationTitle} = require('@/lib/services/chatService');

jest.mock('@/app/components/chat/chatApiClient', () => ({
    chatApiClient: {
        fetchStreamingResponse: jest.fn().mockImplementation((message, onChunk, existingConversationId) => {
            onChunk('Test response', existingConversationId || 'test-uuid');
            return Promise.resolve(existingConversationId || 'test-uuid');
        }),
    },
}));

describe('chatService', () => {
    beforeEach(() => {
        jest.clearAllMocks();

        require('@/app/components/chat/chatApiClient').chatApiClient.fetchStreamingResponse.mockImplementation((_message: any, onChunk: any, existingConversationId: any) => {
            onChunk('Test response', existingConversationId || 'test-uuid');
            return Promise.resolve(existingConversationId || 'test-uuid');
        });
    });

    describe('sendMessage', () => {
        it('sends a message and processes the streaming response', async () => {
            const onChunk = jest.fn();
            const result = await chatService.sendMessage('Test message', onChunk, 'existing-convo-id', mockSession);

            expect(result).toBe('existing-convo-id');

            expect(onChunk).toHaveBeenCalledWith('Test response', 'existing-convo-id');

            expect(require('@/app/components/chat/chatApiClient').chatApiClient.fetchStreamingResponse).toHaveBeenCalledWith('Test message', expect.any(Function), 'existing-convo-id', mockSession);
        });

        it('handles errors properly', async () => {
            require('@/app/components/chat/chatApiClient').chatApiClient.fetchStreamingResponse.mockRejectedValueOnce(new Error('API error'));

            const onChunk = jest.fn();

            await expect(chatService.sendMessage('Error message', onChunk)).rejects.toThrow('API error');

            expect(onChunk).not.toHaveBeenCalled();
        });
    });

    describe('utility functions', () => {
        it('formatMessage creates a message with the correct structure', () => {
            const message = formatMessage(ROLE.USER, 'Hello', 'test-convo-id');

            expect(message.role).toBe(ROLE.USER);
            expect(message.content).toBe('Hello');
            expect(message.conversationId).toBe('test-convo-id');
            expect(message.createdAt).toBeDefined();
        });

        it('isValidUuid validates UUIDs correctly', () => {
            expect(isValidUuid('123e4567-e89b-12d3-a456-426614174000')).toBe(true);

            expect(isValidUuid('not-a-uuid')).toBe(false);
            expect(isValidUuid('')).toBe(false);
            expect(isValidUuid(undefined)).toBe(false);
        });

        it('createConversationTitle creates appropriate titles', () => {
            expect(createConversationTitle('Short message')).toBe('Short message');

            const longMessage = 'This is a very long message that should be truncated for the title';
            expect(createConversationTitle(longMessage)).toMatch(/^This is a very long message th/);
            expect(createConversationTitle(longMessage).endsWith('...')).toBe(true);

            expect(createConversationTitle('')).toBe('New chat');
            expect(createConversationTitle(undefined)).toBe('New chat');
        });
    });
}); 
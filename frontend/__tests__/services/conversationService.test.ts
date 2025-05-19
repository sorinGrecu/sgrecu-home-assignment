import {mockApiConfig, mockClientEnv} from '../mocks/envMock';

interface ApiError extends Error {
    response?: { status: number };
}

jest.mock('@/lib/env', () => ({
    clientEnv: mockClientEnv, env: mockClientEnv,
}));

jest.mock('@/lib/config', () => mockApiConfig);

jest.mock('@/lib/services/apiClient', () => ({
    apiClient: {
        get: jest.fn(), post: jest.fn(),
    }
}));

const {conversationService} = require('@/lib/services/conversationService');
const {apiClient} = require('@/lib/services/apiClient');

const fakeConvos = [{id: 'aaa', title: 'First', updatedAt: new Date().toISOString()}, {
    id: 'bbb',
    title: 'Second',
    updatedAt: new Date().toISOString()
},];

const fakeMessages = [{
    role: 'USER',
    content: 'Hello',
    conversationId: 'aaa',
    createdAt: new Date().toISOString()
}, {role: 'ASSISTANT', content: 'Hi there', conversationId: 'aaa', createdAt: new Date().toISOString()}];

describe('conversationService', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('getUserConversations fetches all conversations', async () => {
        apiClient.get.mockResolvedValueOnce(fakeConvos);

        const conversations = await conversationService.getUserConversations();

        expect(apiClient.get).toHaveBeenCalledWith('/api/conversations');
        expect(conversations).toHaveLength(2);
        expect(conversations[0].id).toBe('aaa');
        expect(conversations[0].title).toBe('First');
        expect(conversations[1].id).toBe('bbb');
        expect(conversations[1].title).toBe('Second');
    });

    it('getConversationById fetches a specific conversation', async () => {
        apiClient.get.mockResolvedValueOnce(fakeConvos[0]);

        const conversation = await conversationService.getConversationById('aaa');

        expect(apiClient.get).toHaveBeenCalledWith('/api/conversations/aaa');
        expect(conversation.id).toBe('aaa');
        expect(conversation.title).toBe('First');
    });

    it('getConversationById handles 404 for non-existent conversation', async () => {
        const error = new Error('Not found') as ApiError;
        error.response = {status: 404};
        apiClient.get.mockRejectedValueOnce(error);

        try {
            await conversationService.getConversationById('non-existent');
            fail('Expected promise to reject');
        } catch (error) {
            expect(error).toBeDefined();
        }

        expect(apiClient.get).toHaveBeenCalledWith('/api/conversations/non-existent');
    });

    it('getConversationMessages fetches messages for a conversation', async () => {
        apiClient.get.mockResolvedValueOnce(fakeMessages);

        const messages = await conversationService.getConversationMessages('aaa');

        expect(apiClient.get).toHaveBeenCalledWith('/api/conversations/aaa/messages');
        expect(messages).toHaveLength(2);
        expect(messages[0].role).toBe('USER');
        expect(messages[0].content).toBe('Hello');
        expect(messages[1].role).toBe('ASSISTANT');
        expect(messages[1].content).toBe('Hi there');
    });

    it('createConversation creates a new conversation', async () => {
        const title = 'Test Conversation';
        const newConversation = {
            id: 'new-id', title, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString()
        };

        apiClient.post.mockResolvedValueOnce(newConversation);

        const result = await conversationService.createConversation(title);

        expect(apiClient.post).toHaveBeenCalledWith('/api/conversations', {title});
        expect(result.id).toBe('new-id');
        expect(result.title).toBe(title);
    });

    it('handles API errors gracefully', async () => {
        const error = new Error('Server error') as ApiError;
        error.response = {status: 500};
        apiClient.get.mockRejectedValueOnce(error);

        try {
            await conversationService.getConversationById('error');
            fail('Expected promise to reject');
        } catch (error) {
            expect(error).toBeDefined();
        }

        expect(apiClient.get).toHaveBeenCalledWith('/api/conversations/error');
    });
}); 
import {apiClient} from './apiClient';
import {Conversation} from '@/types/conversation';
import {Message} from '@/types/core';

/**
 * Service for conversation-related API calls
 */
export const conversationService = {
    /**
     * Fetch all conversations for the current user
     */
    getUserConversations: async (): Promise<Conversation[]> => {
        return apiClient.get<Conversation[]>('/api/conversations');
    },

    /**
     * Fetch a single conversation by ID
     */
    getConversationById: async (id: string): Promise<Conversation> => {
        return apiClient.get<Conversation>(`/api/conversations/${id}`);
    },

    /**
     * Create a new conversation
     */
    createConversation: async (title?: string): Promise<Conversation> => {
        return apiClient.post<Conversation>('/api/conversations', {title});
    },

    /**
     * Fetch messages for a conversation
     */
    getConversationMessages: async (conversationId: string): Promise<Message[]> => {
        return apiClient.get<Message[]>(`/api/conversations/${conversationId}/messages`);
    },
}; 
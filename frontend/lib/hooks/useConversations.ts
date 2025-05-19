"use client";

import {useQuery, UseQueryResult} from '@tanstack/react-query';
import {conversationService} from '@/lib/services/conversationService';
import {Conversation} from '@/types/conversation';
import {Message} from '@/types/core';
import {useSession} from 'next-auth/react';
import logger from '@/lib/utils/logger';
import {queryKeys} from '@/lib/queryKeys';

/**
 * React Query hook for fetching user conversations
 */
export function useConversations(): UseQueryResult<Conversation[], Error> {
    const {status} = useSession();

    return useQuery({
        queryKey: queryKeys.conversations.all, queryFn: async () => {
            try {
                if (status !== 'authenticated') {
                    return [];
                }

                logger.debug('Fetching conversations...');
                const data = await conversationService.getUserConversations();
                logger.debug('Conversations fetched successfully:', data);
                return data;
            } catch (error) {
                logger.error('Error fetching conversations:', error);
                throw error;
            }
        }, enabled: status === 'authenticated', staleTime: 0, retry: 2,
    });
}

/**
 * React Query hook for fetching a specific conversation by ID
 */
export function useConversation(id: string): UseQueryResult<Conversation, Error> {
    const {status} = useSession();

    return useQuery({
        queryKey: queryKeys.conversations.detail(id), queryFn: async () => {
            try {
                logger.debug(`Fetching conversation with ID: ${id}`);
                const data = await conversationService.getConversationById(id);
                logger.debug('Conversation fetched successfully:', data);
                return data;
            } catch (error) {
                logger.error(`Error fetching conversation with ID ${id}:`, error);
                throw error;
            }
        }, staleTime: 1000 * 60 * 5, retry: 2, enabled: status === 'authenticated' && !!id,
    });
}

/**
 * React Query hook for fetching messages of a specific conversation
 */
export function useConversationMessages(conversationId: string): UseQueryResult<Message[], Error> {
    const {status} = useSession();

    return useQuery({
        queryKey: queryKeys.conversations.messages(conversationId), queryFn: async () => {
            try {
                logger.debug(`Fetching messages for conversation ID: ${conversationId}`);
                const data = await conversationService.getConversationMessages(conversationId);
                logger.debug('Conversation messages fetched successfully:', data);
                return data;
            } catch (error) {
                logger.error(`Error fetching messages for conversation ID ${conversationId}:`, error);
                throw error;
            }
        }, staleTime: 1000 * 60 * 2, retry: 2, enabled: status === 'authenticated' && !!conversationId,
    });
} 
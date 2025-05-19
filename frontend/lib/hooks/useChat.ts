"use client";

import {useCallback, useEffect, useRef, useState} from 'react';
import {useSession} from 'next-auth/react';
import {useMutation, useQueryClient} from '@tanstack/react-query';
import {usePathname} from 'next/navigation';
import {Message, Role, ROLE} from '@/types/core';
import logger from '@/lib/utils/logger';
import {queryKeys} from '@/lib/queryKeys';
import {
    chatService,
    createConversation,
    createConversationTitle,
    formatMessage,
    isValidUuid
} from '@/lib/services/chatService';
import {Conversation} from '@/types/conversation';

interface RawMessage {
    role?: string;
    content?: string;
    conversationId?: string;
    timestamp?: string;
    text?: string;
}

interface UseChatProps {
    sessionId?: string;
    initialMessages?: RawMessage[];
    conversationTitle?: string;
}

/**
 * Hook for managing chat conversations
 */
export function useChat({
                            sessionId, initialMessages = [], conversationTitle,
                        }: UseChatProps) {
    const {data: session} = useSession();
    const queryClient = useQueryClient();
    const pathname = usePathname();
    const hasAddedToCache = useRef(false);
    const updateCacheTimeoutRef = useRef<NodeJS.Timeout | null>(null);
    const isMountedRef = useRef(true);

    const [messages, setMessages] = useState<Message[]>(() => Array.isArray(initialMessages) ? initialMessages.map(msg => formatMessage((msg.role as Role) || ROLE.USER, msg.content || (msg.text as string) || '', sessionId)) : []);

    const [isLoading, setIsLoading] = useState(false);
    const [currentSessionId, setCurrentSessionId] = useState<string | undefined>(isValidUuid(sessionId) ? sessionId : undefined);

    useEffect(() => {
        const conversationsData = queryClient.getQueryData<Conversation[]>(queryKeys.conversations.all);
        const currentConversation = conversationsData?.find(conv => conv.id === currentSessionId);

        document.title = conversationTitle ? `Chat: ${conversationTitle}` : currentConversation?.title ? `Chat: ${currentConversation.title}` : 'New Chat';
    }, [conversationTitle, currentSessionId, queryClient]);

    useEffect(() => {
        if (sessionId && !isValidUuid(sessionId)) {
            logger.warn("Invalid UUID format detected in URL");
        }
    }, [sessionId]);

    useEffect(() => {
        isMountedRef.current = true;

        return () => {
            isMountedRef.current = false;
            if (updateCacheTimeoutRef.current) {
                clearTimeout(updateCacheTimeoutRef.current);
                updateCacheTimeoutRef.current = null;
            }
        };
    }, []);

    const addConversationMutation = useMutation({
        mutationFn: async ({sessionId, title}: {
            sessionId: string, title: string
        }) => (createConversation(sessionId, title)), onSuccess: () => {
            queryClient.invalidateQueries({queryKey: queryKeys.conversations.all});
        }
    });

    const addConversationToCache = useCallback((sessionId: string, userMessage?: string) => {
        const firstUserMessage = userMessage || messages.find(msg => msg.role === ROLE.USER)?.content;
        const title = createConversationTitle(firstUserMessage);
        addConversationMutation.mutate({sessionId, title});
    }, [messages, addConversationMutation]);

    const updateSessionId = useCallback((newSessionId: string, userMessage?: string) => {
        if (!isValidUuid(newSessionId)) {
            logger.warn("Received invalid UUID from backend:", newSessionId);
            return;
        }

        if (currentSessionId !== newSessionId) {
            setCurrentSessionId(newSessionId);

            const newUrl = `/chat/${newSessionId}`;
            window.history.replaceState({as: newUrl, url: newUrl}, '', newUrl);

            if (!sessionId && !hasAddedToCache.current) {
                addConversationToCache(newSessionId, userMessage);
                hasAddedToCache.current = true;
            }
        }
    }, [currentSessionId, sessionId, addConversationToCache]);

    const updateConversationCache = useCallback((conversationId: string, content: string, timestamp?: string) => {
        if (!isMountedRef.current) return;

        queryClient.setQueryData(queryKeys.conversations.detail(conversationId), (oldData: Conversation | undefined) => {
            if (!oldData) return oldData;

            return {
                ...oldData, lastMessage: {
                    content, timestamp: timestamp || new Date().toISOString()
                }, updatedAt: new Date().toISOString()
            };
        });
    }, [queryClient]);

    const updateAssistantMessage = useCallback((content: string, conversationId?: string) => {
        if (!isMountedRef.current) return;

        setMessages(prev => {
            const newMessages = [...prev];
            const lastIndex = newMessages.length - 1;

            if (lastIndex >= 0 && newMessages[lastIndex].role === ROLE.ASSISTANT) {
                const updatedMessage = formatMessage(ROLE.ASSISTANT, newMessages[lastIndex].content + content, isValidUuid(conversationId) ? conversationId : currentSessionId);
                newMessages[lastIndex] = updatedMessage;

                const sessionIdToUse = isValidUuid(conversationId) ? conversationId : currentSessionId;
                if (sessionIdToUse) {
                    updateConversationCache(sessionIdToUse, updatedMessage.content, updatedMessage.createdAt);
                }
            }

            return newMessages;
        });
    }, [currentSessionId, updateConversationCache]);

    const sendMessageMutation = useMutation({
        mutationFn: async (input: string) => {
            const sessionConversationId = await chatService.sendMessage(input, (chunk, conversationId) => {
                updateAssistantMessage(chunk, conversationId);
                if (conversationId && isValidUuid(conversationId) && conversationId !== currentSessionId) {
                    updateSessionId(conversationId, input);
                }
            }, currentSessionId, session);

            return {input, sessionConversationId};
        }, onSuccess: ({input, sessionConversationId}) => {
            if (sessionConversationId && isValidUuid(sessionConversationId) && sessionConversationId !== currentSessionId) {
                updateSessionId(sessionConversationId, input);
            }

            if (currentSessionId) {
                queryClient.invalidateQueries({
                    queryKey: queryKeys.conversations.messages(currentSessionId)
                });
                queryClient.invalidateQueries({
                    queryKey: queryKeys.conversations.all
                });

                const lastMessage = messages[messages.length - 1];
                if (lastMessage?.role === ROLE.ASSISTANT) {
                    updateConversationCache(currentSessionId, lastMessage.content, lastMessage.createdAt);
                }
            }
        }, onError: () => {
            if (!isMountedRef.current) return;

            setMessages(prev => {
                const newMessages = [...prev];
                const lastIndex = newMessages.length - 1;

                if (lastIndex >= 0 && newMessages[lastIndex].role === ROLE.ASSISTANT) {
                    newMessages[lastIndex] = {
                        ...newMessages[lastIndex],
                        content: 'Error: Failed to get response. Please check the API endpoint configuration.'
                    };
                }

                return newMessages;
            });

            setIsLoading(false);
        }
    });

    const handleSendMessage = async (input: string) => {
        const userMessage = formatMessage(ROLE.USER, input, currentSessionId);
        const assistantMessage = formatMessage(ROLE.ASSISTANT, '', currentSessionId);

        setMessages(prev => [...prev, userMessage, assistantMessage]);
        setIsLoading(true);

        try {
            await sendMessageMutation.mutateAsync(input);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        if (pathname === '/chat' && !isValidUuid(sessionId)) {
            setCurrentSessionId(undefined);
            setMessages([]);
            hasAddedToCache.current = false;

            document.title = 'New Chat';
        }
    }, [pathname, sessionId]);

    useEffect(() => {
        const handlePopState = () => {
            const pathParts = window.location.pathname.split('/');

            if (pathParts.length >= 2 && pathParts[1] === 'chat') {
                const possibleId = pathParts[2];

                if (!possibleId) {
                    setCurrentSessionId(undefined);
                    setMessages([]);
                    hasAddedToCache.current = false;
                    document.title = 'New Chat';
                } else if (isValidUuid(possibleId) && possibleId !== currentSessionId) {
                    setCurrentSessionId(possibleId);
                }
            }
        };

        window.addEventListener('popstate', handlePopState);
        return () => window.removeEventListener('popstate', handlePopState);
    }, [currentSessionId]);

    return {
        messages, isLoading, currentSessionId, sendMessage: handleSendMessage
    };
} 
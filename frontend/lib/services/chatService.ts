import {Session} from "next-auth";
import {Message, Role} from "@/types/core";
import {chatApiClient} from "@/app/components/chat/chatApiClient";
import {Conversation} from "@/types/conversation";
import {validate as uuidValidate} from "uuid";

/**
 * Validates a UUID string
 */
export function isValidUuid(uuid?: string): boolean {
    return Boolean(uuid && uuidValidate(uuid));
}

/**
 * Creates a conversation title from the first user message
 */
export function createConversationTitle(message?: string): string {
    if (!message?.trim()) return 'New chat';

    const truncated = message.trim().substring(0, 30);
    return truncated + (message.length > 30 ? '...' : '');
}

/**
 * Creates a basic conversation object
 */
export function createConversation(id: string, title: string): Conversation {
    return {
        id, title, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString()
    };
}

/**
 * Formats a message to match expected structure
 */
export function formatMessage(role: Role, content: string, sessionId?: string): Message {
    return {
        role, content, conversationId: sessionId || '', createdAt: new Date().toISOString()
    };
}

type ChunkHandler = (chunk: string, conversationId?: string) => void;

/**
 * Chat service for communication with the chat API
 */
export const chatService = {
    /**
     * Sends a message to the chat API with streaming response
     */
    sendMessage: async (message: string, onChunk: ChunkHandler, conversationId?: string, session?: Session | null): Promise<string> => {
        return chatApiClient.fetchStreamingResponse(message, onChunk, conversationId, session);
    }
}; 
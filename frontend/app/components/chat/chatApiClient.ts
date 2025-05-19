import { BACKEND_URL } from "@/lib/config";
import { Session } from "next-auth";
import { connectToEventStream } from "@/lib/services/sseClient";

const API_BASE_URL = BACKEND_URL;
const API_STREAM_PATH = '/api/chat/stream';
const CONNECTION_TIMEOUT_MS = 30000; 
const isClient = typeof window !== 'undefined';

export interface ChatResponseChunk {
  conversationId: string;
  content?: string;
  meta?: Record<string, unknown>;
}

export interface ChatStreamParams {
  message: string;
  existingConversationId?: string;
}

export type StreamChunkHandler = (chunk: string, conversationId?: string) => void;

/**
 * Chat API client that handles streaming responses
 */
export const chatApiClient = {
  /**
   * Fetches a streaming chat response from the API
   */
  fetchStreamingResponse: async (
    message: string,
    onChunk: StreamChunkHandler,
    existingConversationId?: string,
    session?: Session | null
  ): Promise<string> => {
    if (!isClient) {
      return Promise.resolve(existingConversationId || '');
    }
    
    return new Promise<string>((resolve, reject) => {
      const url = buildStreamUrl({ message, existingConversationId }).toString();
      let conversationId = existingConversationId || '';
      let thinking = false;
      
      const token = session?.backendToken;
      const headers: Record<string, string> = {};
      
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      
      connectToEventStream<ChatResponseChunk>(url, {
        headers,
        onChunk: (data) => {
          if (!data.content) return;
          
          if (data.content === '<think>') {
            thinking = true;
          } else if (data.content === '</think>') {
            thinking = false;
          } else if (!thinking && data.content !== '\n\n' && data.content !== '\n') {
            conversationId = data.conversationId || conversationId;
            onChunk(data.content, conversationId);
          }
        },
        onError: (error) => {
          if (error.message.includes('401') || error.message.includes('403') || 
              error.message.includes('Authentication error')) {
            reject(error);
          } else {
            resolve(conversationId);
          }
        },
        onComplete: () => {
          resolve(conversationId);
        },
        timeoutMs: CONNECTION_TIMEOUT_MS
      });
    });
  }
};

function buildStreamUrl({ message, existingConversationId }: ChatStreamParams): URL {
  const url = new URL(`${API_BASE_URL}${API_STREAM_PATH}`);
  url.searchParams.append('message', message);
  
  if (existingConversationId) {
    url.searchParams.append('conversationId', existingConversationId);
  }
  
  return url;
} 
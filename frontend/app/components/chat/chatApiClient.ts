import {BACKEND_URL} from "@/lib/config";
import {Session} from "next-auth";

// Configuration constants
const API_BASE_URL = BACKEND_URL;
const API_STREAM_PATH = '/api/chat/stream';
const isClient = typeof window !== 'undefined';

// SSE parsing constants
const SSE_DATA_PREFIX = 'data:';
const SSE_FRAME_DELIMITER = '\n\n';
const THINKING_START_MARKER = '<think>';
const THINKING_END_MARKER = '</think>';
const FILTERED_CONTENT = ['\n\n', '\n'] as const;

// HTTP constants
const CONTENT_TYPE_JSON = 'application/json';
const CREDENTIALS_INCLUDE = 'include';

export interface ChatResponseChunk {
  conversationId: string;
  content?: string;
  meta?: Record<string, unknown>;
}

export interface ChatStreamRequest {
  message: string;
  conversationId?: string;
}

export type StreamChunkHandler = (content: string, conversationId: string) => void;

/**
 * Error class for chat API specific errors
 */
class ChatApiError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
    public readonly response?: Response
  ) {
    super(message);
    this.name = 'ChatApiError';
  }
}

/**
 * Build the request headers for the chat API
 */
function buildRequestHeaders(token?: string): Record<string, string> {
  const headers: Record<string, string> = {
    'Content-Type': CONTENT_TYPE_JSON,
  };
  
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  
  return headers;
}

/**
 * Build the request body for the chat stream
 */
function buildRequestBody(message: string, conversationId?: string): ChatStreamRequest {
  const body: ChatStreamRequest = { message };
  
  if (conversationId) {
    body.conversationId = conversationId;
  }
  
  return body;
}

/**
 * Check if content should be filtered out from the stream
 */
function shouldFilterContent(content: string): boolean {
  return (FILTERED_CONTENT as readonly string[]).includes(content) || !content;
}

/**
 * Parse a single SSE data line and extract the JSON payload
 */
function parseSSEDataLine(line: string): ChatResponseChunk | null {
  if (!line.startsWith(SSE_DATA_PREFIX)) {
    return null;
  }
  
  try {
    const jsonData = line.slice(SSE_DATA_PREFIX.length).trim();
    return JSON.parse(jsonData) as ChatResponseChunk;
  } catch (err) {
    console.warn('Invalid JSON chunk', err);
    return null;
  }
}

/**
 * Process SSE frames and extract data lines
 */
function processSSEFrames(
  buffer: string,
  onChunk: StreamChunkHandler,
  state: { conversationId: string; thinking: boolean }
): string {
  let remainingBuffer = buffer;
  let boundary: number;
  
  while ((boundary = remainingBuffer.indexOf(SSE_FRAME_DELIMITER)) !== -1) {
    const frame = remainingBuffer.slice(0, boundary).trim();
    remainingBuffer = remainingBuffer.slice(boundary + SSE_FRAME_DELIMITER.length);
    
    // Process each line in the frame
    for (const line of frame.split('\n')) {
      const payload = parseSSEDataLine(line);
      if (!payload?.content) continue;
      
      // Handle thinking state markers
      if (payload.content === THINKING_START_MARKER) {
        state.thinking = true;
        continue;
      }
      
      if (payload.content === THINKING_END_MARKER) {
        state.thinking = false;
        continue;
      }
      
      // Process content if not in thinking state and not filtered
      if (!state.thinking && !shouldFilterContent(payload.content)) {
        state.conversationId = payload.conversationId || state.conversationId;
        onChunk(payload.content, state.conversationId);
      }
    }
  }
  
  return remainingBuffer;
}

/**
 * Process the SSE stream from the response
 */
async function processSSEStream(
  response: Response,
  onChunk: StreamChunkHandler,
  initialConversationId: string
): Promise<string> {
  if (!response.body) {
    throw new ChatApiError('Response body is null');
  }
  
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  
  const state = {
    conversationId: initialConversationId,
    thinking: false,
  };
  
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      
      buffer += decoder.decode(value, { stream: true });
      buffer = processSSEFrames(buffer, onChunk, state);
    }
    
    return state.conversationId;
  } finally {
    // Ensure reader is always released (if method exists)
    if (typeof reader.releaseLock === 'function') {
      reader.releaseLock();
    }
  }
}

/**
 * Make the HTTP request to the chat stream endpoint
 */
async function makeStreamRequest(message: string, conversationId?: string, session?: Session | null): Promise<Response> {
    const url = `${API_BASE_URL}${API_STREAM_PATH}`;
    const headers = buildRequestHeaders(session?.backendToken);
    const body = buildRequestBody(message, conversationId);

    const response = await fetch(url, {
        method: 'POST', headers, body: JSON.stringify(body), credentials: CREDENTIALS_INCLUDE,
    });

    if (!response.ok) {
        let errorMessage = `Stream failed: ${response.status}`;

        try {
            const errorData = await response.json();
            if (errorData?.message) errorMessage = errorData.message;
        } catch {
        }

        throw new ChatApiError(errorMessage, response.status, response);
    }

    return response;
}

/**
 * Chat API client that handles streaming responses via POST requests
 */
export const chatApiClient = {
  /**
   * Fetches a streaming chat response from the API using POST request
   */
  async fetchStreamingResponse(
    message: string,
    onChunk: StreamChunkHandler,
    existingConversationId?: string,
    session?: Session | null
  ): Promise<string> {
    if (!isClient) {
      return existingConversationId || '';
    }
    
    try {
      const response = await makeStreamRequest(message, existingConversationId, session);
      return await processSSEStream(response, onChunk, existingConversationId || '');
    } catch (error) {
      if (error instanceof ChatApiError) {
        throw error;
      }
      
      throw new ChatApiError(
        error instanceof Error ? error.message : 'Unknown error occurred',
        undefined,
        undefined
      );
    }
  }
}; 
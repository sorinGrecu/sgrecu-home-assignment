import { Message, Role, ROLE } from "@/types/core";

export type { Message, Role };
export { ROLE };

/**
 * Represents a persistent chat conversation session
 */
export interface ChatConversation {
  conversationId: string;
  messages: Message[];
  createdAt: Date;
  lastUpdatedAt: Date;
} 
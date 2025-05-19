/**
 * Core domain type definitions used across the application
 */

export const ROLE = {
  USER: 'USER',
  ASSISTANT: 'ASSISTANT'
} as const;

export type Role = typeof ROLE[keyof typeof ROLE];

/**
 * Base message interface that can be extended by components
 */
export interface Message {
  id?: string;
  conversationId: string;
  role: Role;
  content: string;
  createdAt?: string;
} 
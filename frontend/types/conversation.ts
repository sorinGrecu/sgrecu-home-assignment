/**
 * Conversation type that matches the backend data model
 * 
 * Note: This may need to be adjusted based on the actual backend response
 */
export interface Conversation {
  id: string;
  userId?: string;
  title?: string;
  createdAt?: string;
  updatedAt?: string;
  
  lastMessage?: {
    content?: string;
    text?: string; 
    senderId?: string;
    sender?: string; 
    timestamp?: string;
    date?: string; 
  };
} 
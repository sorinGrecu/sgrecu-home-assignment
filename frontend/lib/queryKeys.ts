export const queryKeys = {
  conversations: {
    all: ['conversations'] as const,
    detail: (id: string) => ['conversation', id] as const,
    messages: (conversationId: string) => ['conversationMessages', conversationId] as const,
  },
  chat: {
    session: ['chat', 'session'] as const,
  },
  auth: {
    user: ['auth', 'user'] as const,
    session: ['auth', 'session'] as const,
  }
}; 
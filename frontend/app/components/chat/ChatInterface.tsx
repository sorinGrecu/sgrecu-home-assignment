"use client"

import { usePathname } from 'next/navigation';
import { useSession } from 'next-auth/react';
import { MessageList } from './MessageList';
import { MessageInput } from './MessageInput';
import { useChat } from '@/lib/hooks/useChat';
import { AuthGuard } from '../auth/AuthGuard';
import { Message } from './chatModels';

export interface ChatInterfaceProps {
  sessionId?: string;
  initialMessages?: Message[];
  conversationTitle?: string;
}

/**
 * Main chat interface component with authentication handling
 */
export default function ChatInterface({ 
  sessionId, 
  initialMessages = [], 
  conversationTitle
}: ChatInterfaceProps) {
  const pathname = usePathname();
  const { status } = useSession();
  const isAuthLoading = status === 'loading';
  const isAuthenticated = status === 'authenticated';
  
  const {
    messages,
    isLoading,
    sendMessage
  } = useChat({
    sessionId,
    initialMessages,
    conversationTitle
  });

  if (!isAuthenticated) {
    return (
      <AuthGuard 
        isLoading={isAuthLoading} 
        redirectPath={pathname} 
      />
    );
  }

  return (
    <div className="flex flex-col min-h-full">
      <MessageList messages={messages} isLoading={isLoading} />
      <div className="fixed bottom-0 left-0 right-0 z-10 pb-8 pt-2 bg-gradient-to-t from-zinc-900 to-transparent">
        <div className="max-w-3xl mx-auto px-4">
          <MessageInput onSubmit={sendMessage} isLoading={isLoading} />
        </div>
      </div>
    </div>
  );
} 
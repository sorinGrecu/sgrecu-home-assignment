import { useEffect, useRef } from 'react';
import { Message } from './chatModels';
import { ChatMessage } from './ChatMessage';
import { cn } from '@/lib/utils';

interface MessageListProps {
  messages: Message[];
  isLoading: boolean;
}

/**
 * Renders a scrollable list of chat messages
 */
export function MessageList({ messages, isLoading }: MessageListProps) {
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  if (messages.length === 0) {
    return (
      <div className="absolute inset-0 flex items-center justify-center">
        <p className="text-zinc-400 text-center px-4 text-lg">Ask a question to start the conversation</p>
      </div>
    );
  }

  return (
    <div 
      ref={containerRef}
      className={cn(
        "flex-1 px-2 md:px-4 py-4 pb-24 overflow-y-auto",
        "scrollbar-on-hover"
      )}
    >
      <div className="space-y-6 pt-16">
        {messages.map((message, index) => (
          <ChatMessage 
            key={index}
            message={message}
            isLoading={isLoading}
            isLastMessage={index === messages.length - 1}
          />
        ))}
        <div ref={messagesEndRef} />
      </div>
    </div>
  );
} 
import { ReactNode } from 'react';

interface ChatContainerProps {
  children: ReactNode;
}

/**
 * Container component for chat interface with consistent styling
 */
export function ChatContainer({ children }: ChatContainerProps) {
  return (
    <div className="flex flex-col min-h-full">
      {children}
    </div>
  );
} 
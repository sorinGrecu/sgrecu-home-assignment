import { useState, useRef, useEffect } from 'react';
import { cn } from '@/lib/utils';
import { AnimatedBorder } from '../ui/AnimatedBorder';

interface MessageInputProps {
  onSubmit: (message: string) => void;
  isLoading: boolean;
}

/**
 * Text input component for sending chat messages
 */
export function MessageInput({ onSubmit, isLoading }: MessageInputProps) {
  const [input, setInput] = useState('');
  const [isFocused, setIsFocused] = useState(false);
  const [isMultiline, setIsMultiline] = useState(false);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (!isLoading && inputRef.current) {
      inputRef.current.focus();
    }
  }, [isLoading]);

  useEffect(() => {
    if (inputRef.current) {
      inputRef.current.style.height = 'auto';
      const newHeight = Math.min(inputRef.current.scrollHeight, 120);
      inputRef.current.style.height = `${newHeight}px`;
      setIsMultiline(newHeight > 40);
    }
  }, [input]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmedInput = input.trim();
    if (!trimmedInput) return;
    
    onSubmit(trimmedInput);
    setInput('');
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }
  };

  return (
    <AnimatedBorder 
      isActive={isFocused}
      variant={isFocused ? 'focus' : 'attention'}
      className={cn(
        "bg-zinc-800",
        isMultiline ? "rounded-xl" : "rounded-full"
      )}
    >
      <form 
        onSubmit={handleSubmit}
        className="flex items-center px-4 py-2"
      >
        <textarea
          ref={inputRef}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          onFocus={() => setIsFocused(true)}
          onBlur={() => setIsFocused(false)}
          placeholder="Ask a question..."
          className="flex-1 bg-transparent border-0 focus:ring-0 text-zinc-100 placeholder:text-zinc-500 resize-none px-3 py-1 max-h-[120px] overflow-y-auto focus:outline-none"
          disabled={isLoading}
          rows={1}
          style={{ overflowY: 'hidden' }}
        />

        <button
          type="submit"
          disabled={isLoading || !input.trim()}
          className="rounded-full bg-indigo-600 p-2 hover:bg-indigo-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          aria-label="Send message"
        >
          <svg 
            className="w-5 h-5 text-white" 
            fill="none" 
            viewBox="0 0 24 24" 
            stroke="currentColor"
          >
            <path 
              strokeLinecap="round" 
              strokeLinejoin="round" 
              strokeWidth={2} 
              d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" 
            />
          </svg>
        </button>
      </form>
    </AnimatedBorder>
  );
} 
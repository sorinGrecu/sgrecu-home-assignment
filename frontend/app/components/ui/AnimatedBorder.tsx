import { cn } from '@/lib/utils';
import React from 'react';

interface AnimatedBorderProps {
  children: React.ReactNode;
  isActive?: boolean;
  className?: string;
  variant?: 'focus' | 'attention' | 'idle';
}

export function AnimatedBorder({
  children,
  isActive = false,
  className = '',
  variant = 'idle'
}: AnimatedBorderProps) {
  const borderClass = isActive
    ? 'gradient-border-focus'
    : variant === 'attention'
    ? 'gradient-border-attention'
    : 'gradient-border-idle';

  return (
    <div
      className={cn(
        'relative transition-all duration-300',
        borderClass,
        className
      )}
    >
      {children}
    </div>
  );
} 
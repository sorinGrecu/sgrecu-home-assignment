import { cn } from '@/lib/utils'
import { FC } from 'react'

interface LoadingSpinnerProps {
  size?: 'sm' | 'md' | 'lg'
  showText?: boolean
  text?: string
  className?: string
}

const SIZE_CLASSES = {
  sm: {
    outer: 'w-8 h-8',
    inner: 'w-4 h-4',
    border: 'border-2'
  },
  md: {
    outer: 'w-16 h-16',
    inner: 'w-8 h-8',
    border: 'border-3'
  },
  lg: {
    outer: 'w-24 h-24',
    inner: 'w-12 h-12',
    border: 'border-4'
  }
};

const OUTER_SPINNER_CLASS = "rounded-full animate-spin will-change-transform border-t-transparent border-r-gray-500 border-b-gray-400 border-l-gray-500";
const INNER_CIRCLE_CLASS = "bg-gradient-to-br from-gray-400 to-gray-500 rounded-full opacity-80 animate-pulse";

/**
 * A reusable loading spinner component with accessibility support
 * and optimized animation performance
 */
export const LoadingSpinner: FC<LoadingSpinnerProps> = ({
  size = 'md',
  showText = false,
  text = 'Loading...',
  className
}) => {
  return (
    <div 
      className={cn("flex flex-col items-center justify-center", className)}
      role="status"
      aria-live="polite"
    >
      <div className="relative">
        <div 
          className={cn(
            OUTER_SPINNER_CLASS,
            SIZE_CLASSES[size].outer,
            SIZE_CLASSES[size].border
          )}
          aria-hidden="true"
        ></div>
        
        <div className="absolute inset-0 flex items-center justify-center">
          <div 
            className={cn(
              INNER_CIRCLE_CLASS,
              SIZE_CLASSES[size].inner
            )}
            aria-hidden="true"
          ></div>
        </div>
      </div>
      
      <span className="sr-only">{text}</span>
      
      {showText && (
        <p className="mt-3 text-sm text-gray-400 font-light tracking-wide">
          {text}
        </p>
      )}
    </div>
  )
} 
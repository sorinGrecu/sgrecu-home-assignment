import { cn } from '@/lib/utils'

interface MaxWidthWrapperProps {
  className?: string
  children: React.ReactNode
}

/**
 * Provides a consistent maximum width container with responsive padding
 */
const MaxWidthWrapper = ({
  className,
  children
}: MaxWidthWrapperProps) => {
  return (
    <div
      className={cn(
        'mx-auto w-full max-w-screen-xl px-2.5 md:px-20',
        className
      )}>
      {children}
    </div>
  )
}

export default MaxWidthWrapper 
import { cn } from "@/lib/utils";

interface SpinnerProps {
  text?: string;
  className?: string;
}

export function Spinner({ text = "Loading...", className }: SpinnerProps) {
  return (
    <div className={cn("flex items-center space-x-2", className)}>
      <div className="h-4 w-4 animate-spin rounded-full border-2 border-t-transparent border-gray-500"></div>
      {text && <span className="text-sm text-gray-500">{text}</span>}
    </div>
  );
} 
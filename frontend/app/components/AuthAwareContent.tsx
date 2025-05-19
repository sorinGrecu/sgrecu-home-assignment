"use client";

import { useAuth } from "@/lib/hooks/useAuth";
import { cn } from "@/lib/utils";
import { useUI } from "@/lib/store";

interface AuthAwareContentProps {
  children: React.ReactNode;
}

/**
 * Applies different layout styles based on authentication status
 * and sidebar collapse state
 */
export function AuthAwareContent({ children }: AuthAwareContentProps) {
  const { isAuthenticated } = useAuth();
  const { isSidebarCollapsed } = useUI();
  
  return (
    <main
      className={cn(
        "transition-all duration-300 ease-in-out",
        isAuthenticated && {
          "lg:pl-64": !isSidebarCollapsed,
          "lg:pl-[70px]": isSidebarCollapsed
        }
      )}
    >
      {children}
    </main>
  );
} 
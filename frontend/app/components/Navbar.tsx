"use client";

import { cn } from "@/lib/utils";
import { useEffect, useState } from "react";
import { AuthButton } from "./auth/AuthButton";
import { Menu, X } from "lucide-react";
import { Button } from "./ui/button";
import { useUI } from "@/lib/store";
import { useAuth } from "@/lib/hooks/useAuth";

/**
 * Main navigation bar with responsive controls for auth and sidebar
 */
export function Navbar() {
  const [isScrolled, setIsScrolled] = useState(false);
  const { isSidebarOpen: isOpen, isSidebarCollapsed: isCollapsed, toggleSidebar, toggleCollapse } = useUI();
  const { isAuthenticated } = useAuth();

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 0);
    };

    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  return (
    <nav
      className={cn(
        "fixed top-0 left-0 right-0 w-full bg-zinc-900/60 backdrop-blur-sm border-b border-zinc-700/60 z-40",
        isScrolled && "shadow-sm"
      )}
    >
      <div className="flex items-center justify-between h-16 px-4">
        <div className="flex items-center gap-2">
          {isAuthenticated && (
            <>
              {/* Desktop sidebar collapse toggle */}
              <Button
                variant="ghost"
                size="icon"
                onClick={toggleCollapse}
                className="text-zinc-400 hover:text-zinc-50 hover:bg-zinc-800/50 hidden lg:flex"
                aria-label={isCollapsed ? "Expand sidebar" : "Collapse sidebar"}
              >
                <Menu className="h-5 w-5" />
              </Button>
              
              {/* Mobile sidebar toggle */}
              <Button
                variant="ghost"
                size="icon"
                onClick={toggleSidebar}
                className="text-zinc-400 hover:text-zinc-50 hover:bg-zinc-800/50 lg:hidden"
                aria-label={isOpen ? "Close sidebar" : "Open sidebar"}
              >
                {isOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
              </Button>
            </>
          )}
          
          {/* Logo */}
          <div className="flex items-center space-x-1 text-lg font-semibold">
            <span className="text-foreground">Home</span>
            <span className="text-foreground/60">Assignment</span>
          </div>
        </div>

        {/* Auth controls */}
        <AuthButton />
      </div>
    </nav>
  );
} 
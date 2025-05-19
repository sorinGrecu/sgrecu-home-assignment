"use client";

import React, { useState } from "react";
import { signIn } from "next-auth/react";
import { toast } from "sonner";
import { AnimatedBorder } from "../ui/AnimatedBorder";
import { LoadingSpinner } from "../ui/LoadingSpinner";
import { cn } from "@/lib/utils";
import logger from "@/lib/utils/logger";

export type ButtonSize = "sm" | "md" | "lg";
export type ButtonVariant = "default" | "attention" | "focus";
export type ButtonLayout = "default" | "compact";

export interface GoogleSignInButtonProps {
  /**
   * Size of the button
   * @default "md"
   */
  size?: ButtonSize;
  
  /**
   * URL to redirect to after successful sign-in
   * @default "/"
   */
  callbackUrl?: string;
  
  /**
   * Visual style variant of the button
   * @default "attention"
   */
  variant?: ButtonVariant;
  
  /**
   * Layout style (default or compact for navbar)
   * @default "default"
   */
  layout?: ButtonLayout;
  
  /**
   * Additional CSS classes to apply to the button
   */
  className?: string;
  
  /**
   * Text to display when loading
   * @default "Processing..."
   */
  loadingText?: string;
  
  /**
   * Text to display on the button
   * @default "Sign in with Google"
   */
  text?: string;
  
  /**
   * Whether to show the Google logo
   * @default true
   */
  showLogo?: boolean;

  /**
   * Whether the button is in loading state
   * @default false
   */
  isLoading?: boolean;
}

export function GoogleSignInButton({
  size = "md",
  callbackUrl = "/",
  variant = "attention",
  layout = "default",
  className = "",
  loadingText = "Processing...",
  text = "Sign in with Google",
  showLogo = true,
  isLoading: externalLoading = false,
}: GoogleSignInButtonProps): React.JSX.Element {
  const [internalLoading, setInternalLoading] = useState<boolean>(false);
  const isLoading = externalLoading || internalLoading;

  const handleLogin = async (): Promise<void> => {
    if (isLoading) return; 
    
    setInternalLoading(true);
    try {
      await signIn("google", { callbackUrl });
    } catch (error) {
      logger.error("Login error:", error);
      toast.error("Failed to sign in. Please try again.");
    } finally {
      setInternalLoading(false);
    }
  };

  const sizeClasses = {
    sm: "py-1.5 px-3 text-xs h-8",
    md: "py-2 px-4 text-sm h-10",
    lg: "py-3 px-6 text-base h-12",
  };
  
  const widthClasses = layout === "compact" 
    ? {
        sm: "w-[120px]",
        md: "w-[160px]",
        lg: "w-[200px]",
      }
    : {
        sm: "w-[140px]",
        md: "w-[180px]",
        lg: "w-[220px]",
      };

  const googleLogo = (
    <svg 
      xmlns="http://www.w3.org/2000/svg" 
      viewBox="0 0 48 48" 
      className={cn(
        "flex-shrink-0",
        size === "sm" ? "w-4 h-4" : size === "md" ? "w-5 h-5" : "w-6 h-6"
      )}
    >
      <path fill="#FFC107" d="M43.611,20.083H42V20H24v8h11.303c-1.649,4.657-6.08,8-11.303,8c-6.627,0-12-5.373-12-12c0-6.627,5.373-12,12-12c3.059,0,5.842,1.154,7.961,3.039l5.657-5.657C34.046,6.053,29.268,4,24,4C12.955,4,4,12.955,4,24c0,11.045,8.955,20,20,20c11.045,0,20-8.955,20-20C44,22.659,43.862,21.35,43.611,20.083z" />
      <path fill="#FF3D00" d="M6.306,14.691l6.571,4.819C14.655,15.108,18.961,12,24,12c3.059,0,5.842,1.154,7.961,3.039l5.657-5.657C34.046,6.053,29.268,4,24,4C16.318,4,9.656,8.337,6.306,14.691z" />
      <path fill="#4CAF50" d="M24,44c5.166,0,9.86-1.977,13.409-5.192l-6.19-5.238C29.211,35.091,26.715,36,24,36c-5.202,0-9.619-3.317-11.283-7.946l-6.522,5.025C9.505,39.556,16.227,44,24,44z" />
      <path fill="#1976D2" d="M43.611,20.083H42V20H24v8h11.303c-0.792,2.237-2.231,4.166-4.087,5.571c0.001-0.001,0.002-0.001,0.003-0.002l6.19,5.238C36.971,39.205,44,34,44,24C44,22.659,43.862,21.35,43.611,20.083z" />
    </svg>
  );
  
  const spinnerSize = size === "sm" ? "sm" : size === "md" ? "sm" : "md";

  return (
    <AnimatedBorder
      variant={variant === "default" ? "idle" : variant}
      className="rounded-md overflow-hidden inline-block"
    >
      <button
        onClick={handleLogin}
        disabled={isLoading}
        className={cn(
          "flex items-center justify-center gap-2 font-medium text-white hover:bg-gray-800/50 transition-colors disabled:opacity-50",
          sizeClasses[size],
          widthClasses[size],
          layout === "compact" && "whitespace-nowrap",
          className
        )}
        aria-label={isLoading ? loadingText : text}
      >
        {isLoading ? (
          <>
            <LoadingSpinner size={spinnerSize} />
            <span className={cn(layout === "compact" && "whitespace-nowrap")}>{loadingText}</span>
          </>
        ) : (
          <>
            {showLogo && googleLogo}
            <span className={cn(layout === "compact" && "whitespace-nowrap")}>{text}</span>
          </>
        )}
      </button>
    </AnimatedBorder>
  );
} 
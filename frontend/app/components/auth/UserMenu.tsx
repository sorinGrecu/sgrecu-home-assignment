"use client";

import { useSession, signIn, signOut } from "next-auth/react";
import { Button } from "../ui/button";
import { Avatar } from "../ui/avatar";
import { Spinner } from "../ui/spinner";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "../ui/dropdown-menu";

/**
 * User menu component that displays authentication state and user options
 */
export function UserMenu({ redirectTo = '/' }: { redirectTo?: string }) {
  const { data: session, status } = useSession();
  const isLoading = status === 'loading';
  const isAuthenticated = status === 'authenticated';
  
  if (isLoading) return <Spinner text="Signing you in…" />;
  
  if (!isAuthenticated) {
    return (
      <Button onClick={() => signIn('google', { callbackUrl: redirectTo })}>
        Sign in with Google
      </Button>
    );
  }

  const userImage = session?.user?.image || '/default-avatar.png';
  const userName = session?.user?.name || 'User';

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Avatar 
          src={userImage} 
          alt={userName} 
          className="cursor-pointer"
          aria-label="User menu" 
        />
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuItem onClick={() => signOut({ callbackUrl: '/' })}>
          Sign out
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
} 
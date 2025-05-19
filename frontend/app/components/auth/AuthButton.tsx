"use client";

import React from "react";
import {useSession} from "next-auth/react";
import {GoogleSignInButton} from "./GoogleSignInButton";
import {UserMenuDropdown} from "./UserMenuDropdown";

/**
 * Authentication button component that conditionally renders either a login button
 * or user profile dropdown based on authentication state
 */
export function AuthButton(): React.JSX.Element {
    const {status} = useSession();
    const isAuthenticated = status === "authenticated";
    const isLoading = status === "loading";

    if (isLoading) {
        return <GoogleSignInButton loadingText="Processing..." isLoading layout="compact"/>;
    }

    return (<div className="flex items-center space-x-2" aria-label="Authentication controls">
        {isAuthenticated ? (<UserMenuDropdown/>) : (<GoogleSignInButton callbackUrl="/" layout="compact"/>)}
    </div>);
} 
"use client"

import {GoogleSignInButton} from './GoogleSignInButton';
import {LoadingSpinner} from '../ui/LoadingSpinner';
import {memo} from 'react';

interface AuthGuardProps {
    isLoading: boolean;
    redirectPath?: string;
}

const AuthLoadingState = () => (<section
    aria-live="polite"
    className="flex flex-col items-center justify-center min-h-[50vh] h-full px-4 py-10 text-center"
>
    <div className="max-w-md space-y-6">
        <LoadingSpinner size="lg" showText text="Checking authentication..."/>
    </div>
</section>);

const AuthSignInState = ({redirectPath = '/'}: { redirectPath: string }) => (<section
    className="flex flex-col items-center justify-center min-h-[50vh] h-full px-4 py-10 text-center"
    aria-labelledby="auth-heading">
    <div className="max-w-md space-y-6">
        <h1 id="auth-heading" className="text-4xl font-bold tracking-tight text-white">
            Welcome to the Chat Application
        </h1>
        <p className="text-lg text-gray-300">
            Please sign in to access the chat features and start having conversations.
        </p>
        <div className="pt-4 flex justify-center">
            <GoogleSignInButton
                size="lg"
                callbackUrl={redirectPath}
            />
        </div>
    </div>
</section>);

/**
 * Component that protects routes by requiring authentication
 * Shows either authentication loading state or login prompt
 */
export const AuthGuard = memo(function AuthGuard({
                                                     isLoading, redirectPath = '/'
                                                 }: AuthGuardProps) {
    return isLoading ? <AuthLoadingState/> : <AuthSignInState redirectPath={redirectPath}/>;
}); 
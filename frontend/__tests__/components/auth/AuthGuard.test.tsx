import {render, screen} from '@testing-library/react';
import {AuthGuard} from '@/app/components/auth/AuthGuard';

jest.mock('@/app/components/ui/LoadingSpinner', () => ({
    LoadingSpinner: ({showText, text}: { showText: boolean, text: string, size: string }) => (
        <div data-testid="loading-spinner">
            {showText && <span>{text}</span>}
        </div>),
}));

jest.mock('@/app/components/auth/GoogleSignInButton', () => ({
    GoogleSignInButton: ({callbackUrl}: { callbackUrl: string, size: string }) => (
        <button data-testid="google-signin-button" data-callback-url={callbackUrl}>
            Sign in with Google
        </button>),
}));

describe('AuthGuard', () => {
    it('shows loading state when isLoading is true', () => {
        render(<AuthGuard isLoading={true}/>);

        const loadingSpinner = screen.getByTestId('loading-spinner');
        expect(loadingSpinner).toBeInTheDocument();
        expect(screen.getByText('Checking authentication...')).toBeInTheDocument();
        expect(screen.queryByTestId('google-signin-button')).not.toBeInTheDocument();
    });

    it('shows sign-in state when isLoading is false', () => {
        render(<AuthGuard isLoading={false}/>);

        expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
        expect(screen.getByText('Welcome to the Chat Application')).toBeInTheDocument();
        expect(screen.getByText('Please sign in to access the chat features and start having conversations.')).toBeInTheDocument();
        expect(screen.getByTestId('google-signin-button')).toBeInTheDocument();
    });

    it('passes the redirectPath to GoogleSignInButton', () => {
        const customRedirectPath = '/dashboard';
        render(<AuthGuard isLoading={false} redirectPath={customRedirectPath}/>);

        const signInButton = screen.getByTestId('google-signin-button');
        expect(signInButton).toHaveAttribute('data-callback-url', customRedirectPath);
    });

    it('uses default redirectPath when none is provided', () => {
        render(<AuthGuard isLoading={false}/>);

        const signInButton = screen.getByTestId('google-signin-button');
        expect(signInButton).toHaveAttribute('data-callback-url', '/');
    });
}); 
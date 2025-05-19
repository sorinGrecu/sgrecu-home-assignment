import {render, screen} from '@testing-library/react';
import {AuthStateWrapper} from '@/app/components/auth/AuthStateWrapper';
import {useSession} from 'next-auth/react';
import {usePathname, useRouter} from 'next/navigation';

jest.mock('next-auth/react', () => ({
    useSession: jest.fn(),
}));

jest.mock('next/navigation', () => ({
    useRouter: jest.fn(), usePathname: jest.fn(),
}));

jest.mock('@/app/components/ui/LoadingSpinner', () => ({
    LoadingSpinner: ({showText, text}: { showText: boolean, text: string, size: string }) => (
        <div data-testid="loading-spinner">
            {showText && <span>{text}</span>}
        </div>),
}));

describe('AuthStateWrapper', () => {
    beforeEach(() => {
        jest.clearAllMocks();

        (useRouter as jest.Mock).mockReturnValue({
            push: jest.fn(),
        });
    });

    it('shows loading spinner when authentication is loading', () => {
        (useSession as jest.Mock).mockReturnValue({
            status: 'loading',
        });

        (usePathname as jest.Mock).mockReturnValue('/');

        render(<AuthStateWrapper>
            <div data-testid="child-content">Protected content</div>
        </AuthStateWrapper>);

        expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();
        expect(screen.getByText('Processing authentication...')).toBeInTheDocument();

        expect(screen.queryByTestId('child-content')).not.toBeInTheDocument();
    });

    it('redirects authenticated users from public routes to /chat', async () => {
        (useSession as jest.Mock).mockReturnValue({
            status: 'authenticated',
        });

        (usePathname as jest.Mock).mockReturnValue('/');

        const mockRouter = {push: jest.fn()};
        (useRouter as jest.Mock).mockReturnValue(mockRouter);

        render(<AuthStateWrapper>
            <div data-testid="child-content">Public content</div>
        </AuthStateWrapper>);

        expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();
        expect(mockRouter.push).toHaveBeenCalledWith('/chat');
    });

    it('redirects unauthenticated users from protected routes to home page', () => {
        (useSession as jest.Mock).mockReturnValue({
            status: 'unauthenticated',
        });

        (usePathname as jest.Mock).mockReturnValue('/chat');

        const mockRouter = {push: jest.fn()};
        (useRouter as jest.Mock).mockReturnValue(mockRouter);

        render(<AuthStateWrapper>
            <div data-testid="child-content">Protected content</div>
        </AuthStateWrapper>);

        expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();
        expect(mockRouter.push).toHaveBeenCalledWith('/');
    });

    it('renders children when user is authenticated and on a protected route', () => {
        (useSession as jest.Mock).mockReturnValue({
            status: 'authenticated',
        });

        (usePathname as jest.Mock).mockReturnValue('/chat');

        render(<AuthStateWrapper>
            <div data-testid="child-content">Protected content</div>
        </AuthStateWrapper>);

        expect(screen.getByTestId('child-content')).toBeInTheDocument();
        expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
    });

    it('renders children when user is unauthenticated and on a public route', () => {
        (useSession as jest.Mock).mockReturnValue({
            status: 'unauthenticated',
        });

        (usePathname as jest.Mock).mockReturnValue('/');

        render(<AuthStateWrapper>
            <div data-testid="child-content">Public content</div>
        </AuthStateWrapper>);

        expect(screen.getByTestId('child-content')).toBeInTheDocument();
        expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
    });

    it('treats API auth routes as public routes', () => {
        (useSession as jest.Mock).mockReturnValue({
            status: 'authenticated',
        });

        (usePathname as jest.Mock).mockReturnValue('/api/auth/callback');

        const mockRouter = {push: jest.fn()};
        (useRouter as jest.Mock).mockReturnValue(mockRouter);

        render(<AuthStateWrapper>
            <div data-testid="child-content">Auth callback</div>
        </AuthStateWrapper>);

        expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();
        expect(mockRouter.push).toHaveBeenCalledWith('/chat');
    });
}); 
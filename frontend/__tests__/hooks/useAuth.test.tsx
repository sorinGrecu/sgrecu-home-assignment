import { renderHook, act } from '@testing-library/react';
import { useAuth } from '@/lib/hooks/useAuth';
import { useSession, signIn, signOut } from 'next-auth/react';

jest.mock('next-auth/react', () => ({
  useSession: jest.fn(),
  signIn: jest.fn(),
  signOut: jest.fn(),
}));

describe('useAuth hook', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('returns loading state when session is loading', () => {
    (useSession as jest.Mock).mockReturnValue({
      status: 'loading',
      data: null,
    });

    const { result } = renderHook(() => useAuth());

    expect(result.current.loading).toBe(true);
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.user).toBeNull();
  });

  it('returns unauthenticated state when no session exists', () => {
    (useSession as jest.Mock).mockReturnValue({
      status: 'unauthenticated',
      data: null,
    });

    const { result } = renderHook(() => useAuth());

    expect(result.current.loading).toBe(false);
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.user).toBeNull();
  });

  it('returns authenticated state with user data when session exists', () => {
    const mockUser = {
      id: 'user-123',
      name: 'Test User',
      email: 'test@example.com',
      image: 'https://example.com/avatar.jpg',
    };

    (useSession as jest.Mock).mockReturnValue({
      status: 'authenticated',
      data: {
        user: mockUser,
        backendToken: 'mock-token-123',
      },
    });

    const { result } = renderHook(() => useAuth());

    expect(result.current.loading).toBe(false);
    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.user).toEqual(mockUser);
    expect(result.current.token).toBe('mock-token-123');
  });

  it('calls signIn with Google provider when login is triggered', async () => {
    (useSession as jest.Mock).mockReturnValue({
      status: 'unauthenticated',
      data: null,
    });

    const { result } = renderHook(() => useAuth());

    await act(async () => {
      await result.current.signIn('/dashboard');
    });

    expect(signIn).toHaveBeenCalledWith('google', { callbackUrl: '/dashboard' });
  });

  it('calls signOut when logout is triggered', async () => {
    (useSession as jest.Mock).mockReturnValue({
      status: 'authenticated',
      data: {
        user: { id: 'user-123' },
      },
    });

    const { result } = renderHook(() => useAuth());

    await act(async () => {
      await result.current.signOut('/login');
    });

    expect(signOut).toHaveBeenCalledWith({ callbackUrl: '/login' });
  });
}); 
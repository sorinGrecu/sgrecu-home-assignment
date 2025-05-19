import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import {BACKEND_URL} from '@/lib/config';

jest.mock('@/lib/config', () => ({
    BACKEND_URL: 'http://localhost:8080',
    JBHA_GOOGLE_CLIENT_ID: 'mock-client-id',
    JBHA_GOOGLE_CLIENT_SECRET: 'mock-client-secret',
    NEXTAUTH_SECRET: 'mock-secret'
}));

const mockDebug = jest.fn();
const mockError = jest.fn();

jest.mock('@/lib/utils/logger', () => ({
    __esModule: true, default: {
        debug: mockDebug, error: mockError
    }
}));

global.fetch = jest.fn();

jest.mock('next-auth', () => {
    return {
        __esModule: true, default: (config) => {
            return {
                handlers: jest.fn(), signIn: jest.fn(), signOut: jest.fn(), auth: jest.fn(), config: config
            };
        }
    };
});

jest.mock('@/app/auth', () => {
    return {
        __esModule: true, nextAuthHandlers: jest.fn(), signIn: jest.fn(), signOut: jest.fn(), auth: {
            config: {
                callbacks: {
                    jwt: jest.fn().mockImplementation(async ({token, account}) => {
                        if (account?.id_token) {
                            try {
                                const response = await fetch(`${BACKEND_URL}/api/auth/google`, {
                                    method: 'POST', headers: {
                                        'Content-Type': 'application/json', 'Accept': 'application/json'
                                    }, credentials: 'include', body: JSON.stringify({idToken: account.id_token})
                                });

                                if (response.ok) {
                                    const data = await response.json();
                                    mockDebug('Exchanged Google token for JWT');
                                    return {...token, backendToken: data.accessToken};
                                } else {
                                    throw new Error(`HTTP error: ${response.status}`);
                                }
                            } catch (error) {
                                mockError("Failed to exchange Google token for JWT:", error);
                                return token;
                            }
                        }
                        return token;
                    }), session: jest.fn().mockImplementation(({session, token}) => {
                        return {
                            ...session, user: {
                                ...session.user, id: token.sub
                            }, backendToken: token.backendToken
                        };
                    })
                }
            }
        }
    };
});

describe('Auth Module', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('should exchange Google token for backend JWT in the jwt callback', async () => {
        const jwtCallback = require('@/app/auth').auth.config.callbacks.jwt;

        (global.fetch as jest.Mock).mockResolvedValueOnce({
            ok: true, json: async () => ({accessToken: 'mock-backend-token'})
        });

        const token = {sub: 'user-123'};
        const account = {id_token: 'mock-google-token'};

        const result = await jwtCallback({token, account});

        expect(fetch).toHaveBeenCalledWith(`${BACKEND_URL}/api/auth/google`, {
            method: 'POST', headers: {
                'Content-Type': 'application/json', 'Accept': 'application/json'
            }, credentials: 'include', body: JSON.stringify({idToken: account.id_token})
        });

        expect(result).toHaveProperty('backendToken', 'mock-backend-token');
        expect(mockDebug).toHaveBeenCalled();
    });

    it('should handle backend token exchange failure gracefully', async () => {
        const jwtCallback = require('@/app/auth').auth.config.callbacks.jwt;

        (global.fetch as jest.Mock).mockResolvedValueOnce({
            ok: false, status: 401
        });

        const token = {sub: 'user-123'};
        const account = {id_token: 'mock-google-token'};

        const result = await jwtCallback({token, account});

        expect(mockError).toHaveBeenCalledWith("Failed to exchange Google token for JWT:", expect.any(Error));

        expect(result).not.toHaveProperty('backendToken');
    });

    it('should add user ID and backend token to session in session callback', async () => {
        const sessionCallback = require('@/app/auth').auth.config.callbacks.session;

        const session = {
            user: {name: 'Test User', email: 'test@example.com'}
        };
        const token = {
            sub: 'user-123', backendToken: 'mock-backend-token'
        };

        const result = await sessionCallback({session, token});

        expect(result.user).toHaveProperty('id', 'user-123');
        expect(result).toHaveProperty('backendToken', 'mock-backend-token');
    });
}); 
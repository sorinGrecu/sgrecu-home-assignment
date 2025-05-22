import NextAuth from "next-auth";
import Google from "next-auth/providers/google";
import {BACKEND_URL, JBHA_GOOGLE_CLIENT_ID, JBHA_GOOGLE_CLIENT_SECRET, NEXTAUTH_SECRET} from "@/lib/config";

export const {handlers: nextAuthHandlers, signIn, signOut, auth: getAuth} = NextAuth({
    providers: [Google({
        clientId: JBHA_GOOGLE_CLIENT_ID, 
        clientSecret: JBHA_GOOGLE_CLIENT_SECRET, 
        authorization: {
            params: {
                prompt: "select_account", 
                use_one_tap: "true",
            },
        },
    })], 
    secret: NEXTAUTH_SECRET,
    cookies: {
        sessionToken: {
            name: `next-auth.session-token`,
            options: {
                httpOnly: true,
                sameSite: 'lax',
                path: '/',
                secure: process.env.NODE_ENV === 'production'
            }
        },
        callbackUrl: {
            name: `next-auth.callback-url`,
            options: {
                sameSite: 'lax',
                path: '/',
                secure: process.env.NODE_ENV === 'production'
            }
        },
        csrfToken: {
            name: `next-auth.csrf-token`,
            options: {
                httpOnly: true,
                sameSite: 'lax',
                path: '/',
                secure: process.env.NODE_ENV === 'production'
            }
        }
    },
    callbacks: {
        async session({session, token}) {
            if (token && session.user && token.sub) {
                (session.user as { id: string }).id = token.sub;
                
                if (token.backendToken) {
                    (session as { backendToken: string }).backendToken = token.backendToken as string;
                }
            }
            
            return session;
        }, 
        async jwt({token, account}) {
            if (account?.id_token) {
                try {
                    const backendToken = await backendAuthExchange(account.id_token);
                    if (backendToken) {
                        token.backendToken = backendToken;
                    }
                } catch (error) {
                    console.error('Failed to exchange Google token for JWT:', error);
                }
            }
            
            return token;
        },
    }, 
    pages: {
        signIn: "/", 
        error: "/",
    },
});

/**
 * Exchanges a Google ID token for a backend JWT token
 */
async function backendAuthExchange(idToken: string): Promise<string | null> {
    const backendUrl = BACKEND_URL;
    const fullUrl = `${backendUrl}/api/auth/google`;

    try {
        const response = await fetch(fullUrl, {
            method: 'POST', 
            headers: {
                'Content-Type': 'application/json', 
                'Accept': 'application/json',
            },
            credentials: 'include', 
            body: JSON.stringify({idToken}),
        });

        if (!response.ok) {
            const errorText = await response.text().catch(() => 'Unable to read response text');
            throw new Error(`Backend responded with status: ${response.status} - ${errorText}`);
        }

        const data = await response.json();
        return data.accessToken || null;
    } catch (error) {
        console.error('Backend auth exchange failed:', error);
        throw error;
    }
}

declare module "next-auth" {
    interface Session {
        backendToken?: string;
    }

    interface User {
        id?: string;
    }
}

declare module "next-auth/jwt" {
}
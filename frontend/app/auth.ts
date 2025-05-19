import NextAuth from "next-auth";
import Google from "next-auth/providers/google";
import logger from "@/lib/utils/logger";
import {BACKEND_URL, JBHA_GOOGLE_CLIENT_ID, JBHA_GOOGLE_CLIENT_SECRET, NEXTAUTH_SECRET} from "@/lib/config";

export const {handlers: nextAuthHandlers, signIn, signOut, auth: getAuth} = NextAuth({
    providers: [Google({
        clientId: JBHA_GOOGLE_CLIENT_ID, clientSecret: JBHA_GOOGLE_CLIENT_SECRET, authorization: {
            params: {
                prompt: "select_account", use_one_tap: "true",
            },
        },
    }),], secret: NEXTAUTH_SECRET, callbacks: {
        async session({session, token}) {
            if (token && session.user && token.sub) {
                (session.user as { id: string }).id = token.sub;
                if (token.backendToken) {
                    (session as { backendToken: string }).backendToken = token.backendToken as string;
                }
            }
            return session;
        }, async jwt({token, account}) {
            if (account?.id_token) {
                try {
                    const backendToken = await backendAuthExchange(account.id_token);

                    if (backendToken) {
                        token.backendToken = backendToken;
                    }
                } catch (error) {
                    logger.error("Failed to exchange Google token for JWT:", error);
                }
            }
            return token;
        },
    }, pages: {
        signIn: "/", error: "/",
    },
});

/**
 * Exchanges a Google ID token for a backend JWT token
 * @param idToken The Google ID token to exchange
 * @returns The backend JWT access token
 */
async function backendAuthExchange(idToken: string): Promise<string | null> {
    const backendUrl = BACKEND_URL;

    logger.debug(`Exchanging Google ID token for backend JWT...`);

    const response = await fetch(`${backendUrl}/api/auth/google`, {
        method: 'POST', headers: {
            'Content-Type': 'application/json', 'Accept': 'application/json'
        }, credentials: 'include', body: JSON.stringify({idToken})
    });

    if (!response.ok) {
        throw new Error(`Backend responded with status: ${response.status}`);
    }

    const data = await response.json();
    return data.accessToken || null;
}

declare module "next-auth" {
    /**
     * Returned by `useSession`, `getSession` and received as a prop on the `SessionProvider` React Context
     */
    interface Session {
        backendToken?: string;
    }

    interface User {
        id?: string;
    }
}

declare module "next-auth/jwt" {
}
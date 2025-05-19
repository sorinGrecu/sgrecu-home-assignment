/**
 * Application configuration with environment variable validation
 */
import {clientEnv, env} from './env';

export const BACKEND_URL = clientEnv.NEXT_PUBLIC_BACKEND_URL;
const isServer = typeof window === 'undefined';

type ServerEnv = Record<string, string>;

export const JBHA_GOOGLE_CLIENT_ID = isServer ? (env as ServerEnv).JBHA_GOOGLE_CLIENT_ID : '';
export const JBHA_GOOGLE_CLIENT_SECRET = isServer ? (env as ServerEnv).JBHA_GOOGLE_CLIENT_SECRET : '';
export const NEXTAUTH_SECRET = isServer ? (env as ServerEnv).NEXTAUTH_SECRET : '';

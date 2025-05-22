/**
 * Application configuration with environment variable validation
 */
import {clientEnv, env} from './env';

const isServer = typeof window === 'undefined';

export const BACKEND_URL = isServer && process.env.BACKEND_INTERNAL_URL 
  ? process.env.BACKEND_INTERNAL_URL 
  : clientEnv.NEXT_PUBLIC_BACKEND_URL;

type ServerEnv = Record<string, string>;

export const JBHA_GOOGLE_CLIENT_ID = isServer ? (env as ServerEnv).JBHA_GOOGLE_CLIENT_ID : '';
export const JBHA_GOOGLE_CLIENT_SECRET = isServer ? (env as ServerEnv).JBHA_GOOGLE_CLIENT_SECRET : '';
export const NEXTAUTH_SECRET = isServer ? (env as ServerEnv).NEXTAUTH_SECRET : '';

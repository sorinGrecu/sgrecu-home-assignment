import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import { getToken, JWT } from 'next-auth/jwt';
import { NEXTAUTH_SECRET } from '@/lib/config';

const PUBLIC_PATHS = [
  '/', 
  '/api/auth',
  '/login',
  '/api/auth/google',
] as const;

const STREAM_ENDPOINTS = [
  '/api/chat/stream'
] as const;

const COOKIE_NAMES = {
  SESSION: 'next-auth.session-token',
  CSRF: 'next-auth.csrf-token',
  CALLBACK: 'next-auth.callback-url',
} as const;

const ERROR_MESSAGES = {
  SESSION_EXPIRED: 'Unauthorized - Session expired',
  AUTH_ERROR: 'Authentication error',
} as const;

const ERROR_PARAMS = {
  SESSION_EXPIRED: 'session_expired',
  AUTH_ERROR: 'auth_error',
} as const;

/**
 * Check if a route is public (doesn't require authentication)
 */
function isPublicRoute(path: string): boolean {
  return PUBLIC_PATHS.some(publicPath => 
    path === publicPath || path.startsWith(`${publicPath}/`) || path.startsWith(`${publicPath}?`)
  );
}

/**
 * Check if a route is a streaming endpoint
 */
function isStreamEndpoint(path: string): boolean {
  return STREAM_ENDPOINTS.some(endpoint => path.startsWith(endpoint));
}

/**
 * Check if a JWT token is expired
 */
function isTokenExpired(token: JWT): boolean {
  if (!token.exp) return false;
  return Date.now() >= token.exp * 1000;
}

/**
 * Create a JSON error response for streaming endpoints
 */
function createJsonErrorResponse(message: string, status: number): NextResponse {
  return new NextResponse(JSON.stringify({ error: message }), {
    status,
    headers: {
      'Content-Type': 'application/json',
    },
  });
}

/**
 * Create a redirect response with cleaned cookies
 */
function createRedirectWithCleanup(request: NextRequest, errorParam: string): NextResponse {
  const response = NextResponse.redirect(new URL(`/?error=${errorParam}`, request.url));
  
  Object.values(COOKIE_NAMES).forEach(cookieName => {
    response.cookies.delete(cookieName);
  });
  
  return response;
}

/**
 * Handle unauthorized access (missing or invalid token)
 */
function handleUnauthorized(request: NextRequest, fullPath: string): NextResponse {
  if (isStreamEndpoint(fullPath)) {
    return createJsonErrorResponse(ERROR_MESSAGES.SESSION_EXPIRED, 401);
  }
  
  return createRedirectWithCleanup(request, ERROR_PARAMS.SESSION_EXPIRED);
}

/**
 * Handle authentication errors (exceptions during token validation)
 */
function handleAuthError(request: NextRequest, fullPath: string): NextResponse {
  if (isStreamEndpoint(fullPath)) {
    return createJsonErrorResponse(ERROR_MESSAGES.AUTH_ERROR, 500);
  }
  
  return createRedirectWithCleanup(request, ERROR_PARAMS.AUTH_ERROR);
}

/**
 * Middleware function that runs before each request
 */
export async function middleware(request: NextRequest): Promise<NextResponse> {
  const path = request.nextUrl.pathname;
  const fullPath = path + request.nextUrl.search;
  
  if (isPublicRoute(fullPath)) {
    return NextResponse.next();
  }
  
  try {
    const token = await getToken({ 
      req: request,
      secret: NEXTAUTH_SECRET,
      secureCookie: process.env.NODE_ENV === 'production',
      cookieName: COOKIE_NAMES.SESSION,
    });
    
    if (!token) {
      return handleUnauthorized(request, fullPath);
    }
    
    if (isTokenExpired(token)) {
      return handleUnauthorized(request, fullPath);
    }
    
    return NextResponse.next();
  } catch (error) {
    console.error('Middleware error:', error);
    return handleAuthError(request, fullPath);
  }
}

export const config = {
  matcher: [
    '/((?!_next/static|_next/image|favicon.ico|.*\\.png$).*)',
  ],
}; 
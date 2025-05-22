import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import { getToken } from 'next-auth/jwt';
import { NEXTAUTH_SECRET } from '@/lib/config';

const publicPaths = [
  '/', 
  '/api/auth',
  '/login',
  '/api/auth/google',
  '/api/chat/stream'
];

const streamEndpoints = [
  '/api/chat/stream'
];

/**
 * Check if a route is public (doesn't require authentication)
 */
function isPublicRoute(path: string): boolean {
  return publicPaths.some(publicPath => 
    path === publicPath || path.startsWith(`${publicPath}/`) || path.startsWith(`${publicPath}?`)
  );
}

/**
 * Check if a route is a stream endpoint
 */
function isStreamEndpoint(path: string): boolean {
  return streamEndpoints.some(streamPath => 
    path === streamPath || path.startsWith(`${streamPath}/`) || path.startsWith(`${streamPath}?`)
  ) || path.startsWith('/stream/');
}

/**
 * Check if token is expired or about to expire
 */
function isTokenExpired(token: any): boolean {
  if (!token?.exp) return false;
  
  const expiryTime = token.exp * 1000;
  const currentTime = Date.now();
  
  return currentTime > expiryTime - 60000;
}

/**
 * Middleware function that runs before each request
 */
export async function middleware(request: NextRequest) {
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
      cookieName: 'next-auth.session-token'
    });
    
    if (!token) {
      if (isStreamEndpoint(fullPath)) {
        return new NextResponse(JSON.stringify({ error: 'Unauthorized - Session expired' }), {
          status: 401,
          headers: {
            'Content-Type': 'application/json',
          },
        });
      }
      
      const response = NextResponse.redirect(new URL('/?error=session_expired', request.url));
      
      response.cookies.delete('next-auth.session-token');
      response.cookies.delete('next-auth.csrf-token');
      response.cookies.delete('next-auth.callback-url');
      
      return response;
    }
    
    if (isTokenExpired(token)) {
      if (isStreamEndpoint(fullPath)) {
        return new NextResponse(JSON.stringify({ error: 'Unauthorized - Session expired' }), {
          status: 401,
          headers: {
            'Content-Type': 'application/json',
          },
        });
      }
      
      const response = NextResponse.redirect(new URL('/?error=session_expired', request.url));
      
      response.cookies.delete('next-auth.session-token');
      response.cookies.delete('next-auth.csrf-token');
      response.cookies.delete('next-auth.callback-url');
      
      return response;
    }
    
    if ((path.startsWith('/api/') || path.startsWith('/stream/')) && token.backendToken) {
      const requestHeaders = new Headers(request.headers);
      requestHeaders.set('Authorization', `Bearer ${token.backendToken}`);
      
      return NextResponse.next({
        request: {
          headers: requestHeaders,
        },
      });
    }
    
    return NextResponse.next();
    
  } catch (error) {
    return NextResponse.redirect(new URL('/?error=auth_error', request.url));
  }
}

export const config = {
  matcher: [
    /*
     * Match all request paths except for:
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico (favicon file)
     * - public folder
     */
    '/((?!_next/static|_next/image|favicon.ico|images/).+)',
  ],
}; 
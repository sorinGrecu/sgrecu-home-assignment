"use client";

import { getAuth } from "@/app/auth";
import logger from "../utils/logger";
import { BACKEND_URL } from "@/lib/config";

/**
 * Interface for token payload structure
 */
export interface TokenPayload {
  sub: string;
  email: string;
  exp: number;
}

/**
 * Auth service for centralized management of authentication-related operations
 */
export class AuthService {
  /**
   * Exchange a Google ID token for a backend JWT
   */
  static async loginWithGoogle(googleToken: string): Promise<{ token: string }> {
    const backendUrl = BACKEND_URL;
    
    logger.debug("Authenticating with Google token");
    
    const response = await fetch(`${backendUrl}/api/auth/google`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      credentials: 'include',
      body: JSON.stringify({ idToken: googleToken })
    });
    
    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Authentication failed: ${errorText}`);
    }
    
    return await response.json();
  }
  
  /**
   * Get the current auth header for API requests
   */
  static async getAuthHeader(): Promise<string> {
    const session = await getAuth();
    return session?.backendToken ? `Bearer ${session.backendToken}` : '';
  }
  
  /**
   * Check if the current user is authenticated
   */
  static async isAuthenticated(): Promise<boolean> {
    const session = await getAuth();
    return !!session?.backendToken;
  }
  
  /**
   * Check if a token is expired
   */
  static async isTokenExpired(token: string): Promise<boolean> {
    try {
      const { jwtDecode } = await import('jwt-decode');
      const payload = jwtDecode<TokenPayload>(token);
      const currentTime = Math.floor(Date.now() / 1000);
      return payload.exp < currentTime;
    } catch (error) {
      logger.error("Error parsing token:", error);
      return true;
    }
  }
  
  /**
   * Get user info from the token
   */
  static async decodeToken(token: string): Promise<TokenPayload | null> {
    try {
      const { jwtDecode } = await import('jwt-decode');
      return jwtDecode<TokenPayload>(token);
    } catch (error) {
      logger.error("Error decoding token:", error);
      return null;
    }
  }
} 
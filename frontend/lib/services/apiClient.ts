"use client";

import axios, {AxiosError, AxiosInstance, AxiosRequestConfig} from "axios";
import {getSession} from "next-auth/react";
import logger from "../utils/logger";
import {BACKEND_URL} from "@/lib/config";

/**
 * Creates a singleton auth error handler for use when not in a React component
 */
class AuthErrorHandler {
    private static instance: AuthErrorHandler;
    private handler: ((error?: string) => Promise<void>) | null = null;

    private constructor() {
    }

    static getInstance(): AuthErrorHandler {
        if (!AuthErrorHandler.instance) {
            AuthErrorHandler.instance = new AuthErrorHandler();
        }
        return AuthErrorHandler.instance;
    }

    setHandler(handler: (error?: string) => Promise<void>): void {
        this.handler = handler;
    }

    async handleError(error?: string): Promise<void> {
        if (this.handler) {
            await this.handler(error);
        } else {
            logger.warn('No auth error handler set, falling back to default behavior');
            document.cookie = "next-auth.session-token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT";
            document.cookie = "next-auth.csrf-token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT";
            document.cookie = "next-auth.callback-url=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT";
            window.location.href = `/?error=${error || 'session_expired'}`;
        }
    }
}

export const authErrorHandler = AuthErrorHandler.getInstance();

/**
 * API client for making authenticated requests to the backend
 */
class ApiClient {
    private client: AxiosInstance;

    constructor() {
        const baseURL = BACKEND_URL;

        this.client = axios.create({
            baseURL, headers: {
                "Content-Type": "application/json",
            },
        });

        this.setupInterceptors();
    }

    /**
     * Make a GET request
     */
    async get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
        try {
            logger.debug(`Making GET request to: ${this.client.defaults.baseURL}${url}`);
            const response = await this.client.get<T>(url, config);
            logger.debug(`GET response from ${url}:`, response.status);
            return response.data;
        } catch (error) {
            logger.error(`Error during GET request to ${url}:`, error);
            throw error;
        }
    }

    /**
     * Make a POST request
     */
    async post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
        const response = await this.client.post<T>(url, data, config);
        return response.data;
    }

    /**
     * Make a PUT request
     */
    async put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
        const response = await this.client.put<T>(url, data, config);
        return response.data;
    }

    /**
     * Make a DELETE request
     */
    async delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
        const response = await this.client.delete<T>(url, config);
        return response.data;
    }

    /**
     * Make a PATCH request
     */
    async patch<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
        const response = await this.client.patch<T>(url, data, config);
        return response.data;
    }

    /**
     * Configure request and response interceptors
     */
    private setupInterceptors(): void {
        this.client.interceptors.request.use(async (config) => {
            logger.debug(`Preparing request for: ${config.url}`);

            const isAuthEndpoint = config.url?.includes("/auth/") && !config.url?.includes("/auth/validate");

            if (isAuthEndpoint) {
                logger.debug(`Auth endpoint detected, skipping token: ${config.url}`);
                return config;
            }

            const session = await getSession();

            if (session?.backendToken) {
                config.headers = config.headers || {};
                config.headers.Authorization = `Bearer ${session.backendToken}`;
                logger.debug(`Adding auth token to request: ${config.url}. Token present: true`);

                try {
                    const payload = JSON.parse(atob(session.backendToken.split('.')[1]));
                    const expiryDate = new Date(payload.exp * 1000);
                    logger.debug(`Token expires at: ${expiryDate.toISOString()}, now: ${new Date().toISOString()}`);
                } catch (e) {
                    logger.warn('Could not parse token expiry time', e);
                }
            } else {
                logger.warn(`No auth token available for request: ${config.url}`);
            }

            logger.debug(`Request prepared for: ${config.url}`);
            return config;
        }, (error) => Promise.reject(error));

        this.client.interceptors.response.use((response) => response, async (error: AxiosError) => {
            const originalRequest = error.config as AxiosRequestConfig & { _retry?: boolean };

            if (error.response) {
                logger.error(`API error ${error.response.status}: ${error.response.statusText}`, {
                    url: originalRequest.url,
                    data: error.response.data
                });
            }

            if ((error.response?.status === 401 || error.response?.status === 403) && !originalRequest._retry && !originalRequest.url?.includes("/auth/")) {
                logger.info("Authentication error, logging out user");

                await authErrorHandler.handleError(error.response?.status === 403 ? 'unauthorized' : 'session_expired');

                return Promise.reject({
                    isAuthError: true,
                    status: error.response?.status,
                    message: "Authentication failed. You have been logged out."
                });
            }

            return Promise.reject(error);
        });
    }
}

export const apiClient = new ApiClient();
"use client";

import React from "react";
import dynamic from "next/dynamic";

/**
 * Dynamic import of the Toaster component from the sonner library.
 * Disabled on the server side to prevent hydration issues.
 */
const Toaster = dynamic(() => import("sonner").then(mod => mod.Toaster), {
    ssr: false
});

interface ToastInterface {
    (message: string, data?: unknown): string | number;

    error: (message: string, data?: unknown) => string | number;
    success: (message: string, data?: unknown) => string | number;
    info: (message: string, data?: unknown) => string | number;
    warning: (message: string, data?: unknown) => string | number;
    promise: (promise: Promise<unknown>, messages: {
        loading: string; success: string; error: string
    }, data?: unknown) => Promise<unknown>;
    custom: (message: React.ReactNode, data?: unknown) => string | number;
    dismiss: (toastId?: string | number) => void;
}

/**
 * Simple client-side toast utility
 */
let toast: ToastInterface = Object.assign(() => -1, {
    error: () => -1,
    success: () => -1,
    info: () => -1,
    warning: () => -1,
    promise: (promise: Promise<unknown>) => promise,
    custom: () => -1,
    dismiss: () => {
    },
});

if (typeof window !== "undefined") {
    import("sonner").then((sonner) => {
        toast = sonner.toast as unknown as ToastInterface;
    });
}

/**
 * ToastProvider component that sets up the toast notification system.
 * Renders the Toaster component with default configuration.
 */
export function ToastProvider(): React.JSX.Element {
    return <Toaster richColors position="top-right"/>;
}

export {toast};
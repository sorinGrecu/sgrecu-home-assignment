import {z} from 'zod';

const clientSchema = z.object({
    NEXT_PUBLIC_BACKEND_URL: z.string().url(),
});

const serverSchema = z.object({
    JBHA_GOOGLE_CLIENT_ID: z.string(),
    JBHA_GOOGLE_CLIENT_SECRET: z.string(),
    NEXTAUTH_SECRET: z.string(),
    NODE_ENV: z.enum(['development', 'stage', 'production']),
});

const processEnv = {
    NEXT_PUBLIC_BACKEND_URL: process.env.NEXT_PUBLIC_BACKEND_URL, ...(typeof window === 'undefined' ? {
        JBHA_GOOGLE_CLIENT_ID: process.env.JBHA_GOOGLE_CLIENT_ID,
        JBHA_GOOGLE_CLIENT_SECRET: process.env.JBHA_GOOGLE_CLIENT_SECRET,
        NEXTAUTH_SECRET: process.env.NEXTAUTH_SECRET,
        NODE_ENV: process.env.NODE_ENV,
    } : {})
};

export const clientEnv = clientSchema.parse(processEnv);

export const env = typeof window === 'undefined' ? serverSchema.parse(processEnv) : {NODE_ENV: process.env.NODE_ENV || 'development'};
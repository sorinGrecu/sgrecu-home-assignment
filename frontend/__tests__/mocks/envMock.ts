export const mockEnv = {
    NEXT_PUBLIC_BACKEND_URL: 'http://localhost:8080', NEXT_PUBLIC_BASE_URL: 'http://localhost:3000',
};

export const mockClientEnv = {
    ...mockEnv,
};

export const mockApiConfig = {
    BACKEND_URL: 'http://localhost:8080',
    API_TIMEOUT: 10000,
    API_HEADERS: {'Content-Type': 'application/json'},
    APP_BASE_URL: 'http://localhost:3000',
    IS_PRODUCTION: false,
};

export const mockZod = {
    __esModule: true, z: {
        string: () => ({
            url: () => ({_type: 'string'}),
        }), enum: () => ({_type: 'enum'}), object: () => ({
            parse: (obj: any) => obj,
        }),
    },
};

process.env.NEXT_PUBLIC_BACKEND_URL = 'http://localhost:8080';
process.env.NEXT_PUBLIC_BASE_URL = 'http://localhost:3000'; 
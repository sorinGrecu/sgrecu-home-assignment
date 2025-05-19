import "@testing-library/jest-dom";
import { server } from './__tests__/msw/handlers';

console.log('🚀 Jest setup started');

process.env.NEXT_PUBLIC_BACKEND_URL = "http://localhost:8080";
process.env.NEXT_PUBLIC_BASE_URL = "http://localhost:3000";

jest.mock('@/lib/env', () => ({
  clientEnv: {
    NEXT_PUBLIC_BACKEND_URL: "http://localhost:8080",
    NEXT_PUBLIC_BASE_URL: "http://localhost:3000",
  },
  env: {
    NEXT_PUBLIC_BACKEND_URL: "http://localhost:8080",
    NEXT_PUBLIC_BASE_URL: "http://localhost:3000",
    NODE_ENV: 'test'
  }
}));

jest.mock('@/lib/config', () => ({
  BACKEND_URL: "http://localhost:8080",
  API_TIMEOUT: 10000,
  API_HEADERS: { 'Content-Type': 'application/json' },
  APP_BASE_URL: "http://localhost:3000",
  IS_PRODUCTION: false
}));

jest.mock("next/navigation", () => ({
  useRouter: () => ({ 
    push: jest.fn(),
    replace: jest.fn() 
  }),
  usePathname: () => "/",
  useSearchParams: () => ({ get: () => null }),
}));

jest.mock("next-auth/react", () => ({
  useSession: jest.fn(() => ({
    data: {
      expires: '2100-01-01T00:00:00.000Z',
      user: { id: 'test-user', name: 'Test User', email: 'test@example.com' },
      backendToken: 'mock-token',
    },
    status: 'authenticated'
  })),
  getSession: jest.fn(() => Promise.resolve({
    expires: '2100-01-01T00:00:00.000Z',
    user: { id: 'test-user', name: 'Test User', email: 'test@example.com' },
    backendToken: 'mock-token',
  })),
  signIn: jest.fn(),
  signOut: jest.fn(),
  SessionProvider: ({ children }: { children: React.ReactNode }) => children,
}));

expect.extend({
  toHaveBeenCalledOnce(received) {
    const pass = received.mock.calls.length === 1;
    return {
      pass,
      message: () => pass
        ? `Expected ${received.getMockName()} not to be called once`
        : `Expected ${received.getMockName()} to be called once, but it was called ${received.mock.calls.length} times`
    };
  }
});

beforeAll(() => {
  console.log('🔄 Setting up MSW server for Jest');
  server.listen({ 
    onUnhandledRequest: (req) => {
      console.error('❌ Found an unhandled %s request to %s', req.method, req.url.href);
    }
  });
  console.log('✅ MSW server started');
});

afterEach(() => {
  console.log('🧹 Resetting MSW request handlers');
  server.resetHandlers();
});

afterAll(() => {
  console.log('🛑 Shutting down MSW server');
  server.close();
  console.log('👋 MSW server closed');
}); 
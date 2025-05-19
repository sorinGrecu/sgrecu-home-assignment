import '@testing-library/jest-dom';

if (typeof TextEncoder === 'undefined') {
  global.TextEncoder = require('util').TextEncoder;
}

if (typeof TextDecoder === 'undefined') {
  global.TextDecoder = require('util').TextDecoder;
}

class MockBroadcastChannel {
  constructor(channel) {
    this.channel = channel;
    this.onmessage = null;
  }
  postMessage() {
  }
  close() {
  }
}

global.BroadcastChannel = MockBroadcastChannel;

global.Request = jest.fn((input, init) => ({
  url: typeof input === 'string' ? input : 'http://localhost',
  method: init?.method || 'GET',
  headers: init?.headers || {},
  json: jest.fn().mockResolvedValue({}),
}));

global.Response = jest.fn((body, init) => {
  const responseBody = typeof body === 'string' ? body : JSON.stringify(body || {});
  return {
    ok: (init?.status || 200) >= 200 && (init?.status || 200) < 300,
    json: jest.fn().mockResolvedValue(
      typeof responseBody === 'string' && responseBody.startsWith('{')
        ? JSON.parse(responseBody)
        : {}
    ),
    text: jest.fn().mockResolvedValue(responseBody),
    headers: new Map(Object.entries(init?.headers || {})),
    status: init?.status || 200,
  };
});

global.Headers = jest.fn(() => new Map());

global.NextRequest = global.Request;
global.NextResponse = {
  json: jest.fn().mockImplementation((body, options = {}) => {
    const status = options.status || 200;
    const mockResponse = {
      json: jest.fn().mockResolvedValue(body),
      status,
      headers: new Map(Object.entries(options.headers || { 'Content-Type': 'application/json' })),
    };
    return mockResponse;
  }),
  redirect: jest.fn().mockImplementation((url) => {
    return new Response(null, {
      status: 302,
      headers: { Location: url },
    });
  }),
  next: jest.fn(),
};

jest.mock('next/server', () => ({
  NextRequest: global.Request,
  NextResponse: global.NextResponse,
}));

global.fetch = jest.fn(() => 
  Promise.resolve({
    ok: true,
    status: 200,
    json: jest.fn().mockResolvedValue({}),
    text: jest.fn().mockResolvedValue(''),
    headers: new Map(),
  })
);

jest.mock('next/navigation', () => ({
  useRouter: () => ({
    push: jest.fn(),
    replace: jest.fn(),
    prefetch: jest.fn(),
    back: jest.fn(),
    forward: jest.fn(),
  }),
  usePathname: jest.fn(() => '/'),
  useSearchParams: jest.fn(() => new URLSearchParams()),
  redirect: jest.fn(),
}));

jest.mock('next-auth', () => ({
  auth: jest.fn(() => Promise.resolve({ user: null })),
}));

jest.mock('next-auth/react', () => ({
  __esModule: true,
  signIn: jest.fn(),
  signOut: jest.fn(),
  useSession: jest.fn(() => ({
    data: null,
    status: 'unauthenticated',
  })),
  getSession: jest.fn(() => Promise.resolve(null)),
  getCsrfToken: jest.fn(() => Promise.resolve('csrf-token')),
  getProviders: jest.fn(() => Promise.resolve({})),
}));

jest.mock('next-auth/providers/google', () => ({
  default: jest.fn(() => ({
    id: 'google',
    name: 'Google',
    type: 'oauth',
  })),
}));

jest.mock('@/app/components/ui/AnimatedBorder', () => ({
  AnimatedBorder: ({ children }) => <>{children}</>
}));

jest.spyOn(console, 'error').mockImplementation(() => {}); 
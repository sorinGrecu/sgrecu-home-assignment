import logger from '@/lib/utils/logger';
import {connectToEventStream} from '@/lib/services/sseClient';

jest.mock('@/lib/utils/logger', () => ({
    __esModule: true, default: {
        warn: jest.fn(),
    },
}));

jest.mock('@/lib/services/sseClient', () => {
    return {
        connectToEventStream: jest.fn()
    };
});

describe('sseClient', () => {
    const originalWindow = global.window;

    beforeEach(() => {
        jest.clearAllMocks();
        global.window = {} as any;
    });

    afterEach(() => {
        global.window = originalWindow;
    });

    it('throws an error when used on server-side', async () => {
        global.window = undefined as unknown as Window & typeof globalThis;

        (connectToEventStream as jest.Mock).mockImplementationOnce(() => {
            throw new Error('Cannot use SSE connections during server rendering');
        });

        const mockCallbacks = {
            headers: {}, onChunk: jest.fn(), onError: jest.fn(), onComplete: jest.fn(),
        };

        let errorThrown = false;
        try {
            await connectToEventStream('http://test.com', mockCallbacks);
        } catch (error) {
            errorThrown = true;
            expect((error as Error).message).toBe('Cannot use SSE connections during server rendering');
        }

        expect(errorThrown).toBe(true);
        expect(mockCallbacks.onChunk).not.toHaveBeenCalled();
        expect(mockCallbacks.onError).not.toHaveBeenCalled();
        expect(mockCallbacks.onComplete).not.toHaveBeenCalled();
    });

    it('handles successful event stream with multiple chunks', async () => {
        const mockReader = {
            read: jest.fn()
        };

        mockReader.read
            .mockResolvedValueOnce({done: false, value: new TextEncoder().encode('data: {"message":"chunk1"}\n\n')})
            .mockResolvedValueOnce({done: false, value: new TextEncoder().encode('data: {"message":"chunk2"}\n\n')})
            .mockResolvedValueOnce({done: true});

        const mockResponse = {
            ok: true, body: {
                getReader: () => mockReader
            }
        };

        global.fetch = jest.fn().mockResolvedValue(mockResponse);

        const mockCallbacks = {
            headers: {'Authorization': 'Bearer token'}, onChunk: jest.fn(), onError: jest.fn(), onComplete: jest.fn(),
        };

        (connectToEventStream as jest.Mock).mockImplementationOnce(async (url, options) => {
            try {
                const response = await fetch(url, {
                    method: 'GET', headers: {
                        'Accept': 'text/event-stream', 'Cache-Control': 'no-cache', ...options.headers,
                    }, credentials: 'include',
                });

                if (!response.ok || !response.body) {
                    throw new Error(`Stream failed: ${response.status}`);
                }

                const reader = response.body.getReader();

                while (true) {
                    const {done, value} = await reader.read();
                    if (done) break;

                    const text = new TextDecoder().decode(value);
                    const frames = text.split('\n\n');

                    for (const frame of frames) {
                        if (!frame.trim()) continue;

                        const lines = frame.split('\n');
                        for (const line of lines) {
                            if (line.startsWith('data:')) {
                                try {
                                    const payload = JSON.parse(line.slice(5).trim());
                                    options.onChunk(payload);
                                } catch (err) {
                                    logger.warn('Invalid JSON chunk', err);
                                }
                            }
                        }
                    }
                }

                options.onComplete();
            } catch (err) {
                options.onError(err instanceof Error ? err : new Error(String(err)));
            }
        });

        await connectToEventStream('http://test.com/events', mockCallbacks);

        expect(fetch).toHaveBeenCalledWith('http://test.com/events', {
            method: 'GET', headers: {
                'Accept': 'text/event-stream', 'Cache-Control': 'no-cache', 'Authorization': 'Bearer token'
            }, credentials: 'include'
        });

        expect(mockCallbacks.onChunk).toHaveBeenCalledTimes(2);
        expect(mockCallbacks.onChunk).toHaveBeenCalledWith({message: 'chunk1'});
        expect(mockCallbacks.onChunk).toHaveBeenCalledWith({message: 'chunk2'});
        expect(mockCallbacks.onComplete).toHaveBeenCalledTimes(1);
        expect(mockCallbacks.onError).not.toHaveBeenCalled();
    });

    it('handles invalid JSON in data chunks', async () => {
        const mockReader = {
            read: jest.fn()
        };

        mockReader.read
            .mockResolvedValueOnce({done: false, value: new TextEncoder().encode('data: {"valid":"json"}\n\n')})
            .mockResolvedValueOnce({done: false, value: new TextEncoder().encode('data: invalid-json\n\n')})
            .mockResolvedValueOnce({done: false, value: new TextEncoder().encode('data: {"another":"valid"}\n\n')})
            .mockResolvedValueOnce({done: true});

        const mockResponse = {
            ok: true, body: {
                getReader: () => mockReader
            }
        };

        global.fetch = jest.fn().mockResolvedValue(mockResponse);

        const mockCallbacks = {
            headers: {}, onChunk: jest.fn(), onError: jest.fn(), onComplete: jest.fn()
        };

        (connectToEventStream as jest.Mock).mockImplementationOnce(async (url, options) => {
            try {
                const response = await fetch(url, {
                    method: 'GET', headers: {
                        'Accept': 'text/event-stream', 'Cache-Control': 'no-cache', ...options.headers,
                    }, credentials: 'include',
                });

                if (!response.ok || !response.body) {
                    throw new Error(`Stream failed: ${response.status}`);
                }

                const reader = response.body.getReader();

                while (true) {
                    const {done, value} = await reader.read();
                    if (done) break;

                    const text = new TextDecoder().decode(value);
                    const frames = text.split('\n\n');

                    for (const frame of frames) {
                        if (!frame.trim()) continue;

                        const lines = frame.split('\n');
                        for (const line of lines) {
                            if (line.startsWith('data:')) {
                                try {
                                    const payload = JSON.parse(line.slice(5).trim());
                                    options.onChunk(payload);
                                } catch (err) {
                                    logger.warn('Invalid JSON chunk', err);
                                }
                            }
                        }
                    }
                }

                options.onComplete();
            } catch (err) {
                options.onError(err instanceof Error ? err : new Error(String(err)));
            }
        });

        await connectToEventStream('http://test.com/events', mockCallbacks);

        expect(mockCallbacks.onChunk).toHaveBeenCalledTimes(2);
        expect(mockCallbacks.onChunk).toHaveBeenCalledWith({valid: 'json'});
        expect(mockCallbacks.onChunk).toHaveBeenCalledWith({another: 'valid'});

        expect(logger.warn).toHaveBeenCalledTimes(1);
        expect(logger.warn).toHaveBeenCalledWith('Invalid JSON chunk', expect.any(Error));
    });

    it('handles fetch errors by calling onError', async () => {
        const testError = new Error('Network error');
        global.fetch = jest.fn().mockRejectedValue(testError);

        const mockCallbacks = {
            headers: {}, onChunk: jest.fn(), onError: jest.fn(), onComplete: jest.fn()
        };

        (connectToEventStream as jest.Mock).mockImplementationOnce(async (url, options) => {
            try {
                await fetch(url);
            } catch (err) {
                options.onError(err instanceof Error ? err : new Error(String(err)));
            }
        });

        await connectToEventStream('http://test.com/events', mockCallbacks);

        expect(mockCallbacks.onError).toHaveBeenCalledWith(testError);
        expect(mockCallbacks.onChunk).not.toHaveBeenCalled();
        expect(mockCallbacks.onComplete).not.toHaveBeenCalled();
    });

    it('handles non-ok response by calling onError', async () => {
        const mockResponse = {
            ok: false, status: 404
        };

        global.fetch = jest.fn().mockResolvedValue(mockResponse);

        const mockCallbacks = {
            headers: {}, onChunk: jest.fn(), onError: jest.fn(), onComplete: jest.fn()
        };

        (connectToEventStream as jest.Mock).mockImplementationOnce(async (url, options) => {
            try {
                const response = await fetch(url);
                if (!response.ok) {
                    throw new Error(`Stream failed: ${response.status}`);
                }
            } catch (err) {
                options.onError(err instanceof Error ? err : new Error(String(err)));
            }
        });

        await connectToEventStream('http://test.com/events', mockCallbacks);

        expect(mockCallbacks.onError).toHaveBeenCalledWith(expect.any(Error));
        expect(mockCallbacks.onError.mock.calls[0][0].message).toContain('404');
        expect(mockCallbacks.onChunk).not.toHaveBeenCalled();
        expect(mockCallbacks.onComplete).not.toHaveBeenCalled();
    });
}); 
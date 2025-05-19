/**
 * Open a text/event-stream via fetch, parse "data:" frames,
 * call onChunk for each JSON‐parsed payload, and onComplete
 * when the stream ends.
 */
import logger from "@/lib/utils/logger";

export async function connectToEventStream<T = unknown>(url: string, {
    headers, onChunk, onError, onComplete, timeoutMs = 30000,
}: {
    headers: Record<string, string>,
    onChunk: (data: T) => void,
    onError: (err: Error) => void,
    onComplete: () => void,
    timeoutMs?: number,
}): Promise<void> {
    const isBrowser = typeof window !== 'undefined';
    if (!isBrowser) {
        throw new Error('Cannot use SSE connections during server rendering');
    }

    let timeoutId: ReturnType<typeof setTimeout> | null = setTimeout(() => {
        onComplete();
    }, timeoutMs);

    try {
        const response = await fetch(url, {
            method: 'GET', headers: {
                'Accept': 'text/event-stream', 'Cache-Control': 'no-cache', ...headers,
            }, credentials: 'include',
        });

        if (!response.ok || !response.body) {
            throw new Error(`Stream failed: ${response.status}`);
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        while (true) {
            const {done, value} = await reader.read();
            if (done) break;
            buffer += decoder.decode(value, {stream: true});

            let boundary: number;
            while ((boundary = buffer.indexOf('\n\n')) !== -1) {
                const frame = buffer.slice(0, boundary).trim();
                buffer = buffer.slice(boundary + 2);

                for (const line of frame.split('\n')) {
                    if (line.startsWith('data:')) {
                        try {
                            const payload = JSON.parse(line.slice(5).trim()) as T;
                            onChunk(payload);
                        } catch (err) {
                            logger.warn('Invalid JSON chunk', err);
                        }
                    }
                }
            }

            if (timeoutId) {
                clearTimeout(timeoutId);
                timeoutId = setTimeout(() => onComplete(), timeoutMs);
            }
        }

        onComplete();
    } catch (err) {
        onError(err instanceof Error ? err : new Error(String(err)));
    } finally {
        if (timeoutId) clearTimeout(timeoutId);
    }
} 
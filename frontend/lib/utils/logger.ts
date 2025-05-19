export interface Logger {
    debug: (...args: unknown[]) => void;
    info: (...args: unknown[]) => void;
    warn: (...args: unknown[]) => void;
    error: (...args: unknown[]) => void;
}

const logger: Logger = process.env.NODE_ENV === 'production' ? {
    debug: () => {
    }, info: () => {
    }, warn: () => {
    }, error: () => {
    }
} : console;

export default logger; 
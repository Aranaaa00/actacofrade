import { HttpErrorResponse } from '@angular/common/http';
import { MonoTypeOperatorFunction, retry, timer } from 'rxjs';

const RETRYABLE_STATUS = new Set<number>([0, 408, 425, 429, 500, 502, 503, 504]);

// Retries transient HTTP failures with linear backoff. Skips client errors.
export function retryReads<T>(maxAttempts = 2, baseDelayMs = 400): MonoTypeOperatorFunction<T> {
  return retry({
    count: maxAttempts,
    delay: (error, attempt) => {
      const status = error instanceof HttpErrorResponse ? error.status : 0;
      if (!RETRYABLE_STATUS.has(status)) {
        throw error;
      }
      return timer(baseDelayMs * attempt);
    },
  });
}

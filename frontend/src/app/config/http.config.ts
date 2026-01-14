import { timeout, Observable } from 'rxjs';

/**
 * HTTP Request Timeout Configuration
 * Prevents hanging requests and ensures timely user feedback
 */
export const HTTP_TIMEOUT = 30000; // 30 seconds

/**
 * Apply timeout to any Observable HTTP call
 * Usage: this.http.get(url).pipe(applyTimeout())
 * Generic type is preserved through the pipe
 */
export function applyTimeout<T>(): (source: Observable<T>) => Observable<T> {
  return (source: Observable<T>) => source.pipe(timeout(HTTP_TIMEOUT));
}

import { HttpInterceptorFn, HttpErrorResponse, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { retry, timer, throwError, catchError } from 'rxjs';

/**
 * Retry Interceptor - Automatically retries on 503 (Service Unavailable)
 * Does NOT retry on 409 (Conflict) for POST/PUT/DELETE as data errors are not transient
 * Uses exponential backoff: 100ms, 200ms, 400ms for 3 retries
 */
export const RetryInterceptor: HttpInterceptorFn = (req, next) => {
  const toastr = inject(ToastrService);

  return next(req).pipe(
    retry({
      count: 3,
      delay: (error: HttpErrorResponse, retryCount: number) => {
        // Only retry on 503 (Service Unavailable) - transient server errors
        // DO NOT retry on 409 (Conflict) - data integrity errors are not transient
        // DO NOT retry on POST/PUT/DELETE with 409 - these are client errors that won't change
        if (error.status === 503) {
          const exponentialDelay = 100 * Math.pow(2, retryCount - 1);
          toastr.info('Server busy. Retrying automatically...', 'Retrying', {
            timeOut: 3000,
            progressBar: true
          });
          return timer(exponentialDelay);
        }

        // Don't retry other errors
        throw error;
      }
    }),
    catchError((error: HttpErrorResponse) => {
      // After retries exhausted
      if (error.status === 503) {
        toastr.error('System is busy. Please try again in a moment.', 'Error');
      }
      return throwError(() => error);
    })
  );
};

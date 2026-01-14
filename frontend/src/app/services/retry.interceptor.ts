import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { retry, timer, throwError, catchError } from 'rxjs';

/**
 * Retry Interceptor - Automatically retries on 409 (Conflict) and 503 (Service Unavailable)
 * Uses exponential backoff: 100ms, 200ms, 400ms for 3 retries
 */
export const RetryInterceptor: HttpInterceptorFn = (req, next) => {
  const toastr = inject(ToastrService);

  return next(req).pipe(
    retry({
      count: 3,
      delay: (error: HttpErrorResponse, retryCount: number) => {
        // Retry only on 409 (Conflict) or 503 (Service Unavailable)
        if (error.status === 409 || error.status === 503) {
          const exponentialDelay = 100 * Math.pow(2, retryCount - 1);

          // Show retry info only on 409 (user-facing issue)
          if (error.status === 409 && retryCount === 1) {
            toastr.info('Data was modified. Retrying automatically...', 'Retrying', {
              timeOut: 3000,
              progressBar: true
            });
          }

          // Return delayed retry
          return timer(exponentialDelay);
        }

        // Don't retry other errors
        throw error;
      }
    }),
    catchError((error: HttpErrorResponse) => {
      // After retries exhausted
      if (error.status === 409) {
        toastr.error('Data conflict persists. Please refresh and try again.', 'Error');
      } else if (error.status === 503) {
        toastr.error('System is busy. Please try again in a moment.', 'Error');
      }
      return throwError(() => error);
    })
  );
};

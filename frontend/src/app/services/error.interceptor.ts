import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { catchError, throwError } from 'rxjs';

/**
 * Error Interceptor - Handles HTTP errors with smart error messages
 * Distinguishes between different error types for better user feedback
 */
export const ErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const toastr = inject(ToastrService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Don't show error toast for 409 (retryable) and 503 (retryable)
      // These will be handled by retry interceptor
      if (error.status === 409 || error.status === 503) {
        // Pass through without showing toast - retry interceptor will handle
        return throwError(() => error);
      }

      // Handle other errors
      let errorMessage = 'An error occurred. Please try again.';

      if (error.status === 0) {
        errorMessage = 'Network error. Please check your connection.';
      } else if (error.status === 400) {
        errorMessage = error.error?.message || 'Invalid request. Please check your input.';
      } else if (error.status === 401) {
        errorMessage = 'Session expired. Please login again.';
      } else if (error.status === 403) {
        errorMessage = 'You do not have permission to perform this action.';
      } else if (error.status === 404) {
        errorMessage = 'Resource not found.';
      } else if (error.status >= 500) {
        errorMessage = 'Server error. Please try again later.';
      } else if (error.error?.message) {
        errorMessage = error.error.message;
      }

      // Only show toast for non-retry errors
      toastr.error(errorMessage, 'Error');
      return throwError(() => error);
    })
  );
};

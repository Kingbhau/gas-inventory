import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';
import { Observable, throwError, switchMap, catchError, of, timeout } from 'rxjs';

// Interceptor to handle 401 errors and refresh token
export const RefreshInterceptor: HttpInterceptorFn = (req, next) => {
  // Inject AuthService at the top level to ensure proper injection context
  const authService = inject(AuthService);
  
  // Add a retry limit to prevent infinite refresh attempts
  const retryCount = req.headers.get('x-retry-count') ? parseInt(req.headers.get('x-retry-count')!) : 0;
  
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Only handle 401 errors for non-auth endpoints with retry count < 1
      if (
        error.status === 401 &&
        !req.url.includes('/auth/refresh') &&
        !req.url.includes('/auth/login') &&
        retryCount < 1
      ) {
        // Add timeout to prevent hanging (5 seconds)
        return authService.refreshToken().pipe(
          timeout(5000),
          switchMap(() => {
            // Retry original request after refresh, increment retry count
            const retryReq = req.clone({
              withCredentials: true,
              headers: req.headers.set('x-retry-count', (retryCount + 1).toString())
            });
            return next(retryReq);
          }),
          catchError(refreshError => {
            // If refresh fails or times out, clear auth and redirect to login
            console.error('Token refresh failed:', refreshError);
            localStorage.removeItem('user_info');
            localStorage.removeItem('user_role');
            localStorage.removeItem('user_name');
            // Use setTimeout to avoid blocking
            setTimeout(() => {
              window.location.href = '/login';
            }, 0);
            return throwError(() => error);
          })
        );
      }
      return throwError(() => error);
    })
  );
};

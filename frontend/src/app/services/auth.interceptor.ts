import { HttpInterceptorFn } from '@angular/common/http';

// Interceptor to add withCredentials: true to all requests for cookie-based auth
export const AuthInterceptor: HttpInterceptorFn = (req, next) => {
  const cloned = req.clone({ withCredentials: true });
  return next(cloned);
};

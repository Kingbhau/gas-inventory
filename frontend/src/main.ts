import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { AuthInterceptor } from './app/services/auth.interceptor';
import { RefreshInterceptor } from './app/services/refresh.interceptor';
import { RetryInterceptor } from './app/services/retry.interceptor';
import { ErrorInterceptor } from './app/services/error.interceptor';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideToastr } from 'ngx-toastr';

import { AppComponent } from './app/app.component';
import { routes } from './app/app.routes';

bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([AuthInterceptor, RefreshInterceptor, RetryInterceptor, ErrorInterceptor])),
    provideAnimations(),
    provideToastr({
      timeOut: 500,
      positionClass: 'toast-top-right',
      preventDuplicates: true,
      progressBar: true,
      progressAnimation: 'increasing'
    })
  ]
}).catch(() => undefined);

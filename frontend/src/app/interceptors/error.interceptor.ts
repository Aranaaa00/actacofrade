import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

// Centralizes 401 handling and surfaces network/server failures as toasts.
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const toast = inject(ToastService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const isAuthCall = req.url.includes('/api/auth/');
      if (error.status === 401 && !isAuthCall && auth.isAuthenticated()) {
        auth.logout();
        router.navigate(['/login']);
        toast.warning('Tu sesión ha caducado. Vuelve a iniciar sesión.');
      } else if (error.status === 0) {
        toast.error('Sin conexión con el servidor. Comprueba tu red e inténtalo de nuevo.');
      } else if (error.status >= 500) {
        toast.error('Error del servidor. Inténtalo de nuevo más tarde.');
      }
      return throwError(() => error);
    })
  );
};

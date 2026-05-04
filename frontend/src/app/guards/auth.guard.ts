import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

// Blocks unauthenticated navigation and redirects to the login page.
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const authenticated = authService.isAuthenticated();
  if (!authenticated) {
    router.navigate(['/login']);
  }
  return authenticated;
};

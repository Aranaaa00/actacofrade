import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const roleGuard = (roles: string[]): CanActivateFn => () => {
  // factory guard that checks whether the current user owns any of the allowed roles
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.hasAnyRole(...roles)) {
    return true;
  }
  router.navigate(['/dashboard']);
  return false;
};

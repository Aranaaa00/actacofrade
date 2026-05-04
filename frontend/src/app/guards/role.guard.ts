import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { Role } from '../shared/constants/roles.const';

// Allows navigation only if the user owns at least one of the given roles.
export const roleGuard = (roles: ReadonlyArray<Role | string>): CanActivateFn => () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.hasAnyRole(...roles)) {
    return true;
  }
  router.navigate(['/dashboard']);
  return false;
};

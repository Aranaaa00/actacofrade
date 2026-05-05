import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { Role } from '../shared/constants/roles.const';

// Factory guard that checks whether the current user owns any of the allowed roles.
export const roleGuard = (roles: ReadonlyArray<Role | string>): CanActivateFn => () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.hasAnyRole(...roles)) {
    return true;
  }
  router.navigate([authService.isSuperAdmin() ? '/super-admin' : '/dashboard']);
  return false;
};

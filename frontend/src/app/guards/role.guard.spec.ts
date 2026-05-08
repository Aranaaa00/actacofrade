import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { roleGuard } from './role.guard';
import { AuthService } from '../services/auth.service';

describe('roleGuard', () => {
  let router: jasmine.SpyObj<Router>;
  let auth: jasmine.SpyObj<AuthService>;

  function run(allowed: ReadonlyArray<string>): boolean | any {
    return TestBed.runInInjectionContext(() => roleGuard(allowed)({} as any, {} as any));
  }

  beforeEach(() => {
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['hasAnyRole', 'isSuperAdmin']);
    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: router },
        { provide: AuthService, useValue: auth },
      ],
    });
  });

  it('allows when user holds an allowed role', () => {
    auth.hasAnyRole.and.returnValue(true);
    expect(run(['ADMINISTRADOR'])).toBe(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('redirects to /super-admin when user is super admin', () => {
    auth.hasAnyRole.and.returnValue(false);
    auth.isSuperAdmin.and.returnValue(true);
    expect(run(['ADMINISTRADOR'])).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/super-admin']);
  });

  it('redirects to /dashboard otherwise', () => {
    auth.hasAnyRole.and.returnValue(false);
    auth.isSuperAdmin.and.returnValue(false);
    expect(run(['ADMINISTRADOR'])).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });
});

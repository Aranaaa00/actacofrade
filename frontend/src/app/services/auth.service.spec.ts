import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { buildAuthResponse, buildEvent } from '../../testing/fixtures';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), AuthService],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
    localStorage.clear();
  });

  it('logs in and persists session', () => {
    const auth = buildAuthResponse({ token: 't1' });
    service.login({ email: 'a@b.c', password: 'x' }).subscribe((res) => {
      expect(res.token).toBe('t1');
    });
    const req = http.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush(auth);
    expect(service.getToken()).toBe('t1');
    expect(service.isAuthenticated()).toBe(true);
    expect(service.getUser()?.email).toBe(auth.email);
  });

  it('registers without starting a session (pending email verification)', () => {
    service.register({ fullName: 'x', email: 'a@b.c', password: 'p', roleCode: 'ADMINISTRADOR', hermandadNombre: 'X' })
      .subscribe((res) => {
        expect(res.status).toBe('pending_verification');
      });
    const req = http.expectOne('/api/auth/register');
    expect(req.request.method).toBe('POST');
    req.flush({ status: 'pending_verification', message: 'ok' });
    expect(service.getToken()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
  });

  it('verifyEmail consumes the token and persists the session', () => {
    const auth = buildAuthResponse({ token: 'verified-token' });
    service.verifyEmail('abc').subscribe((res) => {
      expect(res.token).toBe('verified-token');
    });
    const req = http.expectOne((r) => r.url === '/api/auth/verify-email' && r.params.get('token') === 'abc');
    expect(req.request.method).toBe('GET');
    req.flush(auth);
    expect(service.getToken()).toBe('verified-token');
    expect(service.isAuthenticated()).toBe(true);
  });

  it('resendVerification posts the email and returns the generic status', () => {
    service.resendVerification({ email: 'a@b.c' }).subscribe((res) => {
      expect(res.status).toBe('pending_verification');
    });
    const req = http.expectOne('/api/auth/resend-verification');
    expect(req.request.method).toBe('POST');
    req.flush({ status: 'pending_verification', message: 'ok' });
  });

  it('lists hermandades', () => {
    service.listHermandades().subscribe((list) => expect(list.length).toBe(1));
    const req = http.expectOne('/api/auth/hermandades');
    req.flush([{ id: 1, nombre: 'X' }]);
  });

  it('logout clears stored session', () => {
    sessionStorage.setItem('auth_token', 't');
    sessionStorage.setItem('auth_user', JSON.stringify(buildAuthResponse()));
    service.logout();
    expect(service.getToken()).toBeNull();
    expect(service.getUser()).toBeNull();
  });

  it('returns null and clears storage when stored user is corrupt', () => {
    sessionStorage.setItem('auth_token', 't');
    sessionStorage.setItem('auth_user', '{not json');
    expect(service.getUser()).toBeNull();
    expect(service.getToken()).toBeNull();
  });

  it('updateStoredUser persists the new payload', () => {
    const updated = buildAuthResponse({ fullName: 'New' });
    service.updateStoredUser(updated);
    expect(service.getUser()?.fullName).toBe('New');
  });

  it('hasRole / hasAnyRole reflect stored roles', () => {
    sessionStorage.setItem('auth_user', JSON.stringify(buildAuthResponse({ roles: ['RESPONSABLE'] })));
    sessionStorage.setItem('auth_token', 't');
    expect(service.hasRole('RESPONSABLE')).toBe(true);
    expect(service.hasRole('ADMINISTRADOR')).toBe(false);
    expect(service.hasAnyRole('CONSULTA', 'RESPONSABLE')).toBe(true);
  });

  it('returns false when no user is stored', () => {
    expect(service.hasRole('ADMINISTRADOR')).toBe(false);
  });

  it('canWrite/canManage/isAdmin/isSuperAdmin/isConsulta evaluate roles', () => {
    sessionStorage.setItem('auth_token', 't');
    sessionStorage.setItem('auth_user', JSON.stringify(buildAuthResponse({ roles: ['ADMINISTRADOR'] })));
    expect(service.canWrite()).toBe(true);
    expect(service.canManage()).toBe(true);
    expect(service.isAdmin()).toBe(true);
    expect(service.isSuperAdmin()).toBe(false);
    expect(service.isConsulta()).toBe(false);

    sessionStorage.setItem('auth_user', JSON.stringify(buildAuthResponse({ roles: ['SUPER_ADMIN'] })));
    expect(service.isSuperAdmin()).toBe(true);

    sessionStorage.setItem('auth_user', JSON.stringify(buildAuthResponse({ roles: ['CONSULTA'] })));
    expect(service.isConsulta()).toBe(true);
    expect(service.canWrite()).toBe(false);
  });

  it('canManageAct returns true for admin and matching responsible', () => {
    sessionStorage.setItem('auth_token', 't');
    sessionStorage.setItem('auth_user', JSON.stringify(buildAuthResponse({ userId: 5, roles: ['RESPONSABLE'] })));
    expect(service.canManageAct(buildEvent({ responsibleId: 5 }))).toBe(true);
    expect(service.canManageAct(buildEvent({ responsibleId: 9 }))).toBe(false);
    expect(service.canManageAct(null)).toBe(false);

    sessionStorage.setItem('auth_user', JSON.stringify(buildAuthResponse({ userId: 5, roles: ['ADMINISTRADOR'] })));
    expect(service.canManageAct(buildEvent({ responsibleId: 99 }))).toBe(true);
  });

  it('getUserId returns null when no user is stored', () => {
    expect(service.getUserId()).toBeNull();
  });

  it('migrates legacy localStorage session into sessionStorage on construction', () => {
    sessionStorage.clear();
    localStorage.setItem('auth_token', 'legacy');
    localStorage.setItem('auth_user', JSON.stringify(buildAuthResponse({ token: 'legacy' })));
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), AuthService],
    });
    const svc = TestBed.inject(AuthService);
    expect(svc.getToken()).toBe('legacy');
    expect(localStorage.getItem('auth_token')).toBeNull();
  });
});

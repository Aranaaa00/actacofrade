import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { errorInterceptor } from './error.interceptor';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

describe('errorInterceptor', () => {
  let auth: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;
  let toast: jasmine.SpyObj<ToastService>;
  let http: HttpClient;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['logout', 'isAuthenticated']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    toast = jasmine.createSpyObj<ToastService>('ToastService', ['error', 'warning']);
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router },
        { provide: ToastService, useValue: toast },
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('logs out and redirects on 401 outside auth endpoints', () => {
    auth.isAuthenticated.and.returnValue(true);
    http.get('/api/data').subscribe({ error: () => undefined });
    httpTesting.expectOne('/api/data').flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(auth.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
    expect(toast.warning).toHaveBeenCalled();
  });

  it('does not log out on 401 from /api/auth/ endpoints', () => {
    auth.isAuthenticated.and.returnValue(true);
    http.post('/api/auth/login', {}).subscribe({ error: () => undefined });
    httpTesting.expectOne('/api/auth/login').flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(auth.logout).not.toHaveBeenCalled();
  });

  it('shows offline toast on status 0', () => {
    http.get('/api/x').subscribe({ error: () => undefined });
    httpTesting.expectOne('/api/x').error(new ProgressEvent('error'), { status: 0 });
    expect(toast.error).toHaveBeenCalled();
  });

  it('shows server toast on 5xx', () => {
    http.get('/api/x').subscribe({ error: () => undefined });
    httpTesting.expectOne('/api/x').flush(null, { status: 500, statusText: 'Server' });
    expect(toast.error).toHaveBeenCalled();
  });

  it('forwards 4xx errors without side effects', () => {
    http.get('/api/x').subscribe({ error: () => undefined });
    httpTesting.expectOne('/api/x').flush(null, { status: 400, statusText: 'Bad' });
    expect(auth.logout).not.toHaveBeenCalled();
    expect(toast.error).not.toHaveBeenCalled();
  });
});

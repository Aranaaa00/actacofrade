import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ProfileService, AvatarValidationError, AVATAR_ALLOWED_TYPES, AVATAR_MAX_BYTES } from './profile.service';
import { buildUser } from '../../testing/fixtures';

describe('ProfileService', () => {
  let service: ProfileService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), ProfileService],
    });
    service = TestBed.inject(ProfileService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('basic endpoints', () => {
    service.me().subscribe();
    http.expectOne('/api/me').flush(buildUser());
    service.updateProfile({ fullName: 'x', email: 'a@b.c' }).subscribe();
    expect(http.expectOne('/api/me').request.method).toBe('PUT');
    service.changePassword({ currentPassword: 'a', newPassword: 'b' }).subscribe();
    expect(http.expectOne('/api/me/password').request.method).toBe('PATCH');
    service.deleteAccount().subscribe();
    expect(http.expectOne('/api/me').request.method).toBe('DELETE');
    service.deleteAvatar().subscribe();
    expect(http.expectOne('/api/me/avatar').request.method).toBe('DELETE');
  });

  it('uploadAvatar accepts a valid file and posts FormData', () => {
    const file = new File(['x'], 'a.png', { type: 'image/png' });
    service.uploadAvatar(file).subscribe();
    const req = http.expectOne('/api/me/avatar');
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBe(true);
    req.flush(buildUser());
  });

  it('uploadAvatar rejects unsupported MIME types', () => {
    const file = new File(['x'], 'a.gif', { type: 'image/gif' });
    service.uploadAvatar(file).subscribe({
      error: (err) => {
        expect(err).toEqual(jasmine.any(AvatarValidationError));
        expect(err.code).toBe('TYPE');
      },
    });
  });

  it('uploadAvatar rejects oversized files', () => {
    const big = new File([new Uint8Array(AVATAR_MAX_BYTES + 10)], 'a.png', { type: 'image/png' });
    service.uploadAvatar(big).subscribe({
      error: (err) => {
        expect(err.code).toBe('SIZE');
      },
    });
  });

  it('exposes the allowed type list', () => {
    expect(AVATAR_ALLOWED_TYPES).toContain('image/png');
  });

  it('loadAvatar returns an object URL', () => {
    spyOn(URL, 'createObjectURL').and.returnValue('blob:url');
    service.loadAvatar(7).subscribe((url) => expect(url).toBe('blob:url'));
    http.expectOne('/api/me/avatar/7').flush(new Blob(['x']));
  });
});

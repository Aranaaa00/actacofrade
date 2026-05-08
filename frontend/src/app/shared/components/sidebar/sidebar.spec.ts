import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { Sidebar } from './sidebar';
import { AuthService } from '../../../services/auth.service';
import { ProfileService } from '../../../services/profile.service';
import { LUCIDE_TEST_PROVIDERS } from '../../../../testing/lucide-test.providers';
import { provideRouter } from '@angular/router';
import { AuthResponse } from '../../../models/auth.model';

describe('Sidebar', () => {
  let fixture: ComponentFixture<Sidebar>;
  let s: Sidebar;
  let auth: jasmine.SpyObj<AuthService>;
  let profile: jasmine.SpyObj<ProfileService>;
  let router: Router;

  beforeEach(async () => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['isSuperAdmin']);
    auth.isSuperAdmin.and.returnValue(false);
    profile = jasmine.createSpyObj<ProfileService>('ProfileService', ['loadAvatar']);
    profile.loadAvatar.and.returnValue(of('blob:abc'));
    spyOn(URL, 'revokeObjectURL').and.callFake(() => undefined);

    await TestBed.configureTestingModule({
      imports: [Sidebar],
      providers: [
        provideRouter([]),
        LUCIDE_TEST_PROVIDERS,
        { provide: AuthService, useValue: auth },
        { provide: ProfileService, useValue: profile },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Sidebar);
    s = fixture.componentInstance;
    router = TestBed.inject(Router);
  });

  it('opens acts menu when route starts with /events', () => {
    spyOnProperty(router, 'url', 'get').and.returnValue('/events/list');
    s.ngOnInit();
    expect(s.actsMenuOpen).toBeTrue();
  });

  it('opens acts menu when route starts with /my-tasks', () => {
    spyOnProperty(router, 'url', 'get').and.returnValue('/my-tasks');
    s.ngOnInit();
    expect(s.actsMenuOpen).toBeTrue();
  });

  it('keeps acts menu closed for unrelated route', () => {
    spyOnProperty(router, 'url', 'get').and.returnValue('/dashboard');
    s.ngOnInit();
    expect(s.actsMenuOpen).toBeFalse();
  });

  it('toggleActsMenu flips state', () => {
    s.actsMenuOpen = false;
    s.toggleActsMenu();
    expect(s.actsMenuOpen).toBeTrue();
    s.toggleActsMenu();
    expect(s.actsMenuOpen).toBeFalse();
  });

  it('showContact respects hermandad and super admin role', () => {
    s.user = { hermandadNombre: 'X' } as unknown as AuthResponse;
    expect(s.showContact).toBeTrue();
    auth.isSuperAdmin.and.returnValue(true);
    expect(s.showContact).toBeFalse();
    auth.isSuperAdmin.and.returnValue(false);
    s.user = { hermandadNombre: '' } as unknown as AuthResponse;
    expect(s.showContact).toBeFalse();
    s.user = null;
    expect(s.showContact).toBeFalse();
  });

  it('roleLabel maps role code', () => {
    s.user = { roles: ['SUPER_ADMIN'] } as unknown as AuthResponse;
    expect(s.roleLabel.length).toBeGreaterThan(0);
    s.user = { roles: [] } as unknown as AuthResponse;
    expect(s.roleLabel).toBe('');
    s.user = null;
    expect(s.roleLabel).toBe('');
  });

  it('refreshes avatar when user has avatar', () => {
    s.user = { userId: 1, hasAvatar: true } as unknown as AuthResponse;
    s.ngOnChanges({ user: {} as any });
    expect(profile.loadAvatar).toHaveBeenCalledWith(1);
    expect(s.avatarObjectUrl()).toBe('blob:abc');
  });

  it('clears avatar when user has none', () => {
    s.user = { userId: 1, hasAvatar: false } as unknown as AuthResponse;
    s.ngOnChanges({ user: {} as any });
    expect(profile.loadAvatar).not.toHaveBeenCalled();
  });

  it('handles avatar load error gracefully', () => {
    profile.loadAvatar.and.returnValue(throwError(() => new Error('boom')));
    s.user = { userId: 2, hasAvatar: true } as unknown as AuthResponse;
    s.ngOnChanges({ user: {} as any });
    expect(s.avatarObjectUrl()).toBeNull();
  });

  it('revokes avatar on destroy', () => {
    s.user = { userId: 1, hasAvatar: true } as unknown as AuthResponse;
    s.ngOnChanges({ user: {} as any });
    fixture.destroy();
    expect(URL.revokeObjectURL).toHaveBeenCalled();
  });
});

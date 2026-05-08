import { TestBed } from '@angular/core/testing';
import { Main } from './main';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';
import { AuthResponse } from '../../models/auth.model';

describe('Main', () => {
  let auth: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['getUser', 'updateStoredUser', 'logout', 'isSuperAdmin'], { });
    auth.getUser.and.returnValue({ userId: 1, fullName: 'John Doe', email: 'a@b.c' } as unknown as AuthResponse);
    auth.isSuperAdmin.and.returnValue(false);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    await TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();
  });

  function build(): Main {
    return TestBed.runInInjectionContext(() => new Main());
  }

  it('computes initials from fullName', () => {
    const m = build();
    expect(m.userInitials).toBe('JD');
  });

  it('returns empty string initials when no user', () => {
    auth.getUser.and.returnValue(null);
    const m = build();
    expect(m.userInitials).toBe('');
  });

  it('toggleSidebar flips sidebarOpen', () => {
    const m = build();
    expect(m.sidebarOpen).toBeFalse();
    m.toggleSidebar();
    expect(m.sidebarOpen).toBeTrue();
    m.closeSidebar();
    expect(m.sidebarOpen).toBeFalse();
  });

  it('open/close profile and contact modals', () => {
    const m = build();
    m.openProfile();
    expect(m.profileModalOpen()).toBeTrue();
    m.closeProfile();
    expect(m.profileModalOpen()).toBeFalse();
    m.openContact();
    expect(m.contactModalOpen()).toBeTrue();
    m.closeContact();
    expect(m.contactModalOpen()).toBeFalse();
  });

  it('onProfileUpdated stores updated user and updates signal', () => {
    const m = build();
    const updated = { userId: 9, fullName: 'X Y' } as unknown as AuthResponse;
    m.onProfileUpdated(updated);
    expect(auth.updateStoredUser).toHaveBeenCalledWith(updated);
    expect(m.currentUser()?.userId).toBe(9);
  });

  it('logout clears session and navigates home', () => {
    const m = build();
    m.logout();
    expect(auth.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });

  it('user getter returns the current user', () => {
    const m = build();
    expect(m.user?.fullName).toBe('John Doe');
  });
});

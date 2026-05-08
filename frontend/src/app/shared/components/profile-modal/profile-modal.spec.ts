import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { ProfileModal } from './profile-modal';
import { ProfileService } from '../../../services/profile.service';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';
import { LUCIDE_TEST_PROVIDERS } from '../../../../testing/lucide-test.providers';
import { AuthResponse } from '../../../models/auth.model';

describe('ProfileModal', () => {
  let fixture: ComponentFixture<ProfileModal>;
  let m: ProfileModal;
  let profile: jasmine.SpyObj<ProfileService>;
  let auth: jasmine.SpyObj<AuthService>;
  let toast: jasmine.SpyObj<ToastService>;
  let router: jasmine.SpyObj<Router>;

  const baseUser: AuthResponse = {
    userId: 1, fullName: 'John Doe', email: 'john@example.com', hasAvatar: false,
  } as unknown as AuthResponse;

  beforeEach(async () => {
    profile = jasmine.createSpyObj<ProfileService>('ProfileService', [
      'updateProfile', 'changePassword', 'uploadAvatar', 'deleteAvatar', 'deleteAccount', 'loadAvatar',
    ]);
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['logout']);
    toast = jasmine.createSpyObj<ToastService>('ToastService', ['warning', 'success', 'fromHttpError']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    spyOn(URL, 'createObjectURL').and.returnValue('blob:preview');
    spyOn(URL, 'revokeObjectURL').and.callFake(() => undefined);

    await TestBed.configureTestingModule({
      imports: [ProfileModal],
      providers: [
        LUCIDE_TEST_PROVIDERS,
        { provide: ProfileService, useValue: profile },
        { provide: AuthService, useValue: auth },
        { provide: ToastService, useValue: toast },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileModal);
    m = fixture.componentInstance;
    m.show = true;
    m.user = { ...baseUser };
    profile.loadAvatar.and.returnValue(of('blob:stored'));
  });

  it('initials computed from full name', () => {
    expect(m.initials()).toBe('JD');
  });

  it('initials are empty when no full name', () => {
    fixture.destroy();
    fixture = TestBed.createComponent(ProfileModal);
    fixture.componentInstance.user = { fullName: '' } as AuthResponse;
    expect(fixture.componentInstance.initials()).toBe('');
  });

  it('avatarSrc prefers preview over stored', () => {
    expect(m.avatarSrc()).toBeNull();
    m.storedAvatarUrl.set('stored');
    expect(m.avatarSrc()).toBe('stored');
    m.previewUrl.set('preview');
    expect(m.avatarSrc()).toBe('preview');
  });

  it('ngOnChanges resets forms when shown', () => {
    m.ngOnChanges({ show: {} as any, user: {} as any });
    expect(m.profileForm.controls['fullName'].value).toBe('John Doe');
    expect(m.profileForm.controls['email'].value).toBe('john@example.com');
  });

  it('ngOnChanges loads stored avatar when user has avatar', () => {
    m.user = { ...baseUser, hasAvatar: true };
    m.ngOnChanges({ user: {} as any });
    expect(profile.loadAvatar).toHaveBeenCalledWith(1);
    expect(m.storedAvatarUrl()).toBe('blob:stored');
  });

  it('ngOnChanges handles avatar load error', () => {
    profile.loadAvatar.and.returnValue(throwError(() => new Error('boom')));
    m.user = { ...baseUser, hasAvatar: true };
    m.ngOnChanges({ user: {} as any });
    expect(m.storedAvatarUrl()).toBeNull();
  });

  it('hidden modal clears preview and stored avatar', () => {
    m.previewUrl.set('preview');
    m.storedAvatarUrl.set('stored');
    m.show = false;
    m.ngOnChanges({ show: {} as any });
    expect(m.previewUrl()).toBeNull();
    expect(m.storedAvatarUrl()).toBeNull();
  });

  it('onCancel emits closed', () => {
    let closed = 0;
    m.closed.subscribe(() => closed++);
    m.onCancel();
    expect(closed).toBe(1);
  });

  it('onSaveProfile warns when invalid', () => {
    m.profileForm.reset({ fullName: '', email: '' });
    m.onSaveProfile();
    expect(toast.warning).toHaveBeenCalled();
  });

  it('onSaveProfile sends sanitized payload', () => {
    profile.updateProfile.and.returnValue(of({ fullName: 'New', email: 'new@x.com', hasAvatar: false } as any));
    m.profileForm.setValue({ fullName: 'New Name', email: 'NEW@X.COM' });
    let updated: AuthResponse | undefined;
    m.updated.subscribe((u) => (updated = u));
    m.onSaveProfile();
    expect(profile.updateProfile).toHaveBeenCalled();
    expect(toast.success).toHaveBeenCalled();
    expect(updated?.email).toBe('new@x.com');
    expect(m.profileSaving()).toBeFalse();
  });

  it('onSaveProfile handles error', () => {
    profile.updateProfile.and.returnValue(throwError(() => ({ status: 500 })));
    m.profileForm.setValue({ fullName: 'New Name', email: 'new@x.com' });
    m.onSaveProfile();
    expect(toast.fromHttpError).toHaveBeenCalled();
    expect(m.profileSaving()).toBeFalse();
  });

  it('onSaveProfile is no-op while saving', () => {
    m.profileSaving.set(true);
    m.profileForm.setValue({ fullName: 'New Name', email: 'new@x.com' });
    m.onSaveProfile();
    expect(profile.updateProfile).not.toHaveBeenCalled();
  });

  it('onSavePassword warns when invalid', () => {
    m.onSavePassword();
    expect(toast.warning).toHaveBeenCalled();
  });

  it('onSavePassword sends payload', () => {
    profile.changePassword.and.returnValue(of(void 0 as any));
    m.passwordForm.setValue({ currentPassword: 'OldPass1!', newPassword: 'NewPass1!', confirmPassword: 'NewPass1!' });
    m.onSavePassword();
    expect(profile.changePassword).toHaveBeenCalled();
    expect(toast.success).toHaveBeenCalled();
  });

  it('onSavePassword reports errors', () => {
    profile.changePassword.and.returnValue(throwError(() => ({ status: 401 })));
    m.passwordForm.setValue({ currentPassword: 'OldPass1!', newPassword: 'NewPass1!', confirmPassword: 'NewPass1!' });
    m.onSavePassword();
    expect(toast.fromHttpError).toHaveBeenCalled();
  });

  it('onSavePassword is no-op while saving', () => {
    m.passwordSaving.set(true);
    m.passwordForm.setValue({ currentPassword: 'OldPass1!', newPassword: 'NewPass1!', confirmPassword: 'NewPass1!' });
    m.onSavePassword();
    expect(profile.changePassword).not.toHaveBeenCalled();
  });

  it('drag handlers update dragOver flag', () => {
    const e = new DragEvent('dragover');
    m.onDragOver(e);
    expect(m.dragOver()).toBeTrue();
    m.onDragLeave(new DragEvent('dragleave'));
    expect(m.dragOver()).toBeFalse();
  });

  function makeFile(type: string, sizeBytes: number, name = 'a.png'): File {
    const blob = new Blob([new Uint8Array(sizeBytes)], { type });
    return new File([blob], name, { type });
  }

  it('onDrop with valid file stages preview', () => {
    const file = makeFile('image/png', 100);
    const dt = new DataTransfer();
    dt.items.add(file);
    const e = new DragEvent('drop');
    Object.defineProperty(e, 'dataTransfer', { value: dt, configurable: true });
    m.onDrop(e);
    expect(m.previewUrl()).toBe('blob:preview');
    expect(m.hasPendingFile()).toBeTrue();
  });

  it('onDrop without file does nothing', () => {
    const e = new DragEvent('drop');
    Object.defineProperty(e, 'dataTransfer', { value: null });
    m.onDrop(e);
    expect(m.previewUrl()).toBeNull();
  });

  it('handleFile rejects invalid type', () => {
    const e = new DragEvent('drop');
    const dt = new DataTransfer();
    dt.items.add(makeFile('text/plain', 10));
    Object.defineProperty(e, 'dataTransfer', { value: dt });
    m.onDrop(e);
    expect(toast.warning).toHaveBeenCalled();
    expect(m.previewUrl()).toBeNull();
  });

  it('handleFile rejects oversized files', () => {
    const e = new DragEvent('drop');
    const dt = new DataTransfer();
    dt.items.add(makeFile('image/png', 3 * 1024 * 1024));
    Object.defineProperty(e, 'dataTransfer', { value: dt });
    m.onDrop(e);
    expect(toast.warning).toHaveBeenCalled();
    expect(m.previewUrl()).toBeNull();
  });

  it('onFileSelected stages preview from input', () => {
    const file = makeFile('image/jpeg', 100, 'b.jpg');
    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: [file] });
    const e = { target: input } as unknown as Event;
    m.onFileSelected(e);
    expect(m.previewUrl()).toBe('blob:preview');
  });

  it('onFileSelected ignores empty selection', () => {
    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: [] });
    const e = { target: input } as unknown as Event;
    m.onFileSelected(e);
    expect(m.previewUrl()).toBeNull();
  });

  it('onConfirmAvatar uploads pending file', () => {
    profile.uploadAvatar.and.returnValue(of({ hasAvatar: true } as any));
    const dt = new DataTransfer();
    dt.items.add(makeFile('image/png', 100));
    const e = new DragEvent('drop');
    Object.defineProperty(e, 'dataTransfer', { value: dt });
    m.onDrop(e);
    m.onConfirmAvatar();
    expect(profile.uploadAvatar).toHaveBeenCalled();
    expect(toast.success).toHaveBeenCalled();
  });

  it('onConfirmAvatar handles error', () => {
    profile.uploadAvatar.and.returnValue(throwError(() => ({ status: 413 })));
    const dt = new DataTransfer();
    dt.items.add(makeFile('image/png', 100));
    const e = new DragEvent('drop');
    Object.defineProperty(e, 'dataTransfer', { value: dt });
    m.onDrop(e);
    m.onConfirmAvatar();
    expect(toast.fromHttpError).toHaveBeenCalled();
  });

  it('onConfirmAvatar no-op without pending file', () => {
    m.onConfirmAvatar();
    expect(profile.uploadAvatar).not.toHaveBeenCalled();
  });

  it('onDiscardAvatar clears preview', () => {
    m.previewUrl.set('x');
    m.onDiscardAvatar();
    expect(m.previewUrl()).toBeNull();
  });

  it('onDeleteAvatar removes existing avatar', () => {
    profile.deleteAvatar.and.returnValue(of(void 0 as any));
    m.user = { ...baseUser, hasAvatar: true };
    let updates: AuthResponse[] = [];
    m.updated.subscribe((u) => updates.push(u));
    m.onDeleteAvatar();
    expect(profile.deleteAvatar).toHaveBeenCalled();
    expect(updates[0]?.hasAvatar).toBeFalse();
  });

  it('onDeleteAvatar handles error', () => {
    profile.deleteAvatar.and.returnValue(throwError(() => ({ status: 500 })));
    m.user = { ...baseUser, hasAvatar: true };
    m.onDeleteAvatar();
    expect(toast.fromHttpError).toHaveBeenCalled();
  });

  it('onDeleteAvatar no-op without avatar', () => {
    m.user = { ...baseUser, hasAvatar: false };
    m.onDeleteAvatar();
    expect(profile.deleteAvatar).not.toHaveBeenCalled();
  });

  it('account deletion flow', () => {
    profile.deleteAccount.and.returnValue(of(void 0 as any));
    let closed = 0;
    m.closed.subscribe(() => closed++);
    m.onRequestDeleteAccount();
    expect(m.showDeleteAccountConfirm()).toBeTrue();
    m.onCancelDeleteAccount();
    expect(m.showDeleteAccountConfirm()).toBeFalse();
    m.onRequestDeleteAccount();
    m.onConfirmDeleteAccount();
    expect(profile.deleteAccount).toHaveBeenCalled();
    expect(auth.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/']);
    expect(closed).toBe(1);
  });

  it('account deletion error', () => {
    profile.deleteAccount.and.returnValue(throwError(() => ({ status: 500 })));
    m.onRequestDeleteAccount();
    m.onConfirmDeleteAccount();
    expect(toast.fromHttpError).toHaveBeenCalled();
    expect(m.accountDeleting()).toBeFalse();
  });

  it('account deletion no-op while deleting', () => {
    m.accountDeleting.set(true);
    m.onRequestDeleteAccount();
    expect(m.showDeleteAccountConfirm()).toBeFalse();
    m.onConfirmDeleteAccount();
    expect(profile.deleteAccount).not.toHaveBeenCalled();
  });

  it('controlInvalid reflects touched + invalid state', () => {
    const c = m.profileForm.controls['fullName'];
    c.setValue('');
    c.markAsTouched();
    expect(m.controlInvalid(m.profileForm, 'fullName')).toBeTrue();
    expect(m.controlInvalid(m.profileForm, 'unknown')).toBeFalse();
  });

  it('controlError returns localized messages for each error key', () => {
    const c = m.profileForm.controls['fullName'];
    c.setErrors({ required: true });
    c.markAsTouched();
    expect(m.controlError(m.profileForm, 'fullName')).toContain('obligatorio');
    c.setErrors({ email: true });
    expect(m.controlError(m.profileForm, 'fullName')).toContain('correo');
    c.setErrors({ minlength: true });
    expect(m.controlError(m.profileForm, 'fullName')).toBeTruthy();
    c.setErrors({ maxlength: true });
    expect(m.controlError(m.profileForm, 'fullName')).toBeTruthy();
    c.setErrors({ pattern: true });
    expect(m.controlError(m.profileForm, 'fullName')).toBeTruthy();
    c.setErrors({ noHtml: true });
    expect(m.controlError(m.profileForm, 'fullName')).toBeTruthy();
    c.setErrors({ tooShort: true });
    expect(m.controlError(m.profileForm, 'fullName')).toBeTruthy();
    c.setErrors({ tooLong: true });
    expect(m.controlError(m.profileForm, 'fullName')).toBeTruthy();
    c.setErrors({ noUppercase: true });
    expect(m.controlError(m.profileForm, 'fullName')).toBeTruthy();
    c.setErrors({ noLowercase: true });
    expect(m.controlError(m.profileForm, 'fullName')).toBeTruthy();
    c.setErrors({ noDigit: true });
    expect(m.controlError(m.profileForm, 'fullName')).toBeTruthy();
    c.setErrors({ noSpecial: true });
    expect(m.controlError(m.profileForm, 'fullName')).toBeTruthy();
    c.setErrors({ invalidChars: true });
    expect(m.controlError(m.profileForm, 'fullName')).toBeTruthy();
    c.setErrors({ unknownKey: true });
    expect(m.controlError(m.profileForm, 'fullName')).toBeTruthy();
    c.setErrors(null);
    expect(m.controlError(m.profileForm, 'fullName')).toBe('');
    expect(m.controlError(m.profileForm, 'unknown')).toBe('');
  });

  it('passwordForm validators (mismatch and same as current)', () => {
    m.passwordForm.setValue({ currentPassword: 'AAA', newPassword: 'BBB', confirmPassword: 'CCC' });
    expect(m.formError(m.passwordForm, 'passwordMismatch')).toBeTrue();
    m.passwordForm.setValue({ currentPassword: 'AAA', newPassword: 'AAA', confirmPassword: 'AAA' });
    expect(m.formError(m.passwordForm, 'sameAsCurrent')).toBeTrue();
  });
});

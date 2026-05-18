import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ContactModal } from './contact-modal';
import { AdminChangeRequestService } from '../../../services/admin-change-request.service';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';
import { LUCIDE_TEST_PROVIDERS } from '../../../../testing/lucide-test.providers';

describe('ContactModal', () => {
  let fixture: ComponentFixture<ContactModal>;
  let m: ContactModal;
  let req: jasmine.SpyObj<AdminChangeRequestService>;
  let auth: jasmine.SpyObj<AuthService>;
  let toast: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    req = jasmine.createSpyObj<AdminChangeRequestService>('AdminChangeRequestService', ['create']);
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['getUser']);
    auth.getUser.and.returnValue({ hermandadNombre: 'Hermandad X' } as any);
    toast = jasmine.createSpyObj<ToastService>('ToastService', ['warning', 'success', 'fromHttpError']);

    await TestBed.configureTestingModule({
      imports: [ContactModal],
      providers: [
        LUCIDE_TEST_PROVIDERS,
        { provide: AdminChangeRequestService, useValue: req },
        { provide: AuthService, useValue: auth },
        { provide: ToastService, useValue: toast },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ContactModal);
    m = fixture.componentInstance;
  });

  it('hermandadName comes from current user', () => {
    expect(m.hermandadName).toBe('Hermandad X');
    auth.getUser.and.returnValue(null);
    expect(m.hermandadName).toBe('');
  });

  it('remainingChars is 2000 for empty input', () => {
    expect(m.remainingChars).toBe(2000);
    m.form.controls['message'].setValue('hello');
    expect(m.remainingChars).toBe(1995);
  });

  it('hasError and getError delegate to utils', () => {
    m.form.controls['message'].markAsTouched();
    expect(m.hasError('message')).toBeTrue();
    expect(m.getError('message').length).toBeGreaterThan(0);
  });

  it('onClose resets and emits when not submitting', () => {
    let closed = 0;
    m.closed.subscribe(() => closed++);
    m.form.controls['message'].setValue('hi there friend');
    m.onClose();
    expect(closed).toBe(1);
    expect(m.form.controls['message'].value).toBe('');
  });

  it('onClose is a no-op while submitting', () => {
    let closed = 0;
    m.closed.subscribe(() => closed++);
    m.submitting = true;
    m.onClose();
    expect(closed).toBe(0);
  });

  it('onSubmit warns when invalid', () => {
    m.onSubmit();
    expect(toast.warning).toHaveBeenCalled();
    expect(req.create).not.toHaveBeenCalled();
  });

  it('onSubmit posts payload and closes on success', () => {
    req.create.and.returnValue(of({} as any));
    let closed = 0;
    m.closed.subscribe(() => closed++);
    m.form.controls['message'].setValue('this is a valid message text');
    m.onSubmit();
    expect(req.create).toHaveBeenCalled();
    const arg = req.create.calls.mostRecent().args[0] as { type: string; message: string };
    expect(arg.type).toBe('ADMIN_CHANGE');
    expect(arg.message.startsWith('[Cambio de administrador]')).toBeTrue();
    expect(toast.success).toHaveBeenCalled();
    expect(closed).toBe(1);
    expect(m.submitting).toBeFalse();
  });

  it('selectCategory updates the prefix used on submit', () => {
    req.create.and.returnValue(of({} as any));
    m.selectCategory('VERIFICATION');
    m.form.controls['message'].setValue('this is a valid message text');
    m.onSubmit();
    const arg = req.create.calls.mostRecent().args[0] as { type: string; message: string };
    expect(arg.type).toBe('VERIFICATION');
    expect(arg.message.startsWith('[Verificación manual]')).toBeTrue();
  });

  it('onSubmit reports http errors', () => {
    req.create.and.returnValue(throwError(() => ({ status: 500 })));
    m.form.controls['message'].setValue('this is a valid message text');
    m.onSubmit();
    expect(toast.fromHttpError).toHaveBeenCalled();
    expect(m.submitting).toBeFalse();
  });

  it('onSubmit is idempotent while submitting', () => {
    m.submitting = true;
    m.form.controls['message'].setValue('this is a valid message text');
    m.onSubmit();
    expect(req.create).not.toHaveBeenCalled();
  });
});

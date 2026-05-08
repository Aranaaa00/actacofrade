import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EditUserModal } from './edit-user-modal';
import { LUCIDE_TEST_PROVIDERS } from '../../../../testing/lucide-test.providers';
import { UserResponse } from '../../../models/user.model';

describe('EditUserModal', () => {
  let fixture: ComponentFixture<EditUserModal>;
  let m: EditUserModal;
  let confirmed: string[];
  let cancelled: number;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [EditUserModal], providers: [LUCIDE_TEST_PROVIDERS] }).compileComponents();
    fixture = TestBed.createComponent(EditUserModal);
    m = fixture.componentInstance;
    confirmed = [];
    cancelled = 0;
    m.confirmed.subscribe((r) => confirmed.push(r));
    m.cancelled.subscribe(() => cancelled++);
  });

  it('initialises selectedRole from user.roles when user changes', () => {
    m.user = { roles: ['ROLE_ADMIN'] } as unknown as UserResponse;
    m.ngOnChanges({ user: {} as any });
    expect(m.selectedRole).toBe('ROLE_ADMIN');
  });

  it('falls back to empty string when user has no roles', () => {
    m.user = { roles: [] } as unknown as UserResponse;
    m.ngOnChanges({ user: {} as any });
    expect(m.selectedRole).toBe('');
    m.user = null;
    m.ngOnChanges({ user: {} as any });
    expect(m.selectedRole).toBe('');
  });

  it('reacts to show changes', () => {
    m.user = { roles: ['R'] } as unknown as UserResponse;
    m.ngOnChanges({ show: {} as any });
    expect(m.selectedRole).toBe('R');
  });

  it('cancel emits cancelled', () => {
    m.onCancel();
    expect(cancelled).toBe(1);
  });

  it('confirm emits selectedRole only when set', () => {
    m.selectedRole = '';
    m.onConfirm();
    expect(confirmed).toEqual([]);
    m.selectedRole = 'X';
    m.onConfirm();
    expect(confirmed).toEqual(['X']);
  });
});

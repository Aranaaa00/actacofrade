import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RejectModal } from './reject-modal';
import { LUCIDE_TEST_PROVIDERS } from '../../../../testing/lucide-test.providers';

describe('RejectModal', () => {
  let fixture: ComponentFixture<RejectModal>;
  let m: RejectModal;
  let confirmed: string[];
  let cancelled: number;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [RejectModal], providers: [LUCIDE_TEST_PROVIDERS] }).compileComponents();
    fixture = TestBed.createComponent(RejectModal);
    m = fixture.componentInstance;
    confirmed = [];
    cancelled = 0;
    m.confirmed.subscribe((r) => confirmed.push(r));
    m.cancelled.subscribe(() => cancelled++);
  });

  it('cancel resets state and emits cancelled', () => {
    m.reason = 'x';
    m.submitted = true;
    m.onCancel();
    expect(cancelled).toBe(1);
    expect(m.reason).toBe('');
    expect(m.submitted).toBeFalse();
  });

  it('confirm without reason marks submitted but does not emit', () => {
    m.reason = '   ';
    m.onConfirm();
    expect(confirmed).toEqual([]);
    expect(m.submitted).toBeTrue();
  });

  it('confirm with reason emits the reason and resets state', () => {
    m.reason = 'because';
    m.onConfirm();
    expect(confirmed).toEqual(['because']);
    expect(m.reason).toBe('');
    expect(m.submitted).toBeFalse();
  });
});

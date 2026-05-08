import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConfirmDialog } from './confirm-dialog';
import { LUCIDE_TEST_PROVIDERS } from '../../../../testing/lucide-test.providers';

@Component({
  standalone: true,
  imports: [ConfirmDialog],
  template: `<app-confirm-dialog></app-confirm-dialog>`,
})
class HostComponent {
  @ViewChild(ConfirmDialog, { static: true }) dialog!: ConfirmDialog;
}

describe('ConfirmDialog', () => {
  let fixture: ComponentFixture<HostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [HostComponent], providers: [LUCIDE_TEST_PROVIDERS] }).compileComponents();
    fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
  });

  it('exposes default texts and danger variant', () => {
    const d = fixture.componentInstance.dialog;
    expect(d.title.length).toBeGreaterThan(0);
    expect(d.message.length).toBeGreaterThan(0);
    expect(d.confirmLabel.length).toBeGreaterThan(0);
    expect(d.cancelLabel.length).toBeGreaterThan(0);
    expect(d.variant).toBe('danger');
  });

  it('emits confirmed and cancelled on subscription', () => {
    const d = fixture.componentInstance.dialog;
    let confirmed = 0;
    let cancelled = 0;
    d.confirmed.subscribe(() => confirmed++);
    d.cancelled.subscribe(() => cancelled++);
    d.confirmed.emit();
    d.cancelled.emit();
    expect(confirmed).toBe(1);
    expect(cancelled).toBe(1);
  });
});

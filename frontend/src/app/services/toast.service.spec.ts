import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ToastService } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [ToastService] });
    service = TestBed.inject(ToastService);
  });

  afterEach(() => {
    service.clear();
  });

  it('publishes success/info/warning/error toasts', () => {
    service.success('s');
    service.info('i');
    service.warning('w');
    service.error('e');
    expect(service.toasts().length).toBe(4);
    expect(service.toasts().map((t) => t.tone)).toEqual(['success', 'info', 'warning', 'error']);
  });

  it('ignores empty messages', () => {
    service.success('   ');
    expect(service.toasts().length).toBe(0);
  });

  it('truncates long messages', () => {
    service.info('a'.repeat(300));
    expect(service.toasts()[0].message.endsWith('…')).toBe(true);
    expect(service.toasts()[0].message.length).toBeLessThanOrEqual(240);
  });

  it('auto-dismisses after timer expires', fakeAsync(() => {
    service.success('hola');
    expect(service.toasts().length).toBe(1);
    tick(4000);
    tick(320);
    expect(service.toasts().length).toBe(0);
  }));

  it('manually dismiss schedules removal animation', fakeAsync(() => {
    service.error('boom');
    const id = service.toasts()[0].id;
    service.dismiss(id);
    expect(service.toasts()[0].leaving).toBe(true);
    tick(320);
    expect(service.toasts().length).toBe(0);
  }));

  it('clear empties toasts', () => {
    service.info('x');
    service.clear();
    expect(service.toasts().length).toBe(0);
  });

  it('fromHttpError maps HttpErrorResponse', () => {
    service.fromHttpError(new HttpErrorResponse({ status: 500 }), 'fb');
    expect(service.toasts()[0].tone).toBe('error');
  });

  it('fromHttpErrorSilencingAuth ignores 401', () => {
    service.fromHttpErrorSilencingAuth(new HttpErrorResponse({ status: 401 }), 'fb');
    expect(service.toasts().length).toBe(0);
    service.fromHttpErrorSilencingAuth(new HttpErrorResponse({ status: 500 }), 'fb');
    expect(service.toasts().length).toBe(1);
  });

  it('ignores non-string messages defensively', () => {
    service.success(undefined as unknown as string);
    expect(service.toasts().length).toBe(0);
  });

  it('dismiss clears existing timer before scheduling removal', fakeAsync(() => {
    service.info('x');
    const id = service.toasts()[0].id;
    service.dismiss(id);
    tick(320);
    expect(service.toasts().length).toBe(0);
  }));
});

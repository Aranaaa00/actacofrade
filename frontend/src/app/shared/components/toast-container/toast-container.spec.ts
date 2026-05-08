import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ToastContainer } from './toast-container';
import { ToastService } from '../../../services/toast.service';

describe('ToastContainer', () => {
  let fixture: ComponentFixture<ToastContainer>;
  let component: ToastContainer;
  let svc: ToastService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ToastContainer] }).compileComponents();
    fixture = TestBed.createComponent(ToastContainer);
    component = fixture.componentInstance;
    svc = TestBed.inject(ToastService);
  });

  it('exposes the service toasts signal', () => {
    expect(component.toasts).toBe(svc.toasts);
  });

  it('forwards dismiss to the service', () => {
    spyOn(svc, 'dismiss');
    component.dismiss(42);
    expect(svc.dismiss).toHaveBeenCalledWith(42);
  });

  it('returns alert + assertive only for error tone', () => {
    expect(component.ariaRole('error')).toBe('alert');
    expect(component.ariaRole('success')).toBe('status');
    expect(component.ariaLive('error')).toBe('assertive');
    expect(component.ariaLive('warning')).toBe('polite');
  });
});

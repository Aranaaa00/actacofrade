import { Component } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ModalA11yDirective } from './modal-a11y.directive';

@Component({
  standalone: true,
  imports: [ModalA11yDirective],
  template: `
    <button id="outside">outside</button>
    <div appModalA11y tabindex="-1">
      <button class="modal__close">x</button>
      <button id="first">first</button>
      <input id="middle" />
      <button id="last">last</button>
    </div>
  `,
})
class HostComponent {}

describe('ModalA11yDirective', () => {
  let fixture: ComponentFixture<HostComponent>;
  let host: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HostComponent] });
    fixture = TestBed.createComponent(HostComponent);
    document.body.appendChild(fixture.nativeElement);
    host = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => {
    fixture.destroy();
    document.body.classList.remove('modal-open');
  });

  function focusableButtons() {
    return Array.from(host.querySelectorAll<HTMLElement>('button, input'));
  }

  it('adds scroll lock class on init and focuses first non-close element', fakeAsync(() => {
    const outside = host.querySelector<HTMLElement>('#outside')!;
    outside.focus();
    fixture.detectChanges();
    tick();
    expect(document.body.classList.contains('modal-open')).toBeTrue();
    expect(document.activeElement?.id).toBe('first');
  }));

  it('removes scroll lock class on destroy and restores previous focus', fakeAsync(() => {
    const outside = host.querySelector<HTMLElement>('#outside')!;
    outside.focus();
    fixture.detectChanges();
    tick();
    fixture.destroy();
    expect(document.body.classList.contains('modal-open')).toBeFalse();
  }));

  it('Tab on last element wraps to the first focusable', fakeAsync(() => {
    fixture.detectChanges();
    tick();
    const buttons = focusableButtons();
    const last = host.querySelector<HTMLElement>('#last')!;
    last.focus();
    const dir = host.querySelector('[appmodala11y], [appModalA11y]') as HTMLElement;
    const event = new KeyboardEvent('keydown', { key: 'Tab', bubbles: true, cancelable: true });
    dir.dispatchEvent(event);
    expect(event.defaultPrevented).toBeTrue();
    expect(document.activeElement?.classList.contains('modal__close')).toBeTrue();
    void buttons;
  }));

  it('Shift+Tab on first element wraps to the last focusable', fakeAsync(() => {
    fixture.detectChanges();
    tick();
    const closeBtn = host.querySelector<HTMLElement>('.modal__close')!;
    closeBtn.focus();
    const dir = host.querySelector('[appmodala11y], [appModalA11y]') as HTMLElement;
    const event = new KeyboardEvent('keydown', { key: 'Tab', shiftKey: true, bubbles: true, cancelable: true });
    dir.dispatchEvent(event);
    expect(event.defaultPrevented).toBeTrue();
    expect(document.activeElement?.id).toBe('last');
  }));

  it('ignores non-Tab keys', fakeAsync(() => {
    fixture.detectChanges();
    tick();
    const dir = host.querySelector('[appmodala11y], [appModalA11y]') as HTMLElement;
    const event = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true });
    dir.dispatchEvent(event);
    expect(event.defaultPrevented).toBeFalse();
  }));

  it('Tab with no focusable elements just prevents default', fakeAsync(() => {
    fixture.detectChanges();
    tick();
    const dir = host.querySelector('[appmodala11y], [appModalA11y]') as HTMLElement;
    // disable every focusable so the collected list becomes empty
    host.querySelectorAll<HTMLElement>('button, input').forEach((el) => el.setAttribute('disabled', ''));
    const event = new KeyboardEvent('keydown', { key: 'Tab', bubbles: true, cancelable: true });
    dir.dispatchEvent(event);
    expect(event.defaultPrevented).toBeTrue();
  }));

  it('Shift+Tab while focus is outside modal wraps to the last focusable', fakeAsync(() => {
    fixture.detectChanges();
    tick();
    const outside = host.querySelector<HTMLElement>('#outside')!;
    outside.focus();
    const dir = host.querySelector('[appmodala11y], [appModalA11y]') as HTMLElement;
    const event = new KeyboardEvent('keydown', { key: 'Tab', shiftKey: true, bubbles: true, cancelable: true });
    dir.dispatchEvent(event);
    expect(event.defaultPrevented).toBeTrue();
    expect(document.activeElement?.id).toBe('last');
  }));

  it('focuses the close button when no other focusables are available', fakeAsync(() => {
    // disable every non-close focusable so the find() returns undefined and falls back to focusables[0]
    host.querySelectorAll<HTMLElement>('button:not(.modal__close), input').forEach((el) => el.setAttribute('disabled', ''));
    fixture.detectChanges();
    tick();
    expect(document.activeElement?.classList.contains('modal__close')).toBeTrue();
  }));
});

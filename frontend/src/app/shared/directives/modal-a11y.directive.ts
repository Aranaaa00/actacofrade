import { DOCUMENT } from '@angular/common';
import {
  AfterViewInit,
  Directive,
  ElementRef,
  HostListener,
  OnDestroy,
  OnInit,
  inject,
} from '@angular/core';

const FOCUSABLE_SELECTOR = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled]):not([type="hidden"])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(',');

const SCROLL_LOCK_CLASS = 'modal-open';
let openModalCount = 0;

@Directive({
  selector: '[appModalA11y]',
})
export class ModalA11yDirective implements OnInit, AfterViewInit, OnDestroy {
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly document = inject(DOCUMENT);
  private previouslyFocused: HTMLElement | null = null;

  ngOnInit(): void {
    this.previouslyFocused = this.document.activeElement as HTMLElement | null;
    openModalCount += 1;
    this.document.body.classList.add(SCROLL_LOCK_CLASS);
  }

  ngAfterViewInit(): void {
    queueMicrotask(() => this.focusFirst());
  }

  ngOnDestroy(): void {
    openModalCount = Math.max(0, openModalCount - 1);
    if (openModalCount === 0) {
      this.document.body.classList.remove(SCROLL_LOCK_CLASS);
    }
    this.previouslyFocused?.focus?.();
  }

  @HostListener('keydown', ['$event'])
  onKeydown(event: KeyboardEvent): void {
    if (event.key !== 'Tab') {
      return;
    }
    const focusables = this.collectFocusables();
    if (focusables.length === 0) {
      event.preventDefault();
      return;
    }
    const first = focusables[0];
    const last = focusables[focusables.length - 1];
    const active = this.document.activeElement as HTMLElement | null;
    const inside = this.host.nativeElement.contains(active);
    if (event.shiftKey && (active === first || !inside)) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && active === last) {
      event.preventDefault();
      first.focus();
    }
  }

  private focusFirst(): void {
    const focusables = this.collectFocusables();
    const target =
      focusables.find((el) => !el.classList.contains('modal__close')) ?? focusables[0];
    target?.focus();
  }

  private collectFocusables(): HTMLElement[] {
    const nodes = this.host.nativeElement.querySelectorAll(FOCUSABLE_SELECTOR);
    const result: HTMLElement[] = [];
    nodes.forEach((node: Element) => {
      const el = node as HTMLElement;
      if (!el.hasAttribute('disabled') && el.offsetParent !== null) {
        result.push(el);
      }
    });
    return result;
  }
}

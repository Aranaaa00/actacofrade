import { TestBed } from '@angular/core/testing';
import { Router, NavigationEnd, provideRouter } from '@angular/router';
import { Subject } from 'rxjs';
import { Component } from '@angular/core';
import { BrowserService, APP_ESCAPE_EVENT, APP_NAVIGATED_EVENT } from './browser.service';

@Component({ template: '' })
class DummyComponent {}

describe('BrowserService', () => {
  let service: BrowserService;
  let routerEvents$: Subject<any>;

  beforeEach(() => {
    routerEvents$ = new Subject();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: '**', component: DummyComponent }]),
        BrowserService,
      ],
    });
    const router = TestBed.inject(Router);
    Object.defineProperty(router, 'events', { get: () => routerEvents$.asObservable() });
    Object.defineProperty(router, 'routerState', {
      get: () => ({
        snapshot: { root: { firstChild: { firstChild: null, data: { description: 'd', title: 'T' } } } },
      }),
      configurable: true,
    });
    service = TestBed.inject(BrowserService);
  });

  it('init wires listeners and is idempotent', () => {
    service.init();
    service.init();
    expect((service as any).initialized).toBe(true);
  });

  it('updates document on navigation end', () => {
    service.init();
    let captured: any = null;
    document.addEventListener(APP_NAVIGATED_EVENT, (ev) => (captured = (ev as CustomEvent).detail));
    routerEvents$.next(new NavigationEnd(1, '/x', '/x'));
    expect(captured).toBeTruthy();
    expect(captured.url).toBe('/x');
  });

  it('exposes connection status observable and reacts to online/offline events', () => {
    service.init();
    const states: boolean[] = [];
    service.connectionStatus$.subscribe((v) => states.push(v));
    window.dispatchEvent(new Event('offline'));
    window.dispatchEvent(new Event('online'));
    expect(states).toContain(false);
    expect(states).toContain(true);
  });

  it('dispatches app:escape when Escape key pressed', () => {
    service.init();
    let fired = false;
    document.addEventListener(APP_ESCAPE_EVENT, () => (fired = true));
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    expect(fired).toBe(true);
  });

  it('ignores non-Escape keys', () => {
    service.init();
    let fired = false;
    document.addEventListener(APP_ESCAPE_EVENT, () => (fired = true));
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));
    expect(fired).toBe(false);
  });

  it('falls back to defaults when route data lacks title and description', () => {
    const router = TestBed.inject(Router);
    Object.defineProperty(router, 'routerState', {
      get: () => ({ snapshot: { root: { firstChild: { firstChild: null, data: {} } } } }),
      configurable: true,
    });
    service.init();
    let captured: any = null;
    document.addEventListener(APP_NAVIGATED_EVENT, (ev) => (captured = (ev as CustomEvent).detail));
    routerEvents$.next(new NavigationEnd(2, '/y', '/y'));
    expect(captured).toBeTruthy();
    expect(captured.title.length).toBeGreaterThan(0);
  });

  it('falls back to defaults when route data has empty strings', () => {
    const router = TestBed.inject(Router);
    Object.defineProperty(router, 'routerState', {
      get: () => ({
        snapshot: { root: { firstChild: { firstChild: null, data: { description: '', title: '' } } } },
      }),
      configurable: true,
    });
    service.init();
    let captured: any = null;
    document.addEventListener(APP_NAVIGATED_EVENT, (ev) => (captured = (ev as CustomEvent).detail));
    routerEvents$.next(new NavigationEnd(3, '/z', '/z'));
    expect(captured).toBeTruthy();
  });

  it('falls back to "es" when navigator language is empty', () => {
    const original = Object.getOwnPropertyDescriptor(window.navigator, 'language');
    Object.defineProperty(window.navigator, 'language', { value: '', configurable: true });
    try {
      service.init();
      routerEvents$.next(new NavigationEnd(4, '/w', '/w'));
      expect(document.documentElement.getAttribute('lang')).toBe('es');
    } finally {
      if (original) {
        Object.defineProperty(window.navigator, 'language', original);
      }
    }
  });
});

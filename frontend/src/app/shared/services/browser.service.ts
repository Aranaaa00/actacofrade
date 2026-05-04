import { DOCUMENT } from '@angular/common';
import { DestroyRef, Injectable, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { BehaviorSubject, Observable, filter } from 'rxjs';

// Custom event names dispatched on the document.
export const APP_NAVIGATED_EVENT = 'app:navigated';
export const APP_ESCAPE_EVENT = 'app:escape';

export interface AppNavigatedDetail {
  url: string;
  title: string;
}

// Centralizes browser-level side effects: title, scroll, online status, keyboard.
@Injectable({ providedIn: 'root' })
export class BrowserService {
  private readonly router = inject(Router);
  private readonly document = inject(DOCUMENT);
  private readonly destroyRef = inject(DestroyRef);

  private readonly online$ = new BehaviorSubject<boolean>(this.window.navigator.onLine);
  private readonly defaultTitle = 'ActaCofrade';
  private initialized = false;

  // Observable view of navigator.onLine, updated by browser events.
  get connectionStatus$(): Observable<boolean> {
    return this.online$.asObservable();
  }

  // Wires router, keyboard and connection listeners. Safe to call multiple times.
  init(): void {
    if (this.initialized) {
      return;
    }
    this.initialized = true;

    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd), takeUntilDestroyed(this.destroyRef))
      .subscribe((event) => this.onNavigationEnd(event.urlAfterRedirects));

    this.window.addEventListener('keydown', this.onKeyDown);
    this.window.addEventListener('online', this.onOnline);
    this.window.addEventListener('offline', this.onOffline);

    this.destroyRef.onDestroy(() => {
      this.window.removeEventListener('keydown', this.onKeyDown);
      this.window.removeEventListener('online', this.onOnline);
      this.window.removeEventListener('offline', this.onOffline);
    });
  }

  // Updates document.title, scrolls to top and broadcasts a custom event.
  private onNavigationEnd(url: string): void {
    const routeTitle = this.collectRouteTitle();
    const fullTitle = routeTitle ? `${routeTitle} – ${this.defaultTitle}` : this.defaultTitle;
    this.document.title = fullTitle;
    this.window.scrollTo({ top: 0, left: 0, behavior: 'smooth' });
    const event = new CustomEvent<AppNavigatedDetail>(APP_NAVIGATED_EVENT, {
      detail: { url, title: fullTitle },
      bubbles: false,
    });
    this.document.dispatchEvent(event);
  }

  // Walks the active route tree and returns the deepest title metadata.
  private collectRouteTitle(): string {
    let route = this.router.routerState.snapshot.root;
    let title = '';
    while (route.firstChild) {
      route = route.firstChild;
      const data = route.data['title'];
      if (typeof data === 'string' && data.length > 0) {
        title = data;
      }
    }
    return title;
  }

  // Forwards Escape keypresses as a custom DOM event for modal consumers.
  private readonly onKeyDown = (event: KeyboardEvent): void => {
    if (event.key === 'Escape') {
      this.document.dispatchEvent(new CustomEvent(APP_ESCAPE_EVENT));
    }
  };

  private readonly onOnline = (): void => {
    this.online$.next(true);
  };

  private readonly onOffline = (): void => {
    this.online$.next(false);
  };

  private get window(): Window {
    return this.document.defaultView as Window;
  }
}

import { DOCUMENT } from '@angular/common';
import { DestroyRef, Injectable, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Meta, Title } from '@angular/platform-browser';
import { NavigationEnd, Router } from '@angular/router';
import { BehaviorSubject, Observable, filter } from 'rxjs';

// Custom event names dispatched on the document.
export const APP_NAVIGATED_EVENT = 'app:navigated';
export const APP_ESCAPE_EVENT = 'app:escape';

export interface AppNavigatedDetail {
  url: string;
  title: string;
}

// Default description used when a route does not declare one.
const DEFAULT_DESCRIPTION =
  'ActaCofrade gestiona los actos de tu hermandad: tareas, decisiones e incidencias.';

// Centralizes browser-level side effects: meta description, scroll, online status, keyboard.
@Injectable({ providedIn: 'root' })
export class BrowserService {
  private readonly router = inject(Router);
  private readonly document = inject(DOCUMENT);
  private readonly destroyRef = inject(DestroyRef);
  private readonly meta = inject(Meta);
  private readonly titleService = inject(Title);

  private readonly online$ = new BehaviorSubject<boolean>(this.window.navigator.onLine);
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

  // Updates description meta, scrolls to top and broadcasts a custom event.
  private onNavigationEnd(url: string): void {
    const description = this.collectRouteDescription();
    this.meta.updateTag({ name: 'description', content: description });
    this.window.scrollTo({ top: 0, left: 0, behavior: 'smooth' });
    const event = new CustomEvent<AppNavigatedDetail>(APP_NAVIGATED_EVENT, {
      detail: { url, title: this.titleService.getTitle() },
      bubbles: false,
    });
    this.document.dispatchEvent(event);
  }

  // Walks the active route tree and returns the deepest description metadata.
  private collectRouteDescription(): string {
    let route = this.router.routerState.snapshot.root;
    let description = DEFAULT_DESCRIPTION;
    while (route.firstChild) {
      route = route.firstChild;
      const data = route.data['description'];
      if (typeof data === 'string' && data.length > 0) {
        description = data;
      }
    }
    return description;
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

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

// Base title used when a route does not declare a custom one.
const DEFAULT_TITLE = 'ActaCofrade';

// Id of the screen-reader live region created programmatically on init.
const LIVE_REGION_ID = 'app-live-region';

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
  // Reference to the visually-hidden announcer element built with createElement.
  private liveRegion: HTMLElement | null = null;

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

    this.liveRegion = this.createLiveRegion();

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
      if (this.liveRegion && this.liveRegion.parentNode) {
        this.liveRegion.parentNode.removeChild(this.liveRegion);
      }
    });
  }

  // Updates description meta, scrolls to top and broadcasts a custom event.
  private onNavigationEnd(url: string): void {
    const description = this.collectRouteDescription();
    const routeTitle = this.collectRouteTitle();
    this.meta.updateTag({ name: 'description', content: description });
    // Mutate the existing <title> element through the platform service.
    this.titleService.setTitle(routeTitle);
    // Mutate the existing <html lang="..."> attribute to reflect the navigator language.
    this.document.documentElement.setAttribute('lang', this.window.navigator.language.split('-')[0] || 'es');
    // Update the screen-reader announcer text node so assistive tech reads the new view.
    this.announce(routeTitle);
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

  // Walks the active route tree and returns the deepest title metadata.
  private collectRouteTitle(): string {
    let route = this.router.routerState.snapshot.root;
    let title = DEFAULT_TITLE;
    while (route.firstChild) {
      route = route.firstChild;
      const data = route.data['title'];
      if (typeof data === 'string' && data.length > 0) {
        title = `${data} · ${DEFAULT_TITLE}`;
      }
    }
    return title;
  }

  // Builds a visually-hidden ARIA live region by creating a real DOM element,
  // assigning a utility class from the global stylesheet and appending it to <body>.
  private createLiveRegion(): HTMLElement {
    const existing = this.document.getElementById(LIVE_REGION_ID);
    if (existing) {
      return existing;
    }
    const region = this.document.createElement('div');
    region.setAttribute('id', LIVE_REGION_ID);
    region.setAttribute('role', 'status');
    region.setAttribute('aria-live', 'polite');
    region.setAttribute('aria-atomic', 'true');
    // The class is defined in src/styles/06-utilities/_utilities.scss and applies
    // the visually-hidden mixin, so the announcer stays out of the visual layout
    // without leaking presentation rules into TypeScript.
    region.classList.add('screen-reader-only');
    this.document.body.appendChild(region);
    return region;
  }

  // Replaces the live region content so screen readers announce the new view.
  private announce(message: string): void {
    if (!this.liveRegion) {
      return;
    }
    this.liveRegion.textContent = '';
    // Defer the new text on a microtask so AT detect the mutation as a fresh announcement.
    this.window.queueMicrotask(() => {
      if (this.liveRegion) {
        this.liveRegion.textContent = message;
      }
    });
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

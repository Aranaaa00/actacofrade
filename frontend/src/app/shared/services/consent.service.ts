import { DOCUMENT } from '@angular/common';
import { Injectable, computed, inject, signal } from '@angular/core';

// Categories handled by the platform. `essential` covers auth/session/CSRF and
// is always active (LSSI 22.2 exemption). Optional categories stay OFF until
// the user explicitly opts in from the banner. The shape is forward-compatible:
// any new optional service should be added as a new key here.
export interface ConsentCategories {
  readonly essential: true;
  readonly analytics: boolean;
  readonly marketing: boolean;
}

export type ConsentCategory = keyof ConsentCategories;
export type OptionalCategory = Exclude<ConsentCategory, 'essential'>;
export type ConsentStatus = 'unset' | 'granted' | 'essential';

// Persisted shape. Versioned so future migrations can run safely.
interface StoredConsent {
  v: 1;
  decidedAt: string;
  categories: ConsentCategories;
}

const STORAGE_KEY = 'ac_cookie_consent_v1';
const LEGACY_KEY = 'ac_cookie_consent';
const CURRENT_VERSION = 1 as const;

const DEFAULT_CATEGORIES: ConsentCategories = {
  essential: true,
  analytics: false,
  marketing: false,
};

// Holds the user's decision and gates any optional behaviour. Components and
// services that depend on consent should either read the corresponding signal
// or queue work via `whenGranted`, which guarantees that nothing runs before
// the user has opted in.
@Injectable({ providedIn: 'root' })
export class ConsentService {
  private readonly document = inject(DOCUMENT);

  private readonly state = signal<StoredConsent | null>(this.load());

  // Queue of callbacks waiting for a specific category to be granted. They are
  // released once and never replayed, which avoids double-initialisation.
  private readonly pending = new Map<OptionalCategory, Array<() => void>>();

  readonly hasDecided = computed(() => this.state() !== null);

  readonly categories = computed<ConsentCategories>(
    () => this.state()?.categories ?? DEFAULT_CATEGORIES,
  );

  readonly status = computed<ConsentStatus>(() => {
    const current = this.state();
    if (!current) {
      return 'unset';
    }
    const { analytics, marketing } = current.categories;
    return analytics || marketing ? 'granted' : 'essential';
  });

  readonly analytics = computed(() => this.categories().analytics);
  readonly marketing = computed(() => this.categories().marketing);

  constructor() {
    // Cross-tab synchronisation: if the user updates consent in another tab,
    // reflect it here too so gated services stay coherent.
    const win = this.document.defaultView;
    if (win) {
      win.addEventListener('storage', (event) => {
        if (event.key === STORAGE_KEY) {
          this.state.set(this.load());
          this.flushPending();
        }
      });
    }
  }

  // User opted in to every optional category.
  acceptAll(): void {
    this.persist({ essential: true, analytics: true, marketing: true });
  }

  // User keeps only the strictly necessary storage. Optional categories stay
  // off and any pending callback for them is discarded.
  acceptEssentialOnly(): void {
    this.persist({ essential: true, analytics: false, marketing: false });
    this.pending.clear();
  }

  // Returns whether a category is currently allowed. Essential is always true.
  isAllowed(category: ConsentCategory): boolean {
    if (category === 'essential') {
      return true;
    }
    return Boolean(this.categories()[category]);
  }

  // Runs `callback` once the given optional category is granted. If it is
  // already granted, the callback is invoked synchronously. Otherwise it is
  // queued and released either when the user accepts that category or
  // discarded if the user rejects optional cookies.
  whenGranted(category: OptionalCategory, callback: () => void): void {
    if (this.isAllowed(category)) {
      callback();
      return;
    }
    const queue = this.pending.get(category) ?? [];
    queue.push(callback);
    this.pending.set(category, queue);
  }

  // Re-opens the banner. Used from the cookie policy page so the user can
  // change their mind without clearing browser data.
  reset(): void {
    this.pending.clear();
    try {
      localStorage.removeItem(STORAGE_KEY);
      localStorage.removeItem(LEGACY_KEY);
    } catch {
      // Storage may be blocked (private mode, quota): ignore.
    }
    this.state.set(null);
  }

  private persist(categories: ConsentCategories): void {
    const record: StoredConsent = {
      v: CURRENT_VERSION,
      decidedAt: new Date().toISOString(),
      categories,
    };
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(record));
      localStorage.removeItem(LEGACY_KEY);
    } catch {
      // Same private-mode guard as above; in-memory state still works.
    }
    this.state.set(record);
    this.flushPending();
  }

  private flushPending(): void {
    for (const [category, callbacks] of this.pending) {
      if (this.isAllowed(category)) {
        this.pending.delete(category);
        for (const fn of callbacks) {
          try {
            fn();
          } catch {
            // A failing consumer should not block the rest of the queue.
          }
        }
      }
    }
  }

  private load(): StoredConsent | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw) as Partial<StoredConsent> | null;
        if (parsed && parsed.v === CURRENT_VERSION && parsed.categories) {
          return {
            v: CURRENT_VERSION,
            decidedAt: typeof parsed.decidedAt === 'string' ? parsed.decidedAt : new Date().toISOString(),
            categories: {
              essential: true,
              analytics: Boolean(parsed.categories.analytics),
              marketing: Boolean(parsed.categories.marketing),
            },
          };
        }
      }
      // Migrate the previous boolean storage: legacy "accepted" turned every
      // optional category on, "rejected" kept only the essentials. Either
      // value counts as a real decision so the banner stays closed.
      const legacy = localStorage.getItem(LEGACY_KEY);
      if (legacy === 'accepted' || legacy === 'rejected') {
        const all = legacy === 'accepted';
        const migrated: StoredConsent = {
          v: CURRENT_VERSION,
          decidedAt: new Date().toISOString(),
          categories: { essential: true, analytics: all, marketing: all },
        };
        try {
          localStorage.setItem(STORAGE_KEY, JSON.stringify(migrated));
          localStorage.removeItem(LEGACY_KEY);
        } catch {
          // ignore
        }
        return migrated;
      }
      return null;
    } catch {
      return null;
    }
  }
}

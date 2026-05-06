import { Injectable, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { extractErrorMessage } from '../shared/utils/http-error.utils';

export type ToastTone = 'success' | 'error' | 'warning' | 'info';

export interface Toast {
  id: number;
  tone: ToastTone;
  message: string;
  leaving?: boolean;
}

const DEFAULT_DURATION_MS: Record<ToastTone, number> = {
  success: 4000,
  info: 4000,
  warning: 5500,
  error: 6500,
};

// Duration of the roll-back exit animation; must match the SCSS keyframe.
const LEAVING_DURATION_MS = 320;

// Centralized parchment toast publisher: the only feedback channel allowed in the app.
@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 1;
  private readonly timers = new Map<number, ReturnType<typeof setTimeout>>();
  private readonly _toasts = signal<Toast[]>([]);
  readonly toasts = this._toasts.asReadonly();

  success(message: string): void {
    this.push('success', message);
  }

  error(message: string): void {
    this.push('error', message);
  }

  warning(message: string): void {
    this.push('warning', message);
  }

  info(message: string): void {
    this.push('info', message);
  }

  // Maps a backend HttpErrorResponse to a safe user-facing error toast.
  fromHttpError(error: unknown, fallback: string): void {
    const message = extractErrorMessage(error, fallback);
    this.error(message);
  }

  // Same as fromHttpError but ignores 401: those are handled by the auth flow.
  fromHttpErrorSilencingAuth(error: unknown, fallback: string): void {
    if (error instanceof HttpErrorResponse && error.status === 401) {
      return;
    }
    this.fromHttpError(error, fallback);
  }

  dismiss(id: number): void {
    const timer = this.timers.get(id);
    if (timer) {
      clearTimeout(timer);
      this.timers.delete(id);
    }
    // Mark as leaving so the parchment plays the roll-back animation, then remove.
    this._toasts.update((list) => list.map((t) => (t.id === id ? { ...t, leaving: true } : t)));
    const removeTimer = setTimeout(() => {
      this._toasts.update((list) => list.filter((t) => t.id !== id));
      this.timers.delete(id);
    }, LEAVING_DURATION_MS);
    this.timers.set(id, removeTimer);
  }

  clear(): void {
    this.timers.forEach((t) => clearTimeout(t));
    this.timers.clear();
    this._toasts.set([]);
  }

  private push(tone: ToastTone, rawMessage: string): void {
    const message = this.sanitize(rawMessage);
    if (!message) {
      return;
    }
    const id = this.nextId++;
    this._toasts.update((list) => [...list, { id, tone, message }]);
    const timer = setTimeout(() => this.dismiss(id), DEFAULT_DURATION_MS[tone]);
    this.timers.set(id, timer);
  }

  // Trim and cap length so backend payloads cannot break the UI or leak large blobs.
  private sanitize(message: string): string {
    if (typeof message !== 'string') {
      return '';
    }
    const trimmed = message.trim();
    return trimmed.length > 240 ? `${trimmed.slice(0, 237)}…` : trimmed;
  }
}

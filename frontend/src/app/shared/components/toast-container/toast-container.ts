import { Component, inject } from '@angular/core';
import { ToastService } from '../../../services/toast.service';

@Component({
  selector: 'app-toast-container',
  templateUrl: './toast-container.html',
})
export class ToastContainer {
  private readonly toastService = inject(ToastService);
  readonly toasts = this.toastService.toasts;

  dismiss(id: number): void {
    this.toastService.dismiss(id);
  }

  // Errors must be announced assertively, the rest politely.
  ariaRole(tone: string): string {
    return tone === 'error' ? 'alert' : 'status';
  }

  ariaLive(tone: string): 'assertive' | 'polite' {
    return tone === 'error' ? 'assertive' : 'polite';
  }
}

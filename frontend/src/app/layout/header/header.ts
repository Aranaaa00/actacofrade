import { Component, DestroyRef, EventEmitter, Input, Output, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { distinctUntilChanged, pairwise, startWith } from 'rxjs';
import { Topbar } from '../../shared/components/topbar/topbar';
import { AuthResponse } from '../../models/auth.model';
import { BrowserService } from '../../shared/services/browser.service';
import { ToastService } from '../../services/toast.service';

// Top-level shell header. Shows topbar and broadcasts connection changes via toasts.
@Component({
  selector: 'app-header',
  imports: [Topbar],
  templateUrl: './header.html',
})
export class Header {
  @Input() sidebarOpen = false;
  @Input() user: AuthResponse | null = null;
  @Output() toggledSidebar = new EventEmitter<void>();

  private readonly toast = inject(ToastService);

  constructor() {
    inject(BrowserService).connectionStatus$
      .pipe(
        distinctUntilChanged(),
        startWith(true),
        pairwise(),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe(([previous, current]) => {
        if (previous && !current) {
          this.toast.warning('Sin conexión. Algunos cambios pueden no guardarse.');
        } else if (!previous && current) {
          this.toast.success('Conexión restablecida.');
        }
      });
  }
}

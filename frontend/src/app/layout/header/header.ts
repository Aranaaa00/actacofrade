import { AsyncPipe } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Topbar } from '../../shared/components/topbar/topbar';
import { Banner } from '../../shared/components/banner/banner';
import { AuthResponse } from '../../models/auth.model';
import { BrowserService } from '../../shared/services/browser.service';

// Top-level shell header. Shows topbar and a live connection indicator.
@Component({
  selector: 'app-header',
  imports: [Topbar, Banner, AsyncPipe],
  templateUrl: './header.html',
})
export class Header {
  @Input() sidebarOpen = false;
  @Input() user: AuthResponse | null = null;
  @Output() toggledSidebar = new EventEmitter<void>();

  // Stream consumed by the template via the async pipe.
  readonly online$: Observable<boolean> = inject(BrowserService).connectionStatus$;
}

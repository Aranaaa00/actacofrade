import { Component, Input, Output, EventEmitter } from '@angular/core';
import { Topbar } from '../../shared/components/topbar/topbar';
import { AuthResponse } from '../../models/auth.model';

@Component({
  selector: 'app-header',
  imports: [Topbar],
  templateUrl: './header.html',
})
export class Header {
  @Input() sidebarOpen = false;
  @Input() user: AuthResponse | null = null;
  @Output() toggledSidebar = new EventEmitter<void>();
}

import { Component, Input, Output, EventEmitter } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { AuthResponse } from '../../../models/auth.model';

@Component({
  selector: 'app-topbar',
  imports: [LucideAngularModule],
  templateUrl: './topbar.html',
})
export class Topbar {
  @Input() sidebarOpen = false;
  @Input() user: AuthResponse | null = null;
  @Output() toggledSidebar = new EventEmitter<void>();

  get formattedDate(): string {
    const now = new Date();
    return now.toLocaleDateString('es-ES', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    }).toUpperCase();
  }

  get todayIso(): string {
    return new Date().toISOString().slice(0, 10);
  }
}

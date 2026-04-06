import { Component, Input, Output, EventEmitter } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthResponse } from '../../../models/auth.model';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
})
export class Sidebar {
  @Input() open = false;
  @Input() user: AuthResponse | null = null;
  @Input() userInitials = '';
  @Output() closed = new EventEmitter<void>();
  @Output() loggedOut = new EventEmitter<void>();
}

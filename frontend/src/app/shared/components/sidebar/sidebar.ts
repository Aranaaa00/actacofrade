import { Component, Input, Output, EventEmitter, OnInit, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { AuthResponse } from '../../../models/auth.model';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive, LucideAngularModule],
  templateUrl: './sidebar.html',
})
export class Sidebar implements OnInit {
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);

  @Input() open = false;
  @Input() user: AuthResponse | null = null;
  @Input() userInitials = '';
  @Output() closed = new EventEmitter<void>();
  @Output() loggedOut = new EventEmitter<void>();

  actsMenuOpen = false;

  ngOnInit(): void {
    this.actsMenuOpen = this.router.url.startsWith('/events');
  }

  toggleActsMenu(): void {
    this.actsMenuOpen = !this.actsMenuOpen;
  }
}

import { Component, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { AuthResponse } from '../../models/auth.model';

@Component({
  selector: 'app-main',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './main.html',
  styleUrl: './main.scss',
})
export class Main {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  sidebarOpen = false;

  get user(): AuthResponse | null {
    return this.authService.getUser();
  }

  get userInitials(): string {
    const user = this.user;
    let initials = '';
    if (user?.fullName) {
      initials = user.fullName
        .split(' ')
        .map(n => n[0])
        .join('')
        .toUpperCase()
        .substring(0, 2);
    }
    return initials;
  }

  get formattedDate(): string {
    const now = new Date();
    return now.toLocaleDateString('es-ES', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    }).toUpperCase();
  }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  closeSidebar(): void {
    this.sidebarOpen = false;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}

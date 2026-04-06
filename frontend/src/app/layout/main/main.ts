import { Component, inject } from '@angular/core';
import { RouterOutlet, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { AuthResponse } from '../../models/auth.model';
import { Sidebar } from '../../shared/components/sidebar/sidebar';
import { Header } from '../header/header';

@Component({
  selector: 'app-main',
  imports: [RouterOutlet, Sidebar, Header],
  templateUrl: './main.html',
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

import { Component, inject, signal } from '@angular/core';
import { RouterOutlet, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { AuthResponse } from '../../models/auth.model';
import { Sidebar } from '../../shared/components/sidebar/sidebar';
import { ProfileModal } from '../../shared/components/profile-modal/profile-modal';
import { ContactModal } from '../../shared/components/contact-modal/contact-modal';
import { Header } from '../header/header';

@Component({
  selector: 'app-main',
  imports: [RouterOutlet, Sidebar, Header, ProfileModal, ContactModal],
  templateUrl: './main.html',
})
export class Main {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  sidebarOpen = false;
  readonly profileModalOpen = signal(false);
  readonly contactModalOpen = signal(false);
  readonly currentUser = signal<AuthResponse | null>(this.authService.getUser());

  get user(): AuthResponse | null {
    return this.currentUser();
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

  openProfile(): void {
    this.profileModalOpen.set(true);
  }

  closeProfile(): void {
    this.profileModalOpen.set(false);
  }

  openContact(): void {
    this.contactModalOpen.set(true);
  }

  closeContact(): void {
    this.contactModalOpen.set(false);
  }

  onProfileUpdated(updated: AuthResponse): void {
    this.authService.updateStoredUser(updated);
    this.currentUser.set({ ...updated });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}

import { Component, Input, Output, EventEmitter, OnInit, OnChanges, OnDestroy, SimpleChanges, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { AuthResponse } from '../../../models/auth.model';
import { AuthService } from '../../../services/auth.service';
import { ProfileService } from '../../../services/profile.service';
import { ROLE_LABELS } from '../../constants/roles.const';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive, LucideAngularModule],
  templateUrl: './sidebar.html',
})
export class Sidebar implements OnInit, OnChanges, OnDestroy {
  private readonly router = inject(Router);
  private readonly profileService = inject(ProfileService);
  readonly auth = inject(AuthService);

  @Input() open = false;
  @Input() user: AuthResponse | null = null;
  @Input() userInitials = '';
  @Output() closed = new EventEmitter<void>();
  @Output() loggedOut = new EventEmitter<void>();
  @Output() profileEditRequested = new EventEmitter<void>();
  @Output() contactRequested = new EventEmitter<void>();

  readonly avatarObjectUrl = signal<string | null>(null);
  actsMenuOpen = false;

  ngOnInit(): void {
    this.actsMenuOpen = this.router.url.startsWith('/events') || this.router.url.startsWith('/my-tasks');
    this.refreshAvatar();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['user']) {
      this.refreshAvatar();
    }
  }

  ngOnDestroy(): void {
    this.revokeAvatar();
  }

  toggleActsMenu(): void {
    this.actsMenuOpen = !this.actsMenuOpen;
  }

  // Show the contact link only for hermandad members (super admin has no hermandad).
  get showContact(): boolean {
    return !!this.user?.hermandadNombre && !this.auth.isSuperAdmin();
  }

  get roleLabel(): string {
    const code = this.user?.roles?.[0];
    return code ? (ROLE_LABELS[code] ?? '') : '';
  }

  private refreshAvatar(): void {
    this.revokeAvatar();
    const u = this.user;
    if (!u || !u.hasAvatar) {
      return;
    }
    this.profileService.loadAvatar(u.userId).subscribe({
      next: (url) => this.avatarObjectUrl.set(url),
      error: () => this.avatarObjectUrl.set(null),
    });
  }

  private revokeAvatar(): void {
    const url = this.avatarObjectUrl();
    if (url) {
      URL.revokeObjectURL(url);
    }
    this.avatarObjectUrl.set(null);
  }
}

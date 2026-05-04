import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.model';
import { EventResponse } from '../models/event.model';
import { ROLES_ADMIN, ROLES_MANAGE, ROLES_WRITE, Role } from '../shared/constants/roles.const';

// Authentication facade: login, registration, session storage and role checks.
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/auth';
  private readonly tokenKey = 'auth_token';
  private readonly userKey = 'auth_user';

  // Sends credentials and persists the session on success.
  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, request).pipe(
      tap((response) => this.storeSession(response))
    );
  }

  // Creates a new account and persists the session on success.
  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/register`, request).pipe(
      tap((response) => this.storeSession(response))
    );
  }

  // Removes both token and user payload from local storage.
  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  // Returns the cached user; clears storage if the payload is corrupt.
  getUser(): AuthResponse | null {
    const raw = localStorage.getItem(this.userKey);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as AuthResponse;
    } catch {
      this.logout();
      return null;
    }
  }

  // Persists the freshly updated user payload (e.g. after profile edit).
  updateStoredUser(user: AuthResponse): void {
    localStorage.setItem(this.userKey, JSON.stringify(user));
  }

  // Returns true when the current user holds the given role.
  hasRole(role: Role | string): boolean {
    return this.getUser()?.roles?.includes(role) ?? false;
  }

  // Returns true when the user holds at least one of the given roles.
  hasAnyRole(...roles: ReadonlyArray<Role | string>): boolean {
    return roles.some((r) => this.hasRole(r));
  }

  canWrite(): boolean {
    return this.hasAnyRole(...ROLES_WRITE);
  }

  canManage(): boolean {
    return this.hasAnyRole(...ROLES_MANAGE);
  }

  isAdmin(): boolean {
    return this.hasAnyRole(...ROLES_ADMIN);
  }

  isConsulta(): boolean {
    return this.hasRole('CONSULTA');
  }

  getUserId(): number | null {
    return this.getUser()?.userId ?? null;
  }

  // Admins can manage any act; otherwise only the responsible user can.
  canManageAct(event: EventResponse | null): boolean {
    return this.isAdmin() || (event !== null && event.responsibleId === this.getUserId());
  }

  private storeSession(response: AuthResponse): void {
    localStorage.setItem(this.tokenKey, response.token);
    localStorage.setItem(this.userKey, JSON.stringify(response));
  }
}

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.model';
import { EventResponse } from '../models/event.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/auth';
  private readonly tokenKey = 'auth_token';
  private readonly userKey = 'auth_user';
  private readonly storage: Storage = typeof sessionStorage !== 'undefined' ? sessionStorage : localStorage;

  constructor() {
    // migrate pre-existing session data out of persistent localStorage to sessionStorage
    try {
      if (typeof localStorage !== 'undefined' && localStorage.getItem(this.tokenKey)) {
        const legacyToken = localStorage.getItem(this.tokenKey);
        const legacyUser = localStorage.getItem(this.userKey);
        if (legacyToken) this.storage.setItem(this.tokenKey, legacyToken);
        if (legacyUser) this.storage.setItem(this.userKey, legacyUser);
        localStorage.removeItem(this.tokenKey);
        localStorage.removeItem(this.userKey);
      }
    } catch {
      // browsers may throw in private mode or when storage is full
    }
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, request).pipe(
      tap((response) => this.storeSession(response))
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/register`, request).pipe(
      tap((response) => this.storeSession(response))
    );
  }

  logout(): void {
    this.storage.removeItem(this.tokenKey);
    this.storage.removeItem(this.userKey);
  }

  getToken(): string | null {
    return this.storage.getItem(this.tokenKey);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  getUser(): AuthResponse | null {
    const raw = this.storage.getItem(this.userKey);
    return raw ? JSON.parse(raw) : null;
  }

  updateStoredUser(user: AuthResponse): void {
    this.storage.setItem(this.userKey, JSON.stringify(user));
  }

  hasRole(role: string): boolean {
    return this.getUser()?.roles?.includes(role) ?? false;
  }

  hasAnyRole(...roles: string[]): boolean {
    return roles.some(r => this.hasRole(r));
  }

  canWrite(): boolean {
    return this.hasAnyRole('ADMINISTRADOR', 'RESPONSABLE', 'COLABORADOR');
  }

  canManage(): boolean {
    return this.hasAnyRole('ADMINISTRADOR', 'RESPONSABLE');
  }

  isAdmin(): boolean {
    return this.hasRole('ADMINISTRADOR');
  }

  isConsulta(): boolean {
    return this.hasRole('CONSULTA');
  }

  getUserId(): number | null {
    return this.getUser()?.userId ?? null;
  }

  canManageAct(event: EventResponse | null): boolean {
    return this.isAdmin() || (event !== null && event.responsibleId === this.getUserId());
  }

  private storeSession(response: AuthResponse): void {
    this.storage.setItem(this.tokenKey, response.token);
    this.storage.setItem(this.userKey, JSON.stringify(response));
  }
}

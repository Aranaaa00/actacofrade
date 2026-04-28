import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RoleStatsResponse, UserCreateRequest, UserResponse, UserUpdateRequest } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/users';

  findAll(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(this.baseUrl);
  }

  findAssignable(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(`${this.baseUrl}/assignable`);
  }

  findById(id: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.baseUrl}/${id}`);
  }

  getStats(): Observable<RoleStatsResponse> {
    return this.http.get<RoleStatsResponse>(`${this.baseUrl}/stats`);
  }

  create(request: UserCreateRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(this.baseUrl, request);
  }

  update(id: number, request: UserUpdateRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.baseUrl}/${id}`, request);
  }

  toggleActive(id: number): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.baseUrl}/${id}/toggle-active`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}

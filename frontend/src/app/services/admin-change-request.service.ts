import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AdminChangeRequestApprove,
  AdminChangeRequestCreate,
  AdminChangeRequestResponse,
} from '../models/admin-change-request.model';
import { UserResponse } from '../models/user.model';
import { retryReads } from '../shared/utils/retry.utils';

// HTTP client for /api/admin-change-requests; reads use retryReads() for transient errors.
@Injectable({ providedIn: 'root' })
export class AdminChangeRequestService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/admin-change-requests';

  // Creates a new request for the current user.
  create(request: AdminChangeRequestCreate): Observable<AdminChangeRequestResponse> {
    return this.http.post<AdminChangeRequestResponse>(this.baseUrl, request);
  }

  // Lists every request (super admin only).
  findAll(): Observable<AdminChangeRequestResponse[]> {
    return this.http.get<AdminChangeRequestResponse[]>(this.baseUrl).pipe(retryReads());
  }

  // Gets a single request by id.
  findById(id: number): Observable<AdminChangeRequestResponse> {
    return this.http.get<AdminChangeRequestResponse>(`${this.baseUrl}/${id}`).pipe(retryReads());
  }

  // Returns candidate users that can become the new admin.
  findCandidates(id: number): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(`${this.baseUrl}/${id}/candidates`).pipe(retryReads());
  }

  // Approves the request and assigns the chosen new admin.
  approve(id: number, payload: AdminChangeRequestApprove): Observable<AdminChangeRequestResponse> {
    return this.http.patch<AdminChangeRequestResponse>(`${this.baseUrl}/${id}/approve`, payload);
  }

  // Rejects the request without changing roles.
  reject(id: number): Observable<AdminChangeRequestResponse> {
    return this.http.patch<AdminChangeRequestResponse>(`${this.baseUrl}/${id}/reject`, {});
  }
}

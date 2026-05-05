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

@Injectable({ providedIn: 'root' })
export class AdminChangeRequestService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/admin-change-requests';

  create(request: AdminChangeRequestCreate): Observable<AdminChangeRequestResponse> {
    return this.http.post<AdminChangeRequestResponse>(this.baseUrl, request);
  }

  findAll(): Observable<AdminChangeRequestResponse[]> {
    return this.http.get<AdminChangeRequestResponse[]>(this.baseUrl).pipe(retryReads());
  }

  findById(id: number): Observable<AdminChangeRequestResponse> {
    return this.http.get<AdminChangeRequestResponse>(`${this.baseUrl}/${id}`).pipe(retryReads());
  }

  findCandidates(id: number): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(`${this.baseUrl}/${id}/candidates`).pipe(retryReads());
  }

  approve(id: number, payload: AdminChangeRequestApprove): Observable<AdminChangeRequestResponse> {
    return this.http.patch<AdminChangeRequestResponse>(`${this.baseUrl}/${id}/approve`, payload);
  }

  reject(id: number): Observable<AdminChangeRequestResponse> {
    return this.http.patch<AdminChangeRequestResponse>(`${this.baseUrl}/${id}/reject`, {});
  }
}

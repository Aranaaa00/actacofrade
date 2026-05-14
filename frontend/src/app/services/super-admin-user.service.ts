import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { retryReads } from '../shared/utils/retry.utils';
import {
  InterventionLogEntry,
  PageResponse,
  SuperAdminRoleRequest,
  SuperAdminStatusRequest,
  SuperAdminUserResponse,
} from '../models/super-admin-user.model';

// Fachada del Centro de Intervención SuperAdmin (todas las acciones manuales).
@Injectable({ providedIn: 'root' })
export class SuperAdminUserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/super-admin/users';

  search(query: string, page = 0, size = 20): Observable<PageResponse<SuperAdminUserResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (query) {
      params = params.set('query', query);
    }
    return this.http
      .get<PageResponse<SuperAdminUserResponse>>(this.baseUrl, { params })
      .pipe(retryReads());
  }

  findById(id: number): Observable<SuperAdminUserResponse> {
    return this.http.get<SuperAdminUserResponse>(`${this.baseUrl}/${id}`).pipe(retryReads());
  }

  updateStatus(id: number, request: SuperAdminStatusRequest): Observable<SuperAdminUserResponse> {
    return this.http.patch<SuperAdminUserResponse>(`${this.baseUrl}/${id}/status`, request);
  }

  verify(id: number): Observable<SuperAdminUserResponse> {
    return this.http.post<SuperAdminUserResponse>(`${this.baseUrl}/${id}/verify`, {});
  }

  unverify(id: number): Observable<SuperAdminUserResponse> {
    return this.http.post<SuperAdminUserResponse>(`${this.baseUrl}/${id}/unverify`, {});
  }

  overrideRole(id: number, request: SuperAdminRoleRequest): Observable<SuperAdminUserResponse> {
    return this.http.patch<SuperAdminUserResponse>(`${this.baseUrl}/${id}/role`, request);
  }

  triggerPasswordReset(id: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${id}/password-reset`, {});
  }

  findUserLogs(id: number, page = 0, size = 20): Observable<PageResponse<InterventionLogEntry>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http
      .get<PageResponse<InterventionLogEntry>>(`${this.baseUrl}/${id}/logs`, { params })
      .pipe(retryReads());
  }

  findAllLogs(page = 0, size = 20): Observable<PageResponse<InterventionLogEntry>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http
      .get<PageResponse<InterventionLogEntry>>(`${this.baseUrl}/logs`, { params })
      .pipe(retryReads());
  }
}

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { HermandadResponse, HermandadUpdateRequest } from '../models/hermandad.model';
import { retryReads } from '../shared/utils/retry.utils';

// Reads and updates the brotherhood data of the current tenant.
@Injectable({ providedIn: 'root' })
export class HermandadService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/hermandades';

  getCurrent(): Observable<HermandadResponse> {
    return this.http.get<HermandadResponse>(`${this.baseUrl}/me`).pipe(retryReads());
  }

  updateCurrent(request: HermandadUpdateRequest): Observable<HermandadResponse> {
    return this.http.put<HermandadResponse>(`${this.baseUrl}/me`, request);
  }
}

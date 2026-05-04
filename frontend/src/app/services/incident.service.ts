import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IncidentResponse, CreateIncidentRequest } from '../models/incident.model';
import { retryReads } from '../shared/utils/retry.utils';

// CRUD facade for the incidents reported inside an event.
@Injectable({ providedIn: 'root' })
export class IncidentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/events';

  findByEventId(eventId: number): Observable<IncidentResponse[]> {
    return this.http.get<IncidentResponse[]>(`${this.baseUrl}/${eventId}/incidents`).pipe(retryReads());
  }

  findById(eventId: number, incidentId: number): Observable<IncidentResponse> {
    return this.http.get<IncidentResponse>(`${this.baseUrl}/${eventId}/incidents/${incidentId}`).pipe(retryReads());
  }

  create(eventId: number, request: CreateIncidentRequest): Observable<IncidentResponse> {
    return this.http.post<IncidentResponse>(`${this.baseUrl}/${eventId}/incidents`, request);
  }

  delete(eventId: number, incidentId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${eventId}/incidents/${incidentId}`);
  }

  resolve(eventId: number, incidentId: number): Observable<IncidentResponse> {
    return this.http.patch<IncidentResponse>(`${this.baseUrl}/${eventId}/incidents/${incidentId}/resolve`, {});
  }
}

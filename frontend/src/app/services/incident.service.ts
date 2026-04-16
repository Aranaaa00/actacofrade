import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IncidentResponse, CreateIncidentRequest } from '../models/incident.model';

@Injectable({ providedIn: 'root' })
export class IncidentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/events';

  findByEventId(eventId: number): Observable<IncidentResponse[]> {
    return this.http.get<IncidentResponse[]>(`${this.baseUrl}/${eventId}/incidents`);
  }

  findById(eventId: number, incidentId: number): Observable<IncidentResponse> {
    return this.http.get<IncidentResponse>(`${this.baseUrl}/${eventId}/incidents/${incidentId}`);
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

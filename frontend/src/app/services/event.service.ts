import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EventResponse, EventPage, CreateEventRequest, UpdateEventRequest } from '../models/event.model';
import { retryReads } from '../shared/utils/retry.utils';

// CRUD facade for the event entity. Read endpoints retry transient failures.
@Injectable({ providedIn: 'root' })
export class EventService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/events';

  findAll(): Observable<EventResponse[]> {
    return this.http.get<EventResponse[]>(this.baseUrl).pipe(retryReads());
  }

  filter(params: { eventType?: string; status?: string; eventDate?: string; search?: string; page?: number; size?: number }): Observable<EventPage> {
    // optional fields are appended only when provided so the backend can ignore unset filters
    let httpParams = new HttpParams();
    if (params.eventType) { httpParams = httpParams.set('eventType', params.eventType); }
    if (params.status) { httpParams = httpParams.set('status', params.status); }
    if (params.eventDate) { httpParams = httpParams.set('eventDate', params.eventDate); }
    if (params.search) { httpParams = httpParams.set('search', params.search); }
    if (params.page !== undefined) { httpParams = httpParams.set('page', params.page.toString()); }
    if (params.size !== undefined) { httpParams = httpParams.set('size', params.size.toString()); }
    return this.http.get<EventPage>(`${this.baseUrl}/filter`, { params: httpParams }).pipe(retryReads());
  }

  history(params: { eventType?: string; responsibleId?: number; dateFrom?: string; dateTo?: string; search?: string; page?: number; size?: number }): Observable<EventPage> {
    let httpParams = new HttpParams();
    if (params.eventType) { httpParams = httpParams.set('eventType', params.eventType); }
    if (params.responsibleId) { httpParams = httpParams.set('responsibleId', params.responsibleId.toString()); }
    if (params.dateFrom) { httpParams = httpParams.set('dateFrom', params.dateFrom); }
    if (params.dateTo) { httpParams = httpParams.set('dateTo', params.dateTo); }
    if (params.search) { httpParams = httpParams.set('search', params.search); }
    if (params.page !== undefined) { httpParams = httpParams.set('page', params.page.toString()); }
    if (params.size !== undefined) { httpParams = httpParams.set('size', params.size.toString()); }
    return this.http.get<EventPage>(`${this.baseUrl}/history`, { params: httpParams }).pipe(retryReads());
  }

  availableDates(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/available-dates`).pipe(retryReads());
  }

  findById(id: number): Observable<EventResponse> {
    return this.http.get<EventResponse>(`${this.baseUrl}/${id}`).pipe(retryReads());
  }

  create(request: CreateEventRequest): Observable<EventResponse> {
    return this.http.post<EventResponse>(this.baseUrl, request);
  }

  update(id: number, request: UpdateEventRequest): Observable<EventResponse> {
    return this.http.put<EventResponse>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  close(id: number): Observable<EventResponse> {
    return this.http.patch<EventResponse>(`${this.baseUrl}/${id}/close`, {});
  }

  advanceStatus(id: number): Observable<EventResponse> {
    return this.http.patch<EventResponse>(`${this.baseUrl}/${id}/advance-status`, {});
  }

  clone(id: number): Observable<EventResponse> {
    return this.http.post<EventResponse>(`${this.baseUrl}/${id}/clone`, {});
  }

  export(id: number, format: string, selectedSections: string[]): Observable<Blob> {
    // request the file as a binary blob so it can be passed straight to download
    return this.http.post(`${this.baseUrl}/${id}/export`, { format, selectedSections }, { responseType: 'blob' as const });
  }
}

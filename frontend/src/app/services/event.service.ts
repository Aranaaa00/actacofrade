import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EventResponse, EventPage, CreateEventRequest, UpdateEventRequest } from '../models/event.model';

@Injectable({ providedIn: 'root' })
export class EventService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/events';

  findAll(): Observable<EventResponse[]> {
    return this.http.get<EventResponse[]>(this.baseUrl);
  }

  filter(params: { eventType?: string; status?: string; eventDate?: string; search?: string; page?: number; size?: number }): Observable<EventPage> {
    let httpParams = new HttpParams();
    if (params.eventType) { httpParams = httpParams.set('eventType', params.eventType); }
    if (params.status) { httpParams = httpParams.set('status', params.status); }
    if (params.eventDate) { httpParams = httpParams.set('eventDate', params.eventDate); }
    if (params.search) { httpParams = httpParams.set('search', params.search); }
    if (params.page !== undefined) { httpParams = httpParams.set('page', params.page.toString()); }
    if (params.size !== undefined) { httpParams = httpParams.set('size', params.size.toString()); }
    return this.http.get<EventPage>(`${this.baseUrl}/filter`, { params: httpParams });
  }

  findById(id: number): Observable<EventResponse> {
    return this.http.get<EventResponse>(`${this.baseUrl}/${id}`);
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
}

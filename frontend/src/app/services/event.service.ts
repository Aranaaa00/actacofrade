import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EventResponse, CreateEventRequest, UpdateEventRequest } from '../models/event.model';

@Injectable({ providedIn: 'root' })
export class EventService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/events';

  findAll(): Observable<EventResponse[]> {
    return this.http.get<EventResponse[]>(this.baseUrl);
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
}

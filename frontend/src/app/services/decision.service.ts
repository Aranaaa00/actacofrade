import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DecisionResponse, CreateDecisionRequest, UpdateDecisionRequest } from '../models/decision.model';

@Injectable({ providedIn: 'root' })
export class DecisionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/events';

  findByEventId(eventId: number): Observable<DecisionResponse[]> {
    return this.http.get<DecisionResponse[]>(`${this.baseUrl}/${eventId}/decisions`);
  }

  findById(eventId: number, decisionId: number): Observable<DecisionResponse> {
    return this.http.get<DecisionResponse>(`${this.baseUrl}/${eventId}/decisions/${decisionId}`);
  }

  create(eventId: number, request: CreateDecisionRequest): Observable<DecisionResponse> {
    return this.http.post<DecisionResponse>(`${this.baseUrl}/${eventId}/decisions`, request);
  }

  update(eventId: number, decisionId: number, request: UpdateDecisionRequest): Observable<DecisionResponse> {
    return this.http.put<DecisionResponse>(`${this.baseUrl}/${eventId}/decisions/${decisionId}`, request);
  }

  delete(eventId: number, decisionId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${eventId}/decisions/${decisionId}`);
  }

  accept(eventId: number, decisionId: number): Observable<DecisionResponse> {
    return this.http.patch<DecisionResponse>(`${this.baseUrl}/${eventId}/decisions/${decisionId}/accept`, {});
  }

  reject(eventId: number, decisionId: number): Observable<DecisionResponse> {
    return this.http.patch<DecisionResponse>(`${this.baseUrl}/${eventId}/decisions/${decisionId}/reject`, {});
  }
}

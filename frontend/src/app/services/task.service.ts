import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TaskResponse, CreateTaskRequest, UpdateTaskRequest, MyTaskPage, MyTaskStats } from '../models/task.model';
import { retryReads } from '../shared/utils/retry.utils';

// CRUD facade for tasks bound to an event. Read endpoints retry transient errors.
@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/events';

  findByEventId(eventId: number): Observable<TaskResponse[]> {
    return this.http.get<TaskResponse[]>(`${this.baseUrl}/${eventId}/tasks`).pipe(retryReads());
  }

  findById(eventId: number, taskId: number): Observable<TaskResponse> {
    return this.http.get<TaskResponse>(`${this.baseUrl}/${eventId}/tasks/${taskId}`).pipe(retryReads());
  }

  create(eventId: number, request: CreateTaskRequest): Observable<TaskResponse> {
    return this.http.post<TaskResponse>(`${this.baseUrl}/${eventId}/tasks`, request);
  }

  update(eventId: number, taskId: number, request: UpdateTaskRequest): Observable<TaskResponse> {
    return this.http.put<TaskResponse>(`${this.baseUrl}/${eventId}/tasks/${taskId}`, request);
  }

  delete(eventId: number, taskId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${eventId}/tasks/${taskId}`);
  }

  accept(eventId: number, taskId: number): Observable<TaskResponse> {
    return this.http.patch<TaskResponse>(`${this.baseUrl}/${eventId}/tasks/${taskId}/accept`, {});
  }

  startPreparation(eventId: number, taskId: number): Observable<TaskResponse> {
    return this.http.patch<TaskResponse>(`${this.baseUrl}/${eventId}/tasks/${taskId}/start-preparation`, {});
  }

  confirm(eventId: number, taskId: number): Observable<TaskResponse> {
    return this.http.patch<TaskResponse>(`${this.baseUrl}/${eventId}/tasks/${taskId}/confirm`, {});
  }

  complete(eventId: number, taskId: number): Observable<TaskResponse> {
    return this.http.patch<TaskResponse>(`${this.baseUrl}/${eventId}/tasks/${taskId}/complete`, {});
  }

  reject(eventId: number, taskId: number, rejectionReason: string): Observable<TaskResponse> {
    return this.http.patch<TaskResponse>(`${this.baseUrl}/${eventId}/tasks/${taskId}/reject`, { rejectionReason });
  }

  reset(eventId: number, taskId: number): Observable<TaskResponse> {
    return this.http.patch<TaskResponse>(`${this.baseUrl}/${eventId}/tasks/${taskId}/reset`, {});
  }

  findMyTasks(params: {
    eventType?: string;
    statusGroup?: string;
    search?: string;
    page?: number;
    size?: number;
  }): Observable<MyTaskPage> {
    const httpParams: Record<string, string> = {};
    if (params.eventType) httpParams['eventType'] = params.eventType;
    if (params.statusGroup) httpParams['statusGroup'] = params.statusGroup;
    if (params.search) httpParams['search'] = params.search;
    if (params.page !== undefined) httpParams['page'] = params.page.toString();
    if (params.size !== undefined) httpParams['size'] = params.size.toString();
    return this.http.get<MyTaskPage>('/api/my-tasks', { params: httpParams }).pipe(retryReads());
  }

  getMyTaskStats(): Observable<MyTaskStats> {
    return this.http.get<MyTaskStats>('/api/my-tasks/stats').pipe(retryReads());
  }
}

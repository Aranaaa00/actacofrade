import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TaskResponse, CreateTaskRequest, UpdateTaskRequest } from '../models/task.model';

@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/events';

  findByEventId(eventId: number): Observable<TaskResponse[]> {
    return this.http.get<TaskResponse[]>(`${this.baseUrl}/${eventId}/tasks`);
  }

  findById(eventId: number, taskId: number): Observable<TaskResponse> {
    return this.http.get<TaskResponse>(`${this.baseUrl}/${eventId}/tasks/${taskId}`);
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
}

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { UserResponse } from '../models/user.model';

export interface UpdateProfilePayload {
  fullName: string;
  email: string;
}

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
}

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/me';

  me(): Observable<UserResponse> {
    return this.http.get<UserResponse>(this.baseUrl);
  }

  updateProfile(payload: UpdateProfilePayload): Observable<UserResponse> {
    return this.http.put<UserResponse>(this.baseUrl, payload);
  }

  changePassword(payload: ChangePasswordPayload): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/password`, payload);
  }

  uploadAvatar(file: File): Observable<UserResponse> {
    // build a multipart form so the backend receives the file with its original metadata
    const form = new FormData();
    form.append('file', file);
    return this.http.post<UserResponse>(`${this.baseUrl}/avatar`, form);
  }

  deleteAvatar(): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/avatar`);
  }

  loadAvatar(userId: number): Observable<string> {
    // fetch the avatar as a blob and turn it into an object url usable by img src
    return this.http
      .get(`${this.baseUrl}/avatar/${userId}`, { responseType: 'blob' })
      .pipe(map((blob) => URL.createObjectURL(blob)));
  }
}

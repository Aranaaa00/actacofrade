import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, throwError } from 'rxjs';
import { UserResponse } from '../models/user.model';

export interface UpdateProfilePayload {
  fullName: string;
  email: string;
}

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
}

export const AVATAR_MAX_BYTES = 2 * 1024 * 1024;
export const AVATAR_ALLOWED_TYPES: ReadonlyArray<string> = ['image/jpeg', 'image/png', 'image/webp'];

// Validation error raised before sending the avatar to the backend.
export class AvatarValidationError extends Error {
  constructor(public readonly code: 'TYPE' | 'SIZE', message: string) {
    super(message);
  }
}

// Manages the data and avatar of the currently authenticated user.
@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/me';

  // Returns the profile of the current user.
  me(): Observable<UserResponse> {
    return this.http.get<UserResponse>(this.baseUrl);
  }

  // Updates editable profile fields (name, email).
  updateProfile(payload: UpdateProfilePayload): Observable<UserResponse> {
    return this.http.put<UserResponse>(this.baseUrl, payload);
  }

  // Changes the password of the current user.
  changePassword(payload: ChangePasswordPayload): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/password`, payload);
  }

  // Validates MIME type and size before uploading.
  uploadAvatar(file: File): Observable<UserResponse> {
    if (!AVATAR_ALLOWED_TYPES.includes(file.type)) {
      return throwError(() => new AvatarValidationError('TYPE', 'Formato no permitido. Usa JPG, PNG o WebP.'));
    }
    if (file.size > AVATAR_MAX_BYTES) {
      return throwError(() => new AvatarValidationError('SIZE', 'La imagen supera el tamaño máximo de 2 MB.'));
    }
    const form = new FormData();
    form.append('file', file);
    return this.http.post<UserResponse>(`${this.baseUrl}/avatar`, form);
  }

  // Removes the current avatar of the user.
  deleteAvatar(): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/avatar`);
  }

  // Downloads the avatar binary and exposes it as a local object URL.
  loadAvatar(userId: number): Observable<string> {
    return this.http
      .get(`${this.baseUrl}/avatar/${userId}`, { responseType: 'blob' })
      .pipe(map((blob) => URL.createObjectURL(blob)));
  }
}

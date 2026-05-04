import { HttpErrorResponse } from '@angular/common/http';

// Extracts a user-friendly message from a backend HttpErrorResponse.
export function extractErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error;
    if (body && typeof body === 'object' && typeof body.message === 'string' && body.message.trim()) {
      return body.message;
    }
    if (error.status === 0) {
      return 'Sin conexión con el servidor. Comprueba tu red e inténtalo de nuevo.';
    }
    if (error.status >= 500) {
      return 'Error del servidor. Inténtalo de nuevo más tarde.';
    }
  }
  return fallback;
}

// True for authentication failures that should trigger a session reset.
export function isUnauthorized(error: unknown): boolean {
  return error instanceof HttpErrorResponse && error.status === 401;
}

import { HttpErrorResponse } from '@angular/common/http';
import { extractErrorMessage, isUnauthorized } from './http-error.utils';

describe('http-error.utils', () => {
  describe('extractErrorMessage', () => {
    it('returns body.message when present', () => {
      const error = new HttpErrorResponse({ error: { message: 'Detalle del error' }, status: 400 });
      expect(extractErrorMessage(error, 'fallback')).toBe('Detalle del error');
    });

    it('returns offline message for status 0', () => {
      const error = new HttpErrorResponse({ status: 0 });
      expect(extractErrorMessage(error, 'fallback')).toContain('Sin conexión');
    });

    it('returns server error message for 5xx', () => {
      const error = new HttpErrorResponse({ status: 503 });
      expect(extractErrorMessage(error, 'fallback')).toContain('Error del servidor');
    });

    it('returns fallback for arbitrary errors', () => {
      expect(extractErrorMessage(new Error('boom'), 'mensaje fallback')).toBe('mensaje fallback');
    });

    it('returns fallback for HttpErrorResponse without recognizable info', () => {
      const error = new HttpErrorResponse({ status: 400, error: {} });
      expect(extractErrorMessage(error, 'fallback')).toBe('fallback');
    });

    it('ignores empty body messages', () => {
      const error = new HttpErrorResponse({ status: 400, error: { message: '   ' } });
      expect(extractErrorMessage(error, 'fallback')).toBe('fallback');
    });
  });

  describe('isUnauthorized', () => {
    it('returns true for 401 HttpErrorResponse', () => {
      expect(isUnauthorized(new HttpErrorResponse({ status: 401 }))).toBe(true);
    });

    it('returns false for other statuses', () => {
      expect(isUnauthorized(new HttpErrorResponse({ status: 500 }))).toBe(false);
    });

    it('returns false for non-HttpErrorResponse values', () => {
      expect(isUnauthorized(new Error('x'))).toBe(false);
      expect(isUnauthorized(null)).toBe(false);
    });
  });
});

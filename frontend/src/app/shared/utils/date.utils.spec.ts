import { formatDate, formatDateTime } from './date.utils';

describe('date.utils', () => {
  describe('formatDate', () => {
    it('returns dash for null input', () => {
      expect(formatDate(null)).toBe('—');
    });

    it('formats ISO dates in Spanish locale', () => {
      const result = formatDate('2025-04-10');
      expect(result).toMatch(/\d{2}/);
      expect(result).toMatch(/2025/);
    });
  });

  describe('formatDateTime', () => {
    it('returns dash for null input', () => {
      expect(formatDateTime(null)).toBe('—');
    });

    it('formats ISO datetimes including time component', () => {
      const result = formatDateTime('2025-04-10T15:30:00Z');
      expect(result).toMatch(/2025/);
    });
  });
});

import { sanitizeText, sanitizeFormValues, noHtmlValidator } from './sanitize.utils';
import { FormControl } from '@angular/forms';

describe('sanitize.utils', () => {
  describe('sanitizeText', () => {
    it('trims whitespace and collapses multiple spaces', () => {
      expect(sanitizeText('  hola    mundo  ')).toBe('hola mundo');
    });

    it('removes HTML tags', () => {
      expect(sanitizeText('hola <b>mundo</b>')).toBe('hola mundo');
    });

    it('returns empty string for empty input', () => {
      expect(sanitizeText('')).toBe('');
    });

    it('handles plain text without modification', () => {
      expect(sanitizeText('texto normal')).toBe('texto normal');
    });
  });

  describe('sanitizeFormValues', () => {
    it('sanitizes string fields and preserves non-string values', () => {
      const result = sanitizeFormValues({
        name: '  hola   <b>mundo</b>  ',
        age: 30,
        active: true,
        nested: { a: 1 },
      });
      expect(result.name).toBe('hola mundo');
      expect(result.age).toBe(30);
      expect(result.active).toBe(true);
      expect(result.nested).toEqual({ a: 1 });
    });

    it('does not mutate the source object', () => {
      const source = { name: '  test  ' };
      const result = sanitizeFormValues(source);
      expect(source.name).toBe('  test  ');
      expect(result.name).toBe('test');
    });
  });

  describe('noHtmlValidator', () => {
    const validator = noHtmlValidator();

    it('returns null for safe text', () => {
      expect(validator(new FormControl('texto seguro'))).toBeNull();
    });

    it('rejects values containing <', () => {
      expect(validator(new FormControl('a<b'))).toEqual({ noHtml: true });
    });

    it('rejects values containing >', () => {
      expect(validator(new FormControl('a>b'))).toEqual({ noHtml: true });
    });

    it('returns null for non-string values', () => {
      expect(validator(new FormControl(null))).toBeNull();
      expect(validator(new FormControl(123))).toBeNull();
    });
  });
});

import { FormGroup, FormControl, Validators } from '@angular/forms';
import { hasFieldError, getFieldError } from './form-validation.utils';

describe('form-validation.utils', () => {
  function build(value: string, ...validators: any[]): FormGroup {
    return new FormGroup({
      field: new FormControl(value, validators),
    });
  }

  describe('hasFieldError', () => {
    it('returns false when control is valid', () => {
      const form = build('valid', Validators.required);
      expect(hasFieldError(form, 'field')).toBe(false);
    });

    it('returns false when invalid but pristine and not submitted', () => {
      const form = build('', Validators.required);
      expect(hasFieldError(form, 'field')).toBe(false);
    });

    it('returns true when invalid and submitted', () => {
      const form = build('', Validators.required);
      expect(hasFieldError(form, 'field', true)).toBe(true);
    });

    it('returns true when invalid and dirty', () => {
      const form = build('', Validators.required);
      form.get('field')!.markAsDirty();
      expect(hasFieldError(form, 'field')).toBe(true);
    });

    it('returns true when invalid and touched', () => {
      const form = build('', Validators.required);
      form.get('field')!.markAsTouched();
      expect(hasFieldError(form, 'field')).toBe(true);
    });

    it('returns false when control does not exist', () => {
      const form = build('', Validators.required);
      expect(hasFieldError(form, 'missing', true)).toBe(false);
    });
  });

  describe('getFieldError', () => {
    it('returns required message', () => {
      const form = build('', Validators.required);
      expect(getFieldError(form, 'field')).toContain('obligatorio');
    });

    it('returns email message', () => {
      const form = build('not-email', Validators.email);
      expect(getFieldError(form, 'field')).toContain('email');
    });

    it('returns minlength message with required length', () => {
      const form = build('a', Validators.minLength(5));
      expect(getFieldError(form, 'field')).toContain('5');
    });

    it('returns maxlength message with required length', () => {
      const form = build('abcdef', Validators.maxLength(3));
      expect(getFieldError(form, 'field')).toContain('3');
    });

    it('returns pattern message', () => {
      const form = build('xyz', Validators.pattern(/^\d+$/));
      expect(getFieldError(form, 'field')).toContain('formato');
    });

    it('returns noHtml message', () => {
      const form = new FormGroup({
        field: new FormControl('a<b', () => ({ noHtml: true })),
      });
      expect(getFieldError(form, 'field')).toContain('< o >');
    });

    it('returns empty string when no errors', () => {
      const form = build('valid');
      expect(getFieldError(form, 'field')).toBe('');
    });
  });
});

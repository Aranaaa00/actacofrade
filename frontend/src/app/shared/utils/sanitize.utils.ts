import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

const HTML_TAG_REGEX = /<[^>]*>/g;
const MULTI_SPACE_REGEX = /\s{2,}/g;

export function sanitizeText(input: string): string {
  let result = input.trim();
  result = result.replace(HTML_TAG_REGEX, '');
  result = result.replace(MULTI_SPACE_REGEX, ' ');
  return result.trim();
}

export function sanitizeFormValues<T extends Record<string, unknown>>(values: T): T {
  const sanitized = { ...values };
  for (const key of Object.keys(sanitized)) {
    const value = sanitized[key];
    if (typeof value === 'string') {
      (sanitized as Record<string, unknown>)[key] = sanitizeText(value);
    }
  }
  return sanitized;
}

export function noHtmlValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    let result: ValidationErrors | null = null;
    if (typeof value === 'string' && (value.includes('<') || value.includes('>'))) {
      result = { noHtml: true };
    }
    return result;
  };
}

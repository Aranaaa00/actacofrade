import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

const HTML_TAG_REGEX = /<[^>]*>/g;
const MULTI_SPACE_REGEX = /\s{2,}/g;

// Strips HTML tags and collapses repeated whitespace from a free-text input.
export function sanitizeText(input: string): string {
  // strip html tags and collapse repeated whitespace before sending text to the api
  let result = input.trim();
  result = result.replace(HTML_TAG_REGEX, '');
  result = result.replace(MULTI_SPACE_REGEX, ' ');
  return result.trim();
}

// Returns a copy of the form values with every string field sanitized.
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

// Validator that rejects values containing angle brackets to prevent injected HTML.
export function noHtmlValidator(): ValidatorFn {
  // reactive forms validator that rejects any value containing html-like characters
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    let result: ValidationErrors | null = null;
    if (typeof value === 'string' && (value.includes('<') || value.includes('>'))) {
      result = { noHtml: true };
    }
    return result;
  };
}

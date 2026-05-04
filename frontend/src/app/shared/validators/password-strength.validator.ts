import { AbstractControl, ValidationErrors } from '@angular/forms';

// Reactive Forms validator that enforces a strong password policy.
export function passwordStrength(control: AbstractControl): ValidationErrors | null {
  const value: string = control.value || '';
  const errors: ValidationErrors = {};

  if (value.length < 8) {
    errors['tooShort'] = true;
  }
  if (value.length > 100) {
    errors['tooLong'] = true;
  }
  if (!/\p{Lu}/u.test(value)) {
    errors['noUppercase'] = true;
  }
  if (!/\p{Ll}/u.test(value)) {
    errors['noLowercase'] = true;
  }
  if (!/\d/.test(value)) {
    errors['noDigit'] = true;
  }
  if (!/[@$!%*?&.#_\-]/.test(value)) {
    errors['noSpecial'] = true;
  }
  // Enforce same allowed character set as backend (any Unicode letter, digit, or allowed special).
  if (value.length > 0 && !/^[\p{L}\d@$!%*?&.#_\-]+$/u.test(value)) {
    errors['invalidChars'] = true;
  }

  return Object.keys(errors).length ? errors : null;
}

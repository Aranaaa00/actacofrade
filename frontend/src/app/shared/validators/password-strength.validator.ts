import { AbstractControl, ValidationErrors } from '@angular/forms';

export function passwordStrength(control: AbstractControl): ValidationErrors | null {
  const value: string = control.value || '';
  const errors: ValidationErrors = {};

  if (value.length < 8) {
    errors['minlength'] = true;
  }
  if (!/[A-Z]/.test(value)) {
    errors['noUppercase'] = true;
  }
  if (!/[a-z]/.test(value)) {
    errors['noLowercase'] = true;
  }
  if (!/\d/.test(value)) {
    errors['noDigit'] = true;
  }
  if (!/[@$!%*?&.#_\-]/.test(value)) {
    errors['noSpecial'] = true;
  }

  return Object.keys(errors).length ? errors : null;
}
